# Registering part factories from Java

[API index](../API.md)

Use `MultiPartRegistry.registerPartFactory(IPartFactory2, String...)` on the `algent/java` branch. It delegates to the
existing registry and avoids the Scala types in the `registerParts` overload family. This addition does not yet have
a minimum published dependency version.

To inspect a registered factory without constructing a part, see [factory lookup](FACTORY_LOOKUP.md), including
Schematica's replacement for private registry-map reflection.

## Example and timing

The [complete compiling example](../../src/functionalTest/java/codechicken/multipart/examples/PartRegistrationExample.java)
implements both `IPartFactory2.createPart` methods and registers one persistent type:

```java
MultiPartRegistry.registerPartFactory(new PartRegistrationExample(), PartRegistrationExample.PART_TYPE);
```

Call from your mod's common `preInit` or `init` handler on both sides, before FMP closes registration in `postInit`.
FML must have an active mod container; that container becomes the owner returned by `getModContainer(type)`.
Registration stores the factory without constructing, loading, binding or placing a part.

The example creates fresh, unbound instances whose `getType()` returns `yourmod:part`. Replace that sample ID and part
implementation with your own. Geometry, rendering and gameplay callbacks belong to the real part. Preserve already
published IDs exactly when migrating; adding a namespace to an existing ID changes its save/network identity.
Both single IDs and arrays are accepted. Multiple names can share a factory that selects the part class by name.

## Factory and payload ownership

`IPartFactory2` has two methods, so it is not a single-method lambda interface:

| Construction path | Factory input | What happens next |
| --- | --- | --- |
| Server/NBT | `createPart(String, NBTTagCompound)` receives the original tag object | Normal reconstruction calls `part.load` with that same tag |
| Client/description | `createPart(String, MCDataInput)` receives the original input, after the registry ID | Normal reconstruction calls `part.readDesc` with that same input at its current cursor |

A factory may inspect NBT or consume a packet discriminator to choose a class. There is no packet copy or rewind;
coordinate any consumed prefix with the part's `writeDesc` and `readDesc` protocol. FMP's microblock factory consumes
the material ID this way. Do not call `load` or `readDesc` in the factory merely to initialize the remaining payload,
because normal reconstruction calls them afterward. Return a fresh, unbound part for every supported name.

Direct `MultiPartRegistry.loadPart` and `readPart` calls only construct the part; their caller remains responsible for
the following payload/lifecycle work. The deprecated `createPart(name, boolean)` passes a null NBT or packet argument;
factories reached through that old path must tolerate it. Neither registration entry adds eager factory validation.

## Ordering and failures

IDs are processed in array order. The array itself is not retained, so changing it afterward does not rename registered
parts. The factory object is retained. A duplicate ID throws immediately, leaving any earlier IDs from that call
registered and skipping the suffix. Registration is not transactional; fix duplicate IDs rather than retrying the
whole batch. Later server ID-map setup sorts registered names; registration order is not a stable network numeric ID.

The new entry preserves the old Java array entry's failure order: a closed registry fails first, then the registry
enters its registering state and checks FML's active container, then it reads the array. Empty arrays still perform
the state/container checks; a null array fails after them. Supply a non-null factory and valid, unique non-null names;
the legacy implementation does not eagerly validate those values. Iterator conversion in the retained Scala sequence
entry still occurs before these checks, so malformed Scala inputs can fail earlier.

Factory exceptions propagate when constructing parts, not during registration. A registration call does not probe
factory results or promise that a later NBT/packet will be accepted.

## Migrating old callers

- `IPartFactory2` callers: retain the factory and IDs, then call `registerPartFactory`. ProjectRed can keep its existing
  server/NBT and client/packet methods. Scala code can pass a `String[]`/`Array[String]` as Java varargs using `: _*`.
- `IPartFactory` or Scala `(String, Boolean) => TMultiPart` callers: implement `IPartFactory2`; route the NBT method to
  the old constructor with `false` and the packet method with `true`. Keep any side-specific class selection. A factory
  that ignores the old Boolean, such as ForgeRelocationFMP's frame factory, can construct the same part class in both.
- Keep [converter registration](BLOCK_CONVERTERS.md) and trait registration separate. This change neither replaces `registerConverter` nor
  completes ProjectRed's external microblock trait migration or Schematica's private registry-map migration.

All eight old static/companion registration descriptors remain. Both `IPartFactory2` sequence entries are now
deprecated; the older Boolean/function entries were already deprecated and now point to the supported replacement.
The old Java array entries remain callable too. Prefer the new name for consumer Java source: a compiler probe found
that the old overload family required `scala.collection.Seq` and `scala.Function2` even for Java factory/string inputs.
The new example compiles with Scala excluded from its classpath. FMP still requires Scala internally.

The consumer ledger records ProjectRed and ForgeRelocationFMP migration directions. Their reference checkouts were
not changed; consumer releases and target-pack adoption are still required before retiring the old entries.

## Validation

Forge tests register through all old entries during real mod initialization and exercise lazy construction, active
container ownership, array ownership, partial duplicate registration, failure ordering, both factory paths and shared
payload objects/cursors. The new entry runs through the same checks, and the example registers during initialization.
The archived pre-change Forge test mod and JVM fixtures run against the new implementation without recompiling their
callers. Existing member descriptors, method bodies, Scala signatures and generated output are checked separately.
The dedicated-server suite invokes the client construction method with a real packet; physical-client rendering and
full-pack adoption remain [manual release checks](../migration/MANUAL_CHECKS.md).
