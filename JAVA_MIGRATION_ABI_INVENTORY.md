# Downstream ABI inventory

For runtime behavior, data formats, lifecycle use, and member-level reflection found in consumer source, see
[`JAVA_MIGRATION_CONSUMER_AUDIT.md`](JAVA_MIGRATION_CONSUMER_AUDIT.md).

Completes the Phase 0 item "Inventory downstream mods that compile against or reflect into ForgeMultipart".

Method: constant-pool scan of every mod jar in a real pack, not a source search. Bytecode is the only oracle that
covers reflection strings, closed-source consumers, and mods whose published source does not match their release jar.
GitHub code search was deliberately not used; it indexes default branches only and silently omits results.

Scanned: GTNH daily `2026-08-14+678`, 240 mod jars (`ForgeMultipart-1.7.12.jar` itself excluded).
Reproduce with `tools/AbiScan.java`; frozen baseline in `src/test/fixtures/abi/gtnh-daily-678-consumers.txt`.

```
java tools/AbiScan.java <instance>/.minecraft/mods ForgeMultipart
```

Requires a JDK 17+ single-file source launch. Re-run at every public-API phase and diff against the frozen baseline;
any member that disappears from the port but appears in the baseline is a linkage break in a shipping mod.

## Totals

| Category | Count |
| --- | ---: |
| Consumer jars | 27 of 240 |
| Types extended or implemented downstream | 35 |
| Referenced members with exact descriptors | 255 |
| Other referenced types | 76 |
| Reflective string constants | 20 |

## Consumers by weight

Ranked by number of classes in the jar that touch `codechicken/multipart` or `codechicken/microblock`.

| Classes | Jar |
| ---: | --- |
| 141 | ProjRed-4.12.41-GTNH |
| 38 | extrautilities-1.2.12 |
| 19 | OpenComputers-1.12.55-GTNH |
| 18 | ProjectBlue-1.2.10-GTNH |
| 7 | appliedenergistics2-rv3-beta-1032-GTNH |
| 7 | buildcraft-compat-7.1.22 |
| 5 | ForgeRelocationFMP-0.2.0 |
| 4 | Galacticraft-3.4.33-GTNH, ThaumicTinkerer-2.12.29, WR-CBE-1.7.12, chisel-2.17.32-GTNH |
| 3 | IguanaTweaksTConstruct-2.7.12, WitchingGadgets-1.8.49-GTNH, guidenh-1.3.20 |
| 2 | BloodArsenal-1.5.12, Botania-1.13.33-GTNH, Waila-1.19.31, matter-manipulator-0.1.53-GTNH |
| 1 | BiomesOPlenty, BloodMagic-1.9.12, Natura-2.8.23, Schematica-1.12.6-GTNH, TConstruct-1.14.97-GTNH, ae2fc-1.5.102-gtnh, endercore-0.5.14, etfuturum-2.6.52-GTNH, gregtech-5.09.54.88 |

Two distinct consumer shapes:

- **Java consumers** (extrautilities, AE2, chisel, WitchingGadgets, ThaumicTinkerer, buildcraft-compat, gregtech,
  matter-manipulator, and the long tail) use the Java-facing API: `MultiPartRegistry.registerParts(IPartFactory, String[])`,
  `registerConverter`, `BlockMicroMaterial`, `MicroMaterialRegistry.registerMaterial`, `TileMultipart.jPartList()`,
  `addPart`, `remPart`, `canAddPart`. None of these need Scala.
- **Scala consumers** (ProjRed, OpenComputers, ProjectBlue, ForgeRelocationFMP) mix in ForgeMultipart traits and call
  the generated `$class` static helpers directly. These are the constraint on Scala removal.

## Answers to the plan's open decisions

### 1. Which existing downstream binaries must keep working without recompilation?

The 27 jars above. The compatibility budget is dominated by four: ProjRed, extrautilities, OpenComputers, ProjectBlue.
ProjRed alone accounts for 141 of the touching classes and is the only consumer exercising the trait-generation path.

### 2. Are third-party Scala traits registered through `registerScalaTrait`?

**Yes.** Not internal-only. `ProjRed-4.12.41-GTNH.jar` calls
`codechicken/microblock/MicroblockGenerator$.registerTrait(Ljava/lang/Class;)I` from
`mrtjp.projectred.illumination.LightMicroMaterial$`, passing `mrtjp.projectred.illumination.LightMicroblock`.
That class ships with a `LightMicroblock$class.class` helper and a `Scala` attribute, so
`ASMMixinFactory.registerTrait` takes the `info.isTrait` branch into `registerScalaTrait` and parses its ScalaSignature.

Consequence: Phase 7 cannot drop `registerScalaTrait` or ScalaSignature decoding without breaking ProjRed's
illuminated microblocks. The Java trait path can become the internal default, but the Scala path stays as a
supported external extension point.

### 3. How long should deprecated Scala bridges remain supported?

