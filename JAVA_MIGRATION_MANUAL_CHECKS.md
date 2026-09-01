# Manual compatibility checklist

Behavior that the automated layers cannot assert. Plain-JVM tests cannot reach a world, a client or a renderer, and
the Forge server suite is headless, so everything below needs a human in a running client unless stated otherwise.

Each entry names what to do, what should happen, and which port put it here. Run the relevant entries before any
release that includes those ports.

Change `[ ]` to `[x]` after a successful check. If a check fails, leave it unchecked and add a dated note immediately
below its table.

## Rendering and particles

| Done | Check | Expected | From |
| --- | --- | --- | --- |
| [ ] | Break a cover or microblock part and watch the break overlay | The breaking texture is drawn on the part's own cuboid, not the full block | `TCuboidPart.drawBreaking` |
| [ ] | Hit a part repeatedly without breaking it | Hit particles spawn on the struck face, using that face's icon | `IconHitEffects.addHitEffects` |
| [ ] | Break a part fully | Destroy particles use all six side icons and are scaled to the part's bounds | `IconHitEffects.addDestroyEffects` |
| [ ] | Break a hollow cover | Destroy particles use the full block bounds, not the part bounds | `addDestroyEffects` scaleDensity false |
| [ ] | Hover face, hollow, edge and corner microblock placement targets | The highlight and guide lines match each placement region, and a mod supplying its own highlight renderer overrides them | `PlacementGrid` / `MicroblockEventHandler.drawBlockHighlight` / `MicroMaterialRegistry.renderHighlight` |
| [ ] | Reload client resources after placing several microblock materials | The texture atlas reloads every material icon without missing textures | `MicroblockEventHandler.postTextureStitch` / `BlockMicroMaterial.loadIcons` |
| [ ] | Look at a saw in inventory and in hand | The saw item renders with its custom transform | `ItemSaw` `IItemRenderer` |
| [ ] | Place microblocks of several materials, including glass | Transparent materials render in the correct pass and are not opaque | `BlockMicroMaterial` / `IMicroMaterial.canRenderInPass` |
| [ ] | Place grass and mycelium covers at several thicknesses and biomes | Grass has an untinted base plus a tinted, height-aligned side overlay and tinted top; mycelium uses its top texture on horizontal faces and height-aligned side texture elsewhere | `GrassMicroMaterial` / `TopMicroMaterial` |
| [ ] | Load a saved microblock whose material is no longer installed | The part remains present and renders the magenta/black missing texture instead of becoming material ID 0 | `MissingMicroMaterial` |
| [ ] | Change a mixed multipart tile containing static and dynamically rendered parts | Static parts remain in the block render, dynamic parts remain in the TESR pass, and neither disappears after a part update | `TileMultipartClient` render caches |
| [ ] | Watch a multipart torch long enough to emit display particles | Only parts implementing `IRandomDisplayTick` emit particles, with no duplicate or missing callbacks | `TRandomDisplayTickTile.randomDisplayTick` |
| [ ] | Start a client, join a server, place/update a multipart and use the multipart control key | Block and generated-tile rendering work, client packets arrive, and control-key state changes are sent once without duplicate registrations | `MultipartProxy_clientImpl.postInit` / `onTileClassBuilt` |
| [ ] | Move into view of a chunk containing several multipart tiles, then add, update and remove parts | The initial chunk description reconstructs every tile and compressed updates apply without missing, duplicated or ghost parts | `MultipartCPH` / `MultipartSPH` |
| [ ] | Start a client with 3D saws enabled, inspect all three saw tiers and the microblock item, then repeat with Angelica installed | Each renderer is registered once, saw models use the correct tier, microblock icons load, client packets register, and the Angelica hook initializes without errors | `MicroblockProxy_clientImpl.init` / `postInit` |
| [ ] | Render several block micro-materials with Angelica shaders enabled, then render an unrelated block | Each face receives the matching block/meta shader material and the override is reset after the microblock draw | `MaterialRenderHelper.blockAndMeta` / `render` |
| [ ] | Inspect face, hollow, edge and corner microblocks in inventory and as dropped entities | Every size/material combination uses the correct localized name and centered shape/material render | `ItemMicroPart` / `ItemMicroPartRenderer` |

