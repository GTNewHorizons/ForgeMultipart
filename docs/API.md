# ForgeMultipart Java API

Start here when integrating with ForgeMultipart for **Minecraft 1.7.10 / Forge**. This describes the `algent/java`
migration branch. Its new APIs are implemented and tested but do not yet have a minimum published dependency version.
Use a dev artifact containing the methods you need; source Javadocs are packaged with the sources artifact.

Consumers can use the Java surface without importing Scala types. FMP still retains Scala storage, runtime and
compatibility bridges, and some extension contracts still need migration. A Scala-authored mod can adopt the Java API
without converting the rest of its code to Java.

Run `./gradlew compileJavaApiExamples` (or `gradlew.bat compileJavaApiExamples` on Windows) to compile the existing
Java examples with a Java 8 compiler against the packaged dev jar and dependencies excluding `scala-*` libraries.
This also runs through `check`. It covers every Java file in the test and functional-test `examples` packages,
including their supporting types; add new documented examples there to extend coverage. Main/test class outputs,
annotation processors and source lookup are not used to supply FMP classes. This is a compile-time API check only:
FMP still needs Scala at runtime, and generated-trait behavior remains covered separately by Forge functional tests.

**Rebuilding an existing mod against this branch? Read the [release notes](RELEASE_NOTES.md) first.** Shipped binaries
keep working, but recompiling has three classes of break, and the trait-composition one compiles cleanly and changes
behavior at runtime.

## Direct calls and optional integration

The intended consumer API uses direct typed public calls, without reflection, private-field mixins or Scala
companion lookup. Reuse existing entry points where they cover the task. For optional support, isolate FMP-typed
code in a compatibility class loaded only after mod-presence and supported-version checks; test absent and present
mod loading. Reflection snippets in older guides are legacy interoperability options, not the migration target.
Existing reflective binaries remain supported until consumer release and pack adoption.

## Choose a guide

