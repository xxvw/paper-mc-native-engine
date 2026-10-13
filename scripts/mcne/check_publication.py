#!/usr/bin/env python3
"""Reject local requirement documents in every commit introduced by the fork."""
import json
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[2]
lock = json.loads((root / "upstream.lock.json").read_text())
commits = subprocess.check_output(
    ["git", "rev-list", f'{lock["paperSourceCommit"]}..HEAD'], cwd=root, text=True
).splitlines()
for commit in commits:
    paths = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", commit],
                                    cwd=root, text=True).splitlines()
    if any(path.startswith("docs/") for path in paths):
        raise SystemExit(f"Refusing push: local requirements in {commit}")
print(f"Publication exclusion passed for {len(commits)} fork commits")
