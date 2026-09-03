# Migration history

Historical findings from the Scala-to-Java migration. Counts, next-target suggestions and outstanding work below
describe the state at the time of each entry; use the [working handoff](../../JAVA_MIGRATION_HANDOFF.md) for current
state and the [plan](../../JAVA_MIGRATION.md) for remaining gates. Routine sessions need not read this entire log.

The former detailed per-port handoff is also available with `git show cf8b2f9:JAVA_MIGRATION_HANDOFF.md`.
Keep new findings here; summarize only current state and constraints in the handoff. Intentional compatibility
differences belong in the [divergence ledger](../../JAVA_MIGRATION_DIVERGENCES.md).

### 2026-08-14

- Confirmed that the codebase is feasible to migrate incrementally to Java.
- Confirmed that generalized Scala `try/catch` overhead is not present; only a small set of exception-based non-local returns is relevant.
- Identified closure/boxing/collection allocations as stronger performance candidates.
- Confirmed that the existing runtime compiler already has a Java trait path.
- Confirmed that public bytecode exposes Scala types and compiler artifacts, so bridges and Scala runtime removal must be separate decisions.
- Confirmed Hodgepodge's normal/early/late UniMixins plus GTNHMixins registration pattern as the reference for fixed mixins.
- Confirmed that UniMixins is not a drop-in replacement for ForgeMultipart's runtime composite-tile generator.
- Confirmed GTNHLib as a viable route to fastutil, subject to pack-aligned versioning and runtime-resolution verification.
- Adopted characterization tests against the existing Scala implementation as a prerequisite for each converted area, backed by golden ABI/data fixtures and real Forge integration scenarios where plain JVM tests are insufficient.
- Bootstrapped JUnit Jupiter and passed an eight-case `IDWriter` characterization pilot against the untouched Scala implementation, covering byte/short/int thresholds and exact encoded bytes.
- The pilot found that negative `IDWriter.setMax` values select the byte carrier because Scala 2.11 treats `0xffffffff` as signed `-1`; current registry callers supply non-negative collection sizes, so this is recorded as a dormant edge case and possible deliberate divergence rather than changed during characterization.
- Added a dedicated `functionalTest` source set and `runFunctionalTestServer` task. The task starts a real Java 8 Forge dedicated server, loads a test-only FML mod, runs JUnit after `FMLServerStartedEvent`, writes legacy JUnit XML, and shuts the server down.
- Passed the first two Forge integration checks against the untouched Scala implementation: complete server lifecycle/world startup with all three ForgeMultipart mod IDs loaded, and generation plus constructor-class caching of a `TSlottedTile` composite with its 27-slot map.
- Hardened the Gradle wrapper to delete stale reports and fail when no fresh report is produced, no tests run, or any failure/error is reported. This is necessary because Forge 1.7.10 can terminate after a test-mod exception with process exit code 0.
- Confirmed the Java 8 server lane on Azul OpenJDK 8u492. The current deobfuscated modern-server tasks are not usable as release evidence: Java 21 needs the legacy Security Manager switch and then `Lwjgl3ifyRelauncherTweaker` rejects the server launch; Java 25 rejects `System.setSecurityManager` before Forge loads. Modern-Java validation therefore remains a packaged-server gate rather than a custom test-runner workaround.
- Added eight plain-JVM characterization cases for `NormalOcclusionTest` and `NormallyOccludedPart`: empty sets, separation on every axis, face contact, tolerance-sized overlap, real overlap, containment, identical boxes, multiple-box all-pairs behavior, normal-box delegation, partial-box delegation, and parts without occlusion interfaces.
- Confirmed that normal occlusion returns `true` when parts may coexist, despite the source comment saying “true if the test fails.” Face contact and overlaps no larger than CodeChickenLib's `1e-5` intersection tolerance are accepted; larger overlaps reject placement. Preserve the behavior and correct the misleading documentation separately when this area is ported.
- Added nine plain-JVM characterization cases for `PartialOcclusionTest`: its fixed 8³ grid and x-major indexing, part-ID encoding, half-voxel coordinate rounding, required visibility, complete-occlusion exemption, cross-part overlap, same-part box overlap, unfilled entries, and the public `JPartialOcclusion` overload.
- Confirmed that every non-exempt part needs at least one exclusively owned voxel. Any second write to an occupied voxel permanently changes it to `-1`, including overlap between two boxes supplied by the same part; this can make that part fail the test. Preserve this exact dynamic-tile behavior during translation unless it is changed later as an explicitly documented bug fix.
- Completed the first behavior-preserving production conversion by replacing `IDWriter.scala` with `IDWriter.java`. All eight existing encoding cases, all 25 plain-JVM tests, the clean build, and both Java 8 Forge server checks pass unchanged.
- Preserved the four legacy Scala function accessor descriptors as deprecated binary bridges and added direct Java `write(MCDataOutput, int)` and `read(MCDataInput)` methods. Recompiled Scala registry callers use the new primitive methods because Scala property auto-application does not apply to accessors declared in Java.
- Removed the six `IDWriter$$anonfun$setMax$*` compiler artifacts from the packaged jar. Their disappearance and replacement with Java anonymous helper classes is recorded in `JAVA_MIGRATION_DIVERGENCES.md`; the supported `IDWriter` descriptors remain link-compatible.
- Added a frozen Scala 2.11.5 consumer compiled against the reference dev jar. Its constructor and inherited default method call `JPartialOcclusion$class` directly, so it detects the linkage failure that freshly recompiled tests would miss.
- Replaced `TPartialOcclusion.scala` with Java implementations of `PartialOcclusionTest` and `JPartialOcclusion`. The interface keeps its name and method descriptors, while `allowCompleteOcclusion()` is now a Java default method and the deprecated `$class` helper remains for old Scala binaries.
- Passed all ten partial-occlusion behavior/API cases, the frozen Scala binary consumer, all 27 plain-JVM tests, a clean build, and both Java 8 Forge server checks. The existing marker-interface registration remains unchanged and continues to drive runtime tile generation.
- Completed the downstream ABI inventory by constant-pool scan of 240 mod jars in GTNH daily `2026-08-14+678`, recorded in `JAVA_MIGRATION_ABI_INVENTORY.md` with the scanner in `tools/AbiScan.java` and the frozen baseline in `src/test/fixtures/abi/`. GitHub code search was rejected as an oracle because it indexes default branches only and cannot see reflection strings or closed-source consumers.
- Found 27 consumer jars referencing 35 inherited types, 255 exact member descriptors, 76 other types, and 20 reflective string constants.
- Answered open decision 2: third-party Scala traits are registered externally. ProjRed passes its own `LightMicroblock` Scala trait to `MicroblockGenerator.registerTrait`, so `registerScalaTrait` and ScalaSignature decoding must survive Phase 7.
- Answered open decision 4: Scala runtime removal is not achievable for the first Java release. ProjRed, OpenComputers, ProjectBlue, and ForgeRelocationFMP link against 16 static methods on 8 trait `$class` helpers plus 17 companion `MODULE$` singletons.
- Found only five referenced descriptors containing Scala types, and `TileMultipart.jPartList()` already outweighs `partList()` in downstream use. The Scala bridge surface is much smaller than the initial audit assumed.
- Found zero downstream references to `IDWriter`, so its four retained Scala function accessors are not load-bearing and can be dropped.
- Removed both speculative bridges the inventory proved dead: the four `IDWriter` Scala function accessors and the whole `JPartialOcclusion$class` helper, along with the `ReferenceScalaPartialOcclusion` fixture that only existed to verify the helper. `JPartialOcclusion` itself and both of its method descriptors are unchanged.
- `IDWriter` now selects a carrier width instead of storing Scala closures, removing the per-call `Integer` boxing and `Function1`/`Function2` allocation that the first port had preserved. The nine encoding cases still pass unchanged.
- Established the working rule for the remaining phases: check `JAVA_MIGRATION_ABI_INVENTORY.md` before writing a bridge, rather than writing one reflexively for every converted file.
- Ported `TCuboidPart`, `JCuboidPart` and `TCuboidPart$class` to Java, the first conversion of a trait whose `$class` helper is genuinely load-bearing. All reference descriptors are preserved, verified by diffing `javap -s` against the reference dev jar, and a frozen Scala 2.11.5 consumer whose forwarders call all four statics loads and runs against the port.
- Found the first structural limit of the migration: Scala trait linearization cannot be reproduced by a Java interface, because a superclass method always beats an interface default on the JVM. Recompiled Scala consumers that mix `TCuboidPart` into a `TMultiPart` subclass now silently get `TMultiPart`'s empty implementations. Binary compatibility is unaffected, and `CuboidPartCharacterizationTest` carries the regression guard, but this applies to every remaining trait that overrides `TMultiPart` members and should be assumed for `TFacePart`, `TNormalOcclusion`, `TIconHitEffects` and `TItemMultiPart` as well.
- Recorded the consequence for consumers: Scala code that recompiles must extend `JCuboidPart` or declare the overrides itself. This belongs in the release notes for the first Java release, not only in the divergence log.
- Found a build-order constraint: `compileJava` runs before `compileScala`, so ported Java that still references Scala types must live under `src/main/scala` for joint compilation, as `minecraft/McBlockPart.java` already does. Files can move to `src/main/java` once their Scala dependencies are ported.
- Established the fixture recipe for the remaining load-bearing bridges: build a reference dev jar at the pre-port commit, compile the consumer with Scala 2.11.5 under Java 8 with `-target:jvm-1.8` against that jar plus the project compile classpath, then store the class base64-encoded under `src/test/resources/compat/`.
- Ported the whole `TNormalOcclusion.scala` unit: `NormalOcclusionTest`, `NormalOcclusionTest$`, `JNormalOcclusion`, `TNormalOcclusion`, `TNormalOcclusion$class` and `NormallyOccludedPart`. The singleton and both trait interfaces are descriptor-identical to the reference; the other three add only private members.
- Found that Scala emits trait super accessors as `ACC_SYNTHETIC`, which `javac` hides completely. An abstract synthetic method cannot be implemented by any Java class or by Scala recompiled against a Java interface, so `codechicken$multipart$TNormalOcclusion$$super$occlusionTest` is emitted as a non-synthetic default returning `true`. Method resolution ignores the flag, so existing call sites are unaffected. Expect the same treatment wherever a remaining trait calls `super`.
- Found the dangerous shape of the linearization problem. `HollowMicroblock` simply lacked an override, which is easy to spot. `PostMicroblock` already had one ending in `super.occlusionTest(npart)`, which used to route through the trait and now reaches `Microblock` directly, silently dropping the box test while still compiling. Existing `super` call sites must be audited on every remaining trait conversion, not just missing overrides.
- Recorded a second source-only divergence: `NormalOcclusionTest` was a Scala `object`, so `NormalOcclusionTest(a, b)` apply sugar no longer compiles and recompiled Scala callers must write `NormalOcclusionTest.apply(a, b)`. Both static forwarders and the `MODULE$` singleton keep their descriptors, so binaries are unaffected.
- Kept `scala.collection.Traversable` in two descriptors, as the ABI inventory predicted. They are drained with `JavaConversions.asJavaIterator(boxes.toIterator())`, which is the one interop route callable from Java.
- Removed two more closure artifacts, `NormalOcclusionTest$$anonfun$apply$1` and its nested `$$anonfun$apply$2`, and corrected the scaladoc that claimed the test "returns true if the test fails".
- Ported `TFacePart`, the first trait where Java default methods are actually safe. `TMultiPart` declares neither `solid` nor `redstoneConductionMap`, so there is no superclass method for a default to lose to. The interface is descriptor-identical to the reference and no in-repo Scala needed changing.
- Adopted the resulting rule for the remaining traits: emit default methods only when no class in an implementor's superclass chain declares the same member, otherwise leave the member abstract and add explicit forwarders as `TCuboidPart` and `TNormalOcclusion` required.
- Made `TFacePart` inheritable from Java as a side effect. Both members were abstract on the reference interface, so `minecraft/McSidedMetaPart` had to declare them even to accept the defaults. That class stays descriptor-identical, so the change is purely additive.
- Ported the whole `TIconHitEffects.scala` unit: `IconHitEffects`, `JIconHitEffects`, `JIconHitEffects$class`, `TIconHitEffects` and `TIconHitEffects$class`. Both interfaces are descriptor-identical to the reference and the other three add only a private constructor.
- Confirmed the default-versus-abstract rule discriminates correctly within a single file. `JIconHitEffects.getBreakingIcon` became a default because `TMultiPart` does not declare it, while `TIconHitEffects.addHitEffects` and `addDestroyEffects` stayed abstract because it does. `MicroblockClient` needed explicit forwarders only for the latter pair.
- Removed `IconHitEffects$` and its `MODULE$` field. No jar in the pack references it, in bytecode or by name, so it is dead under the inventory rule. This is the first public class the migration has dropped rather than a method, and it contrasts with `NormalOcclusionTest$`, which is referenced and was kept.
- Preserved evaluation order inside `addDestroyEffects` rather than hoisting the tile read, because the characterization pins which work happens before a null tile fails.
- Established that particle appearance cannot be asserted headless. Icon selection, side order and bounds scaling are covered by tests; the visual result stays on the manual checklist.
- Ported `ItemMultiPart.scala` into `JItemMultiPart`, `TItemMultiPart` and `TItemMultiPart$class`. The interface is descriptor-identical to the reference, `JItemMultiPart` gains only additive statics, and the bridge keeps all three public statics for ProjRed.
- Generalised the default-versus-abstract rule. `TItemMultiPart` extends `Item` rather than `TMultiPart`, and the same split applied: `getHitDepth` became a default because `Item` does not declare it, while `onItemUse` stayed abstract because `Item` does. What matters is the implementor's actual superclass, not `TMultiPart` specifically.
- Confirmed that placement logic is testable headless. `onItemUse` short-circuits as soon as `newPart` returns null, before any world, player or stack access, so the depth branch, the neighbour offset and the shared mutated `BlockCoord` can all be asserted with null arguments.
- Added the first functional coverage of runtime class generation interacting with a ported type. Microblock classes are built by ASM at run time, so a plain-JVM test cannot show that a generated class resolves a Java interface default; the Forge server suite now generates a `FaceMicroblock` and asserts it inherits `redstoneConductionMap`, and generates a `HollowMicroblock` and asserts its own `0x10` and `false` still win.

