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

### `ReferenceScalaNormalOcclusion`

`scala/codechicken/multipart/compat/ReferenceScalaNormalOcclusion.scala` mixes in `TNormalOcclusion` without overriding
`occlusionTest`, so the compiled class carries the generated
`codechicken$multipart$TNormalOcclusion$$super$occlusionTest` accessor and forwards `occlusionTest` to
`TNormalOcclusion$class`. Invoking it therefore covers the whole bridge round trip: forwarder, `$class`, the singleton
`NormalOcclusionTest$.MODULE$`, and the callback through the accessor.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `cd6420f`
(SHA-256 `da0a35a968905203187edc5a1f26018305cf146114426ed7b468080b40c95771`). Class-file SHA-256 is
`0ce8ddd700df8ec859325133a1a683de1aceaabd67cb4c0d1867e6cb907e5790`, stored as
`src/test/resources/compat/ReferenceScalaNormalOcclusion.class.b64`.

### `ReferenceScalaFacePart`

`scala/codechicken/multipart/compat/ReferenceScalaFacePart.scala` mixes in `TFacePart` without overriding either
member it supplies, so its compiled forwarders call all three `TFacePart$class` statics: `solid`,
`redstoneConductionMap` and `$init$`. OpenComputers, ProjRed and ProjectBlue depend on those descriptors.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `a1bf3c8`
(SHA-256 `1cb57d8728f06b3d283b0bca8ce9f92a58f4c2b8cc835fa4f7ee3de6338c27ec`). Class-file SHA-256 is
`a95dfa131f57e6e6efdba25e989154899d9b1f9a5cbff7a1e6b4374ec9c2b0b2`, stored as
`src/test/resources/compat/ReferenceScalaFacePart.class.b64`.

### `ReferenceScalaIconHitEffects`

`scala/codechicken/multipart/compat/ReferenceScalaIconHitEffects.scala` mixes in `TIconHitEffects` without overriding
anything it or `JIconHitEffects` supplies, so its compiled forwarders call all five statics across both bridges:
`TIconHitEffects$class.addHitEffects`, `.addDestroyEffects`, `.$init$`, and `JIconHitEffects$class.getBreakingIcon`,
`.$init$`. ForgeRelocationFMP, OpenComputers and ProjRed depend on all of them.

It records the last side passed to `getBrokenIcon` so the test can prove delegation actually happened rather than only
that the class loaded.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `57130a6`
(SHA-256 `97469dae5152f5040db6106022e8176a1b84814e700c7dfca89f1364e11cf6af`). Class-file SHA-256 is
`80255098990ee61d2c96f70c7020b3bede4f2654786c0c8a3b7618d5060923c7`, stored as
`src/test/resources/compat/ReferenceScalaIconHitEffects.class.b64`.
