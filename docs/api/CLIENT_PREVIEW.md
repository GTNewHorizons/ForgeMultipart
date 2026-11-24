# Building client multipart previews

[Java API index](../API.md)

Use `MultipartHelper.createPreviewTileFromNBT(World, NBTTagCompound)` to rebuild saved multipart NBT as an
uninstalled client tile. It replaces the registry reflection, client-part promotion, Scala collection conversion,
composite generation and manual loading previously needed by preview integrations.

```java
TileMultipart preview = MultipartHelper.createPreviewTileFromNBT(clientWorld, savedTile);
if (preview == null) {
    return false;
}
return MultipartRenderer.renderWorldBlock(
    blockAccess, x, y, z, multipartBlock, multipartBlock.getRenderType(), renderBlocks);
```

The [compiling example](../../src/functionalTest/java/codechicken/multipart/examples/ClientPreviewExample.java) uses
only Java API types. Keep this code in a client integration class because `MultipartRenderer` is client-only.

## Reconstruction contract

The helper accepts only a `savedMultipart` tag with at least one supported part. It preserves part order, loads each
part from its original tag, generates the required client tile traits, restores tile coordinates and NBT state, and
binds the parts. With a client world it then calls `onWorldJoin`, broadcasts a local part change, marks the render and
rebuilds the client render cache. With a null world it creates a worldless staged preview that only loads and binds
its parts: no `onWorldJoin` or `onPartChanged` callbacks run, since parts may assume a world there. Stock microblocks
(covers, hollow covers, strips, corners and posts) normally compute their client render bounds in those callbacks, so
the helper recalculates them directly; this needs only the bound tile and gives the same bounds as a world-backed part
change. Render caches initialize lazily.

Other mods' parts get no such initialization, and attaching a world afterwards does not run the skipped callbacks.
A part whose client render state is built only in `onAdded`, `onWorldJoin` or `onPartChanged` (WR-CBE's transmitter
base bounds, for example) should build it on first render when it is still missing. If that is not possible, return
null from `createPartForClientPreview`: the factory cannot tell whether a world will be supplied, so this leaves the
whole tile out of every preview instead of failing while it renders.

Part `load(tag)` runs before any world is attached, with whatever `MultipartSaveLoad.loadingWorld()` last held. A
part that reads it while loading (ProjectRed's active timer gates) sees a stale world, or null on a client connected
to a remote server, where the resulting exception propagates and drops the tile. FMP 2.0.0 behaves the same way.

Use this helper only for client-side previews. Server-thread work, such as rebuilding tiles while saving in an
integrated server, should use `TileMultipart.createFromNBT` even on a physical client.

A non-null world must have `isRemote == true`. The helper does not validate placement, install the tile in a chunk,
notify neighbors, recalculate lighting or send packets. It reads the supplied NBT without mutating it.

Reconstruction is all-or-nothing. A foreign or empty tag, missing factory, or factory that does not support preview
construction returns null. Factory, part loading and generated-trait failures propagate.

## Factory support

`IPartFactory2.createPartForClientPreview(String, NBTTagCompound)` constructs the client variant needed for saved NBT.
The default returns null, so an existing packet-aware factory does not accidentally treat NBT as a description
packet. Override it when the factory can return a fresh, unbound client part without consuming or changing the tag:

```java
@Override
public TMultiPart createPartForClientPreview(String name, NBTTagCompound tag) {
    return new ExamplePart();
}
```

The helper calls `part.load(tag)` afterward. Legacy Boolean and Scala-function factory adapters already route preview
construction through their client branch. Built-in microblock factories resolve the saved material name and construct
the generated client microblock before loading its shape and other state.

## Consumer migration

- Schematica can replace its private registry map, `MicroblockClass.create`, Scala iterable conversion, companion
  generator, reflected loader and per-part refresh loop with this helper.
- GuideNH can replace its server-tile reconstruction, `TileMultipartClient` promotion, microblock promotion and
  Scala reload/finalization with this helper. Its material export and part statistics already use typed Java traversal.

Keep the old reflection fallback only while supporting an older FMP release that lacks this API.