Not answerable from this data alone, but the surface to support is now bounded and listed below. Of the 255 referenced
members, only 5 carry Scala types in their descriptors.

### 4. Is complete Scala runtime removal required for the first Java release?

**No, and it is not achievable for the first release.** Four shipping mods link against Scala trait helpers and
companion singletons. Java-maintainable source is the correct first milestone; Scala runtime removal requires either a
coordinated rebuild of ProjRed, OpenComputers, ProjectBlue and ForgeRelocationFMP, or an accepted break.

## Load-bearing Scala surface

Everything in this section must keep its exact descriptor, or a named consumer fails to link.

### Trait `$class` helpers (8 classes, 16 static methods)

| Helper | Methods used | Consumers |
| --- | --- | --- |
| `TCuboidPart$class` | `$init$`, `drawBreaking`, `getCollisionBoxes`, `getSubParts` | ForgeRelocationFMP, OpenComputers, ProjRed, ProjectBlue |
| `TIconHitEffects$class` | `$init$`, `addDestroyEffects`, `addHitEffects` | ForgeRelocationFMP, OpenComputers, ProjRed |
| `TNormalOcclusion$class` | `$init$`, `occlusionTest` | ForgeRelocationFMP, OpenComputers, ProjRed |
| `JIconHitEffects$class` | `$init$`, `getBreakingIcon` | ForgeRelocationFMP, OpenComputers, ProjRed |
| `TFacePart$class` | `$init$`, `solid`, `redstoneConductionMap` | OpenComputers, ProjRed, ProjectBlue |
| `TItemMultiPart$class` | `$init$`, `getHitDepth`, `onItemUse` | ProjRed |
| `Saw$class` | `$init$`, `getMaxCuttingStrength` | ProjRed |
| `TEdgePart$class` | `$init$` | OpenComputers |

The `JPartialOcclusion$class` bridge already added on this branch follows the right pattern, but note that
`JPartialOcclusion$class` itself has **no** downstream caller in the pack — the two consumers that touch
`JPartialOcclusion` (ForgeRelocationFMP, WitchingGadgets) only call `getPartialOcclusionBoxes()Ljava/lang/Iterable;`.
The eight helpers above are the ones that actually matter.

### Companion object singletons (17 `MODULE$` fields read downstream)

`MultiPartRegistry$`, `TileMultipart$`, `MultipartGenerator$`, `NormalOcclusionTest$`, `RedstoneInteractions$`,
`MicroMaterialRegistry$`, `MicroblockGenerator$`, `MicroblockRender$`, `BlockMicroMaterial$`, `ItemMicroPart$`,
`CornerMicroClass$`, `EdgeMicroClass$`, `FaceMicroClass$`, `HollowMicroClass$`,
`handler/MultipartProxy$`, `handler/MultipartSaveLoad$`, `handler/MicroblockProxy$`.

Each must keep a public static `MODULE$` field of its own type plus the instance methods used on it.

### Members with Scala types in their descriptors (5 total)

| Descriptor | Consumers |
| --- | --- |
| `TileMultipart.partList()Lscala/collection/Seq;` | OpenComputers, ProjRed, AE2, extrautilities, guidenh |
| `NormalOcclusionTest$.apply(Lscala/collection/Traversable;Lscala/collection/Traversable;)Z` | ForgeRelocationFMP, OpenComputers |
| `MultiPartRegistry$.registerParts(Lcodechicken/multipart/MultiPartRegistry$IPartFactory2;Lscala/collection/Seq;)V` | ProjRed |
| `MultiPartRegistry$.registerParts(Lscala/Function2;Lscala/collection/Seq;)V` | ForgeRelocationFMP |
| `MicroMaterialRegistry.getIdMap()[Lscala/Tuple2;` | extrautilities |

This is a much smaller surface than the plan assumed. `TileMultipart.jPartList()Ljava/util/List;` is already the more
widely used accessor (ProjectBlue, WitchingGadgets, buildcraft-compat, extrautilities, gregtech, matter-manipulator),
so the Java-first collection API mostly exists already and the Scala overloads are a thin retained shell.

`IDWriter`, ported on this branch, has **zero** downstream references. Its four deprecated Scala function accessors
are not load-bearing and can be dropped.

## Reflective consumers

These fail at runtime, not at link time, so ABI tooling will not catch a break. 20 string constants across:

- `guidenh-1.3.20` — the broadest reflective consumer: `MultipartGenerator`/`MultipartGenerator$`,
  `MultipartHelper`/`MultipartHelper$`, `MultipartRenderer`/`MultipartRenderer$`, `TileMultipart`,
  `TileMultipartClient`, `MicroblockClient`, `Microblock`, `MicroblockClass`, `MicroblockGenerator$`,
  `BlockMicroMaterial`/`BlockMicroMaterial$`, `MicroMaterialRegistry`/`MicroMaterialRegistry$`, `BlockMultipart`.
