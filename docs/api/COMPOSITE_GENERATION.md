# Staged composite-tile generation

[Java API index](../API.md)

`MultipartGenerator.generateCompositeTile(TileEntity, java.lang.Iterable<TMultiPart>, boolean)` selects a tile's
generated capabilities without loading its parts. It is available on `algent/java`, with no minimum published
dependency version yet. The [compiling example](../../src/functionalTest/java/codechicken/multipart/examples/CompositeGenerationExample.java)
then assigns world/position and loads the same parts, leaving installation to its caller. It compiles without Scala.

```java
TileMultipart prepared = MultipartGenerator.generateCompositeTile(candidate, parts, client);
// Prepare world, coordinates and any required tile NBT before loading.
prepared.loadPartList(parts);
```

Use a `List<TMultiPart>` or another repeatable collection when the same parts will subsequently be loaded. Generation
accepts `Iterable<TMultiPart>`; loading accepts `Collection<TMultiPart>`. A one-shot iterable can select traits, but
cannot then supply the same parts again. The generator reads the input once on every call, including candidate reuse,
without copying it. Do not mutate it during generation or change the required capabilities between generation and loading.

## Selection and ownership

| Candidate and required traits | Result |
| --- | --- |
| Null or a non-multipart tile | A fresh empty generated tile |
| Registered multipart class with exactly the required trait set | The same tile, with all existing state untouched |
| Registered multipart class with a different trait set or side | A fresh empty replacement; the candidate remains untouched |
| Multipart class unknown to the generator | `NoSuchElementException`, after reading the parts |

An empty iterable is valid and chooses the side's base tile. Order and duplicate parts do not add extra copies of a
trait, but each input entry is visited. The generator caches constructed classes; fresh tiles are still distinct objects.

Generation does not load or bind parts, assign world/position, copy source state, invalidate an old tile, install a
replacement or send notifications. New tiles have empty part storage and caches. Existing part-to-tile bindings remain
until the caller loads/rebinds them; loading a replacement does not clear the old tile's stored list.

Call after trait registration on the initialization/game thread. Trait selection uses existing caches and scratch
state. Iterator exceptions, null inputs/elements and unregistered-class failures propagate without rollback of that
internal state. Generation is neither a placement check nor a transaction for replacing a live tile.

## Side and loading lifecycle

`client=true` selects client tile capabilities. It does not turn server parts into client part variants; supply the
correctly constructed parts yourself. The world assigned for loading must agree with the intended side. The example
also allows null for explicitly worldless staging, which omits world-dependent loading callbacks.

Prepare position, world and any required tile NBT before [loading](PART_LOADING.md). With a world, the default loader
notifies parts and neighbors and updates lighting; a client world also triggers `onWorldJoin`. Preserve the integration's
subsequent installation, tile/render notifications and synchronization. Do not add them blindly to a worldless preview.

For ordinary world placement use `TileMultipart.canPlacePart` / `addPart`. For immediate server construction and
loading, the existing `MultipartHelper.createTileFromParts` remains supported. It is not a substitute for consumers
that must set state between generation and loading.

## Schematica and GuideNH migration

Prefer the direct call `MultipartGenerator.generateCompositeTile(candidate, parts, client)`, as in the compiling
example. Gate optional typed integration code as described in the [API index](../API.md#direct-calls-and-optional-integration).
For temporary interoperability with an existing reflective integration, the method can also be resolved with:

```java
Method generate = MultipartGenerator.class.getMethod(
    "generateCompositeTile", TileEntity.class, java.lang.Iterable.class, boolean.class);
TileMultipart tile = (TileMultipart) generate.invoke(null, candidate, parts, client);
```

An optional integration can obtain the owner through `Class.forName("codechicken.multipart.MultipartGenerator")`.
Pass a Java list directly and cache the method during integration initialization.

- **Schematica:** remove `JavaConversions.collectionAsScalaIterable` from generation; keep material resolution,
  [factory lookup](FACTORY_LOOKUP.md), side-specific microblock construction, per-part NBT loading and rejection when
  any part is missing. Preserve its following tile `readFromNBT`, `loadPartList(parts)` and per-part notification order.
- **GuideNH:** keep promotion of parts to their client variants, then use a Java list for generation. Preserve its
  same-instance branch, copying of coordinates/world/block state, storage assignment where required, loading,
  `notifyTileChange` and `markRender`. Generation does not perform any of those steps automatically.

The old companion's `generateCompositeTile(TileEntity, scala.collection.Iterable, boolean)` keeps its descriptor and
body, deprecated for callers in favor of the static Java entry. Existing internal calls remain unchanged. GuideNH's
argument-assignability matcher still rejects the Java parameter for a Scala sequence and falls back to the companion;
Schematica's exact old reflective lookup remains valid. Consumer source patches/releases and pack adoption remain pending.
GuideNH's separate microblock-generator call has an [existing Java replacement](MICROBLOCK_CREATION.md).
Private-material access has a separate [typed material query](MATERIAL_ACCESS.md); neither consumer adoption contract
is closed by this tile API alone.

## Validation

Forge characterization covers client/server tile selection, exact reuse, empty input, duplicates, unbound generation,
source-state retention, subsequent loading, one input traversal, failure propagation, and old/new reflection.
The example covers server-world setup and worldless client-tile loading. An exact JVM API inventory includes the new
method; the archived run excludes only its obsolete pre-addition inventory assertion. The remaining archived JVM tests
and all archived Forge cases run against the addition, with an independent class/member/body comparison.

Generating a client tile in the Forge server harness does not prove physical-client microblock creation or GPU output.
Schematica and GuideNH preview rendering remain [manual release checks](../migration/MANUAL_CHECKS.md).
