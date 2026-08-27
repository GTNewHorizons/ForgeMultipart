# Manual compatibility checklist

Behavior that the automated layers cannot assert. Plain-JVM tests cannot reach a world, a client or a renderer, and
the Forge server suite is headless, so everything below needs a human in a running client unless stated otherwise.

Each entry names what to do, what should happen, and which port put it here. Run the relevant entries before any
release that includes those ports.

## Rendering and particles

| Check | Expected | From |
| --- | --- | --- |
| Break a cover or microblock part and watch the break overlay | The breaking texture is drawn on the part's own cuboid, not the full block | `TCuboidPart.drawBreaking` |
| Hit a part repeatedly without breaking it | Hit particles spawn on the struck face, using that face's icon | `IconHitEffects.addHitEffects` |
| Break a part fully | Destroy particles use all six side icons and are scaled to the part's bounds | `IconHitEffects.addDestroyEffects` |
| Break a hollow cover | Destroy particles use the full block bounds, not the part bounds | `addDestroyEffects` scaleDensity false |
| Hover a microblock placement target | The placement highlight renders, and a mod supplying its own highlight renderer overrides it | `MicroMaterialRegistry.renderHighlight` |
| Look at a saw in inventory and in hand | The saw item renders with its custom transform | `ItemSaw` `IItemRenderer` |
| Place microblocks of several materials, including glass | Transparent materials render in the correct pass and are not opaque | `IMicroMaterial.canRenderInPass` |

## Placement and interaction

| Check | Expected | From |
| --- | --- | --- |
| Right-click a block face with a multipart item at a shallow angle | The part is placed in the clicked block | `TItemMultiPart.onItemUse` depth below 1 |
| Right-click deep into a face | The part is placed in the neighbouring block on that side | `TItemMultiPart.onItemUse` offset path |
| Place a part where it would overlap an existing one | Placement is refused rather than silently replacing | `NormalOcclusionTest`, `PartialOcclusionTest` |
| Place a cover on a face that already has a hollow cover | Redstone still passes through the hollow centre | `TFacePart.redstoneConductionMap` |
| Run redstone across an edge microblock | Conduction matches the pre-port behavior | `TEdgePart.conductsRedstone` |
| Cut materials with saws of different harvest levels | Only materials at or below the saw's strength can be cut | `Saw.getMaxCuttingStrength` |

## Downstream integration

| Check | Expected | From |
| --- | --- | --- |
| Load the pack with ProjRed installed and place illumar lamp microblocks | The lamps generate, render and light correctly | `MicroblockGenerator.registerTrait` Scala trait path |
| Place and break OpenComputers, ProjectBlue and AE2 parts | No `NoSuchMethodError` or `AbstractMethodError` in the log | retained `$class` bridges |
| Load and render a schematic containing an ordered mixed multipart tile | Every part appears in the preview in the saved order; the integration does not silently disable itself | Schematica private registry-map reflection and tile NBT reconstruction |
| Open a GuideNH scene containing a multipart tile and export its part/material data | The preview, material data and part statistics are complete | companion reflection, `partList_$eq`, and `BlockMicroMaterial` mixin fields |
| Place FMP buttons on every supported face with Et Futurum loaded | Button orientation matches the fixed vanilla behavior | reflected mutable `ButtonPart` side/meta arrays |
| Toggle an OpenComputers print that shares a block with another slotted part | Its slots are rebuilt without losing, hiding or disconnecting either part | external mutation of `TSlottedTile.v_partMap` plus `bindPart` |
| Move the same composite tile once through ForgeRelocationFMP and once through MatterManipulator | Coordinates, part-to-tile references, generated interfaces and rendering remain correct after `onMoved` | live multipart move lifecycle |
| Inspect a mixed multipart tile with Waila | Providers receive every saved part ID and show the expected part data | reflected block identity and tile NBT `parts`/`id` layout |
| Load a world saved before the port | All microblocks and covers keep their material and shape | `MicroMaterialRegistry` id map |
| Join a server whose material set differs from the client | The client is disconnected with the missing material list, not a crash | `readIDMap` |

## Not yet covered anywhere

- CPU and allocation profiles before and after the hot-path work. Phase 4 has not started, so no baseline exists.
- NBT and packet layout fixtures beyond the material id carrier. Phase 0 still lists this as open.
