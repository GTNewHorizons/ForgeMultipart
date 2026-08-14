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