### 2026-08-15

- Ported the six marker interfaces: `TSlottedPart`, `IRandomDisplayTick`, `INeighborTileChange`, `TRandomUpdateTick`, `ISidedHollowConnect` and `IMicroMaterialRender`. All six are member- and descriptor-identical to the reference, and the seven implementors and mixin tiles were diffed as well and are unchanged member for member.
- Confirmed that five of the six were already pure abstract interfaces in bytecode with no `$class` helper, so their conversion had no bytecode consequence at all. Exactly one class left the jar: `TRandomUpdateTick$class`.
- Applied the default-versus-abstract rule to `TRandomUpdateTick.onWorldJoin` and got the failing answer: `TMultiPart` declares `onWorldJoin`, so a default would be shadowed by the superclass and the auto-registration would silently never run. It stays abstract, and implementors declare it and call `TickScheduler.loadRandomTick` themselves, which `RedstoneTorchPart` — the only implementor in this codebase — already did.
- Applied the bridge rule to `TRandomUpdateTick$class` and got a clean negative: no jar in the pack references `TRandomUpdateTick` in any form, and the frozen baseline contains exactly the eight `$class` helpers the inventory lists. No bridge was written, following the `IDWriter` precedent. The accepted cost is a recompiled-Scala-consumer break with no shipping consumer.
- Found that `IMicroMaterialRender` is implemented for every part solely by `TMultiPart`'s Scala-style `world`/`x`/`y`/`z`/`getRenderBounds` accessors. Renaming any of them to a bean accessor would silently unimplement the interface, so the names are now pinned by a characterization test rather than left to reviewer attention.
- Established that for interfaces carrying no implementation, the characterization is the shape: every member abstract and public, no superinterface, and the exact member set. That is enough to catch the two realistic failure modes, a stray default and a renamed accessor.
- Noted that `IRedstonePart.scala` is misleadingly named and is not a marker-trait file. It carries six traits plus `RedstoneInteractions`, whose `MODULE$` is load-bearing, and was moved out of the low-risk group.
- Historical documentation gap: the ports between `TItemMultiPart` and this entry — `TEdgePart`, `Saw`, both
  registries, `TileMultipart`, `TMultiPart`, `TickScheduler` and `BlockMultipart` — did not receive dated findings
  here. Use the handoff for their current status and git history for the original validation narrative; the divergence
  ledger now records only their effective compatibility differences.
- Ported `MultipartHelper`, `MultipartHelper$` and `MultipartHelper$IPartTileConverter` to Java. All three are public-member- and descriptor-identical to the reference and the full emitted class list is unchanged, so this port neither added nor removed a class.
- Kept `MultipartHelper$` on the strength of a reflective string constant alone. It is not among the 17 `MODULE$` singletons read from bytecode and no jar links against it, but guidenh names it, and the inventory warns that removing a companion breaks such consumers invisibly. This is the opposite call to `IconHitEffects$`, which was dropped because nothing referenced it in bytecode or by name, and it establishes that the `MODULE$` list alone is not sufficient grounds to delete a companion.
- Found a reusable characterization technique: a class that cannot class-initialize headless is a probe for control flow. `MultipartSaveLoad` reflects into `TileEntity`'s static maps through `ObfMapping` and always throws under a plain JVM, so `createTileFromNBT` returning null rather than raising proves the id guard short-circuits before the `loadingWorld` assignment. Assert `LinkageError` rather than the exact type, because the first attempt raises `ExceptionInInitializerError` and later ones raise `NoClassDefFoundError`.
- Confirmed that erasure does not weaken `IPartTileConverter.convert`. The cast to `T` compiles away, but the `ClassCastException` a mismatched tile produces comes from the synthetic bridge on the subclass overriding `convertMulti(T)`, which is the same mechanism Scala's `asInstanceOf[T]` relied on. It is now pinned by a test, because the code reads as though the check was lost.
- Added five Forge server tests, doubling that suite, because most of this class cannot run headless. They cover tile construction through the ASM generator, the NBT round trip through the save/load hooks, `registerTileConverter` appending to a Scala `MutableList` from Java, and `sendDescPacket` against a loaded chunk.
- Dropped the two commented-out blocks the reference carried, the `PlayerInstance.playersInChunk` reflection and the multi-tile `sendDescPackets`, rather than reproducing dead Scala as dead Java. The reason they existed, a missing forge access transformer, is now in the class javadoc.
- Read guidenh's actual source at `6137525` rather than inferring from its constant pool, and recorded the exact reflective member list in `JAVA_MIGRATION_ABI_INVENTORY.md`. The scan could see the 20 names; only the source shows which members are looked up on them, and none of it is visible to the ABI diff.
- Found that `MultipartGenerator$.MODULE$` is load-bearing through reflection. `generateCompositeTile` is `private[multipart]`, so no static forwarder exists and guidenh's static attempt always misses, leaving the companion as the only route. Phase 6/7 must keep it.
- Found that `MicroblockGenerator$.create` is matched by exact parameter types, with `MicroblockClass`'s fully qualified name string-compared. Widening a parameter or renaming the class breaks the lookup while every call site still links.
- Verified against the already-ported `TileMultipart` that `partList_$eq(scala.collection.Seq)`, `loadParts`, `notifyTileChange` and `markRender` all survived. `partList_$eq` is a Scala `var` setter with no Java-facing equivalent and is reflectively load-bearing, so dropping it for a list mutator would have broken guidenh invisibly. It was kept.
- Confirmed the MultipartHelper port satisfies guidenh directly: it resolves `createTileFromNBT` as a static on the plain class and only falls back to `MultipartHelper$.MODULE$` if that misses, so the retained companion is belt-and-braces rather than the primary path.
- Ported `TileCache` to Java, the first conversion free to change shape rather than preserve it. It has zero downstream references in bytecode and does not appear in guidenh's reflective names, so `map()` became a `java.util.Map`, `add`/`remove` became `void`, and `apply` returns a nullable `FlaggedTile` instead of a `scala.Option`. No internal caller used any of the discarded return values.
- Dropped `TileCache$`, `TileCache$FlaggedTile$` and the whole case-class machinery on `FlaggedTile`. It is only ever a map value, never compared, hashed, printed or destructured. This is the `IconHitEffects$` case rather than the `MultipartHelper$` one: nothing names it in bytecode or by string.
- Pinned an unreachable branch before removing it. The reference's `findTile` ends in `case _ => null`, but `Some(FlaggedTile(t, rem))` matches whatever the flag holds, so a tile flagged removed is returned like any other and only the warning is suppressed. The functional test asserts that before the port so the Java version cannot quietly start returning null.
- Confirmed the adapter approach works for ports that change collection types. The characterization test named `scala.Option` in exactly one helper, so the port's test diff was two lines and every behavioral assertion carried over untouched.
- Ported `PacketScheduler`, `IScheduledPacketPart` and `TScheduledPacketPart`. `IScheduledPacketPart` is descriptor-identical, `schedulePacket` is unchanged, and `TScheduledPacketPart.read` stays abstract because `TMultiPart` declares it.
- Found the case the default-versus-abstract rule splits within a single interface. `read` cannot be a default because `TMultiPart` declares it, but `writeScheduled` and `readScheduled` can, because `TMultiPart` declares neither. Making the latter two defaults restores what a Scala implementor already got from the trait for free; only Java implementors ever had to write them out.
- Added `TScheduledPacketPart.readMask` as a Java 8 interface static, carrying the dispatch `TScheduledPacketPart$class.read` used to hold. Where a trait's central member cannot become a default, an interface static plus a documented one-line forwarder is the replacement, and it is the pattern to reuse.
- Widened `sendScheduled` from `private[multipart]` to public. Its only caller is in `codechicken.multipart.handler`, and Java has no scope spanning a package and its siblings, so package-private would not compile. Expect this wherever `private[multipart]` crosses into the handler package.
- Recorded that the whole subsystem has no implementors at all, in this codebase or in the pack. It is a third-party extension point, which is why every behavioral test had to construct its own part.
- Confirmed the Forge server suite can drive the packet path without players connected. `MultipartSPH.getTileStream` only needs a non-remote world and a position, so a part bound to a tile with `setWorldObj` is enough to exercise `getWriteStream`.
- Ported `ControlKeyModifer` and `ControlKeyHandler`. `isControlDown(EntityPlayer)` and `isClientPressing()` keep their descriptors; `map()` became a `java.util.Map`, and the implicit-conversion pair `playerControlValue`/`ControlKeyValue` was dropped because it only ever existed to give Scala `player.isControlDown` sugar and the reference already shipped the explicit static "for Java users".
- Kept the misspelling in `ControlKeyModifer`. It is the published type name; only the file name, which said `ControlKeyModifier.scala`, was corrected. Renaming the type is an API change with no forcing reason and should be a deliberate decision rather than a side effect of a port.
- Converted the first Scala `object` that extends a class rather than being a plain singleton. `ControlKeyHandler` is now an ordinary `KeyBinding` subclass with an `INSTANCE` constant, which drops the twelve static forwarders Scala generated for inherited `KeyBinding` members along with the companion. The registration sites pass `ControlKeyHandler.INSTANCE`.
- Found that a Forge `FakePlayer` makes the server half of player-dependent code testable. `FakePlayerFactory.getMinecraft(worldServer)` yields a player whose world is not remote, which is exactly the branch the server takes, and it needs no connection.
- Recorded that the client half remains untestable in both harnesses. `isClientPressing` is only ever set from a client tick, so the key binding, the press/release packet and the client branch of `isControlDown` are all manual checks.
- Removed both package objects rather than porting them. A Scala package object compiles to a class named `package`, which is a Java keyword, so no Java source can declare or even name it; the characterization had to reach both through `Class.forName`. Each held one member, a `logger` alias for its proxy, so the indirection was inlined at its eight call sites instead of being given an invented Java home.
- Proved the substitution was an identity before making it. The functional test asserted that each alias returned the very instance its proxy holds, so replacing `logger` with `MultipartProxy.logger` / `MicroblockProxy.logger` cannot change behavior. `ConfigContent` and `MultipartGenerator` are descriptor-identical afterwards.
- Noted the first case where the migration's answer is deletion. Zero downstream references plus an impossible target name means there is nothing to preserve, and inventing a `MultipartLogger` type to hold a one-line forwarder would add API rather than remove it.
- Emptied the low-risk queue. Everything remaining is the medium group or later, so the next piece of work should be chosen deliberately rather than taken off the top of a list.
- Ported `MultipartRenderer` in the `TickScheduler$` shape, with the companion holding the implementation and the plain class holding static forwarders. Forge registers the singleton itself and guidenh resolves the static first, so both halves are load-bearing for different reasons and the emitted class list is unchanged.
- Applied the reflective-name rule for the second time, and this is the case it was written for: `MultipartRenderer` has no bytecode consumer at all, only guidenh string constants, and both the class and its companion had to survive.
- Found a Scala-to-Java trap that is not silent but will recur: uniform access makes `renderer.hasOverrideBlockTexture` read the same whether it is a field or a no-arg method. It is a method, and the port did not compile until it grew parentheses. Every remaining renderer conversion will meet this.
- Dropped the three static forwarders Scala generated for inherited `TileEntitySpecialRenderer` members, the same category as the twelve `KeyBinding` forwarders dropped from `ControlKeyHandler`.
- Dropped an unused `ThreadSafeISBRH` import. It annotated nothing and appears nowhere else; if the intent was to mark this ISBRH thread-safe for Angelica then the annotation was never applied in the reference either, which is worth checking independently of the migration.
- Recorded that this port adds nothing to the automated suites and everything to the manual checklist. Static and dynamic rendering, the breaking overlay and the render id all need a client.

