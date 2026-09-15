# Loading and assigning multipart parts from Java

[API index](../API.md)

The `algent/java` branch adds `TileMultipart.loadPartList(Collection<TMultiPart>)` and `setPartList(List<TMultiPart>)`.
They adapt to the existing virtual Scala-parameter hooks. Both legacy entries are deprecated for callers and remain
supported for existing binaries, subclasses and reflection. No minimum released dependency version is established yet.

These are reconstruction operations, not normal placement/removal. Use `TileMultipart.addPart` / `remPart` for world
changes. Use `MultipartHelper.createTileFromParts(Iterable<TMultiPart>)` to construct a new server composite tile; the
loader itself does not choose traits, promote a tile or install it in the world.

For separate client/server generation before world/NBT setup and loading, use the
[staged composite-generation API](COMPOSITE_GENERATION.md).

## Loading a prepared tile

To rebuild a tile using its existing parts, capture them before the loader clears the tile:

```java
public static void reloadParts(TileMultipart tile) {
    List<TMultiPart> parts = new ArrayList<>(tile.jPartList());
    tile.loadPartList(parts);
}
```

For a supplied collection, call `preparedTile.loadPartList(parts)`. The tile must already have all capabilities required
by those parts. An arbitrary `new TileMultipart()` does not acquire slot, inventory or other generated traits by loading.

The default implementation:

1. Calls the virtual `clearParts()` hook to clear stored parts and trait caches that implement that hook.
2. Reads the supplied collection in its iteration order. For each part, appends it to stored state, calls `bindPart`,
   then `part.bind(tile)`, and enables ticking if needed. It retains the existing 250-part limit.
3. With a client world, visits bound parts through the legacy `operate` hook to call `onWorldJoin`.
4. With either kind of world, calls `notifyPartChange(null)`, which notifies parts, marks the block for update,
   notifies neighboring blocks and recalculates lighting. Without a world, neither world step runs.

Loading does not call `onAdded`, call old parts' removal/separation callbacks, unbind old parts, reset an already-set
ticking flag, check occlusion/duplicates, or send a complete multipart description packet. It is not a general live-tile
replacement transaction. Reconstruction callers remain responsible for their world/position setup, render updates,
tile notifications and any full description synchronization required by their existing flow.

The Java collection is viewed during the call, not copied first. Its iterator determines the order and mutation
behavior; do not structurally modify it from callbacks. Replacing a pending list entry during a binding callback can
change which part is loaded next. After successful loading, normal stored sequence snapshots no longer track later
edits to the input collection. Part objects remain shared.

Call on the game thread. Failures propagate without rollback: clearing happens before iteration, and each part is
published before binding. A null collection fails after clearing; a null part can remain in partially loaded storage
before binding fails. A callback exception likewise leaves changes already performed. Supply valid parts rather than
using these failures to validate a proposed live-world change.

## Storage assignment

`preparedTile.setPartList(parts)` takes a shallow immutable copy of a non-null Java list, then passes that sequence to
the virtual legacy setter. It preserves order, duplicates, null entries and object identities. Editing the original
list later does not alter the stored sequence; parts themselves are not copied.

The setter performs storage assignment only. It does not bind parts, rebuild slot/inventory/render caches, update
ticking, validate parts or notify anything. Existing legacy setter overrides can customize that behavior. Use it only
where a reconstruction flow specifically needs to stage stored state before its separate loading/binding step.

A null list preserves the legacy unset-state sentinel. The generated client render-cache code handles that sentinel;
most ordinary part queries do not. Use an empty list for ordinary empty storage. The old Scala setter still retains
the exact supplied sequence, including mutable aliasing; the new Java setter deliberately copies a non-null list.

Both operations appear in the [compiling Java example](../../src/test/java/codechicken/multipart/examples/PartLoadingExample.java).
The example runs in the normal test suite and uses no Scala imports. Its staging method illustrates assignment only,
not a complete tile-promotion procedure.

## Overrides and reflection

Dispatch is `setPartList(List)` → virtual `partList_$eq(Seq)` and `loadPartList(Collection)` → virtual
`loadParts(scala.collection.Iterable)`. Internal reconstruction and storage writes continue using the legacy hooks.
Keep existing overrides there; overriding just the new Java sibling does not intercept all internal calls.

GuideNH's existing assignability-based reflection still selects the Scala loader for a Scala sequence. Schematica's
exact `loadParts(scala.collection.Iterable)` lookup also remains valid. Java reflection can now request
`getMethod("setPartList", List.class)` or `getMethod("loadPartList", Collection.class)`. Select by parameter type, not
by the first method with the right name.

The Java loader has a distinct name deliberately: the initially planned `loadParts(Collection)` overload made
`javac --release 8` require `scala.collection.Iterable` during overload resolution even with a Java list argument.
`loadPartList` lets the example compile without Scala on the consumer compile classpath, while retaining the legacy
hook and its name. This changes the planned Java name, not any existing entry point.

For GuideNH's migration, use a Java list of the promoted client parts, retain its existing world/position setup,
replace the raw setter with `setPartList`, and call the Collection loader. Preserve its following `notifyTileChange`
and `markRender` steps. Its tile generator now has a [static Java replacement](COMPOSITE_GENERATION.md); other
reflective dependencies remain. Schematica can select the Collection loader and pass its existing Java list directly,
using the separate [factory lookup](FACTORY_LOOKUP.md) and generation entries to remove those Scala dependencies.

## Validation and release gates

JVM tests pin storage ownership, old and new override dispatch, exact/assignable reflection, ordered non-list inputs,
callback input mutation and partial-failure behavior. Forge tests cover slot-cache rebuilding and server notifications,
plus Java assignment/loading on a generated client tile and explicit render-cache rebuilding. The latter does not run
a real client world or GPU rendering; GuideNH/Schematica previews and full-pack adoption remain manual release gates.

The [consumer ledger](../migration/COMPATIBILITY.md#java-api-adoption-ledger) records the inspected source
revisions and pending consumer changes/releases. The supplied reference checkouts are unchanged.
