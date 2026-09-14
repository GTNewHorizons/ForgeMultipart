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

| Done | Check | Concrete example / setup | Expected | From |
| --- | --- | --- | --- | --- |
| [ ] | Break a cover or microblock part and watch the break overlay | Forge Multipart **Stone Cover** | The breaking texture is drawn on the part's own cuboid, not the full block | `TCuboidPart.drawBreaking` |
| [ ] | Hit a part repeatedly without breaking it | Forge Multipart **Grass Cover**; its top, side and bottom icons make a wrong face visible | Hit particles spawn on the struck face, using that face's icon | `IconHitEffects.addHitEffects` |
| [ ] | Break a part fully | ProjectRed Expansion **Solar Panel** placed horizontally; it has thin bounds and distinct top/side icons | Destroy particles query sides 0–5 and are scaled to the part's bounds | `IconHitEffects.addDestroyEffects` |
| [ ] | Break a hollow cover | Forge Multipart **Hollow Stone Cover** | Destroy particles use the full block bounds, not the part bounds | `addDestroyEffects` scaleDensity false |
| [ ] | Hover face, hollow, edge and corner microblock placement targets | Forge Multipart **Stone Cover**, **Hollow Stone Cover**, **Stone Strip** and **Stone Nook**; for a consumer override, hold a Stone Cover over ProjectRed **Framed Red Alloy Wire** to preview its jacket | The highlight and guide lines match each placement region, and a mod supplying its own highlight renderer overrides them | `PlacementGrid` / `MicroblockRender.renderHighlight` / `MicroblockEventHandler.drawBlockHighlight` / `MicroMaterialRegistry.renderHighlight` |
| [ ] | Reload client resources after placing several microblock materials | Place **Stone**, **Glass**, **Grass** and ProjectRed **Inverted White Lamp Covers**, then press `F3+T` | The texture atlas reloads every material icon without missing textures | `MicroblockEventHandler.postTextureStitch` / `BlockMicroMaterial.loadIcons` |
| [ ] | Look at a **Stone Saw**, **Iron Saw** and **Diamond Saw** in inventory and in hand | All three items are supplied by Forge Multipart | The saw item renders with its custom transform | `ItemSaw` `IItemRenderer` |
| [ ] | Place microblocks of several materials, including glass | Compare Forge Multipart **Stone Cover** and **Glass Cover** | Transparent materials render in the correct pass and are not opaque | `BlockMicroMaterial` / `IMicroMaterial.canRenderInPass` |
| [ ] | Place grass and mycelium covers at several thicknesses and biomes | Forge Multipart **Grass Cover** and **Mycelium Cover**, preferably 1/8 and 1/2 thicknesses | Grass has an untinted base plus a tinted, height-aligned side overlay and tinted top; mycelium uses its top texture on horizontal faces and height-aligned side texture elsewhere | `GrassMicroMaterial` / `TopMicroMaterial` |
| [ ] | Load a saved microblock whose material is no longer installed | In a disposable instance, add `"minecraft:coal_ore"` to `microblocks.cfg`, create and save a **Coal Ore Cover**, remove that line, then reload | The part remains present and renders the magenta/black missing texture instead of becoming material ID 0 | `MissingMicroMaterial` |
| [ ] | Put a **Stone Cover** and a ProjectRed **Inverted White Lamp Cover** on opposite faces of one block space, then add/remove another part | Use another Stone Cover or ProjectRed **Red Alloy Wire** as the part that triggers cache updates | Static parts remain in the block render, dynamic parts remain in the TESR pass, and neither disappears after a part update | `TileMultipartClient` render caches |
| [ ] | Watch a multipart torch long enough to emit display particles | Add a **Stone Cover** to a vanilla **Torch** to convert it into a multipart; an active **Redstone Torch** is another useful case | Only parts implementing `IRandomDisplayTick` emit particles, with no duplicate or missing callbacks | `TRandomDisplayTickTile.randomDisplayTick` |
| [ ] | Start a client, join a server, place/update a multipart and use the multipart control key | Assign Forge Multipart's **Switch block disposition** key, then place and reorient a **Stone Cover** | Block and generated-tile rendering work, client packets arrive, and control-key state changes are sent once without duplicate registrations | `MultipartProxy_clientImpl.postInit` / `onTileClassBuilt` |
| [ ] | Move into view of a chunk containing several multipart tiles, then add, update and remove parts | Use mixed tiles containing an AE2 **ME Glass Cable**, a **Stone Cover**, and ProjectRed **Red Alloy Wire** on different faces | The initial chunk description reconstructs every tile and compressed updates apply without missing, duplicated or ghost parts | `MultipartCPH` / `MultipartSPH` |
| [ ] | Start a client with 3D saws enabled, inspect all three saw tiers and the microblock item, then repeat with Angelica installed | Set `useSawIcons=false`; inspect Forge Multipart's **Stone**, **Iron** and **Diamond Saws** and any Stone microblock | Each renderer is registered once, saw models use the correct tier, microblock icons load, client packets register, and the Angelica hook initializes without errors | `MicroblockProxy_clientImpl.init` / `postInit` |
| [ ] | Render several block micro-materials with Angelica shaders enabled, then render an unrelated block | Render **Stone**, **Grass** and **Glass Covers**, then an unrelated vanilla Stone block | Each face receives the matching block/meta shader material and the override is reset after the microblock draw | `MaterialRenderHelper.blockAndMeta` / `render` |
| [ ] | Inspect face, hollow, edge and corner microblocks in inventory and as dropped entities | Forge Multipart Stone **Cover**, **Hollow Cover**, **Strip**, **Post** and **Nook** at several sizes | Every size/material combination uses the correct localized name and centered shape/material render | `ItemMicroPart` / `ItemMicroPartRenderer` / `MicroblockRender.renderItem` |
| [ ] | Saw **Stone**, **Glass** and a ProjectRed **Inverted White Lamp** into face covers; place them on all six faces beside other microblocks, then inspect their inventory renders | The named Forge Multipart and ProjectRed microblocks are the concrete cases | Opaque covers retain their outer face while internal faces follow the shrink mask; transparent covers use the shrunk bounds, and inventory renders show the complete shape without missing or doubled faces | `FaceMicroblockClient.render` / `FaceMicroblockTraitLogic.render` |
| [ ] | Saw **Stone**, **Glass** and a ProjectRed **Inverted White Lamp** into strip/post shapes; place crossing posts on all three axes between face covers, then add and remove neighbours | The named Forge Multipart and ProjectRed microblocks are the concrete cases | Compare both split segments with the reference, including the second segment's use of physical bounds; removal resets the split without stale geometry or missing sections | `PostMicroblockClient.recalcBounds` / `PostMicroblockClientLogic` / `shrinkFace` / `shrinkPost` |
| [ ] | Run connected AE2 **ME Glass Cable**, **ME Covered Cable** and **ME Dense Smart Cable**, plus ProjectRed **Framed Red Alloy Wire**, through hollow stone/glass covers on all six faces | The named AE2, ProjectRed and Forge Multipart parts are the concrete cases | Each opening follows the connector size; opaque and transparent rims, breaking overlay and highlight have no missing, doubled or stale sections after neighbour changes | `HollowMicroblockClient` / `HollowMicroblockClientLogic` |
| [ ] | Place touching **Stone Cover**, **Glass Cover**, corner/edge counterparts and ProjectRed **Inverted White Lamp** microblocks at mixed sizes; add/remove neighbours, then rejoin the server | The named Forge Multipart and ProjectRed microblocks are the concrete cases | Intersections choose the same winner, opaque covered faces stay masked, and every surviving segment refreshes without gaps or stale bounds | `MicroOcclusion` / `TMicroOcclusionClient` / `TMicroOcclusionClientLogic` |