### 2026-08-27

- Reproduced Schematica 1.12.6's exact `ReflectionHelper` lookup of the private
  `MultiPartRegistry$.codechicken$multipart$MultiPartRegistry$$typeMap` field. The test failed against the existing Java
  port with `UnableToFindFieldException`, matching Schematica's silent integration-disable path.
- Restored the exact private field name and `scala.collection.mutable.Map` descriptor as Scala's live wrapper over the
  canonical Java registry map. The wrapper adds no copied state or synchronization path.
- Proved through the reflected Scala view that Schematica can resolve a factory and that the Java registry immediately
  sees the same entry. The focused registry suite and all 126 plain-JVM tests pass.
- Added four source-consumer structural guards without adding downstream mods as test dependencies. They pin GuideNH's
  two `BlockMicroMaterial` mixin targets, Et Futurum's mutable static button-orientation arrays, and Iguana's reflected
  `ItemSaw.harvestLevel` field.
- Reproduced Galacticraft's name-only method scan and froze its dangerous assumption: exactly one public
  `MicroMaterialRegistry.registerMaterial` method may exist, it must accept `(IMicroMaterial, String)`, and
  `BlockMicroMaterial(Block, int)` must remain reflectively constructible. All 130 plain-JVM tests pass.
- Accepted the dedicated-server EULA locally in ignored `run/server/eula.txt`, enabling the Forge characterization
  suite without adding a distributable acceptance file to the repository.
- Froze tile NBT order with two different built-in parts: the outer tile `id`, ordered `parts` entries and per-part
  `id`/payload values survive reconstruction, and `jPartList` plus `partMap` reproduce the same order and slots.
- Froze add/remove callback order, list publication, slot binding and detachment in a live Forge world. The plain-JVM
  replacement test also proves that a successful `canReplacePart` query does not mutate order or bindings.
- Reproduced MatterManipulator's live-tile relocation shape. Removing the tile first calls `onWorldSeparate` for each
  part in list order; after reinsertion, `onMoved` calls each part's `onMoved` and default `onWorldJoin`, again in list
  order. The original generated tile, part instances, order, slot map and tile references survive.
- Froze the complete logical chunk-description payload around the same torch/button pair: packet type, chunk
  coordinates, tile count, nibble-relative X/Z, full Y, part count, sorted runtime part IDs, part order and both meta
  bytes. CodeChickenLib's deferred Deflater transport step is deliberately outside this FMP fixture.
- Froze representative generated-trait behavior. The existing `TSlottedTile` fixture pins built-in Scala trait class
  caching and field initialization; a ProjectRed-shaped external Scala microblock trait pins registration by name,
  trait initialization and method dispatch; and `TPartialOcclusionTile` pins Java-trait rewriting and override
  dispatch through the generated tile.
- Froze a server-only pass-through interface with primitive/reference overloads. The generated tile forwards to one
  implementing part, rejects a second implementor, preserves and rebinds its delegate through `copyFrom`, and clears
  the generated implementation field on removal.
- Added an opt-in Forge/JFR workload and captured the first focused CPU/allocation baseline. With eight parts,
  `updateEntity` and `operate` allocate 184.0 and 183.9 bytes per call; generated redstone's three-query iteration
  allocates 80.5 bytes. CPU and allocation sites point to `TileMultipart.parts()` collection copies and Scala
  redstone `IntRef`/closure traversal. Full methodology and rerun commands are in `JAVA_MIGRATION_PROFILE.md`.
- The expanded baseline is 130 plain-JVM tests and 28 Forge server tests, all passing.

### 2026-08-28

- Ported all six interfaces and `RedstoneInteractions` from `IRedstonePart.scala` as one unit. The emitted class list is
  unchanged, every public descriptor is unchanged, and `RedstoneInteractions$.MODULE$` remains the implementation
  singleton behind the static façade.
- Added six plain-JVM tests for interface shape, façade/companion members, maps, part-mask precedence, connector routing,
  and neighbor coordinate transforms, plus three Forge tests for initialized vanilla blocks, world power routing, the
  redstone-wire metadata fallback, and generated `IRedstoneTile` selection.
- Re-profiled immediately before and after the port. Allocation stayed at roughly 4.02 GB / 80.5 B per generated
  three-query iteration, and JFR retained the same `TRedstoneTile` Scala closure/iterator sites. That cost belongs to
  the Phase 5 generated-trait conversion, not this helper unit.
- Chose `MicroRecipe.scala` as the next independent Phase 4 unit because its nested collection scans compile early
  exits into `NonLocalReturnControl`; characterize all five recipe paths and precedence before porting it.
- Characterized `MicroRecipe`'s complete 17-method façade/companion ABI, Scala `splitMap`, all five recipe forms,
  class-specific gluing rules, saw ordering, exact material/tag lookup, and hollow-over-gluing precedence against the
  original Scala implementation.
- Ported the recipe singleton to Java with ordinary loops. The published `MicroRecipe` static façade,
  `MicroRecipe$.MODULE$`, `scala.Tuple3` return and immutable Scala map descriptors remain; the 12 private Scala
  closure classes and their `NonLocalReturnControl` exits are gone.
- The recipe port passes all 141 plain-JVM and 37 Forge tests.
- Characterized `TPartialOcclusionTile` against the untouched Scala implementation with four focused tests covering
  its exact direct class shape, partial/normal precedence, complete-occlusion behavior, and both short-circuit paths.
- Ported `TPartialOcclusionTile` to direct Java source without changing the generator. Its compiled input still has
  one no-arg constructor, no fields, and the same two public descriptors; the Forge rewrite still exposes exactly the
  two behavior methods plus its super accessor, caches the generated class, and passes all 37 server tests.
- The pilot raises the plain-JVM baseline to 145 tests. It deliberately proves no field or lifecycle behavior, so the
  next checkpoint is `TSlottedTile`, not a bulk conversion of the remaining traits.
- Characterized `TSlottedTile` against untouched Scala with six focused Forge cases plus the existing live-world
  lifecycle fixture. The gate freezes its exact 13-method runtime interface, 27-slot per-instance initialization,
  getter/setter rebinding, copy identity, clear/remove/bind behavior, occupied-slot rejection, Scala value equality,
  generated field, super accessors, and class cache.
- Ported `TSlottedTile` to a concrete Java mixin input with one public array field and ordinary loops. The runtime
  rewrite remains interface- and behavior-identical, while the four Scala range closures, `$class` artifact in the raw
  jar, and `NonLocalReturnControl` slot rejection disappear. All 145 plain-JVM and 43 Forge tests pass.
- Found a Phase 5 source-build constraint: shipping binaries still see the same runtime interface, but a consumer
  recompiled directly against the untransformed dev jar sees the Java mixin input as a class. Consumers that name a
  generated trait therefore need a transformed compile stub or must avoid direct trait invocations; record this before
  claiming source-compatible downstream rebuilds.
- Characterized `TRedstoneTile` against untouched Scala with three Forge cases covering its exact eight-method runtime
  interface, class caching, face/edge conduction, strong and weak maxima, mask filtering, arbitrary `Seq` input,
  neighbor masks, and vanilla-side translation. This directly covers ProjectRed's `openConnections` and Extra
  Utilities' one-argument `weakPowerLevel` calls.
- Ported `TRedstoneTile` to Java with allocation-free immutable-list traversal and an iterator fallback for other
  published `Seq` values. A package-private shim isolates inherited `TileMultipart` methods and Minecraft coordinate
  fields from known Java-trait transformer limitations; no public redstone or generated-interface member changed.
- The paired 50,000,000-iteration run fell from 4,023,855,000 bytes / 80.5 B per three-query iteration to zero measured
  allocation, while throughput rose 20.3% from 6,636,424 to 7,986,213 iterations/s. The checksum remained
  `3315999992`, and all 145 plain-JVM plus 46 Forge tests pass.
- Characterized mutable-`Seq` read behavior, direct ordered indexing, and `BlockMultipart.getTile` filtering, then
  extended the focused workload with `getLightValue` and `getTile` phases before changing production code.
- Removed the Java-port-only `ArrayList` snapshots from all read paths and Java wrappers from internal block,
  renderer, and scheduler paths. The public `jPartList()` bridge is unchanged; add/remove retain intentional mutable
  snapshots before publishing a replacement immutable `Seq`.
- In the paired run, `getLightValue` fell from 183.9 B to 0.0 B per call with 11.42x throughput, and `getTile` fell
  from 24.0 B to 0.0 B per call with 2.89x throughput. All 147 plain-JVM and 46 Forge tests pass.

### 2026-08-30

- Characterized `TileMultipartClient` and `TRandomDisplayTickTile` together against untouched Scala. Six behavior
  cases freeze render-cache partitioning and bounds, lazy initialization, dynamic short-circuiting, base no-op display
  ticks, and ordered `IRandomDisplayTick` dispatch; a Forge smoke case freezes both exact runtime interfaces,
  inheritance, generated fields, and generated-class caching.
- Ported both traits to Java with package-private access shims. `registerJavaTrait` now linearizes an already registered
  Java parent trait, recognizes explicit field accessors, and skips transient runtime caches when auto-generating
  `copyFrom`. Trait registration now precedes `BlockMultipart` construction so Forge defines the runtime interfaces
  before ordinary Java bytecode can preload the mixin inputs.
- Added three no-op client dispatch hooks to `TileMultipart`, allowing in-repo Java callers to invoke generated
  overrides through the stable superclass rather than emit class opcodes against types Forge rewrites to interfaces.
  The runtime trait surfaces remain exact; the raw dev-jar source-build limitation remains documented.
- All 153 plain-JVM tests and all 77 Java 8 Forge dedicated-server tests pass. Actual static/dynamic rendering and
  particle appearance remain client-only manual checks.
- Characterized `MultipartCompatiblity` and `MCPCCompatModule` against untouched Scala. Three plain-JVM cases freeze
  both static facades, both `MODULE$` companions, the private Scala `Function4` field, default allow behavior and
  callback identity; two Forge cases freeze non-MCPC loading and the logged missing-hook fallback.
- Ported both singletons to four Java types with unchanged public names and descriptors. The callback still propagates
  reflection and cast failures unchanged, while the two Scala anonymous-function artifacts become private named Java
  callback classes. No frozen binary or audited source consumer names those implementation classes.
- All 156 plain-JVM tests and all 79 Java 8 Forge dedicated-server tests pass. This initialization-only hook is not a
  meaningful target for the focused allocation benchmark; successful MCPC integration remains environment-dependent.
- Characterized `MultipartMod` against untouched Scala. Two plain-JVM cases freeze both annotated singleton types,
  all ten lifecycle methods and annotations, plus the `MultipartPH.channel` companion descriptor and identity; two
  Forge cases freeze FML's companion mod instance, completed initialization and server-stop cleanup.
- Ported the singleton to an annotated Java facade/companion pair while retaining `modLanguage = "scala"`. The one
  source use of the object as a value now names `MultipartMod$.MODULE$` explicitly, leaving the compiled packet-handler
  field and accessor unchanged.
- Found that the inherited `MultipartProxy.postInit` static forwarder carries `@SideOnly(CLIENT)` and is stripped on a
  dedicated server. The Java companion therefore invokes `MultipartProxy$.MODULE$` directly, matching the reference
  Scala bytecode and allowing virtual resolution to reach the server implementation.
- All 158 plain-JVM tests and all 81 Java 8 Forge dedicated-server tests pass. Mod lifecycle dispatch is startup-only,
  so it is not a meaningful focused throughput or allocation target.
- Characterized `MultipartEventHandler` against untouched Scala. Two plain-JVM cases freeze both singleton types, all
  twelve public event methods, priorities and the client-only highlight boundary; three Forge cases freeze companion
  registration on both buses, chunk load/unload cleanup, queued watches and END-phase tick dispatch.
- Ported the singleton to a Java facade/companion pair with unchanged public names, descriptors and annotations. The
  proxy now names `MultipartEventHandler$.MODULE$` explicitly, preserving the exact object registered on both buses.
- Server ticking still passes the configuration manager's live player list through Scala's Java-list buffer adapter;
  no copy or new traversal was introduced. All 160 plain-JVM and all 84 Java 8 Forge dedicated-server tests pass.
