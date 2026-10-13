# Phase 0 execution evidence

Status: IN PROGRESS. No-op fork build passed; launch and semantic equivalence
must pass before proceeding to Phase 1. Tracking issue: #1.

## Verified baseline

- Official Downloads Service: Minecraft/Paper API 26.3, STABLE build 181.
- GitHub source commit: `cc7ee9aebdfc9e4acc3ef8f38c35944872fd1f87`.
- Downloaded official JAR SHA256:
  `cada5af7f14363bc8545c0e973130de9431f6dc678dc612759187695027c27d9`.
- Downloaded Temurin 25.0.4.1+1 macOS/aarch64 archive SHA256:
  `61979887f7506a24a57439ff99adb8b3a7fc89977d9cfe3b8984f58a981b7b9d`.
- JDK runtime reports Eclipse Adoptium Temurin 25.0.4.1+1-LTS.
- Gradle wrapper 9.8.0; upstream build-file hashes are recorded in the lock.
- Local platform: macOS/aarch64. Other platform execution is UNVERIFIED.

## Executed commands

`JAVA_HOME` was set to the checksum-verified extracted JDK for Gradle commands.

| Command | Result |
|---|---|
| `./gradlew tasks --all` | PASS; discovered actual upstream task names |
| `./gradlew applyPatches` | PASS; 939 source, 3 resource and 105 mache patches applied |
| `./gradlew createPaperclipJar` | PASS; 13 actionable tasks, 10 executed |
| `python3 scripts/mcne/verify_upstream.py --baseline <downloaded-jar> --jdk-archive <downloaded-jdk>` | PASS; both downloads and maintained build files verified |
| `python3 -m unittest discover -s tests -p 'test_baseline.py' -v` | PASS; 5 tests |
| `cmake -S native -B native/build` | PASS; CMake 4.4.3, AppleClang 21.0.0.21000101; tooling skeleton only |
| Pinned `javac` on both MCNE package scaffolds | PASS; not connected to Paper runtime |
| `git diff --name-only <upstream-sha> -- paper-api paper-server build-data` | Empty; server/API/worldgen source unchanged |

Local logs are retained under `.local/evidence/`; worlds and binaries are not
published. No benchmark, performance result, full differential result, native
binary, client branding verification or release is claimed.

## Pending gates

Independent server startup, persisted semantic smoke comparison, full Phase 3
fixture suite, client F3 integration, native fallback, all six platform runtime
tests, performance qualification and distribution/license review remain open.
The original requirement documents remain local and excluded from Git.
