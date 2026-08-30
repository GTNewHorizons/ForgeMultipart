# Java migration divergence log

This log records intentional differences from the `1.7.12` reference implementation. Behavioral changes must be isolated from behavior-preserving conversions and justified here.

## 2026-08-14 — IDWriter Java pilot

### Observable behavior

No known behavior divergence. Byte, unsigned-short, and integer carrier selection and encoding remain identical, including the reference behavior that negative maximum values select the byte carrier.

### Supported JVM API

- Preserved `write()`, `write_$eq(Function2)`, `read()`, `read_$eq(Function1)`, `setMax(int)`, and the public constructor with their reference JVM descriptors.
- Marked the four Scala function property methods deprecated.
- Added `write(MCDataOutput, int)` and `read(MCDataInput)` as the Java-native API. Recompiled Scala callers also resolve to these methods.

### Compiler artifacts

Accepted divergence: the six public-but-generated `IDWriter$$anonfun$setMax$1` through `$6` classes are removed and replaced by private Java anonymous helper classes. They represented closure implementation details, were not named by source callers, and are not retained as supported API.

### Validation

- Existing `IDWriterCharacterizationTest`: 8 tests, 0 failures, 0 errors.
- Complete plain-JVM suite: 25 tests, 0 failures, 0 errors.
- Clean `checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 2 tests, 0 failures, 0 errors.
- `IDWriter` class-file version: 52 (Java 8).

## 2026-08-14 — Partial occlusion Java port

### Observable behavior

No known voxel-test divergence. Grid resolution, coordinate rounding, owner encoding, conflict marking, required visibility, complete-occlusion exemption, and same-part overlap behavior remain unchanged.

### Supported JVM API

- Preserved every public `PartialOcclusionTest` constructor, accessor, `fill` overload, and `apply()` descriptor. Its four private fields also retain their reference names and descriptors.
- Preserved the `JPartialOcclusion` name plus both method descriptors. `allowCompleteOcclusion()` changes from a Scala-trait abstract method to a Java default method returning `false`.
- Retained deprecated `JPartialOcclusion$class` with the reference `allowCompleteOcclusion(JPartialOcclusion)` and `$init$(JPartialOcclusion)` static descriptors. This keeps Scala 2.11 implementations compiled against the reference jar linkable.

### Compiler artifacts

Accepted divergence: the five generated `PartialOcclusionTest$$anonfun$*` classes are removed. The Java loops produce no replacement helper classes, and these closure implementation details are not retained as supported API.

### Validation

- `PartialOcclusionCharacterizationTest`: 10 tests, 0 failures, 0 errors.
- Reference-compiled Scala consumer: 1 test, 0 failures, 0 errors.
- Complete plain-JVM suite: 27 tests, 0 failures, 0 errors.
- Clean `checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 2 tests, 0 failures, 0 errors.
- `PartialOcclusionTest` class-file version: 52 (Java 8).

## 2026-08-14 — Removal of unused Scala bridges

The downstream ABI inventory (`JAVA_MIGRATION_ABI_INVENTORY.md`) scanned 240 mod jars and found no consumer of either
bridge below. Both were written speculatively before the inventory existed.

### Removed API

- `IDWriter.write()`, `IDWriter.write_$eq(Function2)`, `IDWriter.read()`, and `IDWriter.read_$eq(Function1)`.
  No jar in the pack references `IDWriter` at all. Both in-repo callers, `MultiPartRegistry` and
  `MicroMaterialRegistry`, already use `write(MCDataOutput, int)` and `read(MCDataInput)`.
- `JPartialOcclusion$class`, including the `allowCompleteOcclusion(JPartialOcclusion)` and `$init$(JPartialOcclusion)`
  static descriptors. No jar references the helper. The two consumers that touch `JPartialOcclusion`,
  ForgeRelocationFMP and WitchingGadgets, only call `getPartialOcclusionBoxes()Ljava/lang/Iterable;`, and the two
  jars that implement the interface, WR-CBE and extrautilities, emit no call to the helper. The interface itself and
  both of its method descriptors are unchanged.

The `ReferenceScalaPartialOcclusion` binary fixture and `JPartialOcclusionBinaryCompatibilityTest` were removed with
the helper they existed to verify.

### Observable behavior

`IDWriter` no longer stores Scala closures, so carrier selection and encoding happen without boxing an `Integer` or
allocating a `Function1`/`Function2` per call. Byte, unsigned-short, and integer selection and the encoded bytes are
unchanged, including negative maximums selecting the byte carrier.

Accepted divergence: calling `write` or `read` before `setMax` now throws `IllegalStateException` instead of
`NullPointerException`. No caller reaches this state; the change only improves the failure message.

### Validation

- `IDWriterCharacterizationTest`: 9 tests, 0 failures, 0 errors.
- Complete plain-JVM suite: 27 tests, 0 failures, 0 errors.
- Clean `checkstyleTest build` with Spotless re-enabled: passing.
- Java 8 Forge dedicated-server suite: 2 tests, 0 failures, 0 errors.

## 2026-08-14 — TCuboidPart Java port

### Observable behavior

No known divergence for any part that reaches the cuboid implementation. `getSubParts` still yields one
`IndexedCuboid6` holding a boxed `Integer` 0 and a copy of the bounds, `getCollisionBoxes` still yields the bounds
instance itself, and `drawBreaking` performs the same pipeline setup and `BlockRenderer.renderCuboid` call.

The concrete `java.lang.Iterable` implementation changes from a Scala `Seq` wrapped by `JavaConversions` to
`Collections.singletonList`. Both are single-element and immutable; only code inspecting the runtime class or
attempting mutation could observe the difference.

### Accepted divergence: Scala trait linearization is not reproducible

This is the significant one, and it affects source compatibility rather than binary compatibility.

A Scala trait that overrides superclass methods wins over that superclass. A Java interface loses to it: on the JVM a
superclass method always beats an interface default. `TCuboidPart` is now a Java interface, so a **recompiled** Scala
class written as `class Foo extends TMultiPart with TCuboidPart` silently inherits `TMultiPart`'s empty `getSubParts`
and `getCollisionBoxes` instead of the cuboid ones. It compiles cleanly and fails at runtime with missing collision and
selection boxes.

Declaring the interface methods as `default` would not help, because the superclass still wins. They are therefore left
abstract, which also matches the reference bytecode exactly.

Consequences:

- Already compiled binaries are unaffected. Their forwarders call `TCuboidPart$class`, which is retained.
- Java consumers are unaffected. They extend `JCuboidPart`, which carries the overrides.
- Scala consumers that recompile must extend `JCuboidPart`, or declare the three overrides themselves and delegate to
  `JCuboidPart.subParts`, `JCuboidPart.collisionBoxes` and `JCuboidPart.renderBreaking`.

`Microblock` was the one in-repo case and now does exactly that. It changed from `extends TCuboidPart` to
`extends TMultiPart with TCuboidPart` plus the three explicit forwarders, because a Java interface can no longer supply
`TMultiPart` as its superclass.

`CuboidPartCharacterizationTest.cuboidBehaviorWinsOverTheEmptyTMultiPartDefaults` is the regression guard for this.

### Supported JVM API

- Preserved every reference descriptor on `TCuboidPart`, `JCuboidPart` and `TCuboidPart$class`, verified by diffing
  `javap -s` output against the reference dev jar built at `246daff`.
- Retained `TCuboidPart$class` with all four statics, since ForgeRelocationFMP, OpenComputers, ProjRed and ProjectBlue
  call them. Its bodies cannot delegate to the instance methods, because old forwarders call the statics and would
  recurse; they delegate to the canonical statics on `JCuboidPart` instead.
- Kept `@SideOnly(Side.CLIENT)` on `TCuboidPart.drawBreaking` and `TCuboidPart$class.drawBreaking`, matching the
  reference. As in the reference, `Microblock.drawBreaking` is deliberately not annotated, so on a dedicated server it
  survives while its callee is stripped. That hazard is unchanged; the method is never invoked server-side.
- Added `JCuboidPart.subParts`, `JCuboidPart.collisionBoxes` and `JCuboidPart.renderBreaking` as public statics, plus a
  private constructor on `TCuboidPart$class`. Both are additive.

### Build layout

`compileJava` runs before `compileScala`, so Java sources under `src/main/java` cannot reference types that are still
Scala. All three files live under `src/main/scala/codechicken/multipart/` for joint compilation, matching the existing
`minecraft/McBlockPart.java`. They can move to `src/main/java` once `TMultiPart` is ported.

### Compiler artifacts

None removed. The Scala implementation generated no closure classes for this trait.

### Validation

- `CuboidPartCharacterizationTest`: 5 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `TCuboidPartBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, loading a Scala 2.11.5 class compiled against the
  reference dev jar whose forwarders call all four `$class` statics.
- Complete plain-JVM suite: 33 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 2 tests, 0 failures, 0 errors. The `RenderBlocks` side warning and the `antlr4`
  jar parse errors in that log are present on the pre-port baseline as well.

## 2026-08-14 — TNormalOcclusion Java port

Covers `NormalOcclusionTest`, `NormalOcclusionTest$`, `JNormalOcclusion`, `TNormalOcclusion`,
`TNormalOcclusion$class` and `NormallyOccludedPart`, which all shared one Scala file.

### Observable behavior

No known divergence in the occlusion result. The box test still returns true when every pair of boxes fails to
intersect, still gathers the neighbour's normal boxes followed by its partial boxes in that order, and the bridge still
evaluates the box test before the super chain and short-circuits on failure.

`Traversable` inputs are drained through `JavaConversions.asJavaIterator(boxes.toIterator())` into an `ArrayList`
before testing, rather than traversed with `forall`. Iteration order is preserved. The result of the test does not
depend on order, since `intersects` is side-effect free.

### Accepted divergence: super accessor becomes a non-synthetic default

The reference interface declares `codechicken$multipart$TNormalOcclusion$$super$occlusionTest(TMultiPart)Z` as
`ACC_PUBLIC | ACC_ABSTRACT | ACC_SYNTHETIC`. `javac` hides synthetic members completely, so no Java class, and no
Scala class recompiled against a Java interface, could ever implement it. Left abstract it would make the interface
unimplementable outside the original Scala compiler.

It is therefore emitted as a non-synthetic `default` returning `true`. `true` is the identity for the `&&` chain the
bridge builds, and matches `TMultiPart.occlusionTest`. Classes compiled against the reference supply their own
override carrying their real super chain, so the default is only reached if the deprecated bridge is used on a class
that never had one. Method resolution ignores `ACC_SYNTHETIC`, so existing `invokeinterface` call sites are unaffected.

Consequently `NormallyOccludedPart` no longer declares that method itself and inherits the default. An old
`invokevirtual` against it still resolves through the interface default and still returns `true`.

### Accepted divergence: Scala apply sugar on NormalOcclusionTest

`NormalOcclusionTest` was a Scala `object`, so Scala callers could write `NormalOcclusionTest(boxes1, boxes2)`. It is
now a Java class with static methods, and that sugar no longer compiles; recompiled Scala callers must write
`NormalOcclusionTest.apply(...)`. The in-repo callers in `EdgeMicroblock` and `HollowMicroblock` were updated.

Binary compatibility is unaffected: both static forwarders keep their reference descriptors, and `NormalOcclusionTest$`
keeps `MODULE$` plus both instance methods, which is what ForgeRelocationFMP and OpenComputers link against.

### Accepted divergence: NormallyOccludedPart.getType

The reference emitted two `getType` methods, `()Ljava/lang/String;` and `()Lscala/runtime/Null$;`, because Scala infers
`Null` for `def getType = null`. Only the `String` form remains. The `Null$` form was a compiler artifact of the
inferred type, and no jar in the pack references it. AE2, the only consumer of this class, uses the `(Cuboid6)V`
constructor and the type itself.

### Trait linearization, as with TCuboidPart

Both in-repo mixins needed explicit handling, and the two cases differ:

- `HollowMicroblock` did not override `occlusionTest` at all and relied on the trait. It now declares
  `NormalOcclusionTest.apply(this, npart) && super.occlusionTest(npart)`.
- `PostMicroblock` already overrode `occlusionTest` and ended with `super.occlusionTest(npart)`, which previously
  routed through `TNormalOcclusion` because it was the last mixin. That call now reaches `Microblock` directly and
  would have skipped the box test entirely, so the same expression was substituted at that call site.

This second shape is the more dangerous one: the class looks correct, still compiles, and silently drops the box test.
Every remaining trait conversion needs its existing `super` call sites audited, not just its missing overrides.

### Supported JVM API

- `NormalOcclusionTest$`, `JNormalOcclusion` and `TNormalOcclusion` are descriptor-identical to the reference.
- `NormalOcclusionTest` and `TNormalOcclusion$class` add only a private constructor, and `NormalOcclusionTest` three
  private helpers. Both public `apply` descriptors are unchanged, including
  `(Lscala/collection/Traversable;Lscala/collection/Traversable;)Z`.
- Scala's `Traversable` is retained in two descriptors, as the ABI inventory anticipated.

### Compiler artifacts

Accepted divergence: `NormalOcclusionTest$$anonfun$apply$1` and `NormalOcclusionTest$$anonfun$apply$1$$anonfun$apply$2`
are removed. The Java loops produce no replacement classes. These were closure implementation details of the nested
`forall` and are not retained as supported API.

### Documentation fix

The reference scaladoc said the test "returns true if the test fails", which the earlier characterization showed to be
backwards. The Java doc comments now say the test returns true when the parts may coexist.

### Validation

- `NormalOcclusionCharacterizationTest`: 12 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `TNormalOcclusionBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, exercising forwarder, `$class`, singleton
  and the accessor callback.