- Characterized `MicroblockMod` against untouched Scala. Two plain-JVM cases freeze both annotated singleton types,
  all ten lifecycle/IMC methods, the mutable `angelicaCompat` accessors and shared identity; one Forge case freezes
  FML's companion mod instance and the completed microblock lifecycle.
- Ported the singleton to an annotated Java facade/companion pair while retaining `modLanguage = "scala"`. Lifecycle
  dispatch calls `MicroblockProxy$.MODULE$` directly, matching the reference bytecode and avoiding side-only static
  forwarders that Forge strips on a dedicated server.
- The internal client assignment now calls the preserved `angelicaCompat_$eq` method explicitly because recompiled
  Scala cannot apply property-assignment syntax to a Java-authored setter. The compiled accessor ABI is unchanged.
- All 162 plain-JVM and all 85 Java 8 Forge dedicated-server tests pass. Startup and lifecycle dispatch are not a
  meaningful focused throughput or allocation target; `MicroblockEventHandler.scala` is the next target.
- Reproduced the reported stale incremental `@Mod(version)` failure immediately after a commit. Gradle reran
  `compileScala`, but Zinc retained the joint-compiled Java classes that had inlined the previous `Tags.VERSION`.
- Configured `compileScala` to force Zinc recompilation whenever Gradle schedules the task. The focused version test
  passes without cleaning both before and after a commit changes the generated version, so the assertion remains.
- Characterized `MicroblockEventHandler` against untouched Scala. Two plain-JVM cases freeze both singleton types, all
  four public event methods, normal event metadata and both client-only boundaries. One Forge case freezes companion
  registration and complete method stripping on a dedicated server.
- Ported the handler to a Java facade/companion pair and changed the one Scala registration to name `MODULE$`
  explicitly. Texture-atlas filtering, highlight guards, matrix/render sequence and cancellation behavior are
  unchanged; their actual rendering remains on the client manual checklist.
- All 164 plain-JVM and all 86 Java 8 Forge dedicated-server tests pass. This event-only adapter is not a meaningful
  focused performance target; `microblock/handler/packethandlers.scala` is next.
- Characterized the ForgeMicroblock packet-handler unit against untouched Scala. Seven plain-JVM cases freeze the
  shared channel base, both facade/companion surfaces and exact packet interfaces, the integrated-server registry
  skip, ordered missing-material disconnect, unknown-type `MatchError` and no-op server callback. One Forge case
  freezes the handshake channel, type and complete material-ID payload.
- Ported all five emitted packet-handler classes to Java and changed the two Scala proxy registrations to pass the
  companion singletons explicitly. Every callable public member and the emitted class list match the reference; the
  registry channel and wire format are unchanged.
- All 171 plain-JVM tests pass. After the local EULA was accepted, all 87 Java 8 Forge dedicated-server tests pass,
  including the new registry handshake.
- Characterized `MultipartSaveLoad` against untouched Scala. Three plain-JVM cases freeze the static facade,
  load-bearing companion, private fields and exact dummy class shape. Four Forge cases freeze ProjectRed-style binary
  linkage, both reflected vanilla maps, converter precedence/deletion and saved multipart reconstruction.
- Ported the singleton to a Java facade/companion pair and kept the dummy as the static facade member
  `MultipartSaveLoad.TileNBTContainer`, which emits the literal `MultipartSaveLoad$TileNBTContainer` binary name.
  Nesting it under the companion would emit the wrong double dollar. Every callable public member matches the
  reference. Only the unreferenced compiler-generated `$$anonfun$1` closure disappeared.
- All 174 plain-JVM tests and all 91 Java 8 Forge dedicated-server tests pass. The next medium-risk target is
  `MissingMicroMaterial.scala`; its real icon/render paths remain client-manual work.
- Characterized `MissingMicroMaterial` against untouched Scala. Three plain-JVM cases freeze the exact facade and
  companion surfaces, `MODULE$`, inert material values, interface defaults and all client-only boundaries. One Forge
  case freezes side stripping and the exact singleton registered under the missing-material name and ID.
- Ported the singleton to a Java facade/companion pair and changed both Scala object-value uses to name `MODULE$`
  explicitly. The placeholder key, stone item, sound, strength, resistance and missing-texture render pipeline are
  unchanged; only the actual client rendering remains manual.
- All 177 plain-JVM and all 92 Java 8 Forge dedicated-server tests pass. This inert singleton is not a meaningful
  focused performance target; `DefaultContent.scala` is the next medium-risk target.
- Characterized `DefaultContent` against untouched Scala. One plain-JVM case freezes its one-method static and
  companion surfaces. Two Forge cases freeze the five microblock factories and IDs, all 103 sorted built-in
  materials, their exact implementation types and the complete legacy-name remap table.
- Ported the singleton to a Java facade/companion pair while continuing to use the existing `BlockMicroMaterial$`
  overloads and Scala ranges. Registration contents, ordering and the historical meta-0-only `log2`/`leaves2`
  overload behavior are unchanged.
- All 178 plain-JVM and all 94 Java 8 Forge dedicated-server tests pass. Pre-init-only registration is not a meaningful
  focused performance target; `GrassMicroMaterial.scala` is the next material unit.
- Characterized `GrassMicroMaterial` and `TopMicroMaterial` against untouched Scala. Five plain-JVM cases freeze both
  constructors, the grass overlay accessor used by UtilitiesInExcess, the top default-argument facade/companion,
  common-side boundaries and horizontal/side UV plus colour-pipeline routing. One Forge case freezes their registered
  blocks/meta and the exact surface left on a dedicated server.
- Ported both classes and the default-argument companion directly to Java. Grass keeps its uncoloured base side pass,
  coloured top and height-adjusted side overlay; `TopMicroMaterial` keeps coloured horizontal faces and translated
  side UVs. All three emitted binary names and every callable public descriptor match the Scala reference.
- A clean compile was required to evict the deleted Scala classes before the unchanged characterization could test the
  Java implementation. All 183 plain-JVM and all 95 Java 8 Forge dedicated-server tests pass. The render path retains
  the same per-side transformation work, so no separate performance claim is made; `multipart/handler/proxies.scala`
  is the next medium-risk target.

### 2026-08-31

- Audited the published API surface against all 28 consumer checkouts to decide whether a cleaner API is warranted. It
  largely already is one: `TMultiPart` is an ordinary abstract Java class, and the factory, converter, material,
  `PartMap`, redstone and occlusion interfaces need no replacement. Added Phase 9 for the parts that are not.
- Found nine remaining public entries carrying Scala types. Two already have a Java sibling (`jPartList`, the
  `String...` `registerParts` overload), so their deprecation is free. `getIdMap()` needs no pair or map replacement
  because the array index is the material ID; `materialCount()` plus the existing `materialName(int)` and
  `getMaterial(int)` covers every audited use.
- Found a second and larger cleanliness problem: Scala's `private[multipart]` compiles to public, so seventeen
  implementation hooks are advertised as API. Verified all seventeen have zero external callers, and that the two
  similar-looking members that *are* load-bearing are `bindPart` (OpenComputers) and `internalPartChange`
  (ProjectRed). Fix is javadoc only, no descriptor change.
- Confirmed that removing Scala from FMP has no pack-level dependency payoff: OpenComputers (843 Scala files) and
  ProjectRed (171) keep `scala-library` in the pack regardless. Recorded this under Phase 8 so the motivation is not
  restated later as a dependency argument.
- Traced `ScalaSignature.scala` and `ByteCodecs.scala` to a single external caller, ProjectRed's ~60-line
  `LightMicroblock` trait. Rewriting it in Java deletes roughly 580 lines from FMP plus the Scala branch of
  `ASMMixinCompiler`, but it is not currently possible: `registerJavaTrait` rejects abstract classes while
  `Microblock` is abstract, and `@SideOnly` stripping exists only on the Scala signature path while
  `LightMicroblock.renderDynamic` is client-only and applied on both sides. Both gaps are recorded as Phase 7
  prerequisites.
- Added Phase 10 for upstream consumer cleanup. All 27 consumers are GTNewHorizons forks and therefore patchable;
  Extra Utilities is the sole exception and is constrained rather than fixed. The pattern is a three-step ratchet:
  FMP adds a supported equivalent (additive, safe now), the consumer is patched and released, then FMP drops the
  private shape in a release allowed to break that ABI.

### 2026-09-01

- Characterized `MultipartProxy` against untouched Scala. Four plain-JVM cases freeze the four-type hierarchy, exact
  facade/companion surfaces, mutable singleton accessors, client-only boundaries and both chunk-index conversions. One
  Forge case freezes dedicated-server stripping, virtual fallback to the server implementation and initialized proxy
  state.
- Ported the hierarchy and facade to four Java types without changing their emitted names or callable public
  descriptors. Common generated-tile registration now names `MultipartProxy$.MODULE$` explicitly so it still reaches
  the inherited server method after Forge strips the client override and its static forwarder.
- All 187 plain-JVM and all 96 Java 8 Forge dedicated-server tests pass. Startup registration and two bit-packing
  helpers are not a meaningful focused performance target; `microblock/handler/proxies.scala` is next.
- Characterized `MicroblockProxy` against untouched Scala. Four plain-JVM cases freeze its four-type hierarchy,
  complete facade/companion ABI, eight mutable server fields, protected Scala saw list and exact lazy-renderer shape.
  Two Forge cases freeze side stripping, inherited server lifecycle resolution, item/ore/recipe registration and saw
  order.
- Ported the hierarchy and facade to four Java types. The Scala `MutableList` remains for binary compatibility, the
  client renderer retains its field-only side annotation and lazy bitmap, and the saw-renderer closure becomes a
  direct iterator loop.
- All 191 plain-JVM and all 98 Java 8 Forge dedicated-server tests pass. This startup-only proxy is not a meaningful
  focused performance target; `multipart/handler/packethandlers.scala` is the last Scala handler unit and is next.
- Characterized the ForgeMultipart packet-handler unit against untouched Scala. Eleven plain-JVM cases freeze all six
  emitted retained types, exact facade/companion interfaces, channel identity, private prefixed state accessors,
  ordered registry and desync disconnects, control-key packets, coordinate streams and both update terminators.
- Ported the five top-level types and nested byte stream directly to Java. The three Scala mutable collection
  descriptors remain where reflection already observes them, while direct loops replace the anonymous `MultiMap` and
  thirteen closure classes without changing watcher, batching, framing or cleanup behavior.
- A clean build passes all 202 plain-JVM tests and the Java 8 Forge server passes all 98 tests. All six retained public
  surfaces match the reference by name and descriptor; only fourteen unreferenced compiler artifacts disappear.
  `microblock/ItemMicroPart.scala` is next.
- Characterized `ItemMicroPart` against untouched Scala. Six plain-JVM cases freeze its item, static facade and both
  companion surfaces, NBT/material semantics, creation overloads, invalid-class short circuits and a Scala 2.11.5
  binary consumer that calls all four `ItemMicroPart$.MODULE$` methods used by ProjectRed.
- Ported the item and renderer to four Java types with every callable public name and descriptor retained. Creative
  enumeration is a direct loop, and the renderer crosses the transformed `MicroblockClient` boundary through one
  narrow Scala helper rather than emitting an invalid class-method call from Java.
- A clean build passes all 208 plain-JVM tests and the Java 8 Forge server passes all 98 tests. The jar retains exactly
  the four public ItemMicroPart types; three unreferenced Scala iteration closures disappear.
  `microblock/MicroblockPlacement.scala` is next.
- Characterized `MicroblockPlacement` against untouched Scala. Three plain-JVM cases freeze the exact six-type
  hierarchy, constructors, fields, callable descriptors, companion and defaults. Four Forge cases freeze external
  placement, internal/opposite-slot selection, in-place expansion, custom-placement precedence and consumption.
- Ported the six retained types directly to Java. The only Scala source caller now names
  `MicroblockPlacement$.MODULE$` explicitly; no runtime class was added or removed and every callable public member
  matches the reference.
- A clean build passes all 211 plain-JVM tests and the Java 8 Forge server passes all 102 tests.
  `microblock/PlacementGrids.scala` is next.
- Characterized `PlacementGrids` against untouched Scala. Five plain-JVM cases freeze the exact nine-class trait,
  helper, configurable-grid, facade and companion surface plus every face/corner/edge selection boundary on all six
  hit sides. The tests call ProjectBlue's load-bearing static facade directly.
- Ported all nine retained types to Java, preserving `PlacementGrid$class` for old Scala forwarders and using safe
  Java defaults for the three concrete trait methods. The three remaining Scala object-value users now name their
  companions explicitly.
- A clean build passes all 216 plain-JVM tests and the Java 8 Forge server passes all 102 tests. The jar class list is
  unchanged and all callable public descriptors match the reference. `microblock/BlockMicroMaterial.scala` is next.
