# paper-chunk-native-engine

A minimal Paper fork for measured, opt-in C11/JNI chunk acceleration while
preserving the generated-world semantics of the pinned, unmodified Paper.

Repository: https://github.com/xxvw/paper-mc-native-engine

## Current state

Phase 0 is in progress. The server source and generation logic are unchanged.
There is no working native engine, custom branding or `/mcne` command yet.
`JAVA_BASELINE` is the only runnable path. No performance improvement is claimed.

The immutable baseline is recorded in `upstream.lock.json`. The inherited
Paper build scripts, wrapper, patch workflow and licenses are preserved.
Local requirement documents under `docs/` are deliberately not published.

## Reproduce the baseline build

Install Eclipse Temurin **25.0.4.1+1** for your platform and set `JAVA_HOME`.
Verify vendor/version with `"$JAVA_HOME/bin/java" -version`. Use the inherited
Gradle wrapper; do not substitute a system Gradle or a different JDK.

```sh
git clone git@github.com:xxvw/paper-mc-native-engine.git
cd paper-mc-native-engine
git config core.hooksPath scripts/mcne
python3 scripts/mcne/verify_upstream.py
./gradlew tasks --all
./gradlew applyPatches
./gradlew createPaperclipJar
```

Output: `paper-server/build/libs/paper-paperclip-26.3.local-SNAPSHOT.jar`.
Do not set the upstream `BUILD_NUMBER` environment variable for fork builds:
the fork must not claim to be an official Paper build. Record the baseline
build number separately from fork provenance.

The download URLs/checksums in the lock describe the official untouched
baseline and the development-host JDK archive. Verify downloads with:

```sh
python3 scripts/mcne/verify_upstream.py --baseline /path/to/paper.jar \
  --jdk-archive /path/to/locked-macos-aarch64-jdk.tar.gz
```

For patch changes, follow the pinned `CONTRIBUTING.md`. Use the discovered
`fixupSourcePatches`/`rebuildPatches` tasks; generated Minecraft sources are
not maintained directly. CI never publishes to Paper's Fill or Maven servers.

## Acceptance and distribution

See `tests/phase0.md`, `tests/traceability.md` and `tests/evidence/` for actual
commands, results and pending gates. All server tests use disposable managed
directories and separate baseline/fork JVMs. Minecraft EULA acceptance is
required before launching test servers.

The fork inherits GPLv3 obligations; see `LICENSE.md`, `licenses/` and
`THIRD_PARTY_NOTICES.md`. Native linkage/licensing, six platform runtime tests,
full differential tests and statistically supported performance results are
release gates. A compiled JAR is not a qualified release.
