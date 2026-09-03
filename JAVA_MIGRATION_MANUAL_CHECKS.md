# Manual compatibility checklist

Behavior that the automated layers cannot assert. Plain-JVM tests cannot reach a world, a client or a renderer, and
the Forge server suite is headless, so everything below needs a human in a running client unless stated otherwise.

Each entry names what to do, what should happen, and which port put it here. Run the relevant entries before any
release that includes those ports.

Change `[ ]` to `[x]` after a successful check. If a check fails, leave it unchecked and add a dated note immediately
below its table.

Item examples below use English NEI names, checked against the supplied consumer clones' `en_US.lang` files and
registration code on 2026-09-03. Use the mod name to distinguish similarly named items. Sizes/colours add suffixes;
an OpenComputers print can have a custom label. Record the tested FMP build, pack version and item/setup with results.

For ProjectRed's external Scala-trait path, start with **Inverted White Lamp** (or another inverted colour), then saw
it into microblocks. `LightMicroMaterial.register` registers only lamp metadata 16–31; ordinary lamps and the
**White Illumar** crafting ingredient do not exercise this path. Useful resulting names include **Inverted White
Lamp Cover**, **Hollow Inverted White Lamp Cover**, **Inverted White Lamp Nook**, **Inverted White Lamp Strip** and
**Inverted White Lamp Post**.

## Rendering and particles

| Done | Check | Expected | From |
| --- | --- | --- | --- |
| [ ] | Break a cover or microblock part and watch the break overlay | The breaking texture is drawn on the part's own cuboid, not the full block | `TCuboidPart.drawBreaking` |
| [ ] | Hit a part repeatedly without breaking it | Hit particles spawn on the struck face, using that face's icon | `IconHitEffects.addHitEffects` |
| [ ] | Break a part fully | Destroy particles use all six side icons and are scaled to the part's bounds | `IconHitEffects.addDestroyEffects` |
| [ ] | Break a hollow cover | Destroy particles use the full block bounds, not the part bounds | `addDestroyEffects` scaleDensity false |
| [ ] | Hover face, hollow, edge and corner microblock placement targets | The highlight and guide lines match each placement region, and a mod supplying its own highlight renderer overrides them | `PlacementGrid` / `MicroblockRender.renderHighlight` / `MicroblockEventHandler.drawBlockHighlight` / `MicroMaterialRegistry.renderHighlight` |
| [ ] | Reload client resources after placing several microblock materials | The texture atlas reloads every material icon without missing textures | `MicroblockEventHandler.postTextureStitch` / `BlockMicroMaterial.loadIcons` |
| [ ] | Look at a **Stone Saw**, **Iron Saw** and **Diamond Saw** in inventory and in hand | The saw item renders with its custom transform | `ItemSaw` `IItemRenderer` |
| [ ] | Place microblocks of several materials, including glass | Transparent materials render in the correct pass and are not opaque | `BlockMicroMaterial` / `IMicroMaterial.canRenderInPass` |
| [ ] | Place grass and mycelium covers at several thicknesses and biomes | Grass has an untinted base plus a tinted, height-aligned side overlay and tinted top; mycelium uses its top texture on horizontal faces and height-aligned side texture elsewhere | `GrassMicroMaterial` / `TopMicroMaterial` |
| [ ] | Load a saved microblock whose material is no longer installed | The part remains present and renders the magenta/black missing texture instead of becoming material ID 0 | `MissingMicroMaterial` |
| [ ] | Put a **Stone Cover** and a ProjectRed **Inverted White Lamp Cover** on opposite faces of one block space, then add/remove another part | Static parts remain in the block render, dynamic parts remain in the TESR pass, and neither disappears after a part update | `TileMultipartClient` render caches |
| [ ] | Watch a multipart torch long enough to emit display particles | Only parts implementing `IRandomDisplayTick` emit particles, with no duplicate or missing callbacks | `TRandomDisplayTickTile.randomDisplayTick` |
| [ ] | Start a client, join a server, place/update a multipart and use the multipart control key | Block and generated-tile rendering work, client packets arrive, and control-key state changes are sent once without duplicate registrations | `MultipartProxy_clientImpl.postInit` / `onTileClassBuilt` |
| [ ] | Move into view of a chunk containing several multipart tiles, then add, update and remove parts | The initial chunk description reconstructs every tile and compressed updates apply without missing, duplicated or ghost parts | `MultipartCPH` / `MultipartSPH` |
| [ ] | Start a client with 3D saws enabled, inspect all three saw tiers and the microblock item, then repeat with Angelica installed | Each renderer is registered once, saw models use the correct tier, microblock icons load, client packets register, and the Angelica hook initializes without errors | `MicroblockProxy_clientImpl.init` / `postInit` |
| [ ] | Render several block micro-materials with Angelica shaders enabled, then render an unrelated block | Each face receives the matching block/meta shader material and the override is reset after the microblock draw | `MaterialRenderHelper.blockAndMeta` / `render` |
| [ ] | Inspect face, hollow, edge and corner microblocks in inventory and as dropped entities | Every size/material combination uses the correct localized name and centered shape/material render | `ItemMicroPart` / `ItemMicroPartRenderer` / `MicroblockRender.renderItem` |
| [ ] | Saw **Stone**, **Glass** and a ProjectRed **Inverted White Lamp** into face covers; place them on all six faces beside other microblocks, then inspect their inventory renders | Opaque covers retain their outer face while internal faces follow the shrink mask; transparent covers use the shrunk bounds, and inventory renders show the complete shape without missing or doubled faces | `FaceMicroblockClient.render` / `FaceMicroblockTraitLogic.render` |
| [ ] | Saw **Stone**, **Glass** and a ProjectRed **Inverted White Lamp** into strip/post shapes; place crossing posts on all three axes between face covers, then add and remove neighbours | Compare both split segments with the reference, including the second segment's use of physical bounds; removal resets the split without stale geometry or missing sections | `PostMicroblockClient.recalcBounds` / `PostMicroblockClientLogic` / `shrinkFace` / `shrinkPost` |
| [ ] | Run connected AE2 **ME Glass Cable**, **ME Covered Cable** and **ME Dense Smart Cable**, plus ProjectRed **Framed Red Alloy Wire**, through hollow stone/glass covers on all six faces | Each opening follows the connector size; opaque and transparent rims, breaking overlay and highlight have no missing, doubled or stale sections after neighbour changes | `HollowMicroblockClient.recalcBounds` / `renderHollow` / `drawBreaking` / `drawHighlight` |
| [ ] | Place touching face, corner and edge microblocks of mixed sizes/transparency, then add and remove neighbours | Intersections choose the same winner, opaque covered faces stay masked, and every surviving segment refreshes without gaps or stale bounds | `MicroOcclusion` / `TMicroOcclusionClient.recalcBounds` |