- Characterized `BlockMicroMaterial` against untouched Scala. Five plain-JVM cases freeze all five retained public
  types, exact methods/fields/client annotations, material delegation, thread-local render-helper state and inventory
  pipeline. A frozen Scala 2.11.5 consumer calls both load-bearing companions; one Forge case freezes registered block
  semantics and dedicated-server side stripping.
- Ported the material base, both facade/companion pairs and `ThreadState` to Java. The GuideNH-targeted private final
  `block`/`meta` fields, public `(Block, int)` constructor, Scala `Seq` registration overloads, historical meta-0-only
  overload behavior, render pipeline and Angelica override lifecycle are unchanged.
- A clean build passes all 222 plain-JVM tests and the Java 8 Forge server passes all 103 tests. Every callable public
  descriptor matches the reference; only three unreferenced Scala closure/anonymous classes disappear.
  `microblock/ConfigContent.scala` is next.
- Characterized `ConfigContent` against untouched Scala. Six plain-JVM cases freeze its facade, companion and exact
  mutable-map field, config-file generation and parsing, alias/range semantics, malformed-line recovery, block
  registration and IMC filtering/validation.
- Ported the facade and companion directly to Java. Both retained runtime classes and every callable public descriptor
  match the reference; the seven Scala iteration/parser closure classes disappear. Public file helpers still throw the
  original `IOException` instances without adding checked exceptions to their descriptors or source declarations.
- A clean build passes all 228 plain-JVM tests and the Java 8 Forge server passes all 103 tests.
  `microblock/AngelicaCompat.scala` is next.
- Added a dependent GitHub Actions job that accepts the EULA in its ephemeral runner and invokes the existing
  self-validating `runFunctionalTestServer` task after the shared GTNH build. The Forge suite now gates pull requests
  and pushes instead of being local-only.
- Characterized `AngelicaCompat` against untouched Scala. Two plain-JVM cases freeze its exact non-final public
  surface, unusual `Object` return descriptors, `CapturingTessellator` guard, Iris calls and caught
  `ClassCastException` fallback.
- Ported the sole class directly to Java while retaining `BoxedUnit.UNIT` on the normal path and `Unit$.MODULE$` on
  the fallback path. Both jars contain the same runtime class and every callable descriptor matches the reference.
- A clean build passes all 230 plain-JVM tests and the Java 8 Forge server passes all 103 tests.
  `microblock/ItemSaw.scala` is next.
- Characterized `ItemSaw` and `ItemSawRenderer` against untouched Scala. Four plain-JVM cases freeze all three runtime
  types, the reflective private-final harvest field, default and explicit durability, container behavior, renderer
  gating and supported render-type selection.
- Ported the item, static renderer facade and registered renderer companion directly to Java. All callable public
  descriptors, singleton/model fields and three runtime classes match the reference.
- A clean build passes all 234 plain-JVM tests and the Java 8 Forge server passes all 103 tests.
  `microblock/MicroblockRender.scala` is next.
- Characterized `MicroblockRender` against untouched Scala. Four plain-JVM cases freeze its facade/companion surface,
  thread-local face state, cuboid face-mask traversal, no-placement highlight exit and exact transformed-client call
  opcodes.
- Ported both retained types directly to Java. A clean compile preserves `invokevirtual Microblock.setShape` and
  `invokeinterface MicroblockClient.getBounds/render`; direct face iteration removes three unreferenced Scala
  anonymous/closure classes.
- A clean build passes all 238 plain-JVM tests and the Java 8 Forge server passes all 103 tests.
  `microblock/MicroblockClass.scala` is next.
- Characterized `MicroblockClass`, `CommonMicroClass` and its companion against untouched Scala. Three plain-JVM
  cases freeze the exact hierarchy, constructors, public descriptors, private fields, side annotations, registry
  semantics and generator call descriptors. Constructor execution remains a Forge-only boundary because generator
  initialization requires the launch class loader.
- Ported all three retained types directly to Java. Eager base-trait registration, synchronized one-time client-trait
  registration, part-factory registration order, class IDs, duplicate rejection and both create paths are unchanged.
  The GuideNH-pinned `MicroblockGenerator$.create(MicroblockClass, int, boolean)` descriptor remains exact.
- A clean build passes all 241 plain-JVM tests and the Java 8 Forge server passes all 103 tests. The clean reference and
  port jars contain the same three runtime types and every callable public descriptor matches. `microblock/Microblock.scala`
  is the next deliberately high-risk target.
- Characterized the `Microblock` base, its default-argument companion and all three mixin traits against untouched
  Scala. Three plain-JVM cases freeze the eight retained type surfaces, fields, constructor/default, signed shape
  packing, material delegation, item conversion, description/update bytes and core NBT.
- Ported `Microblock` and `Microblock$` directly to Java while moving `MicroblockClient`, `CommonMicroblock` and
  `CommonMicroblockClient` unchanged to `MicroblockTraits.scala`. Keeping those load-bearing Scala traits avoids a
  Java single-inheritance workaround and preserves the existing ProjectRed generator path. Three remaining Scala
  assignments now call the same public field setters explicitly.
- A clean build passes all 244 plain-JVM tests and the Java 8 Forge server passes all 103 tests, including generated
  face/hollow parts and the external Scala microblock trait fixture. All eight retained public surfaces match the
  reference; only two private Scala iteration closures disappear. `microblock/FaceMicroblock.scala` is next.
- Characterized the face factory, placement singleton and both generated traits against untouched Scala. Two
  plain-JVM cases freeze all eight retained public surfaces and every placement rule; one Forge case freezes factory
  identity, all 42 populated bounds and generated face-part behavior.
- Ported the four concrete facade/companion types directly to Java while retaining `FaceMicroblock` and
  `FaceMicroblockClient` unchanged in `FaceMicroblockTraits.scala`. All callable public descriptors match the
  reference; the two private Scala bounds-initializer closures disappear.
- A clean build passes all 246 plain-JVM tests and the Java 8 Forge server passes all 104 tests. Recompiled Scala must
  spell the Java array getter as `FaceMicroClass.aBounds()(index)`; existing binaries still link to the unchanged
  `aBounds(): Cuboid6[]` descriptor. `microblock/CornerMicroblock.scala` is next.
- Characterized the corner factory, placement singleton and generated trait against untouched Scala. Two plain-JVM
  cases freeze all six retained public surfaces and all 48 slot/side placement mappings; one Forge case freezes
  factory metadata, all 56 populated bounds and generated shape/slot behavior.
- Ported the four concrete facade/companion types directly to Java while retaining `CornerMicroblock` in
  `CornerMicroblockTraits.scala`. ProjectRed's load-bearing `CornerMicroClass$.MODULE$.getClassId()` linkage and every
  callable public descriptor remain exact; the two private Scala bounds-initializer closures disappear.
- A clean build passes all 248 plain-JVM tests and the Java 8 Forge server passes all 105 tests. Recompiled Scala must
  spell the Java array getter as `CornerMicroClass.aBounds()(index)`; existing binaries still link unchanged.
  `microblock/EdgeMicroblock.scala` is next as one Edge/Post source unit.
- Characterized the combined Edge/Post unit against untouched Scala. Two plain-JVM cases freeze all twelve retained
  public surfaces, state/super accessors and edge-opposite mappings. Three Forge cases freeze both factories, all 84
  edge and 12 post bounds, generated behavior, even-size post placement and matching-post expansion.
- Ported the six concrete facade/companion types directly to Java while retaining `EdgeMicroblock`,
  `PostMicroblock`, and stateful `PostMicroblockClient` in `EdgeMicroblockTraits.scala`. ProjectRed and
  UtilitiesInExcess class-ID linkage remains exact; the Post client traversal closure and all trait helpers remain.
- A clean build passes all 250 plain-JVM tests and the Java 8 Forge server passes all 108 tests. All callable public
  descriptors match the reference; only four private bounds-initializer closures disappear.
  `microblock/HollowMicroblock.scala` is next.
- Characterized the Hollow unit against untouched Scala. Two plain-JVM cases freeze all nine retained public
  surfaces, including the source-visible nested placement-grid relationship and both generated traits. Two Forge
  cases freeze both 42-entry tables, generated server behavior, every face, connected hollow sizes 1 through 11,
  and all collision, occlusion and subpart geometry.
- Ported the four concrete placement/factory facade and companion sources directly to Java while retaining
  `HollowMicroblock` and the large stateful `HollowMicroblockClient` in `HollowMicroblockTraits.scala`. The nested
  `HollowPlacement.HollowPlacementGrid$` remains a real public static nested class rather than merely keeping its
  binary name.
- A clean build passes all 252 plain-JVM tests and the Java 8 Forge server passes all 110 tests. All nine supported
  public surfaces match the reference; both retained trait helpers and all seven trait closures are bytecode-identical.
  Only three private factory table-initializer closures disappear. `microblock/TMicroOcclusion.scala` is next.
- Characterized `MicroOcclusion` against untouched Scala. Five plain-JVM cases freeze the facade, companion, three
  generated-trait surfaces, all valid shrink-side mappings, exhaustive priority/size/transparency decisions, render
  masks, traversal ranges and the complete `TMicroOcclusion` decision matrix.
- Ported only the concrete facade and companion to Java. `JMicroShrinkRender`, `TMicroOcclusion` and the stateful
  `TMicroOcclusionClient` remain Scala; both trait helpers and all five retained Scala types have bytecode-identical
  disassembly. Direct Java iteration removes the sole private shrink closure.
- A clean build passes all 257 plain-JVM tests and the Java 8 Forge server passes all 110 tests. All seven supported
  public surfaces and WR-CBE's static `recalcBounds` descriptor match the reference. `microblock/MicroblockGenerator.scala`
  is next.
- Characterized `MicroblockGenerator` against untouched Scala. Three plain-JVM cases freeze its facade, companion,
  nested material interface, inherited `ASMMixinFactory`/`ScratchBitSet` shape, replaceable thread-local scratch state
  and load-bearing calls. One Forge case freezes the complete material-added external Scala-trait path.
- Ported the facade, companion and real public static nested `IGeneratedMaterial` interface directly to Java while
  leaving the generator and ScalaSignature machinery unchanged. Scratch-bit reuse, base/client selection, material
  callback ordering, boxed constructor argument and the ProjectRed Scala-trait registration path are unchanged.
- A clean build passes all 260 plain-JVM tests and the Java 8 Forge server passes all 111 tests. The same three runtime
  classes and every callable public descriptor match the reference, including ProjectRed's companion registration
  and GuideNH's exact companion `create` method. `multipart/MultipartGenerator.scala` is next.
- Characterized `MultipartGenerator` against untouched Scala. Two plain-JVM cases freeze both public surfaces,
  private Scala-map descriptors and companion call opcodes. Five Forge cases freeze side-specific hierarchy caches,
  duplicate/failed registration, scratch clearing, class snapshots/reuse, tile upgrades/downgrades, vanilla-block
  conversion and a precompiled Scala consumer exercising companion generation and pass-through registration.
- Ported the facade and companion directly to Java. All five Scala maps, both compiler-generated public accessors,
  the companion-only `generateCompositeTile` descriptor and side-safe proxy callback remain. Direct iteration removes
  six private closures; the ASM factory's sole source adjustment explicitly names `MultipartGenerator$.MODULE$`.
- A clean build passes all 262 plain-JVM tests and the Java 8 Forge server passes all 116 tests. Both supported public
  surfaces match the reference. `multipart/asm/ScratchBitSet.scala` is the next isolated support target; the generated
  microblock traits still require the documented abstract-Java-mixin and side-only-member prerequisites.
- Characterized `ScratchBitSet` against untouched Scala. Four plain-JVM cases freeze the exact interface/helper
  surface, lazy allocation, repeated accessor calls, owner/thread isolation, bit preservation/clearing, storage
  replacement/reinitialization and `freshBitSet` dispatch through an overridden `getBitSet`.
- Ported the interface and `$class` helper directly to Java without changing either generator. All seven callable
  methods, their abstract/static modifiers and both binary names remain exact; no new API or default methods are
  introduced. Neither downstream audit contains a reference to this support trait.
- A clean build passes all 266 plain-JVM tests and the Java 8 Forge server passes all 116 tests. The two-type ABI and
  both generator companions' disassembly match the reference. `multipart/asm/ByteCodecs.scala` is next as an isolated
  codec port, leaving signature parsing and trait compilation for separate targets.
- Condensed `JAVA_MIGRATION_DIVERGENCES.md` from 3,134 to 189 lines, keeping effective runtime, binary and source
  differences plus one shared classfile section. Removed repeated preservation claims, validation histories and
  superseded intermediate decisions; the original narrative remains in git history. The workflow now records test
  results here and updates the divergence ledger only for a genuinely new difference.