- Complete plain-JVM suite: 38 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 2 tests, 0 failures, 0 errors.

## 2026-08-14 — TFacePart Java port

### Observable behavior

No known divergence. `solid` still returns true for every side, including negative and out-of-range values, and
`redstoneConductionMap` still returns 0. `TFacePart` still extends `TSlottedPart`.

### The first trait where default methods are safe

`TMultiPart` declares neither `solid` nor `redstoneConductionMap`, and neither does anything in the microblock
superclass chain. There is therefore no superclass method for an interface default to lose to, and both members are
emitted as real Java `default` methods rather than left abstract.

This is the check to apply to every remaining trait: a default is safe only when no class in an implementor's
superclass chain declares the same member. `TCuboidPart` and `TNormalOcclusion` both failed that check and had to stay
abstract with explicit forwarders.

As a result no in-repo Scala needed changing. `FaceMicroblock` and `HollowMicroblock` already override what they care
about, and `FaceMicroblock` picks up the `redstoneConductionMap` default exactly as it previously picked up the trait
implementation.

### Improvement for Java implementors

Both members were abstract on the reference interface, so a Java class implementing `TFacePart` had to supply them even
to accept the defaults; `minecraft/McSidedMetaPart` does exactly that. Java implementors can now inherit them. This is
additive: `McSidedMetaPart` is descriptor-identical to the reference, and existing implementors that declare their own
overrides are unaffected.

### Supported JVM API

- `TFacePart` is descriptor-identical to the reference, including both members remaining public and non-static on the
  interface. Changing them from abstract to default does not affect method resolution for existing implementors, which
  all declare their own.
- Retained `TFacePart$class` with all three statics for OpenComputers, ProjRed and ProjectBlue. It adds only a private
  constructor. Its bodies hold the defaults directly rather than dispatching through the instance, matching the
  reference and avoiding recursion from old forwarders.
- The emitted class-file set for this area is unchanged; the Scala implementation generated no closure classes here.

### Validation

- `FacePartCharacterizationTest`: 4 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `TFacePartBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, loading a Scala 2.11.5 consumer whose forwarders
  call all three statics.
- Complete plain-JVM suite: 43 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 3 tests, 0 failures, 0 errors. A new case covers what plain-JVM tests cannot:
  microblock classes are generated by ASM at runtime, so it checks that a generated `FaceMicroblock` inherits the
  `redstoneConductionMap` default and that a generated `HollowMicroblock` still returns its own `0x10` and `false`.

## 2026-08-14 — TIconHitEffects Java port

Covers `IconHitEffects`, `JIconHitEffects`, `JIconHitEffects$class`, `TIconHitEffects` and `TIconHitEffects$class`,
which shared one Scala file.

### Observable behavior

No known divergence. `getBreakingIcon` still ignores its `subPart` argument and returns `getBrokenIcon(side)`, destroy
effects still request sides 0 through 5 in order, and the `scaleDensity` overload still chooses between the part's own
bounds and `Cuboid6.full`.

Evaluation order in `addDestroyEffects` is deliberately preserved: icons first, then bounds, then the tile. Hoisting
the tile read to the top of the method would have been tidier but changes which work happens before a null tile fails,
and the characterization pins the reference order.

### Removed API: IconHitEffects$

Accepted divergence: the `IconHitEffects$` companion singleton and its `MODULE$` field are removed. No jar in the pack
references the class, by bytecode or by name; the four consumers of this area use the static forwarders on
`IconHitEffects` (WR-CBE, extrautilities) or the two `$class` bridges (ForgeRelocationFMP, OpenComputers, ProjRed).

This is the same reasoning applied to the dead `IDWriter` and `JPartialOcclusion` bridges, but it removes a public
class rather than a method, so it is worth calling out separately. Restoring it is a thin delegating class if any
consumer outside the pack turns out to need it. Note the contrast with `NormalOcclusionTest$`, which is referenced by
ForgeRelocationFMP and OpenComputers and was therefore kept.

### Trait linearization, split decision

The two interfaces needed opposite treatment, which is the rule from the `TFacePart` port working as intended:

- `JIconHitEffects.getBreakingIcon` becomes a real `default`. `TMultiPart` does not declare it, so nothing in an
  implementor's superclass chain can beat it. Java implementors can now inherit it; `minecraft/McBlockPart` already
  declared its own and stays descriptor-identical.
- `TIconHitEffects.addHitEffects` and `addDestroyEffects` stay abstract. `TMultiPart` declares both, so a default would
  silently lose to its empty versions.

`MicroblockClient` was the one in-repo case and now declares both explicitly, delegating to the `IconHitEffects`
statics.

### Supported JVM API

- `JIconHitEffects` and `TIconHitEffects` are descriptor-identical to the reference.
- `IconHitEffects`, `JIconHitEffects$class` and `TIconHitEffects$class` add only a private constructor. All three
  `IconHitEffects` static overloads keep their reference descriptors, including the three-argument `scaleDensity` form
  that extrautilities links against.
- `@SideOnly(Side.CLIENT)` placement matches the reference exactly: two on `JIconHitEffects`, one on
  `JIconHitEffects$class`, two on `TIconHitEffects`, two on `TIconHitEffects$class`, and none on `IconHitEffects`.
- `JIconHitEffects` does not extend `TMultiPart` at bytecode level in the reference either, so the statics cast to
  `TMultiPart` internally exactly as the reference does. Passing a non-`TMultiPart` still fails with
  `ClassCastException`.

### Compiler artifacts

Accepted divergence: `IconHitEffects$$anonfun$addDestroyEffects$1` is removed along with `IconHitEffects$`. The Java
loop produces no replacement class.

### Validation

- `IconHitEffectsCharacterizationTest`: 5 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `TIconHitEffectsBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, exercising all five statics across both
  bridges.
- Complete plain-JVM suite: 49 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 3 tests, 0 failures, 0 errors.
- Particle appearance itself needs a client and stays on the manual checklist.

## 2026-08-14 — TItemMultiPart Java port

Covers `JItemMultiPart`, `TItemMultiPart` and `TItemMultiPart$class`, which shared `ItemMultiPart.scala`.

### Observable behavior

No known divergence. `getHitDepth` still projects a copy of the hit vector onto the axis for that side and adds 1 on
even sides, leaving the caller's vector untouched. `onItemUse` still tries the clicked block only when the depth is
below 1, then always tries the neighbour on that side, reusing and mutating a single `BlockCoord` across both attempts
and passing the same `Vector3` to every `newPart` call. Placement still short-circuits on a null part before touching
the world, still skips `addPart` on the client, and still decrements the stack only outside creative mode.

### First trait extending a Minecraft class rather than TMultiPart

`TItemMultiPart extends Item`, so the linearization check runs against `Item` instead of `TMultiPart`. The outcome is
the same split seen in the `TIconHitEffects` port:

- `getHitDepth` becomes a real `default`. `Item` does not declare it, so nothing in an implementor's superclass chain
  can beat it.
- `onItemUse` stays abstract. `Item` declares it, so a default would silently lose to the vanilla implementation and
  placement would stop working.

This confirms the rule generalises: what matters is the implementor's actual superclass, not `TMultiPart` specifically.

There were no in-repo implementors beyond `JItemMultiPart` itself, so nothing else needed changing.

### Supported JVM API

- `TItemMultiPart` is descriptor-identical to the reference.
- `JItemMultiPart` keeps its constructor, both instance methods and their descriptors; WR-CBE links against the
  constructor and the inherited `func_77658_a`. It gains two public statics, `hitDepth` and the `onItemUse` overload
  taking the part, plus a private `place`. All additive.
- `TItemMultiPart$class` keeps all three public statics for ProjRed, its only consumer. It adds a private constructor
  and drops the reference's private `place$1` helper, which was never visible to consumers.
- `getHitDepth` on the bridge ignores its part argument and calls the static directly, matching the reference, whose
  trait body never used `this`. The `onItemUse` path still dispatches `getHitDepth` through the interface, so an
  implementor's override is honoured exactly as before.

### Compiler artifacts

None removed or added. The emitted class-file set for this area is unchanged; the local `place` function compiled to a
private static method rather than a closure class in both versions.

### Validation

- `ItemMultiPartCharacterizationTest`: 7 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `TItemMultiPartBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, driving a reference-compiled Scala consumer
  through both attempt positions.
- Complete plain-JVM suite: 57 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 3 tests, 0 failures, 0 errors.
- Real placement into a world needs a client or a server scenario and stays on the functional and manual layers.

## 2026-08-14 — TEdgePart Java port

### Observable behavior

No known divergence. `conductsRedstone` still returns false and `TEdgePart` still extends `TSlottedPart`.

### Default method, same test as TFacePart

`TMultiPart` does not declare `conductsRedstone`, and nothing in the microblock superclass chain does either, so the
member is emitted as a real Java `default`. `EdgeMicroblock` never declared its own forwarder even in the reference,
because it is a Scala trait rather than a concrete class, and the generated concrete classes now resolve the interface
default instead of a Scala trait forwarder. The functional microblock generation case added during the `TFacePart`
port already covers that resolution path.

No in-repo Scala needed changing.

### Supported JVM API

- `TEdgePart` is descriptor-identical to the reference.
- `TEdgePart$class` keeps both statics and adds only a private constructor. OpenComputers links against `$init$` only,
  but `conductsRedstone` is retained because the bridge class is being kept regardless and dropping one static from a
  live bridge would be a gratuitous shape change.
- The emitted class-file set for this area is unchanged; the Scala implementation generated no closure classes here.

### Validation

- `EdgePartCharacterizationTest`: 3 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `TEdgePartBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors.
- Complete plain-JVM suite: 61 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 3 tests, 0 failures, 0 errors.

