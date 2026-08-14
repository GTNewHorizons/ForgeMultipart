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