- Characterized `ByteCodecs` against untouched Scala in five plain-JVM cases: exact facade/companion ABI, every byte
  value and packing remainder, zero escaping, extra decoded padding, signed shifts and partial writes before malformed
  input fails. The baseline also rejects a raw `0xBF` escaped into a lone `0xC0`; that behavior is preserved.
- Ported the facade and companion directly to Java, retaining the original LAMP/EPFL attribution and unrolled
  algorithm. The signature-parser source is unchanged; its recompiled calls now use the retained static facade.
- Clean formatting/checkstyle/build passes all 271 plain-JVM tests, and Java 8 Forge passes all 116 tests including
  external Scala-trait generation. Both packaged types have Java source markers, Java 8 class versions and identical
  callable public names/descriptors. An additional one-off differential run compared all two-byte inputs across all
  six methods plus randomized lengths through 256, null and invalid decode lengths: all 450,790 cases matched on
  return values, exception types and full mutated arrays. No new difference needs a separate ledger entry.
  `multipart/asm/ASMImplicits.scala` is next; compiler behavior changes remain separate work.
- Characterized `ASMImplicits` against untouched Scala in five plain-JVM cases: all six runtime surfaces, identity
  conversions, node names, null handling, BitSet self-replacement and clear-before-failure, independent plain-BitSet
  copies, and boxed equality/hash behavior. Neither consumer audit identifies a direct external user.
- A follow-up characterization, also passed against the saved Scala reference jar, pins virtual `BitSet.equals`
  dispatch even for identical references. The Java implementation uses an explicit null check here because
  `Objects.equals` would skip that call.
- Ported the facade, companion and four nested value-class/extension types to two Java sources, retaining every
  callable public name, descriptor and modifier. Seven Scala compiler/factory call sites now explicitly invoke the
  helpers; compiler algorithms and trait registration are unchanged. The ledger records the lost implicit syntax and
  the Java publication rule: explicitly constructing either public extension companion no longer replaces `MODULE$`,
  verified against the reference in isolated class loaders.
- Clean formatting/checkstyle/build passes all 276 plain-JVM tests, and Java 8 Forge passes all 116 tests. All six
  helpers have Java source markers and Java 8 class versions; compiler/factory public descriptors and the full
  464-class packaged inventory match the reference. `MultipartGenerator$` disassembly is unchanged. The next bounded
  target is extracting `DebugPrinter` from `ASMMixinCompiler.scala`, leaving the compiler algorithms for later.
- Extracted `DebugPrinter` into a Java facade and companion, retaining all five methods on each and the companion
  singleton field. Configuration/default gating, immediate-child cleanup, dump paths/content and the 16,000-byte
  logging threshold are unchanged. Only three compiler call sites explicitly name `DebugPrinter$.MODULE$`.
- Per the current user instruction, no tests were added. A clean formatting/checkstyle/build passes all 276 existing
  plain-JVM tests; the Java 8 Forge suite passes all 116 tests with dumping both enabled and disabled. Against the
  saved `1faf0dd` baseline, all 39 generated dump filenames and SHA-256 hashes and all six ordered byte-count messages
  match. The disabled run leaves the existing dump contents and modification times untouched and retains those log
  messages. Restored the original local `debug_asm=true` setting afterward.
- Both converted types have Java source markers and Java 8 class versions, and all callable public names/descriptors
  match. Removing the private cleanup closure shifts the compiler's private closure numbering; all 64 compiler types'
  disassembly matches after that renaming alone. The packaged inventory drops from 464 to 463 classes. No new
  divergence needs a ledger entry. `ByteCodeReader` in `multipart/asm/ScalaSignature.scala` is the next isolated target.
- Extracted `ByteCodeReader` to one Java source, retaining its public constructor, all nine methods and both private
  fields. Accessor/reader calls remain virtual, the generic `advance(int, A)` call remains eager, and byte unboxing
  retains Scala's null-to-zero behavior for overrides. String reads use a standard-library slice with the original
  default charset and Scala `drop`/`take` clamping; signed-byte decoding, integer overflow and unchecked failures are
  preserved. No signature-model or compiler algorithm changed.
- No tests were added, per the current user instruction. A clean formatting/checkstyle/build passes all 276 existing
  plain-JVM tests; Java 8 Forge passes all 116 tests, including external Scala-trait generation. All 39 generated dump
  names and SHA-256 hashes match the saved `b45527e` reference.
- The Java 8 packaged reader retains every callable public name/descriptor and the complete 463-class inventory is
  unchanged. All 140 retained `ScalaSignature`, `ScalaSigReader` and `ASMMixinCompiler` types have identical
  disassembly. Recompiled Scala uses `pos_$eq` and `advance(length, value)`; the latter source-syntax difference is
  recorded in the ledger. `ScalaSigReader` in `multipart/asm/ScalaSignature.scala` is the next isolated target.
- Extracted `ScalaSigReader` to a Java facade and companion, retaining all five methods on each and the singleton
  field. Decode still uses the platform charset; encode keeps UTF-8 and the reference's trailing-byte truncation,
  including empty input. Annotation reads still use `ScalaSignature.Bytes$`, writes return the replaced list value,
  and lookup returns the first match as a Scala `Option` with the original malformed-entry failures.
- Replaced Scala array slicing and annotation traversal with standard-library operations. Only two compiler calls
  explicitly name the companion; no signature-model, byte-codec or compiler algorithm changed. Both Java 8 public
  surfaces match the saved `9f92704` reference, and all 137 retained signature/compiler types have identical
  disassembly. Only the private annotation-search closure disappears, taking the packaged inventory from 463 to 462.
- No tests were added, per the current user instruction. A clean formatting/checkstyle/build passes all 276 existing
  JVM tests; Java 8 Forge passes all 116 tests, including external Scala-trait generation. All 39 generated dump names
  and SHA-256 hashes match the reference. No new ledger entry is needed. `multipart/asm/ASMMixinFactory.scala` is next
  as a single factory port before the larger nested signature model and compiler.

### 2026-09-02

- Ported `ASMMixinFactory` directly to Java, retaining the Scala `Seq` constructor/argument descriptors, all six
  fields, synchronized construction, live parameter conversion, copied cache keys, generated-name sequence and both
  Scala- and Java-trait registration paths. Parent validation and callback/registration order remain unchanged;
  reflection failures escape as the same exceptions without adding checked exceptions to the public methods.
- Kept the reference's public JVM callbacks and both public mangled parent helpers. The retained Scala subclass
  needs only an explicit empty parameter sequence and public override syntax. Its companion and five closure types
  have identical disassembly; Scala adds four static facade forwarders without changing any existing entry. The
  ledger records that additive surface and the Scala-source syntax changes.
- No tests were added, per the current user instruction. Clean formatting/checkstyle/build passes all 276 existing
  JVM tests; Java 8 Forge passes all 116 tests, including the external Scala-trait and generated-tile fixtures. All
  39 generated dump names and SHA-256 hashes match the saved `f7be2b1` reference. Both generator companions and all
  137 signature/compiler types have identical disassembly. Removing two private parent-traversal closures reduces
  the packaged inventory from 462 to 460 classes. `multipart/asm/MultipartMixinFactory.scala` is next, with the
  signature model and compiler algorithms still reserved for later targets.
- Ported `MultipartMixinFactory` to a Java facade/companion pair, retaining all ten facade methods, the singleton,
  both public callback overrides and the two companion-only mangled helpers. No other production source needed an
  adjustment. The compiler, signature model and stack analyser remain unchanged.
- Preserved the non-transient-field snapshot, existing `copyFrom` guard, generated delegate binding/removal,
  single-implementor gate, inherited-method override precedence, names, logging and registration order. The method
  collector retains Scala's immutable map and ordered inherited-entry buffer, preserving emitted method order;
  the public bridge helper still updates its supplied `ObjectRef` before emitting instructions. `onCompiled` keeps
  the side-safe `MultipartGenerator$.MODULE$` call.
- No tests were added, per the current user instruction. Formatting/checkstyle/build passes all 276 existing JVM
  tests; Java 8 Forge passes all 116 tests, including pass-through delegation/copying and the frozen Scala consumer.
  Both Java 8 public surfaces match the saved `13ec2f5` reference. All 39 generated dump names and SHA-256 hashes
  match, and all 185 retained compiler/model/base-factory/generator types have identical disassembly. Only five
  private closures disappear, reducing the packaged inventory from 460 to 455 classes. The existing ledger row now
  describes the retained Java forwarders; no new divergence was added. `multipart/asm/ScalaSignature.scala` is next
  as the remaining parser/nested-model unit, with ASM compiler algorithm changes still deferred.
- Extracted `ScalaSignature` table decoding, name and literal evaluation, collection and object/class lookup into one
  package-private Java helper. The Scala shell retains the complete nested model and five generic construction
  branches. That boundary is required: primitive literal case classes expose both primitive and erased `Object`
  `value()` methods, which Java cannot declare together, while Java misreads the outer parameter in five Scala 2.11
  generic inner constructors. A full model port is deferred until it has an explicit bridge strategy.
- The parser keeps direct case-class construction, path-dependent return types, Scala `List`/`IndexedSeq`/`Option`
  results, table mutation behavior, annotation grouping, signed literal conversion and the existing partial tag
  interpretation. All 69 retained signature types' public names, descriptors and generic declarations match the saved
  `1ad2b0f` reference. The disassembly of 188 otherwise unchanged model, compiler, analyser, factory and generator
  types is identical; all 39 generated dump names and SHA-256 hashes also match.
- No tests were added, per the current user instruction. Formatting/checkstyle/build passes all 276 existing JVM
  tests and Java 8 Forge passes all 116 tests, including the external Scala-trait path. Four private parser/lookup
  closures are replaced by one Java helper, reducing the packaged inventory from 455 to 452 classes and leaving 206
  Java files plus 9 Scala files / 1,883 nonblank Scala lines. No new compatibility divergence needs a ledger entry.
  `multipart/asm/StackAnalyser.scala` is the next bounded target; keep its control flow and nested model separable.
- Backfilled, on request, the characterization tests the six ports from `b45527e` to `8581d30` had skipped. Six new
  suites in `src/test/java/codechicken/multipart/asm/` add 32 plain-JVM tests, taking that suite from 276 to 308.
  `ByteCodeReader` and the signature parser are covered by behavior; `ScalaSigReader` by round trips, annotation
  lookup and its replaced-value result. Debug initialization and generator execution need Forge, so those tests
  pin public surfaces, fields and bytecode constants and calls, leaving generated
  tiles, pass-through delegation and Java-trait `copyFrom` to the Forge suite.
- Ran the backfill against `src/main` restored from `1faf0dd`: it compiles against the untouched Scala and 45 of 46
  tests pass. The only failure is `MultipartMixinFactory`'s facade method set, which is the four additive static
  forwarders already in the ledger. Two assertions were relaxed to hold on both trees: the facade private
  constructors Scala never emitted, and the `DebugPrinter$` directory cleanup Scala emitted as a closure class.
- Two behaviors were pinned that inspection had not recorded. `ScalaSigReader.encode` drops the final 7-bit group, so
  a decode round trip is exact only when that group is empty; a payload ending in a high-bit byte does not survive
  it. `ScalaSignature.evalS` throws `MatchError` on an unknown tag while `eval` returns the table entry unchanged.

### 2026-09-03

- Addressed the three backfill review findings without changing production code. The lossy signature round trip now
  asserts the exact reference bytes `[0, 62]`, rather than accepting any result different from `[0, 0xfe]`.
- Replaced the two presence-only `DebugPrinter` JVM checks with three Forge behavior tests for both config modes:
  directory creation, immediate-child-only startup cleanup, dump gating/naming/content and cumulative 16,000-byte
  logging boundaries (including an empty input and a single input crossing two boundaries). An isolated copy changes
  only the hard-coded output path to `@TempDir`; config and logger state are restored and live dev dumps are untouched.
  A temporary fault-injection run removing deletion and reversing dump/log guards failed all three new printer tests;
  those production edits were then restored.
- Replaced the `copyFrom` call-presence check with three Forge tests invoking the actual completer. They assert
  byte-for-byte no-ops for empty/transient-only nodes and an existing method, then exact emitted super-call/guard/field
  order, transient exclusion and idempotence for mixed fields. Corrected the coverage claims: singleton construction
  is headless, but generation reaches Forge through `ObfMapping`; earlier dump comparisons were one-off checks.
- Verification baseline is now 305 JVM / 122 Forge tests: three weak JVM checks moved to six focused Forge checks.
  Formatting/checkstyle/build and the Forge suite pass; all 39 generated dump hashes and the dev config are unchanged.
  No production ABI, source counts or next target changed. `multipart/asm/StackAnalyser.scala` remains next, using
  explicit migration-test authorization and separate characterization and refactor commits.
