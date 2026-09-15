# Compatibility fixtures

Files below this directory are frozen baselines, not a Gradle source set. They must be reviewed when they change
rather than regenerated automatically, otherwise an accidental change silently redefines the baseline.

## `abi/`

`gtnh-daily-678-consumers.txt` is the constant-pool scan of every mod jar in GTNH daily `2026-08-14+678`, listing
every downstream reference into `codechicken/multipart` and `codechicken/microblock`. Regenerate with
`tools/AbiScan.java` and diff against this file at every public-API phase; a member present here but absent from the
port is a linkage break in a shipping mod. See `docs/migration/COMPATIBILITY.md` for the analysis.

## Precompiled binary consumers

When a port changes descriptors that the ABI inventory shows are load-bearing, freeze a consumer compiled against the
reference dev jar here as a class file and load it directly in a test. Recompiling a consumer against the port would
hide exactly the linkage failure the fixture exists to catch.

`ReferenceScalaPartialOcclusion` was removed together with the `JPartialOcclusion$class` bridge once the inventory
showed no downstream consumer of that helper. The trait helpers that do need this treatment are listed in
`docs/migration/COMPATIBILITY.md`.

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

### `ReferenceScalaFaceMicroblockClient`

`scala/codechicken/multipart/compat/ReferenceScalaFaceMicroblockClient.scala` freezes a concrete face-client
implementor and its `FaceMicroblockClient$class.render` forwarder. Tests execute exact per-face rendering for all
six slots and 64 masks, negative passes, virtual transparency/slot dispatch and material caching across both draws.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the unchanged dev jar at `d89aaea`
(SHA-256 `c97d389524bab62fabe6584fcaace5b5fe2d59db3e575a60605fb083fc46856f`). Source SHA-256 is
`3848f0ba68344b103614e6aaa85dfe300a1b3c41b94cb171224b411a46ecdf3b`; class-file SHA-256 is
`96283797af5f81137e6fa354fbce5886e0de4f00655a6ab89a21de94bfadcbde`, stored as
`src/test/resources/compat/ReferenceScalaFaceMicroblockClient.class.b64`. Do not regenerate it against the port.

### `ReferenceScalaCornerMicroblock`

`scala/codechicken/multipart/compat/ReferenceScalaCornerMicroblock.scala` freezes a concrete corner implementor
whose forwarders call `CornerMicroblock$class`. Observable shape accessors pin virtual dispatch, byte truncation,
out-of-range input behavior and the seven-slot offset without requiring Forge initialization.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the unchanged dev jar at `cfde822`
(SHA-256 `0c01bf316fe3706155dda05f6cb56a084658e5133b7590503e69a263a3fdfb85`). Source SHA-256 is
`6c0ff48c4bc6ff5508a429d53f13b718062173cc4a117095c192aed9cf0a0311`; class-file SHA-256 is
`5a6c3cf1ce593eb189af59f6db26a04d85902972073de83a77d8f0d920642cfa`, stored as
`src/test/resources/compat/ReferenceScalaCornerMicroblock.class.b64`. Do not regenerate it against the port.

### `ReferenceScalaEdgeMicroblock`

`scala/codechicken/multipart/compat/ReferenceScalaEdgeMicroblock.scala` freezes a concrete edge implementor
whose forwarders call `EdgeMicroblock$class`. Observable shape accessors pin virtual dispatch, byte truncation,
out-of-range input behavior and the fifteen-slot offset; tests also exercise inherited `TEdgePart` conduction.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the unchanged dev jar at `33e0bc2`
(SHA-256 `764bff78657bc14ab918c267e836702982f7a72d64e998357709df13bce90058`). Source SHA-256 is
`6524a023b8015e6ecc75aaab9dafa09bb353e66ccdb72e7d76a3469288ba5952`; class-file SHA-256 is
`2b51d17fb89999fd0324e4ea53843596539c5885c2b2eb0a476926e18446b587`, stored as
`src/test/resources/compat/ReferenceScalaEdgeMicroblock.class.b64`. Do not regenerate it against the port.

### `ReferenceScalaPostMicroblock`

`scala/codechicken/multipart/compat/ReferenceScalaPostMicroblock.scala` freezes post forwarders with a real superclass
predecessor and a face candidate. Tests observe special-case ordering, the synthetic super accessor, virtual shape,
bounds and list dispatch, list mutation failures, and malformed-candidate errors.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the unchanged dev jar at `7c4c87e`
(SHA-256 `c9fcc62deebf198f3aa1de1799d3a58a31cae9fa92f1b5dd129a84b93eaab73d`). Source SHA-256 is
`d22d82ab5e53074022d305bb9537a5bae77804cdc28b0bf46f58686f47e2b62d`. Frozen class files under
`src/test/resources/compat/` have these SHA-256s; do not regenerate them against the port:

