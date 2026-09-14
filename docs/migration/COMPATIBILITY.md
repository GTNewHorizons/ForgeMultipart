# Downstream compatibility: ABI inventory and consumer audit

Status: 2026-09-09. Read this before changing any compatibility surface. It has two parts, answering two different
questions.

- **Part 1, the ABI inventory**, answers "what must still link?" It is a constant-pool scan of every mod jar in a
  real pack, not a source search.
- **Part 2, the consumer audit**, answers the harder question: "what behavior do real consumers expect after they
  have linked?" It covers all 27 ForgeMultipart consumers in the supplied GTNH daily `2026-08-25+700` instance, plus
  UtilitiesInExcess as the planned Extra Utilities replacement.

Intentional differences from the reference belong in the [divergence ledger](DIVERGENCES.md), and the
consumer-facing summary of what breaks on rebuild is in the [release notes](../RELEASE_NOTES.md).

# Part 1 - Downstream ABI inventory

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
`JPartialOcclusion$class` itself has **no** downstream caller in the pack - the two consumers that touch
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

The branch now supplies `NormalOcclusionTest.testBoxes(java.lang.Iterable, java.lang.Iterable)` for the box-list
contract. Both old `apply(Traversable, Traversable)` entries remain, with deprecations pointing to the Java entry.
The shipping ForgeRelocationFMP/OpenComputers companion calls still require their exact descriptor until adoption;
see the [migration guide](../api/OCCLUSION.md#box-versus-box-queries) and consumer ledger.

`MultiPartRegistry.registerPartFactory(IPartFactory2, String...)` now provides the Java registration entry without
Scala overload resolution. Both `registerParts(IPartFactory2, Seq)` entries are deprecated; all eight existing
registration descriptors remain. ProjectRed's sequence and ForgeRelocationFMP's function companion calls still need
their bridges until release/adoption. See the [registration guide](../api/PART_REGISTRATION.md).

`IDWriter`, ported on this branch, has **zero** downstream references. Its four deprecated Scala function accessors
are not load-bearing and can be dropped.

## Reflective consumers

These fail at runtime, not at link time, so ABI tooling will not catch a break. 20 string constants across:

- `guidenh-1.3.20` - the broadest reflective consumer: `MultipartGenerator`/`MultipartGenerator$`,
  `MultipartHelper`/`MultipartHelper$`, `MultipartRenderer`/`MultipartRenderer$`, `TileMultipart`,
  `TileMultipartClient`, `MicroblockClient`, `Microblock`, `MicroblockClass`, `MicroblockGenerator$`,
  `BlockMicroMaterial`/`BlockMicroMaterial$`, `MicroMaterialRegistry`/`MicroMaterialRegistry$`, `BlockMultipart`.
- `Schematica-1.12.6-GTNH` - `TileMultipart`, `TMultiPart`, `MultiPartRegistry$`, `MultipartGenerator$`, `MicroblockClass`.
- `Galacticraft-3.4.33-GTNH` - `TileMultipart`, `MicroMaterialRegistry`, `BlockMicroMaterial`.
- `Waila-1.19.31` - `BlockMultipart`.
- `AE2`, `ae2fc` - `TileMultipart`.
- `etfuturum-2.6.52-GTNH` - `codechicken.multipart.minecraft.ButtonPart`.

Note that both the class and its `$` companion are named in several cases, so renaming or removing a companion
object breaks these even where no bytecode reference exists.

The branch now offers `MultiPartRegistry.getPartFactory(String): IPartFactory2` to replace Schematica's private
registry-map reflection, with a [migration guide](../api/FACTORY_LOOKUP.md). Its old companion field retains the
exact private Scala mutable-map shape and live backing; the public addition does not authorize removing that field
before consumer release/adoption. Generator reflection and preview lifecycle remain separate contracts.

`ButtonPart.setOrientation(int, ForgeDirection): void` now replaces Et Futurum's four reflective array writes and
keeps the two direction maps consistent; [guide](../api/BUTTON_ORIENTATIONS.md). The existing public mutable static
`metaSideMap` and `sideMetaMap` fields retain their exact names, types and modifiers for old releases. Removing or
narrowing them still waits for a migrated Et Futurum release, target-pack adoption and a fresh reflective-source scan.

`ItemSaw.setHarvestLevel(int): void` now replaces Iguana's private-field write; [guide](../api/SAW_STRENGTH.md).
The existing private `harvestLevel: int` field remains for old releases and shares storage with the setter. It loses
only `ACC_FINAL`, which makes the supported mutation explicit and keeps reflective writes working. Removal still
waits for an Iguana release, target-pack adoption and a fresh scan.

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
| `MultipartGenerator` | `generateCompositeTile(TileEntity, scala.collection.Iterable, boolean)` | **companion only for the Scala descriptor**; new static Java-Iterable entry is additive |
| `BlockMicroMaterial` | `register(material)`, plus a constructor of arity 2 `(Block, int)` or 1 `(Block)` | static; companion is fallback |
| `MicroMaterialRegistry` | `getMaterial(int)` | static; companion is fallback |
| `MicroblockGenerator$` | `create(MicroblockClass, int, boolean)` | GuideNH explicitly selects the companion, matched by exact parameter types; static Java replacement exists |
| `TileMultipart` | `partList()`, `partList_$eq(scala.collection.Seq)`, `loadParts(...)`, `notifyTileChange()`, `markRender()` | instance |
| `Microblock` | `microClass()`, `material()`, `shape()` | instance |
| `TMultiPart` | `getDrops()` | instance |
| `BlockMicroMaterial` | `block()`, `meta()` | instance; absent from general `IMicroMaterial` |

Types used only for `isInstanceOf`: `BlockMultipart`, `TileMultipart`, `TileMultipartClient`, `Microblock`,
`MicroblockClient`, `BlockMicroMaterial`.

### Three constraints this adds

1. **`MultipartGenerator$.MODULE$` is load-bearing through reflection.** `generateCompositeTile` is `private[multipart]`
   in Scala, so no static forwarder is emitted on `MultipartGenerator` and guidenh's static attempt always misses. It
   reaches the method only through the companion. A Phase 6/7 port that keeps the class but drops the companion, or
   promotes the method to a public static and removes it from the companion, breaks guidenh silently.

   The branch now adds `MultipartGenerator.generateCompositeTile(TileEntity, java.lang.Iterable, boolean)` while
   retaining the exact companion Scala descriptor/body. GuideNH's old Scala sequence is not assignable to the new
   Java parameter, so its existing matcher still falls back to the companion. Both old and new reflective contracts
   are tested; migration requires a Java parts collection. See the [guide](../api/COMPOSITE_GENERATION.md).

2. **`MicroblockGenerator$.create` is matched by exact parameter types**, including `pts[0].getName()` string-compared
   against `"codechicken.microblock.MicroblockClass"`. That pins `create(MicroblockClass, int, boolean)` on the
   companion and pins `MicroblockClass`'s fully qualified name. Widening a parameter or renaming the class breaks the
   lookup even though every call site still links.

   The existing static `MicroblockGenerator.create(MicroblockClass, int, boolean)` now has a
   [guide and compiling example](../api/MICROBLOCK_CREATION.md). Both exact reflective entries are tested; no API
   shape or production method body changes were needed. GuideNH must change its explicitly selected owner and receiver
   before it stops using the companion. Release/adoption, shape-setter override checks and client previews remain gates.

3. **`TileMultipart.partList_$eq(scala.collection.Seq)` is reflectively load-bearing.** The branch now supplies
   `setPartList(java.util.List)` and `loadPartList(java.util.Collection)`, while retaining the exact legacy setter and
   loader descriptors and override dispatch. Dropping the old setter in favor of the Java method would still break
   guidenh invisibly. Exact and assignability-based reflective tests cover both legacy contracts. `resolvePartList` also falls back to a public *field*
   named `partList`, which never existed - Scala emits the field private - so that path is dead in the reference too.

`MultipartHelper$` is retained on the strength of the fallback path alone: the static is found first today, so the
companion is never reached in practice. That is a weaker justification than the two "companion only" entries above,
but the cost of keeping a four-method forwarder is negligible against a silent break.

GuideNH's private `BlockMicroMaterial.block: Block` and `meta: int` fields remain unchanged for its accessor mixin.
Existing public virtual `block()` / `meta()` now have a [typed query guide](../api/MATERIAL_ACCESS.md) and regression
coverage; no descriptor or method body changed. The mixin reads constructor fields whereas direct calls honor
subclass overrides, as the old reflective fallback already did. Remove the consumer's mixin only when adopting the
typed query; FMP field removal still waits for released-consumer adoption.


# Part 2 - Source-level consumer audit


## Bottom line

The Java conversion cannot be treated as a set of independent class translations. Downstream code depends on five
connected contracts:

1. **The generated composite tile architecture.** Parts cause `TileMultipart` subclasses to acquire Scala traits,
   Java traits, and arbitrary pass-through interfaces. AE2, ProjectRed, Extra Utilities, WR-CBE,
   WitchingGadgets, ForgeRelocationFMP, and UtilitiesInExcess all exercise this architecture.
2. **Scala-emitted binary shapes.** ProjectRed, OpenComputers, ProjectBlue, and ForgeRelocationFMP call generated
   trait `$class` helpers and companion `MODULE$` objects. Java consumers also use Scala-shaped APIs such as
   `partList(): scala.collection.Seq` and `getIdMap(): scala.Tuple2[]`.
3. **Multipart state and lifecycle.** Consumers do more than add parts. They inspect `partMap`, mutate/rebuild slot
   maps, move the live tile object, call `onMoved`, remove and reload parts, trigger part/tile notifications, and
   make deliberate choices about description packets.
4. **World, NBT, and packet formats.** The container `parts` list, each part's `id`, core microblock fields and part
   type strings are an interchange format used by builders, schematic tools, movers, HUD mods, and save/load code.
   Material numeric IDs and their packet encoding are also shared outside FMP.
5. **Names that are not in ordinary linkage.** Schematica reaches a private Scala-mangled field, Et Futurum mutates
   static arrays, GuideNH mixes into private fields, Iguana reflects a saw field, and several mods load FMP classes
   and companions by string. A clean ABI diff alone will miss these breaks.

The safest first Java release is therefore **Java-maintainable implementation with the bounded Scala compatibility
shell retained**. Removing the Scala runtime is a separate coordinated migration, not the end of this source port.

## Coverage and method

The supplied instance was rescanned with `tools/AbiScan.java` before reading source:

- scanner visited 241 mod JARs, one of which was excluded as ForgeMultipart itself;
- 27 consumer JARs;
- 35 downstream-extended or implemented FMP types;
- 255 referenced FMP members with exact descriptors;
- 76 other referenced FMP types;
- 20 class-name/reflection strings.

Those structural totals are unchanged from the frozen `+678` inventory. The consumer versions changed, but no new
binary API family appeared. The compatibility audit covers active consumer binaries, including Extra Utilities,
and the supplied source checkouts. It records required types/members, reflection contracts, factories, converters,
part subclasses, tile access, registry access, NBT, packets, rendering, occlusion, redstone and movement behavior.
Where source checkouts are available, important call sites were traced through the owning code instead of counting imports.

This gives full coverage of the current pack's known source and binary consumers. It does not claim that an arbitrary
future mod, a runtime-generated script, or an unpublished patched JAR cannot contain another reflective use. The ABI
scanner should remain a release gate for exactly that reason.

### Consumer references

| Installed consumer | Reference used | Match to installed code                                   |
| --- | --- |-----------------------------------------------------------|
| ProjRed `4.12.43-GTNH` | `ProjectRed` at `4.12.43-GTNH` (`e173952e96a4`) | Exact                                                     |
| Extra Utilities `1.2.12` | Active supported consumer | Compatibility required for the installed release |
| OpenComputers `1.12.56-GTNH` | `OpenComputers`; installed tag plus HEAD `1.12.57-GTNH` | FMP files unchanged after installed tag                   |
| ProjectBlue `1.2.10-GTNH` | `ProjectBlue` at `1.2.10-GTNH` (`c01a5e769643`) | Exact                                                     |
| AE2 `rv3-beta-1041-GTNH` | `Applied-Energistics-2-Unofficial`; installed tag plus HEAD `1042` | FMP files unchanged after installed tag                   |
| BuildCraftCompat `7.1.22` | `BuildCraftCompat` at `7.1.22` (`79e02803be32`) | Exact                                                     |
| ForgeRelocationFMP `0.2.0` | `ForgeRelocationFMP` at `0.2.0` (`49a810b8c63b`) | Exact; installed JAR also disassembled as a cross-check   |
| Galacticraft `3.4.33-GTNH` | `Galacticraft` at `3.4.33-GTNH` (`26a472f9c7ae`) | Exact                                                     |
| ThaumicTinkerer `2.12.30` | `ThaumicTinkerer` at `2.12.30` (`18c787153c96`) | Exact                                                     |
| WR-CBE `1.7.12` | `WirelessRedstone-CBE` at `1.7.12` (`9dfb913b4880`) | Exact                                                     |
| Chisel `2.17.32-GTNH` | `Chisel` at `2.17.32-GTNH` (`ff70c8b2e3cd`) | Exact                                                     |
| IguanaTweaksTConstruct `2.7.12` | `IguanaTweaksTConstruct` at `2.7.12` (`2bc09889d3e2`) | Exact                                                     |
| WitchingGadgets `1.8.50-GTNH` | `WitchingGadgets` at `1.8.50-GTNH` (`f82988693af3`) | Exact                                                     |
| GuideNH `1.3.22` | `GuideNH` at `1.3.22` (`7d8fb44e77b9`) | Exact                                                     |
| BloodArsenal `1.5.12` | `BloodArsenal` at `1.5.12` (`890824be106a`) | Exact                                                     |
| Botania `1.13.33-GTNH` | `Botania`; installed tag plus HEAD `1.13.34-GTNH` | FMP files unchanged after installed tag                   |
| Waila `1.19.34` | `waila` at `1.19.34` (`f998f9ebfe09`) | Exact                                                     |
| MatterManipulator `0.1.54-GTNH` | `MatterManipulator` at `0.1.54-GTNH` (`897ffbf17a02`) | Exact                                                     |
| Biomes O' Plenty `2.1.0.2308` | `BiomesOPlenty`, branch `BOP-1.7.10-2.1.x` | No exact tag; historical source plus exact binary surface |
| BloodMagic `1.9.13` | `BloodMagic` at `1.9.13` (`6776676ef209`) | Exact                                                     |
| Natura `2.8.24` | `Natura` at `2.8.24` (`4f88c0665928`) | Exact                                                     |
| Schematica `1.12.6-GTNH` | `Schematica` at `1.12.6-GTNH` (`3b03ee937953`) | Exact                                                     |
| TConstruct `1.14.103-GTNH` | `TinkersConstruct`; installed tag plus HEAD `1.14.104-GTNH` | FMP file unchanged after installed tag                    |
| AE2 Fluid Craft `1.5.105-gtnh` | `AE2FluidCraft-Rework` at `1.5.105-gtnh` (`1706b5a8daf2`) | Exact                                                     |
| EnderCore `0.5.14` | `EnderCore` at `0.5.14` (`075609240fa7`) | Exact                                                     |
| Et Futurum Requiem `2.6.56-GTNH` | `Et-Futurum-Requiem`; installed tag plus HEAD `2.6.57-GTNH` | FMP file unchanged after installed tag                    |
| GT5U `5.09.54.108` | `GT5-Unofficial`; installed tag plus HEAD after `5.09.54.112` | FMP file unchanged after installed tag                    |
| Future: UtilitiesInExcess | `UtilitiesInExcess` HEAD `3e107a1fe9bc` | Not installed; audited as forward compatibility scope     |

`ForgeRelocation` itself is not the source of `ForgeRelocationFMP`; both repositories are needed and both are now
present. Only the plugin is one of the 27 FMP consumers.

## Cross-cutting compatibility map

| Contract | Consumers that make it load-bearing | What must survive |
| --- | --- | --- |
| Part factory/converter registration | ProjectRed, Extra Utilities, OpenComputers, AE2, BuildCraftCompat, ForgeRelocationFMP, Chisel, ThaumicTinkerer, WR-CBE, WitchingGadgets, ProjectBlue, UtilitiesInExcess | Java `IPartFactory`/`IPartFactory2`, converters, the Scala `Function2` and `Seq` overloads, stable type strings, client/server construction |
| Dynamic composite tiles | ProjectRed, Extra Utilities, AE2, ForgeRelocationFMP, WR-CBE, WitchingGadgets, UtilitiesInExcess; GuideNH and Schematica reconstruct them | Trait-bit selection, generated subclass caching, pass-through interfaces, client/server selection, promotion of an existing tile |
| External Scala trait generation | ProjectRed illuminated microblocks | `MicroblockGenerator.registerTrait`, ScalaSignature decoding, `$class` helper handling, correct trait initialization and dispatch |
| Tile collections and slot state | ProjectRed, Extra Utilities, OpenComputers, AE2, ProjectBlue, GT5U, BuildCraftCompat, WitchingGadgets, MatterManipulator, GuideNH | `partList(): Seq`, `jPartList(): List`, ordering, `partMap(int)`, mutable `TSlottedTile.v_partMap`, `bindPart`, slot-map rebuilds |
| Capability-cache refresh | OpenComputers `PrintPart.toggleState` | Supported `TileMultipart.bindPart` dispatch, caller-cleared old slot entries, no implicit list insertion or part binding |
| Local part-change notification | ProjectRed `RedstoneGatePart.onOutputChange` | Supported `TileMultipart.internalPartChange`, equality filtering and `operate` dispatch; caller retains packet/dirty/external-neighbor policy |
| Placement/removal/replacement | Most custom-part mods | `getTile`/`getOrConvertTile`, `canPlacePart`, `canAddPart`, `canReplacePart`, `addPart`, `remPart`, binding and notifications in their current order |
| Move lifecycle | ForgeRelocationFMP, MatterManipulator | A live `TileMultipart` can be detached/reinserted, coordinates rewritten, then `onMoved`; render, dirty, lighting, and description synchronization remain coherent |
| Occlusion and geometry | ProjectRed, Extra Utilities, OpenComputers, AE2, ForgeRelocationFMP, Chisel, WR-CBE, WitchingGadgets, UtilitiesInExcess | Normal and partial occlusion, collision/subpart boxes, `PartMap` numbering, Java iterable adapters, Scala `Traversable` overload |
| Redstone and connectivity | ProjectRed, Extra Utilities, OpenComputers, ProjectBlue, AE2, WR-CBE | `IRedstonePart` family, `TRedstoneTile`, `RedstoneInteractions`, face/edge/center maps, neighbor and part-change propagation |
| Micro-material registry | Extra Utilities, ProjectRed, ProjectBlue, AE2, BuildCraftCompat, Chisel, material-only integrations, UtilitiesInExcess | Stable material names, integer IDs, remapping, packet ID encoding, `getIdMap(): Tuple2[]`, block/meta access, material behavior hooks |
| Multipart save/load | Extra Utilities, ProjectRed, OpenComputers, BuildCraftCompat, MatterManipulator, GuideNH, Schematica, Waila, UtilitiesInExcess | Tile `parts` list, per-part `id`, factory lookup, `save`/`load` ordering, microblock fields, unknown/invalid-part behavior |
| Description/update packets | Every substantial custom part integration | `writeDesc`/`readDesc`, keyed part packets, `sendDescUpdate`, `sendDescPacket`, material packet IDs, render and neighbor notification behavior |
| Reflection/mixins | GuideNH, Schematica, Et Futurum, Galacticraft, Waila, AE2/AE2FC, Iguana | Exact class/companion/member/field names and, where stated below, exact parameter and field types |

## Consumer findings

### ProjectRed - deepest current consumer

ProjectRed is not merely an optional integration. Wires, framed wires, bundled wires, gates, lamps, buttons, pipes,
solar panels, and fabricated gates are FMP parts. Its 141 touching classes dominate the installed binary surface.

- Its parts mix `TMultiPart` with `TCuboidPart`, `TFacePart`, `TSlottedPart`, `TNormalOcclusion`,
  `TIconHitEffects`, redstone interfaces, and microblock hollow-connect interfaces. The emitted classes directly call
  Scala trait `$class` helpers.
- Item placement uses `TItemMultiPart`; registries use both `IPartFactory` and `IPartFactory2`, including the
  Scala-`Seq` registration descriptor.
- Connectivity is built on exact `PartMap` face/edge/center numbering and repeated `partMap` queries across the same
  and neighboring tiles. Redstone propagation uses `RedstoneInteractions` and casts generated tiles to
  `scalatraits.TRedstoneTile` for `openConnections`.
- Parts rely on lifecycle callbacks and their ordering: `onAdded`, `onRemoved`, `onPartChanged`,
  `onNeighborChanged`, `onChunkLoad`, `onWorldJoin`, scheduled ticks, `notifyPartChange`, `markDirty`, and
  `markRender`.
- Framed wires persist material names and use `materialID`, `materialName`, `getMaterial`, packet material IDs, and
  `ItemMicroPart.create`. Their fit calculation temporarily changes their own occlusion bounds and calls
  `tile.canReplacePart(this, this)`.
- Part-owned NBT and packet fields include `connMap`, `side`, `orient`, `shape`, `mat`, and type-specific data. FMP
  must keep treating these tags as opaque and must continue calling part save/load and packet hooks exactly once in
  the established order.
- `LightMicroMaterial` implements `MicroblockGenerator.IGeneratedMaterial` and registers the third-party Scala trait
  `LightMicroblock` with `MicroblockGenerator.registerTrait`. Generated microblocks must actually acquire that trait;
  it supplies dynamic halo rendering and computes light from `tile.partList`.
- ProjectRed also uses `MultipartSaveLoad.loadingWorld`, `MultipartProxy.block`, vanilla multipart part classes,
  microblock class companions, saw traits, and the material highlight renderer extension.

**Migration consequence:** ProjectRed is the acceptance test for the complete architecture: Scala trait ingestion,
trait linearization, tile and microblock code generation, redstone tile traits, core lifecycle, rendering, packets,
and the micro-material system. A launch-only smoke test is inadequate; illuminated microblocks, gates, face/framed
wires, and routed pipes need functional fixtures.

### Extra Utilities 1.2.12 - active supported consumer

Extra Utilities remains in the supported compatibility target. UtilitiesInExcess is its intended replacement;
approval to switch that target and adoption of the replacement in the pack are still pending. Preserve the current
consumer's required APIs, saved-data behavior and integration coverage until both gates pass.

Required compatibility spans several FMP subsystems:

- It registers generated-tile pass-through interfaces for `IAntiMobTorch`, pipe/cosmetic/filter pipe interfaces,
  node, inventory, liquid, and energy node interfaces, plus CoFH `IEnergyHandler`. Composite tiles must expose these
  interfaces and dispatch to the correct part.
- Transfer pipes and nodes are multipart factories/converters. They use face/slot traits, neighbor-tile callbacks,
  hollow connections, `TRedstoneTile`, `RedstoneInteractions`, part lists/maps, and part-change notifications.
- Custom parts include the magnum torch, fence, wall, sphere, pipe jackets, and several microblock-derived shapes.
  They exercise normal/partial occlusion, icon hit effects, `PartMetaAccess`, `MicroblockRender`, and client/server
  factories.
- Connected-texture and colored/full-bright micro-materials subclass `BlockMicroMaterial` and participate in
  material rendering. Fences and walls add their own pass-through interfaces.
- NEI recipes and item enumeration consume `MicroMaterialRegistry.getIdMap()` as `scala.Tuple2[]`, use
  `MicroRecipe`, saw cutting strength, material names, and `ItemMicroPart` creation.
- Some paths use `jPartList`; others use `partList().toIterator()`. Both collection views and stable ordering are
  required even though the mod's implementation language is Java.

The jar is sufficient to establish behavior, while the exact JAR's constant pool remains the authority for
names and descriptors. Obfuscated Minecraft method names should not be treated as source-quality naming evidence.

**Migration consequence:** Extra Utilities covers almost every Java-facing extension seam and is the strongest test
that Java wrappers still compose into the same generated tile. UtilitiesInExcess does not make this compatibility
burden disappear until existing worlds and the old mod have been retired.

### OpenComputers - generated slot state is part of the effective API

OpenComputers converts its cable and 3D print blocks to parts and registers `oc` cable/print factories.

- `CablePart` mixes cuboid, slotted, hollow-connect, normal-occlusion, icon-effect, and OC network-environment
  behavior. Network join/separate is driven from part lifecycle callbacks.
- Network traversal locates cable parts through `TileMultipart.partList`. Connection checks combine FMP normal
  occlusion boxes, solid face parts, and exact `PartMap.face(...).mask` values.
- `PrintPart` mixes cuboid, edge, face, slotted, normal occlusion, and redstone traits. Its shape and slot mask change
  at runtime.
- When a print toggles shape, it casts the generated tile to `scalatraits.TSlottedTile`, directly clears every
  matching entry in `v_partMap`, and calls `tile.bindPart(this)` to rebuild its slots. `v_partMap` field shape and
  `bindPart` behavior are therefore externally load-bearing, not private implementation details in practice.
- Placement uses `getOrConvertTile`, `canAddPart`, and `addPart`; complexity limits are calculated across the Scala
  `partList`. Print redstone input also folds over that list.

**Migration consequence:** a Java slot-map rewrite must retain a binary and behavioral bridge for `TSlottedTile`,
including its public generated state. Test a print whose on/off shapes occupy different slots next to a cable and a
face part.

### ProjectBlue - Java parts built on Scala shim bases

ProjectBlue defines tiny Scala bridge bases (`JFacePart`, `JCuboidFacePart`, `JCenterPart`) so most of its FMP code
can be Java. Its active multipart is the bundled-redstone control panel.

- The bridge bases inherit `TFacePart`, `TCuboidPart`, and `TSlottedPart`, so their generated `$class` calls remain
  binary dependencies.
- Control panels register an `IPartFactory`, place through `canPlacePart`/`addPart`, remove through `remPart`, and
  send part description updates.
- Bundled/redwire connectivity repeatedly queries `partMap` using exact face, edge, and center indices and uses
  `RedstoneInteractions` for vanilla/FMP power.
- The panel resolves its cover material by stable micro-material name and item representation. Its own persistent
  fields are opaque part data.
- Its edit network identifies a target part by world coordinates plus index in `jPartList`; list order is therefore
  observable over the network.

**Migration consequence:** preserve trait bridges, part ordering, `PartMap`, redstone semantics, and stable material
name lookup. A control-panel edit packet is a useful ordering regression test.

### Applied Energistics 2

AE2's cable bus is a full multipart integration, not just visual compatibility.

- `CableBusPart` is a Java cuboid part with normal occlusion, masked redstone, and `AEMultiTile`; AE2 dynamically
  registers `AEMultiTile` and layer interfaces as pass-through interfaces.
- `PartRegistry` constructs registered AE2 parts through AE2's combined-instance helper. The FMP integration acts as
  both part factory and block converter.
- Placement/conversion uses `getOrConvertTile`, `canAddPart`, and `addPart`. Code traverses the Scala `partList`
  iterator.
- Occlusion is checked both directly with `NormalOcclusionTest` and by asking `tile.canAddPart` with an AE2
  `NormallyOccludedPart` probe.
- When a generated tile implements `TIInventoryTile`, AE2 calls `rebuildSlotMap()` after changes.
- AE2's Waila path and AE2 Fluid Craft load `TileMultipart` by name to register multipart HUD providers.

**Migration consequence:** preserve dynamic pass-through interfaces and generated tile inventory traits, not only
the `CableBusPart` class ABI. Test a cable bus sharing a block with an inventory-bearing part and an occluding cover.

### BuildCraftCompat

BuildCraftCompat serializes multipart blocks for builders and reconstructs them later.

- It identifies `MultipartProxy.block`, walks `jPartList`, calls each part's `save`, and records the part type.
- Restore calls `MultiPartRegistry.loadPart(type, tag)`, then uses `canPlacePart`/`addPart`.
- Its microblock schematic validates the saved material name through `MicroMaterialRegistry.getMaterial`.
- It understands and rotates FMP's own saved type IDs for face, corner, edge, post, hollow, torch, lever, button,
  and redstone-torch parts.

**Migration consequence:** registry lookup, part `save`/`load`, core type IDs, material names, and placement validation
are a world-data contract. A builder round trip containing every built-in Minecraft part and microblock shape should
be a golden test.

### ForgeRelocationFMP

The plugin registers `rfmp_frame`, a frame-block converter, an `IFrame` pass-through interface, and an FMP tile
mover.

- `FramePart` mixes `TCuboidPart`, `TNormalOcclusion`, and `TIconHitEffects`. Its special fit test temporarily exposes
  one face occlusion box, calls `canReplacePart(this, this)`, and combines another part's normal boxes, partial boxes,
  and collision boxes through the Scala `NormalOcclusionTest` overload.
- Frame placement requires an existing multipart tile and uses `canPlacePart`/`addPart`.
- The mover removes the live `TileMultipart`, moves the original block and tile object, rewrites `xCoord/yCoord/zCoord`,
  reinserts it, and calls `onMoved()` in the post-move hook.

**Migration consequence:** this is a direct test of Scala trait helpers, Scala factory overloads, mixed occlusion,
pass-through interfaces, and live tile movement. `onMoved` must reestablish every coordinate-sensitive/generated-tile
invariant without replacing the tile object.

### Chisel

Chisel registers its blocks as micro-materials and adds a `chisel_torch` factory/converter. `PartChiselTorch`
subclasses FMP's `TorchPart`, including its constructors, load/read/write behavior and static side mapping. Placement
uses tile conversion, fit checks, and `addPart`; Chisel also supplies a full-bright material implementation.

**Migration consequence:** the Minecraft-part classes are extension bases, not internal examples. Keep protected
state, constructors, packet/NBT hooks, and static orientation tables source- and binary-compatible.

### ThaumicTinkerer

ThaumicTinkerer registers block micro-materials and factories/converters for candle and nitor parts. The parts extend
`McMetaPart`; the candle participates in random display ticks. They rely on FMP meta-part persistence, placement,
render hooks, and factory type dispatch.

**Migration consequence:** preserve `McMetaPart` subclass semantics and random-display capability composition.

### Wireless Redstone CBE

WR-CBE registers transmitter, receiver, and jammer parts and pass-through interfaces for wireless tile APIs.

- Its Java parts use `JItemMultiPart`, `JCuboidPart`, face/redstone, normal/partial occlusion, icon effects, and
  micro-shrink rendering interfaces.
- It uses `partMap`, `RedstoneInteractions`, `IRedstonePart`, `NormalOcclusionTest`, `MicroOcclusion.recalcBounds`,
  `onPartChanged`, `remPart`, and tile item drops.

**Migration consequence:** Java compatibility bases must retain the same trait defaults and generated composite
behavior. Test wireless interface dispatch through a composite tile, not just direct calls on the part.

### WitchingGadgets

WitchingGadgets registers multipart essentia tubes and buffers, with block converters and factories.

- Parts extend `McMetaPart`; the tube is slotted, and parts implement Thaumcraft transport/wand interfaces.
- Those external APIs are registered as pass-through interfaces, including the overload carrying client/server
  generation flags.
- Connection logic walks `jPartList` and directly consumes other parts' normal and partial occlusion boxes.
- Rendering goes through `PartMetaAccess`; placement uses `canAddPart`/`addPart`.

**Migration consequence:** preserve flag-sensitive pass-through registration, Java occlusion iterables, slotted part
semantics, and `PartMetaAccess` behavior.

### MatterManipulator

MatterManipulator is the broadest external editor of multipart state.

- Analysis records each part's `getType`, calls `save` into NBT containing `id`, and captures `getDrops`.
- Application removes current parts with `remPart`, sorts microblocks before other parts, reconstructs through
  `MultiPartRegistry.loadPart(type, nbt)`, calls `part.load(nbt)`, and adds through `TileMultipart.addPart`.
- It interprets core microblock type prefixes and NBT fields `material`, `shape`, `side`, `orient`, and `connMap`, and
  transforms exact `PartMap` edge/corner numbering.
- Its mover detaches and reinserts the same tile object, rewrites coordinates, validates it, calls `onMoved`, then
  performs dirty, render, block-update, lighting, and description-packet synchronization.
- Its apply path intentionally does **not** send a redundant description packet: rebuilding the tile a second time
  previously produced null bounds. Packet timing and reconstruction side effects therefore matter.

**Migration consequence:** add capture/apply/move golden tests. This consumer will expose changes in part ordering,
factory semantics, core NBT, movement lifecycle, and over-eager network synchronization.

### GuideNH

GuideNH reconstructs and renders multipart tiles for guide scenes and exports part statistics. Most access is
reflective to keep the integration optional.

- It reflects `MultipartHelper.createTileFromNBT`, `MultipartRenderer.renderWorldBlock`, material registration and
  construction, registry lookup, tile part-list getters/setters, `loadParts`, `notifyTileChange`, `markRender`,
  microblock properties, and part drops.
- `MultipartGenerator$.MODULE$.generateCompositeTile(TileEntity, scala.collection.Iterable, boolean)` remains
  companion-only for that descriptor; a static Java-Iterable entry is now available.
- `MicroblockGenerator$.create(MicroblockClass, int, boolean)` is explicitly selected on the companion and matched by exact parameter
  classes.
- Client preview promotion replaces parts with client microblock instances, assigns
  `partList_$eq(scala.collection.Seq)`, loads/binds them, then triggers tile/render notifications.
- A late mixin targets `BlockMicroMaterial` fields named exactly `block: Block` and `meta: int`. Method reflection is
  only a fallback if an object is not mixin-transformed.

**Migration consequence:** keep both companion entry points, the Scala `partList` setter descriptor, and the private
field names/types unless GuideNH is updated in lockstep. Failures may appear only as missing guide previews or export
data rather than a startup crash.

### Schematica

Schematica's optional integration reconstructs multipart tiles from schematic NBT, especially on the client. It is
more invasive than its small binary footprint suggests.

- It reflects `MultiPartRegistry$.MODULE$` and the **private Scala-mangled field**
  `codechicken$multipart$MultiPartRegistry$$typeMap`, casting it to `scala.collection.mutable.Map`.
- It resolves material IDs, obtains the registered microblock class from that map, and invokes
  `MicroblockClass.create(boolean, int)`.
- It reflects part `load`, `onPartChanged`, `TileMultipart.loadParts`, `TileMultipart.createFromNBT`, and
  `MultipartGenerator$.generateCompositeTile(TileEntity, scala.collection.Iterable, boolean)`.
- It parses tile NBT `parts`, each part's `id`, and microblock `material`; it rejects a preview if any part fails to
  reconstruct.

**Migration consequence:** the registry's private field name **and Scala mutable-map representation are currently
effective public API**. A Java `Map` replacement will silently disable Schematica unless an exact compatibility field
is retained or Schematica is patched and deployed first. This is a higher-risk constraint than the constant-pool
inventory alone shows.

**Current branch:** the Java port initially broke this lookup. `MultiPartRegistry$` now retains the exact private field
as a Scala mutable-map wrapper backed by the canonical Java `HashMap`, and a regression test performs Schematica's
lookup and verifies that both views resolve the same factory.

The branch now provides `MultiPartRegistry.getPartFactory(String)` as the supported replacement for map lookup.
It returns the exact registered factory or null without construction or missing-name logging. Schematica can reflect
this public static method and continue invoking `MicroblockClass.create(client, materialId)` on the returned object;
`loadPart` would incorrectly choose the server factory path for its client preview. The old private field remains.
See the [guide](../api/FACTORY_LOOKUP.md) and the now-available [Java tile generator](../api/COMPOSITE_GENERATION.md);
consumer patches and pack adoption remain pending.

### UtilitiesInExcess - future replacement, already a substantial consumer

UtilitiesInExcess is not in the `+700` JAR scan but is in scope because it is intended to replace Extra Utilities.

- `Content` implements `IPartFactory2` for `ue_fence`, `ue_wall`, and `ue_sphere`, with legacy Extra Utilities type
  aliases when the old mod is absent.
- Parts extend `TMultiPart`, implement Java icon/normal-occlusion interfaces, call the normal occlusion protocol, and
  render through microblock material helpers.
- Placement and connectivity use `canPlacePart`, `addPart`, `BlockMultipart`, neighboring `TileMultipart` instances,
  `jPartList`, and part sub-boxes.
- Recipes and item enumeration consume `MicroMaterialRegistry.getIdMap()` as `scala.Tuple2[]` and use microblock
  class IDs plus `ItemMicroPart.create`.
- Materials use name/ID lookup, packet material ID helpers, remapping, sounds, icons, drops, and rendering. Legacy
  Extra Utilities material names are explicitly remapped for world conversion.
- At audited HEAD, the server factory reads NBT key `material` while `MaterialBasedPart.save` writes `mat`. That is a
  UtilitiesInExcess-side inconsistency to resolve before relying on save/load round trips; FMP should not special-case
  it.

**Migration consequence:** replacing Extra Utilities does not permit early removal of `IPartFactory2`, the Scala
`Tuple2[]` registry view, legacy material remapping, or the Java part/occlusion bridges. UtilitiesInExcess should be
added to compatibility CI before it enters the pack.

## Narrow consumers, still real contracts

| Consumer | Actual use | Compatibility requirement |
| --- | --- | --- |
| BloodArsenal | Registers decorative blocks/metas as `BlockMicroMaterial` instances | Constructor and `registerMaterial`; stable chosen names |
| Botania | Registers decorative blocks/metas as micro-materials | Same material registration API and block/meta behavior |
| Biomes O' Plenty | Registers historical BOP block/metas as micro-materials | Constructor, registry, and its dot-separated material names |
| Natura | Registers single/ranged block metas, using both `createAndRegister` and direct registration | Static helper plus registry API and generated material keys |
| TConstruct | Optional init plugin registers many smeltery/metal/glass blocks with `createAndRegister` | Helper availability and init-time registration behavior |
| Galacticraft | Reflectively constructs `BlockMicroMaterial` and chooses the first public method named `registerMaterial`; separately rejects `TileMultipart` as an energy receiver | Exact class/constructor names; compatible public registration method; tile class identity. Avoid incompatible same-name overloads because it does not check the signature |
| BloodMagic | Teleposer reconstructs multipart tiles with `MultipartHelper.createTileFromNBT` and sends a description packet after placement | NBT reconstruction, placement lifecycle, and `sendDescPacket` |
| Waila | Reflects `BlockMultipart`, reads tile NBT `parts`, then dispatches providers using each part tag's `id` | Block class name and tile/part NBT layout |
| AE2 Fluid Craft | Reflects `TileMultipart` and registers Waila part providers | Tile class name and `jPartList`/provider behavior reached by AE2 |
| GT5U | Detects `TileMultipart`, iterates `jPartList`, and finds a ProjectRed `GatePart` to expose its screwdriver slot | Tile identity, Java list view/order, part instance identity |
| EnderCore | Tests items with `instanceof ItemSaw` for durability display | `ItemSaw` class identity and inheritance |
| IguanaTweaksTConstruct | Creates saws through `MicroblockProxy`, reads saw methods, accesses proxy companions, and reflectively mutates private `ItemSaw.harvestLevel` | Proxy methods and `MODULE$`, `Saw`/`ItemSaw` identity, exact `harvestLevel: int` field name/type until adoption; `setHarvestLevel(int)` is the supported replacement |
| Et Futurum Requiem | Reflectively gets and mutates static `int[] ButtonPart.metaSideMap` and `sideMetaMap` to add floor and ceiling orientation | Exact class and mutable static field names/types until migration; `ButtonPart.setOrientation` is the supported replacement |

## Source-only hazards that must be added to the compatibility checklist

The original constant-pool inventory identified reflective class names, but source inspection adds exact member-level
requirements:

| Consumer | Hidden requirement | Likely symptom if broken |
| --- | --- | --- |
| Schematica | `MultiPartRegistry$.MODULE$`; field `codechicken$multipart$MultiPartRegistry$$typeMap` of Scala mutable-map shape; exact generator/load methods | FMP schematic tile reconstruction disables itself or returns no preview |
| GuideNH | Companion-only generator methods; `partList_$eq(Seq)`; `BlockMicroMaterial.block` and `.meta` mixin fields | Missing/incorrect guide preview, material export, or part statistics |
| Et Futurum | Static mutable `ButtonPart.metaSideMap` and `sideMetaMap`, both `int[]`, until direct API adoption | FMP buttons attach with pre-fix orientation behavior |
| Iguana | Private `ItemSaw.harvestLevel` field until direct setter adoption | Existing saw cutting strengths are not adjusted |
| Galacticraft | Reflection by method name only for `registerMaterial` | Galacticraft micro-material registration is skipped after a caught exception |
| Waila | `BlockMultipart` string plus tile NBT `parts`/part `id` | No multipart HUD data |

These need targeted runtime tests or explicit downstream patches. They cannot be proven safe by `javap` ABI diffing.

**Current branch:** `ConsumerReflectionCompatibilityTest` freezes the GuideNH, Et Futurum, Iguana, and Galacticraft
member shapes above. `ButtonOrientationFunctionalTest` also freezes Et Futurum's exact array mutation and all-face
placement, then verifies the supported typed replacement. `ItemSawCharacterizationTest` runs Iguana's exact boxed
field mutation and the direct setter against shared storage. Schematica's registry lookup has its own live-view
regression test. The manual client checks remain necessary for end-to-end integration behavior.

## Data and ordering contracts

### Core NBT and identity

The following are read by code outside FMP and must be treated as serialized API:

- the tile's `parts` compound list;
- the `id` string in every part compound;
- built-in multipart type strings and registry lookup behavior;
- microblock `material`, `shape`, and orientation fields used by schematic/editor code;
- material names and remapping, including legacy Extra Utilities names;
- invalid/unknown part handling during `loadPart`, tile creation, and preview reconstruction.

Consumer-specific fields such as ProjectRed's `connMap` and UtilitiesInExcess's `mat` belong to their parts. FMP's
obligation is to invoke each part's hooks with the same tag and ordering, not to interpret or rewrite those fields.

### Observable part order

Part list order is not an internal detail:

- ProjectBlue identifies an edited part by its `jPartList` index;
- rendering code in ProjectRed and Extra Utilities indexes `partList` from hit data;
- MatterManipulator deliberately sorts placement, with microblocks first;
- GuideNH and Schematica build an ordered list before generating/loading a composite tile;
- Waila preserves tile NBT order when dispatching part providers.

The Java port may add faster indexed storage, but all published views, serialization, binding, ray-hit indices, and
packet consumers must observe one consistent order.

Current-branch verification: `MultipartHelperFunctionalTest` freezes ordered two-part NBT reconstruction and slot
rebinding plus the exact logical chunk-description framing and part payload. `TileMultipartLifecycleFunctionalTest`
freezes add/remove callbacks and the MatterManipulator-shaped live move, including ordered `onWorldSeparate` before
ordered `onMoved`/`onWorldJoin`, without replacing the generated tile.

### Material IDs and packets

Numeric material IDs are runtime registry identities used in descriptions and custom-part packets. Names are the
persistent identity. Preserve:

- deterministic name-to-ID lookup for one runtime;
- remap behavior for renamed materials;
- `writeMaterialID`/`readMaterialID` wire agreement;
- `materialName`/`materialID` round trips;
- the legacy `getIdMap(): scala.Tuple2[]` view until Extra Utilities and UtilitiesInExcess are migrated.

## Migration gates derived from the audit

Before converting or materially changing each subsystem, add the smallest test that freezes its consumer-visible
behavior:

1. **Registry:** Java `IPartFactory`, `IPartFactory2`, Scala function factory, converter priority, `loadPart`, unknown
   type behavior, stable type strings, and Schematica's compatibility view.
2. **Tile storage:** `partList`, `jPartList`, `partMap`, exact order, slot rebinding, `TIInventoryTile.rebuildSlotMap`,
   add/remove/replace callback and notification order.
3. **Generator:** generated class caching; Java and Scala traits; ProjectRed `LightMicroblock`; pass-through interfaces
   representative of AE2, Extra Utilities, WR-CBE, WitchingGadgets, ForgeRelocationFMP, and UtilitiesInExcess;
   client/server generation and promotion of an existing tile.
4. **Save/load:** a mixed tile golden fixture containing a built-in microblock, ProjectRed part, Extra Utilities or
   UtilitiesInExcess part, AE2 cable bus, OpenComputers part, and a Minecraft meta part. Exercise normal world load,
   BuildCraft builder, MatterManipulator, Schematica client preview, and GuideNH preview.
5. **Movement:** move the same live tile through both ForgeRelocationFMP-style and MatterManipulator-style flows;
   verify coordinates, part tile references, generated interfaces, scheduled state, render/lighting, dirty state, and
   client synchronization after `onMoved`.
6. **Occlusion:** mixed Java/Scala normal and partial occlusion parts, collision-only boxes, `canReplacePart(this,
   this)`, face/edge/center `PartMap`, and the Scala `NormalOcclusionTest` descriptor.
7. **Redstone:** ProjectRed face/framed wire, OpenComputers print, ProjectBlue panel, AE2 cable bus, and WR-CBE part in
   composite tiles; verify neighbor/part-change propagation and generated `TRedstoneTile` behavior.
8. **Reflection:** launch focused checks for Schematica, GuideNH, Et Futurum, Iguana, Galacticraft, Waila, AE2, and
   AE2 Fluid Craft. Assert the exact names and types listed above.
9. **Binary gate:** rebuild and diff all 27 installed JAR consumers against the frozen inventory after every public
   API, trait, generator, registry, or tile change. Add UtilitiesInExcess as a compile/runtime fixture now even though
   it is not yet in the pack.

## Java API adoption ledger

This ledger tracks migration of a specific legacy contract, not completion of an entire consumer's migration.
Factory registration/lookup, material enumeration, tile collection/traversal access, tile loading/storage assignment,
tile occlusion and box queries, render-ID accessors and named tile conversion results are implemented on
`algent/java`. The [API index](../API.md) links the guides and compiling Java examples. These additions are not yet
tied to a released minimum dependency version. Unlisted contracts remain governed by the inventories above.

| Legacy contract | Consumer and inspected source | Supported replacement | Consumer migration/release | Target-pack adoption and removal gate |
| --- | --- | --- | --- | --- |
| `TRedstoneTile.openConnections(int)` | ProjectRed `e173952e96a4`: `transmission/redwires.scala` | Existing stable `IRedstoneTile.openConnections(int)` with the same mask/rotation logic; [guide](../api/TILE_TRAIT_ACCESS.md) | FMP source-access example and Forge coverage complete; consumer source patch/release pending | Retain the old runtime interface descriptor until adoption |
| `TSlottedTile.v_partMap()` array mutation | OpenComputers `2c00f79be24b`: `PrintPart.toggleState` | `TileMultipart.refreshPartSlots(part)`; [equality, cache and notification contract](../api/TILE_TRAIT_ACCESS.md#refreshing-a-changed-slot-mask) | FMP API/example complete; consumer source patch/release pending, preserving validation and following sound/notify/packet/schedule order | Keep the live array/accessor ABI until released pack adoption; do not replace it with reflection or a whole-tile reload |
| External Scala `LightMicroblock` registered through `MicroblockGenerator.registerTrait` | ProjectRed `e173952e96a4`: `illumination/lightmicroblocks.scala` | Java source trait registered by name, `IGeneratedMaterial`, typed sibling traversal and halo helper; [example](../api/MICROBLOCK_EXTENSIONS.md) | FMP example tested; consumer rewrite/release, existing config/halo wiring and physical-client checks pending | Retain external Scala trait ingestion until released-consumer adoption and internal Scala-trait removal |
| `MultiPartRegistry$.registerParts(IPartFactory2, Seq)` and existing array calls | ProjectRed `e173952e96a4`: transmission, expansion and fabrication proxies | `MultiPartRegistry.registerPartFactory` with existing `IPartFactory2` methods and unchanged IDs | Source patch and release pending; Boolean-factory proxies separately need the two-method adapter described in the guide | No migrated pack version verified; retain all registration bridges and external trait support |
| `MultiPartRegistry$.registerParts(Function2, Seq)` | ForgeRelocationFMP `49a810b8c63b`: proxy init registering `rfmp_frame` | `IPartFactory2` creating a fresh `FramePart` on both paths, registered through `registerPartFactory` | Source patch and release pending; preserve `rfmp_frame` and keep converter/pass-through registration | No migrated pack version verified; retain the function companion descriptor |
| `MicroMaterialRegistry.getIdMap(): scala.Tuple2[]` | UtilitiesInExcess `3e107a1fe9bc15fcb6a808242ffda1354dac7c3a`: `FMPRecipeLoader.run`, `UEMultipartItem.getSubItems` | `materialCount()` with `materialName(int)` and, when needed, `getMaterial(int)` | Source patch and first released version pending; checkout used as reference only | No migrated pack version verified; retain the bridge |
| `MicroMaterialRegistry.getIdMap(): scala.Tuple2[]` | Extra Utilities 1.2.12, active supported consumer: NEI and microblock material enumeration | Same ID-based Java enumeration | Keep current compatibility; switching support to UtilitiesInExcess awaits approval and pack adoption | Verify Extra Utilities is absent and its replacement uses the new API before retiring this dependency |
| `TileMultipart.partList(): scala.collection.Seq` | ProjectRed `e173952e96a4`: illumination aggregation, packet indices and rendered-part lookup | `jPartList()` with unchanged indices, filters and aggregation | Source patch and release pending; Scala consumer code may remain Scala | No migrated pack version verified; retain the getter and external trait support |
| `TileMultipart.partList(): scala.collection.Seq` | OpenComputers `2c00f79be24b`: cable/print/network searches and aggregation | `jPartList()` with the same search/aggregation semantics | Source patch and release pending | No migrated pack version verified; retain the getter |
| `TileMultipart.partList(): scala.collection.Seq` | AE2 `87f2b3817c2a`: `FMPPlacementHelper.getPart` and `removePart` | Iterate `jPartList()`; retain last-match lookup and removal/break behavior | Source patch and release pending | No migrated pack version verified; retain the getter |
| `TileMultipart.partList(): scala.collection.Seq` | Extra Utilities 1.2.12: multipart renderer iterators | Iterate `jPartList()` without adding detached-part filtering | Retirement/replacement pending | Confirm absence or migration in the target pack before retiring the getter |
| `TileMultipart.partList(): scala.collection.Seq` plus reflective getter/setter/loading | GuideNH `7d8fb44e77b9`: `Ae2ForgeMultipartBridge`, `ForgeMultipartHelpers` | `jPartList()` for reads, `setPartList(List)` for staging, `loadPartList(Collection)` for binding/cache reconstruction | Source patch and release pending; preserve world/position setup and following tile/render notifications | Retain the legacy getter/setter/loader until adoption; companion-only generator migration is separate |
| `TileMultipart.loadParts(scala.collection.Iterable)` exact reflection | Schematica `3b03ee937953`: `nbt.ForgeMultipart` | Call `loadPartList(parts)` directly with its existing Java part list | Source patch and release pending; registry lookup and Java tile generation available separately | No migrated pack version verified; retain the Scala loader descriptor |
| Private `MultiPartRegistry$` Scala `typeMap` field and `Map.get` / `Option` | Schematica `3b03ee937953`: `nbt.ForgeMultipart.init` and `createPart` | Call public static `MultiPartRegistry.getPartFactory(partID)` directly; keep material lookup, `MicroblockClass.create(client, materialId)` and whole-preview rejection | Source patch/release pending; lookup and Java tile generation implemented | No migrated pack version verified; retain the exact private live Scala-map field |
| `MultipartGenerator$.generateCompositeTile(TileEntity, scala.collection.Iterable, boolean)` | Schematica `3b03ee937953` exact reflection; GuideNH `7d8fb44e77b9` static-first assignability matcher | Static `MultipartGenerator.generateCompositeTile(TileEntity, java.lang.Iterable, boolean)` with Java parts; retain subsequent state setup/loading | Source patches/releases pending; preserve client part construction, candidate-reuse branch and notification order | No migrated pack version verified; retain the companion and Scala descriptor |
| `MicroblockGenerator$.create(MicroblockClass, int, boolean)` exact reflection and private shape copy | GuideNH `7d8fb44e77b9`: `getMicroblockGeneratorCreate`, `promoteMicroblockToClient` | Existing static `MicroblockGenerator.create` with the same parameter types/order; recreate family/material and restore encoded shape through public API | Source patch/release pending; guide/example tested on server, physical client and custom shape-setter overrides require adoption checks | No migrated pack version verified; retain companion singleton and exact descriptor |
| Private `BlockMicroMaterial.block` / `meta` accessor mixin and reflective material query | GuideNH `7d8fb44e77b9`: `AccessorBlockMicroMaterial`, `resolvePrimaryMicroblockId` | Direct `jPartList()`, `Microblock.material()`, `getMaterial(int)`, `BlockMicroMaterial.block()` / `meta()`; [typed example](../api/MATERIAL_ACCESS.md) | Source patch/release pending; remove mixin, retain export filtering/failure policy, validate getter overrides and optional loading | No migrated pack version verified; retain private fields |
| Reflective mutation of `ButtonPart.metaSideMap` / `sideMetaMap` | Et Futurum Requiem `78a5744dfd33`: `compat.CompatMisc.runModHooksInit` | Two direct `ButtonPart.setOrientation(int, ForgeDirection)` calls; [mapping and lifecycle guide](../api/BUTTON_ORIENTATIONS.md) | FMP API/example complete; consumer source patch and first released version pending | Retain both public mutable arrays until the migrated release is in the target pack and a fresh scan confirms no legacy access |
| Reflective mutation of private `ItemSaw.harvestLevel` | IguanaTweaksTConstruct `2bc09889d3e2`: `modcompat.fmp.IguanaFMPCompat.postInit` | Read `harvestLevel()` and call `setHarvestLevel(int)`; [state and lifecycle guide](../api/SAW_STRENGTH.md) | FMP API/example complete; consumer source/lifecycle patch and first released version pending | Retain the private field until the migrated release is in the target pack; ensure Iguana runs before ForgeMicroblock post-init and rescan for legacy access |
| `NormalOcclusionTest$.apply(Traversable, Traversable)` | OpenComputers `2c00f79be24b`: `common.block.Cable.canConnectFromSideFMP`, `server.network.Network.canConnectFromSideFMP` | `NormalOcclusionTest.testBoxes(ownBounds.asJava, otherBounds)`; `otherBounds` already comes from Java `getOcclusionBoxes()` | Source patch and release pending; retain side/color/face filtering | No migrated pack version verified; retain the companion and descriptor |
| `NormalOcclusionTest$.apply(Traversable, Traversable)` | ForgeRelocationFMP `49a810b8c63b`: `FramePart.occlusionTest` | `NormalOcclusionTest.testBoxes(boxes.asJava, getOcclusionBoxes)`; retain combined normal/partial/collision boxes and caller order | Source patch and release pending; preserve temporary face bounds and replacement protocol | No migrated pack version verified; retain the companion and descriptor |

Button orientation evidence is under ignored `run/migration-button-orientation-reference/`. Baseline commit `cd13ad6`
freezes Et Futurum's exact four array writes and all-face placement before the API addition. The supported setter keeps
the two maps one-to-one and validates inputs, while the Java example compiles against the dev artifact without Scala or
reflection. The reference checkout remains unchanged; no consumer migration, release or target-pack adoption is claimed.

Saw-strength evidence is under ignored `run/migration-saw-strength-reference/`. Baseline commit `098e747` reproduces
Iguana's boxed reflective field mutation before the API addition. The supported setter updates the same value used by
the getter, recipes and renderer without changing durability; the private field remains for old releases. Iguana's
source patch must also order its post-init before ForgeMicroblock's cached maximum calculation. The reference checkout
remains unchanged; no consumer migration, release or target-pack adoption is claimed.

Evidence for the FMP addition is under ignored `run/migration-material-enumeration-reference/`. The original
reference-compiled Scala consumer still exercises the companion and tuple-array descriptor. The new compiling Java
example has no Scala imports or bytecode references. Runtime checks cover initialized ID lookup and handshake order;
they do not establish that an updated UtilitiesInExcess release has shipped or entered the pack.

FMP's own `ItemMicroPart` and `MicroRecipe$` still use the legacy array internally. Their migration, any additional
retained companion users, and the other Scala-facing contracts remain separate removal gates.

Tile traversal evidence is under ignored `run/migration-part-traversal-reference/`. Re-scanning the supplied Java/Scala
sources found no FMP `operate` calls/overrides or `forEachPart` name collisions; two unrelated no-argument renderer
`operate()` implementations were excluded. The existing `operate(Function1)` override hook nevertheless remains
supported. The Java convenience delegates through it, while lifecycle callbacks continue calling it directly.
Deprecation is caller guidance, not permission to remove or bypass existing overrides. All 538 frozen pre-change JVM
tests, including the legacy getter/callback subclass cases, run against the addition without recompilation.

Existing `jPartList()` consumers such as ProjectBlue, BuildCraftCompat, GT5U, WitchingGadgets, MatterManipulator and
UtilitiesInExcess already have Java collection access. They need no rename for this contract. The new `forEachPart`
convenience is not a blanket replacement for their loops: its detached-part filtering can change read/query behavior.
The reference checkouts were not edited, built or counted as released migrations.

The installed GTNH daily `2026-09-04+719` rescan scanned 241 jars and excluded one FMP jar. Its 27 consumers still
reference the exact same 35 inherited types, 255 members, 76 other types and 20 reflection strings as the frozen
`+678` inventory, compared by full row rather than counts alone. The report is archived with the traversal evidence;
source revisions above remain the inspected checkout revisions, not a claim of source parity with every newer jar.

Loading/storage evidence is under ignored `run/migration-part-loading-reference/`. The supplied source search found
no FMP loader/setter overrides or `setPartList`/`loadPartList` name collisions; GuideNH's assignability-based selection and Schematica's
exact Scala-parameter lookup remain pinned. The Java setter copies non-null list storage while the old setter retains
its exact sequence; neither binds parts. The loader retains callback order, client/server branching and partial
failure behavior. All 546 pre-change compiled JVM tests pass with their recorded version; generated tile checks cover
server slot rebuilding/notifications and client storage/loading/render-cache queries. Actual client preview rendering,
consumer release and pack adoption are still separate gates.

Tile collection occlusion is implemented as `testOcclusion(Collection<? extends TMultiPart>, TMultiPart)`, documented
in the [occlusion guide](../api/OCCLUSION.md). The supplied Java/Scala source search found no direct consumer calls or
overrides of the old two-argument tile `occlusionTest` and no `testOcclusion` name collisions. The frozen ABI floor
likewise contains no direct reference to that tile descriptor. The legacy hook is nevertheless required by generated
`TPartialOcclusionTile` behavior and by existing tile placement/replacement dispatch; it remains supported.

ProjectRed and ForgeRelocationFMP's `canReplacePart` callers need no rename and must retain outgoing-part exclusion.
The new collection query is not a blanket replacement for placement checks or the part-level `occlusionTest` hook.
`NormalOcclusionTest$.apply(Traversable, Traversable)` remains a separate box-list bridge used by ForgeRelocationFMP
and OpenComputers. Its `testBoxes(Iterable, Iterable)` replacement is now implemented; adoption of either query does
not retire the other contract.
Evidence for this slice is under ignored `run/migration-part-occlusion-reference/`; reference checkouts remain unchanged.

The box-query slice retains eager left-then-right input collection, shallow snapshots, ordered/short-circuit intersection
calls, touching tolerance and original input/callback exceptions. Both legacy entries are deprecated without changing
their descriptors or bodies. The Java example compiles with Scala excluded; the shared private copy helper only widens
its generic input to accept box subclasses. The source check found the three box-list calls listed above and no
`testBoxes` collision. The installed `+719` rescan retains all 386 member/type/reflection rows from `+678` across 27
consumers. Evidence: `run/migration-box-occlusion-reference/`; [guide](../api/OCCLUSION.md#box-versus-box-queries).

Factory registration is implemented as `registerPartFactory(IPartFactory2, String...)`, with a
[guide and compiling example](../api/PART_REGISTRATION.md). A probe against the old Java overload family required
Scala `Seq`/`Function2` even for Java arrays and factories; the new name compiles without Scala. All eight legacy
static/companion entries retain their bodies/descriptors. Both `IPartFactory2` sequence entries are deprecated, and
the older Boolean/function adapter deprecations now identify the supported replacement.

Four baseline Forge cases register through the legacy APIs during real initialization, then check ownership,
lazy construction, duplicate-prefix retention, array ownership, failure order and both NBT/packet paths. The new
entry runs through the same checks plus an example case. All 563 archived JVM tests and the byte-identical archived
Forge test mod's 247 cases pass against the new implementation. The `+719` rescan retains all 386 ABI/reflection rows
across 27 consumers. Schematica's private registry-map dependency is separate and remains pending; the supplied
checkouts remain unchanged. Evidence: `run/migration-registration-reference/`.

Render-ID access is implemented as `TileMultipart.getRenderID()` / `setRenderID(int)`, with a
[lifecycle guide](../api/RENDER_ID.md). No direct use of the four old static/companion accessors or new-name collision
was found in FMP-related consumer source, and the frozen member inventory contains none of those descriptors.
Internal block/renderer calls retain the old paths and share the same global value; all four legacy entries remain
with deprecations. This addition does not replace GuideNH's renderer reflection or allocate/register a renderer.
JVM and dedicated-server checks preserve the -1 sentinel, unrestricted integer assignment and shared block state;
physical-client rendering remains a separate gate. Evidence: `run/migration-render-id-reference/`.

Tile conversion is available as `TileMultipart.getOrConvertTileResult(World, BlockCoord)`, returning an immutable
`TileConversionResult` with `getTile()` / `isConverted()`; see the [guide](../api/TILE_CONVERSION.md). Both legacy
static/companion tuple methods remain with deprecations and unchanged bodies/descriptors. The new API wraps their
existing path, retaining existing-tile identity, converter dispatch and uninstalled-placeholder behavior.

The supplied consumer source search found no `getOrConvertTile2` caller or new-name collision. The frozen member
inventory likewise has no direct tuple-method reference. AE2, Chisel, OpenComputers and Extra Utilities already call
the supported tile-only `getOrConvertTile`; they need no rename for this contract. Internal generator and microblock
placement tuple calls remain, including `MicroblockPlacement.gtile()`; their migration is a separate Scala-removal
gate. All 566 archived JVM tests and the unchanged archived Forge test mod's 252 cases pass against this addition.
The `+719` rescan retains all 386 member/type/reflection rows across 27 consumers. Reference checkouts remain unchanged.
Evidence: `run/migration-tile-conversion-reference/`.

Registered factory lookup is implemented as `MultiPartRegistry.getPartFactory(String)`, with the same identity and
current-map behavior as Schematica's reflected lookup. No new-name collision was found in the supplied Java/Scala
sources. The Java example compiles without Scala; exact public reflection is exercised in Forge. Both lookup paths
are checked without factory callbacks, including late mapping changes, equal names and misses. Forge additionally
checks all five built-in microblock factories, server construction and NBT loading; physical-client construction and
preview rendering remain manual because the dedicated server strips `MicroblockClass.clientTrait()`.
All 567 archived JVM tests and the byte-identical archived Forge mod's 256 cases pass against this addition. The
`+719` scan retains all 386 member/type/reflection rows across 27 consumers. The private map retains its exact binary
shape and live backing. Reference checkouts remain unchanged. Evidence: `run/migration-factory-lookup-reference/`.

Staged tile generation is available through static `MultipartGenerator.generateCompositeTile(TileEntity,
java.lang.Iterable<TMultiPart>, boolean)`. The adapter passes a view to the unchanged companion method, now deprecated
for callers. Both sides, exact candidate reuse, empty/duplicate input, no implicit copying/loading/installation,
input traversal and failures are characterized. Old exact reflection and GuideNH's Scala-argument fallback still work;
Java reflection and the example compile/use the Java parameter. The `+719` scan retains all 386 rows across 27 consumers.
Archived validation runs 567 JVM tests (one obsolete exact facade inventory assertion excluded) and all 261 archived
Forge tests; the full current suite and independent ABI comparison check the intentional method addition. Client tile
generation/worldless loading is covered, while physical-client microblock construction and GPU previews remain manual.
Reference checkouts and consumer adoption remain unchanged. Evidence: `run/migration-composite-generation-reference/`.

GuideNH's microblock creation contract is documented at [microblock creation](../api/MICROBLOCK_CREATION.md). The
static Java method already existed, so this slice adds Javadocs, tests and an example with no production API/body
changes. GuideNH hardcodes the companion owner and caches its singleton; its source must switch both to use the static
method. It copies only shape after recreation, not arbitrary custom state or binding. The Java example preserves
all 256 shape bytes for a stock face microblock through `setShape`, which uses virtual setter dispatch unlike the old
private-field write; custom override validation remains a consumer gate.

The example captures factory, material and shape before generation, matching GuideNH's read order. A regression
callback that mutates the source shape proves the copied value is the pre-callback value, not a later reread.

Existing generated-material coverage proves the external Scala trait is attached before construction. New tests pin
exact static/companion reflection, fresh instances, factory/material identity, caller-owned NBT/shape and failure
propagation with reused scratch state. The example compiles without Scala. All 568 archived JVM tests and 267 tests
from the unchanged archived Forge mod pass. Physical-client creation/GPU previews remain manual; the dedicated
server strips the client's factory method. All 386 pack ABI/reflection rows remain across 27 consumers, and reference
checkouts are unchanged. Evidence: `run/migration-microblock-creation-reference/`.

GuideNH's block/meta query now has a [typed example and contract](../api/MATERIAL_ACCESS.md) using existing public
methods throughout. The baseline pins constructor identity/raw metadata and the distinction between virtual getters
and raw mixin fields. Forge exercises first-usable-part ordering, non-block/invalid-block filtering, registry aliases,
metadata formatting and failure propagation. Getter overrides are intentional public semantics; the old mixin bypasses
them while its reflective fallback respects them. No new accessors or production behavior changes were needed.
Consumer migrations should use direct calls, including optional integrations gated on presence/version. Reflection
snippets remain legacy interoperability options only. Supplied consumer checkouts remain reference-only.
Evidence: `run/migration-material-access-reference/`; archived validation retains 570 JVM and 270 Forge cases.

## API boundary audit

Rechecked 2026-09-05 across 28 source checkouts and active Extra Utilities compatibility. Searches cover method
names, FMP-importing Java/Scala source, reflective names and the installed `+719` constant-pool inventory. No external
calls were found to the 15 tile/material-registry hooks listed in [Phase 9.2](README.md#92--mark-the-internal-boundary).
The registry's five companion forwarders carry matching Javadocs. Common-name matches included GuideNH comments
explicitly avoiding `from` / `copyFrom` and WR-CBE's unrelated `RenderWireless.loadIcons`; neither calls these hooks.
This is an audit of the supplied references, not proof about every possible external mod or dynamic lookup.

The supported exceptions are actual calls: OpenComputers `2c00f79be24b`, `PrintPart.scala:171`, clears the old slot
entries before `tile.bindPart(this)`; ProjectRed `e173952e96a4`, `gatepartrs.scala:74`, calls
`tile.internalPartChange(this)` between its own packet/dirty work and selective external-neighbor notification.
The [API guide](../API.md#supported-api-and-internal-hooks) preserves both contracts. The old `bindPart` comment's
blanket prohibition on external calls was incorrect. Cache binding does not place/rebind a part, and it is not a
universal idempotent refresh; local part notification does not implicitly perform world updates.

One new JVM baseline pins captured traversal, detachment/rebinding, callback failure propagation and worldless local
notification. One new Forge baseline pins cache-only binding and the old-slot clearing requirement. Existing equality,
loading, generated-trait, registry and handshake fixtures remain in the full suite. No API/member/behavior changes or
new deprecations are introduced; `operate`, `getOrConvertTile2` and material `loadIcons` keep their distinct supported
legacy/extension contracts. No consumer checkout was modified or counted as migrated.
Evidence: `run/migration-api-boundary-reference/`, including source revisions and reviewed caller searches.

## Java illuminated microblock extension

The ProjectRed-style [Java example](../api/MICROBLOCK_EXTENSIONS.md) uses public name-based registration and
`IGeneratedMaterial`; no consumer reflection or Scala classpath is needed. Register the input before class loading,
keep inherited access in an ordinary helper with Object parameters, and dispatch via the stable Microblock base.
The example preserves metadata 16–31, cutter metadata modulo 16, all-shape trait selection, strict sibling selection,
size-based light rounding/capping, pass-zero halo submission and the hollow opening's trimmed strips. It adds no
part NBT or packet fields. A distinct test lamp supplies the fixture; stock materials keep their exact assertions.

The baseline extends retained external Scala-trait composition to all five built-in shape families. Four added Forge
cases cover Java registration/shape composition, material persistence, light behavior, halo geometry and a retained
client-body compiler probe. That probe removes only the test input's side annotation to exercise generated dispatch
headlessly; it is not physical-client evidence. Actual client construction/rendering and consumer configuration/halo
integration remain adoption checks. Reference checkouts and released-consumer status are unchanged.
Evidence: `run/migration-illuminated-extension-reference/`; frozen consumers retain 571 JVM and 274 Forge cases.

## Practical priority for the current branch

1. Keep the existing `+678` ABI fixture as the exact binary floor and correct failures before source cleanup.
2. Add the hidden-member reflection checks, especially Schematica's `typeMap` compatibility and GuideNH's mixin
   fields, before converting their owners.
3. Keep the completed `TileMultipart` list/map/slot and live-move characterization green before optimizing storage or
   iteration.
4. Keep the completed pass-through and ProjectRed-shaped external Scala-trait characterization green before changing
   either generator.
5. Keep the completed compact mixed NBT/chunk-description fixture green; add subsystem-specific payload cases when
   converting microblocks or Minecraft part implementations.
6. Treat Scala runtime removal as downstream coordination work after the Java port, not as a requirement of it.

Current-branch generator verification: `ForgeEnvironmentSmokeTest` pins exact direct-Java-source `TSlottedTile`
interface/field generation, initialization, rebinding and caching, an external Scala microblock trait registered by
name like ProjectRed's `LightMicroblock`, direct-Java-source `TPartialOcclusionTile` rewriting and override dispatch,
direct-Java-source `TRedstoneTile`'s exact eight-method interface and class caching, and server-only pass-through
forwarding, overloading, single-implementor selection, copying/rebinding, and removal cleanup.
`TRedstoneTileFunctionalTest` additionally pins the ProjectRed/Extra Utilities query surface, masks, conduction, and
world-side routing. `JavaTraitRegistrationFunctionalTest` pins current/opposite-side selection for visible and
invisible Java annotations, including method dispatch, field state and constructor initialization.
`MicroblockTraitsCharacterizationTest` executes a frozen concrete Scala consumer through all three common microblock
helper bridges, covering slots, partial-box list behavior, material/render routing and particles. Its Forge companion
covers generated common microblocks; the runtime interfaces used by Extra Utilities, ForgeRelocationFMP and GuideNH
remain intact. `FaceMicroblockTraitsCharacterizationTest` additionally executes a frozen Scala face-client forwarder
and checks every emitted face for all masks/slots, including material reuse and repeated virtual slot lookup between
opaque draws. Its Forge companion pins generated face bounds and solidity. The `FaceMicroblockClient` interface
tested by Extra Utilities' connected-texture renderer remains intact. Corner, edge and post tests freeze virtual
access, every supported geometry, packed shape/material NBT and descriptions, edge conduction and post occlusion
ordering. The post's frozen Scala superclass also pins its synthetic super dispatch, including short-circuiting.
Post-client fixtures additionally exercise render/lifecycle dispatch, equality receivers, repeated virtual reads and
ordered neighbour shrinking; Forge uses real cover/post geometry while client generation and GPU output remain manual.
Full downstream mixed tiles and client-side selection remain Phase 7 integration cases.

With these gates, the source audit is actionable: the port can simplify internals freely where no listed observation
changes, while each externally observed behavior has a named consumer and a focused way to prove compatibility.

## Java block converter guidance

The existing `IPartConverter` and static `registerConverter` now have a [Java guide and compiling example](../api/BLOCK_CONVERTERS.md).
No production descriptor or method body changed. Reference calls confirm Chisel's block/metadata selection,
ForgeRelocationFMP's frame construction, OpenComputers' original cable/print tile lookup and AE2's cable-bus state copy.
Their converter registrations need no replacement API; companion users can select the existing static entry.
Keep separate factory registrations and published IDs, and preserve consumer-specific state-transfer/cleanup rules.
Extra Utilities remains an active supported converter consumer. Reference checkouts were neither changed nor adopted.

Two new JVM tests characterize registration snapshots/duplicates and exception propagation. A Forge baseline pins
fresh candidates, rejected probes, original-tile ownership and invalidate/replace/onConverted/onAdded ordering.
The example adds initialized converter/factory registration and NBT/packet/placement coverage, compiled without Scala.
Evidence: `run/migration-converter-reference/`; physical-client conversion and real inventory/network transfers still
require integration validation. This is FMP API coverage, not a released consumer migration.

## Stable Java access to transformed tile traits

The [tile access guide](../api/TILE_TRAIT_ACCESS.md) distinguishes raw dev-jar classes from runtime interfaces.
Actual javac callers reproduce `IncompatibleClassChangeError` for `TRedstoneTile.openConnections` and `NoSuchFieldError`
for `TSlottedTile.v_partMap`; the same generated tiles work through `IRedstoneTile` and `TileMultipart.partMap`.
`IRedstoneTile` is now documented as a supported capability, correcting its former internal label without changing
its binary shape or behavior. The Java example compiles without Scala and calls the stable interface.

ProjectRed needs only a cast-owner change for its open-connection query; preserve the surrounding rotation/mask
calculation. OpenComputers can replace its slot-array cast/loop plus `bindPart` call with
`tile.refreshPartSlots(this)`, preserving its preceding validation/state change and following sound, notification,
description and scheduling order. FMP clears value-equal entries from the live array, then dispatches the virtual bind
chain once; no storage, ownership or notification work is added. Reference checkouts remain unchanged and no
release/adoption is recorded. Evidence: `run/migration-tile-trait-access-reference/` and
`run/migration-slot-refresh-reference/`; physical-client checks remain.

## Custom Java tile-trait authoring

The [authoring guide](../api/CUSTOM_TILE_TRAITS.md) now covers the existing name-based
`MultipartGenerator.registerTrait` path with a top-level Java input, separate marker and stable capability interfaces,
an ordinary helper and no reflection. The Forge example exercises both side selections, transformed dispatch, binding,
exact tile reuse and generated-class caching. It owns no state and derives its aggregate from the live part list;
stateful extensions retain the documented copy, persistence, synchronization and lifecycle responsibilities.

The supplied consumer source audit found no direct custom multipart tile-trait registration. Consumers currently use
built-in traits, generated pass-through interfaces, or ProjectRed's separate microblock-trait registration path.
Accordingly this is supported extension coverage, not a consumer migration or a basis for retiring any current binary
contract. Evidence: `run/migration-custom-tile-trait-reference/`.