- `Schematica-1.12.6-GTNH` — `TileMultipart`, `TMultiPart`, `MultiPartRegistry$`, `MultipartGenerator$`, `MicroblockClass`.
- `Galacticraft-3.4.33-GTNH` — `TileMultipart`, `MicroMaterialRegistry`, `BlockMicroMaterial`.
- `Waila-1.19.31` — `BlockMultipart`.
- `AE2`, `ae2fc` — `TileMultipart`.
- `etfuturum-2.6.52-GTNH` — `codechicken.multipart.minecraft.ButtonPart`.

Note that both the class and its `$` companion are named in several cases, so renaming or removing a companion
object breaks these even where no bytecode reference exists.

## Consequences for the migration plan

1. Phase 8 (Scala runtime removal) should be reclassified as deferred, not scheduled. Decision 4 is answered.
2. Phase 7 must keep `registerScalaTrait`. Add a functional test that registers a Scala trait through
   `MicroblockGenerator.registerTrait` to freeze the path ProjRed depends on.
3. Bridge effort should be spent on the 8 `$class` helpers, 17 `MODULE$` singletons, and 5 Scala-typed descriptors
   listed above. Every other converted file can drop its Scala bridge after checking this inventory, as `IDWriter` can.
4. Add the reflective names to the manual compatibility checklist; the automated ABI diff cannot see them.

## guidenh's reflective surface, from source

The constant-pool scan can see the names guidenh reflects on but not the members it looks up. This section is read
from the checked-out source at `6137525`, in
`src/main/java/com/hfstudio/guidenh/integration/forgemultipart/ForgeMultipartHelpers.java`. **None of this is visible
to the ABI diff**; a break here is silent until a user opens a guide page.

The dispatch helper is `invokeStaticOrSingletonMethod(class, companion, method, args...)`. It tries a **static** method
on the plain class first, and only on `INVOCATION_MISSING` falls back to reading `companion.MODULE$` and invoking the
method on it. So for most entries the companion is a fallback. The exceptions below are the ones that matter.

| Owner | Member reflected on | Reached via |
| --- | --- | --- |
| `MultipartHelper` | `createTileFromNBT(World, NBTTagCompound)` | static; companion is fallback |
| `MultipartRenderer` | `renderWorldBlock(IBlockAccess, int, int, int, Block, int, RenderBlocks)` | static; companion is fallback |
| `MultipartGenerator` | `generateCompositeTile(TileEntity, Iterable, boolean)` | **companion only** — see below |
| `BlockMicroMaterial` | `register(material)`, plus a constructor of arity 2 `(Block, int)` or 1 `(Block)` | static; companion is fallback |
| `MicroMaterialRegistry` | `getMaterial(int)` | static; companion is fallback |
| `MicroblockGenerator$` | `create(MicroblockClass, int, boolean)` | **companion only**, matched by exact parameter types |
| `TileMultipart` | `partList()`, `partList_$eq(scala.collection.Seq)`, `loadParts(...)`, `notifyTileChange()`, `markRender()` | instance |
| `Microblock` | `microClass()`, `material()`, `shape()` | instance |
| `TMultiPart` | `getDrops()` | instance |
| `IMicroMaterial` | `block()`, `meta()` | instance |

Types used only for `isInstanceOf`: `BlockMultipart`, `TileMultipart`, `TileMultipartClient`, `Microblock`,
`MicroblockClient`, `BlockMicroMaterial`.

### Three constraints this adds

1. **`MultipartGenerator$.MODULE$` is load-bearing through reflection.** `generateCompositeTile` is `private[multipart]`
   in Scala, so no static forwarder is emitted on `MultipartGenerator` and guidenh's static attempt always misses. It
   reaches the method only through the companion. A Phase 6/7 port that keeps the class but drops the companion, or
   promotes the method to a public static and removes it from the companion, breaks guidenh silently.

2. **`MicroblockGenerator$.create` is matched by exact parameter types**, including `pts[0].getName()` string-compared
   against `"codechicken.microblock.MicroblockClass"`. That pins `create(MicroblockClass, int, boolean)` on the
   companion and pins `MicroblockClass`'s fully qualified name. Widening a parameter or renaming the class breaks the
   lookup even though every call site still links.

3. **`TileMultipart.partList_$eq(scala.collection.Seq)` is reflectively load-bearing.** It is a Scala `var` setter with
   no Java-facing equivalent, so a Java-first port that drops it in favour of a list mutator would break guidenh
   invisibly. The current port keeps it, verified by `javap`. `resolvePartList` also falls back to a public *field*
   named `partList`, which never existed — Scala emits the field private — so that path is dead in the reference too.

`MultipartHelper$` is retained on the strength of the fallback path alone: the static is found first today, so the
companion is never reached in practice. That is a weaker justification than the two "companion only" entries above,
but the cost of keeping a four-method forwarder is negligible against a silent break.
