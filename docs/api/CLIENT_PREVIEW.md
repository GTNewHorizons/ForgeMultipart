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
rebuilds the client render cache. With a null world it creates a worldless staged preview; render caches initialize
lazily.

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
- GuideNH at `16f142417df1` can replace its server-tile reconstruction, `TileMultipartClient` promotion, microblock
  promotion and Scala reload/finalization with this helper. These are now direct calls; reflection and raw part-list
  assignment were removed in `4a0cd02e`. Material export and part statistics already use typed Java traversal.

Optional integrations should isolate their FMP-typed client class behind mod-presence and supported-version checks.
Keep the old reflection fallback only while supporting an older FMP release that lacks this API.

## Validation limits

JVM checks pin the public method and factory opt-in default. Forge coverage checks legacy factory adapters plus ordered
NBT loading into an unbound generated client tile. The example compiles with Scala excluded. Physical-client checks
remain required for generated microblock client traits, custom preview factory implementations and GPU rendering; see
the [manual release checks](../migration/MANUAL_CHECKS.md).
