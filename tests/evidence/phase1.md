# Phase 1 execution evidence

Status: PASS for local Phase 1 acceptance. Tracking issue: #2.

## Implementation

The maintained server build manifest changes Brand-Name only; Paper's Brand-Id
is retained. The pinned server exposes this same name through its existing
Bukkit/version and vanilla BrandPayload paths. No Minecraft generated source,
generation algorithm or client protocol override is modified. Two maintained
Paper command files add startup initialization, `/mcne` registration and a
truthful pinned upstream provenance line in `/version`.

Startup-only YAML configuration defaults native off; invalid settings fail
closed. Explicit native requests retain JAVA_BASELINE with a reason because
the Phase 4 bridge does not yet exist. Operator-default permissions and
per-sender monotonic one-second rate limits protect diagnostics. Benchmarks
and metrics commands remain Phase 2 work (REQ-006 is not fully complete).

## Executed validation

Gradle used the locked Temurin 25.0.4.1+1 JDK.

| Command/test | Result |
|---|---|
| `./gradlew tasks --all` | PASS; inherited tasks rediscovered |
| `./gradlew createPaperclipJar` | PASS |
| `./gradlew :paper-server:test --tests '*McneTestSuite'` | PASS; 8 tests (configuration, permission/completion/rate limit, branding/provenance) |
| `python3 -m unittest discover -s tests -p 'test_baseline.py' -v` | PASS; 6 tests including immutable baseline with declared maintained delta |
| Snapshot canonicalization suite | PASS; 4 tests |
| Phase 0 semantic harness on branded build | PASS; 18/18 chunks; `phase1-smoke.json` |
| `compat-tests/phase1.py --java <locked-java> --fork <built-jar> --accept-eula` | PASS; disposable Bukkit API fixture, console version/ver/status/native info, explicit-native fallback, clean shutdown |
| Direct local player connection | PASS; online-mode authenticated login on loopback preview |
| Vanilla client F3 brand | PASS; user explicitly reported `paper-chunk-native-engine` displayed after connecting |

Plugin run `phase1-api-woy94oco` checked Bukkit name/version/API, builtin command
availability and operator-default permission registration. It is a minimal
plugin smoke, not certification of every third-party plugin. Its result is
published as `phase1-api.json`; disposable logs/worlds remain local.

The preview on localhost:25565 is deliberately kept running for user access.
It serves a copied development artifact built while source changes were
uncommitted; its inherited DEV manifest commit predates the Phase 1 commit.
`run/mcne-preview/build-provenance.json` records the actual artifact checksum,
dirty-build status and immutable baseline separately. It is not a release.

## Remaining gates

Phase 2 benchmark/metrics implementation, full Phase 3 differential fixtures
and profiling, native bridge, six platform qualification and release/license
gates remain open. Linux CI is reported separately from local acceptance.
No performance claim or native release is made.
