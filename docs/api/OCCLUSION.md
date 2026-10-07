# Testing occlusion from Java

[API index](../API.md)

The `algent/java` branch adds `TileMultipart.testOcclusion(Collection<? extends TMultiPart>, TMultiPart)`.
It tests a supplied collection of parts through the tile's existing occlusion hook, including generated
partial-occlusion checks. This addition is not yet tied to a minimum published dependency version.
For raw geometry, `NormalOcclusionTest.testBoxes(Iterable<? extends Cuboid6>, Iterable<? extends Cuboid6>)` tests
two groups of bounding boxes without constructing parts. See [box-versus-box queries](#box-versus-box-queries).

## Example

Use a `NormallyOccludedPart` to test a normal bounding shape against a supplied group of parts:

```java
public static boolean fitsShape(TileMultipart tile, Collection<? extends TMultiPart> parts, Cuboid6 bounds) {
    return tile.testOcclusion(parts, new NormallyOccludedPart(bounds));
}
```

The [complete Java example](../../src/test/java/codechicken/multipart/examples/OcclusionExample.java) compiles and runs
in the test suite. It also compiles with Scala excluded from its classpath and has no Scala bytecode references.
FMP still retains its Scala runtime internally. Collections of part subclasses, such as `List<NormallyOccludedPart>`,
are accepted without a cast.

Pass `tile.jPartList()` to test against the tile's currently stored parts, or pass a separate collection for a preview
or selected subset. To exercise a real candidate's custom occlusion behavior, pass that part instead of the shape
wrapper. The wrapper represents normal bounding geometry, not all the capabilities of a custom part.

## What the query checks

The base tile tests each supplied part against the candidate, then tests the candidate against that same part. It
proceeds in input iteration order and stops immediately when either direction rejects. Unbound/detached parts and
duplicate entries participate; the candidate is not automatically excluded if it is also in the collection.

Generated partial-occlusion tiles retain their extra aggregate check. When the candidate implements `JPartialOcclusion`,
the existing trait checks the group plus the candidate before the ordinary pair callbacks. A plain `TileMultipart`
does not gain that capability just because the supplied parts need it. Use the appropriate composite tile; this method
does not generate or select tile traits.

This is an occlusion query, not full placement approval. It does not check placement permissions, duplicate membership,
slot occupancy or world replaceability, and does not bind/store parts or send notifications. For ordinary placement
and replacement, continue using `canPlacePart`, `canAddPart` and `canReplacePart` as appropriate. In particular,
`canReplacePart(outgoing, candidate)` performs its existing outgoing-part exclusion; this collection query does not.

## Ownership, callbacks and failures

The Java entry copies the input into an immutable sequence before calling the legacy hook. Order, duplicates, null
entries and part identities are preserved. Callback edits to the caller's collection do not change the current
query's sequence. Part objects are shared, so mutations of their state can affect later callbacks. A nested query
takes its own copy.

Copying failures, including a null collection, propagate before the legacy hook is called. There is no eager
candidate/null-entry validation: with the base implementation, an empty collection accepts even a null candidate,
and an early rejection can avoid a later null dereference. Callback exceptions propagate unchanged and stop the query;
callback side effects already performed are not undone. Overrides may add checks or customize these boundaries.

Use the game thread when querying live parts. A geometric query may invoke arbitrary part callbacks, including ones
that read or modify world/part state; it is not a thread-safety guarantee.

## Overrides and migration

Dispatch is `testOcclusion(Collection, candidate)` → virtual `occlusionTest(scala.collection.Seq, candidate)`.
The old method keeps its descriptor and body, and is deprecated for callers. Existing subclasses and generated traits
continue to override it. `canAddPart` and `canReplacePart` still call that hook directly; overriding only the new
Java method does not intercept their checks. The distinct Java name avoids introducing another Scala/Java overload
pair, following the [loader compilation finding](PART_LOADING.md#overrides-and-reflection).

No direct consumer calls or overrides of the two-argument tile hook were found in the supplied source/ABI audit.
Consumers such as ProjectRed and ForgeRelocationFMP already use the Java-typed `canReplacePart` API and need no rename
for that contract. Part-level `occlusionTest(TMultiPart)` remains supported as-is. The Scala `NormalOcclusionTest.apply`
box-versus-box entry is a separate, still-used contract with the Java replacement below.

## Box-versus-box queries

Use `NormalOcclusionTest.testBoxes` when you already have box iterables, including `getOcclusionBoxes()` results:

```java
public static boolean fitsBounds(Iterable<? extends Cuboid6> occupied, Cuboid6 candidate) {
    return NormalOcclusionTest.testBoxes(occupied, Collections.singletonList(candidate));
}
```

The [complete example](../../src/test/java/codechicken/multipart/examples/BoxOcclusionExample.java) compiles without
Scala on its classpath. Both arguments accept box subclasses and need only implement Java `Iterable`.

The query fully consumes the first input, then the second, requesting each input's iterator once. It takes shallow
snapshots before testing any pairs, even when one input is empty or the first pair will overlap. Iteration failures
therefore take precedence over geometry results; a failure in the first input prevents reading the second. Input
collections can change during intersection callbacks without changing the captured membership, but boxes themselves
are shared: coordinate changes remain visible. Supply finite iterables and use the game thread for live geometry.

Each first-group box calls `intersects` on second-group boxes in encounter order. Duplicates participate; the first
intersection returns `false`. No intersections returns `true`. The existing `Cuboid6.intersects` tolerance is retained:
touching faces and sub-tolerance overlaps are allowed. Only cross-group pairs are tested, not pairs within one group.
Neither input nor its boxes is modified by the helper; custom iterators or `intersects` overrides can have side effects.

Null inputs fail during copying. Null box entries are not eagerly rejected: an empty opposite group never uses them,
and an earlier intersection may avoid them. A reached intersection involving an ordinary null box fails. Iterator and
intersection exceptions propagate unchanged, without undoing callback side effects.

This does not run part callbacks, generated aggregate partial-occlusion checks, slot checks or placement logic.
Use `tile.testOcclusion` for the tile's occlusion protocol, and the placement/replacement APIs for their wider checks.
The existing `NormalOcclusionTest.apply(JNormalOcclusion, TMultiPart)` part helper remains unchanged.

Both Scala box-list `apply(Traversable, Traversable)` entries, static and companion, are deprecated with their original
descriptors and bodies retained. OpenComputers' cable/network checks can pass `getOcclusionBoxes()` directly as the
second argument; their Scala `ownBounds` collection needs a Java adapter:

```scala
import scala.collection.JavaConverters._
NormalOcclusionTest.testBoxes(ownBounds.asJava, otherBounds)
```

ForgeRelocationFMP can similarly adapt its assembled Scala `boxes` sequence and pass its existing Java
`getOcclusionBoxes` iterable directly. Keep its combined normal, partial and collision boxes and caller order intact.
These are migration directions, not changes to the reference checkouts. Consumer releases and pack adoption remain
pending, and the new method has no published minimum dependency version yet. Scala consumers can adopt this FMP API
while keeping their own Scala code.

## Validation

JVM tests cover both directions, early rejection, original exceptions, null boundaries, snapshot ownership, duplicates,
subtype/non-list collections, override routing and the shape example. A Forge test demonstrates that the generated
partial trait rejects overlapping partial parts that plain pair tests accept. Existing descriptors, generated output,
and frozen pre-change callers are checked separately. Actual client previews and full-pack adoption remain covered
by the [manual release checks](../migration/MANUAL_CHECKS.md).

Box-query tests run the same snapshot, input-failure, ordering, null and exception contracts against both legacy entries
and the Java entry. The compiling example also covers touching tolerance and containment; frozen pre-change tests
retain calls to both old descriptors.
