# Phase 2 managed-storage foundation

Tracking issue: #3. The immutable Paper/JDK pin is unchanged.

`LevelStorageAccess.getDimensionPath` resolves only explicitly leased
`mcne:bench_<run-id>` keys to owned benchmark directories. Unregistered keys
keep Paper's ordinary path. The maintained source patch was applied through
`./gradlew applyPatches` and regenerated with `./gradlew rebuildSourcePatches`.
Generated sources were not directly edited.

Focused JVM safety tests cover inclusive/unique worksets, coordinate bounds,
unsafe IDs, unknown roots, symlinks, duplicate leases, forged ownership and
ordinary-dimension routing. Configuration tests cover quotas and unsafe flags.
The full development MCNE suite passed after these changes; lifecycle tests
are added with the subsequent command integration commit.

Isolated development run `phase2-0glrq_sv` validated generate 9/9, flush/save,
unload/recreate and reload 9/9. Region files stayed below `bench-worlds/<id>/`;
no matching directory was created below the ordinary world's dimensions.
Console-only TP was rejected. Cancellation, exclusivity, schema/redaction,
INCONCLUSIVE comparison requests and shutdown partial reports passed.
The integration used reduced before/after windows and cooldown explicitly;
it is functionality evidence, not performance qualification.

The default lifecycle remains JAVA_BASELINE. Native and authoritative A/B,
internal stage profiling and full platform qualification remain later gates.
