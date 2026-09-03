# Java migration divergences

Current intentional differences from the `1.7.12` reference. This is a compatibility ledger, not a port diary.
An unchanged implementation needs no entry. Results and port history belong in `JAVA_MIGRATION.md` and
`JAVA_MIGRATION_HANDOFF.md`; client gaps in `JAVA_MIGRATION_MANUAL_CHECKS.md`; measurements in
`JAVA_MIGRATION_PROFILE.md`. Superseded decisions and the original per-port narrative remain in git history.

“No audited consumer” means the frozen GTNH binary inventory and consumer-source audit, not every possible
third-party mod. Removed members can still break unaudited consumers. Check both `JAVA_MIGRATION_ABI_INVENTORY.md`
and `JAVA_MIGRATION_CONSUMER_AUDIT.md` before extending this list.

## Runtime and error behavior

| Area | Difference and consequence |
| --- | --- |
| `IDWriter` before `setMax` | `read`/`write` throw `IllegalStateException` instead of `NullPointerException`. Uninitialized use is unsupported. |
| `PacketScheduler` invalid `maskWidth` | Throws `IllegalArgumentException` instead of `scala.MatchError`. Valid widths remain 1, 2, 4 and 8. |
| `NormalOcclusionTest.apply(Traversable, Traversable)` | Materializes both inputs into Java lists before testing instead of nested Scala `forall`. Finite, side-effect-free collections give the same result; eager traversal can change side effects, failure timing and termination for unusual inputs. |
| `MultipartRenderer` dynamic-part guard | Uses short-circuit `\|\|` instead of `\|`; the second getter is no longer evaluated when the first operand succeeds. Ordinary getters give the same render decision. |
| `ControlKeyModifer.map()` | The exposed Java map has no Scala default-value wrapper: an absent key returns null from `get`, not false from Scala `apply`. Use `isControlDown` for the unchanged absent-means-false behavior. |
| Public companion constructors, including `ASMImplicits.ExtBitSet$` / `ExtClass$` | Explicit construction creates an independent instance instead of replacing `MODULE$`. Java initializes the retained final module field in its static initializer; use `MODULE$` for singleton identity. |
| `registerJavaTrait` abstract inputs | Abstract Java classes are accepted instead of throwing `IllegalArgumentException`. Declared abstract methods become generated-interface contracts without helper bodies; a later mixin must implement them before those members can be invoked. A no-argument abstract-mixin constructor may call an argument-taking superclass constructor; registration discards that direct call and its argument evaluation because the composite invokes the real base constructor first. This enables Java microblock mixins without changing existing concrete registrations. |

Concrete collection implementations also change without changing the declared `java.lang.Iterable`/`List` contracts:

| Return path | Reference → Java |
| --- | --- |
| `JCuboidPart.getSubParts` / `getCollisionBoxes` | Scala `Seq` adapter → immutable `Collections.singletonList` |
| `MultipartHelper.IPartTileConverter.convertMulti` default | Scala `Seq` adapter → `Collections.emptyList` / `singletonList` |
| `Microblock.getDrops` | Java view of Scala `ListBuffer` → mutable `ArrayList` |

Code depending on concrete collection classes or their exact mutation failure behavior must adapt.

## Removed or changed binary surface

These removals were accepted because neither audit identified a consumer of the removed surface. This does not
authorize removing other members of the same type.

