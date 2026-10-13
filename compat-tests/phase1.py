#!/usr/bin/env python3
"""Run a compiled disposable Bukkit API fixture without touching preview worlds."""
import argparse
import hashlib
import json
from pathlib import Path
import tempfile
import zipfile
from smoke import ROOT, launch

parser = argparse.ArgumentParser()
parser.add_argument("--java", type=Path, required=True)
parser.add_argument("--fork", type=Path, required=True)
parser.add_argument("--accept-eula", action="store_true")
args = parser.parse_args()
if not args.accept_eula:
    parser.error("Explicit --accept-eula is required")
managed = ROOT / ".local/compat-runs"
managed.mkdir(parents=True, exist_ok=True)
run = Path(tempfile.mkdtemp(prefix="phase1-api-", dir=managed))
plugin = run / "api-smoke.jar"
name = "io/github/xxvw/mcnativechunkengine/fixture/ApiSmokePlugin.class"
compiled = ROOT / "paper-server/build/classes/java/test" / name
with zipfile.ZipFile(plugin, "w") as archive:
    archive.write(compiled, name)
    archive.writestr("plugin.yml", "name: MCNEApiSmoke\nversion: '1'\nmain: io.github.xxvw.mcnativechunkengine.fixture.ApiSmokePlugin\napi-version: '26.3'\n")
result = {"status": "RUNNING", "forkSha256": hashlib.sha256(args.fork.read_bytes()).hexdigest(),
          "scope": "Disposable plugin API, console aliases and explicit-native fallback; not broad plugin certification"}
try:
    launch(args.java.resolve(), args.fork.resolve(), run / "fork", 0, plugin)
    result["status"] = "PASS"
except BaseException as error:
    result.update(status="ERROR", error=str(error))
    raise
finally:
    (run / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(f"{result['status']}: {run / 'result.json'}", flush=True)
