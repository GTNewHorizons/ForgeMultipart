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