2026-09-03: Placing a ProjectRed part with `1.7.12-algent-java.186+60d060a3ff` crashed in
`MultipartRenderer$.renderTileEntityAt`: its class call to `TileMultipartClient.hasDynamicParts()` encountered the
transformed runtime interface. The renderer now calls the generated getter through a stable `TileMultipart` base
hook. Four headless Forge cases cover the actual renderer bytecode's guards and dispatch; both nonempty cases
reproduced the original crash. The user retested with the supplied fix and confirmed that placement no longer
crashes. Static/dynamic drawing and part updates still need their full client checks before marking the rendering
entries complete.

## Placement and interaction

| Done | Check | Concrete example / setup | Expected | From |
| --- | --- | --- | --- | --- |
| [ ] | Right-click a block face with ProjectRed **Red Alloy Wire** at a shallow angle | ProjectRed **Red Alloy Wire** | The part is placed in the clicked block | `TItemMultiPart.onItemUse` depth below 1 |
| [ ] | Repeat with **Red Alloy Wire**, clicking deep into a face | ProjectRed **Red Alloy Wire** | The part is placed in the neighbouring block on that side | `TItemMultiPart.onItemUse` offset path |
| [ ] | Place a part where it would overlap an existing one | Place a **Stone Cover**, then force a second Stone or Hollow Stone Cover into the same face slot using **Switch block disposition** if needed | Placement is refused rather than silently replacing | `NormalOcclusionTest`, `PartialOcclusionTest` |
| [ ] | Pass redstone through a hollow cover on a multipart face | Put ProjectRed **Framed Red Alloy Wire** in the centre and a **Hollow Stone Cover** between it and an external redstone source | Redstone still passes through the hollow centre | `TFacePart.redstoneConductionMap` |
| [ ] | Place **Hollow Stone Cover** and **Hollow Inverted White Lamp Cover** parts at several thicknesses around ProjectRed **Framed Red Alloy Wire** on all six faces; add/remove the wire, select the rim, then save and reload | The named Forge Multipart and ProjectRed parts are the concrete cases | The opening follows the connector, selection/collision respect the hollow centre, incompatible overlaps are refused, and shape/material survive reload | `HollowMicroblock` / `HollowMicroblockTraitLogic` |
| [ ] | Run ProjectRed **Red Alloy Wire** across a **Stone Strip** edge microblock | ProjectRed **Red Alloy Wire** and Forge Multipart **Stone Strip** | Conduction matches the pre-port behavior | `TEdgePart.conductsRedstone` |
| [ ] | Cut materials with saws of different harvest levels | Try **Stone Saw → Iron Block**, **Iron Saw → Diamond Block**, and **Diamond Saw → Obsidian**; confirm weaker saws reject the higher-tier cases | Only materials at or below the saw's strength can be cut | `Saw.getMaxCuttingStrength` |
| [ ] | Place the same microblock in survival and creative, including onto an existing multipart tile | Use a **Stone Cover** in empty space and beside ProjectRed **Framed Red Alloy Wire** | Valid placements play the material sound; survival consumes the expected amount while creative does not | `ItemMicroPart.onItemUse` |
| [ ] | With a second client watching, convert a vanilla torch by adding a cover, then add/remove compatible ProjectRed **Red Alloy Wire**, ProjectBlue **Control Panel**, or AE2 **ME Glass Cable** parts | Start with a vanilla **Torch + Stone Cover**, then use any named consumer part that fits | Both clients see each part exactly once, with no ghost block, stale render or missing part after tile replacement | `MultipartGenerator` conversion packet order and generated tile upgrade/downgrade |

