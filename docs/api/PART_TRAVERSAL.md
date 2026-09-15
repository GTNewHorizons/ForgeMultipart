# Reading and visiting multipart parts from Java

[API index](../API.md)

Use `TileMultipart.jPartList()` for ordered collection access. The `algent/java` branch also adds
`forEachPart(Consumer<TMultiPart>)` for callbacks that skip detached parts. Consumers using the new method must require
an FMP release containing it; no minimum released version is established yet. Both methods work on generated tiles.

## Examples

Read all part types in list order:

```java
public static List<String> partTypes(TileMultipart tile) {
    List<String> types = new ArrayList<>();
    for (TMultiPart part : tile.jPartList()) {
        types.add(part.getType());
    }
    return types;
}
```

To visit only parts that are still bound to this tile:

```java
public static List<String> boundPartTypes(TileMultipart tile) {
    List<String> types = new ArrayList<>();
    tile.forEachPart(part -> types.add(part.getType()));
    return types;
}
```

The [complete Java example](../../src/test/java/codechicken/multipart/examples/PartTraversalExample.java) compiles and
runs in the normal test task. Its imports and compiled references contain no Scala types. FMP still needs its retained
Scala runtime internally. The example also compiles with Scala excluded from its classpath. The returned name lists
are independent copies.

## Collection ownership and ordering

`jPartList()` wraps the sequence returned by the virtual `partList()` getter at that call. It preserves every entry,
its index and its object identity, including detached parts. Use it for indexing, searches, serialization and ordinary
iteration. It does not require parts to have joined a world.

Normal FMP additions/removals replace an immutable sequence. A previously returned Java list keeps the old sequence;
call `jPartList()` again for the tile's new contents. A legacy setter or getter override can instead supply a mutable
sequence, in which case mutations of that sequence remain visible through its Java view. Therefore the method does
not promise either an independent snapshot or an unmodifiable list for every implementation.

Treat the view as read-only. Editing it, even when the backing sequence permits it, does not perform part binding,
slot updates, notifications or synchronization. Use `TileMultipart.addPart(world, pos, part)` and `tile.remPart(part)`
for normal world changes, retaining their returned tile because adding/removing capabilities can replace the tile
instance. To capture independent list storage, use `new ArrayList<>(tile.jPartList())`; the part objects remain shared.

## Callback behavior

Use the game thread. The default `forEachPart` traversal has the same behavior as the legacy `operate` hook:

- Capture the current sequence once and visit it in order. Normal additions during a callback publish a new sequence,
  so the current traversal does not visit the added parts.
- Check `part.tile() == this` immediately before each callback. Parts detached or rebound to another tile are skipped,
  including when an earlier callback replaces the tile and transfers its parts.
- A nested traversal captures the sequence anew and can observe the updated list. Each call has its own traversal state.
- Propagate the original callback exception immediately and stop that traversal. Side effects already performed remain.
- Preserve the iterator behavior of legacy mutable sequences. Do not structurally mutate such a sequence during traversal.
- Pass a non-null callback. For compatibility, there is no eager null check: with the default hook, null throws
  `NullPointerException` only when a part still bound to this tile reaches the callback. Empty sequences or sequences
  containing only detached/transferred parts invoke nothing.

`tile.jPartList().forEach(action)` visits every entry, including detached parts. It is not equivalent to
`tile.forEachPart(action)`. Preserve the consumer's original filtering and callback behavior when migrating.

## Overrides and legacy entry points

The dispatch paths are `jPartList()` → virtual `partList()`, and `forEachPart(consumer)` → virtual `operate(adapter)`.
The existing Java adapter and traversal implementation are shared; the legacy methods keep their descriptors and
bodies. Both legacy methods are deprecated for callers, with replacements named in their Javadocs.

Existing getter and `operate` overrides remain supported and can customize these behaviors. Lifecycle callbacks still
call `operate` directly. Overriding `forEachPart` intercepts calls to that convenience method only; it does not intercept
ticks, chunk callbacks or other lifecycle dispatch. Keep a legacy `operate` override in place if it supplies that hook.
No replacement lifecycle override contract is introduced by this addition.

## Consumer migration

| Existing use | Java replacement |
| --- | --- |
| `partList().size()` / `isEmpty()` / `indexOf(part)` | The corresponding `jPartList()` list method |
| Scala sequence indexing (`partList().apply(index)` or `tile.partList(index)`) | `tile.jPartList().get(index)` |
| Scala iterator for reading/searching every part | Enhanced `for` loop over `tile.jPartList()` |
| `operate(scalaFunction)` callback | `forEachPart(javaConsumer)`; preserve any existing hook overrides |

AE2's placement helper and Extra Utilities' renderer loops can use `jPartList()` without adding detached-part filtering.
ProjectRed's illumination calculations and packet/render indices, and OpenComputers' cable/print/network searches,
must retain their original order, filters and aggregation. Those mods can remain Scala internally while calling Java
collection methods. Existing Java consumers already using `jPartList()` need no rename.

GuideNH also reflects `partList`, `partList_$eq` and `loadParts` while reconstructing client tiles. Moving read-only
uses to the Java getter does not migrate its setter/loading contracts. Their [Java replacements](PART_LOADING.md)
are implemented; the consumer patch, release and pack adoption remain separate work. No reference checkout was edited.

The [adoption ledger](../migration/COMPATIBILITY.md#java-api-adoption-ledger) records inspected revisions and
the remaining release gates. JVM checks cover both getter forms, mutable storage, callback mutation/reentrancy,
detached/rebound parts, override dispatch and failures. The Forge test removes and adds real parts during traversal
of a generated slotted tile. Client rendering and full-pack adoption remain manual release checks.
