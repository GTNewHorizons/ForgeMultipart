# Creating and recreating microblocks

[Java API index](../API.md)

Use the existing `MicroblockGenerator.create(MicroblockClass, int material, boolean client)` static Java entry.
It delegates to the retained companion implementation. This guide describes `algent/java`; it does not establish a
minimum published dependency version. No additional creation API or production behavior change is introduced here.

```java
Microblock part = MicroblockGenerator.create(microClass, materialId, client);
```

`microClass.create(client, materialId)` uses the same generator; note its different parameter order. To find a factory
by part type, use [registered factory lookup](FACTORY_LOOKUP.md) and verify that it is a `MicroblockClass`.

## Construction and ownership

Creation chooses the factory's base trait and, for the client path, its client trait. It then resolves the material
from the active numeric ID map and invokes `IGeneratedMaterial.addTraits` when implemented. Finally it constructs a
fresh instance of the resulting generated class with the material ID as its constructor argument. Generated classes
are cached; part instances are not reused. Trait initializers execute during construction.

The generator does not copy an existing part, assign a requested shape, load NBT, bind the part or install a tile.
Stock face microblocks begin with shape zero, which is not a completed placement shape. Restore shape/NBT before
using geometry or rendering and before [preparing/loading a composite tile](COMPOSITE_GENERATION.md).

Resolve the intended material before construction. A later `part.load(tag)` or `material_$eq` updates stored material
but does not regenerate the class's material traits. For NBT reconstruction, resolve `tag.getString("material")`,
create the correctly sided part with that ID, then call `load(tag)`. Ordinary `MultiPartRegistry.loadPart` selects the
server/NBT path; it does not provide client preview construction.

Call after factory/trait registration and material-map initialization, on the initialization/game thread. Numeric IDs
belong to the active map and may change across sessions or a multiplayer handshake; persist material names, not IDs.
See [material enumeration and ownership](MATERIAL_ENUMERATION.md). Invalid/unresolved IDs, null factories, callback
exceptions and constructor failures propagate; there is no new fallback or rollback.

## Material-provided traits

`IGeneratedMaterial.addTraits(BitSet, MicroblockClass, boolean)` receives the exact factory and side supplied by the
caller. Its borrowed mutable bit set already contains the selected base/client traits. Add registered trait IDs;
do not remove required traits. The generator consumes the modified set immediately after the callback.

Do not retain the set or recursively call this generator from the callback: the next creation on that thread clears
and reuses it. On failure, already-performed callback side effects and scratch mutations remain; a later creation
clears scratch again. Thread-local scratch does not make registry changes or the complete generator thread-safe.

ProjectRed's existing external Scala trait path remains supported. The Forge fixture confirms material-driven trait
injection and generated behavior; this does not replace the planned representative external Java extension example
or authorize removing Scala-signature support before consumer release/adoption.

## GuideNH migration

GuideNH `7d8fb44e77b9` explicitly resolves the companion's `create(MicroblockClass, int, boolean)` and invokes it on
`MODULE$`. It does not automatically discover the existing static method. Prefer the direct typed creation example
below, with [gated optional integration](../API.md#direct-calls-and-optional-integration). The following cache is only
a temporary interoperability option for consumers retaining reflection:

```java
Class<?> generator = Class.forName("codechicken.microblock.MicroblockGenerator");
Class<?> factoryType = Class.forName("codechicken.microblock.MicroblockClass");
Method create = generator.getMethod("create", factoryType, int.class, boolean.class);
Object promoted = create.invoke(null, microClass, materialId, true);
```

Keep the exact parameter types and order. The old companion class, singleton, descriptor and behavior remain
unchanged for existing releases.

GuideNH's promotion recreates the source family and material on the client, then copies the encoded shape byte.
The [compiling Java example](../../src/functionalTest/java/codechicken/multipart/examples/MicroblockCreationExample.java)
implements that core-data policy through `setShape(size, slot)`, preserving all eight shape bits. `getShape()` alone
is only the slot nibble, while `shape()` returns the complete byte. The example leaves source bindings untouched and
returns an unbound replacement. It is not a clone of arbitrary custom-part state or a shape-validity check.

Factory, material and shape are captured before construction, as in GuideNH. Material callbacks can have side effects;
capturing shape after creation could copy a callback-modified value instead of the original one.

Public `setShape` uses the virtual shape setter; GuideNH's old private-field write bypassed that dispatch. Validate
custom traits overriding these hooks before adopting the change. Preserve its existing missing/failed-promotion
handling, part order, Java collection migration, tile state setup and subsequent loading/notifications. Do not copy
the source tile binding to the new part. Private material-field access has a separate [typed query guide](MATERIAL_ACCESS.md).

## Side and validation limits

Use `client=true` on the physical client for client variants. Unlike generating a client tile container, normal client
microblock construction cannot run in the dedicated-server harness: Forge strips `MicroblockClass.clientTrait()`.
The side parameter selects traits; it does not infer a world side or bypass Forge's side-only filtering.

Server Forge tests cover both exact reflective entries, factory/material identity, fresh instances, shape/NBT
ownership, material trait injection, callback failures and scratch reuse. The Java example tests all 256 shape-byte
values without copying bindings or altering the source and compiles without Scala. Archived callers, existing APIs,
method bodies and generated dumps are checked separately. Physical-client creation and GuideNH preview/GPU output
remain [manual release checks](../migration/MANUAL_CHECKS.md); reference checkouts and adoption status are unchanged.
