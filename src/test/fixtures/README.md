# Compatibility fixtures

Files below this directory are frozen baselines, not a Gradle source set. They must be reviewed when they change
rather than regenerated automatically, otherwise an accidental change silently redefines the baseline.

## `abi/`

`gtnh-daily-678-consumers.txt` is the constant-pool scan of every mod jar in GTNH daily `2026-08-14+678`, listing
every downstream reference into `codechicken/multipart` and `codechicken/microblock`. Regenerate with
`tools/AbiScan.java` and diff against this file at every public-API phase; a member present here but absent from the
port is a linkage break in a shipping mod. See `JAVA_MIGRATION_ABI_INVENTORY.md` for the analysis.

## Precompiled binary consumers

When a port changes descriptors that the ABI inventory shows are load-bearing, freeze a consumer compiled against the
reference dev jar here as a class file and load it directly in a test. Recompiling a consumer against the port would
hide exactly the linkage failure the fixture exists to catch.

`ReferenceScalaPartialOcclusion` was removed together with the `JPartialOcclusion$class` bridge once the inventory
showed no downstream consumer of that helper. The trait helpers that do need this treatment are listed in
`JAVA_MIGRATION_ABI_INVENTORY.md`.

### `ReferenceScalaCuboidPart`

`scala/codechicken/multipart/compat/ReferenceScalaCuboidPart.scala` mixes in `TCuboidPart` without overriding anything
the trait supplies, so its compiled forwarders call all four `TCuboidPart$class` statics: `$init$`, `getSubParts`,
`getCollisionBoxes` and `drawBreaking`. Four shipping mods depend on those descriptors.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `246daff`
(SHA-256 `0ca7103905c8e2ada010bfc0362a257f19a67f7af4e9fb9dd00a7379c274f69a`). Class-file SHA-256 is
`3aba21aa01c4d689e2e1fb33b872424c7e7b5f4a8254abf2c86e9b11bb9fabe9`, stored as
`src/test/resources/compat/ReferenceScalaCuboidPart.class.b64`.

`TCuboidPartBinaryCompatibilityTest` exercises `$init$`, `getSubParts` and `getCollisionBoxes`. The `drawBreaking`
forwarder is present in the fixture but cannot be invoked without a client render context, so its bridge descriptor is
covered by the ABI baseline rather than by execution.