## 2026-08-14 — Saw Java port

The first port outside `codechicken.multipart`. `Saw` was declared in `ItemSaw.scala`; only the trait is extracted,
and `ItemSaw` itself stays Scala.

### Observable behavior

No known divergence. `getMaxCuttingStrength` still asks the saw about a stack of itself, so the stack handed to
`getCuttingStrength` wraps the saw and has size 1, and the result still follows an implementor's
`getCuttingStrength` override.

### Default method, and a shape change on ItemSaw

`Item` does not declare `getMaxCuttingStrength`, so it becomes a real Java `default`.

Consequence worth recording: `ItemSaw` previously carried a Scala trait forwarder for `getMaxCuttingStrength` and now
inherits the default instead, so it no longer declares that method. Three jars reference the `ItemSaw` type
(IguanaTweaksTConstruct, ProjectBlue, endercore) but none reference any of its members, and an `invokevirtual` against
`ItemSaw.getMaxCuttingStrength` would still resolve through the interface default. `harvestLevel` and
`getCuttingStrength` are unchanged.

This is the first time a port changed the declared members of a class it did not touch. Watch for it whenever a trait
default is introduced on a type with concrete Scala implementors.

### Unlike the other constant bridges

`Saw$class.getMaxCuttingStrength` dispatches `getCuttingStrength` back through the interface, so an implementor's
override is honoured even when the bridge is called directly. `TFacePart$class` and `TEdgePart$class` hold constants
and deliberately ignore overrides. Both behaviors are preserved as found and are asserted separately.

### Supported JVM API

- `Saw` is descriptor-identical to the reference.
- `Saw$class` keeps both statics for ProjRed and adds only a private constructor. Its body casts to `Item` to build
  the stack, exactly as the reference bytecode did.
- The emitted class-file set for this area is unchanged.

### Validation