- Characterized `StackAnalyser` against untouched Scala in 15 plain-JVM tests, committed separately as `a6aca65`.
  The baseline build passed 320 JVM tests and the Forge suite passed 122. The new tests exercise receiver/parameter
  initialization, narrow/wide slot aliases, partial failures, every duplication opcode, typed constants, case-class
  equality/copy/products, instruction provenance, loads/stores/increments, arithmetic/casts, arrays, fields, calls,
  branches/switches/returns, handler precedence, unsupported-node behavior and overridable default-argument dispatch.
- Extracted analyser control flow (`setL`, `pop`, `insert`, `popArgs`, `visitInsn`) into package-private Java
  `StackAnalyserLogic`. The Scala shell retains construction/state, default-argument bridges, simple accessors and
  the complete nested model. An isolated Scala 2.11.5 probe rejects a Java class alongside its same-name Scala
  companion; completing the model requires a coordinated class/companion conversion rather than replacing the class
  independently. The retained `ScalaSignature` model and its primitive/erased bridges are untouched.
- Preserved current opcode behavior, including wide `DUP2` expansion, int-conversion/comparison/`INSTANCEOF` result
  types, reference-array descriptor construction, reversed multidimensional size capture and `NEWARRAY`'s existing
  `MatchError`. Virtual calls still use the Scala default-argument getters. No compiler algorithm, optimization or
  unrelated bug fix is included.
- Saved the pre-port dev jar, Scala source, test reports and generated dumps under ignored
  `run/migration-stack-analyser-reference/`. All 40 named analyser surfaces match by member name, descriptor,
  modifiers, generic declarations and private fields; the other 39 named model/companion classfiles are byte-identical,
  preserving their serialized and Scala-facing behavior. All 199 retained ASM/generator disassemblies and all 39
  generated dump names/hashes match. An additional one-off comparison of 8,960 opcode/node/stack combinations matches
  expression trees, types, aliases, instruction bindings, locals, exceptions and partial mutation.
- Formatting/checkstyle/build and Forge pass, including a clean build after stopping Gradle: 320 JVM / 122 Forge
  tests, zero failures/errors/skips. All characterization tests remain unchanged after the port. The forced Scala
  compilation version guard is retained, and all five `@Mod` version annotations match both clean packaged jars.
  Two unreferenced traversal closures become one Java helper (452 to 451 packaged classes); sources now total 207
  Java files and 9 Scala files / 1,726 nonblank Scala lines. No new compatibility divergence is introduced. Next is
  `ASMMixinCompiler.scala`, bounded to `ClassInfo`/`MethodInfo` metadata lookup and traversal, with fresh characterization
  before touching its Forge-initialized state and no trait-rewriting algorithm changes.
- Characterized the compiler metadata unit before modification in `abdf0b0`: six JVM cases cover hierarchy order and
  diamond duplicates, parent-view capture/laziness, strict/view concatenation, virtual selection and short-circuiting,
  mutable node metadata, reflection order/descriptors/exceptions and case-class outer owners. Five Forge cases cover
  bytecode versus reflection roots, exact string-key caching and name-only node overloads, null/failure caching,
  `internalDefine` invalidation and Scala trait/companion metadata. Untouched Scala passes 326 JVM / 127 Forge tests.
- Extracted metadata lookup, cache population, parent flattening, method concatenation/selection, interface/method
  mapping and exception-name copying into package-private Java `ClassInfoLookup`. The same Scala collection operations
  and builders preserve result representations, laziness, traversal order and virtual dispatch. The nested model,
  scalar accessors, state, implicit overloads and construction callbacks remain in Scala; no compiler composition,
  trait registration/rewriting, stack-analysis or signature-decoding algorithm changed.
- Found and retained two necessary source bridges. javac cannot name the concrete metadata classes nested under
  `ClassInfo$`: it expects an extra `$` absent from their existing binary names. Scala's `IterableLike.view()` also
  has an `Object` generic return signature but an `IterableView` descriptor; calling it from Java emits a nonexistent
  `Object`-returning method. Construction and the initial `.view` call stay Scala. Path-dependent `ClassSymbolRef`
  travels through an `Object` parameter and is cast back only at its retained Scala construction boundary.
- Preserved existing quirks exposed by characterization: dotted/slashed keys have independent metadata cache entries,
  `internalDefine` invalidates only the normalized key, null names are cached as null while load failures are not,
  bytecode roots can return `Some(null)`, and the `StackAnalyser$` companion's superclass/interface queries throw
  `ClassCastException` in the current signature model. These are unchanged behavior and do not add divergence entries.
- Saved the pre-port source/jar, reports and 40 generated dumps in ignored `run/migration-class-info-reference/`.
  All 16 named compiler APIs match by public/protected member names/descriptors/modifiers/generic declarations and
  private fields. All four metadata case-class/companion serialization IDs match. All 205 other named compiler
  methods and 36 algorithm closures match after normalizing private closure and captured-variable numbering; 137
  ASM/generator classes outside the compiler have identical disassembly. All 40 dump names/hashes match, including
  the new deterministic cache-invalidation fixture; the local dev configuration is unchanged.
- Formatting/checkstyle/build and Forge pass, including a clean build after stopping Gradle: 326 JVM / 127 Forge,
  zero failures/errors/skips, with the characterization tests unchanged after the port. The forced Scala compilation
  guard is retained and all five `@Mod` annotations match both clean packaged jar versions. Private implementation
  closures become the helper/callbacks and the packaged inventory falls from 451 to 450. Sources now total 208 Java
  files and 9 Scala files / 1,714 nonblank Scala lines. Next is the bounded descriptor/bridge-emission helper unit in
  `ASMMixinCompiler.scala`: `seperateDesc`, `staticDesc`, `finishBridgeCall`, `writeBridge` and `writeStaticBridge`, with
  fresh characterization and no composition/trait-rewriting algorithm changes.
- Added eight Forge behavior tests for the descriptor/bridge helpers and committed them separately as `5d611e0`.
  All pass against untouched Scala: descriptor splitting and receiver insertion, every argument/return category,
  local-slot widths, invocation flags, instruction order, maxima, partial visitor failures, overridable metadata
  getter order and descriptor rereads. Generated executable bridges verify virtual, special, interface and
  Scala-style static-helper dispatch with mixed wide/reference arguments. The original compiler singleton requires
  a Forge `LaunchClassLoader`; these tests therefore use the existing Forge harness. Baseline: 326 JVM / 135 Forge.
- Extracted only `seperateDesc`, `staticDesc`, `finishBridgeCall`, `writeBridge` and `writeStaticBridge` into Java
  `ASMBridgeEmitter`, retaining their Scala entry points and exact ABI. No composition, trait-rewriting, stack or
  signature-decoding algorithm changed. Existing descriptor-validation gaps, malformed-input exception types,
  unnormalized owner strings, independent bridge/callee descriptors and callback order are preserved.
- Saved the pre-port jar/source, reports and 40 generated dumps under ignored `run/migration-bridge-reference/`.
  All 16 named compiler APIs match by names/descriptors/modifiers/generic signatures and private fields; 14 named
  classfiles are byte-identical. All 214 other named compiler methods, 35 algorithm closures and 143 ASM/generator
  disassemblies outside the compiler match. All 40 dump names/hashes match and the dev configuration is unchanged.
  One private bridge closure is replaced by the Java helper, keeping 450 packaged classes. No new ledger entry is
  needed. Sources total 209 Java files and 9 Scala files / 1,689 nonblank Scala lines. Next: `ASMMixinCompiler.getSuper`
  recognition/lookup, with fresh characterization and no algorithm fixes mixed into its port.
- Formatting/checkstyle/build and the full Forge suite pass, including a clean build after stopping Gradle:
  326 JVM / 135 Forge, zero failures/errors/skips. The eight characterization tests are unchanged after the port,
  and the clean jar repeats the API/disassembly/dump matches above. The forced Scala-compilation version guard is
  retained; all five `@Mod` annotations match both packaged jar versions. External Scala-trait coverage remains green
  and the existing manual client checks and Java-source bridge limitations remain outstanding.
- Added six Forge characterization cases for `ASMMixinCompiler.getSuper`, passing on untouched Scala and committed
  separately as `c0df2e0`: owner/name filter short-circuiting, greedy Scala super-name stripping, exact inherited
  signature selection and visibility, receiver recognition, argument indexing, failure paths, stack preservation,
  virtual getter/callback order, returned option identity and descriptor rereads. Baseline: 326 JVM / 141 Forge.
  The compiler singleton requires Forge initialization, so these behavior tests use its existing harness.
- Moved only `getSuper` into the existing Java `ClassInfoLookup`, retaining the public Scala entry point and
  `Option.flatMap` callback dispatch. The current argument-count/stack-slot mismatch for wide arguments remains,
  as do the lack of invocation-opcode and `This.owner` validation and the limited target-owner check. The caller's
  `INVOKESPECIAL` guard, all trait rewrites and metadata/model behavior remain unchanged; algorithm fixes are separate.
- Saved reference source/jar, reports and 40 generated dumps in ignored `run/migration-super-reference/`. All 16 named
  compiler APIs retain their names/descriptors/modifiers/generic signatures and private fields; 14 named classfiles
  are byte-identical. All 218 other named compiler methods, 34 algorithm closures, 17 existing metadata-helper methods
  and 143 other ASM/generator disassemblies match. All dump names/hashes match. One private Scala closure becomes a
  Java callback, leaving 450 packaged classes and no new divergence entry. Sources total 209 Java files and 9 Scala
  files / 1,675 nonblank Scala lines. Next: `ASMMixinCompiler.listSideOnly` annotation filtering, characterized first.
- Formatting/checkstyle/build and Forge pass, including clean verification after stopping Gradle: 326 JVM / 141 Forge,
  zero failures/errors/skips. Characterization tests remain unchanged. The clean jar repeats the API/disassembly/dump
  matches, the local dev config is unchanged, and all five `@Mod` versions match both packaged jar versions with the
  forced Scala-compilation guard retained. External Scala-trait tests remain green; existing manual client checks and
  Java-source model-bridge limitations remain outstanding.
- Reviewed the reported `TileMultipart` compatibility findings against `cacc9a3^`. Confirmed all three equality
  changes and the dropped virtual light query. Restored null-safe Scala equality in change notifications and
  replacement/removal filtering, removed all equal entries, and retained Scala `contains`/`indexOf` behavior.
  Removal still reports the first matching index and invokes hooks/detaches only the requested part, as Scala did.
- Restored `getLightValue()` before `preRemove()` and corrected the related early/stale snapshot reads: filtering
  now sees list updates from those callbacks, while empty/ticking decisions see updates from removal callbacks.
  Seven JVM regression tests cover these behaviors, including light-query exceptions and missing-part rejection.
  Six failed against the unfixed Java port; all seven pass against the complete original Scala class and fixed Java.
  The reference class needed only explicit Java getter/setter, singleton and boxed-function call syntax for migrated
  dependencies. Sources, compilation/test logs and reports are saved in ignored `run/tile-compatibility-review/`.
- Added brief comments explaining the profiled direct-list traversal and arbitrary-`Seq` fallback. No traversal or
  compiler algorithm changed. The existing `NEWARRAY` limitation, forced Scala-compilation guard and manual client
  checklist require no change in this review. All 450 class APIs match by names/descriptors/modifiers/generic signatures
  and private fields. All 40 generated dumps retain the same instructions: 39 hashes are exact, with only debug line
  numbers changed in the redstone helper by its added comment. These fixes restore the reference behavior and need
  no divergence entry. The next migration target remains `ASMMixinCompiler.listSideOnly`.
- Clean formatting/checkstyle/build and the full Forge suite pass after stopping Gradle: 333 JVM / 141 Forge tests,
  zero failures/errors/skips. The clean jar repeats the API/generated-output checks above; all five `@Mod` annotations
  match both packaged jar versions. The forced Scala-compilation guard is unchanged. Review corrections were committed
  as `5af333c` and fast-forwarded onto `algent/java`; subsequent migration work continues on that branch.
- Added six Forge characterization tests for `ASMMixinCompiler.listSideOnly`, passing against untouched Scala and
  committed separately as `b288d3d`. A compiled Scala fixture covers actual signature annotations; synthetic signatures
  freeze exact-name selection, current-side exclusion, unknown/null enum names, owner-name deduplication, immutable
  result snapshots, short-circuiting and missing/wrong/null-value failures. Virtual accessor mutations and exceptions
  prove all filter predicates run before any selected owner is read. Baseline: 333 JVM / 147 Forge. The original
  singleton and side initialization require the existing Forge harness.
