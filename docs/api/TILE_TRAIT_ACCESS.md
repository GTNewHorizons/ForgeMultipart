# Calling generated tile capabilities from Java

[API index](../API.md) · [Composite generation](COMPOSITE_GENERATION.md)

Call generated tiles through `TileMultipart` or a stable public capability interface. Several dev-jar classes under
`scalatraits`, and `TileMultipartClient`, are raw inputs to FMP's transformer: Forge changes them into interfaces
before generating a concrete tile. A Java signature that compiles against those classes can still fail at runtime.

## Why raw trait calls fail

These consumer expressions compile against the current dev jar but are unsafe:

```java
((TRedstoneTile) tile).openConnections(side); // javac emits invokevirtual for a class
((TSlottedTile) tile).v_partMap;              // javac emits getfield
```

At runtime `TRedstoneTile` is an interface, so the first call throws `IncompatibleClassChangeError`. `TSlottedTile`
has generated accessor methods instead of that field, so the second throws `NoSuchFieldError`. A cast or an
`instanceof` check alone can still succeed; that does not make subsequent class-method/field bytecode valid.
Merely adding Scala to the compile classpath does not turn these raw Java classes into interface stubs.

Already compiled consumers using the original interface/accessor descriptors remain supported. Recompiling source
against the raw Java port is a separate gate. Do not instantiate or subclass raw trait inputs in consumer code, or
call their generated `$class` helpers as a new integration strategy. Reflection is not the migration target.

## Stable entry points

| Need | Compile against | Contract |
| --- | --- | --- |
| Parts, slot reads, placement, lifecycle | `TileMultipart` | `jPartList()`, `partMap(slot)`, placement and documented notifications dispatch through generated overrides |
| Open redstone geometry | `IRedstoneTile` | `openConnections(side)`; capability is present when the tile hosts a redstone part |
| Offered redstone connections or masked power | `IRedstoneConnector` | `getConnectionMask(side)`, `weakPowerLevel(side, mask)`; also used by non-FMP connectors |
| Ordinary tile power queries | `TileMultipart` | `strongPowerLevel(side)`, `weakPowerLevel(side)`; preserve each method's side convention |
| Inventory or sided inventory | Minecraft `IInventory` / `ISidedInventory` | Use the interface implemented by the tile, rather than raw `JInventoryTile` or its internal caches |
| Fluid operations | Forge `IFluidHandler` | Use the interface rather than raw `TFluidHandlerTile` or `tankList` |
| A consumer-owned pass-through capability | The original ordinary Java interface | Register with `MultipartGenerator.registerPassThroughInterface`; generated calls forward to the implementing part |

Test the capability when it is optional. Keep using the tile returned by placement/removal because adding or removing
capabilities can replace the tile instance. These interfaces do not give a snapshot, reserve a tile, or add thread
safety. Apply the owning method's lifecycle and side rules. Client-only rendering calls remain client-only even when
a stable base method exists; a headless success does not establish physical-client rendering.

## Compiling redstone example

[TileTraitAccessExample.java](../../src/functionalTest/java/codechicken/multipart/examples/TileTraitAccessExample.java)
uses only `TileMultipart` and `IRedstoneTile` and compiles as Java 8 without Scala on the classpath:

```java
boolean open = TileTraitAccessExample.hasOpenConnection(tile, side, mask);
```

The example returns false for a null tile or absent capability. Its caller supplies a Minecraft side in 0..5 and
a mask using bits 0..3 for rotations around that side and bit 4 for the center. `openConnections` describes openings
after face/edge obstruction; `getConnectionMask` describes the connections offered by contained parts. They are not
interchangeable. The API does not validate the side or add a cache.

ProjectRed's `transmission/redwires.scala` already knows its tile supports redstone. Its migration can change only
the cast type, preserving the surrounding mask and rotation calculation:

```scala
tile.asInstanceOf[IRedstoneTile].openConnections(absDir)
```

That emits an interface call with the stable owner. ProjectRed can remain Scala internally. Keep its legacy
`TRedstoneTile.openConnections` binary contract until release and pack adoption.

## Refreshing a changed slot mask

Use `TileMultipart.refreshPartSlots(part)` after an already stored part validates and applies a new
`TSlottedPart.getSlotMask()`. It provides the Java replacement for OpenComputers' direct `TSlottedTile.v_partMap`
mutation:

```java
tile.refreshPartSlots(this);
tile.notifyPartChange(this);
sendDescUpdate();
```

The [compiling example](../../src/functionalTest/java/codechicken/multipart/examples/TileTraitAccessExample.java) keeps
the cache operation itself separate because validation, state changes and notification policy belong to the part:

```java
TileTraitAccessExample.refreshSlots(tile, part);
```

On a generated slotted tile, FMP scans the live slot array in index order, clears every entry for which
`Objects.equals(stored, part)` is true, then dispatches the existing virtual `bindPart(part)` once. This preserves
Scala `==` behavior, including distinct equal objects. It reads the array directly, matching the old consumer rather
than a possible `partMap` override. Unrelated slots, part-list storage and tile ownership stay unchanged. The current
mask may overwrite a slot if the caller skipped its normal replacement/occlusion check.

A tile without the slotted capability does nothing. The method does not check that the part is stored, bound to this
tile or slotted, and it does not validate placement, send notifications, mark dirty/render state or schedule updates.
Call it on the owning world thread only after the new shape passes the consumer's existing `canReplacePart` logic.
Keep the tile/part's existing notification, sound, packet and scheduling sequence afterward.

`bindPart` dispatches through every generated cache trait, so those hooks still run once. As before, some non-slot
caches append on bind and are not generally idempotent. This API matches OpenComputers' print part, whose additional
capabilities do not append such caches; it is not a general refresh for inventory/fluid capability changes. A whole
`loadPartList` reconstruction would run broader cache, binding and notification behavior and is not equivalent.

For custom generated tiles, keep consumer-callable methods on an ordinary Java capability interface and use the
stable base for existing tile hooks. The [custom tile-trait guide](CUSTOM_TILE_TRAITS.md) provides the complete
registration, helper, state/lifecycle and access pattern. Pass-through registration remains the smaller choice when
a tile should only forward one interface to one implementing part.

No transformed compile-stub artifact is introduced here: redstone already has a stable interface and slot refresh is
available on the stable base tile. Assess remaining trait-only requirements individually before adding build
machinery. Other audited private/reflection contracts remain open.

## Validation

[Forge fixtures](../../src/functionalTest/java/codechicken/multipart/test/TileTraitAccessFunctionalTest.java) execute
[actual javac-compiled unsafe callers](../../src/functionalTest/java/codechicken/multipart/test/RawTileTraitCalls.java)
against transformed traits and assert both linkage failures. Stable interface/base calls succeed on the same tiles,
and the example checks every five-bit mask, absent capabilities and slot refresh. The slot fixtures compare the new
entry with the frozen consumer sequence, including equality, current-mask rebinding, unrelated slots, ownership and
storage. Packaged-jar compilation and bytecode inspection verify that the example uses stable owners without Scala,
reflection or raw trait-class references. Existing binary APIs remain callable; `refreshPartSlots` is additive.

This is consumer source-access coverage. It does not validate physical-client rendering or establish that any consumer
has released and adopted a migration. Generated custom tile traits have separate both-side coverage in the
[authoring guide](CUSTOM_TILE_TRAITS.md#validation-and-adoption).