2026-09-03: Placing a ProjectRed part with `1.7.12-algent-java.186+60d060a3ff` crashed in
`MultipartRenderer$.renderTileEntityAt`: its class call to `TileMultipartClient.hasDynamicParts()` encountered the
transformed runtime interface. The renderer now calls the generated getter through a stable `TileMultipart` base
hook. Four headless Forge cases cover the actual renderer bytecode's guards and dispatch; both nonempty cases
reproduced the original crash. The user retested with the supplied fix and confirmed that placement no longer
crashes. Static/dynamic drawing and part updates still need their full client checks before marking the rendering
entries complete.

## Placement and interaction

| Done | Check | Expected | From |
| --- | --- | --- | --- |
| [ ] | Right-click a block face with ProjectRed **Red Alloy Wire** at a shallow angle | The part is placed in the clicked block | `TItemMultiPart.onItemUse` depth below 1 |
| [ ] | Repeat with **Red Alloy Wire**, clicking deep into a face | The part is placed in the neighbouring block on that side | `TItemMultiPart.onItemUse` offset path |
| [ ] | Place a part where it would overlap an existing one | Placement is refused rather than silently replacing | `NormalOcclusionTest`, `PartialOcclusionTest` |
| [ ] | Place a cover on a face that already has a hollow cover | Redstone still passes through the hollow centre | `TFacePart.redstoneConductionMap` |
| [ ] | Run ProjectRed **Red Alloy Wire** across a **Stone Strip** edge microblock | Conduction matches the pre-port behavior | `TEdgePart.conductsRedstone` |
| [ ] | Cut materials with saws of different harvest levels | Only materials at or below the saw's strength can be cut | `Saw.getMaxCuttingStrength` |
| [ ] | Place the same microblock in survival and creative, including onto an existing multipart tile | Valid placements play the material sound; survival consumes the expected amount while creative does not | `ItemMicroPart.onItemUse` |
| [ ] | With a second client watching, convert a vanilla torch by adding a cover, then add/remove compatible ProjectRed **Red Alloy Wire**, ProjectBlue **Control Panel**, or AE2 **ME Glass Cable** parts | Both clients see each part exactly once, with no ghost block, stale render or missing part after tile replacement | `MultipartGenerator` conversion packet order and generated tile upgrade/downgrade |

## Downstream integration

