# Multipart render ID

[API index](../API.md)

`TileMultipart.getRenderID()` and `setRenderID(int)` expose FMP's existing global block-render registration ID.

```java
int renderType = TileMultipart.getRenderID();
```

See the [compiling example](../../src/test/java/codechicken/multipart/examples/RenderIdExample.java). Reading the ID does not initialize a client renderer, allocate an ID or access a tile.

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

## Legacy compatibility

The old static and companion `renderID()` / `renderID_$eq(int)` methods remain with unchanged descriptors and bodies,
and are deprecated in favor of the Java names. All readers and writers share the original storage.
