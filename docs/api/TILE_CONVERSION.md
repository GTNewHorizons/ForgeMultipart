# Tile lookup and conversion results

[Java API index](../API.md)

`TileMultipart.getOrConvertTileResult(World, BlockCoord)` returns a `TileConversionResult` with `getTile()` and
`isConverted()` accessors. It is available on `algent/java`, with no minimum published dependency version yet.

```java
TileConversionResult result = TileMultipart.getOrConvertTileResult(world, pos);
TileMultipart tile = result.getTile(); // May be null.
boolean placeholder = result.isConverted();
```

The [compiling example](../../src/functionalTest/java/codechicken/multipart/examples/TileConversionExample.java) selects a
converted placeholder for inspection. It also compiles with Scala excluded from the compile classpath.

## Outcomes and ownership

| World state | `getTile()` | `isConverted()` |
| --- | --- | --- |
| A multipart tile already exists | That exact tile | `false` |
| A registered converter accepts the block | A fresh generated placeholder with its converted part bound | `true` |
| No multipart tile and no successful conversion | `null` | `false` |

An unrelated tile entity does not count as a multipart tile. Converters still get a chance to handle its block.
The existing-tile path returns before querying the block or converters. Conversion follows the existing registry
dispatch and uses the world's client/server side when generating the tile.

The result is an immutable record of one lookup; the referenced tile and its parts remain mutable. There is no
snapshot of tile contents, ownership transfer, synchronization or automatic update when the world later changes.

To implement conversion, see [block converter registration and lifecycle](BLOCK_CONVERTERS.md).

## Placeholder lifecycle

A converted placeholder has world and position assigned and its converted part bound. FMP has not replaced the
original block or installed the tile in the world. This lookup does not invoke the part's `onAdded` or `onConverted`
callbacks. Repeating it can create another tile and part; do not rely on identity across conversion queries.

Converters and binding hooks execute normally and can have side effects or throw. This is not a pure query or a
placement permission check. Call it on the appropriate world thread after FMP initialization. Converter failures
propagate without a new fallback or rollback guarantee.

For ordinary placement, use `canPlacePart(world, pos, part)` and then `addPart(world, pos, part)`, retaining the tile
returned by the addition. Addition performs conversion again and handles installation, callbacks and generated tile
replacement. Do not manually install the earlier placeholder, and do not treat it as a reservation of that location.

Use `getTile(world, pos)` to find only an existing multipart tile. The existing `getOrConvertTile(world, pos)` remains
supported when only the tile is needed; it has the same placeholder caveat and does not need migration.

## Compatibility and validation

Both static and companion `getOrConvertTile2(World, BlockCoord)` tuple entries remain, with unchanged descriptors and
bodies, deprecated in favor of the named Java result. The new method delegates to that existing path. Internal
generator and microblock placement callers retain their tuple calls; `MicroblockPlacement.gtile()` remains a separate
Scala-shaped accessor whose eventual migration belongs to placement/extension guidance.

No direct tuple-method use or new-name collision was found in the supplied consumer sources, and the frozen pack
inventory has no reference to that tuple method. AE2, Chisel, OpenComputers and Extra Utilities already use the
tile-only method and need no rename for this contract. Consumer checkouts remain reference-only.

Forge tests run the same scenarios through both legacy entries and the Java entry: existing tile identity, air,
nonconvertible solid blocks, unrelated tile entities, bound torch placeholders, repeated conversions and subsequent
normal placement. Archived callers, class/member APIs and generated outputs are checked separately. Actual client
preview rendering remains a [manual release check](../migration/MANUAL_CHECKS.md).