## Placement and interaction

| Done | Check | Expected | From |
| --- | --- | --- | --- |
| [ ] | Right-click a block face with a multipart item at a shallow angle | The part is placed in the clicked block | `TItemMultiPart.onItemUse` depth below 1 |
| [ ] | Right-click deep into a face | The part is placed in the neighbouring block on that side | `TItemMultiPart.onItemUse` offset path |
| [ ] | Place a part where it would overlap an existing one | Placement is refused rather than silently replacing | `NormalOcclusionTest`, `PartialOcclusionTest` |
| [ ] | Place a cover on a face that already has a hollow cover | Redstone still passes through the hollow centre | `TFacePart.redstoneConductionMap` |
| [ ] | Run redstone across an edge microblock | Conduction matches the pre-port behavior | `TEdgePart.conductsRedstone` |
| [ ] | Cut materials with saws of different harvest levels | Only materials at or below the saw's strength can be cut | `Saw.getMaxCuttingStrength` |
| [ ] | Place the same microblock in survival and creative, including onto an existing multipart tile | Valid placements play the material sound; survival consumes the expected amount while creative does not | `ItemMicroPart.onItemUse` |

## Downstream integration

| Done | Check | Expected | From |
| --- | --- | --- | --- |
| [ ] | Load the pack with ProjRed installed and place illumar lamp microblocks | The lamps generate, render and light correctly | `MicroblockGenerator.registerTrait` Scala trait path |
| [ ] | Place and break OpenComputers, ProjectBlue and AE2 parts | No `NoSuchMethodError` or `AbstractMethodError` in the log | retained `$class` bridges |
| [ ] | Load and render a schematic containing an ordered mixed multipart tile | Every part appears in the preview in the saved order; the integration does not silently disable itself | Schematica private registry-map reflection and tile NBT reconstruction |
| [ ] | Open a GuideNH scene containing a multipart tile and export its part/material data | The preview, material data and part statistics are complete | companion reflection, `partList_$eq`, and `BlockMicroMaterial` mixin fields |
| [ ] | Place FMP buttons on every supported face with Et Futurum loaded | Button orientation matches the fixed vanilla behavior | reflected mutable `ButtonPart` side/meta arrays |
| [ ] | Toggle an OpenComputers print that shares a block with another slotted part | Its slots are rebuilt without losing, hiding or disconnecting either part | external mutation of `TSlottedTile.v_partMap` plus `bindPart` |
| [ ] | Move the same composite tile once through ForgeRelocationFMP and once through MatterManipulator | Coordinates, part-to-tile references, generated interfaces and rendering remain correct after `onMoved` | live multipart move lifecycle |
| [ ] | Inspect a mixed multipart tile with Waila | Providers receive every saved part ID and show the expected part data | reflected block identity and tile NBT `parts`/`id` layout |
| [ ] | Load a world saved before the port | All microblocks and covers keep their material and shape | `MicroMaterialRegistry` id map |
| [ ] | Join a server whose material set differs from the client | The client is disconnected with the missing material list, not a crash | `readIDMap` |
| [ ] | Join a server whose multipart type set differs from the client | The client is disconnected with the ordered missing-part list, not a crash | `MultipartCPH.handlePartRegistration` |

## Not yet covered anywhere

- [ ] A focused pre-optimization CPU/allocation baseline now exists in `JAVA_MIGRATION_PROFILE.md`. A representative
  full-pack capture and the matching post-optimization comparison still need to be performed.
- [ ] Microblock-specific NBT and packet payloads. The compact core tile/part NBT and logical chunk-description fixtures
  are complete.
