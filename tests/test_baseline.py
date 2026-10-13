"""Regression checks for lock validation and local requirement exclusions."""
import hashlib
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/mcne/verify_upstream.py"
spec = importlib.util.spec_from_file_location("verify_upstream", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class BaselineVerificationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-q")
        self.git("config", "user.name", "Fixture")
        self.git("config", "user.email", "fixture@example.invalid")
        (self.root / "wrapper").write_bytes(b"immutable")
        self.git("add", "wrapper")
        self.git("commit", "-qm", "Fixture baseline")
        self.lock = {"paperChannel": "STABLE", "paperSourceCommit": self.git("rev-parse", "HEAD").strip(),
                     "upstreamFilesSha256": {"wrapper": hashlib.sha256(b"immutable").hexdigest()},
                     "paperDownloadSha256": hashlib.sha256(b"server").hexdigest()}

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, text=True)

    def test_accepts_unchanged_stable_baseline(self):
        module.verify(self.lock, self.root)

    def test_rejects_changed_upstream_tooling(self):
        (self.root / "wrapper").write_bytes(b"modified")
        with self.assertRaisesRegex(ValueError, "build file changed"):
            module.verify(self.lock, self.root)

    def test_rejects_tracked_requirements(self):
        (self.root / "docs").mkdir()
        (self.root / "docs/requirements.md").write_text("local")
        self.git("add", "docs")
        with self.assertRaisesRegex(ValueError, "must not be tracked"):
            module.verify(self.lock, self.root)

    def test_rejects_wrong_download(self):
        artifact = self.root / "paper.jar"
        artifact.write_bytes(b"wrong")
        with self.assertRaisesRegex(ValueError, "download SHA256 mismatch"):
            module.verify(self.lock, self.root, baseline=artifact)

    def test_rejects_nonstable_build(self):
        self.lock["paperChannel"] = "BETA"
        with self.assertRaisesRegex(ValueError, "must be STABLE"):
            module.verify(self.lock, self.root)


if __name__ == "__main__":
    unittest.main()
