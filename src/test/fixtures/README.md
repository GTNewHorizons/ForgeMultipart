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

### `ReferenceScalaItemMultiPart`

`scala/codechicken/multipart/compat/ReferenceScalaItemMultiPart.scala` mixes in `TItemMultiPart` without overriding
anything it supplies, so its compiled forwarders call all three `TItemMultiPart$class` statics: `getHitDepth`,
`onItemUse` and `$init$`. ProjRed depends on them.

Its `newPart` records each attempted position and returns null, which is the short-circuit that keeps placement from
touching the world, so the fixture can be driven with null stack, player and world.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `7843f13`
(SHA-256 `c003d02407d901271f42e182b5036ed3c295c2acbf416603513d2bec5b8210d0`). Class-file SHA-256 is
`0b9d9dba78a25b0c0867fe545bc6c6400be272c418d755ef72f46dde6294c75e`, stored as
`src/test/resources/compat/ReferenceScalaItemMultiPart.class.b64`.

### `ReferenceScalaEdgePart`

`scala/codechicken/multipart/compat/ReferenceScalaEdgePart.scala` mixes in `TEdgePart` without overriding what it
supplies, so its compiled forwarder calls `TEdgePart$class.conductsRedstone` and its constructor calls
`TEdgePart$class.$init$`. OpenComputers links against `$init$`.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `f2f1870`
(SHA-256 `cec40ee4c94f89fadf29e3f4860c68ca7d55caf12634d950a3eb5ecfb272a01b`). Class-file SHA-256 is
`76e4d3875ae9246ed2277f0470b22f596885282386ed179a42e5f571711ae5e0`, stored as
`src/test/resources/compat/ReferenceScalaEdgePart.class.b64`.

### `ReferenceScalaSaw`

`scala/codechicken/multipart/compat/ReferenceScalaSaw.scala` mixes in `Saw` without overriding
`getMaxCuttingStrength`, so its compiled forwarder calls `Saw$class.getMaxCuttingStrength` and its constructor calls
`Saw$class.$init$`. ProjRed links against both.

Its `getCuttingStrength` reports 7 only when handed a stack that actually wraps the saw itself, so the test proves the
bridge built the stack rather than only that it linked.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `0de7283`
(SHA-256 `e931927e83134f57197aba082345ea96909291469d01a4f146836418a76bb3d1`). Class-file SHA-256 is
`a03e8eb8566ae830829d59a379c67f355519bc82de24265baabd84720f068ce1`, stored as
`src/test/resources/compat/ReferenceScalaSaw.class.b64`.

### `ReferenceScalaMicroMaterialConsumer`

`scala/codechicken/multipart/compat/ReferenceScalaMicroMaterialConsumer.scala` reads
`MicroMaterialRegistry$.MODULE$` and calls the instance methods ProjRed links against (`getMaterial(int)`,
`materialID`, `materialName`), plus the `scala.Tuple2` array `getIdMap` that extrautilities links against. It reads
`_1` and `_2` off the raw tuples rather than going through accessors, so the array element type is exercised too.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `52ff8ff`
(SHA-256 `122d89cf0a070cfd2325bbbcf4bb4550d6130bb7da1ffba1a3b2e2efea6ec7e5`). Class-file SHA-256 is
`e66be14e6422277cc9567acf8e96a26e419abf5ec6582085c8c6e32cc60aa599`, stored as
`src/test/resources/compat/ReferenceScalaMicroMaterialConsumer.class.b64`.

### `ReferenceScalaItemMicroPartConsumer`

`scala/codechicken/multipart/compat/ReferenceScalaItemMicroPartConsumer.scala` reads `ItemMicroPart$.MODULE$` and
calls the four companion methods ProjRed links against: both two-argument `create` overloads, `getMaterial`, and
`getMaterialID`.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `fdfd776`
(SHA-256 `c3bb8a18ab961a24989d895ade59dc358581c82eec8ecf925256e164eae0ab03`). Class-file SHA-256 is
`967c6c2041c43896f624b60ce74c424575a303eed79c29c8fe60a6344f97dc71`, stored as
`src/test/resources/compat/ReferenceScalaItemMicroPartConsumer.class.b64`.

### `ReferenceScalaMultipartSaveLoadConsumer`

`scala/codechicken/multipart/compat/ReferenceScalaMultipartSaveLoadConsumer.scala` reads
`MultipartSaveLoad$.MODULE$` and calls the `loadingWorld` getter and setter that ProjRed links against.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the reference dev jar built at `471c767`
(SHA-256 `1f571035283b5ca9120bf817f00ce961ebcbae258d901fb8ee9147636f71a481`). Class-file SHA-256 is
`27543f97e0973f042c3867c1aa0ae5598324c60c895c8d9a7ea5a6cea2f5ee70`, stored as
`src/functionalTest/resources/compat/ReferenceScalaMultipartSaveLoadConsumer.class.b64`.

### `ReferenceScalaBlockMicroMaterialConsumer`

`scala/codechicken/multipart/compat/ReferenceScalaBlockMicroMaterialConsumer.scala` calls the
`BlockMicroMaterial$.MODULE$` key/default methods and the `MaterialRenderHelper$.MODULE$` pass accessors used by
downstream Scala code.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched classes at `1939159`
(reference dev-jar SHA-256 `3dfb365ee96603dec74e22787ba17e324792410265b85afd103b07aa57ddf8f6`). Class-file
SHA-256 is `710f925a5b4bd44e337672b6c4a3c7a0dddf0c5e31287989c4774c34cf628fa8`, stored as
`src/test/resources/compat/ReferenceScalaBlockMicroMaterialConsumer.class.b64`.

### `ReferenceScalaMultipartGeneratorConsumer`

`scala/codechicken/multipart/compat/ReferenceScalaMultipartGeneratorConsumer.scala` calls
`MultipartGenerator$.MODULE$.generateCompositeTile(TileEntity, scala.collection.Iterable, boolean)` and the
one-argument `registerPassThroughInterface` companion method. GuideNH and Schematica depend on the former through
reflection; shipping Scala consumers link directly to the companion registration path.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched classes at `2f4972e`
(reference dev-jar SHA-256 `69c0723afd70ab6208b97e5fab12062bfb28a941dfae12575c5d61274102ef6f`). Class-file
SHA-256 is `b0f540b063bd6fffcc2a1462d72de1f31b4c8a68caa92ac9298dfdf18a9eb24f`, stored as
`src/functionalTest/resources/compat/ReferenceScalaMultipartGeneratorConsumer.class.b64`.

### `ReferenceScalaMicroblockTraits`

`scala/codechicken/multipart/compat/ReferenceScalaMicroblockTraits.scala` is a concrete `Microblock` with
`CommonMicroblockClient`. Its frozen Scala forwarders exercise all three `MicroblockTraits.scala` helper classes,
initialization, virtual slot/material/class dispatch, particle callbacks and both common rendering branches.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the unchanged dev jar at `f5d449d`
(SHA-256 `8061ae3ebb260b619a283519b1752a32e38126ccff537e0fbcdb33c1a736fde3`). Source SHA-256 is
`8a8754632121c8bac8c0c61bf2facbd317462c1c4e43b82127511908ffbcbd98`; class-file SHA-256 is
`dccc96505689e4c6084e11133812b56b39ae90b274762327815a22ef9a1ce75c`, stored as
`src/test/resources/compat/ReferenceScalaMicroblockTraits.class.b64`. Do not regenerate it against the Java helper.