| Task | API and guide |
| --- | --- |
| Define, place, persist, synchronize and remove a custom part | `TMultiPart`, `canPlacePart`, `addPart`, `remPart` — [end-to-end custom-part lifecycle](api/CUSTOM_PARTS.md) |
| Register block converters | `registerConverter(IPartConverter)` — [registration, state ownership and committed lifecycle](api/BLOCK_CONVERTERS.md) |
| Register part factories during mod initialization | `registerPartFactory(IPartFactory2, String...)` — [factory timing, payload ownership and migration](api/PART_REGISTRATION.md) |
| Find the factory registered for a part type | `getPartFactory(String)` — [lookup ownership and Schematica reflection migration](api/FACTORY_LOOKUP.md) |
| Enumerate microblock materials by numeric ID | `materialCount()`, `materialName(int)`, `getMaterial(int)` — [material enumeration](api/MATERIAL_ENUMERATION.md) |
| Read a microblock material's block and metadata | `BlockMicroMaterial.block()`, `meta()` — [typed GuideNH query, identity and overrides](api/MATERIAL_ACCESS.md) |
| Create a microblock with the requested material and side | `MicroblockGenerator.create(MicroblockClass, int, boolean)` — [construction, material traits and GuideNH migration](api/MICROBLOCK_CREATION.md) |
| Call generated tile capabilities from Java | `TileMultipart`, `IRedstoneTile` and ordinary interfaces — [safe compilation and raw-trait pitfalls](api/TILE_TRAIT_ACCESS.md) |
| Add aggregate or lifecycle behavior to generated multipart tiles | `registerTrait(marker, trait)` — [custom Java tile traits, stable capabilities and transformer constraints](api/CUSTOM_TILE_TRAITS.md) |
| Refresh slots after a stored part changes shape | `refreshPartSlots(TMultiPart)` — [ownership, equality and notification responsibilities](api/TILE_TRAIT_ACCESS.md#refreshing-a-changed-slot-mask) |
| Add material-specific behavior to generated microblocks | `registerTrait(String)` and `IGeneratedMaterial` — [illuminated Java extension, compilation and side contracts](api/MICROBLOCK_EXTENSIONS.md) |
| Change an existing FMP saw's cutting strength | `ItemSaw.setHarvestLevel(int)` — [state, lifecycle and Iguana migration](api/SAW_STRENGTH.md) |
| Read/index/search a tile's parts | `jPartList()` — [part collection ownership and order](api/PART_TRAVERSAL.md#collection-ownership-and-ordering) |
| Run callbacks only on parts still owned by the tile | `forEachPart(Consumer)` — [callback and override behavior](api/PART_TRAVERSAL.md#callback-behavior) |
| Rebuild parts on an already prepared composite tile | `loadPartList(Collection)` — [part loading](api/PART_LOADING.md) |
| Select client/server tile capabilities before preparing state and loading | `MultipartGenerator.generateCompositeTile(TileEntity, Iterable, boolean)` — [staged generation and reflection migration](api/COMPOSITE_GENERATION.md) |
| Assign stored parts during reconstruction, without binding or notifications | `setPartList(List)` — [storage assignment](api/PART_LOADING.md#storage-assignment) |
| Test a candidate against a selected collection of parts | `testOcclusion(Collection, candidate)` — [occlusion queries and generated hooks](api/OCCLUSION.md) |
| Test two groups of bounding boxes directly | `NormalOcclusionTest.testBoxes(Iterable, Iterable)` — [box-versus-box queries](api/OCCLUSION.md#box-versus-box-queries) |
| Read FMP's global render registration ID | `TileMultipart.getRenderID()` — [render-ID meaning, lifecycle and setter](api/RENDER_ID.md) |
| Inspect an existing tile or converted placeholder with a named result | `getOrConvertTileResult(World, BlockCoord)` — [conversion outcomes and placement lifecycle](api/TILE_CONVERSION.md) |
| Add or remap multipart button attachment orientations | `ButtonPart.setOrientation(int, ForgeDirection)` — [metadata, face mapping and Et Futurum migration](api/BUTTON_ORIENTATIONS.md) |

Each guide explains ownership, lifecycle, legacy replacements and limitations, and links to a compiling Java example
exercised by the test suite. The loading/setter APIs are advanced reconstruction operations; ordinary placement and
removal should use the existing world APIs below.

## Existing Java entry points

These entry points already exist. The links lead to their source/Javadocs; detailed migration guides and checks for
compilation without Scala are still pending in these areas. A Java-typed overload can still require Scala on the
compile classpath for overload resolution, as the [loading guide](api/PART_LOADING.md#overrides-and-reflection) explains.

| Area | Starting points |
| --- | --- |
| Construct a server composite tile from parts, or restore saved multipart NBT | [MultipartHelper](../src/main/scala/codechicken/multipart/MultipartHelper.java): `createTileFromParts(Iterable)`, `createTileFromNBT(World, NBTTagCompound)` |
| Register microblock materials | [MicroMaterialRegistry](../src/main/scala/codechicken/microblock/MicroMaterialRegistry.java) and [BlockMicroMaterial](../src/main/scala/codechicken/microblock/BlockMicroMaterial.java) |
| Register generated tile traits or pass-through interfaces | [MultipartGenerator](../src/main/scala/codechicken/multipart/MultipartGenerator.java); see [custom tile-trait authoring](api/CUSTOM_TILE_TRAITS.md) and [safe capability access](api/TILE_TRAIT_ACCESS.md) |

New API examples use Java 8 language/library features. The source path `src/main/scala` also contains Java classes
because of the current joint-compilation layout; it does not imply that those APIs require Scala source in a consumer.

## Supported API and internal hooks

Public visibility alone does not make a method a consumer entry point. Scala's former package-private methods are
public in bytecode. Source Javadocs now mark the following audited implementation hooks **Internal FMP**; keep
consumer code on the public operations in the right column. These markers change no visibility, descriptor or
behavior and do not deprecate or authorize removing a method. Trait callbacks and generated overrides still run.

| Internal hooks | Consumer operation or responsibility |
| --- | --- |
| `TileMultipart.addPart_impl`, `addPart_do`, `writeAddPart` | Place through `TileMultipart.addPart(world, pos, part)`; use [part loading](api/PART_LOADING.md) for prepared reconstruction |
| `TileMultipart.remPart_impl` | Remove through `tile.remPart(part)` on the server and retain the returned tile |
| `TileMultipart.partAdded`, `partRemoved` | Lifecycle callbacks for trait overrides; callers use placement/removal APIs |
| `TileMultipart.from`, `copyFrom`, `loadFrom`, `setValid` | FMP composite transitions; use [staged generation](api/COMPOSITE_GENERATION.md) and [loading](api/PART_LOADING.md) for consumer reconstruction |
| `MicroMaterialRegistry.setupIDMap`, `calcMaxCuttingStrength` | FMP initializes IDs and cutting strength; register materials at initialization and read the resulting values |
| `MicroMaterialRegistry.loadIcons` | FMP dispatches client texture callbacks; override the supported `IMicroMaterial.loadIcons()` material callback |
| `MicroMaterialRegistry.writeIDMap`, `readIDMap` | FMP's whole-map handshake; part packets use `writeMaterialID` / `readMaterialID` for individual IDs |

The registry's matching companion methods carry the same internal marker. This is the bounded Phase 9.2 audit,
not a claim that every other public member is a supported API. See [TileMultipart's Javadocs](../src/main/scala/codechicken/multipart/TileMultipart.java)
and [registry Javadocs](../src/main/scala/codechicken/microblock/MicroMaterialRegistry.java) for method-specific details.

Three advanced methods remain supported because consumers use them directly:

- **`bindPart(part)`** updates capability caches through generated overrides. It does not insert the part, change
  its tile binding or perform placement/notifications. The legacy OpenComputers integration clears a print part's old
  slot entries before calling it to populate a changed slot mask. A repeated call does not clear obsolete slots and
  may append entries in other trait caches. Use `refreshPartSlots` for that migration; full reconstruction uses
  `loadPartList`.
- **`refreshPartSlots(part)`** replaces OpenComputers' direct generated-array mutation. Generated slotted tiles clear
  equal cached entries and invoke the virtual bind chain once; base tiles do nothing. It leaves ownership, storage,
  validation and notifications to the caller. See the [slot-refresh contract](api/TILE_TRAIT_ACCESS.md#refreshing-a-changed-slot-mask).
- **`internalPartChange(part)`** sends local `onPartChanged` callbacks through the retained `operate` hook. The base
  traversal captures list order, visits only parts still bound to the receiving tile, and excludes parts equal to the
  changed part using `part.equals(p)`. Null broadcasts to all eligible parts. Detached or transferred parts are skipped.
  Callback failures stop traversal and propagate. ProjectRed deliberately handles dirty state,
  packets and external neighbors separately; this method does none of those. `notifyPartChange` also performs world
  update/neighbor/lighting notifications when needed.

No reflection is needed to call any of these methods. `operate` and `getOrConvertTile2` retain their documented legacy
contracts and Java replacements; neither receives an internal-only marker. Removing those bridges still requires
consumer and internal migration gates. The [consumer audit](../JAVA_MIGRATION_COMPATIBILITY.md#api-boundary-audit)
records the checked source calls and validation.

## Compatibility and remaining work

Deprecated Scala-facing entry points remain callable, with their descriptors and supported override dispatch retained.
Follow the method-specific guide: a Java sibling is not automatically a replacement override hook, and reflection
must select the intended parameter types when a method is overloaded.

An [illuminated Java microblock extension](api/MICROBLOCK_EXTENSIONS.md) now covers registration, material traits,
light aggregation and halo geometry, with compiling examples and Forge coverage. Its physical-client rendering and
ProjectRed adoption remain open. [Converter registration and lifecycle](api/BLOCK_CONVERTERS.md) are documented and
tested. [Stable tile capability access](api/TILE_TRAIT_ACCESS.md) covers safe Java calls, ProjectRed redstone queries
and OpenComputers slot refresh. [Custom Java tile-trait authoring](api/CUSTOM_TILE_TRAITS.md) covers registration,
stable capabilities, lifecycle/state rules and transformer constraints with generated Forge coverage. Supported
[button orientation mapping](api/BUTTON_ORIENTATIONS.md) replaces Et Futurum's reflective array mutation. Supported
[saw-strength mutation](api/SAW_STRENGTH.md) replaces Iguana's private-field access. ProjectRed's existing Scala
traits remain supported until adoption.

All ten entries in the plan's Phase 9.1 API table have Java replacements. That table is a bounded list of signatures;
the broader API, extension and consumer adoption work above remains open.

FMP-side implementation is separate from consumer releases and target-pack adoption. Retiring legacy bridges or the
Scala dependency requires those gates and removal of FMP's remaining internal Scala users.

| Need | Document |
| --- | --- |
| What is complete and what comes next? | [Working handoff](../JAVA_MIGRATION_HANDOFF.md), [migration plan](../JAVA_MIGRATION.md) |
| Which consumers need changes, releases and pack adoption? | [Consumer adoption ledger](../JAVA_MIGRATION_COMPATIBILITY.md#java-api-adoption-ledger) |
| Which binary names, reflective lookups and runtime contracts must survive? | [ABI inventory and consumer audit](../JAVA_MIGRATION_COMPATIBILITY.md) |
| What has been tested and what still needs a client/pack run? | [Migration history](migration/HISTORY.md), [manual release checks](../JAVA_MIGRATION_MANUAL_CHECKS.md) |
| Why is some Scala or older Java syntax retained? | [Modern Java policy](../JAVA_MIGRATION.md#modern-java-readability-policy), [compiler/toolchain handoff](../JAVA_MIGRATION.md#modern-java-readability-policy) |
