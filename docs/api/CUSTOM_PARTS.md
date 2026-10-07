# Writing and placing a custom part

[API index](../API.md) · [Factory registration](PART_REGISTRATION.md)

This guide connects the ordinary custom-part lifecycle from registration through placement, saved state, client
state and removal. The [compiling example](../../src/functionalTest/java/codechicken/multipart/examples/PartRegistrationExample.java)
uses one integer as representative state; replace its empty geometry and rendering with the capabilities your part
needs.

The APIs shown here are available on the `algent/java` branch and do not yet have a minimum published dependency
version. Existing part binaries remain supported; use the [release notes](../RELEASE_NOTES.md) when rebuilding source.

## Register and construct

Give every published part type a stable persistent ID and register its factory during common `preInit` or `init`, on
both client and server:

```java
MultiPartRegistry.registerPartFactory(new PartRegistrationExample(), "yourmod:part");
```

The factory returns a fresh, unbound part. It should select the implementation, not finish loading it: FMP calls
`load` after the NBT factory path and `readDesc` after the packet factory path. Preserve existing IDs and packet
prefixes when migrating. See [factory registration](PART_REGISTRATION.md) for payload ownership and failure ordering.

A basic part extends `TMultiPart`, returns its registered ID from `getType()`, and overrides only the capabilities and
callbacks it needs. The base class ticks by default, so a part without per-tick logic should return `false` from
`doesTick()` as the example does. Add collision/ray-trace boxes, drops, rendering, slots, redstone or other capability
interfaces according to the part's real behavior.

## Place on the server

Choose the target from the item or interaction, then check and place on the server:

```java
TileMultipart tile = PartRegistrationExample.place(world, pos, initialValue);
if (tile == null) {
    return false;
}
```

The example returns null on the client or when `TileMultipart.canPlacePart` rejects the candidate. A successful
`TileMultipart.addPart` installs or updates the generated container, binds the part, calls `onAdded`, notifies the
world and clients, and returns the current tile. Retain that return value: adding a capability can replace a previous
tile instance. A successful `canPlacePart` is only a check; do not mutate inventory or other durable state until
`addPart` succeeds.

## Persist and synchronize state

Use separate callbacks for disk and network state:

| Purpose | Write | Read | When |
| --- | --- | --- | --- |
| Saved server state | `save(NBTTagCompound)` | `load(NBTTagCompound)` | World save and server reconstruction |
| Initial client state | `writeDesc(MCDataOutput)` | `readDesc(MCDataInput)` | Chunk description and part addition |
| Later client changes | `sendDescUpdate()` | Base `read` calls `readDesc` | Buffered part update |

The example's `setValue` is server-owned and performs the three actions needed because its value is assumed to affect
all three concerns:

```java
sendDescUpdate();
tile().notifyPartChange(this);
tile().markDirty();
```

`sendDescUpdate` synchronizes the description payload; the base client `read` then calls `readDesc` and marks the tile
for rendering. `notifyPartChange` calls sibling-part callbacks and updates the block, neighbors and lighting.
`markDirty` requests persistence. Use only the calls the changed state requires, but keep packet reads exactly
symmetric with writes. Run authoritative mutation on the server; the client applies network state in `readDesc` or a
custom `read` implementation.

## Lifecycle callbacks

`onAdded` / `onRemoved` describe placement and removal. Their base implementations call `onWorldJoin` /
`onWorldSeparate`. Server chunk load/unload and movement can call the world-level hooks again, while initial client
reconstruction calls `onWorldJoin` without `onAdded`. Put repeatable external-network registration in the world-level
hooks, and one-time placement/removal effects in the narrower hooks. Preserve the base call when overriding a narrow
hook unless deliberately replacing its world-level behavior.

`preRemove` runs before the part leaves storage and before its binding is cleared. Remove on the server and retain the
returned tile because dropping the last capability can replace the container, while removing the final part returns
null after the block is removed:

```java
TileMultipart current = part.tile();
TileMultipart replacement = current.remPart(part);
```

Ordinary placement and removal already perform container synchronization and notifications. The generation,
`setPartList` and `loadPartList` APIs are for controlled reconstruction and have different ownership rules; see
[part loading](PART_LOADING.md) and [composite generation](COMPOSITE_GENERATION.md) before using them.

## Validation boundary

`compileJavaApiExamples` compiles this example against the packaged dev jar with Java 8 and without Scala libraries.
The Forge functional suite covers registration, placement, binding, NBT/description state, updates and final removal.
Real geometry, interaction and rendering still need a physical-client check for the implementing mod.
