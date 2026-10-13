# Codex agent instructions — mc-native-chunk-engine

> Read `docs/00-PROJECT-OVERVIEW.md`, `docs/01-REQUIREMENTS.md`, `docs/12-ROADMAP.md` and `docs/DECISIONS.md` before edits. This repository initially contains **specifications**, not working implementation.

## Non-negotiable rules
1. Maintain Minecraft/Paper-compatible world generation. Baseline is **the pinned unmodified Paper source/build**, at the same Minecraft version, world seed, generator config, datapacks and runtime conditions. Do not enable an optimization unless equivalence tests pass.
2. Work on a **minimal Paper fork** using upstream-supported patch workflows; never silently modify generated/decompiled sources outside maintained patch flows. Keep independent features as small, reviewable commits.
3. Always ship two runtime paths: `JAVA_BASELINE` (default initially) and `NATIVE` (opt-in until gates pass). Native unavailable, unsupported architecture, ABI mismatch or recoverable error => safe baseline mode. Native segfaults cannot be caught by ordinary Java try/catch. Do not hot-swap implementation inside an in-progress generation task.
4. Use C11 (or compatible toolchain) plus JNI; no Java object graph crossing JNI, no uncontrolled native memory, no CPU-specific instruction without a guarded dispatch.
5. Benchmark reproducibly and never claim faster without paired comparable runs and confidence intervals. No benchmark may delete/change a non-dedicated world. Run destructive operations only in managed, disposable benchmark directories.
6. Preserve API/plugin behavior; `/mcne` requires authorization and is rate-limited. Teleport benchmarks require player consent through command invocation; no automatic teleport of others.
7. Required branding: `paper-chunk-native-engine` in `/version` (`/ver` alias as available) and the F3 brand. Include integration tests for both; preserve truthful Paper/Minecraft/build provenance elsewhere.
8. Do not hardcode an unverified newest Paper version or unsupported build identifier. Resolve a STABLE release, pin exact upstream Git SHA/build/Java/toolchain in `upstream.lock.json`, and write provenance to reports. If lookup fails, stop with an actionable message, not an invented version.
9. Treat `docs/14-LICENSING.md` as a release blocker: own standalone C/Java code may be MIT subject to provenance review, but forked Paper distribution has upstream GPLv3 obligations. Do not relabel upstream/fork JAR as MIT.
10. Do not invent measurements, tests, release artifacts, API hook locations, or patch compatibility. Mark unresolved discoveries as TODO and validate against pinned source.

## Workflow per implementation phase
- Read the phase in `docs/12-ROADMAP.md`; create a narrow issue/PR with explicit IDs (`REQ-*`, `BENCH-*`, etc.).
- Add focused code, tests and documentation; run `./gradlew tasks` to discover exact upstream tasks before invoking them.
- Run unit, integration, differential and platform tests as applicable. Record actual commands, results and known failures in PR.
- Do not turn on production native by default until acceptance gates are satisfied.

## Intended packages/modules
- Java root `io.github.xxvw.mcnativechunkengine` (e.g. `.benchmark`, `.nativebridge`, `.metrics`, `.brand`, `.config`).
- C library `mc_native_chunk_engine`, C symbols are internal except the versioned public bridge.
- GitHub repository `https://github.com/xxvw/mc-native-chunk-engine`.

## Delivery order
1. Baseline pinned fork + branding + zero-overhead disabled hook.
2. `/mcne` command, safe benchmark harness, metric pipeline and reports, baseline tests.
3. Profiler-driven hotspot selection and differential golden reference.
4. Native proof-of-concept behind explicit flag + cross-platform build.
5. Native optimization, CI release gates and license audit.

## Definition of done
Every acceptance criterion in `docs/01-REQUIREMENTS.md` is linked to automated evidence or a documented manual test. Release is blocked by differential mismatch, missing platform binary, missing license notices/source obligations, unexplained performance regression, or unsafe benchmark deletion.
