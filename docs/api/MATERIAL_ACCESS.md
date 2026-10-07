# Read a microblock's block and metadata

Use the existing public `BlockMicroMaterial.block()` and `meta()` methods. Neither reflection nor an accessor mixin
is needed. The [compiling Java example](../../src/functionalTest/java/codechicken/multipart/examples/MaterialAccessExample.java)
implements the primary-material query using GuideNH's filtering and metadata convention, with the failure and
accessor-order differences documented below:

1. Traverse `TileMultipart.jPartList()` in its stored order.
2. For each `Microblock`, resolve `material()` through `MicroMaterialRegistry.getMaterial(int)`.
3. If the result is a `BlockMicroMaterial`, read `block()` then `meta()`.
4. Skip null/air blocks and absent/empty block registry names; return the first usable block ID.

For example, wool with metadata 14 exports as `minecraft:wool:14`. GuideNH appends a metadata suffix only when the
value is positive; zero and negative values export the unsuffixed block ID. This is GuideNH's export convention,
not a general material serialization format. The example preserves positive values above 15 without masking.

## Identity, lifecycle and overrides

- `block()` returns the constructor's block by identity in the base implementation, including null. `meta()` returns
  the original integer unchanged. Both are public virtual methods; consumer calls must respect subclass overrides.
- A microblock's numeric `material()` value is an ID in the active material map, not block metadata. Query after
  registry initialization and, on a client, material-ID synchronization. Do not persist these numeric IDs across
  sessions; see [material enumeration](MATERIAL_ENUMERATION.md).
- `IMicroMaterial` does not promise a block/meta representation. Skip non-block materials for this block-only query;
  do not cast every registered material to `BlockMicroMaterial` or infer its block from an item drop.
- `blockKey()` caches the constructor block's registry name. It can differ from an overridden `block()` and from
  the material's registered name. Export the registry name of the returned block, as the example does.
- This is a read operation with no binding, world access, material registration or tile mutation. Observe the
  [part-list ownership contract](PART_TRAVERSAL.md#collection-ownership-and-ordering); the traversal is not a snapshot.

## GuideNH migration

GuideNH at `16f142417df1` already uses direct part traversal, material lookup and public getters in
`resolvePrimaryMicroblockId`. Commit `4a0cd02e` (2026-09-16, included in tags `1.3.33` onward) removed
`AccessorBlockMicroMaterial` and its configuration entry. Keep FMP's private fields unchanged for older supported
jars until migrated pack adoption is verified. Source tags are not evidence of installed-artifact adoption.

The old mixin reads constructor fields directly, bypassing overrides. The old reflection fallback already calls
the public virtual getters. The supported typed path follows those getters too, so custom materials that override
them may produce a different export from the mixin. Validate those materials in GuideNH; do not introduce raw-field
accessors to preserve an implementation detail that disagrees with the material's public behavior.

The example returns null for a null/non-multipart tile or no usable block. Invalid material IDs and exceptions from
custom accessors propagate. Current GuideNH instead catches runtime failures during material lookup, logs and
continues to the next part; getter failures propagate. It reads metadata only after accepting the block and registry
name, and calls `meta()` twice for positive values. The example reads metadata once, before the null/air check.
These differences matter for failing or stateful custom getters; the example is not a drop-in copy of current
GuideNH's failure/accessor-order policy. Validate any intentional change during migration.

For optional FMP support, isolate typed code in a compatibility class and load it only after checking mod presence
and the supported dependency version. Keep FMP types out of classes loaded unconditionally when FMP is absent.
An optional dependency does not itself require reflection. The [API index](../API.md) describes the current
unreleased branch/version status; pin an artifact containing every method used by the integration.

## Validation

JVM characterization pins constructor identity, every metadata bit, virtual dispatch and the retained private
fields. Dedicated-server Forge tests cover initialized material lookup, ordered filtering, registry aliases,
null/air/unregistered blocks, non-block materials, metadata formatting, overridden accessors and failure propagation.
The example compiles with Scala excluded from its classpath and contains no reflection, companion or Scala calls.
Actual GuideNH exports and optional-mod absent/present loading remain consumer adoption checks.