| Done | Check | Expected | From |
| --- | --- | --- | --- |
| [ ] | Saw a ProjectRed **Inverted White Lamp** into cover, hollow cover, nook/corner and strip/post shapes; test face, corner, edge and centre-post placement | The lamps generate, use the correct bounds, render and light correctly | `MicroblockGenerator.registerTrait` Scala trait path / shape factory facades |
| [ ] | Place and break OpenComputers **Cable** and a **3D Print**, ProjectBlue **Control Panel** (size suffix varies), and AE2 **ME Glass Cable**; share their block space with a compatible cover | No `NoSuchMethodError` or `AbstractMethodError` in the log | retained `$class` bridges |
| [ ] | Load and render a schematic containing an ordered mixed multipart tile | Every part appears in the preview in the saved order; the integration does not silently disable itself | Schematica private registry-map reflection, `MicroblockClass.create`, and tile NBT reconstruction |
| [ ] | Open a GuideNH scene containing a multipart tile and export its part/material data | The preview, material data and part statistics are complete | companion reflection, `MicroblockGenerator$.create`, `Microblock.microClass/material/shape`, `partList_$eq`, and `BlockMicroMaterial` mixin fields |
| [ ] | Place FMP buttons on every supported face with Et Futurum loaded | Button orientation matches the fixed vanilla behavior | reflected mutable `ButtonPart` side/meta arrays |
| [ ] | Use OpenComputers **3D Printer** to make a **3D Print** with different off/on shapes that both fit beside a cover; place it with the cover and right-click to toggle | Its slots are rebuilt without losing, hiding or disconnecting either part | external mutation of `TSlottedTile.v_partMap` plus `bindPart` |
| [ ] | Move the same composite tile using a ProjectRed **Frame Motor** with MCFrames **Frame** blocks, then a **Matter Manipulator MKIII** in move mode | Coordinates, part-to-tile references, generated interfaces and rendering remain correct after `onMoved` | ForgeRelocationFMP / MatterManipulator live multipart move lifecycle |
| [ ] | Inspect a mixed multipart tile with Waila | Providers receive every saved part ID and show the expected part data | reflected block identity and tile NBT `parts`/`id` layout |
| [ ] | Load a world saved before the port | All microblocks and covers keep their material and shape | `Microblock` shape/material NBT and `MicroMaterialRegistry` id map |
| [ ] | Join a server whose material set differs from the client | The client is disconnected with the missing material list, not a crash | `readIDMap` |
| [ ] | Join a server whose multipart type set differs from the client | The client is disconnected with the ordered missing-part list, not a crash | `MultipartCPH.handlePartRegistration` |

## Not yet covered anywhere

- [ ] A focused pre-optimization CPU/allocation baseline now exists in `JAVA_MIGRATION_PROFILE.md`. A representative
  full-pack capture and the matching post-optimization comparison still need to be performed.
- [ ] Shape-specific microblock NBT and packet payloads. Core `Microblock` shape/material NBT, description bytes and
  one-byte shape updates are complete, along with compact core tile/part NBT and logical chunk-description fixtures.

## Item-name references

Resource paths below are relative to each consumer's `src/main/resources` in the clone root recorded in the handoff.
Code locations identify the matching source packages/methods. Registration/behavior was checked in addition to
translation keys; these are examples for running the checks, not claims that the checks have passed.

| Consumer | Translation key / source |
| --- | --- |
| ProjectRed | `assets/projectred/lang/en_US.lang`: `tile.projectred.illumination.lamp\|16`, `item.projectred.transmission.wire\|0`, `framewire\|0`, `tile.projectred.expansion.machine2\|8`; `illumination/lightmicroblocks.scala` registers inverted lamp materials, `expansion/TileFrameMotor.scala` uses the relocation API |
| OpenComputers | `assets/opencomputers/lang/en_US.lang`: `tile.oc.cable`, `tile.oc.print`, `tile.oc.printer`; `integration/fmp/PrintPart.scala` toggles state and rebuilds slots |
| ProjectBlue | `assets/projectblue/lang/en_US.lang`: `item.gcewing_projectblue:controlPanel`; `ControlPanelItem.getItemStackDisplayName` appends its grid size |
| AE2 | `assets/appliedenergistics2/lang/en_US.lang`: `item.appliedenergistics2.ItemPart.CableGlass`, `CableCovered`, `CableDense`; `appeng/fmp/CableBusPart.getHollowSize` supplies connected cable bounds |
| ForgeRelocation / MatterManipulator | `assets/mcframes/lang/en_US.lang`: `tile.mcframes.frame`; `assets/matter-manipulator/lang/en_US.lang`: `item.itemMatterManipulator3`; the MKIII tier permits moving |
| FMP shapes and saws | This repository's `src/main/resources/assets/multipart/lang/en_US.lang`: `mcr_face`, `mcr_hllw`, `mcr_edge`, `mcr_cnr`, and `item.microblock:sawStone/sawIron/sawDiamond` |