| Area | Removed or changed surface |
| --- | --- |
| `IDWriter` | Removed `write()`, `write_$eq(Function2)`, `read()` and `read_$eq(Function1)`. Use the Java methods listed below. |
| Both registries | `MicroMaterialRegistry.readIDMap` and `MultiPartRegistry$.readIDMap` return `java.util.List<String>` instead of `scala.collection.Seq<String>`; the return descriptor changes. |
| Registry compiler accessors | Removed `MicroMaterialRegistry`'s public mangled `$$idMap()`, `$$typeMap()` and `$$nameMap()` accessors, and the five `codechicken$multipart$MultiPartRegistry$$...()` accessors. Schematica's separate private `MultiPartRegistry$.$$typeMap` **field is retained** as a live Scala-map view; it is not one of the removed methods. |
| `NormallyOccludedPart` | Removed the inferred `getType(): scala.runtime.Null$` overload; the `String` method remains. |
| `MissingMicroMaterial` | Removed static `canRenderInPass(int)` and `isSolid()` forwarders. The companion no longer declares these methods but inherits the interface defaults. |
| `TileMultipart` | Removed compiler accessor `protected$worldObj(TileMultipart)`. |
| `BlockMultipart` | Removed `hasTileEntity$default$1()`; source callers must supply metadata explicitly. |
| `TileCache` | `map()` now returns Java `Map`; `add`/`remove` return `void` instead of Scala `Option`; `apply(BlockCoord)` returns a nullable `FlaggedTile` instead of `Option`. |
| `TileCache.FlaggedTile` | Dropped Scala case-class `copy`, copy-default getters, `canEqual`, value `equals`/`hashCode`/`toString`, product methods, and `scala.Product`/`scala.Serializable`. It now has ordinary object identity semantics; `t()` and `removed()` remain. |
| `ControlKeyModifer` | Replaced `isClientPressing_$eq(boolean)` with `setClientPressing(boolean)`; `map()` returns Java `Map<EntityPlayer, Boolean>` instead of Scala mutable `Map`; removed `playerControlValue(EntityPlayer)` and its implicit wrapper type. |
| `ControlKeyHandler` | Former static facade is now a `KeyBinding` subclass with `INSTANCE`. `wasPressed`/`tick` are instance methods, and the twelve inherited-KeyBinding static forwarders disappear. Initializing this client-only class now initializes its binding. |
| `MultipartRenderer` | Removed inherited-TESR static forwarders `func_147498_b`, `func_147496_a` and `func_147497_a`. |

Removed named helpers and companions (besides private compiler artifacts covered below):

- `JPartialOcclusion$class`, including `allowCompleteOcclusion` and `$init$`.
- `MicroMaterialRegistry$IMicroMaterial$class` and its four statics.
- `TRandomUpdateTick$class`, including `onWorldJoin` and `$init$`.
- `TScheduledPacketPart$class` and `PacketScheduler$`.
- `IconHitEffects$`.
- `TileCache$` and `TileCache$FlaggedTile$`.
- `ControlKeyModifer$`, `ControlKeyModifer$ControlKeyValue` and `ControlKeyHandler$`.
- `codechicken.multipart.package` / `package$` and `codechicken.microblock.package` / `package$`.
  Their logger aliases are replaced by the corresponding proxy's `logger()`.

Generated-tile helpers removed only from the **raw artifact** are listed separately below; Forge recreates them.
Other retained `$class` bridges and `MODULE$` companions are not covered by this removal decision.

## Additions and default-method changes

| Area | Additive Java entry points |
| --- | --- |
| `IDWriter` | `write(MCDataOutput, int)` and `read(MCDataInput)` |
| `JCuboidPart` | Static `subParts`, `collisionBoxes` and `renderBreaking` implementations for explicit forwarding |
| `JItemMultiPart` | Static `hitDepth` and `onItemUse(part, ...)` implementations |
| `MicroMaterialRegistry` | Static `setupIDMap`, `calcMaxCuttingStrength` and `loadIcons` forwarders |
| `PacketScheduler` / `TScheduledPacketPart` | Static `sendScheduled()` on the facade and `readMask(part, packet)` on the interface |
| `TileMultipart` | `hasDynamicParts` (false by default), `renderStatic`, `renderDynamic` and `randomDisplayTick` hooks for dispatching generated overrides through a stable superclass |
| `MultipartMixinFactory` | Four additional static forwarders: `onCompiled`, `autoCompleteJavaTrait` and the two mangled `ASMMixinFactory` parent helpers. Scala emitted them when the base became Java with its original public JVM access; the Java facade retains them. Existing facade entries are unchanged. |

Previously abstract JVM interface methods now have Java defaults, allowing inheritance without Scala forwarders:

