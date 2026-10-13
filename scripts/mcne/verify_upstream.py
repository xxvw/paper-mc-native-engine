#!/usr/bin/env python3
"""Verify the immutable baseline; no network or installation side effects."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def verify(lock, root=ROOT, baseline=None, jdk_archive=None):
    if lock["paperChannel"] != "STABLE":
        raise ValueError("Baseline must be STABLE")
    commit = lock["paperSourceCommit"]
    if len(commit) != 40 or any(c not in "0123456789abcdef" for c in commit):
        raise ValueError("Expected full upstream source SHA")
    subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"], cwd=root, check=True)
    for path, expected in lock["upstreamFilesSha256"].items():
        upstream = subprocess.check_output(["git", "show", f"{commit}:{path}"], cwd=root)
        if hashlib.sha256(upstream).hexdigest() != expected:
            raise ValueError(f"Pinned upstream build file hash mismatch: {path}")
        maintained = lock.get("maintainedBuildFilesSha256", {}).get(path, expected)
        if digest(root / path) != maintained:
            raise ValueError(f"Upstream build file changed: {path}")
    paths = subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().split("\0")
    if any(path.startswith("docs/") for path in paths):
        raise ValueError("Local requirements under docs/ must not be tracked")
    if baseline and digest(baseline) != lock["paperDownloadSha256"]:
        raise ValueError("Paper download SHA256 mismatch")
    if jdk_archive and digest(jdk_archive) != lock["jdk"]["buildHostArchiveSha256"]:
        raise ValueError("JDK download SHA256 mismatch")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--jdk-archive", type=Path)
    args = parser.parse_args()
    try:
        verify(json.loads((ROOT / "upstream.lock.json").read_text()), baseline=args.baseline,
               jdk_archive=args.jdk_archive)
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"Baseline verification failed: {error}\n")
    print("Verified STABLE pin, ancestry, upstream build files and publication exclusions")


if __name__ == "__main__":
    main()