## Downstream integration

| Done | Check | Concrete example / setup | Expected | From |
| --- | --- | --- | --- | --- |
| [ ] | Saw a ProjectRed **Inverted White Lamp** into cover, hollow cover, nook/corner and strip/post shapes; test face, corner, edge and centre-post placement | ProjectRed **Inverted White Lamp**; ordinary lamps and **White Illumar** do not exercise this registration path | The lamps generate, use the correct bounds, render and light correctly | `MicroblockGenerator.registerTrait` Scala trait path / shape factory facades |
| [ ] | Place and break OpenComputers **Cable** and a **3D Print**, ProjectBlue **Control Panel** (size suffix varies), and AE2 **ME Glass Cable**; share their block space with a compatible cover | Use a **Stone Cover** with each named consumer part | No `NoSuchMethodError` or `AbstractMethodError` in the log | retained `$class` bridges |
| [ ] | Load and render a schematic containing an ordered mixed multipart tile | Schematica; capture an AE2 **ME Glass Cable** with a **Stone Cover** and ProjectRed **Red Alloy Wire** on different faces | Every part appears in the preview in the saved order; the integration does not silently disable itself | Schematica private registry-map reflection, `MicroblockClass.create`, and tile NBT reconstruction |
| [ ] | Open a GuideNH scene containing a multipart tile and export its part/material data | GuideNH; use the same **ME Glass Cable + Stone Cover + Red Alloy Wire** composite | The preview, material data and part statistics are complete | companion reflection, `MicroblockGenerator$.create`, `Microblock.microClass/material/shape`, `partList_$eq`, and `BlockMicroMaterial` mixin fields |
| [ ] | Place FMP buttons on every supported face with Et Futurum loaded | Convert a vanilla **Stone Button** by adding a **Stone Cover**; repeat its supported orientations with Et Futurum Requiem installed | Button orientation matches the fixed vanilla behavior | supported `ButtonPart.setOrientation`; legacy arrays until consumer adoption |
| [ ] | With Iguana harvest-level tweaks enabled, inspect and use the Stone, Iron, Diamond, Ardite, Cobalt and Manyullyn saws against materials spanning their configured tiers | Forge Multipart supplies the first three saws; Iguana Tweaks for Tinkers' Construct supplies **Ardite**, **Cobalt** and **Manyullyn Saws** | Tooltips, saw textures and cutting recipes use the remapped tier; weaker saws do not gain the cached strongest-saw exemption | supported `ItemSaw.setHarvestLevel`; legacy field until consumer adoption |
| [ ] | Use OpenComputers **3D Printer** to make a **3D Print** with different off/on shapes that both fit beside a cover; place it with the cover and right-click to toggle | OpenComputers **3D Printer**, its resulting **3D Print**, and a Forge Multipart **Stone Cover** | Its slots are rebuilt without losing, hiding or disconnecting either part | external mutation of `TSlottedTile.v_partMap` plus `bindPart` |
| [ ] | Move the same composite tile using a ProjectRed **Frame Motor** with MCFrames **Frame** blocks, then a **Matter Manipulator MKIII** in move mode | Move the **ME Glass Cable + Stone Cover + Red Alloy Wire** composite with the named ProjectRed, MCFrames and Matter Manipulator items | Coordinates, part-to-tile references, generated interfaces and rendering remain correct after `onMoved` | ForgeRelocationFMP / MatterManipulator live multipart move lifecycle |
| [ ] | Inspect a mixed multipart tile with Waila | Waila; inspect the **ME Glass Cable + Stone Cover + Red Alloy Wire** composite | Providers receive every saved part ID and show the expected part data | reflected block identity and tile NBT `parts`/`id` layout |
| [ ] | Load a world saved before the port | Use a pre-port save containing **Stone Covers**, ProjectRed lamp microblocks and an AE2 cable | All microblocks and covers keep their material and shape | `Microblock` shape/material NBT and `MicroMaterialRegistry` id map |
| [ ] | Join a server whose material set differs from the client | In disposable client/server instances, add `"minecraft:coal_ore"` only to the server's `microblocks.cfg` | The client is disconnected with the missing material list, not a crash | `readIDMap` |
| [ ] | Join a server whose multipart type set differs from the client | Setup-only: use a small test addon that registers a part on only one side; stock consumer removal may be rejected earlier by Forge's mod handshake | The client is disconnected with the ordered missing-part list, not a crash | `MultipartCPH.handlePartRegistration` |