| Interface | Default methods |
| --- | --- |
| `JPartialOcclusion` | `allowCompleteOcclusion` |
| `TFacePart` | `solid`, `redstoneConductionMap` |
| `JIconHitEffects` | `getBreakingIcon` |
| `TItemMultiPart` | `getHitDepth` |
| `TEdgePart` | `conductsRedstone` |
| `Saw` | `getMaxCuttingStrength` |
| `MicroMaterialRegistry.IMicroMaterial` | `loadIcons`, `canRenderInPass`, `isSolid` |
| `TScheduledPacketPart` | `writeScheduled`, `readScheduled` |
| `PlacementGrid` | `render`, `drawLines`, `glTransformFace` |

`ItemSaw` consequently no longer declares `getMaxCuttingStrength`; ordinary calls still resolve to the default,
but declared-member reflection differs. The same distinction applies to the two `MissingMicroMaterial$` methods above.

The `TNormalOcclusion` super accessor
`codechicken$multipart$TNormalOcclusion$$super$occlusionTest(TMultiPart)` additionally loses `ACC_SYNTHETIC` and
becomes a default returning true, so javac can see and implement it. `NormallyOccludedPart` inherits it rather than
declaring its own. Existing Scala binaries retain their explicit super-chain overrides.

## Recompiling Scala consumers

Binary descriptors alone do not preserve Scala source semantics after `ScalaSignature` metadata is removed.

### Trait composition

A Java interface cannot supply a Scala trait's class supertype or its linearization. A superclass method wins over
an interface default, so recompiling `extends TMultiPart with TCuboidPart` silently selects the empty superclass
methods. Explicit forwarding is required where the implementation remains abstract:

| Trait | Source migration |
| --- | --- |
| `TCuboidPart` | Extend `JCuboidPart`, or forward subparts, collision boxes and breaking rendering to its statics. |
| `TNormalOcclusion` | Explicitly combine `NormalOcclusionTest.apply(this, other)` with the intended super chain. Audit existing `super.occlusionTest` calls too: they no longer route through the old trait. |
| `TIconHitEffects` | Explicitly forward `addHitEffects` and `addDestroyEffects` to `IconHitEffects`. |
| `TItemMultiPart` | Explicitly forward `onItemUse` to `JItemMultiPart`; the inherited Item method otherwise wins. |
| `TRandomUpdateTick` | Explicitly implement `onWorldJoin` with `TickScheduler.loadRandomTick(this)`. |
| `TScheduledPacketPart` | Explicitly forward `read` to `TScheduledPacketPart.readMask(this, packet)`. |
| `ScratchBitSet` | Implement both methods and storage accessors, and initialize storage explicitly; the retained `$class` helper supplies the original implementations. |

Previously compiled calls to **retained** `$class` bridges still link. This does not apply to the removed helpers above.

### Source syntax and generic signatures

- An object used as a value now needs `X$.MODULE$` where that companion is retained (event handlers, proxies,
  recipes, packet handlers, materials and generators). Static calls alone do not supply a singleton value.
- Object-application sugar becomes explicit `.apply(...)`, e.g. `NormalOcclusionTest.apply(...)` and
  `MicroblockPlacement$.MODULE$.apply(...)`.
- `ASMImplicits` no longer provides Scala implicit/value-class syntax. Use `nodeName(clazz.getName)` and
  `ExtBitSet$.MODULE$.copy$extension(bits)` explicitly, or construct its retained Java wrappers. Existing boxed and
  `$extension` binary entry points remain; implicit conversion requires Scala metadata Java cannot emit.
- Scala property assignment needs the explicit Java-authored `_$eq(...)` method, e.g. `renderID`, `loadingWorld`,
  `angelicaCompat` and microblock `shape`.
- `ByteCodeReader.advance(length)(value)` becomes `advance(length, value)` when recompiling Scala. Argument
  evaluation remains eager, and previously compiled calls retain the same binary descriptor.
- `ASMMixinFactory` constructor parameters and `construct` arguments need an explicit Scala `Seq`, not Scala
  varargs syntax. Overrides of `onCompiled` and `autoCompleteJavaTrait` must be public: Java preserves their original
  public JVM access without Scala's protected-source metadata.
