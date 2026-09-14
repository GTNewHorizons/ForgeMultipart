# Registering block converters from Java

[API index](../API.md) · [Factory registration](PART_REGISTRATION.md) · [Conversion results](TILE_CONVERSION.md)

Use the existing `MultiPartRegistry.registerConverter(IPartConverter)` on both sides during common mod initialization,
after registering the source blocks. It already accepts ordinary Java types; no new wrapper, reflection or Scala
companion lookup is needed. Register the converted part's persistent factory separately with `registerPartFactory`.
The latter is a migration-branch addition with no minimum published dependency version yet.

## Compiling example

[BlockConversionExample.java](../../src/functionalTest/java/codechicken/multipart/examples/BlockConversionExample.java)
registers one converter and its NBT/packet factory:

```java
BlockConversionExample.register(yourSourceBlock);
```

Replace `yourmod:converted` and the sample part with your own stable type and implementation. The example copies
block metadata into a fresh part, preserves it through NBT and description packets, and leaves the source untouched.
It intentionally covers registration and state only: it supplies no custom geometry, rendering, drops or gameplay.
Existing published IDs, field names and packet formats must remain unchanged when migrating real parts.

`blockTypes()` returns a Java `Iterable<Block>`; `Collections.singletonList` or `Arrays.asList` covers ordinary cases.
These are block instances, not numeric IDs or metadata values. `convert(world, pos)` checks metadata or tile type as
needed and returns null to decline. The example checks the source block explicitly because it can also be called
directly. Read the source before returning: the registry does not subsequently call `load` or `readDesc` on this part.
The independent factory creates blank instances; normal reconstruction supplies their payload afterward.

## Registration and dispatch

- Register once on the initialization thread. FMP retains the converter, enumerates its block iterable immediately,
  and appends it to each block's list. Mutating the iterable afterward does not change existing entries.
- Order matters: the first non-null result wins. Returning null continues; throwing stops dispatch and propagates.
  There is no priority field, fallback after an exception, result validation, or rollback.
- Duplicate blocks and repeated registration produce duplicate entries. There is no unregister operation. Avoid
  registration on world/server load, which can repeat within the same process.
- Unlike factory registration, converter registration does not enforce a mod-container or postInit state gate.
  Common preInit/init is the supported integration timing, not a promise of a runtime registration error afterward.
  Supply valid non-null blocks and a non-null iterable. An iterator failure leaves earlier registrations present.
- `convertBlock(world, pos, block)` selects by its explicit block argument, passes the same world/position references
  to callbacks and returns the result unchanged. It does not check the world's block, call a factory, bind a part,
  replace the block or invoke lifecycle hooks. Treat the position as borrowed and do not mutate it.

## Probes and committed conversion

Conversion can run repeatedly: a preview, `getOrConvertTileResult`, a rejected or accepted `canPlacePart`, then
`addPart` may each create a different part. Return a fresh, unbound instance each time. Do not drain inventories,
disconnect networks, remove the source block or transfer ownership inside `convert`. A rejected candidate receives
no compensating cleanup callback. Copy state without changing the original; use `world.isRemote` if construction
needs different client/server classes. The sample uses the same class on both paths.

An accepted lookup creates a bound, uninstalled placeholder. Normal server placement performs conversion again and
then follows this sequence:

1. The converted part is bound to a placeholder with world and position, while the original block/tile still exists.
2. `invalidateConvertedTile()` runs before replacement. Clear resources already copied into the part here when
   necessary to prevent the old block's removal from dropping them again. The base implementation does nothing.
3. FMP replaces the block, installs the placeholder, sends the block-change notification, then calls `onConverted()`.
   The base implementation calls `onAdded()`. Preserve that lifecycle if overriding, unless deliberately replacing it.
4. FMP writes the converted part's add-part description, promotes the tile if the requested new part needs more
   capabilities, then adds that new part through its normal placement lifecycle.

Keep the tile returned by `TileMultipart.addPart`; even the tile observed inside `onConverted()` may later be replaced
by capability promotion. A callback failure propagates at its current stage; committed conversion is not transactional.
These hooks run on the server placement path, not on probes. Clients reconstruct from packets instead. For ordinary
placement, check `canPlacePart` first and call `addPart` only on the server. Neither a successful check nor a saved
placeholder reserves the location. See [conversion results](TILE_CONVERSION.md) for lookup ownership.

## Consumer migration and validation

Chisel copies torch variant/orientation, ForgeRelocationFMP constructs a frame, OpenComputers inspects cable/print
tiles, and AE2 copies cable-bus state from the original tile. Preserve each consumer's eligibility, state-copy and
cleanup rules. Existing Java converter registrations need no rename; Scala consumers can retain Scala implementation
code while returning Java collections and calling the static entry. Their separate factory migration still uses
`registerPartFactory`. Extra Utilities remains an active supported converter consumer.

The [registry tests](../../src/test/java/codechicken/multipart/MultiPartRegistryCharacterizationTest.java) cover block
selection, first acceptance, captured lists, duplicates and exception propagation. The
[Forge tests](../../src/functionalTest/java/codechicken/multipart/test/PartConverterFunctionalTest.java) cover rejected
probes, original-tile ownership, callback order, repeated conversion, initialized example registration, NBT/packet
state and normal installation. The example compiles as Java 8 without Scala on its compile classpath. FMP still uses
Scala internally. Physical-client construction/rendering and real consumer inventory/network conversion remain
[manual integration checks](../migration/MANUAL_CHECKS.md); the example does not establish consumer adoption.
