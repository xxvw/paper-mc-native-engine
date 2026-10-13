#!/usr/bin/env python3
"""Launch isolated no-op servers and compare persisted semantic smoke fixtures."""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
import queue
import socket
import struct
import subprocess
import tempfile
import threading
import time
import zlib

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
COORDS = [(0, 0), (-1, -1), (1, -1), (31, 31), (32, 32), (-32, -32)]
DIMENSIONS = {dimension: f"world/dimensions/minecraft/{dimension}/region"
              for dimension in ("overworld", "the_nether", "the_end")}
EXCLUSIONS = {"LastUpdate", "InhabitedTime"}


def canonical(value):
    if isinstance(value, dict):
        return {str(key): canonical(value[key]) for key in sorted(value)}
    if isinstance(value, (list, tuple)):
        return [canonical(item) for item in value]
    if hasattr(value, "tolist"):
        return canonical(value.tolist())
    return value


def expand_palette(container, count, min_bits):
    palette = [canonical(value) for value in container["palette"]]
    if len(palette) == 1:
        return [palette[0]] * count
    bits = max(min_bits, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    packed = container["data"]
    if len(packed) != (count + per_long - 1) // per_long:
        raise ValueError("Malformed packed palette length")
    result = []
    for index in range(count):
        word = int(packed[index // per_long]) & ((1 << 64) - 1)
        entry = (word >> ((index % per_long) * bits)) & ((1 << bits) - 1)
        if entry >= len(palette):
            raise ValueError("Palette index out of bounds")
        result.append(palette[entry])
    return result


def normalize(chunk):
    result = {key: canonical(value) for key, value in chunk.items() if key not in EXCLUSIONS}
    for section in result.get("sections", []):
        for field, count, bits in [("block_states", 4096, 4), ("biomes", 64, 1)]:
            if field in section:
                section[field] = expand_palette(section[field], count, bits)
    if "sections" in result:
        result["sections"].sort(key=lambda section: section["Y"])
    if "block_entities" in result:
        result["block_entities"].sort(key=lambda entity: (entity.get("x"), entity.get("y"), entity.get("z")))
    return result


def read_chunk(directory, x, z):
    path = directory / f"r.{x // 32}.{z // 32}.mca"
    index = (x % 32) + (z % 32) * 32
    with path.open("rb") as stream:
        stream.seek(index * 4)
        location = int.from_bytes(stream.read(4), "big")
        offset, sectors = location >> 8, location & 255
        if not offset or not sectors:
            raise ValueError(f"Missing persisted chunk ({x}, {z})")
        stream.seek(offset * 4096)
        length = struct.unpack(">I", stream.read(4))[0]
        if length < 1 or length > sectors * 4096 - 4:
            raise ValueError("Malformed region record length")
        compression = stream.read(1)[0]
        if compression & 128:
            raise ValueError("External region chunks need a separately verified reader")
        payload = stream.read(length - 1)
    if compression == 1:
        payload = gzip.decompress(payload)
    elif compression == 2:
        payload = zlib.decompress(payload)
    elif compression != 3:
        raise ValueError(f"Unverified compression type {compression}")
    chunk = nbtlib.File.parse(io.BytesIO(payload)).unpack()
    if chunk.get("Status") not in ("full", "minecraft:full"):
        raise ValueError(f"Chunk not fully generated: {chunk.get('Status')}")
    if (chunk["xPos"], chunk["zPos"]) != (x, z):
        raise ValueError("Chunk position does not match requested fixture")
    return normalize(chunk)


def first_difference(left, right, path="$"):
    if type(left) is not type(right):
        return path
    if isinstance(left, dict):
        if left.keys() != right.keys():
            return path + ".keys"
        for key in left:
            difference = first_difference(left[key], right[key], f"{path}.{key}")
            if difference:
                return difference
    elif isinstance(left, list):
        if len(left) != len(right):
            return path + ".length"
        for index, (a, b) in enumerate(zip(left, right)):
            difference = first_difference(a, b, f"{path}[{index}]")
            if difference:
                return difference
    elif left != right:
        return path
    return None


def launch(java, jar, directory, seed):
    directory.mkdir()
    (directory / ".mcne-managed.json").write_text(json.dumps({"purpose": "phase0-smoke", "seed": seed}))
    (directory / "eula.txt").write_text("eula=true\n")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    properties = {
        "server-ip": "127.0.0.1", "server-port": port, "online-mode": "true",
        "level-seed": seed, "view-distance": 2, "simulation-distance": 2,
        "max-players": 1, "enable-rcon": "false", "enable-query": "false",
        "spawn-protection": 0, "sync-chunk-writes": "true",
    }
    (directory / "server.properties").write_text("\n".join(f"{k}={v}" for k, v in properties.items()) + "\n")
    (directory / "config").mkdir()
    # Verified pinned Paper configuration keys. Serialize generation workers
    # for smoke fixtures; this is not representative performance evidence.
    (directory / "config/paper-global.yml").write_text(
        "_version: 31\nchunk-system:\n  worker-threads: 1\n  io-threads: 1\n")
    process = subprocess.Popen([str(java), "-Xms1G", "-Xmx2G", "-jar", str(jar), "--nogui"],
                               cwd=directory, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, bufsize=1)
    lines = queue.Queue()

    def consume():
        with (directory / "console.log").open("w") as log:
            for line in process.stdout:
                log.write(line)
                log.flush()
                lines.put(line)

    reader = threading.Thread(target=consume, daemon=True)
    reader.start()

    def send(command):
        if process.poll() is not None:
            raise RuntimeError(f"Server exited with {process.returncode}; inspect console.log")
        process.stdin.write(command + "\n")
        process.stdin.flush()

    def wait_for(token, timeout=180):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                line = lines.get(timeout=0.5)
            except queue.Empty:
                if process.poll() is not None:
                    raise RuntimeError(f"Server exited before {token}; inspect console.log")
                continue
            if token in line:
                return
        raise TimeoutError(f"Timed out waiting for {token}; inspect console.log")

    try:
        # Chunk workers and console commands keep running, while gameplay
        # block/fluid ticks must not mutate fixtures at different wall times.
        wait_for('Done (', 300)
        send("tick freeze")
        wait_for("frozen", 10)
        for dimension in DIMENSIONS:
            prefix = f"execute in minecraft:{dimension} run "
            # Disable gameplay mutations; no players or third-party plugins are present.
            for rule, value in [("random_tick_speed", "0"), ("spawn_mobs", "false"), ("advance_time", "false")]:
                send(prefix + f"gamerule {rule} {value}")
                wait_for("is now set to", 10)
            for index, (x, z) in enumerate(COORDS):
                marker = f"MCNE_READY_{dimension}_{index}"
                send(prefix + f"forceload add {x * 16} {z * 16}")
                wait_for("Marked chunk")
                deadline = time.monotonic() + 180
                while time.monotonic() < deadline:
                    send(prefix + f"execute if loaded {x * 16} 64 {z * 16} run say {marker}")
                    try:
                        wait_for(marker, 2)
                        break
                    except TimeoutError:
                        pass
                else:
                    raise TimeoutError(f"Fixture chunk never became loaded: {dimension} {x} {z}")
        send("save-all flush")
        wait_for("Saved the game")
        send("stop")
        process.wait(timeout=120)
        if process.returncode != 0:
            raise RuntimeError(f"Unclean shutdown: {process.returncode}")
    finally:
        if process.poll() is None:
            try:
                send("stop")
                process.wait(timeout=60)
            except (OSError, subprocess.TimeoutExpired):
                process.terminate()
                process.wait(timeout=30)
        reader.join(timeout=10)


def compare(run):
    hashes = {}
    differences = []
    for dimension, relative in DIMENSIONS.items():
        for x, z in COORDS:
            a = read_chunk(run / "baseline" / relative, x, z)
            b = read_chunk(run / "fork" / relative, x, z)
            key = f"{dimension}:{x},{z}"
            hashes[key] = {mode: hashlib.sha256(json.dumps(snapshot, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
                           for mode, snapshot in [("baseline", a), ("fork", b)]}
            difference = first_difference(a, b)
            if difference:
                differences.append({"chunk": key, "field": difference})
    return {"status": "PASS" if not differences else "FAIL", "hashes": hashes, "differences": differences}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", type=Path)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--fork", type=Path)
    parser.add_argument("--compare-run", help="Re-read a retained managed run without starting servers")
    parser.add_argument("--accept-eula", action="store_true")
    parser.add_argument("--seed", type=int, default=0)
    args = parser.parse_args()
    managed = ROOT / ".local/compat-runs"
    managed.mkdir(parents=True, exist_ok=True)
    if not managed.resolve().is_relative_to(ROOT) or managed.is_symlink():
        parser.error("Managed test root must remain inside this checkout")
    if args.compare_run:
        run = managed / args.compare_run
        if run.parent != managed or not run.resolve().is_relative_to(managed.resolve()):
            parser.error("Comparison must use an existing managed run ID")
        for mode in ("baseline", "fork"):
            if not (run / mode / ".mcne-managed.json").is_file():
                parser.error("Comparison requires managed baseline/fork fixtures")
        result = compare(run)
        result["sourceManifest"] = json.loads((run / "result.json").read_text())
        (run / "comparison.json").write_text(json.dumps(result, indent=2) + "\n")
        print(f"Result: {result['status']}; {run / 'comparison.json'}")
        raise SystemExit(0 if result["status"] == "PASS" else 1)
    if not args.accept_eula or not all((args.java, args.baseline, args.fork)):
        parser.error("Launching requires --java, --baseline, --fork and explicit --accept-eula")
    run = Path(tempfile.mkdtemp(prefix="phase0-", dir=managed))
    manifest = {"status": "RUNNING", "seed": args.seed, "workset": COORDS,
                "dimensions": list(DIMENSIONS), "excludedChunkFields": sorted(EXCLUSIONS),
                "tickPolicy": "tick freeze before requesting fixtures; no gameplay steps",
                "chunkWorkerThreads": 1, "chunkIoThreads": 1,
                "scope": "Persisted terrain smoke only; entities and live plugin/player behavior are Phase 3 gates",
                "artifacts": {name: hashlib.sha256(path.read_bytes()).hexdigest()
                              for name, path in [("baseline", args.baseline), ("fork", args.fork)]}}
    try:
        for mode, artifact in [("baseline", args.baseline), ("fork", args.fork)]:
            print(f"Starting {mode}; evidence: {run.name}/{mode}", flush=True)
            launch(args.java.resolve(), artifact.resolve(), run / mode, args.seed)
        manifest.update(compare(run))
    except BaseException as error:
        manifest.update(status="CANCELLED" if isinstance(error, KeyboardInterrupt) else "ERROR", error=str(error))
        raise
    finally:
        (run / "result.json").write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Result: {manifest['status']}; {run / 'result.json'}", flush=True)
    if manifest["status"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
