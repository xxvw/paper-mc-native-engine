# No-op persisted terrain smoke

Use Python 3.11+ and install the pinned packages in `requirements.txt` into an
isolated virtual environment. Run the reader tests before launching servers:

```sh
python -m unittest discover -s compat-tests -p 'test_*.py' -v
python compat-tests/smoke.py --java /path/to/pinned/java \
  --baseline /path/to/official-paper.jar --fork /path/to/fork.jar --accept-eula
```

`--accept-eula` asserts the operator has accepted the Minecraft EULA. The
harness creates only fresh managed directories under `.local/compat-runs`,
binds to loopback, retains all evidence/worlds and performs no deletion. It
starts baseline and fork sequentially in independent JVMs with the same seed,
properties, no plugins and deterministic command/workset ordering. Gameplay
ticks are frozen before requesting fixtures so fluid/block updates do not
depend on different JVM startup or generation timings. It verifies
gamerule command feedback, waits for loaded chunks and clean save/shutdown.
The pinned Paper worker and I/O thread counts are each set to one to constrain
generation ordering. This fixture policy is not a performance benchmark.

Fixtures: six chunk coordinates covering negative coordinates and positive
and negative region boundaries in overworld, nether and end; default seed 0.
The reader requires FULL persisted chunks and canonicalizes block-state and
biome palettes into voxel values. All other chunk fields are compared,
including heightmaps, block entities, structure data and saved tick lists.
`LastUpdate` (save-time game tick) and `InhabitedTime` (runtime exposure) are
excluded because sequential JVM runs have different elapsed tick counts.
Region timestamps are not generated-world contents and are not compared.

This smoke does not qualify entity behavior, representative plugin/player
load, native execution or the complete Phase 3 seed/fixture/reload matrix.
Unsupported compression/data encodings fail rather than silently skip data.
`result.json` records artifact hashes, workset, exclusions and the first field
of each mismatch. A mismatch is a failing gate, never an accepted new golden.
