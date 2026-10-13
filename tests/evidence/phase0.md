# Phase 0 execution evidence

Status: PASS for local Phase 0 acceptance. Tracking issue: #1.

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

## CI setup correction

GitHub run `38442024172` failed before build: `actions/setup-java` rejected
`25.0.4.1+1` as invalid SemVer. The version was not relaxed. The corrected CI
uses `scripts/mcne/install_jdk.py` and a locked Temurin archive URL/SHA256.
The installer was executed successfully on macOS/aarch64 and confirmed the
exact vendor/version. Linux CI must execute the correction before PASS.

## Independent startup and semantic smoke

Command (paths below refer to managed, ignored development downloads):

```sh
.tools/compat-venv/bin/python compat-tests/smoke.py \
  --java .tools/jdk-25.0.4.1+1/Contents/Home/bin/java \
  --baseline .tools/downloads/paper-26.3-181.jar \
  --fork paper-server/build/libs/paper-paperclip-26.3.local-SNAPSHOT.jar \
  --accept-eula
```

Run `phase0-20_rm2am`: PASS, 18/18 persisted FULL chunks matched. Separate
baseline/fork JVMs started successfully and exited cleanly after save/flush.
Seed 0; overworld/nether/end; coordinates `(0,0)`, `(-1,-1)`, `(1,-1)`,
`(31,31)`, `(32,32)`, `(-32,-32)`. Canonical hashes and artifact checksums are
in `phase0-smoke.json`. `compat-tests/test_snapshot.py`: 4/4 tests passed.

The exact pinned Paper uses `world/dimensions/minecraft/<dimension>/region`,
not older Paper world layouts. Its verified gamerule names are
`random_tick_speed`, `spawn_mobs` and `advance_time`. Gameplay is frozen after
startup; worker and I/O counts are both one to constrain generation ordering.
This smoke is not a representative workload/performance claim.

Earlier diagnostic runs were retained: one used obsolete gamerule names and
was interrupted; one reader assumed the old storage layout; default-worker
comparisons showed terrain/light/tick differences; sending freeze before
startup completed caused an upstream command-source NPE. These runs did not
pass. The harness now verifies feedback and source-confirmed configuration,
freezes only after startup, and does not discard terrain/light/tick mismatches.
Only `LastUpdate` and `InhabitedTime` are excluded as runtime counters.

## Remaining gates

Full Phase 3 fixture suite, client F3 integration, native fallback, all six platform runtime
tests, performance qualification and distribution/license review remain open.
The original requirement documents remain local and excluded from Git.
