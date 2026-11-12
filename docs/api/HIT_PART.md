# Struck part index

[API index](../API.md)

`BlockMultipart.hitPartIndex(MovingObjectPosition)` returns the index of the part a multipart ray trace struck, or
`-1` if the hit carries no part index.

```java
int index = BlockMultipart.hitPartIndex(hit);
List<TMultiPart> parts = tile.jPartList();
TMultiPart struck = index >= 0 && index < parts.size() ? parts.get(index) : null;
```

See the [compiling example](../../src/test/java/codechicken/multipart/examples/HitPartExample.java).

## Where the index comes from

`TileMultipart.rayTraceAll` asks each part in `jPartList()` order for its own hit. It then wraps each hit's data as
`(index, partData)`, so a part that misses still uses up its index. `collisionRayTrace` returns the nearest of those
hits. A hit from `BlockMultipart.collisionRayTrace` or `RayTracer.retraceBlock` on a multipart block goes through the
same path.

The index refers to the tile's part list when the trace ran. Adding or removing parts afterwards makes it stale, so
retrace rather than keeping a hit across ticks. The method reads only the hit. It does not check coordinates, look
up a tile or bounds-check the index. Retrieve the tile at the hit's block yourself, as the example does.

`-1` means the hit is null, a plain `MovingObjectPosition`, or an `ExtendedMOP` whose data is not FMP's index pair.
For example, another mod's tile can produce an `ExtendedMOP` with its own data.

## Legacy compatibility

`BlockMultipart.reduceMOP(MovingObjectPosition)` and its companion entry remain unchanged and are not deprecated.
They return a `scala.Tuple2` of the boxed `Integer` index and a copy of the hit rebased onto the part's own data.
FMP's activation, click, harvest, hardness, hit-effect, pick-block and highlight dispatch uses the rebased hit; there is no Java replacement for it
because no consumer needs one.

GT5U `5.09.54.205` reads `(Integer) BlockMultipart.reduceMOP(hit)._1()` to recolor ProjectRed wires. That call
can become `hitPartIndex(hit)` with the same value and no Scala types. Keep the existing `hit instanceof ExtendedMOP`
and coordinate checks, which protect the tile lookup. The released GT5U jar keeps linking the `Tuple2` descriptor,
which stays until a migrated release is in the target pack.
