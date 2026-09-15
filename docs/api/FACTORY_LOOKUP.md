# Registered part factory lookup

[Java API index](../API.md)

`MultiPartRegistry.getPartFactory(String)` returns the registered `IPartFactory2` for an exact part type name, or
`null` when no factory is mapped. It is available on `algent/java`; no minimum published dependency version exists yet.

```java
IPartFactory2 factory = MultiPartRegistry.getPartFactory("mcr_face");
```

Lookup is case-sensitive, uses string equality and reads the current registration map. It neither constructs a part,
logs unknown names, waits for registration nor changes registry state. It does not depend on the numeric network ID
map. Look up after the owning mod has registered its types, on the initialization/game thread; no synchronization is
added. Use published, non-null type names.

The result is the exact registered factory instance, not a copy or a mutable registry view. Do not modify its state.
Registration remains the job of [`registerPartFactory`](PART_REGISTRATION.md); lookup does not expose a replacement,
removal or enumeration operation. An earlier lookup result does not update itself if registry state later changes.

## Choose the construction path

Most consumers should continue using `loadPart(name, nbt)` or `readPart(input)`. Those choose the registered factory's
NBT/server or packet/client method. Their caller still invokes `part.load(nbt)` or `part.readDesc(input)`, respectively,
and later binds/places the part. See [factory and payload contracts](PART_REGISTRATION.md).

Factory lookup is useful when a supported factory subtype provides a specialized construction method. Schematica
looks up a `MicroblockClass` and calls `create(client, materialId)` before loading the saved microblock NBT. Calling
`loadPart` instead would select the server/NBT construction path, even for a client preview. Passing a null packet or
using deprecated `createPart(name, true)` is also unsuitable: microblock packet factories read a material ID from it.

See [microblock creation](MICROBLOCK_CREATION.md) for side selection, material traits and caller-owned shape/NBT state.

The [compiling Java example](../../src/functionalTest/java/codechicken/multipart/examples/PartFactoryLookupExample.java)
checks for `MicroblockClass` and creates a fresh unbound microblock. The caller must resolve a valid material ID,
load the part's NBT, prepare the correct composite tile and complete its existing lifecycle. This example only covers
factory selection and construction, not a complete schematic loader. Its null result for an unsupported type must
cause the whole preview to be rejected when matching Schematica's all-parts-required policy.

## Migrating Schematica's optional reflection

Prefer direct `MultiPartRegistry.getPartFactory(partID)` calls, as in the compiling example above. Isolate typed
optional integration code behind mod-presence/version checks; see the [direct-call policy](../API.md#direct-calls-and-optional-integration).
The following is only a legacy interoperability option for a consumer retaining reflection temporarily. It replaces
the companion singleton/private-field lookup and Scala `Map.get` / `Option` handling:

```java
Class<?> registry = Class.forName("codechicken.multipart.MultiPartRegistry");
Method lookup = registry.getMethod("getPartFactory", String.class);
Object factory = lookup.invoke(null, partID);
if (factory == null) {
    return null; // Keep the caller's missing-part rejection.
}
```

Cache the `Method` during integration initialization as Schematica already does. Continue resolving
`MicroblockClass.create(boolean.class, int.class)` with its exact parameter types and invoking it on this factory.
Not every registered factory is a microblock class: keep the existing rejection of an unsupported factory, or check
the reflected microblock class's `isInstance(factory)` explicitly before invoking. Do not silently produce a partial preview.

This removes the registry-map Scala dependency. It does not finish Schematica's migration: use the
[Java loading API](PART_LOADING.md#overrides-and-reflection) for its part collection and the
[static Java composite generator](COMPOSITE_GENERATION.md). Material lookup, saved part order,
shape/material loading, tile NBT and subsequent notifications remain unchanged.

The private `MultiPartRegistry$.codechicken$multipart$MultiPartRegistry$$typeMap` field keeps its exact name, modifiers,
Scala mutable-map type and live backing. Existing binary/reflection callers remain supported until a migrated consumer
release is adopted by the target pack. The reference Schematica checkout was not edited or counted as migrated.

## Validation

JVM characterization compares legacy reflection and Java lookup for exact factory identity, equal strings, case-sensitive
misses, replacement/removal visibility and absence of construction callbacks. Test-only registry mutations restore
their state; they are not a supported consumer API. Forge covers all five built-in microblock factories, exact public
reflection, server construction, NBT loading, unbound ownership and the Java example. The example also compiles with
Scala excluded from its classpath. Archived callers and the private field's binary shape are checked separately.

Physical-client microblock construction and Schematica preview rendering remain
[manual release checks](../migration/MANUAL_CHECKS.md). A dedicated Forge server strips `clientTrait()`, so
calling client microblock creation there fails before it can exercise a real client preview; the server fixture does
not claim that coverage.