| Class | SHA-256 |
| --- | --- |
| `ReferencePostMicroblockBase` | `a6bed1c01fd74be061edfa287b724679c4cdbcb9c550cc3a3304ebda6973c7ac` |
| `ReferenceScalaPostMicroblock` | `ceafadad14da1f749ec60af6609d1b2eb7afa0666425fff25271dee1e46a7bc2` |
| `ReferencePostFace` | `066037dc6000fbcf2404583ff4ba8a9048b140beb0e1c27700564284bc4d8c06` |

### `ReferenceScalaPostMicroblockClient`

`scala/codechicken/multipart/compat/ReferenceScalaPostMicroblockClient.scala` freezes the client forwarders and
lifecycle superclass predecessors. Tests observe render dispatch, material reuse, repeated virtual getters,
equality receiver/order, collection `foreach`, split/reset behavior and super-call failure ordering.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `1c7dd14`
(SHA-256 `6c80983b6ba49bf7bc6c30248b9af4bdb0954bb1091ebd0e2ab4d823b994697b`). Source SHA-256 is
`6a04c03977858ce01ebd4951d105dba8be926977d0c5892aeec469c17f89ac39`. Frozen class files under
`src/test/resources/compat/` have these SHA-256s; do not regenerate against the port:

| Class | SHA-256 |
| --- | --- |
| `ReferencePostClientBase` | `88a996255cae7f981f4386eba5bc1d4b5308c2eef900d120b105ec5727536342` |
| `ReferenceScalaPostMicroblockClient` | `71afd739893a0cff7022c758cccff8a653a219c7937310df7b893b31f0ef82a4` |

### `ReferenceScalaHollowMicroblock`

`scala/codechicken/multipart/compat/ReferenceScalaHollowMicroblock.scala` freezes hollow-cover forwarders with a
real Scala trait super chain. Tests pin repeated tile/raw-shape reads, connector slot dispatch, collision geometry,
list mutability and copying, normal/partial occlusion short-circuiting and superclass failures.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `556fd51`
(SHA-256 `8bffb00300087a3f202354a836c9f3f2cf22c4cb3b145e03c8c2ac893028c311`). Source SHA-256 is
`277b708d11e37a66f311759af50b1b1e98d84238550add0e98b43793d1f55ad5`. Frozen class files under
`src/test/resources/compat/` have these SHA-256s; do not regenerate against the port:

| Class | SHA-256 |
| --- | --- |
| `ReferenceHollowMicroblockBase` | `a329f86030d4706ba07a478d36a2fe9fd6b4e2bcaac76bd92e128769cd91110e` |
| `ReferenceScalaHollowMicroblock` | `3df6e92864c984ad973a115c56495deee8fb8a8c4014dbd34786a2caf754c7f6` |

### `ReferenceScalaHollowMicroblockClient`

`scala/codechicken/multipart/compat/ReferenceScalaHollowMicroblockClient.scala` freezes the hollow client
forwarders and a real recalculation predecessor. Tests observe initialization, mask read/super order, render
selection, callback dispatch and snapshots, rim geometry/masks, breaking pipelines and highlight commands.
A child loader redirects only rendering-service calls to record their effects; trait control flow and virtual
part calls remain intact. This tests drawing commands, not actual GPU output.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `940609b`
(SHA-256 `04838fb3e100503437092fa018872d324e1765c267d5f8dc83b9831b73e66593`). Source SHA-256 is
`0dcef588bb0ee808d5ec7fba1716b2d2fd6cc6464e7650d9d29f6400d5673149`. Frozen class files under
`src/test/resources/compat/` have these SHA-256s; do not regenerate against the port:

| Class | SHA-256 |
| --- | --- |
| `ReferenceHollowClientBase` | `fe39f52cd776ded56adceb27ed011f3f72d405ee363dcb54877717e18de16450` |
| `ReferenceScalaHollowMicroblockClient` | `5b11e01e3995bc314ffa10d129da7277e386edfe08520370e6b3ee40bdd18c67` |

### `ReferenceScalaTMicroOcclusion`

