# Phase 0 acceptance

Tracks REQ-001, REQ-002, REQ-004, REQ-005, CI-001 and LIC-001.

1. Verify the official STABLE build, source commit and downloadable server.
2. Run `python3 scripts/mcne/verify_upstream.py` with the downloaded baseline
   and JDK archive to check checksums and the maintained upstream build files.
3. Use pinned JDK 25; discover tasks with `./gradlew tasks --all`.
4. Run the discovered upstream patch application and Paperclip build tasks.
5. Launch the official baseline and unchanged fork in independent disposable
   JVM environments. Match seed, settings, datapacks, workset and generation
   order; stop cleanly after save/flush.
6. Compare normalized persisted chunk semantics, including block states,
   biomes, heightmaps, block entities and structures. Record exclusions for
   runtime metadata explicitly. A difference blocks Phase 0 completion.

Only steps with recorded execution evidence may be marked PASS. Build success
does not establish chunk equivalence. All later phase gates remain pending.
