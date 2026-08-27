# Source-level downstream consumer audit

Status: 2026-08-27. This is the source-level companion to
[`JAVA_MIGRATION_ABI_INVENTORY.md`](JAVA_MIGRATION_ABI_INVENTORY.md).

The ABI inventory answers "what must still link?" This audit answers the harder question: "what behavior do real
consumers expect after they have linked?" It covers all 27 ForgeMultipart consumers in the supplied GTNH daily
`2026-08-25+700` instance, plus UtilitiesInExcess as the planned Extra Utilities replacement.

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
binary API family appeared. The source audit then searched every clone and the Extra Utilities jar for direct
imports, fully qualified names, reflection strings, factories, converters, part subclasses, tile access, registry
access, NBT, packet, rendering, occlusion, redstone, and movement paths. Important call sites were traced through the
owning code instead of counting imports.

This gives full coverage of the current pack's known source and binary consumers. It does not claim that an arbitrary
future mod, a runtime-generated script, or an unpublished patched JAR cannot contain another reflective use. The ABI
scanner should remain a release gate for exactly that reason.

### Source provenance

| Installed consumer | Source used | Match to installed code                                   |
| --- | --- |-----------------------------------------------------------|
| ProjRed `4.12.43-GTNH` | `ProjectRed` at `4.12.43-GTNH` (`e173952e96a4`) | Exact                                                     |
| Extra Utilities `1.2.12` | `extrautilities-1.2.12` | Exacted JAR; not original source                          |
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
| Placement/removal/replacement | Most custom-part mods | `getTile`/`getOrConvertTile`, `canPlacePart`, `canAddPart`, `canReplacePart`, `addPart`, `remPart`, binding and notifications in their current order |
| Move lifecycle | ForgeRelocationFMP, MatterManipulator | A live `TileMultipart` can be detached/reinserted, coordinates rewritten, then `onMoved`; render, dirty, lighting, and description synchronization remain coherent |
| Occlusion and geometry | ProjectRed, Extra Utilities, OpenComputers, AE2, ForgeRelocationFMP, Chisel, WR-CBE, WitchingGadgets, UtilitiesInExcess | Normal and partial occlusion, collision/subpart boxes, `PartMap` numbering, Java iterable adapters, Scala `Traversable` overload |
| Redstone and connectivity | ProjectRed, Extra Utilities, OpenComputers, ProjectBlue, AE2, WR-CBE | `IRedstonePart` family, `TRedstoneTile`, `RedstoneInteractions`, face/edge/center maps, neighbor and part-change propagation |
| Micro-material registry | Extra Utilities, ProjectRed, ProjectBlue, AE2, BuildCraftCompat, Chisel, material-only integrations, UtilitiesInExcess | Stable material names, integer IDs, remapping, packet ID encoding, `getIdMap(): Tuple2[]`, block/meta access, material behavior hooks |
| Multipart save/load | Extra Utilities, ProjectRed, OpenComputers, BuildCraftCompat, MatterManipulator, GuideNH, Schematica, Waila, UtilitiesInExcess | Tile `parts` list, per-part `id`, factory lookup, `save`/`load` ordering, microblock fields, unknown/invalid-part behavior |
| Description/update packets | Every substantial custom part integration | `writeDesc`/`readDesc`, keyed part packets, `sendDescUpdate`, `sendDescPacket`, material packet IDs, render and neighbor notification behavior |
| Reflection/mixins | GuideNH, Schematica, Et Futurum, Galacticraft, Waila, AE2/AE2FC, Iguana | Exact class/companion/member/field names and, where stated below, exact parameter and field types |

## Consumer findings

### ProjectRed — deepest current consumer

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

### Extra Utilities 1.2.12 — broad Java consumer with Scala-shaped registry access

The code shows several separate FMP subsystems, not one compatibility class:

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

### OpenComputers — generated slot state is part of the effective API

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

### ProjectBlue — Java parts built on Scala shim bases

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
- `MultipartGenerator$.MODULE$.generateCompositeTile(TileEntity, Iterable, boolean)` is companion-only.
- `MicroblockGenerator$.create(MicroblockClass, int, boolean)` is companion-only and matched by exact parameter
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

**Current branch:** this is already broken by the Java `MultiPartRegistry` port. The canonical state is now a private
Java `HashMap` on `MultiPartRegistry`, while `MultiPartRegistry$` has no field with the reflected name. Restoring and
testing a compatibility view is the immediate migration blocker.

### UtilitiesInExcess — future replacement, already a substantial consumer

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
| IguanaTweaksTConstruct | Creates saws through `MicroblockProxy`, reads saw methods, accesses proxy companions, and reflectively mutates private `ItemSaw.harvestLevel` | Proxy methods and `MODULE$`, `Saw`/`ItemSaw` identity, exact `harvestLevel: int` field name/type |
| Et Futurum Requiem | Reflectively gets and mutates static `int[] ButtonPart.metaSideMap` and `sideMetaMap` to repair button orientation | Exact class and mutable static field names/types; array index meanings |

## Source-only hazards that must be added to the compatibility checklist

The original constant-pool inventory identified reflective class names, but source inspection adds exact member-level
requirements:

| Consumer | Hidden requirement | Likely symptom if broken |
| --- | --- | --- |
| Schematica | `MultiPartRegistry$.MODULE$`; field `codechicken$multipart$MultiPartRegistry$$typeMap` of Scala mutable-map shape; exact generator/load methods | FMP schematic tile reconstruction disables itself or returns no preview |
| GuideNH | Companion-only generator methods; `partList_$eq(Seq)`; `BlockMicroMaterial.block` and `.meta` mixin fields | Missing/incorrect guide preview, material export, or part statistics |
| Et Futurum | Static mutable `ButtonPart.metaSideMap` and `sideMetaMap`, both `int[]` | FMP buttons attach with pre-fix orientation behavior |
| Iguana | Private `ItemSaw.harvestLevel` field | Existing saw cutting strengths are not adjusted |
| Galacticraft | Reflection by method name only for `registerMaterial` | Galacticraft micro-material registration is skipped after a caught exception |
| Waila | `BlockMultipart` string plus tile NBT `parts`/part `id` | No multipart HUD data |

These need targeted runtime tests or explicit downstream patches. They cannot be proven safe by `javap` ABI diffing.

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

## Practical priority for the current branch

1. Keep the existing `+678` ABI fixture as the exact binary floor and correct failures before source cleanup.
2. Add the hidden-member reflection checks, especially Schematica's `typeMap` compatibility and GuideNH's mixin
   fields, before converting their owners.
3. Characterize `TileMultipart` list/map/slot behavior and move lifecycle before optimizing storage or iteration.
4. Characterize pass-through interfaces and ProjectRed's external Scala trait before changing either generator.
5. Add mixed NBT/packet fixtures before converting microblocks and Minecraft part implementations.
6. Treat Scala runtime removal as downstream coordination work after the Java port, not as a requirement of it.

With these gates, the source audit is actionable: the port can simplify internals freely where no listed observation
changes, while each externally observed behavior has a named consumer and a focused way to prove compatibility.