- `SawCharacterizationTest`: 3 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- `SawBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, where the fixture reports its strength only if handed a
  stack that wraps itself.
- Complete plain-JVM suite: 65 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 3 tests, 0 failures, 0 errors.

## 2026-08-14 — MicroMaterialRegistry Java port

The first stateful registry converted, and the widest surface so far: ten jars reference the type and three more reach
it reflectively. Covers `MicroMaterialRegistry`, `MicroMaterialRegistry$` and the two nested interfaces.

### Observable behavior

No known divergence. `setupIDMap` still sorts the type map by name into the id array so ids are indexes into it,
`materialID` and `materialName` still round trip, `remapName` still resolves on both `getMaterial(String)` and
`materialID`, `getMissingId` still fails loudly when the placeholder is absent rather than falling back to zero, ids
still round trip through the shared `IDWriter`, and a highlight renderer that claims the highlight still short-circuits
the default renderer.

`calcMaxCuttingStrength` still throws when no saw is registered. The reference reached that through `max` on an empty
collection; the port throws `UnsupportedOperationException` explicitly to keep the same failure rather than silently
leaving the field at zero.

### Changed API: readIDMap

Accepted divergence: `readIDMap` returns `java.util.List<String>` instead of `scala.collection.Seq<String>`. No jar in
the pack references it, and the only caller is the in-repo material registration packet handler, which was updated
from `mkString` to `String.join`. This removes one of the five Scala-typed descriptors the ABI inventory identified.

`getIdMap()[Lscala/Tuple2;` is deliberately **not** changed, because extrautilities links against it. The registry
therefore still stores `scala.Tuple2` internally rather than converting on every call.

### Removed API

- The three mangled accessors Scala emitted as public so its closure classes could reach private state:
  `codechicken$microblock$MicroMaterialRegistry$$idMap()`, `$$typeMap()` and `$$nameMap()`. They are compiler
  artifacts, not API, and no jar references them.
- `MicroMaterialRegistry$IMicroMaterial$class`, along with its four statics. No jar references it. All ten
  implementors reach `IMicroMaterial` through `BlockMicroMaterial`, which is in-repo, so nothing external carries
  forwarders to the helper.
- `MissingMicroMaterial.canRenderInPass(int)` and `MissingMicroMaterial.isSolid()` static forwarders, plus the
  matching instance methods on `MissingMicroMaterial$`. That object overrides neither, so with the members now being
  Java interface defaults Scala emits no forwarder and therefore no static forwarder either. Nothing in the pack
  references `MissingMicroMaterial` at all. This is the same class of change as `ItemSaw` in the `Saw` port, and the
  characterization caught it by failing to compile.

### Added API

`setupIDMap`, `calcMaxCuttingStrength` and `loadIcons` gain static forwarders on `MicroMaterialRegistry`. They were
`private[microblock]` in Scala, which is public on the JVM but produces no static forwarder. Additive.

### IMicroMaterial defaults

`loadIcons`, `canRenderInPass` and `isSolid` become real Java defaults. Neither nested interface has a superclass to
lose to, and both in-repo implementors, `BlockMicroMaterial` and `MissingMicroMaterial`, extend nothing else. Java
implementors can now inherit the three instead of restating them; both nested interfaces are descriptor-identical to
the reference.

### Compiler artifacts

Accepted divergence: all eleven `MicroMaterialRegistry$$anonfun$*` classes are removed. The Java loops produce no
replacement classes.

### Validation

- `MicroMaterialRegistryCharacterizationTest`: 7 tests, 0 failures, 0 errors, passing against both the Scala baseline
  and the port.
- `MicroMaterialRegistryBinaryCompatibilityTest`: 1 test, 0 failures, 0 errors, driving a reference-compiled Scala
  consumer through `MODULE$` and the raw `scala.Tuple2` array.
- Complete plain-JVM suite: 73 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 3 tests, 0 failures, 0 errors. This exercises the real path, since server
  startup calls `setupIDMap`, `calcMaxCuttingStrength` and the default content registration.
- Error paths remain uncovered by tests: they call the microblock logger, which is null until `MicroblockProxy.preInit`
  runs, so they cannot execute headless. They are unchanged from the reference.

## 2026-08-14 — MultiPartRegistry Java port

Covers `MultiPartRegistry`, `MultiPartRegistry$` and the three nested interfaces. Twelve jars reference the type and
Schematica reaches the singleton reflectively.

### Observable behavior

Registry behavior itself has no known divergence. `beforeServerStart` still sorts the type map by name so ids are indexes into it, part ids still
round trip through the shared `IDWriter`, converters are still grouped per block with the first non-null result
winning, registration still refuses to run once the registry is closed and still requires an active mod container, a
duplicate part id still throws, and `writePartID` and `getModContainer` still throw rather than returning null for an
unknown name.

### Resolved reflective regression, discovered and fixed 2026-08-27

The full consumer-source audit found a constraint that the bytecode scan missed. Schematica 1.12.6 obtains the private
field `codechicken$multipart$MultiPartRegistry$$typeMap` from `MultiPartRegistry$` and casts its value to
`scala.collection.mutable.Map`. The Java port moved the canonical map to a private `java.util.HashMap` on
`MultiPartRegistry`, and `MultiPartRegistry$` exposed no compatibility field, so Schematica disabled its FMP
integration during reflective initialization.

`MultiPartRegistry$` now restores that exact private field and descriptor using Scala's existing live Java-map wrapper;
the Java map remains the single source of truth. `MultiPartRegistryCharacterizationTest` performs Schematica's actual
`ReflectionHelper` lookup, casts the result to `scala.collection.mutable.Map`, and proves mutations reach the Java
registry. No separate registry or synchronization path was added.

### Preserved Scala-typed descriptors

Both remain, because ForgeRelocationFMP and ProjRed link against them:

- `registerParts(Lscala/Function2;Lscala/collection/Seq;)V`
- `registerParts(Lcodechicken/multipart/MultiPartRegistry$IPartFactory2;Lscala/collection/Seq;)V`

The `Seq` overload drains through `JavaConversions.seqAsJavaList`. The `Function2` overload applies the function with
a boxed `Boolean`, matching what the Scala closure received.

### Array overloads become Java varargs

`registerParts(IPartFactory, String[])` and `registerParts(IPartFactory2, String[])` are declared `String...`. The
descriptor is unchanged, verified against the reference dev jar; `ACC_VARARGS` is a compiler flag and does not affect
linkage. This was needed because `MicroblockClass` called the Scala varargs form with a single string, which no longer
resolves against a plain array parameter. It also makes the Java call sites more natural without adding API.

### Changed and removed API

- `readIDMap` returns `java.util.List<String>` instead of `scala.collection.Seq<String>`, as with
  `MicroMaterialRegistry`. It is `private[multipart]` in Scala, so it has no static forwarder, and no jar references
  it.
- The mangled public accessor methods Scala emitted so its closures could reach private state
  (`codechicken$multipart$MultiPartRegistry$$typeMap()` and the four others) are gone. No jar links those methods.
  This original check did not cover Schematica's reflective access to the separate private backing field described
  above.

### Compiler artifacts

Accepted divergence: the eight `MultiPartRegistry$$anonfun$*` classes and `MultiPartRegistry$$anon$1` are replaced by
two Java anonymous classes, `MultiPartRegistry$1` and `$2`, which are the two `IPartFactory2` adapters the deprecated
overloads build.

### Validation

- `MultiPartRegistryCharacterizationTest`: 5 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- Complete plain-JVM suite: 78 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 5 tests, 0 failures, 0 errors. This is the layer that matters for this class:
  registration needs FML's active mod container, so real startup is the only place both factory overloads actually
  run, and the suite resolves a part registered through each.
- Error paths that log remain uncovered headless, as before, because the multipart logger is null until preInit.

### 2026-08-27 compatibility repair validation

- The new Schematica reflection case failed first with `UnableToFindFieldException`, then passed with the compatibility
  field present.
- `javap -private -s` confirms the field is private/final with descriptor `Lscala/collection/mutable/Map;`.
- `MultiPartRegistryCharacterizationTest`: 6 tests, 0 failures, 0 errors.
- Complete plain-JVM suite: 126 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.

## 2026-08-14 — TileMultipart Java port

The central tile. Twelve jars reference it and about twenty-five of its members. `TileMultipartClient` was split into
its own Scala file and deliberately **not** converted: it is registered with the ASM generator through
`registerTrait`, so converting it is Phase 5 `registerJavaTrait` work with different machinery.

### Observable behavior

No known divergence. Parts are still appended in order, the list is still replaced rather than mutated so a previously
published `Seq` never changes under a caller, `operate` still skips parts whose tile is null, ticking still starts on
the first ticking part and stops when the last one leaves, `getLightValue` is still max-or-zero while
`getExplosionResistance` is still max-or-throws, occlusion still requires agreement in both directions, and
`canReplacePart` still excludes the outgoing part while rejecting a part already present.

`handlePacket` case 254 resolves the tile twice, once for the receiver and once for the argument, because the
reference used a by-name local def that re-evaluated `TileCache.findTile` at each use. Argument order is preserved.

The `assert` calls on part count, client removal and client addition become explicit `AssertionError` throws with the
same messages, since Scala's `assert` is always enabled here and Java's is not.

### Storage

`partList` remains a `scala.collection.immutable.List` published through `partList()Lscala/collection/Seq;`, which
five jars link against. Mutations copy to a Java list, mutate, and republish through `JavaConversions`, matching the
reference's allocate-a-new-Seq semantics exactly. The focused Phase 4 baseline later found that the Java port's
`parts()` read helper also materializes an `ArrayList`, backing array, iterator and conversion wrappers on every
traversal. That read-side cost was later removed after focused characterization; see `JAVA_MIGRATION_PROFILE.md`.

`operate` keeps its `(Lscala/Function1;)V` descriptor and internal callers still go through it, wrapping their action
in an `AbstractFunction1`. That preserves both the virtual dispatch, so any trait overriding `operate` still sees
every call, and the reference's callback-adapter call shape.

### 2026-08-27 Phase 4 traversal optimization

The initial Java port implemented every read through `parts()`, which copied the Scala `Seq` into a Java `ArrayList`.
The focused profile measured about 184 allocated bytes for every eight-part `operate` call. `operate` now captures the
published sequence itself and walks the normal immutable Scala `List` through its existing head/tail chain. Other
`Seq` implementations accepted through `partList_$eq` retain an iterator fallback.

This restores the reference's captured-sequence behavior: a callback-added part is not visited, and an original part
detached before its turn is skipped by the existing null-tile guard. The public member descriptors and virtual call
path are unchanged. The matching profile measured 0 B/call for direct `operate`, 0.05 B/call through `updateEntity`,
and roughly 4.3x higher throughput. Three new plain-JVM tests cover addition, detachment, and the non-list fallback;
the complete 133-test plain-JVM and 28-test Forge server suites pass.

### Preserved Scala-typed descriptors

- `partList()Lscala/collection/Seq;` and `partList_$eq(Lscala/collection/Seq;)V`
- `operate(Lscala/Function1;)V`
- `getOrConvertTile2(...)Lscala/Tuple2;`
- `loadParts(Lscala/collection/Iterable;)V`
- `rayTraceAll` still stores a `scala.Tuple2` into `ExtendedMOP.data`, which `EdgeMicroblock` reads back through
  `getData[(Int, _)]`.

### Removed API

`protected$worldObj(TileMultipart)`, the public accessor Scala emitted so the `dropItems` closure could reach the
protected field. It is a compiler artifact and no jar references it. `TileMultipartClient` lost nothing, including its
generated super accessor for `markRender`, which Scala regenerated correctly against the Java superclass.

### Recompiled Scala consumers

The largest source-only divergence so far. Scala reads the Java signatures rather than a ScalaSignature, so:

- `tile.partList(i)` no longer parses as an index; it must be `tile.partList.apply(i)`. Eleven in-repo call sites
  were updated.
- `TileMultipart.renderID = x` must become `renderID_$eq(x)`.
- `getOrConvertTile2` returns `Tuple2[TileMultipart, Object]` rather than `(TileMultipart, Boolean)`, so `_2` needs an
  explicit cast. Two in-repo call sites.
- `operate { p => ... }` no longer accepts a lambda, because Scala 2.11 will not adapt `TMultiPart => Unit` to
  `Function1[TMultiPart, BoxedUnit]`. The one in-repo caller, `TTileChangeTile`, now builds an `AbstractFunction1`.

Binary compatibility is unaffected in all four cases.

### Compiler artifacts

Accepted divergence: all 31 `TileMultipart$$anonfun$*` classes are removed, replaced by one Java anonymous class for
the `operate` adapter.

### Validation

- `TileMultipartCharacterizationTest`: 12 tests, 0 failures, 0 errors, unchanged from the Scala baseline.
- Complete plain-JVM suite: 90 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 5 tests, 0 failures, 0 errors. This is the load-bearing check: the ASM
  generator builds composite tiles that extend this class and mix Scala traits into it, and the suite still generates
  a `TSlottedTile` composite and two microblock classes.
- Every public member of the reference is still present except `protected$worldObj`, verified by name-level diff, and
  `partList`, `operate`, `jPartList`, `getOrConvertTile2`, `multiPartChange`, `rayTraceAll` and `dropItems` were
  checked descriptor by descriptor.
- World-dependent paths, NBT round trips through `createFromNBT`, and desc packets are not covered by tests and stay
  on the manual checklist.

## 2026-08-14 — TMultiPart Java port

The base class every part extends. Five jars extend it and about twenty of its members are referenced.

### Observable behavior

No known divergence. The class is almost entirely no-op defaults; the only real implementation is
`collisionRayTrace`, which still offsets each sub part by the tile coordinates, rebuilds them as `IndexedCuboid6`
carrying the original `data`, and delegates to `RayTracer.rayTraceCuboids` with the same block coordinate and block
type. `x`, `y` and `z` still throw `NullPointerException` when the part is unbound, and `world` still returns null
rather than throwing.

`getSubParts` was traversed through `JavaConversions` purely to get Scala's `map`; `rayTraceCuboids` already took a
`java.util.List`, so the Java loop builds that list directly and the conversion disappears.

### Supported JVM API

This is the cleanest result so far. Diffed against a reference dev jar built at `cacc9a3`:

- Every descriptor is identical, checked across all members rather than a sample.
- No public member lost and none added.
- `@SideOnly(Side.CLIENT)` appears on the same seven members.
- `getTile()` and the no-argument `addDestroyEffects` remain the only deprecated members. `javap` reports a higher
  raw count because javac emits both the `Deprecated` attribute and the annotation where Scala emitted one form; the
  marked members are identical.
- `tile` keeps its `tile()` and `tile_$eq(TileMultipart)` accessor pair, so Scala callers can still write
  `part.tile = t`.

### Recompiled Scala consumers

None. No in-repo Scala call site needed changing, which is the first port where that has been true of a type this
widely used. Scala resolves the empty-paren Java accessors the same way it resolved the Scala ones, and assignment to
`tile` still desugars to `tile_$eq`.

### Compiler artifacts

Accepted divergence: `TMultiPart$$anonfun$1`, the closure from the `getSubParts.map` in `collisionRayTrace`, is
removed. The Java loop produces no replacement class.

### Validation

- Complete plain-JVM suite: 90 tests, 0 failures, 0 errors, including the twelve `TileMultipart` cases that drive
  parts through `bind`, `doesTick`, `occlusionTest`, `getLightValue` and `explosionResistance`.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 5 tests, 0 failures, 0 errors, covering real part registration and
  instantiation of `mc_torch` and `mcr_face` plus composite tile generation.
- Ray tracing, rendering and particle paths need a client and stay on the manual checklist.

## 2026-08-14 — TickScheduler Java port

No jar in the pack references this class, so the ABI constraint was minimal, but the reference shape was preserved
anyway for consistency with the other ports.

### Observable behavior

No known divergence. Ticks scheduled while the world is mid-tick are still queued and applied after processing rather
than during it, an existing entry for the same part is still only overridden when going from random to scheduled,
random-update parts still reschedule themselves 800 to 1600 ticks out, chunks are still dropped from the active set
once their list empties, and only non-random entries are still written to chunk NBT.

`processTicks` filtered a `ListBuffer` with a side-effecting predicate that fires the part callbacks and mutates the
entry. The Java version iterates and rebuilds the list in the same order, so callback order and the reschedule
mutation are unchanged.

### The singleton carries behavior here

Unlike the other companion singletons in this migration, `TickScheduler$` is not a pure forwarder. The object extends
`WorldExtensionInstantiator` and `WorldExtensionManager.registerWorldExtension` is handed the instance itself, so the
two instantiator hooks have to live on the singleton. It stays a Java class extending `WorldExtensionInstantiator`
with `MODULE$`, delegating into the statics.

### Supported JVM API

- `TickScheduler$` and `TickScheduler$PartTickEntry` are descriptor-identical to the reference, including
  `PartTickEntry`'s Scala-style `time()`, `time_$eq`, `random()` and `random_$eq` accessor pairs and both
  constructors.
- `TickScheduler` adds only a private constructor.
- `WorldTickScheduler` and `ChunkTickScheduler` were Scala `private class` and are now package-private static nested
  classes, keeping their `TickScheduler$WorldTickScheduler` and `TickScheduler$ChunkTickScheduler` binary names.

### Recompiled Scala consumers

One call site. `WorldExtensionManager.registerWorldExtension(TickScheduler)` passed the object as a value and must now
pass `TickScheduler$.MODULE$`.

`TRandomUpdateTick` is a Scala trait extending `TMultiPart`, so at bytecode level it is a bare interface that does not
extend the class. The Java code casts where the reference relied on Scala's view of the type, the same pattern already
recorded for `JIconHitEffects`.

### Compiler artifacts

Accepted divergence: the five `$$anonfun$` classes under `WorldTickScheduler.postTick` and `ChunkTickScheduler`'s
`processTicks`, `saveData` and `loadData` are removed. The Java loops produce no replacement classes.

### Validation

- Complete plain-JVM suite: 90 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 5 tests, 0 failures, 0 errors. This exercises the port directly rather than
  incidentally: the scheduler is registered as a world extension during startup, so `createWorldExtension`, `load`,
  and the `preTick`/`postTick` pair all run for every server tick of the suite.
- Scheduled and random tick firing over time, and the chunk NBT round trip, are not asserted by any test and stay on
  the manual checklist.

## 2026-08-14 — BlockMultipart Java port

The Block face of the API. No jar references any member of it; it appears only as the return type of
`MultipartProxy.block()`, which ProjRed and buildcraft-compat call, and by name in Waila and guidenh reflection.

### Observable behavior

No known divergence. Every override still resolves the tile through `getTile`, which continues to return null both
when the tile is not a multipart tile and when its part list is empty, so the null branches and the empty-tile branches
stay merged exactly as before. `isAir` still reports true for a missing tile, `getExplosionResistance` still returns 0
rather than reaching the throwing max on an empty tile, and the redstone queries still flip the side with `side ^ 1`.

`reduceMOP` still returns the struck part's index alongside a rebased `ExtendedMOP` carrying that part's own data and
the original distance.

### Supported JVM API

- `BlockMultipart$` is descriptor-identical to the reference, including `reduceMOP`'s `scala.Tuple2` return.
- `BlockMultipart` differs by exactly one descriptor, discussed below. `addCollisionBoxesToList` keeps its descriptor;
  only its generic signature changes from Scala's `List<?>` to the raw `List` that vanilla's `Block` actually
  declares, which is the more faithful override.
- The four static forwarders `getTile`, `getClientTile`, `reduceMOP` and `drawHighlight` are unchanged.

### Removed API

`hasTileEntity$default$1()`, the accessor Scala emits for the default argument on `hasTileEntity(meta: Int = 0)`.
Java has no default arguments, so it disappears. It is a compiler artifact of the default parameter, not API, and no
jar references any member of this class.

### TileMultipartClient is a bare interface here

`getClientTile` returns `TileMultipartClient`, which is a Scala trait extending `TileMultipart`, so in bytecode it is
an interface carrying none of the class's members. `addHitEffects` casts to `TileMultipart` to reach the part list.
This is the third occurrence of the pattern, after `JIconHitEffects` and `TRandomUpdateTick`.

### Compiler artifacts

Accepted divergence: the four `$$anonfun$` classes under `getDrops` and `addCollisionBoxesToList`, including the two
nested inner closures, are removed. The Java loops produce no replacement classes.

### Validation

- Complete plain-JVM suite: 90 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 5 tests, 0 failures, 0 errors. The block is registered during startup and is
  the block every generated multipart tile is placed against, so registration and `hasTileEntity` run for real.
- Almost everything else in this class needs a player, a ray trace or a renderer. Block breaking, selection boxes,
  collision, pick block, activation, particles and light are all on the manual checklist and are the reason that
  checklist exists.

## 2026-08-15 — Marker interface Java port

Six interfaces that carry no implementation: `TSlottedPart`, `IRandomDisplayTick`, `INeighborTileChange`,
`TRandomUpdateTick`, `ISidedHollowConnect` and `IMicroMaterialRender`.

### Observable behavior

No known divergence. Five of the six were already pure abstract interfaces in bytecode with no `$class` helper, so
their conversion is a source-language change with no bytecode consequence at all. The sixth is discussed below.

### Supported JVM API

All six are member-identical and descriptor-identical to the reference. Every implementor and mixin was diffed as
well — `Microblock`, `RedstoneTorchPart`, `TorchPart`, `HollowMicroblock`, `TSlottedTile`, `TTileChangeTile` and
`TRandomDisplayTickTile` are unchanged member for member, so no concrete class gained or lost a forwarder.

`TSlottedPart` and `TRandomUpdateTick` extended `TMultiPart` in Scala source. A trait extending a class is a bare
interface in bytecode, carrying none of the class's members, so the Java interfaces are bare too and match the
reference exactly. This is the same pattern as `JIconHitEffects` and `TileMultipartClient`; the port makes it explicit
in the source rather than changing it.

`IMicroMaterialRender` keeps the Scala accessor names `world`, `x`, `y` and `z`. `TMultiPart` declares all five
members under exactly these names, which is what implements the interface for every part. Renaming any of them to a
bean accessor would silently unimplement it, so the names are pinned by a characterization test.

### Removed API

`TRandomUpdateTick$class`, with its `onWorldJoin` and `$init$` statics. It is the only class removed from the jar by
this port; the full class list is otherwise identical.

`onWorldJoin` **cannot** become an interface default. `TMultiPart` declares `onWorldJoin`, and a superclass method
always beats an interface default on the JVM, so the default would never run and the failure would be silent. It stays
abstract, and each implementor declares it and calls `TickScheduler.loadRandomTick` itself, which is what
`RedstoneTorchPart` — the only implementor in this codebase — already did. The Scaladoc already carried this warning
for Java implementors; it now applies to Scala implementors too.

No jar in the pack references `TRandomUpdateTick` in any form, and the frozen baseline contains exactly the eight
`$class` helpers listed in the inventory, which does not include this one. Per the `IDWriter` precedent, no
compatibility bridge was written. The consequence to accept: a Scala class mixing in `TRandomUpdateTick` and relying
on the trait's `onWorldJoin` must now declare it, or it will resolve to `TMultiPart`'s no-op and never register for
random ticks. This is a recompiled-Scala-consumer break with no shipping consumer.

### Validation

- Complete plain-JVM suite: 99 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 5 tests, 0 failures, 0 errors.
- Public members, descriptors and the full emitted class list diffed against a reference jar built at `3e68e67`.
- Random tick firing over time still needs a world and stays on the manual checklist, as does torch display-tick
  rendering and hollow-cover connection sizing.

## 2026-08-15 — MultipartHelper Java port

The NBT and description-packet entry point. Two of its statics are linked against by shipping jars, and guidenh
reflects on both `MultipartHelper` and `MultipartHelper$` by name.

### Observable behavior

No known divergence. The id guard still returns null before assigning `MultipartSaveLoad.loadingWorld`, so a tag that
is not a saved multipart still leaves the loading world untouched. `sendDescPacket` still resolves the chunk from the
tile's own coordinates and still sends nothing when `getDescPacket` returns null, which it does when the iterator
yields no `TileMultipart`.

`createTileFromParts` now builds one `JavaConversions` wrapper and passes it to both `generateCompositeTile` and
`loadParts`, where the Scala applied the implicit conversion separately at each call site. Both wrappers were views
over the same `java.lang.Iterable`, so this is one allocation fewer and the same traversal.

### Supported JVM API

- `MultipartHelper` is public-member-identical and descriptor-identical to the reference, including
  `createTileFromParts`'s `java.lang.Iterable` parameter, which is easy to misread as Scala's.
- `MultipartHelper$IPartTileConverter` is identical, including the `clazz()` accessor Scala generated for its `val`.
- `MultipartHelper$` keeps `MODULE$`, its private constructor and all four instance forwarders, identical.
- The full emitted class list is unchanged: nothing was added or removed.

`MultipartHelper$` was kept deliberately. It is not among the 17 `MODULE$` singletons the inventory found read from
bytecode, and no jar links against it, but guidenh names it as a reflective string constant. The inventory warns that
removing a companion breaks those consumers even where no bytecode reference exists, so this one stays. This is the
opposite call to `IconHitEffects$`, which was dropped because nothing referenced it in bytecode *or* by name.

### Differences that are not API

- `MultipartHelper` gains a private constructor, as every converted Scala `object` has.
- `MultipartHelper$`'s class initializer loses its `ACC_PUBLIC` flag: Scala emits `public static {}`, `javac` emits
  `static {}`. The JVM invokes `<clinit>` implicitly and it cannot be named by any caller.
- `IPartTileConverter.convertMulti` returns `Collections.emptyList()` or `Collections.singletonList(part)` instead of a
  `scala.collection.convert.Wrappers$SeqWrapper` over a Scala `Seq`. The declared type was always `java.lang.Iterable`
  and no consumer implements or inspects the concrete type, so this is an implementation detail, and it removes a
  wrapper allocation per converted tile.
- The two commented-out blocks the reference carried — the `PlayerInstance.playersInChunk` reflection and the
  multi-tile `sendDescPackets` — are not reproduced as dead code. The reason they were removed, a missing forge access
  transformer, is recorded in the class javadoc instead.

### The cast in convert is still checked

`convert(TileEntity)` calls `convertMulti((T) tile)`, and erasure turns that cast into `(TileEntity)`, which cannot
fail. The `ClassCastException` a mismatched tile produces comes from the synthetic bridge method on the *subclass*
that overrides `convertMulti(T)`, exactly as it did under Scala's `asInstanceOf[T]`. The mechanism is unchanged, and
`convertFailsOnATileTheConverterDoesNotAccept` pins it because it looks like erasure should have swallowed it.

### Validation

- Complete plain-JVM suite: 107 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 10 tests, 0 failures, 0 errors, 5 of them new for this port. They cover
  everything the plain-JVM suite cannot reach: tile construction through the ASM generator, the NBT round trip through
  the save/load hooks, `registerTileConverter` appending to a Scala `MutableList`, and `sendDescPacket` against a
  loaded chunk.
- Public members, descriptors and the full emitted class list diffed against a reference jar built at `0fdf9e5`.
- What stays manual: a description packet actually arriving at a watching client. The functional test builds and sends
  one with no players connected, so the wire format and the client's handling of it are still unasserted.

## 2026-08-15 — TileCache Java port

The client-side tile recovery cache. **Zero downstream references**: no jar links against any member, and guidenh's
reflective name list does not include it. That makes this the first port free to change shape rather than preserve it,
on the `IDWriter` precedent.

### Observable behavior

No known divergence. `add` still stores the tile unflagged and `remove` still replaces that entry with a flagged one
rather than dropping it, both keyed by a fresh `BlockCoord` built from the tile's own coordinates. `findTile` still
prefers the world's tile, still warns only when the cached entry is not flagged removed, and still throws
`RuntimeException("DC: Client multipart @" + c + " not found")` when the cache has nothing.

The reference's trailing `case _ => null` in `findTile` is unreachable — `Some(FlaggedTile(t, rem))` matches whatever
the flag is — so a tile flagged removed is returned just like any other, silently. The Java port makes that explicit
with a comment instead of a dead branch, and `stillReturnsATileThatWasFlaggedRemoved` pins it.

### Changed API

All of it is unreferenced, and each change removes a Scala type from a signature no Java caller could use naturally:

| Member | Reference | Port |
| --- | --- | --- |
| `map()` | `scala.collection.mutable.Map` | `java.util.Map` |
| `add`, `remove` | returned `scala.Option` (the displaced entry) | `void` |
| `apply(BlockCoord)` | `scala.Option<FlaggedTile>` | `FlaggedTile`, null when absent |

`findTile` and `clear` are unchanged. No internal caller used any of the discarded return values: `TileMultipart` calls
`add`/`remove`/`findTile` and `MultipartEventHandler` calls `clear`.

### Removed API

- `TileCache$` and `TileCache$FlaggedTile$`, the two companion singletons. Nothing references either in bytecode or by
  name, which is the `IconHitEffects$` case rather than the `MultipartHelper$` one.
- `FlaggedTile`'s case-class machinery: `copy`, `copy$default$1`, `copy$default$2`, `canEqual`, `equals`, `hashCode`,
  `toString`, `productPrefix`, `productArity`, `productElement`, `productIterator`, and the `scala.Product` and
  `scala.Serializable` interfaces. It is only ever a map *value*, never compared, hashed, printed or destructured, so
  none of it was load-bearing. `t()` and `removed()` are kept under their generated names.

### Validation

- Complete plain-JVM suite: 114 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 13 tests, 0 failures, 0 errors, 3 of them new. `findTile` needs a world, so all
  three branches are covered there rather than headless.
- Class list and members diffed against a reference jar built at `b92d4b7`. Two classes removed, both companions.
- One characterization assertion changed with the port: the test's `entryAt` adapter stopped unwrapping a
  `scala.Option`. It was written as the single place the collection types are named precisely so this diff would be two
  lines. Every behavioral assertion is unchanged and passed before and after.
- What stays manual: the recovery path itself. It only fires when a client tile is evicted from its chunk before its
  update packet arrives, which no test reproduces.

## 2026-08-15 — PacketScheduler Java port

`PacketScheduler`, `IScheduledPacketPart` and `TScheduledPacketPart`. **Zero downstream references** and, unusually,
zero implementors in this codebase either: the whole subsystem is a third-party extension point that no shipping mod
in the pack uses.

### Observable behavior

No known divergence. `schedulePacket` still refuses a client world with `IllegalArgumentException`, still ORs repeated
masks for the same part, and `sendScheduled` still skips parts whose tile is null, still writes the mask at the part's
own width before calling `writeScheduled`, and still clears the schedule at the end.

It also still fails rather than writing nothing for a mask width outside 1, 2, 4 and 8. The reference matched with no
fallback and raised `scala.MatchError`; the port throws `IllegalArgumentException`. Both are `RuntimeException`, which
is what the characterization asserts, and there is no consumer to notice the type. The map is still **not** cleared
when that throw happens, because the reference had no `try`/`finally` and the port does not add one.

### Supported JVM API

- `IScheduledPacketPart` is member- and descriptor-identical to the reference.
- `PacketScheduler.schedulePacket(TMultiPart, long)` is unchanged.
- `TScheduledPacketPart` keeps `read` **abstract**. `TMultiPart` declares `read`, so a default would be shadowed by the
  superclass and a part would silently read a description instead of a mask.

### Changed API

`TScheduledPacketPart.writeScheduled` and `readScheduled` became interface defaults with empty bodies. They were
abstract on the reference interface with the empty bodies held in `TScheduledPacketPart$class`, which is to say a Scala
implementor already got them for free and only a Java implementor had to write them out. Making them defaults restores
the trait's source-level semantics for Java implementors rather than changing them. `TMultiPart` declares neither, so
the default-versus-superclass rule permits it.

Added `TScheduledPacketPart.readMask(TScheduledPacketPart, MCDataInput)`, a Java 8 interface static carrying the mask
dispatch that `TScheduledPacketPart$class.read` used to hold. Because `read` cannot be a default, this is what makes
the interface worth mixing in: implementors write a one-line `read` that delegates to it. The javadoc carries the
snippet.

`sendScheduled` is public. The reference declared it `private[multipart]`, which reaches `codechicken.multipart.handler`
where the only caller lives; Java has no scope that spans a package and its siblings, so package-private would not
compile and public is the only option.

### Removed API

`PacketScheduler$` with its `MODULE$`, and `TScheduledPacketPart$class`. Neither is referenced by any jar in the pack,
in bytecode or by reflective name, so both fall under the `IconHitEffects$` case rather than the `MultipartHelper$`
one. The two `PacketScheduler$$anonfun$` closure classes go with them; the Java loop produces no replacement.

### Validation

- Complete plain-JVM suite: 118 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 18 tests, 0 failures, 0 errors, 5 of them new for this port.
- Class list and members diffed against a reference jar built at `3ad25f5`.
- Two characterization adjustments, both anticipated: the functional test's `sendScheduled` helper stopped naming
  `PacketScheduler$.MODULE$`, and the structural test stopped asserting that `writeScheduled` and `readScheduled` are
  abstract on `TScheduledPacketPart`, asserting the member set instead. Every behavioral assertion is unchanged.
- What stays manual: the client-world guard. `schedulePacket` throws only when `world.isRemote`, which neither harness
  can produce — the plain JVM cannot build a `World` and the dedicated server is never remote.

## 2026-08-15 — ControlKeyModifer and ControlKeyHandler Java port

The placement-modifier key: a server-side map of which players hold it, and the client key binding that keeps that map
in sync. **Zero downstream references**, and not among guidenh's reflective names.

The misspelling in `ControlKeyModifer` is the published name and is kept. The file was named `ControlKeyModifier.scala`
while the object inside it was `ControlKeyModifer`; only the file name is corrected. Renaming the type would be an API
change with no forcing reason, so it is left for a deliberate decision rather than smuggled into a port.

### Observable behavior

No known divergence. `isControlDown` still branches on whether the player's world is remote, still reports
`isClientPressing` on a client and the recorded map value on a server, and an unrecorded player still reads as not
holding. `ControlKeyHandler.tick` still fires only on a change, still checks the net handler before sending, and still
sets the client flag before writing packet 1.

The reference's map was `HashMap().withDefaultValue(false)`; the port uses a plain `HashMap` and treats an absent key
as false at the single read site, which is the only place the default was ever observable.

### Changed and removed API

| Member | Reference | Port |
| --- | --- | --- |
| `isControlDown(EntityPlayer)` | static | **unchanged**, the documented Java entry point |
| `isClientPressing()` | static | unchanged |
| `isClientPressing_$eq(boolean)` | static | `setClientPressing(boolean)` |
| `map()` | `scala.collection.mutable.Map<EntityPlayer, Object>` | `java.util.Map<EntityPlayer, Boolean>` |
| `playerControlValue(EntityPlayer)` | static, returned `ControlKeyValue` | removed |

Removed classes: `ControlKeyModifer$`, `ControlKeyModifer$ControlKeyValue` and `ControlKeyHandler$`. Nothing
references any of them in bytecode or by reflective name.

`ControlKeyValue` and `playerControlValue` existed only to give Scala `player.isControlDown` sugar through an implicit
conversion. They are meaningless from Java, and the reference already shipped `isControlDown(EntityPlayer)` explicitly
"for Java users". The one in-repo user, `MicroblockPlacement`, now calls that static directly.

### ControlKeyHandler is now an ordinary class with a singleton

The reference was a Scala `object` extending `KeyBinding`, so the instance the key registry and the event bus both
needed was `ControlKeyHandler$.MODULE$`, and `ControlKeyHandler` was a class of static forwarders — including
forwarders for twelve inherited `KeyBinding` members. The port makes `ControlKeyHandler` itself the `KeyBinding`
subclass with a single `INSTANCE`, and `MultipartProxy_clientImpl` passes `ControlKeyHandler.INSTANCE`.

Consequence to note: referencing `ControlKeyHandler` at all now loads `KeyBinding`, where before only the companion
did. Both are reached solely from the client proxy's `postInit`, so nothing changes in practice, and the class carries
`@SideOnly(Side.CLIENT)`.

### Validation

- Complete plain-JVM suite: 120 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 21 tests, 0 failures, 0 errors, 3 of them new. `isControlDown` needs a real
  player to pick its branch, so they run against a Forge `FakePlayer` on a server world, which takes the server branch.
- Class list and members diffed against a reference jar built from the pre-port `src/main`.
- No characterization assertion changed with the port.
- What stays manual, and it is most of the client half: the key binding appearing in the controls screen, the packet
  actually reaching the server on press and release, and `isControlDown` reporting the pressed state on a client world.
  No harness has a client, so `isClientPressing` is never exercised.

## 2026-08-15 — Package objects removed rather than ported

`codechicken.multipart.package` and `codechicken.microblock.package`. This entry records a **removal**, not a
conversion, because a conversion is impossible.

### Why they have no Java form

A Scala package object compiles to a class literally named `package`, plus its `package$` companion. `package` is a
Java keyword, so no Java source can declare or even name those types. The characterization test had to reach both of
them through `Class.forName`, which is the whole argument in one line.

That left three options: leave two six-line Scala files in the tree forever, invent a Java type to hold the member, or
remove the indirection. Each package object held exactly one member — `def logger = MultipartProxy.logger` and
`def logger = MicroblockProxy.logger` — so the third is the honest one. The alias existed only to let Scala code inside
the package write `logger` unqualified; Java has no such sugar, and the proxies already publish the value.

### Observable behavior

None changed. The functional characterization asserted before the removal that each alias returned the *very instance*
its proxy holds, so replacing `logger` with `MultipartProxy.logger` / `MicroblockProxy.logger` at the call sites is an
identity substitution. The eight call sites are `ConfigContent` (7) and `MultipartGenerator` (1); both classes are
descriptor-identical after the change, since only method bodies moved.

### Removed API

`codechicken/multipart/package`, `codechicken/multipart/package$`, `codechicken/microblock/package` and
`codechicken/microblock/package$`. Exactly those four classes leave the jar and nothing else changes. No jar in the
pack references any of them, in bytecode or by reflective name — no Java consumer *could* reference them by name, and
no Scala consumer did.

### Validation

- Complete plain-JVM suite: 120 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 22 tests, 0 failures, 0 errors.
- Class list diffed against a reference jar built from the pre-removal `src/main`: exactly four classes removed.
- Test churn, both expected for a removal rather than a port: `PackageObjectCharacterizationTest` was deleted with its
  subject, and `ProxyLoggerFunctionalTest` lost only its alias-identity assertion. What survives is the assertion worth
  keeping — that both proxies publish a non-null logger during preInit, which is what the inlined call sites now read.

## 2026-08-15 — MultipartRenderer Java port

The client rendering callbacks. No jar links against any member, but guidenh reflects on **both** `MultipartRenderer`
and `MultipartRenderer$` by name, resolving `renderWorldBlock` as a static first and falling back to the companion's
`MODULE$`. Both classes are therefore kept, in the `TickScheduler$` shape: the companion holds the implementation
because Forge registers that instance itself, and the plain class is static forwarders.

### Observable behavior

No known divergence. The render id is still claimed in the singleton's initialiser, which preserves the reference's
timing: a Scala object runs its body on first access, and the first access is the client proxy's `postInit`.
`renderWorldBlock` still returns false for a non-multipart tile, for an empty part list, and after drawing a breaking
overlay; `renderTileEntityAt` still returns early unless the tile has dynamic parts; `getRenderId` still reads
`TileMultipart.renderID`.

The reference wrote the dynamic-part guard with a non-short-circuiting `|`. The port uses `||`. Both operands are
side-effect-free getters, so this cannot differ.

The breaking-overlay branch moved into a private helper. The reference nested a pattern match inside an `if`; the hit
data is a `scala.Tuple2` that arrives erased, so the port checks the tuple and then the boxed index before unboxing,
which is what the match compiled to. `BlockMultipart` already reads the same data the same way.

### Supported JVM API

The emitted class list is **unchanged** — nothing added, nothing removed. All five rendering callbacks keep their
exact signatures on `MultipartRenderer`, `MODULE$` keeps its type and flags on `MultipartRenderer$`, and both classes
keep class-level `@SideOnly(Side.CLIENT)`, so both are still stripped on a dedicated server.

`MultipartRenderer$` still extends `TileEntitySpecialRenderer` and implements `ISimpleBlockRenderingHandler`. Forge
rejects the registration outright if either is missing, and the characterization asserts both.

### Differences that are not API

- `MultipartRenderer` loses `func_147498_b`, `func_147496_a` and `func_147497_a`, the forwarders Scala generates for
  inherited `TileEntitySpecialRenderer` members. Same category as the twelve `KeyBinding` forwarders dropped from
  `ControlKeyHandler`: artifacts of the object-forwarder mechanism, not API, and unreferenced.
- `MultipartRenderer$`'s class initialiser loses `ACC_PUBLIC`, as every converted companion has.
- The unused `com.gtnewhorizons.angelica.api.ThreadSafeISBRH` import is dropped. It appears nowhere else in the
  codebase and annotated nothing; no behavior depended on it. Worth a second look independently — if the intent was to
  mark this ISBRH thread-safe for Angelica, that annotation was never actually applied in the reference either.

### Scala's uniform access hid a field-versus-method difference

`renderer.hasOverrideBlockTexture` reads identically in Scala whether it is a field or a no-arg method. It is a method
on `RenderBlocks`, and the port did not compile until it became `hasOverrideBlockTexture()`. This is a compile-time
failure rather than a silent one, but it will recur in every remaining renderer conversion.

### Validation

- Complete plain-JVM suite: 125 tests, 0 failures, 0 errors.
- Clean `spotlessApply checkstyleTest build`: passing.
- Java 8 Forge dedicated-server suite: 22 tests, 0 failures, 0 errors. It does not exercise this class and cannot:
  the server never loads it.
- Class list and members diffed against a reference jar built from the pre-port `src/main`.
- No characterization assertion changed with the port.
- What stays manual, and it is everything this class actually does: static block rendering, dynamic part rendering,
  the breaking overlay on the struck part, inventory rendering, and the render id being claimed exactly once. None of
  it can run without a client, so all of it is on the checklist.

## 2026-08-28 — IRedstonePart and RedstoneInteractions Java port

The complete source unit: six abstract interfaces plus the routing singleton. ProjectRed, ProjectBlue, WR-CBE,
Extra Utilities, OpenComputers and AE2 consume this surface; ProjectRed also reads `RedstoneInteractions$.MODULE$`
directly, so the companion cannot be collapsed into a static utility class.

### Observable behavior

No known divergence. Plain, face, and custom-mask parts retain the same precedence and bit masks. Connector tiles
still win over connector blocks, which still win over vanilla handling. Neighbor lookups retain the same coordinate
offset and opposite-side transform. Full-connect blocks, wire/comparator visual masks, repeater orientation, the
redstone-wire metadata fallback, and `Block.canConnectRedstone` retain their existing branches. Java uses
`Objects.equals` where Scala used null-safe `==`.

`vanillaSideMap`, `sideVanillaMap`, and `fullVanillaBlocks` remain singleton-owned objects returned by identity from
both published classes. `fullVanillaBlocks` deliberately remains a `scala.collection.immutable.Set<Block>` so its
descriptor and existing Scala consumers do not change.

### Supported JVM API

The emitted class list is unchanged: the six interface classes, `RedstoneInteractions`, and
`RedstoneInteractions$`. Every public member and descriptor is unchanged. `RedstoneInteractions` remains the static
façade and `RedstoneInteractions$.MODULE$` remains the implementation instance. The Java façade necessarily has a
private constructor where Scala's static-forwarder class emitted none; no consumer references constructors on either
class.

The six Scala traits contained only abstract members, so they emitted no `$class` helpers and need no compatibility
bridges. Their Scala signatures disappear, but inheritance and JVM method descriptors are identical.

### Performance boundary

The fresh Scala baseline measured 80.4 B per generated redstone three-query iteration; the Java result measured
80.5 B. Post-port JFR still attributes the work to Scala `List.foreach`, `Iterator.foreach`, `PartMap.edgeBetween`, and
the generated strong-power closure in `scalatraits/TRedstoneTile.scala`. This port therefore claims no performance
gain. Removing that allocation requires the later `registerJavaTrait` conversion and is explicitly deferred.

### Validation

- `RedstoneInteractionsCharacterizationTest`: 6 tests, covering every interface shape and pure routing branch.
- Complete plain-JVM suite: 139 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 31 tests, 0 failures, 0 errors; three tests cover initialized vanilla state,
  actual world routing, and a generated redstone tile.
- Clean `spotlessApply checkstyleTest build`: passing.
- Class list and public members diffed against the pre-port build; no characterization assertion changed.

## 2026-08-28 — MicroRecipe Java port

The complete recipe singleton moved together. Extra Utilities consumes this area through NEI recipe enumeration, and
the audited binary corpus calls `create`, `findMaterial`, and `getCraftingResult`; the port preserves the larger
published surface as well.

### Observable behavior

No known recipe divergence. Hollow construction, gluing, vertical thinning, horizontal splitting, and hollow filling
retain their exact slot shapes and early rejection rules. A ring of eight size-one covers is valid both as a hollow
recipe and as gluing back to a full block; `getCraftingResult` still chooses the hollow result first. Gluing retains
the separate cover/hollow, edge, and corner rules, and splitting keeps the `0 -> 3`, `1 -> 3`, `3 -> 2` map.

Saw discovery remains row-major. Cutting still accepts either sufficient strength or exactly the registry's maximum
saw strength. Full-size results still copy the material's source stack, while smaller results retain the material name
in an `ItemMicroPart`. `findMaterial` still returns the first exact item, damage, and NBT match. This means the missing
material placeholder's vanilla-stone stack can shadow a later stone entry; the Forge tests deliberately use glass when
they need an unambiguous raw-material round trip.

Java uses `Objects.equals` where Scala used null-safe `==`. Internal thinning and splitting locate the saw by slot
number to avoid constructing the published tuple for private work; direct `getSaw` calls still return the same boxed
`scala.Tuple3<Saw, Object, Object>`.

### Supported JVM API

`MicroRecipe` retains all 17 public static methods with their exact descriptors. `MicroRecipe$` still implements
`IRecipe`, exposes `MODULE$`, carries the implementation methods, and publishes the same
`scala.collection.immutable.Map<Object, Object>` by identity. The Java façade necessarily has a private constructor,
as with the other converted Scala forwarder classes.

The 12 private `MicroRecipe$$anonfun$...` compiler classes disappear; no consumer references them. The in-repo Scala
proxy now registers `MicroRecipe$.MODULE$` explicitly because deleting the Scala object removes the compiler symbol
that allowed `MicroRecipe` to be used as a value. Existing bytecode remains compatible through the retained companion.

### Performance boundary

Ordinary loops remove the range, filter, collection-search, captured-ref, and `NonLocalReturnControl` machinery from
recipe matching. No recipe workload was added to the focused tick/redstone profiler, so this is a structural result,
not a numeric performance claim. The public `getSaw` tuple allocation remains compatibility-required; internal recipe
paths no longer pay for it.

### Validation

- `MicroRecipeBinaryCompatibilityTest`: 2 tests covering the complete façade/companion ABI, `MODULE$`, and split map.
- `MicroRecipeFunctionalTest`: 6 Forge tests covering all recipe forms, precedence, class mappings, saw order,
  material lookup, NBT sensitivity, metadata, creation, and rejection paths.
- Complete plain-JVM suite: 141 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 37 tests, 0 failures, 0 errors.
- Class list and public members compared with the pre-port build; no characterization assertion changed.

## 2026-08-28 — TPartialOcclusionTile Java-trait pilot

This is the first built-in generated tile implementation whose source moved directly to Java. It was already a
concrete Scala class, so the existing registration path treated it as a Java trait and rewrote it at class-load time;
the port changes the compiler input without changing that generator.

### Observable behavior

No known divergence. A partial-occlusion candidate is still appended to the existing Scala `Seq` and tested before
the normal superclass pairwise chain. Failure still short-circuits that chain. Non-partial candidates still go directly
to the superclass. `partialOcclusionTest` still fills only `JPartialOcclusion` entries at their original indices and
retains `PartialOcclusionTest`'s exclusive-voxel and complete-occlusion rules.

Java uses a Scala `Seq` builder for the appended candidate because Scala 2.11's `Seq.:+` builder signature is not
expressible type-safely from javac with its wildcarded `CanBuildFrom`. The resulting call still dispatches through the
public `partialOcclusionTest(Seq)` method, preserving subclass/generated override behavior.

### Supported JVM and generated APIs

The direct compiled class still extends `TileMultipart`, has no fields or interfaces, one public no-arg constructor,
and exactly the same two public method descriptors. No anonymous or helper class is emitted. There were no direct
downstream references to this class in the audited mod corpus; consumers reach the behavior through
`JPartialOcclusion` parts and generated tiles.

At Forge class-load time the same name is still rewritten to a public interface with no fields and exactly three
methods: `occlusionTest`, `partialOcclusionTest`, and the generated
`codechicken$multipart$scalatraits$TPartialOcclusionTile$$super$occlusionTest` accessor. Generated composite instances
still implement it, and equal trait sets still reuse the same generated class.

The Scala source signature disappears from the untransformed class. No supported consumer used it, no `$class` helper
existed in the input artifact, and the runtime generator continues to create the implementation helper it requires.

### Scope boundary

This pilot has no fields or lifecycle callbacks. It therefore does not validate field-to-accessor rewriting,
constructor initialization injection, setter rebinding, copying, or part add/remove lifecycle order. Those are not
assumed safe from this result; `TSlottedTile` is the explicit stateful checkpoint before broader trait conversion.

### Validation

- `TPartialOcclusionTileCharacterizationTest`: 4 tests covering exact direct shape, partial/normal precedence,
  exclusive visibility, complete-occlusion exemption, and both short-circuit paths.
- Complete plain-JVM suite: 145 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 37 tests, 0 failures, 0 errors.
- Runtime reflection verifies the exact generated interface methods, no fields, override behavior, and class reuse.

## 2026-08-28 — TSlottedTile Java-trait port

This is the stateful `registerJavaTrait` checkpoint. OpenComputers casts generated tiles to this type, retrieves the
public `v_partMap()` array, mutates matching entries in place, and calls `bindPart` to rebuild them. The frozen binary
inventory contains that exact getter descriptor and no reference to `TSlottedTile$class`.

### Observable behavior

No known runtime divergence. Every generated tile receives its own 27-entry array. `copyFrom` still calls its super
implementation first and then shares the source array by identity only when the source is slotted. `partMap` preserves
normal array indexing. `clearParts` still clears both the superclass part list and every entry in the current array.

`partRemoved` still calls super first, acts only on `TSlottedPart`, scans exactly 27 slots, and uses Scala-style
null-safe value equality; Java uses `Objects.equals` to preserve the last detail. `canAddPart` still rejects the first
occupied masked slot before delegating to the base tile, while non-slotted and conflict-free parts delegate normally.
`bindPart` still calls super first and fills each of the 27 positive masked slots. OpenComputers' external mutation and
rebind pattern is exercised directly.

### Generated ABI and raw input divergence

At Forge runtime `TSlottedTile` remains a public interface with no fields and the exact same 13 abstract methods: six
behavior methods, `v_partMap` getter/setter, and five super accessors. The generated composite still owns a
`TMultiPart[] v_partMap` field, runs the 27-entry initializer for every instance, and reuses its class for equal trait
sets. All 27 shipping consumer jars therefore retain their binary linkage.

The untransformed artifact necessarily changes shape. Scala emitted an interface, a static `$class` implementation
helper, and four `$$anonfun$` range classes. Java emits one concrete `TileMultipart` subclass with a public array field,
one no-arg constructor, and six overrides; `registerJavaTrait` creates the interface/helper at class-load time. No
shipping consumer references the removed raw helper or closure classes.

This produces an accepted source-build limitation: code recompiled directly against the raw dev jar sees a class and
public field, so it may emit `invokevirtual` or `getfield`; Forge later rewrites that owner to an interface, for which
those opcodes are invalid. Existing binaries such as OpenComputers use `invokeinterface` and remain valid. A future
downstream-source release needs either a transformed compile stub/API artifact or explicit avoidance of direct
generated-trait calls; runtime binary compatibility does not by itself solve that development artifact problem.

### Performance boundary

Ordinary loops remove the four Scala `Range.foreach` closure classes and the `NonLocalReturnControl` used for occupied
slot rejection. The retained focused profiler does not perform slotted placement, so this is a structural removal, not
a numeric performance claim. No new dependency or generator change was needed.

### Validation

- `TSlottedTileFunctionalTest`: 6 Forge tests covering copy identity, non-slotted copy, external mutation/rebinding,
  bind/remove/clear semantics, value equality, and occupied/free/non-slotted placement.
- `ForgeEnvironmentSmokeTest` verifies the exact 13-method runtime interface, generated field, distinct initialized
  arrays, setter rebinding, and class-cache identity.
- Existing live-world lifecycle tests continue to cover add/remove slot caches and relocation of the generated tile.
- Complete plain-JVM suite: 145 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 43 tests, 0 failures, 0 errors.

## 2026-08-28 — TRedstoneTile Java-trait port

ProjectRed directly casts a generated tile to `TRedstoneTile` and calls `openConnections(int)`. Extra Utilities casts
the same type and calls the inherited one-argument `weakPowerLevel(int)`. Neither shipping consumer references
`TRedstoneTile$class`; UtilitiesInExcess has no direct reference in its current source.

### Observable behavior

No known runtime divergence. Strong power still scans only `IRedstonePart` instances, ignores connection masks, and
returns the greatest positive level from a zero baseline. Face and edge parts retain the same center/rotation
conduction rules. Connection masks are still intersected with the open face/edge mask before being combined, while
weak power calls only connected redstone parts and takes their maximum from zero.

The one-argument weak-power method still intersects the tile's mask with the adjacent connector's power mask.
`canConnectRedstone` still applies the legacy vanilla-to-multipart side mapping and uses the neighbor's non-power mask.
Normal immutable Scala `List` storage is traversed without allocation; an arbitrary `Seq` supplied through the public
setter retains an iterator fallback.

### Generated ABI and raw input divergence

At Forge runtime `TRedstoneTile` remains a public interface extending exactly `IRedstoneTile`, with no fields and the
same eight abstract methods: `strongPowerLevel(int)`, both `weakPowerLevel` overloads, `canConnectRedstone(int)`,
`openConnections(int)`, `getConnectionMask(int)`, `redstoneConductionF(int)`, and `redstoneConductionE(int)`.
Generated composites still implement that interface, and equal trait sets still reuse the same generated class.

The untransformed artifact changes from the Scala interface, `$class` helper, and four `$$anonfun$` classes to a
concrete Java `TileMultipart` subclass plus package-private `TRedstoneTileAccess`. The shim is required because the
current Java-trait transformer treats direct inherited field reads as trait fields and emits an invalid cast for
inherited virtual calls. It performs coordinate, `partList`, and virtual `partMap` access outside the transformed
class. It adds no public member and does not change the generator.

As with `TSlottedTile`, this preserves existing consumer binaries but not recompilation directly against the raw dev
jar: Java source presents the mixin input as a class, while Forge rewrites it to an interface at runtime. A transformed
compile stub or downstream source guidance is still required before claiming source-compatible rebuilds.

### Performance result

The paired 50,000,000-iteration Forge/JFR run fell from 4,023,855,000 allocated bytes / 80.5 B per three-query
iteration to zero measured bytes / 0.0 B. Elapsed time fell from 7.534 s to 6.261 s, raising throughput from 6,636,424
to 7,986,213 iterations/s (20.3%). Both runs produced checksum `3315999992`.

### Validation

- `TRedstoneTileFunctionalTest`: 3 Forge tests covering the exact eight-method interface and class cache, face/edge
  conduction, strong and weak maxima, mask filtering, non-redstone parts, arbitrary `Seq` input, neighbor masks, and
  vanilla-side translation.
- Complete plain-JVM suite: 145 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 46 tests, 0 failures, 0 errors.
- Clean formatting, checkstyle, build, generated runtime reflection, and paired profile checksum.

## 2026-08-28 — multipart read-path cleanup

### Observable behavior and ABI

No known runtime divergence. Focused tests pin max-or-zero lighting, torch support, live reads through a mutable `Seq`,
direct ordered indexing, and `BlockMultipart.getTile`'s non-multipart/empty/non-empty filtering. Internal code now reads
the published `Seq` directly. The public `partList`, `partList_$eq`, and `jPartList` descriptors and live-view behavior
are unchanged; add/remove still snapshot before publishing a replacement immutable `Seq`.

### Performance result

The paired 50,000,000-iteration Forge/JFR run reduced `getLightValue` from 9,196,067,864 allocated bytes / 183.9 B per
call to zero and raised throughput 11.42x. `BlockMultipart.getTile` fell from 1,200,000,000 bytes / 24.0 B per call to
zero and rose 2.89x. Both runs used the same eight-part tile and produced the same checksum.

### Validation

- Complete plain-JVM suite: 147 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 46 tests, 0 failures, 0 errors.
- The public Java list bridge remains for downstream consumers; there are no internal `jPartList()` callers.

## 2026-08-28 — TTileChangeTile Java-trait port

The first Java trait carrying both mutable state and inherited-member access. Nothing in the frozen consumer baseline
references `TTileChangeTile` or a `$class` helper for it; the load-bearing surface is `INeighborTileChange`, which is
already Java and byte-for-byte unchanged across the port. ProjRed and Extra Utilities implement that interface on their
parts and never touch the tile trait.

### Observable behavior

No known runtime divergence. `bindPart` still calls super first and only ever ors the flag in, so a later part that
does not want weak changes cannot clear it. `copyFrom` still takes the flag from another change tile and leaves it
untouched for a plain source. `clearParts` still resets both the superclass list and the flag. `partRemoved` still
recomputes from the remaining published list rather than from the part it is handed, and still short-circuits on the
first weak part, matching `exists`.

`onNeighborTileChange` keeps the same coordinate filter: it calls super first, computes the offset from the tile's own
coordinates, and returns unless the offset is axial with an absolute sum of one or two. Two blocks is still reported as
weak. Dispatch still goes through `operate`, so the null-tile guard and any trait overriding `operate` still apply.

### Generated ABI and raw input divergence

At Forge runtime `TTileChangeTile` remains a public interface with no fields and the exact same 13 methods: six
behavior methods, the `weakTileChanges` getter and setter, and five super accessors. The generated composite owns a
`boolean weakTileChanges` field, and state stays per tile. `ForgeEnvironmentSmokeTest` freezes that shape.

The untransformed artifact changes shape, as with every Java-trait port. Scala emitted an interface, a `$class` helper,
an `$$anon$1` for the operate callback, and an `$$anonfun$partRemoved$1` for the `exists` scan. Java emits one concrete
`TileMultipart` subclass, plus the `TTileChangeTileAccess` helper and its callback class. No shipping jar references
any of the removed classes. The recompile-against-the-raw-dev-jar limitation recorded for `TSlottedTile` applies here
unchanged.

### Two transformer constraints found by this port

**A Java mixin trait may not carry an inner class.** `registerJavaTrait` rejects a non-empty `InnerClasses` attribute
outright, so the anonymous `AbstractFunction1` for the `operate` callback could not live in the trait. It moved to
`TTileChangeTileAccess` as a named class, which the transformer does not process. Any future Java trait needing a
callback, a lambda or a switch on strings has the same constraint.

**An access shim must not name a compile-time supertype of the trait.** The first attempt typed the shim parameters as
`TTileChangeTile` and cast to `TileMultipart` inside. Because the untransformed trait extends `TileMultipart`, javac
elided the cast entirely, and once Forge rewrote the trait to an interface the verifier rejected the resulting
`getfield` with "Type TTileChangeTile is not assignable to TileMultipart". `TRedstoneTileAccess` avoids this only by
accident: `IRedstoneTile` is unrelated to `TileMultipart`, so javac was forced to emit a real `checkcast`. The general
fix, used here, is to type the shim parameters as `Object` so the cast is always emitted.

### Validation

- `TTileChangeTileFunctionalTest`: 7 Forge tests covering the flag lifecycle across bindPart, copyFrom, clearParts and
  partRemoved, plus the coordinate filter and the per-part dispatch filter.
- `ForgeEnvironmentSmokeTest.generatesAndCachesTileChangeTileClass`: interface shape, field type, per-tile state and
  generated-class reuse.
- All eight were written and passed against the untouched Scala trait first, and pass unchanged after the port.
- Complete plain-JVM suite: 153 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 54 tests, 0 failures, 0 errors.
- `INeighborTileChange` descriptors identical between the reference and ported dev jars.

## 2026-08-30 — TFluidHandlerTile Java-trait port

No frozen binary or audited source consumer names `TFluidHandlerTile` or its `$class` helper. The load-bearing surface
is Forge's `IFluidHandler`, which generated multipart tiles still implement with the same six fluid method
descriptors.

### Observable behavior

No known runtime divergence. Every generated tile receives a distinct `LinkedList`; `copyFrom` still shares that list
by identity only for another fluid tile. Fluid parts are appended in binding order, removal deletes the first equal
entry, and clear empties both the superclass part list and the existing tank list.

`getTankInfo` still visits every handler twice, first to size the result and then to flatten each returned array in
part order. Fill still passes a fresh copy with the decreasing remaining amount to every handler, including handlers
reached after the full quantity has been accepted. `canFill` and `canDrain` still stop at the first matching handler.

Both drain overloads still simulate every handler first, accept the first positive fluid and only later matching
fluids, and commit only accepted simulations when requested. The first accepted simulated stack remains the returned
object, with its amount replaced by the total. The stack overload still passes decreasing copies and leaves the
caller's stack unchanged.

### Generated ABI and raw input divergence

At Forge runtime `TFluidHandlerTile` remains a public interface extending exactly `IFluidHandler`, with no fields and
the same 16 abstract methods: ten behavior methods, the `tankList` getter and setter, and four super accessors. The
generated composite still owns and initializes a `LinkedList tankList` field, permits setter rebinding, and reuses its
class for equal trait sets.

The untransformed artifact changes from a Scala interface, `$class` helper, and eight `$$anonfun$` classes to one
concrete Java `TileMultipart` subclass plus package-private `TFluidHandlerTileAccess`. No shipping consumer references
the removed classes. The raw-dev-jar source-build limitation recorded for `TSlottedTile` applies unchanged.

### Transformer constraint

The Java-trait transformer rewrites every `GETFIELD` instruction as access to trait-owned state without checking the
instruction's owner. The first port therefore failed registration on the public `FluidStack.amount` field with
`key not found: amount`. `TFluidHandlerTileAccess` now owns those reads, writes, and amount-adjusted copies outside the
transformed class. It adds no public surface and requires no generator change.

### Performance boundary

Ordinary loops remove the eight Scala closure classes, but no representative workload in the retained profiler uses
multipart fluid distribution. This is a structural removal, not a throughput or allocation claim.

### Validation

- `TFluidHandlerTileFunctionalTest`: 7 Forge tests covering list lifecycle and identity, tank-info order, fill
  distribution, capability short-circuiting, fluid matching, simulation, commit behavior, and both drain overloads.
- `ForgeEnvironmentSmokeTest.generatesAndCachesFluidHandlerTileClass`: exact 16-method runtime interface, parent
  interface, generated field, distinct initialization, setter rebinding, and generated-class reuse.
- All eight tests passed against the untouched Scala trait first and pass unchanged after the port.
- Complete plain-JVM suite: 153 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 62 tests, 0 failures, 0 errors.
- The frozen consumer inventory and audited source consumers contain no `TFluidHandlerTile` reference.

## 2026-08-30 — TIInventoryTile/JInventoryTile Java-trait port

AE2 is the load-bearing consumer for this port. `CableBusPart.notifyNeighbors` checks a generated tile with
`instanceof TIInventoryTile` and directly invokes `rebuildSlotMap()`. The registered trait input is separately named
`JInventoryTile`; preserving only one of those types would break either registration or AE2 linkage.

### Observable behavior

No known runtime divergence. Each generated tile starts with distinct empty inventory-list and slot-map instances.
Binding appends only `IInventory` parts in order, removal deletes the matching inventory, and clear empties both the
superclass part list and inventory state. Copying from another inventory tile shares its inventory list by identity and
rebuilds a distinct slot map; copying from a plain tile leaves both current references untouched.

The slot map still flattens every inventory in list order and routes each global slot to its original inventory and
local index. The fixed name, custom-name flag, stack limit, usability and no-op open/close behavior are unchanged.
Sided access still exposes only `ISidedInventory` slots, while its global base advances across every inventory;
insert/extract checks still delegate to sided inventories and return true for ordinary inventories.

### Generated ABI and raw input divergence

At Forge runtime `TIInventoryTile` remains a public interface extending exactly `ISidedInventory`, with no fields and
the exact same 28 methods: 20 behavior/inventory methods, four public state accessors, and four super accessors.
`JInventoryTile` remains a public child interface with no fields and the exact same 36 methods, adding four private-
field accessors and four `JInventoryTile` super bridges. The generated composite still owns the two private fields
`codechicken$multipart$scalatraits$JInventoryTile$$invList` and
`codechicken$multipart$scalatraits$JInventoryTile$$slotMap`, with `LinkedList` and `scala.Tuple2[]` descriptors.

The raw dev artifact now contains a Java interface `TIInventoryTile`, a concrete Java `JInventoryTile` mixin input,
and package-private `JInventoryTileAccess` instead of the Scala interface, `$class` helper and concrete forwarding
class. No frozen consumer references `TIInventoryTile$class` or names `JInventoryTile`; AE2's load-bearing
`TIInventoryTile.rebuildSlotMap()` descriptor is unchanged. As with the other Java-trait ports, consumers compiling
against the untransformed dev jar should use a transformed compile stub when they name the registered mixin type.

### Transformer constraint

The Java-trait stack analyzer has no case for JVM opcode `NEWARRAY` (`188`). Creating the primitive result array in
`getAccessibleSlotsFromSide` initially failed trait registration with `scala.MatchError: 188`. The method now collects
the same ordered boxed slots and delegates only the final `int[]` allocation/copy to package-private
`JInventoryTileAccess`. Reference-array allocation remains inside the trait because its `ANEWARRAY` opcode is already
supported. The helper adds no public surface.

### Validation

- `TIInventoryTileFunctionalTest`: 7 Forge tests covering list/map lifecycle and identity, flattened routing, the
  direct AE2-shaped rebuild call, fixed metadata, sided global offsets, and insert/extract delegation.
- `ForgeEnvironmentSmokeTest.generatesAndCachesInventoryTileClassWithBothPublicTraitInterfaces`: exact 28- and
  36-method runtime interfaces, inheritance, generated private fields, distinct initialization, setter rebinding, and
  generated-class reuse.
- All eight tests passed against the untouched Scala implementation first and pass unchanged after the port.
- Complete plain-JVM suite: 153 tests, 0 failures, 0 errors.
- Java 8 Forge dedicated-server suite: 70 tests, 0 failures, 0 errors.
- Raw `javap` descriptors for both public types match the reference, including `scala.Tuple2[]` state accessors.