`scala/codechicken/multipart/compat/ReferenceScalaTMicroOcclusion.scala` freezes a trait implementor with a real
superclass predecessor. Tests pin super/type-guard ordering, repeated slot reads, signed size overflow, material
short-circuits, own-receiver edge/corner delegation, getter failures and the two independent edge reads.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `a50ce52`
(SHA-256 `241e6315ddae2b09bb2baed7f28b545985f1cf329504288cc6de454b25c945fb`). Source SHA-256 is
`087afdc786c5ac59e70c5152f183bd0cc27e65de1672ece549870fb04f09b4ab`. Frozen class files under
`src/test/resources/compat/` have these SHA-256s; do not regenerate against the port:

| Class | SHA-256 |
| --- | --- |
| `ReferenceMicroOcclusionBase` | `f3c7c10095fd19d6e18edcee345c4ea5e22557b07595e5274e46be36d0edcb1b` |
| `ReferenceScalaTMicroOcclusion` | `d92b9b9d938e8e85b364193807e878f7eb595c099205a4ded84fb1363e11639d` |

### `ReferenceScalaTMicroOcclusionClient`

`scala/codechicken/multipart/compat/ReferenceScalaTMicroOcclusionClient.scala` freezes the client trait's
lifecycle super chain and forwarders. Tests pin argument identity, super/recalc failure order, physical-bounds
copying, clipping/mask replacement and partial state publication on failure.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `f6d1568`
(SHA-256 `6949f0c4afb22b3d68ecc42272ca1f88ac1712497556cf5c32c52b2833eb7161`). Source SHA-256 is `ed90e8f184cc4239c69bb6f700283fb25ef2225a59279eb567fc85195ab0f734`. Do not regenerate these class files against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceMicroOcclusionClientBase` | `017938f169d532f72b0ebcba31a67988b32b9116c2a6dbd558c8da2d1ac8f0bf` |
| `ReferenceScalaTMicroOcclusionClient` | `f966dc8e807327865c5ca1736f323ed33a5885f5be1dc6f1b964fa8bd3fdb21d` |

### `ReferenceScalaInterfaceNames`

`scala/codechicken/multipart/compat/ReferenceScalaInterfaceNames.scala` freezes a Scala caller of
`ClassSymbolRef.jInterfaces` and subclasses that override the path-dependent `ClassType.interfaces` and
`ClassSymbol.info` members. Suppliers expose ordered reads, changing results and failures to JVM tests.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `240a788`
(SHA-256 `03b3fdc1400d6c9a5d23cfa2a17837a20b48f7b014a02f13366b0c0dcb876e06`). Source SHA-256 is
`43f0451ae9e5540748f24ff8f4333d76e0d36040bd74cdfcd08d20a95fd68df3`. Frozen class files are stored under
`src/test/resources/compat/`; do not regenerate them against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaInterfaceNames` | `996493c5c2dbab7d71e4ecf6c5598d8bf371bd9af182f1f3ee57a8eea2c1c9e1` |
| `ReferenceScalaInterfaceNames$$anon$1` | `448774963acc6afc1641d039ec79fc82b9e825de3cee65fc44e7fc4b6a238315` |
| `ReferenceScalaInterfaceNames$$anon$2` | `ecf0adc32ca5cab232a8fcbed45acad8bda8091dd5ecdf10213e9ddfc13f8217` |

### `ReferenceScalaAppliedTypeDescriptor`

`scala/codechicken/multipart/compat/ReferenceScalaAppliedTypeDescriptor.scala` freezes a Scala caller and
TypeRefType subclass with a virtual name supplier. Tests use it to preserve array/fallback routing and repeated reads.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `d5bf16f`
(SHA-256 `822052863670e511283fa7941ea141cb2069233530524f232e07c4379e52f373`). Source SHA-256 is
`e09a7b21b88c3cb184819b93822f5128374b881dce45efced79922b0b6c8e9af`. Frozen class files are under
`src/test/resources/compat/`; do not regenerate them against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaAppliedTypeDescriptor` | `40eebab873311f3fec01dd5678e1c12c3af959b7f357aa49ed9cca59e1889b99` |
| `ReferenceScalaAppliedTypeDescriptor$$anon$1` | `0ac784d966048b6daa6715ead62360624a22d6a0927aba280c6d3908de11447e` |

### `ReferenceScalaClassSymbolFull`

`scala/codechicken/multipart/compat/ReferenceScalaClassSymbolFull.scala` freezes a Scala caller and direct
ClassSymbolRef implementation. Tests use it to preserve the old trait helper, virtual owner/name reads and failures.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `24c7723`
(SHA-256 `40c5851ad982190b2f44cd7473d8fb3ab73e0dc3d3bfa2911de06b924532248d`). Source SHA-256 is
`3744035617fbe0a1619d20ae2a44259e9aea9ea3dfa16d19a1c9b57daef0ffee`. Frozen class files are under
`src/test/resources/compat/`; do not regenerate them against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaClassSymbolFull` | `77498a049a18080a67b557b1751c7f8ab905b9f832ebd680db822e5447ea0799` |
| `ReferenceScalaClassSymbolFull$$anon$1` | `df9d4c907de922b6e917ea848e40f746ef446bf601dbda4b8d9f9761403b5f46` |

