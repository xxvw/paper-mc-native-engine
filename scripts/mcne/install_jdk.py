#!/usr/bin/env python3
"""Install a checksum-locked JDK under this checkout; print only JAVA_HOME."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile
import urllib.request

ROOT = Path(__file__).resolve().parents[2]


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True, choices=("linux-x86_64", "macos-aarch64"))
    args = parser.parse_args()
    lock = json.loads((ROOT / "upstream.lock.json").read_text())
    artifact = lock["jdk"]["archives"][args.target]
    tools = ROOT / ".tools"
    tools.mkdir(exist_ok=True)
    if tools.is_symlink() or not tools.resolve().is_relative_to(ROOT):
        parser.error("Tool install root must remain within the checkout")
    downloads = tools / "downloads"
    downloads.mkdir(exist_ok=True)
    if downloads.is_symlink():
        parser.error("Download root cannot be a symlink")
    archive = downloads / f"jdk-{args.target}-{artifact['sha256']}.tar.gz"
    if not archive.exists():
        request = urllib.request.Request(artifact["url"], headers={"User-Agent": "mcne-locked-jdk/0.1"})
        with urllib.request.urlopen(request, timeout=120) as response, archive.open("xb") as output:
            while data := response.read(1024 * 1024):
                output.write(data)
    if sha256(archive) != artifact["sha256"]:
        parser.error("JDK checksum mismatch; retained download requires inspection")
    installation = tools / f"jdk-{args.target}-{artifact['sha256']}"
    if not installation.exists():
        installation.mkdir()
        with tarfile.open(archive) as source:
            source.extractall(installation, filter="data")
    if installation.is_symlink() or not installation.resolve().is_relative_to(tools.resolve()):
        parser.error("JDK installation cannot escape the managed root")
    candidates = [path.parent.parent for path in installation.rglob("bin/java") if path.is_file()]
    if len(candidates) != 1:
        parser.error("Expected exactly one installed JDK")
    home = candidates[0]
    version = subprocess.check_output([str(home / "bin/java"), "-version"], stderr=subprocess.STDOUT, text=True)
    if lock["jdkVersion"] not in version or "Temurin" not in version:
        parser.error("Installed JDK vendor/version does not match lock")
    print(home)


if __name__ == "__main__":
    main()
