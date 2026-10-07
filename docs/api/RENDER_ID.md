# Multipart render ID

[API index](../API.md)

`TileMultipart.getRenderID()` and `setRenderID(int)` expose FMP's existing global block-render registration ID on
the `algent/java` branch. They have no minimum published dependency version yet.

```java
int renderType = TileMultipart.getRenderID();
```

The [compiling example](../../src/test/java/codechicken/multipart/examples/RenderIdExample.java) also builds with Scala
excluded from its classpath. Reading the ID does not initialize a client renderer, allocate an ID or access a tile.

## Meaning and lifecycle

The initial value is `-1`. FMP's client renderer obtains an ID from Forge's `RenderingRegistry` when the renderer
singleton is first initialized, normally from client postInit. `BlockMultipart.getRenderType()` and the renderer's
`getRenderId()` read that same shared value. The dedicated server normally retains `-1`.

This is not a per-tile setting, a part type ID, a microblock material ID or a persistent identifier. Read the value
after normal client initialization when comparing render types; do not cache `-1` as the eventual client ID.

The setter is for controlled render setup. It only assigns the existing field, accepting every `int` unchanged,
including negative values. It does not reserve an ID, register a handler, update Forge's mapping, synchronize clients
or rebuild rendering. FMP's later renderer initialization still performs its own assignment. Ordinary integrations
should read the value and leave registration to FMP. Make any intentional setup changes on the initialization/game
thread; there is no new synchronization guarantee.

## Compatibility and validation

The old static and companion `renderID()` / `renderID_$eq(int)` methods remain with unchanged descriptors and bodies,
and are deprecated in favor of the Java names. All readers and writers share the original storage. Existing internal
block/renderer calls still use their old paths. No direct old-accessor use or Java-name collision was found in the
supplied consumer sources or the frozen downstream member inventory; consumer checkouts are unchanged.

JVM tests cover the initial sentinel, both legacy setters, Java accessors, signed integer boundaries and multiple
block instances reading the global value. Forge verifies the server sentinel and shared block state without loading
the client renderer. Frozen callers, class APIs and generated output are checked separately. Actual client allocation,
handler registration and GPU rendering remain [manual release checks](../migration/MANUAL_CHECKS.md).