- `tile.partList(i)` becomes `tile.partList.apply(i)`. Static bounds getters on Face, Corner, Edge and Post factories
  need `aBounds()(index)` instead of `aBounds(index)`; Java no-argument methods may need explicit parentheses.
- `TileMultipart.getOrConvertTile2` exposes `Tuple2<TileMultipart, Object>` instead of a Scala-typed Boolean second
  element; source callers need a cast. Scala 2.11 callbacks to `operate(Function1<..., BoxedUnit>)` need an explicit
  `AbstractFunction1` instead of the previous Unit-returning lambda adaptation.
- The Face, Corner, Edge, Post and Hollow factory **companions** widen `baseTrait` to
  `Class<? extends Microblock>` from their specific trait type. Their static facades retain the specific generic
  type, and the raw JVM return descriptor remains `Class`.
- `MultiPartRegistry.registerParts(IPartFactory, String[])` and the `IPartFactory2` array overload gain Java varargs
  flags. `BlockMultipart.addCollisionBoxesToList` uses raw `List` rather than `List<?>` to override vanilla. Neither
  changes its raw JVM descriptor.

## Generated tile traits: raw artifact versus Forge runtime

These ports replace Scala interfaces/helpers with concrete Java mixin inputs. Forge rewrites the inputs back into
interfaces and generates implementation helpers. Existing binaries retain the runtime interface calls, but code
recompiled against the **untransformed dev jar** may emit `invokevirtual` or `getfield` against a type that becomes
an interface, causing verification/linkage failures. A transformed compile stub or explicit source guidance is
required before claiming source-compatible downstream rebuilds.

| Raw artifact change | Affected types |
| --- | --- |
| Interface + `$class` → concrete `TileMultipart` subclass | `TSlottedTile`, `TRedstoneTile`, `TTileChangeTile`, `TFluidHandlerTile`, `TileMultipartClient` |
| Interface + `$class` → concrete child mixin input | `TRandomDisplayTickTile` (extends the registered `TileMultipartClient` input) |
| Removed raw `TIInventoryTile$class`; moved implementation into `JInventoryTile` input | `TIInventoryTile` itself remains a Java interface. Source consumers should target it, not the transformed `JInventoryTile` class. |

Inherited/foreign-field and callback workarounds add package-private access shims for redstone, tile-change,
fluid, inventory and both client traits; they are not new consumer APIs. Transformer constraints and shim recipes
are documented once in the handoff.

The client-trait port also deliberately extends the Java-trait transformer:

- Registered Java traits may extend an already registered Java trait; the parent runtime interface participates in
  hierarchy and linearization.
- Explicit field accessors are recognized rather than duplicated.
- Transient input fields are excluded from automatic `copyFrom`. Client render caches use this opt-out; their
  generated runtime fields remain ordinary private fields.

## Shared classfile differences

These are accepted once for the migration, not repeated for every otherwise equivalent port:

- Java-authored types lose Scala signature/marker attributes and use Java source-file/debug metadata. Static facades
  and helper classes gain private constructors where Scala emitted none. Java class initializers lack Scala's
  `ACC_PUBLIC`, and singleton publication follows Java initialization rather than Scala's constructor assignment.
- Unreferenced Scala `$$anonfun$`/anonymous implementation classes and private compiler accessors disappear or are
  replaced by Java helpers/lambdas. Their names are not supported API. This does **not** include named compatibility
  bridges or reflective fields: audit those individually. Retained Scala traits still emit their required helpers.
- The `Object[]` renderer bridges on `ItemMicroPartRenderer$` and `ItemSawRenderer$` are ordinary Java methods rather
  than synthetic Scala methods; signatures and dispatch remain available.
- The private scratch `ThreadLocal` fields on `MicroblockGenerator$` and `MultipartGenerator$` lose `ACC_FINAL` so
  their public Scala-trait setters can still replace storage.
- TickScheduler's private Scala scheduler types become package-private static nested Java classes, retaining their
  binary names.

## Updating this ledger

Add only a new effective runtime, binary, source or classfile difference, with its consequence and reason for
acceptance. Amend an existing row when a later change supersedes it. Do not add passing test counts, unchanged
behavior, build observations, performance claims or a “no divergence” section for each new port.