The [Java illuminated extension example](../api/MICROBLOCK_EXTENSIONS.md) has dedicated-server and headless halo
coverage. When adopting it in ProjectRed, rerun the illuminated shape/lighting checks above on a physical client,
including hollow connector widths and all sides, pass filtering and the existing halo queue/configuration. Neither
its callback-side input tests nor its retained client-body probe validates actual client generation or GPU output.

## Not yet covered anywhere

- [ ] Follow the [performance protocol](MAINTAINERS.md#performance-validation); historical focused measurements are not full-pack evidence. A representative
  full-pack capture and the matching post-optimization comparison still need to be performed. Example setup: a loaded
  chunk containing many AE2 cables, ProjectRed wires/lamps and assorted Forge Multipart microblocks.
- [ ] Shape-specific microblock NBT and packet payloads. Core `Microblock` shape/material NBT, description bytes and
  one-byte shape updates are complete, along with compact core tile/part NBT and logical chunk-description fixtures.
  Example setup: Stone Cover, Hollow Cover, Nook, Strip and Post; save/rejoin and mutate each shape.

## Item-name references

Resource paths below are relative to each consumer's `src/main/resources` in the clone root recorded in the handoff.
Code locations identify the matching source packages/methods. Registration/behavior was checked in addition to
translation keys; these are examples for running the checks, not claims that the checks have passed.

| Consumer | Translation key / source |
| --- | --- |
| ProjectRed | `assets/projectred/lang/en_US.lang`: `tile.projectred.illumination.lamp\|16`, `item.projectred.transmission.wire\|0`, `framewire\|0`, `item.projectred.expansion.solar_panel`, `tile.projectred.expansion.machine2\|8`; `illumination/lightmicroblocks.scala` registers inverted lamp materials, `expansion/TileSolarPanel.scala` supplies a thin part with top/side particle icons, and `expansion/TileFrameMotor.scala` uses the relocation API |
| OpenComputers | `assets/opencomputers/lang/en_US.lang`: `tile.oc.cable`, `tile.oc.print`, `tile.oc.printer`; `integration/fmp/PrintPart.scala` toggles state and rebuilds slots |
| ProjectBlue | `assets/projectblue/lang/en_US.lang`: `item.gcewing_projectblue:controlPanel`; `ControlPanelItem.getItemStackDisplayName` appends its grid size |
| AE2 | `assets/appliedenergistics2/lang/en_US.lang`: `item.appliedenergistics2.ItemPart.CableGlass`, `CableCovered`, `CableDense`; `appeng/fmp/CableBusPart.getHollowSize` supplies connected cable bounds |
| ForgeRelocation / MatterManipulator | `assets/mcframes/lang/en_US.lang`: `tile.mcframes.frame`; `assets/matter-manipulator/lang/en_US.lang`: `item.itemMatterManipulator3`; the MKIII tier permits moving |
| Chisel | `compat/fmp/PartChiselTorch.java` and `FMPCompat.java`: any Chisel torch can be converted while preserving its variant and orientation |
| Iguana Tweaks for Tinkers' Construct | `modcompat/fmp`: Ardite, Cobalt and Manyullyn saws plus harvest-level remapping for Forge Multipart saws |
| FMP shapes and saws | This repository's `src/main/resources/assets/multipart/lang/en_US.lang`: `mcr_face`, `mcr_hllw`, `mcr_edge`, `mcr_cnr`, and `item.microblock:sawStone/sawIron/sawDiamond` |

## Converter integration follow-up

The [converter guide](../api/BLOCK_CONVERTERS.md) and Forge fixtures establish registration, discarded probes,
committed callback order and example state round trips. Before adopting consumer changes:

- [ ] On a physical client/server, preview and cancel conversion of Chisel torches, OpenComputers cables/prints,
  AE2 cable buses and supported converter blocks; the source state/network must remain intact. Concrete cases: any
  **Chisel Torch**, OpenComputers **Cable** and **3D Print**, and AE2 **ME Glass Cable**.
- [ ] Commit conversion, reconnect/reload, and confirm metadata, inventory and network state survive exactly once,
  without duplicate drops, lost contents or ghost connections. Exercise rejection as well as successful placement
  with the same Chisel, OpenComputers and AE2 cases.
- [ ] Recheck consumer side-specific constructors and descriptions; the headless example packet test does not cover
  physical-client class selection or rendering. Record consumer release and actual pack adoption separately. Reuse
  the Chisel Torch, OpenComputers Cable/3D Print and AE2 ME Glass Cable cases on both client and server.

## Stable tile capability adoption

- [ ] Rebuild ProjectRed's open-connection query against `IRedstoneTile`, retaining its rotation/mask logic; validate
  face/framed wires with face covers and edge blockers on a physical client/server, including part changes and moves.
  Concrete setup: ProjectRed **Red Alloy Wire** and **Framed Red Alloy Wire** with Stone Covers and Stone Strips.
- [ ] Verify consumer bytecode uses the stable interface/base owner rather than raw transformed trait class calls or
  fields. The [Forge fixture](../api/TILE_TRAIT_ACCESS.md) proves server linkage, not all client-side capabilities.
  This is a `javap`/ASM inspection check; no in-game item exercises the bytecode owner directly.
- [ ] Rebuild OpenComputers `PrintPart.toggleState` using `tile.refreshPartSlots(this)`, then exercise inactive/active
  and button modes on client/server. Confirm old slots clear, new slots occupy, rejected changes leave caches intact,
  and sound, part notification, description update and scheduled reset still occur once in the original order.
  Concrete setup: an OpenComputers **3D Print** with different compact off/on shapes beside a Stone Cover.
- [ ] Verify the migrated OpenComputers bytecode no longer names `TSlottedTile`/`v_partMap`; retain the old accessor
  until its released version is present in the target pack. This is also a `javap`/ASM inspection check, not an
  additional in-game item test.