- Extracted only annotation filtering into the existing Java `ClassInfoLookup`, retaining the Scala entry point and
  separate Scala collection `filter`, `map` and `toSet` dispatch/builders. Trait registration, rewriting, composition
  and signature decoding remain unchanged. Unknown enum names, null owner names and malformed-input failures keep
  their original behavior; no algorithm fixes are mixed in and the external Scala-trait/model bridges remain intact.
- Saved the pre-port source/jar, reports and 40 generated dumps under ignored `run/migration-side-only-reference/`.
  All 16 named compiler APIs match by member names/descriptors/modifiers/generic signatures and private fields.
  All 218 other named compiler methods, 32 algorithm closures, 18 existing metadata-helper methods and 144 other
  ASM/generator disassemblies match. Retained compiler models have shared-source debug line shifts only. All 40 dump
  names/hashes match exactly. Two private Scala closures become two Java callbacks, leaving 450 packaged classes;
  the shared compiler entry covers this and no new divergence entry is needed. Sources total 209 Java files and
  9 Scala files / 1,664 nonblank Scala lines.
- Formatting/checkstyle/build and Forge pass, including clean verification after stopping Gradle: 333 JVM / 147 Forge,
  zero failures/errors/skips. The six characterization tests are unchanged after the port. The clean jar repeats the
  API/disassembly/dump matches; the local dev config and forced Scala-compilation guard are unchanged. All five `@Mod`
  versions match both packaged jar versions. External Scala-trait tests remain green; existing manual client checks
  and Java-source model-bridge limitations remain outstanding. Next: Scala-trait registration metadata,
  `getAndRegisterParentTraits` and `registerScalaTrait`, characterized first with no registration algorithm changes.
- Added twelve Forge characterization tests for Scala-trait registration, passing against untouched Scala and
  committed separately as `d2276c4`. Real compiled Scala traits exercise parent/field/method/super metadata and side
  selection; synthetic signatures pin cached identity/nulls, lookup-before-registration ordering, duplicate parents,
  partial caches after failure, owner value equality, filtering precedence, first exact method identity, preceding
  accessor selection and mutation of the publication key by a metadata callback. Baseline: 333 JVM / 159 Forge.
- Extracted `getAndRegisterParentTraits` and `registerScalaTrait` into Java `ScalaTraitRegistration`, retaining their
  exact Scala entry points and four small callbacks for nested metadata type tests/casts and accessors. A javac probe
  confirms it expects `ASMMixinCompiler$ClassInfo$$ScalaClassInfo` for the existing nested model, whose binary name has
  only one `$` between `ClassInfo` and `ScalaClassInfo`. The model stays in Scala. The class-symbol callback uses
  `Object` at the joint-compilation boundary and the retained `ClassSymbolRef` inside Java. No named model or bridge
  is removed; collection dispatch/builders, cache publication and registration algorithms are unchanged.
- The compiled fixtures exposed the existing Scala `String` alias limitation: its parameter descriptor becomes
  `Lscala/Predef/String;`, which cannot match the classfile method. A dedicated test freezes the exact registration
  failure, while the successful fixture spells the type `java.lang.String`. Missing accessors/methods and partial
  parent registration on failure remain unchanged. These are reference behaviors, not new divergences or fixes.
- Saved pre-port source/jar, reports and 40 generated dumps in ignored `run/migration-scala-trait-reference/`. All 16
  named compiler APIs match by member names/descriptors/modifiers/generic signatures and private fields; 14 named
  classfiles are byte-identical. All 217 other named compiler methods, 28 other algorithm closures, 19 existing
  metadata-helper methods and 147 other ASM/generator disassemblies match. All 40 dump names/hashes match exactly.
  The Java helper/callbacks and retained Scala type bridges change the packaged inventory from 450 to 455 classes;
  private artifacts are covered by the shared compiler entry, so no new ledger entry is needed. Sources total 210
  Java files and 9 Scala files / 1,629 nonblank Scala lines.
- Formatting/checkstyle/build and Forge pass, including clean verification after stopping Gradle: 333 JVM / 159 Forge,
  zero failures/errors/skips. The twelve characterization tests are unchanged after extraction. The clean jar repeats
  the API/disassembly/dump comparisons; the local dev config and forced Scala-compilation guard are unchanged. All
  five `@Mod` versions match both packaged jar versions. External Scala-trait tests remain green; existing manual
  client checks and Java-source model-bridge limitations remain. Next: `getBytes`, `classNode` and `internalDefine`
  class-byte loading/cache helpers, characterized first and without loader/cache algorithm changes.
- Investigated the 2026-09-03 ProjectRed placement crash on `60d060a`. The failure occurs in multipart TESR rendering,
  not registration: the earlier client-trait port (`970e888`) left an `INVOKEVIRTUAL` call to
  `TileMultipartClient.hasDynamicParts()Z` in `MultipartRenderer$`, while Forge exposes that type as an interface.
  A raw-jar audit of calls/field accesses to all eight Java-authored tile trait inputs found this one unsafe external
  call. The recent Scala-trait registration extraction is not its cause.
- Added four Forge regression cases executing the shipped renderer method body against generated client tiles.
  Only GL/pass services and the final drawing callback are recorded, because the dedicated server strips the actual
  `TMultiPart.renderDynamic` method. Static and empty early returns, non-client rejection, render-state setup and
  exact dynamic coordinates/frame/pass are covered. The final tests were rerun against the broken renderer: both
  nonempty cases raise the same `IncompatibleClassChangeError` as the supplied crash. This closes the call-site gap
  left by earlier client-tile tests, which invoked trait methods through method handles.
- Added a default-false `TileMultipart.hasDynamicParts()` base hook and routed the renderer's flag query through it.
  Generated client tiles already supply the overriding getter. The original checked client-trait cast and guards
  remain; no generator algorithm or runtime trait interface changes. The additive base method is recorded in the
  existing divergence entry. Reference source/jar, reproduction reports and scripts are saved under ignored
  `run/renderer-compatibility-review/`; the manual checklist records the failed full-client check and required retest.
- Clean formatting/checkstyle/build and Forge pass after stopping Gradle: 333 JVM / 163 Forge tests, zero
  failures/errors/skips. All 455 class APIs match except the new base hook, and 3,713 other method bodies match.
  No unsafe external calls/field accesses to the eight transformed Java tile inputs remain. All 40 generated outputs
  keep their instructions: 34 names/hashes are exact; six composites are renumbered by the new tests' earlier client
  tile construction. The dev config and forced Scala-compilation guard remain unchanged; all five `@Mod` versions
  match both packaged jars. The user subsequently confirmed that placement with the supplied fix no longer crashes;
  the broader static/dynamic drawing and part-update checklist remains open.
- Added ten Forge characterization tests for `ASMMixinCompiler.getBytes`, `classNode` and `internalDefine`, passing
  against untouched Scala and committed separately as `4928cdf`. A recording LaunchClassLoader exercises the real
  reflective transformer chain; the real FML remapper is tested with a temporary mapping/environment flag. Fixtures
  restore loader, caches and flags. They pin dotted/slash names, remapper input and transformer argument order,
  exclusion short-circuiting, raw-array identity, nulls, reflection wrapping, expanded frames, fresh node parsing,
  cached parse failures versus retried load failures, normalized metadata invalidation and publication before dump
  failure. `internalDefine` still stores bytes without defining a JVM class. Baseline: 333 JVM / 173 Forge.
- Extracted those three helpers into Java `ClassBytes`, retaining the Scala singleton, exact entry points and private
  cache fields. Scala `find`/`getOrElseUpdate` dispatch remains, as do the load-before-exclusion and
  publish/invalidate-before-dump ordering. The helper resolves the retained `MODULE$` internally: joint compilation
  cannot expose the Scala-authored `ASMMixinCompiler$` as a Java parameter to a Scala caller. This follows the earlier
  metadata helper pattern and changes no consumer API. Reflective class definition, startup initialization, trait
  registration/rewriting, composition and compiler algorithms are unchanged.
- Saved pre-port source/jar, reports, 41 generated dumps and reproducible checks in ignored
  `run/migration-class-bytes-reference/`. All 423 non-closure class APIs match by names/descriptors/modifiers/generic
  signatures and private fields, including all 16 named compiler APIs. All 3,615 other method bodies and 30 other
  compiler closures match after normalizing private closure numbering. All 41 generated dump names/hashes match
  exactly. Two private Scala closures become two Java callbacks plus their helper, taking the packaged inventory
  from 455 to 456 classes. The shared compiler ledger entry covers this, with no new effective divergence. Sources
  total 211 Java files and 9 Scala files / 1,597 nonblank Scala lines.
- Formatting/checkstyle/build and Forge pass, including a clean build after stopping Gradle: 333 JVM / 173 Forge,
  zero failures/errors/skips. All ten characterization tests remain unchanged. Clean APIs and dumps repeat the
  matches above; the dev config and forced Scala-compilation guard are unchanged. All five `@Mod` versions match
  both packaged jar versions. External Scala-trait tests remain green; existing manual client checks and Java-source
  model/trait limitations remain. Next: `ASMMixinCompiler.define`, characterized first for publication/debug
  accounting, reflective definition and failure ordering, without algorithm changes.
- Added ten Forge characterization tests for `ASMMixinCompiler.define`, passing on untouched Scala and committed
  first as `4d59900`. They execute real JVM definitions in isolated LaunchClassLoaders and restore compiler caches,
  loader/reflection state and debug state. Tests freeze bytecode-name versus cache-key handling, delayed class
  initialization, exact byte publication, metadata invalidation, byte accounting and failures before/after reflection.
  Real duplicate definitions remain `InvocationTargetException` wrapping `LinkageError`; direct linkage errors from
  reflective class initialization exercise the original case-sensitive duplicate guard, including its null-message
  `NullPointerException`. Dump/accounting failures remain outside that guard. These quirks are preserved, not fixed.
- Moved only `define` into the existing Java `ClassBytes`, retaining its exact Scala entry point and the loader's
  startup initialization. Class composition, trait rewriting and external Scala-trait/model bridges are unchanged.
  Reference source/jar, reports, 42 dumps and verification scripts are in ignored `run/migration-define-reference/`.
  The 456-class inventory is unchanged; all 426 non-closure APIs (including 16 named compiler APIs), 3,628 other method
  bodies and 30 compiler closures match. All 42 generated dump names/hashes match exactly. No new divergence is needed.
- Formatting/checkstyle/build and Forge pass, including clean verification after stopping Gradle: 333 JVM / 183 Forge,
  zero failures/errors/skips. The characterization tests, dev config and forced Scala-compilation guard are unchanged;
  all five `@Mod` versions match both packaged jar versions. Sources total 211 Java files and 9 Scala files / 1,583
  nonblank Scala lines. Next: `ASMMixinCompiler.mixinClasses`, characterized for composition/constructor/dispatch and
  generated output before extraction; keep compiler algorithm fixes separate.

- Consolidated the active plan/handoff after the reflective-definition port. The dated findings remain intact in
  this history, duplicated completed-port handoff summaries were removed, and current phase/status text was refreshed.
  The manual checklist now names verified consumer items, including the inverted ProjectRed lamp requirement.

### 2026-09-03 — composite class generation

- Added eight Forge characterization tests for `ASMMixinCompiler.mixinClasses`, passing on untouched Scala and
  committed first as `1c9cd65`. They execute generated JVM classes and freeze empty selection, constructor forwarding,
  diamond linearization, initialization, field storage and mangling, method/super dispatch, covariant bridges,
  implemented-interface order and failures before or during definition. The reference also confirms the existing
  verifier failure for generated `long` and `double` field getters whose maximum stack remains one.
- Moved only composite generation into Java `MixinClassGenerator`, retaining the Scala entry point and the unusual
  public compiler-local `allParents` helper descriptor. External Scala-trait registration, Java-trait rewriting and
  compiler algorithms are unchanged. The 456-class reference becomes 442 classes: 16 unreferenced target closures
  become the Java helper and one private callback. The shared classfile ledger covers this with no new effective
  divergence.
- Saved the pre-port source/jar, reports, 79 generated outputs and checks in ignored
  `run/migration-composition-reference/`. Clean verification after stopping Gradle matches all 426 non-closure APIs,
  3,628 non-target method bodies, 13 other compiler closures and every generated output name/hash. Formatting,
  checkstyle, build and Forge pass: 333 JVM / 191 Forge, zero failures/errors/skips. All five packaged `@Mod` versions,
  the forced Scala-compilation guard and dev config remain correct. Sources total 212 Java files and 9 Scala files /
  1,444 nonblank Scala lines. Next: `ASMMixinCompiler.registerJavaTrait`, characterized before extraction; abstract
  mixins, side-only filtering and the wide-field defect remain separate compiler changes.