### `ReferenceScalaMethodSymbolFull`

`scala/codechicken/multipart/compat/ReferenceScalaMethodSymbolFull.scala` freezes a Scala caller of
`MethodSymbol.full`. Tests combine it with virtual JVM accessor overrides to preserve read order and failures.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `64f5abc`
(SHA-256 `39dcdec0f93d8d475b326b70abb0c8afd8c76f45d175e0d4fd032d6d61e6dc07`). Source SHA-256 is
`7a82e44389fe45d9aaf0b3a830ba554074b2f947e634a9ae5417163e9e969b33`. The frozen class file is under
`src/test/resources/compat/`; do not regenerate it against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaMethodSymbolFull` | `82aad2c3f67b61337d7f9daf853ccdd5e68bd30ccf82abd084195e7b66e0846d` |

### `ReferenceScalaClassParent`

`scala/codechicken/multipart/compat/ReferenceScalaClassParent.scala` freezes a Scala caller, ClassType subclass and
direct ClassSymbolRef implementation. Tests use them to preserve the old trait helper and info/parent lookup order.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `a670ea3`
(SHA-256 `c633ae5c5eeb98cd06fd8dbc81d138f1f241b59534708a1fbf59f4681df5eede`). Source SHA-256 is
`81be49b2c65c62df5de9e1d3d3eb785f25049889ef8dc1b8ad308a943ebdad6b`. Frozen class files are under
`src/test/resources/compat/`; do not regenerate them against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaClassParent` | `38001778088d012f62256291ede409203138b396cbc28f208e980a29f2b9418f` |
| `ReferenceScalaClassParent$$anon$1` | `91fee2779c962b2b970383ef965b0ec5f2e8d1d44cfc026b7394804f1e56993d` |
| `ReferenceScalaClassParent$$anon$2` | `941a3dce5726e8f355159ae001007f54ad4a307e7d91887909eb84f11952802c` |

### `ReferenceScalaMethodSymbolDescriptor`

`scala/codechicken/multipart/compat/ReferenceScalaMethodSymbolDescriptor.scala` freezes a Scala caller and
MethodSymbol subclass with a virtual method-type supplier. Tests preserve info/descriptor order and failure behavior.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `27863b4`
(SHA-256 `1d2dcad447e30fb8df3f73e60b9a02987c42d9f8a632ce5c4523f365c7043ff5`). Source SHA-256 is
`c584b3c91ce80c3b3e5e413bb24af160d1eb89fb0a9bb51b4570193e99b27c9b`. Frozen class files are under
`src/test/resources/compat/`; do not regenerate them against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaMethodSymbolDescriptor` | `47731de0986ab2701a3d5acae0b0a93b24c979495c288383fcab4a925370c192` |
| `ReferenceScalaMethodSymbolDescriptor$$anon$1` | `9fbfb463190e14a9be53a27add9d460d2e8e45bedb0054059806798aa098d2e1` |

### `ReferenceScalaMethodSymbolInfo`

`scala/codechicken/multipart/compat/ReferenceScalaMethodSymbolInfo.scala` freezes a Scala caller of
`MethodSymbol.info`. Tests preserve the path-dependent return descriptor, virtual info-ID evaluation and failures.

Compiled with Scala 2.11.5 under Java 8, `-target:jvm-1.8`, against the untouched dev jar at `257d0e4`
(SHA-256 `1d671a402ec0fe06fcf44c0edbcca350372c05886ad2a7b3cb5fb8688ab95267`). Source SHA-256 is
`53c16080dcd7fe271a9c89b2c869bcb8410baad88e72641d73668aea1cbfd8b8`. The frozen class file is under
`src/test/resources/compat/`; do not regenerate it against the port.

| Class | SHA-256 |
| --- | --- |
| `ReferenceScalaMethodSymbolInfo` | `0d89ef63187efc9f7959e292ba8e6cee1f4b68794891b913c53c3e96032c869e` |
