# Migration history

Historical findings from the Scala-to-Java migration. Counts, next-target suggestions and outstanding work below
describe the state at the time of each entry; use the [working handoff](HANDOFF.md) for current
state and the [plan](README.md) for remaining gates. Routine sessions need not read this entire log.

The former detailed per-port handoff is also available with `git show cf8b2f9:docs/migration/HANDOFF.md`.
Keep new findings here; summarize only current state and constraints in the handoff. Intentional compatibility
differences belong in the [divergence ledger](DIVERGENCES.md).

Per-entry green-build status was removed in a later pass: superseded intermediate test counts, generated-dump
comparisons, formatting/checkstyle results and stale "X is next" pointers. Findings, decisions, rejected options and
ABI observations were kept. A passing build was the baseline expectation for every entry, so its absence here does not
mean a check was skipped. The last two sections hold records moved from the retired profile and downgrader documents.

### 2026-09-09 - Scheduler unload and inventory size regressions

- Restored the original Scala mutable-set filtering in `TickScheduler.WorldTickScheduler.postTick`, matching the
  retained-collection approach in `PacketScheduler`. Java's fail-fast set iterator threw when a scheduled callback
  unloaded a chunk. The regression test covers multiple unloading callbacks and an unrelated delayed tick, including
  exactly-once delivery on later ticks. This restores the reference traversal rather than defining new unload ordering.
- Captured each inventory's size once in the slot-building pass of `JInventoryTile.rebuildSlotMap`, preserving the
  original two-pass evaluation. A 54-slot inventory now receives two size queries instead of 56. The generated-trait
  regression covers empty, single-slot and 54-slot inventories and checks every flattened slot's owner and local index.
- Both new tests failed before their respective fixes. Formatting, checkstyle, the build, all 577 JVM tests and all
  290 Java 8 Forge tests pass with zero failures/errors/skips. The added scheduler callback brings the packaged class
  count to 451; all remain Java 8 bytecode (version 52). No public API changes or TPS improvement are claimed.

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
- Removed the six `IDWriter$$anonfun$setMax$*` compiler artifacts from the packaged jar. Their disappearance and replacement with Java anonymous helper classes is recorded in `docs/migration/DIVERGENCES.md`; the supported `IDWriter` descriptors remain link-compatible.
- Added a frozen Scala 2.11.5 consumer compiled against the reference dev jar. Its constructor and inherited default method call `JPartialOcclusion$class` directly, so it detects the linkage failure that freshly recompiled tests would miss.
- Replaced `TPartialOcclusion.scala` with Java implementations of `PartialOcclusionTest` and `JPartialOcclusion`. The interface keeps its name and method descriptors, while `allowCompleteOcclusion()` is now a Java default method and the deprecated `$class` helper remains for old Scala binaries.
- Passed all ten partial-occlusion behavior/API cases, the frozen Scala binary consumer, all 27 plain-JVM tests, a clean build, and both Java 8 Forge server checks. The existing marker-interface registration remains unchanged and continues to drive runtime tile generation.
- Completed the downstream ABI inventory by constant-pool scan of 240 mod jars in GTNH daily `2026-08-14+678`, recorded in `docs/migration/COMPATIBILITY.md` with the scanner in `tools/AbiScan.java` and the frozen baseline in `src/test/fixtures/abi/`. GitHub code search was rejected as an oracle because it indexes default branches only and cannot see reflection strings or closed-source consumers.
- Found 27 consumer jars referencing 35 inherited types, 255 exact member descriptors, 76 other types, and 20 reflective string constants.
- Answered open decision 2: third-party Scala traits are registered externally. ProjRed passes its own `LightMicroblock` Scala trait to `MicroblockGenerator.registerTrait`, so `registerScalaTrait` and ScalaSignature decoding must survive Phase 7.
- Answered open decision 4: Scala runtime removal is not achievable for the first Java release. ProjRed, OpenComputers, ProjectBlue, and ForgeRelocationFMP link against 16 static methods on 8 trait `$class` helpers plus 17 companion `MODULE$` singletons.
- Found only five referenced descriptors containing Scala types, and `TileMultipart.jPartList()` already outweighs `partList()` in downstream use. The Scala bridge surface is much smaller than the initial audit assumed.
- Found zero downstream references to `IDWriter`, so its four retained Scala function accessors are not load-bearing and can be dropped.
- Removed both speculative bridges the inventory proved dead: the four `IDWriter` Scala function accessors and the whole `JPartialOcclusion$class` helper, along with the `ReferenceScalaPartialOcclusion` fixture that only existed to verify the helper. `JPartialOcclusion` itself and both of its method descriptors are unchanged.
- `IDWriter` now selects a carrier width instead of storing Scala closures, removing the per-call `Integer` boxing and `Function1`/`Function2` allocation that the first port had preserved. The nine encoding cases still pass unchanged.
- Established the working rule for the remaining phases: check `docs/migration/COMPATIBILITY.md` before writing a bridge, rather than writing one reflexively for every converted file.
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
- Applied the default-versus-abstract rule to `TRandomUpdateTick.onWorldJoin` and got the failing answer: `TMultiPart` declares `onWorldJoin`, so a default would be shadowed by the superclass and the auto-registration would silently never run. It stays abstract, and implementors declare it and call `TickScheduler.loadRandomTick` themselves, which `RedstoneTorchPart` - the only implementor in this codebase - already did.
- Applied the bridge rule to `TRandomUpdateTick$class` and got a clean negative: no jar in the pack references `TRandomUpdateTick` in any form, and the frozen baseline contains exactly the eight `$class` helpers the inventory lists. No bridge was written, following the `IDWriter` precedent. The accepted cost is a recompiled-Scala-consumer break with no shipping consumer.
- Found that `IMicroMaterialRender` is implemented for every part solely by `TMultiPart`'s Scala-style `world`/`x`/`y`/`z`/`getRenderBounds` accessors. Renaming any of them to a bean accessor would silently unimplement the interface, so the names are now pinned by a characterization test rather than left to reviewer attention.
- Established that for interfaces carrying no implementation, the characterization is the shape: every member abstract and public, no superinterface, and the exact member set. That is enough to catch the two realistic failure modes, a stray default and a renamed accessor.
- Noted that `IRedstonePart.scala` is misleadingly named and is not a marker-trait file. It carries six traits plus `RedstoneInteractions`, whose `MODULE$` is load-bearing, and was moved out of the low-risk group.
- Historical documentation gap: the ports between `TItemMultiPart` and this entry - `TEdgePart`, `Saw`, both
  registries, `TileMultipart`, `TMultiPart`, `TickScheduler` and `BlockMultipart` - did not receive dated findings
  here. Use the handoff for their current status and git history for the original validation narrative; the divergence
  ledger now records only their effective compatibility differences.
- Ported `MultipartHelper`, `MultipartHelper$` and `MultipartHelper$IPartTileConverter` to Java. All three are public-member- and descriptor-identical to the reference and the full emitted class list is unchanged, so this port neither added nor removed a class.
- Kept `MultipartHelper$` on the strength of a reflective string constant alone. It is not among the 17 `MODULE$` singletons read from bytecode and no jar links against it, but guidenh names it, and the inventory warns that removing a companion breaks such consumers invisibly. This is the opposite call to `IconHitEffects$`, which was dropped because nothing referenced it in bytecode or by name, and it establishes that the `MODULE$` list alone is not sufficient grounds to delete a companion.
- Found a reusable characterization technique: a class that cannot class-initialize headless is a probe for control flow. `MultipartSaveLoad` reflects into `TileEntity`'s static maps through `ObfMapping` and always throws under a plain JVM, so `createTileFromNBT` returning null rather than raising proves the id guard short-circuits before the `loadingWorld` assignment. Assert `LinkageError` rather than the exact type, because the first attempt raises `ExceptionInInitializerError` and later ones raise `NoClassDefFoundError`.
- Confirmed that erasure does not weaken `IPartTileConverter.convert`. The cast to `T` compiles away, but the `ClassCastException` a mismatched tile produces comes from the synthetic bridge on the subclass overriding `convertMulti(T)`, which is the same mechanism Scala's `asInstanceOf[T]` relied on. It is now pinned by a test, because the code reads as though the check was lost.
- Added five Forge server tests, doubling that suite, because most of this class cannot run headless. They cover tile construction through the ASM generator, the NBT round trip through the save/load hooks, `registerTileConverter` appending to a Scala `MutableList` from Java, and `sendDescPacket` against a loaded chunk.
- Dropped the two commented-out blocks the reference carried, the `PlayerInstance.playersInChunk` reflection and the multi-tile `sendDescPackets`, rather than reproducing dead Scala as dead Java. The reason they existed, a missing forge access transformer, is now in the class javadoc.
- Read guidenh's actual source at `6137525` rather than inferring from its constant pool, and recorded the exact reflective member list in `docs/migration/COMPATIBILITY.md`. The scan could see the 20 names; only the source shows which members are looked up on them, and none of it is visible to the ABI diff.
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
  sees the same entry.
- Added four source-consumer structural guards without adding downstream mods as test dependencies. They pin GuideNH's
  two `BlockMicroMaterial` mixin targets, Et Futurum's mutable static button-orientation arrays, and Iguana's reflected
  `ItemSaw.harvestLevel` field.
- Reproduced Galacticraft's name-only method scan and froze its dangerous assumption: exactly one public
  `MicroMaterialRegistry.registerMaterial` method may exist, it must accept `(IMicroMaterial, String)`, and
  `BlockMicroMaterial(Block, int)` must remain reflectively constructible.
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
  redstone `IntRef`/closure traversal. Full methodology and rerun commands are in `docs/migration/README.md#phase-4b--measured-performance-pass`.
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
  jar, and `NonLocalReturnControl` slot rejection disappear.
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
  from 24.0 B to 0.0 B per call with 2.89x throughput.

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
- Actual static/dynamic rendering and particle appearance remain client-only manual checks.
- Characterized `MultipartCompatiblity` and `MCPCCompatModule` against untouched Scala. Three plain-JVM cases freeze
  both static facades, both `MODULE$` companions, the private Scala `Function4` field, default allow behavior and
  callback identity; two Forge cases freeze non-MCPC loading and the logged missing-hook fallback.
- Ported both singletons to four Java types with unchanged public names and descriptors. The callback still propagates
  reflection and cast failures unchanged, while the two Scala anonymous-function artifacts become private named Java
  callback classes. No frozen binary or audited source consumer names those implementation classes.
- This initialization-only hook is not a meaningful target for the focused allocation benchmark; successful MCPC
  integration remains environment-dependent.
- Characterized `MultipartMod` against untouched Scala. Two plain-JVM cases freeze both annotated singleton types,
  all ten lifecycle methods and annotations, plus the `MultipartPH.channel` companion descriptor and identity; two
  Forge cases freeze FML's companion mod instance, completed initialization and server-stop cleanup.
- Ported the singleton to an annotated Java facade/companion pair while retaining `modLanguage = "scala"`. The one
  source use of the object as a value now names `MultipartMod$.MODULE$` explicitly, leaving the compiled packet-handler
  field and accessor unchanged.
- Found that the inherited `MultipartProxy.postInit` static forwarder carries `@SideOnly(CLIENT)` and is stripped on a
  dedicated server. The Java companion therefore invokes `MultipartProxy$.MODULE$` directly, matching the reference
  Scala bytecode and allowing virtual resolution to reach the server implementation.
- Mod lifecycle dispatch is startup-only, so it is not a meaningful focused throughput or allocation target.
- Characterized `MultipartEventHandler` against untouched Scala. Two plain-JVM cases freeze both singleton types, all
  twelve public event methods, priorities and the client-only highlight boundary; three Forge cases freeze companion
  registration on both buses, chunk load/unload cleanup, queued watches and END-phase tick dispatch.
- Ported the singleton to a Java facade/companion pair with unchanged public names, descriptors and annotations. The
  proxy now names `MultipartEventHandler$.MODULE$` explicitly, preserving the exact object registered on both buses.
- Server ticking still passes the configuration manager's live player list through Scala's Java-list buffer adapter;
  no copy or new traversal was introduced.
- Characterized `MicroblockMod` against untouched Scala. Two plain-JVM cases freeze both annotated singleton types,
  all ten lifecycle/IMC methods, the mutable `angelicaCompat` accessors and shared identity; one Forge case freezes
  FML's companion mod instance and the completed microblock lifecycle.
- Ported the singleton to an annotated Java facade/companion pair while retaining `modLanguage = "scala"`. Lifecycle
  dispatch calls `MicroblockProxy$.MODULE$` directly, matching the reference bytecode and avoiding side-only static
  forwarders that Forge strips on a dedicated server.
- The internal client assignment now calls the preserved `angelicaCompat_$eq` method explicitly because recompiled
  Scala cannot apply property-assignment syntax to a Java-authored setter. The compiled accessor ABI is unchanged.
- Startup and lifecycle dispatch are not a meaningful focused throughput or allocation target.
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
- This event-only adapter is not a meaningful focused performance target.
- Characterized the ForgeMicroblock packet-handler unit against untouched Scala. Seven plain-JVM cases freeze the
  shared channel base, both facade/companion surfaces and exact packet interfaces, the integrated-server registry
  skip, ordered missing-material disconnect, unknown-type `MatchError` and no-op server callback. One Forge case
  freezes the handshake channel, type and complete material-ID payload.
- Ported all five emitted packet-handler classes to Java and changed the two Scala proxy registrations to pass the
  companion singletons explicitly. Every callable public member and the emitted class list match the reference; the
  registry channel and wire format are unchanged.
- After the local EULA was accepted, all 87 Java 8 Forge dedicated-server tests pass, including the new registry
  handshake.
- Characterized `MultipartSaveLoad` against untouched Scala. Three plain-JVM cases freeze the static facade,
  load-bearing companion, private fields and exact dummy class shape. Four Forge cases freeze ProjectRed-style binary
  linkage, both reflected vanilla maps, converter precedence/deletion and saved multipart reconstruction.
- Ported the singleton to a Java facade/companion pair and kept the dummy as the static facade member
  `MultipartSaveLoad.TileNBTContainer`, which emits the literal `MultipartSaveLoad$TileNBTContainer` binary name.
  Nesting it under the companion would emit the wrong double dollar. Every callable public member matches the
  reference. Only the unreferenced compiler-generated `$$anonfun$1` closure disappeared.
- The next medium-risk target is `MissingMicroMaterial.scala`; its real icon/render paths remain client-manual work.
- Characterized `MissingMicroMaterial` against untouched Scala. Three plain-JVM cases freeze the exact facade and
  companion surfaces, `MODULE$`, inert material values, interface defaults and all client-only boundaries. One Forge
  case freezes side stripping and the exact singleton registered under the missing-material name and ID.
- Ported the singleton to a Java facade/companion pair and changed both Scala object-value uses to name `MODULE$`
  explicitly. The placeholder key, stone item, sound, strength, resistance and missing-texture render pipeline are
  unchanged; only the actual client rendering remains manual.
- This inert singleton is not a meaningful focused performance target.
- Characterized `DefaultContent` against untouched Scala. One plain-JVM case freezes its one-method static and
  companion surfaces. Two Forge cases freeze the five microblock factories and IDs, all 103 sorted built-in
  materials, their exact implementation types and the complete legacy-name remap table.
- Ported the singleton to a Java facade/companion pair while continuing to use the existing `BlockMicroMaterial$`
  overloads and Scala ranges. Registration contents, ordering and the historical meta-0-only `log2`/`leaves2`
  overload behavior are unchanged.
- Pre-init-only registration is not a meaningful focused performance target.
- Characterized `GrassMicroMaterial` and `TopMicroMaterial` against untouched Scala. Five plain-JVM cases freeze both
  constructors, the grass overlay accessor used by UtilitiesInExcess, the top default-argument facade/companion,
  common-side boundaries and horizontal/side UV plus colour-pipeline routing. One Forge case freezes their registered
  blocks/meta and the exact surface left on a dedicated server.
- Ported both classes and the default-argument companion directly to Java. Grass keeps its uncoloured base side pass,
  coloured top and height-adjusted side overlay; `TopMicroMaterial` keeps coloured horizontal faces and translated
  side UVs. All three emitted binary names and every callable public descriptor match the Scala reference.
- A clean compile was required to evict the deleted Scala classes before the unchanged characterization could test the
  Java implementation. The render path retains the same per-side transformation work, so no separate performance claim
  is made.

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
- Added Phase 10 for upstream consumer cleanup. Editable consumer projects can migrate upstream; Extra Utilities
  remains an active supported consumer whose compatibility must be retained. The pattern is a three-step ratchet:
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
- Startup registration and two bit-packing helpers are not a meaningful focused performance target.
- Characterized `MicroblockProxy` against untouched Scala. Four plain-JVM cases freeze its four-type hierarchy,
  complete facade/companion ABI, eight mutable server fields, protected Scala saw list and exact lazy-renderer shape.
  Two Forge cases freeze side stripping, inherited server lifecycle resolution, item/ore/recipe registration and saw
  order.
- Ported the hierarchy and facade to four Java types. The Scala `MutableList` remains for binary compatibility, the
  client renderer retains its field-only side annotation and lazy bitmap, and the saw-renderer closure becomes a
  direct iterator loop.
- This startup-only proxy is not a meaningful focused performance target; `multipart/handler/packethandlers.scala` is
  the last Scala handler unit and is next.
- Characterized the ForgeMultipart packet-handler unit against untouched Scala. Eleven plain-JVM cases freeze all six
  emitted retained types, exact facade/companion interfaces, channel identity, private prefixed state accessors,
  ordered registry and desync disconnects, control-key packets, coordinate streams and both update terminators.
- Ported the five top-level types and nested byte stream directly to Java. The three Scala mutable collection
  descriptors remain where reflection already observes them, while direct loops replace the anonymous `MultiMap` and
  thirteen closure classes without changing watcher, batching, framing or cleanup behavior.
- All six retained public surfaces match the reference by name and descriptor; only fourteen unreferenced compiler
  artifacts disappear.
- Characterized `ItemMicroPart` against untouched Scala. Six plain-JVM cases freeze its item, static facade and both
  companion surfaces, NBT/material semantics, creation overloads, invalid-class short circuits and a Scala 2.11.5
  binary consumer that calls all four `ItemMicroPart$.MODULE$` methods used by ProjectRed.
- Ported the item and renderer to four Java types with every callable public name and descriptor retained. Creative
  enumeration is a direct loop, and the renderer crosses the transformed `MicroblockClient` boundary through one
  narrow Scala helper rather than emitting an invalid class-method call from Java.
- The jar retains exactly the four public ItemMicroPart types; three unreferenced Scala iteration closures disappear.
- Characterized `MicroblockPlacement` against untouched Scala. Three plain-JVM cases freeze the exact six-type
  hierarchy, constructors, fields, callable descriptors, companion and defaults. Four Forge cases freeze external
  placement, internal/opposite-slot selection, in-place expansion, custom-placement precedence and consumption.
- Ported the six retained types directly to Java. The only Scala source caller now names
  `MicroblockPlacement$.MODULE$` explicitly; no runtime class was added or removed and every callable public member
  matches the reference.
- Characterized `PlacementGrids` against untouched Scala. Five plain-JVM cases freeze the exact nine-class trait,
  helper, configurable-grid, facade and companion surface plus every face/corner/edge selection boundary on all six
  hit sides. The tests call ProjectBlue's load-bearing static facade directly.
- Ported all nine retained types to Java, preserving `PlacementGrid$class` for old Scala forwarders and using safe
  Java defaults for the three concrete trait methods. The three remaining Scala object-value users now name their
  companions explicitly.
- The jar class list is unchanged and all callable public descriptors match the reference.
- Characterized `BlockMicroMaterial` against untouched Scala. Five plain-JVM cases freeze all five retained public
  types, exact methods/fields/client annotations, material delegation, thread-local render-helper state and inventory
  pipeline. A frozen Scala 2.11.5 consumer calls both load-bearing companions; one Forge case freezes registered block
  semantics and dedicated-server side stripping.
- Ported the material base, both facade/companion pairs and `ThreadState` to Java. The GuideNH-targeted private final
  `block`/`meta` fields, public `(Block, int)` constructor, Scala `Seq` registration overloads, historical meta-0-only
  overload behavior, render pipeline and Angelica override lifecycle are unchanged.
- Every callable public descriptor matches the reference; only three unreferenced Scala closure/anonymous classes
  disappear.
- Characterized `ConfigContent` against untouched Scala. Six plain-JVM cases freeze its facade, companion and exact
  mutable-map field, config-file generation and parsing, alias/range semantics, malformed-line recovery, block
  registration and IMC filtering/validation.
- Ported the facade and companion directly to Java. Both retained runtime classes and every callable public descriptor
  match the reference; the seven Scala iteration/parser closure classes disappear. Public file helpers still throw the
  original `IOException` instances without adding checked exceptions to their descriptors or source declarations.
- Added a dependent GitHub Actions job that accepts the EULA in its ephemeral runner and invokes the existing
  self-validating `runFunctionalTestServer` task after the shared GTNH build. The Forge suite now gates pull requests
  and pushes instead of being local-only.
- Characterized `AngelicaCompat` against untouched Scala. Two plain-JVM cases freeze its exact non-final public
  surface, unusual `Object` return descriptors, `CapturingTessellator` guard, Iris calls and caught
  `ClassCastException` fallback.
- Ported the sole class directly to Java while retaining `BoxedUnit.UNIT` on the normal path and `Unit$.MODULE$` on
  the fallback path. Both jars contain the same runtime class and every callable descriptor matches the reference.
- Characterized `ItemSaw` and `ItemSawRenderer` against untouched Scala. Four plain-JVM cases freeze all three runtime
  types, the reflective private-final harvest field, default and explicit durability, container behavior, renderer
  gating and supported render-type selection.
- Ported the item, static renderer facade and registered renderer companion directly to Java. All callable public
  descriptors, singleton/model fields and three runtime classes match the reference.
- Characterized `MicroblockRender` against untouched Scala. Four plain-JVM cases freeze its facade/companion surface,
  thread-local face state, cuboid face-mask traversal, no-placement highlight exit and exact transformed-client call
  opcodes.
- Ported both retained types directly to Java. A clean compile preserves `invokevirtual Microblock.setShape` and
  `invokeinterface MicroblockClient.getBounds/render`; direct face iteration removes three unreferenced Scala
  anonymous/closure classes.
- Characterized `MicroblockClass`, `CommonMicroClass` and its companion against untouched Scala. Three plain-JVM
  cases freeze the exact hierarchy, constructors, public descriptors, private fields, side annotations, registry
  semantics and generator call descriptors. Constructor execution remains a Forge-only boundary because generator
  initialization requires the launch class loader.
- Ported all three retained types directly to Java. Eager base-trait registration, synchronized one-time client-trait
  registration, part-factory registration order, class IDs, duplicate rejection and both create paths are unchanged.
  The GuideNH-pinned `MicroblockGenerator$.create(MicroblockClass, int, boolean)` descriptor remains exact.
- The clean reference and port jars contain the same three runtime types and every callable public descriptor matches.
- Characterized the `Microblock` base, its default-argument companion and all three mixin traits against untouched
  Scala. Three plain-JVM cases freeze the eight retained type surfaces, fields, constructor/default, signed shape
  packing, material delegation, item conversion, description/update bytes and core NBT.
- Ported `Microblock` and `Microblock$` directly to Java while moving `MicroblockClient`, `CommonMicroblock` and
  `CommonMicroblockClient` unchanged to `MicroblockTraits.scala`. Keeping those load-bearing Scala traits avoids a
  Java single-inheritance workaround and preserves the existing ProjectRed generator path. Three remaining Scala
  assignments now call the same public field setters explicitly.
- A clean build passes all 244 plain-JVM tests and the Java 8 Forge server passes all 103 tests, including generated
  face/hollow parts and the external Scala microblock trait fixture. All eight retained public surfaces match the
  reference; only two private Scala iteration closures disappear.
- Characterized the face factory, placement singleton and both generated traits against untouched Scala. Two
  plain-JVM cases freeze all eight retained public surfaces and every placement rule; one Forge case freezes factory
  identity, all 42 populated bounds and generated face-part behavior.
- Ported the four concrete facade/companion types directly to Java while retaining `FaceMicroblock` and
  `FaceMicroblockClient` unchanged in `FaceMicroblockTraits.scala`. All callable public descriptors match the
  reference; the two private Scala bounds-initializer closures disappear.
- Recompiled Scala must spell the Java array getter as `FaceMicroClass.aBounds()(index)`; existing binaries still link
  to the unchanged `aBounds(): Cuboid6[]` descriptor.
- Characterized the corner factory, placement singleton and generated trait against untouched Scala. Two plain-JVM
  cases freeze all six retained public surfaces and all 48 slot/side placement mappings; one Forge case freezes
  factory metadata, all 56 populated bounds and generated shape/slot behavior.
- Ported the four concrete facade/companion types directly to Java while retaining `CornerMicroblock` in
  `CornerMicroblockTraits.scala`. ProjectRed's load-bearing `CornerMicroClass$.MODULE$.getClassId()` linkage and every
  callable public descriptor remain exact; the two private Scala bounds-initializer closures disappear.
- Recompiled Scala must spell the Java array getter as `CornerMicroClass.aBounds()(index)`; existing binaries still
  link unchanged.
- Characterized the combined Edge/Post unit against untouched Scala. Two plain-JVM cases freeze all twelve retained
  public surfaces, state/super accessors and edge-opposite mappings. Three Forge cases freeze both factories, all 84
  edge and 12 post bounds, generated behavior, even-size post placement and matching-post expansion.
- Ported the six concrete facade/companion types directly to Java while retaining `EdgeMicroblock`,
  `PostMicroblock`, and stateful `PostMicroblockClient` in `EdgeMicroblockTraits.scala`. ProjectRed and
  UtilitiesInExcess class-ID linkage remains exact; the Post client traversal closure and all trait helpers remain.
- All callable public descriptors match the reference; only four private bounds-initializer closures disappear.
- Characterized the Hollow unit against untouched Scala. Two plain-JVM cases freeze all nine retained public
  surfaces, including the source-visible nested placement-grid relationship and both generated traits. Two Forge
  cases freeze both 42-entry tables, generated server behavior, every face, connected hollow sizes 1 through 11,
  and all collision, occlusion and subpart geometry.
- Ported the four concrete placement/factory facade and companion sources directly to Java while retaining
  `HollowMicroblock` and the large stateful `HollowMicroblockClient` in `HollowMicroblockTraits.scala`. The nested
  `HollowPlacement.HollowPlacementGrid$` remains a real public static nested class rather than merely keeping its
  binary name.
- All nine supported public surfaces match the reference; both retained trait helpers and all seven trait closures are
  bytecode-identical. Only three private factory table-initializer closures disappear.
- Characterized `MicroOcclusion` against untouched Scala. Five plain-JVM cases freeze the facade, companion, three
  generated-trait surfaces, all valid shrink-side mappings, exhaustive priority/size/transparency decisions, render
  masks, traversal ranges and the complete `TMicroOcclusion` decision matrix.
- Ported only the concrete facade and companion to Java. `JMicroShrinkRender`, `TMicroOcclusion` and the stateful
  `TMicroOcclusionClient` remain Scala; both trait helpers and all five retained Scala types have bytecode-identical
  disassembly. Direct Java iteration removes the sole private shrink closure.
- All seven supported public surfaces and WR-CBE's static `recalcBounds` descriptor match the reference.
- Characterized `MicroblockGenerator` against untouched Scala. Three plain-JVM cases freeze its facade, companion,
  nested material interface, inherited `ASMMixinFactory`/`ScratchBitSet` shape, replaceable thread-local scratch state
  and load-bearing calls. One Forge case freezes the complete material-added external Scala-trait path.
- Ported the facade, companion and real public static nested `IGeneratedMaterial` interface directly to Java while
  leaving the generator and ScalaSignature machinery unchanged. Scratch-bit reuse, base/client selection, material
  callback ordering, boxed constructor argument and the ProjectRed Scala-trait registration path are unchanged.
- The same three runtime classes and every callable public descriptor match the reference, including ProjectRed's
  companion registration and GuideNH's exact companion `create` method.
- Characterized `MultipartGenerator` against untouched Scala. Two plain-JVM cases freeze both public surfaces,
  private Scala-map descriptors and companion call opcodes. Five Forge cases freeze side-specific hierarchy caches,
  duplicate/failed registration, scratch clearing, class snapshots/reuse, tile upgrades/downgrades, vanilla-block
  conversion and a precompiled Scala consumer exercising companion generation and pass-through registration.
- Ported the facade and companion directly to Java. All five Scala maps, both compiler-generated public accessors,
  the companion-only `generateCompositeTile` descriptor and side-safe proxy callback remain. Direct iteration removes
  six private closures; the ASM factory's sole source adjustment explicitly names `MultipartGenerator$.MODULE$`.
- Both supported public surfaces match the reference.
- Characterized `ScratchBitSet` against untouched Scala. Four plain-JVM cases freeze the exact interface/helper
  surface, lazy allocation, repeated accessor calls, owner/thread isolation, bit preservation/clearing, storage
  replacement/reinitialization and `freshBitSet` dispatch through an overridden `getBitSet`.
- Ported the interface and `$class` helper directly to Java without changing either generator. All seven callable
  methods, their abstract/static modifiers and both binary names remain exact; no new API or default methods are
  introduced. Neither downstream audit contains a reference to this support trait.
- The two-type ABI and both generator companions' disassembly match the reference.
- Condensed `docs/migration/DIVERGENCES.md` from 3,134 to 189 lines, keeping effective runtime, binary and source
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
  and SHA-256 hashes match the reference. No new ledger entry is needed.

### 2026-09-02

- Ported `ASMMixinFactory` directly to Java, retaining the Scala `Seq` constructor/argument descriptors, all six
  fields, synchronized construction, live parameter conversion, copied cache keys, generated-name sequence and both
  Scala- and Java-trait registration paths. Parent validation and callback/registration order remain unchanged;
  reflection failures escape as the same exceptions without adding checked exceptions to the public methods.
- Kept the reference's public JVM callbacks and both public mangled parent helpers. The retained Scala subclass
  needs only an explicit empty parameter sequence and public override syntax. Its companion and five closure types
  have identical disassembly; Scala adds four static facade forwarders without changing any existing entry. The
  ledger records that additive surface and the Scala-source syntax changes.
- No tests were added, per the current user instruction. Clean formatting/checkstyle/build passes all 276 existing JVM
  tests; Java 8 Forge passes all 116 tests, including the external Scala-trait and generated-tile fixtures. All 39
  generated dump names and SHA-256 hashes match the saved `f7be2b1` reference. Both generator companions and all 137
  signature/compiler types have identical disassembly. Removing two private parent-traversal closures reduces the
  packaged inventory from 462 to 460 classes.
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
  describes the retained Java forwarders; no new divergence was added.
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
- No tests were added, per the current user instruction. Formatting/checkstyle/build passes all 276 existing JVM tests
  and Java 8 Forge passes all 116 tests, including the external Scala-trait path. Four private parser/lookup closures
  are replaced by one Java helper, reducing the packaged inventory from 455 to 452 classes and leaving 206 Java files
  plus 9 Scala files / 1,883 nonblank Scala lines. No new compatibility divergence needs a ledger entry.
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
- All characterization tests remain unchanged after the port. The forced Scala compilation version guard is retained,
  and all five `@Mod` version annotations match both clean packaged jars. Two unreferenced traversal closures become
  one Java helper (452 to 451 packaged classes); sources now total 207 Java files and 9 Scala files / 1,726 nonblank
  Scala lines. No new compatibility divergence is introduced. Next is `ASMMixinCompiler.scala`, bounded to
  `ClassInfo`/`MethodInfo` metadata lookup and traversal, with fresh characterization before touching its
  Forge-initialized state and no trait-rewriting algorithm changes.
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
- The eight characterization tests are unchanged after the port, and the clean jar repeats the API/disassembly/dump
  matches above. The forced Scala-compilation version guard is retained; all five `@Mod` annotations match both
  packaged jar versions. External Scala-trait coverage remains green and the existing manual client checks and
  Java-source bridge limitations remain outstanding.
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
- Characterization tests remain unchanged. The clean jar repeats the API/disassembly/dump matches, the local dev
  config is unchanged, and all five `@Mod` versions match both packaged jar versions with the forced Scala-compilation
  guard retained. External Scala-trait tests remain green; existing manual client checks and Java-source model-bridge
  limitations remain outstanding.
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
- The six characterization tests are unchanged after the port. The clean jar repeats the API/disassembly/dump matches;
  the local dev config and forced Scala-compilation guard are unchanged. All five `@Mod` versions match both packaged
  jar versions. External Scala-trait tests remain green; existing manual client checks and Java-source model-bridge
  limitations remain outstanding. Next: Scala-trait registration metadata, `getAndRegisterParentTraits` and
  `registerScalaTrait`, characterized first with no registration algorithm changes.
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
- The twelve characterization tests are unchanged after extraction. The clean jar repeats the API/disassembly/dump
  comparisons; the local dev config and forced Scala-compilation guard are unchanged. All five `@Mod` versions match
  both packaged jar versions. External Scala-trait tests remain green; existing manual client checks and Java-source
  model-bridge limitations remain. Next: `getBytes`, `classNode` and `internalDefine` class-byte loading/cache
  helpers, characterized first and without loader/cache algorithm changes.
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
  compiler closures match after normalizing private closure numbering. Two private Scala closures become two Java
  callbacks plus their helper, taking the packaged inventory from 455 to 456 classes. The shared compiler ledger entry
  covers this, with no new effective divergence. Sources total 211 Java files and 9 Scala files / 1,597 nonblank Scala
  lines.
- All ten characterization tests remain unchanged. Clean APIs and dumps repeat the matches above; the dev config and
  forced Scala-compilation guard are unchanged. All five `@Mod` versions match both packaged jar versions. External
  Scala-trait tests remain green; existing manual client checks and Java-source model/trait limitations remain. Next:
  `ASMMixinCompiler.define`, characterized first for publication/debug accounting, reflective definition and failure
  ordering, without algorithm changes.
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
  The 456-class inventory is unchanged; all 426 non-closure APIs (including 16 named compiler APIs), 3,628 other
  method bodies and 30 compiler closures match. No new divergence is needed.
- The characterization tests, dev config and forced Scala-compilation guard are unchanged; all five `@Mod` versions
  match both packaged jar versions. Sources total 211 Java files and 9 Scala files / 1,583 nonblank Scala lines. Next:
  `ASMMixinCompiler.mixinClasses`, characterized for composition/constructor/dispatch and generated output before
  extraction; keep compiler algorithm fixes separate.

- Consolidated the active plan/handoff after the reflective-definition port. The dated findings remain intact in
  this history, duplicated completed-port handoff summaries were removed, and current phase/status text was refreshed.
  The manual checklist now names verified consumer items, including the inverted ProjectRed lamp requirement.

### 2026-09-03 - composite class generation

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
  3,628 non-target method bodies, 13 other compiler closures and every generated output name/hash. All five packaged
  `@Mod` versions, the forced Scala-compilation guard and dev config remain correct. Sources total 212 Java files and
  9 Scala files / 1,444 nonblank Scala lines. Next: `ASMMixinCompiler.registerJavaTrait`, characterized before
  extraction; abstract mixins, side-only filtering and the wide-field defect remain separate compiler changes.

### 2026-09-03 - Java trait rewriting

- Added nine Forge characterization tests for `ASMMixinCompiler.registerJavaTrait`, passing against untouched Scala
  and committed first as `85ad2e4`. They freeze rejected shapes, exact input cloning, constructors, initialization,
  field accessors, private/public methods, virtual self calls, super dispatch, parent traits, source metadata,
  implemented-interface deduplication, side-only annotation retention and partial publication after definition errors.
  They also preserve the existing primitive-`NEWARRAY`, foreign-field and inherited-call failures.
- Moved only rewriting into Java `JavaTraitRegistration`; registration dispatch and the Scala `MixinInfo` shell remain
  in the singleton. Abstract mixins and Java-path side filtering remain planned behavior changes. Nine unreferenced
  target closures and local compiler helpers become the Java helper, its context and callback, taking the packaged
  inventory from 442 to 436 classes. The shared classfile ledger covers these artifacts; no effective divergence was
  added.
- Saved the pre-port source/jar, reports, 102 generated outputs and checks in ignored
  `run/migration-java-trait-reference/`. Clean verification after stopping Gradle matches 428 non-target class APIs,
  3,635 non-target method bodies, all five other compiler closures and every output name/hash. All five packaged
  `@Mod` versions, the forced Scala-compilation guard and dev config remain correct. Sources total 213 Java files and
  9 Scala files / 1,196 nonblank Scala lines. Next: assess the remaining compiler startup/model shell before selecting
  another extraction; retained ScalaSignature model bridges remain the Java-source limit.

### 2026-09-03 - compiler startup

- Added seven Forge characterization tests for the `ASMMixinCompiler` singleton startup, passing against untouched
  Scala and committed first as `e9d81d6`. They execute reflective definition and transformer invocation on isolated
  launch loaders, verify the live transformer-exception set, exercise sanity-checker byte loading and freeze loader,
  member-accessibility and independent mutable-map behavior.
- Moved reflection lookup/access, map construction and sanity warmup into Java `CompilerBootstrap` while retaining the
  singleton's field names, descriptors, modifiers, accessors and initialization order. The packaged inventory grows
  from 436 to 437 only for the helper; the shared classfile ledger covers it and no effective divergence was added.
- Saved the reference source/jar, reports, 102 generated outputs and checks in ignored
  `run/migration-compiler-startup-reference/`. Clean verification after stopping Gradle matches 431 non-target class
  APIs, 3,653 non-target method bodies, all five compiler closures and every output name/hash. All five packaged
  `@Mod` versions, the forced Scala-compilation guard and dev config remain correct. Sources total 214 Java files and
  9 Scala files / 1,188 nonblank Scala lines.
- Audited the preserved Java-rewriter defects against current registrations and supplied consumers. The ABI inventory
  exposes only ProjectRed's external Scala-trait registration, which bypasses this path; current Java mixins have no
  wide state fields and the Forge registration suite remains green. Wide getter maxima, primitive-array analysis and
  malformed inherited-call casts are real but dormant for current consumers. Abstract Java mixins and Java-path side
  filtering remain required before the microblock traits can move, and will be separate behavior targets.

### 2026-09-03 - abstract Java mixins

- Added two Forge characterizations before changing behavior and committed them as `3556725`. They freeze the old
  abstract-input rejection before parent or method inspection and prove that concrete Java mixins already preserve
  virtual dispatch when their base class is abstract.
- `registerJavaTrait` now accepts abstract inputs. Declared abstract methods remain on the generated interface so
  concrete methods can call them, but no static helper or `MixinInfo.methods` entry is emitted until a child mixin
  supplies the implementation. The no-argument mixin constructor may directly call a superclass constructor with
  arguments; registration removes that receiver/argument/call sequence while retaining subsequent state
  initialization. Constructor parameters and missing direct superclass calls still fail before publication.
- The feature fixture executes a two-layer generated composite over an abstract base with an `int` constructor. It
  verifies base-argument forwarding, post-super field initialization, parent metadata, abstract-contract exceptions,
  interface dispatch from the base and parent mixin, and final concrete instantiation. Existing Scala-trait
  registration and all current Java mixin outputs are unchanged.
- Saved the pre-change jar/source, reports, 106 generated outputs and checks in ignored
  `run/migration-abstract-java-reference/`. Clean verification after stopping Gradle matches all 432 class APIs, 3,643
  non-target method bodies and all five compiler closures. The 106 existing dump names/hashes match exactly; the
  abstract-layer fixture adds six outputs. All five packaged `@Mod` versions, the forced Scala-compilation guard and
  dev config remain correct. Sources stay at 214 Java files and 9 Scala files / 1,188 nonblank Scala lines. Next:
  Java-path `@SideOnly` member filtering, characterized first as its own compiler behavior target.

### 2026-09-03 - Java mixin side filtering

- Expanded the Forge Java-trait fixture against the untouched implementation and committed it first as `a481859`.
  It proves that the old Java path retained runtime-visible and runtime-invisible client annotations on a dedicated
  server: both methods execute, both fields keep initialized state, and a client-marked constructor runs its body.
- `registerJavaTrait` now filters opposite-side fields and methods before building field metadata, method signatures
  or helper bodies. It reads both annotation tables and retains current-side members. When the no-argument constructor
  is absent, the helper receives an empty `$init$`; generated composites construct normally without executing
  side-specific initialization. The regressions cover a method body that would otherwise fail primitive-array
  analysis and input already processed by Forge's real side transformer. External Scala traits still use the
  unchanged ScalaSignature path.
- This deliberately changes the previously ignored Java annotation, so the divergence ledger records it. No audited
  Java trait depends on retaining an opposite-side member; the change supplies the remaining compiler prerequisite
  for a Java ProjectRed `LightMicroblock`, whose `renderDynamic` is client-only while the trait is selected both ways.
- Saved the pre-change jar/source, Forge report and 112 generated outputs under ignored
  `run/migration-java-side-only-reference/`. Clean verification after stopping Gradle keeps all 437 packaged classes,
  matches 432 class APIs, 3,643 non-target method bodies and all five compiler closures. All 112 reference dump names
  remain: 109 hashes are exact and only the SideOnly fixture's trait, helper and composite change; four outputs are
  new for the already-stripped-constructor fixture. All five packaged `@Mod` versions, the forced Scala-compilation
  guard and dev config remain correct. Sources stay at 214 Java files and 9 Scala files / 1,188 nonblank Scala lines.
  Next: `microblock/MicroblockTraits.scala`, characterized before conversion.

### 2026-09-03 - Common microblock trait implementation

- Committed seven JVM and two Forge characterization tests first as `d6eb56b`. A frozen Scala 2.11.5 concrete
  `CommonMicroblockClient`, compiled under Java 8 against the unchanged jar, exercises all three trait helper bridges.
  Tests pin initialization, signed shape/slot handling and virtual shift operands, fresh immutable partial-box lists
  with shared bounds, dynamic class/material lookup, render-pass gating, common cuboid face masks and particle callback
  evaluation order. Forge verifies generated common parts and the original `NoSuchMethodError` when the missing-material
  client icon fallback reaches server-stripped `Block.getIcon`; the test does not turn that existing limitation into a fix.
- Moved eight implementation methods to package-private `MicroblockTraitLogic.java`. Particle callbacks were already
  Java delegates. Retained the three Scala trait declarations, exact inheritance/signature metadata and `$class`
  bridges: Scala parent collection does not recognize Java interfaces as Scala traits, and Java registration does not
  incorporate implemented Scala interfaces into linearization. Both prior Java-mixin prerequisites remain useful,
  but are insufficient to remove this multiple-inheritance shell without another compiler behavior target.
- Saved the reference jar, source, reports and generated dumps under ignored `run/migration-microblock-traits-reference/`.
  The clean comparison preserves all 437 original class/member APIs, all 17 ScalaSignature payloads and 3,674 non-target
  method bodies; only the new helper raises the class inventory to 438. All 116 generated dump names/hashes are exact,
  including external Scala and pass-through fixtures. No new effective divergence is introduced.
- The forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total 215 Java
  files and 9 Scala files / 1,170 nonblank Scala lines. Next: `microblock/FaceMicroblockTraits.scala` implementation,
  with characterization before conversion. GPU rendering and full client selection remain on the manual checklist.

### 2026-09-03 - Face microblock trait implementation

- Committed five JVM and three Forge characterization tests first as `dc845bc`. The frozen Scala 2.11.5 face-client
  consumer was compiled under Java 8 against the untouched jar. Tests verify actual emitted face indices, position,
  pass and bounds for all six slots and 64 masks; negative-pass bypass; material caching and repeated virtual slot
  evaluation between opaque draws; and null-material failure timing. Forge pins all 42 valid shape bounds, the
  existing signed-byte indexing failures, null entries/live replacement of the public bounds array, singleton
  identity and material solidity independent of the side argument.
- Moved rendering, bounds lookup and solidity to package-private `FaceMicroblockTraitLogic.java`. The Scala
  declarations, inheritance metadata, `$class` helpers and singleton accessor remain; Extra Utilities' runtime
  face-client type check and the external Scala registration path are preserved. No compiler algorithm or existing
  behavior was changed, and no new divergence entry is needed.
- Saved the pre-port jar/source, reports and 116 generated dumps in ignored `run/migration-face-traits-reference/`.
  Clean verification preserves all 438 original class/member APIs, all 17 ScalaSignature payloads and 3,688 non-target
  method bodies. All 116 generated names and SHA-256 hashes match; only the new helper increases the jar inventory to
  439 classes. The forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total
  216 Java files and 9 Scala files / 1,144 nonblank Scala lines.
- Added a manual face-cover check using Stone, Glass and ProjectRed Inverted White Lamp materials. Headless tests
  prove per-face dispatch, not GPU output. Next: `microblock/CornerMicroblockTraits.scala` implementation, characterized
  before conversion.

### 2026-09-03 - Corner microblock trait implementation

- Committed three JVM and three Forge characterization tests first as `82da452`. A frozen Scala 2.11.5 corner
  implementor, compiled under Java 8 against the untouched jar, pins the virtual shape setter/getter and seven-slot
  offset, including byte truncation and out-of-range integer behavior. Forge covers all 56 supported corner bounds,
  singleton/type identity, signed-index failures, null/live-replaced bounds arrays and generated NBT/description
  round-trips for the packed shape and material name.
- Moved shape packing, bounds lookup and slot decoding to package-private `CornerMicroblockTraitLogic.java`.
  The Scala trait declaration, inheritance metadata, `$class` helper and singleton accessor remain. No compiler
  algorithm, validation or existing behavior changed; no new divergence entry is needed.
- Saved reference jar/source, reports and 116 generated dumps under ignored `run/migration-corner-traits-reference/`.
  Clean verification preserves all 439 original class/member APIs, all 17 ScalaSignature payloads and 3,692 non-target
  method bodies. All 116 generated names and SHA-256 hashes match; only the helper raises the inventory to 440
  classes. The forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total 217
  Java files and 9 Scala files / 1,144 nonblank Scala lines.
- Next: `EdgeMicroblock` in `microblock/EdgeMicroblockTraits.scala`; characterize and port the post traits separately.
  Client/GPU and full-pack validation remain on the existing manual checklist.

### 2026-09-03 - Edge microblock trait implementation

- Committed three JVM and three Forge characterization tests first as `5e020e5`. A frozen Scala 2.11.5 implementor,
  compiled under Java 8 against the untouched jar, pins virtual shape access, byte truncation/integer overflow and the
  fifteen-slot offset. Generated tests cover all 84 edge bounds and NBT/description round-trips, singleton/type
  identity, inherited redstone non-conduction, signed-index failures and live/null/replaced bounds arrays.
- Moved only `EdgeMicroblock` shape packing, bounds lookup and slot decoding to package-private
  `EdgeMicroblockTraitLogic.java`. Retained the Scala declaration, inheritance metadata, `$class` bridge, singleton
  accessor and `TEdgePart` default. Both post traits in the same file are untouched. No compiler algorithm or existing
  behavior changed; no new divergence entry is needed.
- Saved the reference jar/source, reports and 116 generated dumps in ignored `run/migration-edge-trait-reference/`.
  Clean verification preserves all 440 original class/member APIs, all 17 ScalaSignature payloads and 3,696 non-target
  method bodies. All 116 generated dump names and hashes match; only the helper raises the inventory to 441 classes.
  The forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total 218 Java
  files and 9 Scala files / 1,144 nonblank Scala lines.
- Next: `PostMicroblock` in the same file; characterize its occlusion ordering, axis bounds, item identity, torch
  support and generated state before extraction. `PostMicroblockClient` follows separately. The existing manual
  Stone Strip / ProjectRed Red Alloy Wire and illuminated-strip checks cover the remaining client/full-pack gate.

### 2026-09-03 - Post microblock trait implementation

- Committed six JVM and four Forge characterization tests first as `afd6c69`. Frozen Scala 2.11.5 post/face
  implementors compiled under Java 8 against the untouched jar pin virtual calls, failure ordering and a real
  superclass predecessor. Tests cover post-first and aligned-face bypasses, normal/partial box rejection before
  super, superclass results/exceptions, fresh read-only lists and partial-list delegation. Forge covers all nine
  supported post bounds, signed/live/null array behavior, torch support, edge-item drops/picking, NBT/descriptions,
  crossing posts and aligned/non-overlapping face covers.
- Extracted seven method bodies to package-private `PostMicroblockTraitLogic.java`; retained Scala declarations,
  metadata, singleton access and the synthetic super accessor. Java returns an occlusion decision or requests the
  original Scala super call, preserving short-circuit ordering without exposing a new accessor or allocating a
  callback. The returned list still uses the same Scala Seq-to-Java conversion. `PostMicroblockClient` is untouched;
  no compiler algorithm, existing behavior or effective compatibility difference changed.
- Saved reference source/jar, reports and 116 generated dumps under ignored `run/migration-post-trait-reference/`.
  Clean verification preserves all 441 original class/member APIs, all 17 ScalaSignature payloads and 3,696 non-target
  method bodies. All 116 generated names/hashes match; the helper alone raises the jar inventory to 442 classes. The
  forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total 219 Java files
  and 9 Scala files / 1,140 nonblank Scala lines.
- Next: `PostMicroblockClient`; characterize render dispatch, lifecycle super ordering, shrink/split/reset behavior
  and size/transparency/axis tie-breaks before conversion. Client/GPU and full-pack checks remain manual.

### 2026-09-03 - Post microblock client implementation

- Committed eight JVM characterization tests and one Forge test first as `47885d1`. Frozen Scala 2.11.5 client forwarders
  and lifecycle predecessors pin the exact `-1` render branch, material reuse, live second-segment changes, repeated
  virtual size/transparency/axis reads, superclass ordering/exceptions and equality receiver dispatch (even on the
  same reference). Tests also pin collection `foreach`, neighbour order, fresh bounds copies, split reuse/reset and
  malformed face failures. Forge exercises real cover/post geometry across all three axes through the client helper.
- Extracted five behavior methods to `PostMicroblockClientLogic.java`; retained Scala state/inheritance metadata,
  accessors and lifecycle/super forwarders. The named Java callback preserves `foreach` and replaces the internal
  Scala anonymous callback under the existing shared classfile policy. No algorithm fix or new effective divergence
  was introduced. In particular, the second split segment still copies physical bounds rather than the already
  face-clipped first segment; the new Forge check records that reference behavior.
- Saved source/jar, reports and 116 generated dumps in ignored `run/migration-post-client-reference/`. Clean checks
  preserve all 441 retained class/member APIs, all 17 ScalaSignature payloads, 3,703 non-target method bodies and all
  116 generated names/hashes. Two Java classes replace one Scala callback, bringing the jar inventory to 443 classes.
  The forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total 220 Java
  files and 9 Scala files / 1,104 nonblank Scala lines.
- Expanded the existing manual post check with Stone, Glass and ProjectRed Inverted White Lamp examples. The
  dedicated server strips the client factory entry point; helper/geometry coverage does not establish actual client
  generation or GPU output. Next: `HollowMicroblock`, followed separately by `HollowMicroblockClient`.

### 2026-09-03 - Hollow microblock trait implementation

- Committed five JVM and four Forge characterization tests first as `6e807cc`, before changing production code.
  Frozen Scala 2.11.5 forwarders compiled under Java 8 against the untouched jar pin repeated tile/raw-shape reads,
  center-slot lookup and arbitrary connector results, signed thickness and invalid rotations, fresh read-only collision
  boxes, mutable copied subparts, normal/partial overlap short-circuiting and the real Scala super chain. Forge covers
  all 42 supported shapes, live opening sizes, public bounds arrays/views, null/signed-index/invalid-slot failures and
  exact material/shape NBT and description bytes. Existing ordered geometry and ProjectRed trait fixtures still run.
- Extracted seven method bodies to package-private `HollowMicroblockTraitLogic.java`; retained Scala inheritance,
  singleton/constants and the synthetic super bridge. Java cannot directly invoke that Scala synthetic accessor.
  Kept Scala collection builders and conversions: a null partial-box sequence returns a wrapper whose use throws,
  collision lists reject mutations, and subparts are mutable copies. Connector sizes remain unclamped; applying the
  original transformation still normalizes inverted collision bounds. These are characterized existing behaviors,
  not new defects or fixes. Two internal Scala callbacks are replaced under the existing shared classfile policy;
  no new effective divergence, compiler change or client-rendering change was introduced.
- Saved the source/reference jar, frozen fixture compilation, reports and 116 generated dumps in ignored
  `run/migration-hollow-trait-reference/`. Normal and clean verification after stopping Gradle preserve all 441
  retained original class/member APIs, all 17 ScalaSignature payloads, 3,704 unrelated method bodies and all 116
  generated dump names/hashes. The helper and two callbacks replace two Scala callbacks: 443 -> 444 packaged classes.
  The forced Scala-compilation guard and all five packaged `@Mod` versions remain verified. Sources total 221 Java
  files and 9 Scala files / 1,051 nonblank Scala lines.
- Next: `HollowMicroblockClient`. Characterize render-mask initialization/recalculation and super ordering,
  pass/transparency dispatch, rim geometry/callback order, breaking and highlight behavior before extraction.
  Added a manual hollow-cover interaction/reload check with ProjectRed Framed Red Alloy Wire and Hollow Inverted
  White Lamp Cover examples; actual client generation, GPU output and full-pack validation remain manual gates.

### 2026-09-04 - Hollow microblock client implementation

- Committed eight JVM and one Forge characterization tests first as `c22d1ac`, before changing production code.
  Frozen Scala 2.11.5 forwarders and a real superclass predecessor pin default-mask initialization, super/read order,
  arbitrary opening sizes, physical/transparent/opaque render selection, repeated slot reads, callback dispatch,
  all six ordered rim bounds/masks on every axis, material/coordinate/mask snapshots and failure short-circuiting.
  A child loader records only rendering-service calls while executing the real method bodies: breaking tests assert
  translation/icon pipelines and the cuboid callback; highlight tests assert GL state order, world/part transforms,
  expanded outlines and the original lack of cleanup after failure. Forge exercises real material/bounds factories,
  all 42 supported shapes and live center-connector sizes through the client helpers.
- Extracted five method bodies to package-private `HollowMicroblockClientLogic.java`; retained Scala inheritance,
  `$class`, initializer and the synthetic recalculation super call. The Java helper accepts an existential `Function5`
  so the retained Scala method keeps its primitive function signature. Two named Java callbacks replace five internal
  Scala callbacks under the existing shared classfile policy. No new effective divergence or compiler algorithm fix
  was introduced; opening sizes, callback order and highlight failure behavior remain unchanged.
- Saved source/reference jar, fixture compilation, reports and 116 generated dumps under ignored
  `run/migration-hollow-client-reference/`. Normal and clean verification after stopping Gradle preserve all 439
  retained class/member APIs, all 17 ScalaSignature payloads, 3,706 unrelated method bodies and all 116 generated
  names/hashes. The helper and two callbacks replace five Scala callbacks: 444 -> 442 packaged classes. The forced
  Scala compilation guard and all five packaged `@Mod` versions remain verified. Sources total 222 Java files and 9
  Scala files / 820 nonblank Scala lines.
- Next: `TMicroOcclusion`; characterize super short-circuiting, size/material/slot constraints, repeated reads and
  edge/corner bit tests before extraction. Leave `TMicroOcclusionClient` for a separate target. Existing manual checks
  cover AE2 cables and ProjectRed Framed Red Alloy Wire through hollow covers. Recorded rendering commands and
  headless helper tests do not validate actual client generation or GPU/full-pack output.

### 2026-09-04 - Micro occlusion trait implementation

- Committed eight JVM and two Forge characterization tests first as `c709759`. Frozen Scala 2.11.5 forwarders
  compiled under Java 8 against the untouched jar pin the real super call before null/non-micro guards, repeated
  getter order, signed size overflow, material short-circuits, own-receiver edge/corner delegation and getter errors.
  Edge/corner tests cover the full 12-by-8 mapping, independent edge reads and invalid-slot failure order. Forge
  exercises generated face/corner/edge pairs at size thresholds with Stone/Glass materials in both directions.
  The existing complete decision-table digest and external Scala-trait fixtures remain unchanged and passing.
- Extracted two method bodies to package-private `TMicroOcclusionLogic.java`. The Scala shell retains inheritance
  metadata, `$class`, initializer and the synthetic super call, which Java source cannot invoke directly. Rules still
  use the original virtual reads and integer arithmetic, including overflow and out-of-range slot behavior; no
  unrelated fix, compiler change or new effective divergence was introduced. `TMicroOcclusionClient` is untouched.
- Evidence in ignored `run/migration-micro-occlusion-trait-reference/` includes source/jar, fixture compilation,
  reports and 116 generated dumps. Normal and clean checks after stopping Gradle preserve all 442 original
  class/member APIs, all 17 ScalaSignature payloads, 3,721 unrelated method bodies and all 116 dump names/hashes. Only
  the Java helper is added: 442 -> 443 packaged classes. The forced Scala-compilation guard and all five packaged
  `@Mod` versions remain verified. Sources total 223 Java files and 9 Scala files / 784 nonblank Scala lines.
- Next: `TMicroOcclusionClient`; characterize lifecycle super/recalc ordering, packet propagation, bounds copying,
  mask updates and failures. Retain `JMicroShrinkRender` and required Scala metadata/state/super bridges. Actual
  client generation, GPU drawing and full-pack checks remain on the existing manual checklist.

### 2026-09-04 - Micro occlusion client state updates

- Committed six JVM and one Forge characterization tests first as `d69d12b`, against untouched production code.
  Frozen Scala 2.11.5 forwarders compiled under Java 8 pin the three real superclass predecessors, virtual recalc
  dispatch, exact part/packet identity (including null), super/recalc failure order and predecessor state changes.
  Bounds tests cover virtual copying before traversal, mask replacement, clipping, fresh copies on repeated
  updates, null/copy failures and partially clipped bounds with the old mask after traversal failure. Forge uses
  the existing hollow client probe with real Stone/Glass materials and factory bounds, checking priority,
  opacity, neighbor resizing/removal and physical-bounds immutability.
- Extracted render-state updates to package-private `TMicroOcclusionClientLogic.java`. Scala retains
  `JMicroShrinkRender`, trait inheritance metadata, mutable state/accessors, initialization and lifecycle bridges.
  Java source cannot call the synthetic Scala super accessors directly; those three super calls and subsequent
  virtual recalculation calls retain identical instructions and dispatch. The helper still calls the bounds setter
  and getter separately before mask publication. No compiler algorithm change or new effective divergence.
- Evidence in ignored `run/migration-micro-occlusion-client-reference/` includes reference source/jar, frozen fixture
  compilation, reports and 116 generated dumps. Normal and clean checks after stopping Gradle preserve all 443
  original class/member APIs, all 17 ScalaSignature payloads, 3,725 unrelated method bodies and all 116 generated
  names/hashes. Only the Java helper is added: 443 -> 444 packaged classes. The forced Scala compilation guard and all
  five packaged `@Mod` versions remain verified. Sources total 224 Java files and 9 Scala files / 782 nonblank Scala
  lines.
- Next: extract `StackAnalyser` constructor initialization after characterizing receiver/parameter slot setup,
  virtual `pushL` order, malformed descriptors and duplicate exception-handler precedence. Retain its coordinated
  Scala shell/models; keep opcode fixes separate. The client manual row now names Stone/Glass covers and
  ProjectRed Inverted White Lamp microblocks, including a server rejoin. Headless probes do not establish actual
  client generation, GPU output or full-pack correctness; those checks remain unrecorded release gates.

### 2026-09-04 - Packet scheduler callback regression

- Restored the original Scala mutable hash-map `foreach` traversal behind `PacketScheduler`'s Java API. Java's
  fail-fast iterator threw `ConcurrentModificationException` when a write callback scheduled another part. Keeping
  the reference collection also preserves live mask updates, hash-bucket visitation and clearing after a successful
  flush; no new batching or callback timing policy is introduced.
- Added two Forge regression cases for scheduling a new part during a flush and merging masks into an existing
  pending entry. The first failed with the reported exception before the fix. The new-entry hash placement was
  checked against the original Scala implementation under Java 8.
- Formatting, test checkstyle, all four packet-interface JVM tests and all 237 Forge server tests pass.

### 2026-09-04 - Multipart virtual tile accessor regression

- Restored virtual `tile()` and `tile_$eq` dispatch throughout `TMultiPart`, matching the original Scala methods.
  Binding, coordinates/world lookup, ray tracing, harvesting and packet operations now honor part subclasses that
  override the tile accessors instead of reading or writing the base class's private storage directly.
- Added three JVM regression cases covering overridden binding/unbinding, convenience methods and description
  reads that publish a replacement tile before the render update. All three failed before the fix.
- Formatting, test checkstyle, the three new cases and all seven existing tile equality cases pass.

### 2026-09-04 - Tick scheduler equality regression

- Restored Scala's null-safe part equality in `ChunkTickScheduler.scheduleTick` with `Objects.equals`. Distinct but
  equal parts share the original entry and callback target, and random-to-scheduled promotion updates that entry.
  Existing explicit ticks keep their original timing.
- Added three JVM regression cases for deduplication, promotion and retaining an existing explicit tick. All three
  failed before the fix. Together with the accessor and packet fixes, this adds six JVM and two Forge cases.
- The four touched class surfaces retain their callable member names and descriptors. The packaged inventory grows
  from 444 to 445 classes solely for the packet traversal callback; no existing class is removed.

### 2026-09-04 - Scoped modern Java compilation

- Kept Scala 2.11.5 and joint Java compilation on Java 8. Scalac resolves `StackAnalyserLogic` through its source
  path; a separate JDK 25 task compiles that helper with `--release 21`, and JVM Downgrader supplies only Java 8
  output to normal compilation consumers, tests, Forge, and packaging. The explicit downgrade output declaration
  also covers clean builds. The existing forced Scala-compilation guard remains.
- Applied the characterized Java 21 instruction-type switch. Normal and clean builds pass 398 JVM tests, the same
  398 frozen JVM consumer tests, and 237 Java 8 Forge tests. All 445 dev/release classes remain Java 8 compatible;
  with matching version metadata, only the helper changes and it exactly matches the isolated prototype. All
  retained Scala classes and 116 generated ASM dumps are identical. Source-jar contents, formatting, and checkstyle
  pass. Evidence and reproduction commands are in `docs/migration/README.md#modern-java-readability-policy` and ignored `run/jvmdg-trial/`.
- Prefer completing useful remaining Scala behavior extractions before broad Java syntax changes. Most remaining
  Scala declarations preserve model/trait binary contracts; zero Scala is a separate compatibility decision.

### 2026-09-04 - StackAnalyser constructor initialization

- Committed nine JVM characterization cases first as `929a704`, against the untouched Scala constructor.
  They cover direct constructor arguments despite overridden getters, virtual receiver/parameter pushes and
  `setL` slot order, descriptor mutation and malformed descriptors, wide aliases and formal parameter indices,
  receiver/parameter callback failures, delayed handler publication, duplicate/null keys, node identity, partial
  maps on failure, repeated virtual map reads and Scala-backed handler buffers' `foreach` dispatch.
- Moved initialization control flow to the existing `StackAnalyserLogic.initialize` Java helper. Scala retains
  field initialization and one handler callback: accessing its private map from that closure preserves the
  mangled field/accessor and virtual reads before each handler key. Java retains `asScalaBuffer(...).foreach`
  so wrapping a Scala buffer as a Java list does not bypass its overrides. Constructor arguments are passed
  directly; callback changes to the descriptor and handler list remain visible at the original points.
- Normal and clean formatting/checkstyle/build/Forge checks pass with 407 JVM tests, the same 407 frozen JVM
  consumers, and 237 Java 8 Forge tests, with zero failures/errors/skips. The frozen baseline jar, sources,
  compiled tests, reports, comparison tools and 116 matching ASM dumps are saved under ignored
  `run/migration-stack-initialization-reference/`. The version override matches the frozen test commit because
  two existing consumers inline that version; the normal final build still uses the actual Git version.
- Preserved all 443 retained class/member APIs, all 17 ScalaSignature payloads and 3,727 unrelated method bodies.
  The parameter-loop closure disappears and the handler closure is identical after compiler renumbering. Forty
  retained model/companion classes differ only in debug metadata after import removal. The helper adds two
  references to existing nested models in its `InnerClasses` table. These are covered compiler artifacts, with
  no new effective divergence. Dev/release jars contain 444 Java 8 classes; packaged sources match both edited
  sources and the helper introduces no JVM Downgrader runtime-stub reference.
- Reused the scoped modern Java build without configuration changes. Sources remain 224 Java files and nine
  Scala files / 779 nonblank lines. Next bounded candidate: `StackAnalyser.Const.getType`, preserving its
  case-class, product and serialization contracts. The simple model getters and full Scala shell replacement
  remain separate from useful behavior extraction.

### 2026-09-04 - StackAnalyser constant types

- Committed eight JVM characterization cases first as `89f31e3`, against the untouched Scala `Const.getType`.
  They cover all eight boxed primitive classes without numeric coercion, fresh object types for null and strings,
  unsupported numbers/ASM types/unit/arrays/string-like objects, virtual accessor read counts, message formatting
  and unwrapped accessor/formatting failures. The unsupported branch reads `c` a second time for its message;
  even a supported second value is formatted without reclassification. A null-returning `toString` prints `null`.
- Extracted classification into the existing package-private `StackAnalyserLogic.constType`, using direct type
  checks and a second virtual accessor call only for failures. Scala retains the entire case class, companion,
  product/copy methods and serialization shape. The scoped modern Java build is reused without configuration or
  dependency changes; the only extra compiler method handles the failure string concatenation.
- The build, formatting, checkstyle, frozen-consumer lane and Java 8 Forge tests pass, including verification from a
  clean build after stopping the Gradle daemon: 415 JVM tests, 415 frozen JVM consumers and 237 Forge tests, with zero
  failures/errors/skips. Both jars contain 444 Java 8 classes, their five `@Mod` versions match their filenames, and
  both edited sources match the source jar.
- Preserved all 443 retained class/member APIs, all 17 ScalaSignature payloads and 3,731 unrelated method bodies.
  The helper retains every previous member and adds only `constType` and its private synthetic concatenation
  method. No class is added or removed, no JVM Downgrader runtime-stub reference is introduced, and there is no new
  effective compatibility divergence. The consumer source scan found no direct `StackAnalyser` references.
- Baseline jar/sources, frozen compiled tests/resources, JVM/Forge reports, generated dumps, comparison tools and
  build logs are saved under ignored `run/migration-stack-const-reference/`. Rerun `frozen-consumers.gradle` with
  `VERSION` from `version.txt` to preserve the two existing frozen tests' inlined version assertions; ordinary final
  builds still use the actual Git version. `verify.py`, `Compare.java` and `VerifyVersions.java` check the artifacts.
- Sources now total 224 Java files and nine Scala files / 767 nonblank lines. The next bounded candidate is
  `ASMMixinCompiler.FieldMixin.accessName`: characterize private-flag selection, owner mangling, null behavior and
  virtual accessor/failure ordering while retaining the case-class model.

### 2026-09-04 - FieldMixin accessor names

- Committed seven JVM characterization cases first as `ddeed27`, against the untouched Scala `accessName` body.
  They cover every access-flag bit and combinations, literal slash-to-dollar owner mangling, unchanged field names,
  null owner/name behavior, virtual `access` then `name` reads, unused descriptors and unwrapped accessor failures.
  Private fields process the owner before reading the name; a null private owner therefore fails between the two
  reads. Non-private fields ignore the owner and return the exact name reference, including null.
- Moved the body to `MixinClassGenerator.fieldAccessName`, retaining virtual accessor calls and evaluation order.
  The existing Java 8 joint-compilation path handles this helper; no modern-Java build change or dependency is
  needed. Scala retains the complete case class, companion, product/copy methods and serialization shape. Its
  return type remains inferred: an explicit Scala `String` annotation changes the ScalaSignature payload even
  though the JVM descriptor stays the same.
- Normal and clean builds pass formatting/checkstyle, 422 JVM tests, all 422 frozen compiled JVM consumers, and
  237 Java 8 Forge tests, with zero failures/errors/skips. The clean run follows a Gradle daemon stop. All 116
  generated ASM dump names and hashes match, including Java/Scala trait registration and generated field access.
  Dev/release jars contain 444 Java 8 classes and packaged edited sources match the source jar.
- Preserved all 443 retained class/member APIs, all 17 ScalaSignature payloads and 3,733 unrelated method bodies.
  The Java helper retains every previous member and adds only the package-private `fieldAccessName` method.
  No class is added or removed and no new effective divergence is introduced. The consumer source scan found no
  direct `FieldMixin` or `accessName` references; generated trait behavior remains the integration contract.
- Evidence is saved under ignored `run/migration-field-access-reference/`: baseline jar/sources, frozen compiled
  tests/resources, reports, generated dumps, logs, `Compare.java`, `VerifyVersions.java` and `verify.py`.
  Use `frozen-consumers.gradle` and `VERSION` from `version.txt` for the frozen lane because two existing tests
  inline the original version. Ordinary final builds use the actual Git version.
- Sources remain 224 Java files and nine Scala files / 765 nonblank lines. Next candidate:
  `ASMMixinCompiler.MixinInfo.linearise`, with characterization of recursive parent order, repeated/diamond parents,
  virtual collection/parent dispatch and null/failure behavior before extraction.

### 2026-09-04 - MixinInfo linearisation

- Committed seven JVM characterization cases first as `d93b09d`, against the untouched Scala method. They cover
  depth-first parent order, duplicate/diamond ancestors, identity, mutable and immutable collection builders,
  virtual parent results, nulls, callback failures and erased collection casts. A custom `flatMap` can return only
  `SeqLike`; its virtual append supplies the final `Seq`, including a null result. Preserve that intermediate cast.
- Extracted traversal into `MixinClassGenerator.linearise`, reusing its Java callback adapter while keeping Scala
  `flatMap`, append and `Seq.canBuildFrom` dispatch. Recursion still invokes each parent's virtual `linearise`.
  Deduplication stays in composite generation, after traversal. Model, companion, product and serialization APIs
  remain Scala. The unreferenced `MixinInfo$$anonfun$linearise$1` class disappears under the existing compiler-artifact
  ledger entry; no new effective divergence or dependency is introduced.
- Normal and clean builds pass formatting/checkstyle, 429 JVM tests, all 429 frozen JVM consumers and 237 Java 8 Forge
  tests, with zero failures/errors/skips. Checks preserve 442 retained class/member APIs, all 17 ScalaSignature
  payloads and 3,731 unrelated method bodies. Both jars contain 443 Java 8 classes, with matching packaged sources.
  Evidence, frozen consumers and reproducible comparison tools are under ignored `run/migration-linearise-reference/`;
  use its `version.txt` for frozen version assertions.
- Source totals remain 224 Java files and nine Scala files / 765 nonblank lines. The next candidate is
  `ScalaSignature.Bytes.section`, preserving clamping, copy boundaries and virtual getter/failure ordering.

### 2026-09-04 - ScalaSignature byte sections

- Committed six JVM characterization cases first as `13de853`, against the untouched Scala `Bytes.section`.
  They cover independent drop/take clamping including integer extremes, fresh empty/full/partial result arrays,
  source/result isolation, virtual getter reads, mutations during `pos` and `len`, and every getter failure.
  A null source still evaluates `pos` before failing; an empty dropped array still evaluates `len`.
- Extracted slicing into `ScalaSignatureParser.section` using `Arrays.copyOfRange` and `Arrays.copyOf`. The first
  copy remains between the `pos` and `len` reads: mutations from `pos` are visible, while mutations from `len` are
  excluded. This holds even for zero/negative positions. Scala retains the case class, companion, product/copy
  methods and serialization shape. Ordinary Java 8 joint compilation remains sufficient.
- Normal and clean formatting/checkstyle/build/Forge checks pass: 435 JVM tests, all 435 frozen JVM consumers and 237
  Java 8 Forge tests, with zero failures/errors/skips. Both jars contain 443 Java 8 classes; packaged sources match.
  The comparison preserves 442 retained class/member APIs, all 17 ScalaSignature payloads and 3,732 unrelated method
  bodies. Only `Bytes.section` and the parser helper have changed class bytes; the helper adds one package-private
  method. No new effective divergence is introduced.
- Evidence and rerun tools are saved under ignored `run/migration-bytes-section-reference/`, including baseline
  jar/sources, frozen tests/resources, reports, dumps and logs. Use its `version.txt` with `frozen-consumers.gradle`
  for the existing inlined-version tests. The prior linearisation reference remains independently reproducible.
- Source totals remain 224 Java files and nine Scala files / 765 nonblank lines. Next bounded candidate:
  `ScalaSignature.TypeRef.jName`, preserving name normalization, aliases and virtual dispatch while retaining
  path-dependent model declarations and trait bridges. Descriptor conversion remains a separate target.

### 2026-09-04 - ScalaSignature type names

- Committed seven JVM characterization cases first as `ac1182e`, against the untouched Scala `TypeRef.jName`.
  They cover literal dot-to-slash conversion, exact normalized Any/AnyRef aliases, unchanged string identities,
  primitive/descriptor-looking inputs, virtual name reads, default symbol/full lookup, nulls and getter failures.
  They also exercise SingleType's retained super call and descriptor routing, NoType's name override, and the
  legacy static `TypeRef$class.jName(TypeRef)` descriptor with a proxy that supplies only `name()`.
- Extracted normalization into `ScalaSignatureParser.typeName(String)`. Scala retains the single virtual `name`
  read before entering Java, so the helper needs no path-dependent model parameter or callback. Trait declarations,
  static helper/forwarder descriptors, SingleType's super shell and serialization shapes remain unchanged. The
  existing Java 8 joint-compilation path suffices; no dependency or build configuration changes are needed.
- Normal and clean builds pass formatting/checkstyle, 442 JVM tests, all 442 frozen JVM consumers, and 237 Java 8
  Forge tests, with zero failures/errors/skips. Both jars contain 443 Java 8 classes, with matching packaged sources.
  Comparison preserves 442 retained class/member APIs, all 17 ScalaSignature payloads and 3,733 unrelated method
  bodies. The Java helper adds only one package-private method; no class is added or removed and no new effective
  divergence is introduced.
- Evidence, frozen tests/resources and rerun tools are under ignored `run/migration-type-name-reference/`.
  Use its `version.txt` with `frozen-consumers.gradle` to retain the existing inlined-version assertions. Ordinary
  final builds use the committed Git version. Source totals are 224 Java files and nine Scala files / 762 nonblank
  lines. Next candidate: `ScalaSignature.TypeRef.jDesc`, preserving primitive/array cases, virtual fallback and
  failure order while retaining the model overrides and trait bridges.

### 2026-09-04 - ScalaSignature type descriptors

- Committed seven JVM characterization cases first as `b3b3f10` against the untouched `TypeRef.jDesc`. They pin
  exact primitive/array matching, legacy reference descriptors (including Char), virtual fallback and repeated
  symbol reads, null/failure order, SingleType/NoType behavior, TypeRefType's first array argument and the static
  trait helper with an independent proxy consumer.
- Extracted conversion into `ScalaSignatureParser.typeDescriptor(Object)`, keeping path-dependent types inside
  Java method bodies. Literal equality retains the null-name fallback, and `jName()` remains virtual and lazy.
  Scala declarations, array override, super dispatch and serialization shapes remain unchanged.
- Both jars contain 443 Java 8 classes and matching packaged sources. Comparison preserves 442 retained class/member
  APIs, all 17 ScalaSignature payloads, 3,734 unrelated method bodies and all 116 generated dumps. Only one
  package-private helper method is added; there is no class inventory change or new divergence.
- Evidence is under ignored `run/migration-type-descriptor-reference/`, including the reference jar, compiled tests,
  resources, reports and comparison tools. Frozen runs use its `version.txt`; final builds use the committed version.
  Source totals are 224 Java files and nine Scala files / 751 nonblank lines. Next: `TMethodType.jDesc` assembly,
  preserving parameter/return lookup order, nulls, failures and the retained trait/models.

### 2026-09-04 - ScalaSignature method descriptors

- Committed seven JVM characterization cases first as `d61aed2` against the untouched `TMethodType.jDesc`. They
  cover MethodType/ParameterlessType assembly, ordered virtual parameter info/return-type/descriptor reads,
  repeated parameters and queries, literal null/invalid descriptors, each getter failure and null-chain boundary,
  plus the legacy static trait helper using an independent proxy. Parameter `jDesc` and nested `params` must not
  be used; the method return type is read only after every parameter descriptor completes.
- Extracted assembly into `ScalaSignatureParser.methodDescriptor(Object)`. Its Java callback retains the original
  Scala List builder, map and mkString semantics, using the existing TraversableLike bridge pattern to resolve
  javac's ambiguous Scala List map overloads. Path-dependent models stay inside method bodies; Scala trait/model
  declarations, forwarders, generic signatures and serialization shapes remain unchanged.
- All 116 generated dumps match the reference; both jars contain 443 Java 8 classes and matching packaged sources.
  Comparison preserves 441 retained class/member APIs, all 17 ScalaSignature payloads and 3,732 unrelated method
  bodies. The sole inventory replacement is `TMethodType$$anonfun$jDesc$1` with `ScalaSignatureParser$1`, covered by
  the existing unreferenced compiler-artifact ledger entry. The helper gains one package-private method; no new
  effective divergence is introduced.
- Evidence is under ignored `run/migration-method-descriptor-reference/`, including the reference jar, frozen
  tests/resources, reports and comparison tools. Frozen runs use its `version.txt`; final builds use the committed
  version. Sources remain 224 Java files and nine Scala files / 749 nonblank lines. Next: `ClassSymbolRef.jInterfaces`,
  retaining its List contract, virtual lookup, ordered mapping and failure behavior.

### 2026-09-04 - ScalaSignature interface names

- Committed seven JVM characterization cases first as `6ccb83f` against the untouched `ClassSymbolRef.jInterfaces`.
  They pin ClassSymbol/ObjectSymbol parent exclusion, ordered names and duplicates, the empty List singleton,
  string identity and null names, repeated queries, virtual info/interfaces/jName dispatch and failure boundaries.
  A proxy also exercises the exact static trait-helper descriptor without an outer instance.
- Added `ReferenceScalaInterfaceNames`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev jar
  at `240a788`. Its Scala caller and ClassSymbol/ClassType subclasses prove compatibility with old bytecode and
  overridden path-dependent methods. Source, encoded classes and SHA-256 provenance are committed under
  `src/test/fixtures/` and `src/test/resources/compat/`; do not regenerate them against the port.
- Extracted the mapping into `ScalaSignatureParser.interfaceNames(Object)`. The Java callback retains virtual
  lookup and Scala List map/builder behavior, using the existing TraversableLike bridge pattern. The helper returns
  `List<String>`; Scala retains inferred types, trait/model declarations, forwarders and serialization shapes.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 441 retained class/member APIs, all
  17 ScalaSignature payloads and 3,733 unrelated method bodies. The sole inventory replacement is
  `ClassSymbolRef$$anonfun$jInterfaces$1` with `ScalaSignatureParser$2`, covered by the existing unreferenced
  compiler-artifact ledger entry. No new effective divergence is introduced.
- Evidence is under ignored `run/migration-interface-names-reference/`, including the fixture/reference jars,
  frozen tests/resources, reports and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Sources remain 224 Java files and nine Scala files / 749 nonblank lines. Next:
  `ClassSymbolRef.toString`, preserving formatting, virtual reads and failure order.

### 2026-09-04 - ScalaSignature class-symbol strings

- Committed seven JVM characterization cases first as `1234112` against the untouched
  `ClassSymbolRef.toString`. They cover concrete ClassSymbol/ObjectSymbol runtime names, anonymous suffix trimming,
  punctuation and nulls, lowercase two's-complement hexadecimal flags, negative/limit info IDs, repeated virtual
  reads, owner formatting and every getter/failure boundary. A proxy exercises the exact static trait-helper
  descriptor without needing an outer ScalaSignature instance.
- Extracted formatting into `ScalaSignatureParser.classSymbolString(Object)`. Java retains the original greedy
  class-name regular expression, left-to-right virtual getter order, object-to-string conversion and hexadecimal
  representation. Scala keeps the inferred return type, trait/case-class declarations, forwarders, products and
  serialization shapes.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,737 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-class-symbol-string-reference/`, including the reference jar, frozen
  tests/resources, reports and comparison tools. Frozen runs use its `version.txt`; final builds use the committed
  version. Source totals are 224 Java files and nine Scala files / 748 nonblank lines. Next:
  `MethodSymbol.toString`, preserving formatting, virtual reads and failure order.

### 2026-09-04 - ScalaSignature method-symbol strings

- Committed seven JVM characterization cases first as `d81195a` against the untouched `MethodSymbol.toString`.
  They cover its fixed prefix for concrete and anonymous subclasses, punctuation/nulls, lowercase two's-complement
  hexadecimal flags, negative/limit info IDs, repeated virtual reads, owner formatting and every failure boundary.
  Derived full-name, info, descriptor and flag-query methods must not be read during formatting.
- Extracted formatting into `ScalaSignatureParser.methodSymbolString(Object)`. Java retains left-to-right virtual
  getter order, object-to-string conversion and hexadecimal representation. Scala keeps the inferred return type,
  case-class declaration, symbol/flag bridges, product members and serialization shape.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,738 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-method-symbol-string-reference/`, including the reference jar, frozen
  tests/resources, reports and comparison tools. Frozen runs use its `version.txt`; final builds use the committed
  version. Source totals are 224 Java files and nine Scala files / 747 nonblank lines. Next:
  `TypeRefType.jDesc`, preserving array/super routing, virtual reads and malformed-input behavior.

### 2026-09-04 - ScalaSignature applied-type descriptors

- Committed seven JVM characterization cases first as `5493086` against the untouched `TypeRefType.jDesc`. They pin
  the array branch's first argument descriptor, fallback routing through `TypeRef.jDesc` and `jName`, two/three
  virtual name reads, repeated uncached queries, malformed/null argument lists, null descriptors, failure boundaries
  and the retained `TMethodType` parameter/return view.
- Added `ReferenceScalaAppliedTypeDescriptor`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev
  jar at `d5bf16f`. Its frozen Scala caller and `TypeRefType` subclass preserve the old method descriptor and virtual
  override dispatch. Source, encoded classes and SHA-256 provenance are committed under `src/test/fixtures/` and
  `src/test/resources/compat/`; do not regenerate them against the port.
- Extracted the applied-type branch into `ScalaSignatureParser.appliedTypeDescriptor(Object)`. The array path reads
  only the first type argument; the fallback reuses `typeDescriptor`, preserving its virtual name and `jName` order.
  Scala retains the inferred return type, case class, method-type mixin, product members and serialization shape.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,739 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-applied-type-descriptor-reference/`, including the reference/fixture jars,
  frozen tests/resources, reports, logs and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Source totals are 224 Java files and nine Scala files / 744 nonblank lines. Next:
  `ClassSymbolRef.full`, preserving owner/name concatenation, virtual reads and failure order.

### 2026-09-04 - ScalaSignature class-symbol full names

- Committed seven JVM characterization cases first as `05c2792` against the untouched `ClassSymbolRef.full`. They
  pin concrete class/object names, punctuation, literal null full/name results, repeated owner/full/name read order,
  null-owner and getter failure boundaries, unused symbol members and the exact static trait-helper descriptor.
- Added `ReferenceScalaClassSymbolFull`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev jar
  at `24c7723`. Its frozen Scala caller and direct `ClassSymbolRef` implementation preserve the old `$class.full`
  forwarder and virtual overrides. Source, encoded classes and SHA-256 provenance are committed under
  `src/test/fixtures/` and `src/test/resources/compat/`; do not regenerate them against the port.
- Extracted concatenation into `ScalaSignatureParser.classSymbolFull(Object)`. Java retains owner getter, virtual
  owner `full()` and name getter order plus Java/Scala null string conversion. Scala keeps the inferred return type,
  trait helper, case-class forwarders, path-dependent declarations, products and serialization shapes.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,740 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-class-symbol-full-reference/`, including the reference/fixture jars,
  frozen tests/resources, reports, logs and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Sources remain 224 Java files and nine Scala files / 744 nonblank lines. Next:
  `MethodSymbol.full`, preserving owner/name concatenation, virtual reads and failure order.

### 2026-09-04 - ScalaSignature method-symbol full names

- Committed seven JVM characterization cases first as `55f21ef` against the untouched `MethodSymbol.full`. They pin
  constructor and punctuation names, literal null full/name results, repeated owner/full/name read order, null-owner
  and getter failure boundaries, unused derived members and a frozen Scala caller's descriptor/null behavior.
- Added `ReferenceScalaMethodSymbolFull`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev jar
  at `64f5abc`. Its frozen Scala caller preserves source-compiled dispatch while Java accessor overrides exercise the
  public JVM virtual surface. Source, encoded class and SHA-256 provenance are committed under `src/test/fixtures/`
  and `src/test/resources/compat/`; do not regenerate them against the port.
- Extracted concatenation into `ScalaSignatureParser.methodSymbolFull(Object)`. Java retains owner getter, virtual
  owner `full()` and name getter order plus Java/Scala null string conversion. Scala keeps the inferred return type,
  case class, path-dependent declaration, products and serialization shape.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,741 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-method-symbol-full-reference/`, including the reference/fixture jars,
  frozen tests/resources, reports, logs and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Sources remain 224 Java files and nine Scala files / 744 nonblank lines. Next:
  `ClassSymbolRef.jParent`, preserving info/parent/name lookup order and failures.

### 2026-09-04 - ScalaSignature class parent names

- Committed seven JVM characterization cases first as `6e3a301` against the untouched `ClassSymbolRef.jParent`.
  They pin ClassSymbol/ObjectSymbol results, repeated virtual info/parent/jName order, first-parent selection without
  interface reads, empty-list behavior, null results/chains, getter failures and the static trait-helper descriptor.
- Added `ReferenceScalaClassParent`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev jar at
  `a670ea3`. Its frozen Scala caller, direct `ClassSymbolRef` implementation and `ClassType` subclass preserve the old
  `$class.jParent` forwarder and virtual dispatch. A frozen Scala constructor also covers default head/empty-list
  behavior because javac's accepted extra-outer `ClassType` call resolves to a nonexistent descriptor at runtime.
  Source, encoded classes and SHA-256 provenance are committed under `src/test/fixtures/` and resources.
- Extracted the lookup chain into `ScalaSignatureParser.classParentName(Object)`. Java retains virtual info, parent
  and `jName` order and passes through a null name. Scala keeps the inferred return type, trait helper, case-class
  forwarders, path-dependent declarations, products and serialization shapes.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,742 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-class-parent-reference/`, including the reference/fixture jars, frozen
  tests/resources, reports, logs and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Sources remain 224 Java files and nine Scala files / 744 nonblank lines. Next:
  `MethodSymbol.jDesc`, preserving info/descriptor lookup order and failures.

### 2026-09-04 - ScalaSignature method-symbol descriptors

- Committed seven JVM characterization cases first as `16f4568` against the untouched `MethodSymbol.jDesc`. They pin
  literal and null descriptors, repeated virtual info/descriptor read order, changing uncached info results, null info,
  both failure boundaries, unused method shape/symbol members and a frozen Scala caller's descriptor/null behavior.
- Added `ReferenceScalaMethodSymbolDescriptor`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev
  jar at `27863b4`. Its frozen Scala caller and `MethodSymbol` subclass preserve the original descriptor and virtual
  info override. Source, encoded classes and SHA-256 provenance are committed under `src/test/fixtures/` and resources.
- Extracted the lookup into `ScalaSignatureParser.methodSymbolDescriptor(Object)`. Java retains virtual info and
  `jDesc` order and passes through a null descriptor. Scala keeps the inferred return type, case class, path-dependent
  declaration, products and serialization shape.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,743 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-method-symbol-descriptor-reference/`, including the reference/fixture jars,
  frozen tests/resources, reports, logs and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Sources remain 224 Java files and nine Scala files / 744 nonblank lines. Next:
  `MethodSymbol.info`, preserving info-id/evaluation lookup order, casts and failures.

### 2026-09-04 - ScalaSignature method-symbol info evaluation

- Committed seven JVM characterization cases first as `9859aa8` against the untouched `MethodSymbol.info`. They pin
  exact/null evaluated results, repeated virtual info-ID/eval order, changing uncached IDs and results, the invalid-type
  cast failure, both callable failure boundaries, unused symbol/type members and the frozen Scala return descriptor.
- Added `ReferenceScalaMethodSymbolInfo`, compiled once with Scala 2.11.5 under Java 8 against the unchanged dev jar at
  `257d0e4`. Its frozen Scala caller preserves the path-dependent `TMethodType` return and null-receiver behavior.
  Source, encoded class and SHA-256 provenance are committed under `src/test/fixtures/` and resources.
- Extracted evaluation into `ScalaSignatureParser.methodSymbolInfo(ScalaSignature, Object)`. Scala passes its enclosing
  signature explicitly because javac cannot call the synthetic outer accessor. Java retains virtual `infoId` then
  `evalT` dispatch; Scala retains the final `TMethodType` cast, case class, products and serialization shape.
- Both jars contain 443 Java 8 classes and matching sources; comparison preserves 442 retained class/member APIs, all
  17 ScalaSignature payloads and 3,744 unrelated method bodies. The helper adds one package-private method; no class
  inventory or effective divergence changes.
- Evidence is under ignored `run/migration-method-symbol-info-reference/`, including the reference/fixture jars,
  frozen tests/resources, reports, logs and comparison tools. Frozen runs use its `version.txt`; final builds use the
  committed version. Sources remain 224 Java files and nine Scala files / 747 nonblank lines. Next:
  `ClassSymbolRef.info`, preserving outer/info-id/evaluation order, casts, trait bridges and failures.

### 2026-09-04 - TileMultipart part-list accessor compatibility

- Restored virtual `partList()` / `partList_$eq()` dispatch throughout the Java tile implementation, including
  traversal, mutation, copying, serialization and queries. Retained the optimized list traversal, captured-sequence
  behavior and detached-part guard; light and explosion queries retain the original accessor lookup counts.
- Four new regression cases compiled against the original `ForgeMultipart-1.7.12-dev.jar` pass on Scala, fail on
  the unfixed port and pass unchanged after the fix. Build/style checks, 523 JVM tests and 237 Java 8 Forge tests
  pass. The tile's declared JVM signatures and private fields are unchanged.
- Audited the supplied GTNH consumer checkouts: no `TileMultipart` subclass overriding these accessors was found.
  This fixes an extension-contract regression, without claiming a demonstrated current-pack gameplay failure.
  Evidence is under ignored `run/conversion-review/`; migration-plan changes remain deferred until the review fixes
  are complete.

### 2026-09-04 - Microblock state accessor compatibility

- Restored virtual shape/material getters and setters for geometry, material lookup, drops, pick-block, NBT and
  description/incremental packets. The backing fields, JVM signatures and serialized formats remain unchanged.
- Three new regression cases compiled against the original Scala jar pass there, fail on the unfixed Java port
  and pass unchanged after the fix. The test material explicitly implements the original Scala interface defaults
  and uses the retained registry companion so the same test source compiles against both versions.
- Build/style checks, 526 JVM tests and 237 Java 8 Forge tests pass. ProjectRed extends `Microblock`, and consumers
  use its shape/material accessors, but no supplied consumer overriding those accessors was found. Evidence is
  under ignored `run/conversion-review/`; no migration-plan changes were made.

### 2026-09-04 - BlockMicroMaterial property accessor compatibility

- Restored virtual block, metadata, icon-transform and registry-key reads across material properties, items,
  strength, icon loading and rendering. Preserved the constructor's direct argument use, private reflective fields,
  client-only annotations, safe-icon exception boundary and original render/accessor evaluation order.
- Four new cases compiled against the original Scala jar pass there, fail on the unfixed port and pass unchanged
  after the fix. Together the three fixes add eleven JVM cases. Build/style checks, 530 JVM tests, 237 Java 8 Forge
  tests and all 14 assertions in the original-jar accessor probe pass. All 519 archived JVM consumers also pass
  when built with their recorded version; their two embedded-version assertions require that override.
- Compared against the pre-fix dev jar: all 443 class/member APIs, all 17 ScalaSignature payloads and 3,702 unrelated
  method bodies are unchanged, allowing only the build-version literal to differ. The fixes change 44 method bodies;
  all 116 generated tile dumps are identical. Both jars contain 443 Java 8 classes and five matching mod versions.
- ProjectRed, Chisel and ExtraUtilities extend `BlockMicroMaterial`; no supplied consumer overrides the affected
  accessors. These are verified extension-contract regressions, without a demonstrated current-pack gameplay
  failure. Evidence is under ignored `run/conversion-review/`; migration-plan changes remain deferred.

### 2026-09-04 - Java material enumeration API

- Committed two baseline cases first as `9b78099`, pinning the shared legacy array, uninitialized access, server ID
  ordering, missing slots and empty maps. Saved the pre-API jar and all 532 compiled tests before implementation.
- Added `MicroMaterialRegistry.materialCount()` beside the existing indexed name/material lookups. It reports the
  active ID-map length and explicitly rejects an uninitialized map. The static and companion `getIdMap()` methods
  keep their original descriptors and live-array behavior, with deprecation documentation pointing at the Java API.
- Added three JVM cases, a compiling Java enumeration example and one Forge handshake/registry case. Normal/clean build/style
  checks, 535 JVM tests, 238 Java 8 Forge tests and all 532 frozen pre-change tests pass. The example compiles against
  only the dev jar and Java 8 APIs and has no Scala bytecode references; FMP's runtime dependencies remain in place.
- Binary comparison retains all 443 class APIs with exactly one new public static method and two deprecations.
  All 17 ScalaSignature payloads, 3,746 existing method bodies and 116 generated dumps are unchanged, allowing only
  the expected deprecation metadata and build-version literals. No production class or dependency is added.
- Added the material-enumeration guide and the first consumer adoption ledger rows. UtilitiesInExcess's two source
  call sites and Extra Utilities' tuple-array readers have supported replacements; consumer changes, releases and
  pack adoption remain pending. Evidence is under ignored `run/migration-material-enumeration-reference/`.

### 2026-09-04 - Java tile collection and traversal API

- Committed three baseline cases first as `5c1e760`, pinning captured Java views, lifecycle override dispatch,
  non-null binding checks and callback failure propagation. Saved the pre-API jar, source, reports, generated dumps
  and all 538 compiled JVM tests before implementation.
- Added `TileMultipart.forEachPart(Consumer<TMultiPart>)` through the existing adapter and virtual `operate` hook.
  Documented the existing `jPartList()` view and deprecated `partList()`/`operate(Function1)` for callers, retaining
  their descriptors, bodies and override contracts. Lifecycle still calls `operate` directly; no new lifecycle hook
  or independently maintained traversal was introduced.
- Added four JVM cases, a compiling Java example and one Forge generated-tile case that removes/adds real parts
  during traversal. Normal/clean style and build checks pass: 542 JVM tests, 239 Java 8 Forge tests and all 538 frozen
  pre-change tests. The example also compiles with Scala excluded from its classpath and has no Scala bytecode references.
- Binary comparison retains all 443 class APIs with exactly one new public instance method and two deprecations.
  All 17 ScalaSignature payloads, 3,747 existing method bodies and 116 generated dumps are unchanged, allowing only
  the expected deprecation metadata and build-version literals. No production class or dependency was added.
- Added the traversal guide and consumer ledger entries for ProjectRed, OpenComputers, AE2, Extra Utilities and
  GuideNH. Getter migration does not cover GuideNH's reflective setter/loading contracts. The supplied source search
  found no FMP `operate` overrides or `forEachPart` collisions; existing override support remains required.
- Rescanned the installed GTNH daily `2026-09-04+719`: 241 jars scanned, one FMP jar excluded, 27 consumers. All
  386 member/type/reflection rows match the frozen `+678` inventory; consumer version changes do not permit bridge
  removal. Reference checkouts were not modified. Evidence: `run/migration-part-traversal-reference/`.

### 2026-09-05 - Java tile loading/state API and documentation index

- Committed four JVM and one Forge baseline cases first as `92273bd`. They pin storage aliasing/null state, binding
  and cache-hook order, stale ticking/old bindings, partial failures, exact/assignable reflection and generated slots.
  Saved the pre-API jar, source, reports, 116 generated dumps and all 546 compiled JVM tests before implementation.
- Added `setPartList(List<TMultiPart>)` through the old setter and `loadPartList(Collection<TMultiPart>)` through
  the old loader. The Java setter copies non-null list storage and retains the null sentinel; loading retains the
  input's iteration behavior, callback order and partial failures. Old hooks/descriptors remain supported and deprecated.
- Changed the planned loader name after the `loadParts(Collection)` trial made javac require Scala's `Iterable`
  during overload resolution with a Java list. The distinct `loadPartList` entry lets the example compile without
  Scala on its classpath. Added this compile check to the remaining API gates; no existing method was renamed.
- Added three JVM cases, a compiling Java example and two Forge cases for server slots/notifications and generated
  client storage/loading/render-cache queries. Normal/clean build/style checks pass with 549 JVM and 242 Java 8 Forge
  tests; all 546 frozen pre-change JVM tests also pass. Real client-world/GPU preview checks remain manual.
- All 443 class APIs retain their existing members, with exactly two new public instance methods and two deprecations.
  All 17 ScalaSignature payloads, 3,748 existing method bodies and 116 generated dumps remain unchanged, allowing only
  the expected deprecation metadata and build-version literals. No production class or dependency is added.
- Added `docs/API.md` as the consumer entry point, a loading/storage guide, compiling examples, and reciprocal guide
  links. Updated the plan, ABI notes and GuideNH/Schematica adoption ledger; reference checkouts remain unchanged and
  consumer releases/pack adoption are pending. Evidence: `run/migration-part-loading-reference/`.

### 2026-09-05 - Java collection occlusion query

- Committed three JVM baseline cases and stronger generated-trait assertions first as `6744f5b`. They pin pair order,
  both rejection directions, lazy null behavior, original exceptions and replacement-hook dispatch. Saved the pre-API
  jar, source, reports, 116 generated dumps and all 552 compiled JVM tests before implementation.
- Added `TileMultipart.testOcclusion(Collection<? extends TMultiPart>, TMultiPart)` as an adapter through the old
  virtual `occlusionTest(Seq, TMultiPart)`. It takes a shallow immutable snapshot, retains order/duplicates/identities,
  and accepts subtype collections. The distinct name avoids a Scala/Java overload pair. The legacy hook is deprecated
  for callers, with its descriptor and body unchanged; placement/replacement still dispatch directly through it.
- Four new JVM cases and a compiling Java shape example cover snapshot/callback behavior, non-list/subtype inputs,
  failures, override routing and touching/overlapping bounds. One new Forge case proves aggregate partial-occlusion
  rejection through the generated trait when plain pair tests would accept the same parts.
- Normal/clean build/style checks pass: 556 JVM tests, 243 Java 8 Forge tests and all 552 frozen pre-change JVM tests.
  The Java example compiles with Scala excluded from the classpath and contains no Scala bytecode references.
  All 443 existing class APIs, 17 ScalaSignature payloads, 3,750 existing method bodies and 116 generated dumps are
  retained, with exactly one added public method and one deprecation. Only expected deprecation metadata and build
  versions differ; no production class or dependency is added.
- Added the occlusion guide and API index entry. No direct consumer use of the two-argument tile hook or collision
  with the new name was found. The separately load-bearing Scala box-list `NormalOcclusionTest.apply` contract now
  has an explicit plan row and is the next candidate. Reference checkouts remain unchanged; client previews and
  consumer release/adoption remain separate gates. Evidence: `run/migration-part-occlusion-reference/`.

### 2026-09-05 - Java box-versus-box occlusion query

- Committed three JVM baseline cases as `c7a3ed9` before changing production code. Both static and companion entries
  are checked for eager input collection, shallow snapshots, duplicate/callback order, short-circuiting, null handling
  and original failures. Saved the pre-API jar, sources, all 559 compiled JVM tests, reports and 116 generated dumps.
- Added `NormalOcclusionTest.testBoxes(Iterable<? extends Cuboid6>, Iterable<? extends Cuboid6>)` over the existing
  private Java copy/intersection helpers. The copy helper's private generic input now accepts subtypes; its body and
  descriptor are unchanged. Both legacy Scala box entries are deprecated without changing their bodies/descriptors.
- Four further JVM cases run the same contracts against the new entry and exercise a compiling geometry example,
  including touching tolerance and containment. The Java example compiles without Scala on its classpath.
- Normal/clean build and style checks pass: 563 JVM tests, 243 Forge tests and 559 frozen pre-change JVM tests.
  All 443 existing classes, 17 ScalaSignature payloads, 3,751 existing method bodies and 116 generated dumps remain;
  exactly one public method and two deprecations are added. Build versions and the private helper's generic widening
  are the only other expected metadata differences.
- Updated the API guide/index, ABI notes and adoption ledger for OpenComputers' two connection checks and
  ForgeRelocationFMP's combined geometry check. The installed `+719` rescan retains all 386 ABI/reflection rows across
  27 consumers. Reference checkouts are unchanged; consumer release/adoption and manual client/pack checks remain
  pending. Next: multipart factory registration. Evidence: `run/migration-box-occlusion-reference/`.

### 2026-09-05 - Java part factory registration

- Committed four Forge baseline cases as `e4deac8`. Real mod initialization exercises all eight old registration
  descriptors. Tests pin lazy construction, owner/array identity, partial duplicate failures, closed-state ordering,
  Boolean side adapters and the NBT/packet factory handoff. Saved the pre-API jar, registry sources, 563 JVM fixtures,
  the compiled Forge test mod, reports and 116 generated dumps before implementation.
- Added `registerPartFactory(IPartFactory2, String...)` as one forwarder over existing Java registration. The old
  same-name overload family required Scala `Seq`/`Function2` even with Java factory/string arguments. The distinct
  name and a complete Java factory example compile with Scala excluded. Both `IPartFactory2` sequence entries are
  now deprecated; older Boolean/function deprecations point to the supported replacement.
- The new entry runs through the existing registration cases, and an additional Forge example case checks fresh,
  unbound construction. Normal/clean build/style checks pass: 563 JVM and 248 Forge tests, all with zero failures,
  errors or skips. All 563 frozen JVM fixtures and the byte-identical archived Forge test mod's 247 tests also pass
  against the new implementation. The example ID sorts after built-ins so existing exact packet fixtures remain intact.
- All 443 classes, 17 ScalaSignature payloads, 3,752 existing method bodies and 116 generated dumps remain. Exactly
  one public method and two deprecations are added; only expected deprecation/build-version metadata differs.
- Added the factory guide/index entry, documented payload ownership and migration directions for ProjectRed and
  ForgeRelocationFMP, and retained all eight old registration descriptors. The `+719` scan retains all 386 member/type/
  reflection rows across 27 consumers. Reference checkouts and adoption status are unchanged. Registry lookup,
  external trait migration and physical-client/pack checks remain separate. Next API-table entry: render-ID accessors.
  Evidence: `run/migration-registration-reference/`.

### 2026-09-05 - Java render-ID accessors

- Committed two JVM cases and one Forge case as `3c22ab6` before changing production code. They pin the initial -1
  sentinel, shared static/companion storage, all integer values and block render-type reads. Saved the reference jar,
  sources, 565 compiled JVM fixtures, the Forge test mod, reports and 116 generated dumps.
- Added `getRenderID()` / `setRenderID(int)` as adapters over existing accessors. The four old static/companion
  accessors are deprecated, preserving their bodies/descriptors. Client renderer initialization/ID allocation and
  internal block/renderer call sites are unchanged. The Java example compiles without Scala.
- Normal/clean build/style checks pass: 566 JVM and 249 Forge tests. All 565 frozen JVM fixtures and the archived
  Forge mod's 249 cases pass without recompiling their callers. All 443 classes, 17 ScalaSignature payloads,
  3,753 existing method bodies and 116 generated dumps remain, with exactly two public methods and four deprecations
  added; only expected deprecation/build-version metadata differs.
- Added a lifecycle guide and API index entry. No direct consumer use/name collision was found; the `+719` scan still
  retains all 386 member/type/reflection rows across 27 consumers. Reference checkouts are unchanged. Physical-client
  allocation/registration/rendering remain manual gates. Next API-table entry: the tile conversion result.
  Evidence: `run/migration-render-id-reference/`.

### 2026-09-05 - Planned measured performance follow-up

- Added Phase 4b after checking the completed focused Phase 4 work. It covers realistic consumer/pack hot paths and
  broader startup, transition, client rendering, networking and memory costs, with repeated controlled comparisons.
- The protocol distinguishes changes inside FMP from the cost/benefit of migrating consumer calls on the same FMP
  artifact, retains correctness/compatibility gates, and requires raw evidence, variability and reported regressions.
  It can overlap consumer migration once representative API/extension workloads are stable. No new performance
  measurements or speedup claims are introduced by this plan update.

### 2026-09-05 - Named Java tile conversion result

- Committed three Forge characterization cases as `7396f2d` before production edits. Both legacy entries preserve
  existing-tile identity, null/false for nonconvertible blocks (including unrelated tile entities), and bound torch
  placeholders without world installation. Repeated conversion produces fresh objects; subsequent normal placement
  installs its own tile. Saved the reference jar, sources, 566 compiled JVM tests, Forge test mod and generated dumps.
- Added `getOrConvertTileResult(World, BlockCoord)` and immutable `TileConversionResult` with `getTile()` and
  `isConverted()`. The wrapper delegates to the unchanged tuple method; both old tuple entries are deprecated. The
  tile-only method remains supported, and internal generator/placement tuple calls are unchanged. The result uses
  Java 8 syntax at the existing joint-compilation boundary without new compiler machinery.
- Three additional Forge cases exercise the Java entry and example against the same characterization. The example
  lives in the Forge source set because lookup needs an initialized world; it also compiles standalone without Scala.
  Normal/clean style/build checks pass with 566 JVM and 255 Forge tests, zero failures/errors/skips. All 566 frozen
  JVM tests and the byte-identical archived Forge mod's 252 cases pass against the addition.
- All 443 old class APIs, 17 ScalaSignature payloads, 3,755 existing method bodies and 116 generated dumps remain.
  Added one result class and one static entry; only intended deprecation/build-version metadata changes on old
  members. All 444 packaged classes target Java 8. Source inventory is 225 Java and 9 Scala / 747 nonblank Scala lines.
- Added the conversion lifecycle guide and index entry, completed all ten Phase 9.1 table rows, and clarified that
  the broader extension/API audit is still open. No direct tuple consumer or new-name collision was found; the
  installed `+719` scan retains all 386 member/type/reflection rows across 27 consumers. Reference checkouts remain
  unchanged. Next bounded candidate: Schematica registry lookup. Consumer adoption, internal tuple users, external
  Java extensions, measured performance work and actual client/pack checks remain separate gates.
  Evidence: `run/migration-tile-conversion-reference/`.

### 2026-09-05 - Supported registered-factory lookup

- Traced Schematica `3b03ee937953` from private Scala map lookup through `MicroblockClass.create(client, materialId)`
  and NBT loading. Existing `loadPart` selects server construction, so it cannot replace that client-preview lookup.
  Added `MultiPartRegistry.getPartFactory(String)` as a direct read of the canonical map, returning the exact factory
  or null without logging, construction, registration mutation or dependency on the network ID map.
- Committed one JVM and one Forge characterization case first as `2a52980`, saving the reference jar, sources,
  compiled fixtures, reports and generated dumps. Legacy reflection and the Java API share checks for factory identity,
  string equality, case-sensitive misses, current-map changes and no construction callbacks. Forge checks all five
  microblock factories, exact reflective construction, server side, NBT material/shape and unbound ownership.
- Added one JVM and two Forge cases for the Java API, exact public reflection and a compiling example. The example
  compiles standalone without Scala. A baseline attempt at client microblock creation on the dedicated server failed
  because Forge strips `MicroblockClass.clientTrait()` there; real client construction/preview rendering remain manual,
  and no client coverage is claimed from the server fixture.
- Normal and clean style/build checks pass with 568 JVM and 258 Forge tests, zero failures/errors/skips. All 567 frozen
  JVM tests and the byte-identical archived Forge mod's 256 cases pass against the addition. All 444 class APIs,
  17 ScalaSignature payloads, 3,759 existing method bodies and 116 generated dumps remain, with exactly one static
  method added. The private Scala mutable-map field retains its exact name/modifiers/type and live backing.
- Added a factory lookup guide, typed and reflective migration examples, API-index links and a Schematica adoption
  ledger row. No new-name collision was found in supplied consumer sources; the `+719` scan still matches all 386
  member/type/reflection rows across 27 consumers. Checkouts remain reference-only; no consumer release/adoption is
  claimed. Next bounded task is Java client/server composite generation for Schematica and GuideNH; loading,
  material/shape restoration, all-parts rejection and notifications must retain their staged lifecycle.
  Evidence: `run/migration-factory-lookup-reference/`.

### 2026-09-05 - Staged Java composite-tile generation

- Traced Schematica and GuideNH generation followed by their distinct NBT/world/position/loading/notification steps.
  `MultipartHelper.createTileFromParts` constructs and loads a server tile immediately, so it cannot replace their
  staged client paths. Added static `MultipartGenerator.generateCompositeTile(TileEntity, Iterable<TMultiPart>, boolean)`
  through a Java iterable view over the existing companion behavior; no snapshot or new generation logic.
- Committed three Forge characterization cases first as `ab5654b`, preserving the reference jar, sources, compiled
  fixtures, reports and dumps. Both tile sides, exact reuse, capability changes, empty/duplicate inputs, no implicit
  loading/copying/invalidation, iterator evaluation/failures and exact companion reflection are covered.
- Four additional Forge cases exercise the Java entry, public reflection and staged world/coordinate setup followed
  by loading. The example compiles without Scala. GuideNH's old Scala argument does not match the new Java parameter,
  retaining its companion fallback; Schematica's old exact descriptor remains. The companion method is deprecated,
  with its body and internal callers unchanged.
- The JVM facade inventory initially rejected the intentional new public method; updated its exact expected set,
  leaving the companion set unchanged. Archived validation excludes only that obsolete inventory assertion, running
  the remaining 567 JVM cases and all 261 cases from the byte-identical archived Forge mod. Full normal/clean suites
  pass with 568 JVM and 265 Forge tests, zero failures/errors/skips. An independent class/member comparison verifies
  all 444 old class APIs with precisely one added static entry and one deprecation. All 17 ScalaSignature payloads,
  3,760 existing method bodies and 116 generated dumps remain apart from expected metadata/build versions.
- Added the generation guide, Java/reflection examples and consumer adoption ledger entry, and linked the loading
  and factory guides. The `+719` scan retains all 386 member/type/reflection rows across 27 consumers. Checkouts remain
  reference-only and releases/adoption are pending. Client tile generation/worldless loading are tested; actual client
  microblock construction and preview rendering remain manual. Next bounded task: GuideNH's existing Java microblock
  creation entry and its documented contract. Evidence: `run/migration-composite-generation-reference/`.

### 2026-09-05 - Existing Java microblock creation contract

- Traced GuideNH's exact companion selection and shape-only promotion. The static Java creation method already
  delegates to that implementation, so no API addition, deprecation or production behavior change was needed.
  Added Javadocs for creation and borrowed `IGeneratedMaterial` trait-set ownership, plus a consumer guide/example.
- Committed two Forge baseline cases first as `5d572ef`, with saved reference artifacts. Exact static/companion
  reflection returns fresh unbound parts with the stock factory/material identity; shape/NBT loading is caller-owned.
  Material callback exceptions propagate, leaving scratch changes until the next creation clears the reused set.
  Existing generated-material coverage retains the external Scala trait path and its generated behavior.
- Added two example cases: all 256 encoded shape bytes survive public `setShape` recreation without copying bindings,
  and factory/material/shape are captured before material callbacks, matching GuideNH's read order. A callback that
  changes source shape verifies the original value is copied. This is a core-data recreation example, not a clone of
  custom state. Public setter dispatch differs from GuideNH's private-field write; custom overrides remain an adoption check.
- The example compiles without Scala. Normal style/build checks and the final clean build pass; the final suite has
  568 JVM and 269 Forge tests, zero failures/errors/skips. All 568 archived JVM tests and 267 cases from the byte-identical
  archived Forge mod pass with no exclusions. All 444 class APIs, 17 ScalaSignature payloads, 3,761 method bodies and
  116 generated dumps remain apart from build versions. No production source inventory or Java 8 target change.
- Added the GuideNH adoption ledger row and corrected the historical companion-only wording: this consumer explicitly
  selects the companion even though the static replacement exists. Its source patch/release/adoption are pending.
  The `+719` scan retains all 386 member/type/reflection rows across 27 consumers; reference checkouts remain unchanged.
  Physical-client creation/previews remain manual because the dedicated server strips client factory methods.
  Next bounded task: GuideNH's private material access versus existing public accessors.
  Evidence: `run/migration-microblock-creation-reference/`.

### 2026-09-05 - Typed GuideNH material access

- Confirmed that existing `BlockMicroMaterial.block()` / `meta()` cover GuideNH's private-field read use case. Added
  Javadocs and a compiling complete material query using direct typed part traversal, material lookup and block
  registry access. No new facade, accessor aliases or production behavior changes were necessary.
- Committed the passing baseline first as `079739e`: two JVM cases pin constructor identity, raw integer metadata,
  virtual override dispatch and retained private-field values; one Forge case pins initialized block/material lookup.
- Two Forge example cases cover first usable part order, non-microblock/non-block material filtering, null/air and
  unregistered blocks, material aliases, all metadata bits, getter ordering/overrides and failure propagation. The
  query preserves GuideNH's positive-only metadata suffix policy; failures are handled at the consumer boundary.
- Public getters honor overrides whereas GuideNH's old accessor mixin reads constructor fields. Its reflective
  fallback already calls virtual getters. Documented that distinction and retained private fields until released
  consumer adoption, rather than adding raw-field accessors. Custom exports and optional-mod loading need consumer
  validation; reference checkouts were not modified or counted as migrated.
- Updated the plan and API guides: supported integrations must have direct typed public access. Optional support
  uses gated compatibility classes. Reflection snippets are legacy interoperability options, not the migration target.
- Validation: clean formatting/checkstyle/build and Java 8 Forge run pass with **570 JVM / 272 Forge tests**, zero
  failures/errors/skips. All **570 archived JVM / 270 archived Forge** tests pass; the frozen Forge mod is byte-identical.
  The typed example compiles with Scala excluded and has no Scala/reflection calls. All **444 class APIs, 17
  ScalaSignature payloads and 3,761 production method bodies** are unchanged. Of 116 generated dumps, 114 match
  byte-for-byte; the face/post pair differs only by swapped allocation-order names (`Microblock_cmp$$0` / `$$1`)
  because the added example tests create the face earlier. Explicit pairwise renaming proves identical bodies;
  no generator behavior changed. The installed
  `+719` pack retains all 386 ABI/reflection rows across 27 consumers. Packaged source and Java 8 bytecode are checked,
  and the post-commit rebuild verifies all five mod versions in dev/release artifacts. Evidence:
  `run/migration-material-access-reference/`.
- Next bounded task: Phase 9.2 internal-boundary Javadocs with a fresh caller audit, including supported `bindPart`
  and `internalPartChange`; external Java extension guidance and the measured performance pass remain separate.

### 2026-09-05 - Document the internal API boundary

- Rechecked Phase 9.2 across 28 source checkouts and active Extra Utilities compatibility. Found no external calls to the
  15 listed implementation hooks. Reviewed name collisions: WR-CBE calls its own renderer's `loadIcons`; GuideNH
  explicitly avoids `from` / `copyFrom` in comments. OpenComputers still calls `bindPart` at `PrintPart.scala:171`,
  and ProjectRed calls `internalPartChange` at `gatepartrs.scala:74`. Recorded source revisions and searches locally.
- Committed the passing baseline first as `8b12dfe`: one JVM test pins local callback order under list replacement,
  detachment/rebinding and failure, with no world required; one Forge test pins cache-only slot binding, no list
  insertion or part rebinding, and the requirement to clear old slot entries after a slot-mask change.
- Added internal Javadocs to all 15 hooks and the five registry companion bridges. Corrected the blanket prohibition
  on externally calling `bindPart`, and documented both supported advanced methods and their caller responsibilities.
  Repeated binding is not a universal cache refresh; local notifications exclude equal parts, retain legacy `operate`
  dispatch and do not send packets or external-world notifications. Marked only the registry-wide texture dispatcher
  internal; material `IMicroMaterial.loadIcons` remains a supported extension callback. Lifecycle override behavior,
  including generated `copyFrom`, remains unchanged. No new methods, deprecations, annotations or visibility changes.
- Updated the API index, cross-cutting consumer map and plan's Phase 9.2 checkboxes. The boundary list is bounded and
  does not imply every unlisted public member is supported. Complete external extension guidance remains open.
- Validation: normal and clean formatting/checkstyle/build plus the Java 8 Forge lane pass with **571 JVM / 273 Forge
  tests**, zero failures/errors/skips. The **571 archived JVM / 273 archived Forge** cases pass without recompilation;
  the archived Forge mod is byte-identical. All **444 class APIs, 17 ScalaSignature payloads, 3,761 production method
  bodies and 116 generated dump names/hashes** remain unchanged. The installed `+719` scan retains all 386 ABI/reflection
  rows across 27 consumers. Source-jar contents match the three edited production files; packaged classes remain
  Java 8, and the post-commit rebuild verifies all five mod versions in both dev/release jars. Evidence:
  `run/migration-api-boundary-reference/`.
- Next: a compiling representative Java microblock extension based on ProjectRed's illuminated material/trait,
  with generated Forge coverage. Reference consumers remain unmodified; consumer releases/adoption, physical-client
  rendering and the focused measured performance pass remain separate gates.

### 2026-09-05 - Clarify remaining work and active consumer support

- Standardized Extra Utilities references as an active supported consumer. UtilitiesInExcess remains the intended
  replacement; switching the compatibility target awaits approval and pack adoption, with the replacement's FMP
  contracts verified before removal of retained support. No compatibility gate or consumer adoption was changed.
- Added an overall remaining-work summary separating API/extension completion, downstream migration/adoption,
  measured performance work, release validation, pre-merge cleanup and final Scala removal. Corrected stale Phase 6,
  Phase 9 and GuideNH status text and aligned Galacticraft's checklist with the direct-call policy.
- Documentation-only change: checked wording, local links and diff whitespace. No production sources or fixtures
  changed; the existing 571 JVM / 273 Forge checkpoint remains the runtime evidence. The post-commit build verifies
  clean artifact versions under the existing workflow.

### 2026-09-05 - Java illuminated microblock extension example

- Traced ProjectRed `e173952e96a4`'s illuminated material/trait through registration, metadata, light aggregation and
  client halo geometry. The existing public `MicroblockGenerator.registerTrait(String)` already selects Java input;
  no production implementation change or new API was needed. Added Javadocs for registration before class loading.
- Committed baseline `c41ed8d` first: one Forge test proves retained external Scala-trait composition and class reuse
  across face, hollow, corner, edge and post shapes. The baseline has 571 JVM / 274 Forge cases and 120 generated dumps.
- Added three compiling Java example files: an abstract top-level trait, block-backed generated material and ordinary
  helper with consumer-supplied configuration/halo rendering. The trait uses Object-parameter delegation to avoid the
  transformer's inherited-access constraints. Consumers call only public typed FMP APIs; no reflection or Scala
  compile dependency is needed. The common light behavior and SideOnly client callback remain separate.
- Four Forge tests cover initialization registration of all metadata 16–31, both addTraits side inputs, all five
  generated shape families, fresh instances/class caching, constructor material identity and saved state. Light
  tests cover rounding/capping, matching membership, detached siblings, config switching and unbound failure. Halo
  tests preserve collision-box ownership and the hollow opening's trimmed strips across all six orientations.
- A uniquely named headless compiler probe retains the actual Java client method by removing only its test-input
  side annotation, then verifies generated dispatch, pass filtering and queued coordinates/colour/bounds. It does
  not establish physical-client generation, side selection or GPU correctness. Those checks and consumer config/halo
  wiring remain explicit adoption gates; ProjectRed's checkout/released code were not changed.
- The test mod registers a distinct lamp and 16 known fixture materials. The default-content test retains an exact
  inventory assertion, including these additions, rather than ignoring unexpected entries. Archived consumers use
  a separate disposable world: the old test mod cannot load a world containing the new fixture block without a
  missing-mapping prompt. The original test-world configuration was restored after the archived run.
- Validation: normal and clean formatting/checkstyle/build plus Java 8 Forge pass with **571 JVM / 278 Forge tests**,
  zero failures/errors/skips. All **571 archived JVM / 274 archived Forge** cases pass, with a byte-identical archived
  Forge mod. Production remains **444 class APIs, 17 ScalaSignature payloads and 3,761 unchanged method bodies**;
  the 120 baseline dump bodies are retained after mapping only allocation-order microblock self-names, and ten
  extension/probe dumps are added. All other baseline dump hashes match exactly. The three examples compile
  with Scala excluded and have no Scala/reflection bytecode references. The `+719` pack retains all 386 member/type/
  reflection rows across 27 consumers. Packaged source and Java 8 bytecode are checked; the post-commit build verifies
  all five mod versions in dev/release jars. Evidence: `run/migration-illuminated-extension-reference/`.
- Updated the API index, plan, adoption ledger and manual checklist. Next bounded task: converter registration and
  lifecycle guidance; broader transformed-tile compilation guidance and remaining audited reflection replacements
  remain separate. The performance pass, consumer release/adoption and final Scala removal gates are unchanged.

### 2026-09-05 - Java block converter registration and lifecycle

- Reused existing `IPartConverter` / `registerConverter` / `convertBlock`; no API addition, descriptor change or
  production method-body change. Javadocs now describe block iterable ownership, ordered/duplicate registration,
  borrowed inputs, fresh candidates, failure propagation and server conversion hooks. Fixed the old Boolean/blockID
  description on `blockTypes`, whose actual result is `Iterable<Block>`.
- Committed the untouched-implementation baseline as `a56f90c`: two JVM tests cover captured block lists, duplicate
  entries and exception propagation; one Forge test covers direct unbound conversion, rejected and accepted probes,
  original tile ownership, fresh placement conversion, hook order and existing-tile short-circuiting.
- Added the [Java converter guide](../api/BLOCK_CONVERTERS.md) and `BlockConversionExample` in the functional-test
  source set. It registers a separate persistent factory, copies source metadata without mutation and preserves it
  through NBT/descriptions. One Forge example test covers registration, rejection, fresh candidates, both payload
  paths and normal installation. The test reuses the existing fixture lamp; no extra block/world mapping is added.
  Real geometry/gameplay, consumer inventory/network transfers and physical-client behavior are explicitly outside
  this state/registration sample.
- Confirmed consumer patterns in reference-only Chisel, ForgeRelocationFMP, OpenComputers and AE2 checkouts.
  Converter registration already has a usable static Java entry. Preserve eligibility, state-copy and cleanup rules,
  with factory migration separate. Extra Utilities remains supported; no consumer source/release/adoption changed.
- Validation: normal and clean formatting/checkstyle/build/Forge runs pass, with **573 JVM / 280 Forge** tests and
  zero failures/errors/skips. The **573 archived JVM / 279 archived Forge** callers pass without recompilation,
  using their recorded build version for embedded version assertions; the archived Forge jar is byte-identical.
  Initial frozen execution at the new version failed only the two expected literal-version comparisons; rerunning
  with the recorded version passes without exclusions or fixture edits.
- All **444** production class APIs, **17** ScalaSignature payloads, **3,761** method bodies and **130** generated
  dumps are unchanged. The example compiles with `javac --release 8` without Scala and has no Scala/reflection
  bytecode references. Production jars exclude it. Packaged classes remain Java 8; both jar variants' five `@Mod`
  annotations are checked against the final clean filename version after committing.
- Evidence: `run/migration-converter-reference/`. Updated the API index, plan, audits, handoff and manual integration
  checks. Next: source-compilation guidance and safe public access for transformed tile traits. Remaining reflection
  capabilities, client validation, downstream releases/adoption and final Scala removal stay open.

### 2026-09-05 - Stable Java access to transformed tile traits

- Committed baseline `bfe5b86` before production documentation changes. Two Forge tests execute javac-compiled raw
  consumer calls: `TRedstoneTile.openConnections` fails with IncompatibleClassChangeError and `TSlottedTile.v_partMap`
  field access fails with NoSuchFieldError after runtime transformation. Stable IRedstoneTile and TileMultipart calls
  succeed on the same generated tiles. This characterizes a known source-compilation constraint, not a new runtime
  regression in existing interface-compiled consumers.
- Added the [safe access guide](../api/TILE_TRAIT_ACCESS.md) and a Java example using the existing IRedstoneTile
  interface. An additional Forge case covers all five-bit masks and null/non-redstone tiles. Corrected IRedstoneTile's
  old internal label to a supported capability contract and warned on both raw trait inputs. No new interface,
  reflection workaround, compile-stub artifact or production method body was needed.
- Confirmed ProjectRed's redwire query can change its cast owner to IRedstoneTile while retaining its existing mask
  and rotation calculation. Added that adoption-ledger row. OpenComputers PrintPart's slot mutation is explicitly
  separate: it clears array entries equal to itself, then dispatches bindPart before its own notifications. A focused
  slot-refresh API is the next bounded candidate; whole-tile reload and bindPart alone are not equivalent.
- Normal and clean formatting/checkstyle/build/Forge validation passes: **573 JVM / 283 Forge** cases, zero
  failures/errors/skips. **573 archived JVM / 282 archived Forge** callers pass without recompilation using their
  recorded version; the archived Forge test jar is byte-identical. The example compiles with Java 8 targeting and no
  Scala library against the dev artifact, and javap confirms invokeinterface through IRedstoneTile. Its bytecode has
  no Scala, reflection or raw trait-class references.
- All **444** production class APIs, **17** ScalaSignature payloads and **3,761** method bodies remain unchanged.
  Of **130** generated dumps, **128** are byte-identical; the TRedstoneTile/TSlottedTile helpers differ only by exact
  LINENUMBER offsets of +4/+5 from Javadoc additions. No executable generation difference or new divergence exists.
  Release/dev jars remain Java 8 and their five @Mod versions are checked after the final commit.
- Updated API index, plan, audits, handoff and physical-client adoption checks. Reference consumers were not edited,
  released or counted as adopted. Custom tile-trait authoring examples, slot mutation, remaining reflection APIs and
  physical-client/full-pack validation remain open. Evidence: `run/migration-tile-trait-access-reference/`.

### 2026-09-08 - Stable slot refresh for stored parts

- Traced OpenComputers PrintPart's state toggle at the recorded consumer revision. It changes its slot mask, clears
  every live slot entry equal to itself, calls the virtual `bindPart` chain once, then performs its own sound, part
  notification, description update and scheduling. Whole-tile reconstruction and `bindPart` alone are not equivalent.
- Committed baseline `a37e4ec` first. Its Forge characterization pins value equality, stale-slot clearing, unrelated
  slot retention, current-mask rebinding, ownership and stored-list identity on the untouched implementation.
- Added `TileMultipart.refreshPartSlots(TMultiPart)`. The base method is a no-op; the generated slotted capability
  clears equal entries in its live array and calls `bindPart` once. It does not validate, mutate the part, change
  storage/ownership or send notifications. This adds no interface, reflection path or general cache abstraction.
- Added a compiling Java example and two Forge regressions for generated dispatch and the non-slotted no-op. Updated
  the API index, transformed-tile guide, plan, ABI/adoption ledgers and manual OpenComputers check.
- Normal formatting/checkstyle/build/Forge validation passes with **573 JVM / 286 Forge** tests and zero failures,
  errors or skips. All **573 archived JVM** callers pass. The byte-identical archived Forge mod finds **284** cases:
  283 pass, and its exact generated-method inventory assertion reports only the intentional added method; the current
  inventory assertion includes that method and passes.
- The packaged inventory remains **444 classes** and **17 ScalaSignature payloads**. All **3,761 existing methods**
  are unchanged and exactly two methods are added. Of **130** generated dumps, 124 are byte-identical; the other six
  add only the transformed declaration/helper and four slotted-composite forwarders. The Java example targets Java 8
  without Scala/reflection/raw-trait references. Evidence: `run/migration-slot-refresh-reference/`.
- Consumer source/release/adoption remain pending. Next bounded task: a custom Java tile-trait authoring example with
  generated Forge coverage; remaining audited reflection replacements and physical-client checks stay separate.

### 2026-09-08 - Custom Java tile-trait authoring

- Committed baseline `d98e48f` before production documentation changes. Its Forge example registers a top-level Java
  trait by name during initialization and verifies server/client selection, stable capability dispatch, mixed-part
  aggregation, part binding, exact tile reuse and generated-class caching.
- Added the [custom tile-trait guide](../api/CUSTOM_TILE_TRAITS.md) and expanded both `registerTrait` Javadocs. The
  four-type pattern separates the requesting marker, consumer capability, transformed input and ordinary helper. It
  records class-loading order, side-specific registration, first-mapping behavior, state/lifecycle ownership and the
  current transformer constraints. The example uses a derived aggregate instead of adding a cache.
- No supplied active consumer directly registers a custom multipart tile trait. Current consumers use built-in traits,
  pass-through interfaces or ProjectRed's distinct microblock-trait path. Reference checkouts remain unchanged and no
  migration/release/adoption is claimed.
- Normal and clean formatting/checkstyle/build/Forge validation passes with **573 JVM / 287 Forge** cases, zero
  failures/errors/skips. All **573 archived JVM / 287 archived Forge** callers pass. The example targets Java 8 against
  the dev artifact without Scala or reflection and calls only the stable capability/base API.
- All **444** production classes, **17** ScalaSignature payloads and **3,763** packaged methods remain. All **134**
  generated dumps retain executable equivalence; only source-line metadata moves in the facade class. Evidence:
  `run/migration-custom-tile-trait-reference/`.
- Next bounded task: expose supported button-orientation customization for Et Futurum, then cover Iguana's saw-strength
  customization. Consumer adoption, physical-client/full-pack checks and measured performance remain separate gates.

### 2026-09-09 - Supported multipart button orientations

- Inspected Et Futurum Requiem at `78a5744dfd33`. Its initialization hook reflectively obtains the two public static
  `ButtonPart` direction arrays, writes metadata `0` to `UP` and metadata `5` to `DOWN` in both directions, then catches
  every exception. No other supplied consumer mutates these maps.
- Committed baseline `cd13ad6` before the production change. Its Forge test freezes the default arrays, the consumer's
  exact four writes, unavailable vertical placement before them and all six metadata-to-face placement results after.
- Added `ButtonPart.setOrientation(int, ForgeDirection)`. It validates orientation metadata and cardinal faces before
  mutation, updates both existing arrays in place and clears displaced inverse entries so remapping stays one-to-one.
  The arrays keep their exact public mutable static field shape for old consumer binaries and reflection.
- Added the [button orientation guide](../api/BUTTON_ORIENTATIONS.md) and compiling example. Et Futurum can replace its
  reflection with two direct common-initialization calls. Bounds still come from the transformed vanilla button block;
  consumer patch/release/pack adoption and physical-client all-face validation remain open.
- Normal and clean formatting/checkstyle/build/Forge validation passes with **573 JVM / 289 Forge** tests, zero
  failures/errors/skips. All **573 archived JVM / 287 archived Forge** callers pass. The example compiles on Java 8
  against the dev artifact with no Scala classpath or bytecode reference, and all 146 local documentation links resolve.
- The packaged inventory remains **444 classes** and **17 ScalaSignature payloads**. All **3,763 existing methods** are
  unchanged and exactly one public static method is added. All **134 generated dumps** match the baseline by name and
  SHA-256. Evidence: `run/migration-button-orientation-reference/`.
- Next bounded task: expose supported saw-strength customization for Iguana. Consumer adoption, physical-client/full-pack
  checks and measured performance remain separate gates.

### 2026-09-09 - Supported saw-strength mutation

- Inspected IguanaTweaksTConstruct `2.7.12` at `2bc09889d3e2`. Its post-init loop reads and writes the private
  `ItemSaw.harvestLevel` field with boxed reflection, then uses the changed value for cutting and tooltips. The
  supplied checkout remains reference-only.
- Committed baseline `098e747` before the production change. Its JVM test reproduces that exact field access and
  freezes updated getter, stack-specific cutting, maximum-strength and unchanged-durability behavior.
- Added `ItemSaw.setHarvestLevel(int)` and a compiling Java example. The setter updates the existing private field;
  its name, `int` type and visibility remain for old Iguana releases. The field loses `ACC_FINAL`, recorded as an
  intentional classfile divergence, so the public and legacy paths share one mutable value.
- FMP caches the process-wide maximum saw strength during ForgeMicroblock post-init. Iguana currently orders itself
  after `ForgeMultipart` but not relative to `ForgeMicroblock`, while it both relevels and adds saws in post-init.
  Its migration must also run before ForgeMicroblock's post-init, for example with optional `before:ForgeMicroblock`
  ordering, so weaker remapped saws cannot inherit a stale strongest-saw exemption.
- Normal formatting/checkstyle/build/Forge validation passes with **575 JVM / 289 Forge** tests, zero failures,
  errors or skips. The example targets Java 8 against the dev artifact without Scala or reflection, and all 150 local
  API documentation links resolve.
- The packaged inventory remains **444 classes** and **17 ScalaSignature payloads**. All **3,764 existing method
  bodies** are unchanged and exactly one public method is added; only the documented private-field modifier changes.
  All **134 generated dumps** match the baseline by name and SHA-256. Evidence:
  `run/migration-saw-strength-reference/`.
- The identified FMP-side reflection gaps now have typed replacements. Next work moves to consumer source patches,
  releases and target-pack adoption, alongside the outstanding physical-client and measured-performance gates.

## Phase 4 focused performance records (2026-08-27 / 2026-08-28)

Recorded results from the focused pre-optimization baseline, moved here when `docs/migration/README.md#phase-4b--measured-performance-pass` was folded
into the plan. Each result is scoped to its recorded workload and revision. The reusable protocol, harness commands
and workload description are in [the plan](README.md#phase-4b--measured-performance-pass).

## Baseline captured 2026-08-27

Environment: Zulu 8.96 / OpenJDK `1.8.0_504-b01`, Windows 11, 16 hardware threads, `-Xms1G -Xmx4G`, JFR `profile`
settings.

| Phase | Elapsed | Operations/s | Allocated bytes | Bytes/operation | CPU samples |
| --- | ---: | ---: | ---: | ---: | ---: |
| `updateEntity` | 2.970 s | 16,835,622 | 9,198,500,864 | 184.0 | 209 |
| `operate` | 3.004 s | 16,646,549 | 9,195,084,824 | 183.9 | 205 |
| `redstoneQueries` | 3.689 s | 13,553,242 | 4,023,003,032 | 80.5 | 239 |

Timing is machine- and JIT-sensitive; compare it only with the same workload and environment. Allocation per operation
is the more stable regression metric.

### CPU and allocation-site findings

- `updateEntity` and `operate` are dominated by `TileMultipart.parts()`, `AbstractCollection.toArray`, Scala list
  length/iteration, and Java-conversion wrappers. Their nearly identical 184-byte allocation cost shows that the
  traversal snapshot, not the update callback itself, is the first target.
- The current Java `parts()` constructs an `ArrayList` from the published Scala `Seq` on every traversal. The reference
  Scala `operate` captured the immutable `Seq` and iterated it directly, so this cost is a port artifact rather than a
  compatibility requirement.
- Redstone CPU samples are concentrated in Scala `List.foreach` (118/239), `PartMap.edgeBetween` (46/239), iterator
  `foreach` (35/239), and the generated strong-power closure (31/239). Allocation events identify
  `scala.runtime.IntRef`, Scala iterators, and generated closures as the major sites.

## Decision

The first Phase 4 implementation targeted `TileMultipart` traversal after focused tests froze its mutation semantics:
iteration observes the captured part order, skips a part whose tile was cleared before its turn, and does not visit a
part added during the callback. The implementation retains the public Scala `Seq` and `operate(Function1)` ABI.

## Traversal result captured 2026-08-27

`operate` now captures `partList` directly. The normal immutable Scala `List` path walks its existing head/tail chain,
which creates no iterator, Java wrapper, array, or copied collection. The published setter still accepts any Scala
`Seq`; unusual implementations use the reference-style iterator fallback rather than being forced into the fast-path
representation.

The retained post-change report is from the same machine, JVM, arguments, warm-up, and 50,000,000-iteration workload:

| Phase | Baseline elapsed | Result elapsed | Baseline B/op | Result B/op | Throughput change |
| --- | ---: | ---: | ---: | ---: | ---: |
| `updateEntity` | 2.970 s | 0.682 s | 184.0 | 0.05 | 4.36x |
| `operate` | 3.004 s | 0.700 s | 183.9 | 0.0 | 4.29x |
| `redstoneQueries` | 3.689 s | 7.286 s | 80.5 | 80.4 | control only |

A repeat produced 0.732 s / 0.687 s for `updateEntity` / `operate`, with the same 0.05 / 0.0 B/op. The result therefore
removes effectively all measured traversal allocation and raises throughput from roughly 16.7 million to 68–73
million calls/s. The post-change JFR hot-method view no longer contains `TileMultipart.parts()`,
`AbstractCollection.toArray`, or the Java-conversion wrappers in these paths.

The unchanged redstone allocation is the useful control. Its timing was consistently slower in both post-change runs,
but its code did not change and it now starts several seconds earlier because the preceding phases finish faster; do
not attribute that timing difference to this traversal change. Re-baseline the redstone unit immediately before its
own implementation comparison.

## Redstone helper-unit comparison captured 2026-08-28

Immediately before converting `IRedstonePart.scala`, the same workload measured `redstoneQueries` at 7.603 s,
6,575,956 iterations/s, 4,022,151,064 allocated bytes, and 80.4 B/iteration. After its six interfaces and
`RedstoneInteractions` were converted together, the retained report measured 3.880 s, 12,884,996 iterations/s,
4,023,003,032 allocated bytes, and 80.5 B/iteration. Both runs produced checksum `3315999992`.

The allocation result is unchanged. The elapsed-time difference is not treated as a port win: earlier unchanged
Scala runs ranged from 3.689 to 7.603 s on this machine. More importantly, the post-port JFR has the same dominant
sites: Scala `List.foreach`, `Iterator.foreach`, `PartMap.edgeBetween`, and
`TRedstoneTile$$anonfun$strongPowerLevel$1`.

That evidence corrects the earlier plan. `IRedstonePart.scala` owned the public interfaces and routing helpers, but the
measured `IntRef`, iterator, and closure allocations are emitted by `scalatraits/TRedstoneTile.scala`. Removing them
requires the Phase 5 `registerJavaTrait` path and must not be smuggled into this otherwise descriptor-identical port.

`MicroRecipe.scala` was the next independent Phase 4 unit and is now Java. Its five recipe forms and precedence are
characterized, and ordinary loops replaced its range/closure scans and exception-backed non-local returns. The focused
server workload does not craft recipes, so no timing claim is made for that structural removal.

The generated-trait checkpoints are complete. `TPartialOcclusionTile` proved the no-field path; `TSlottedTile` proved
field/accessor generation, initialization, copying, lifecycle behavior, and caching. Its ordinary loops remove four
Scala range closures and the exception-backed slot-scan return structurally, but the focused workload has no slotted
placement phase, so no numeric performance claim is made for that port.

## TRedstoneTile result captured 2026-08-28

The port was measured immediately before and after with the same JVM, eight-part generated tile, warm-up, and
50,000,000-iteration workload. Both runs produced checksum `3315999992`.

| Implementation | Elapsed | Operations/s | Allocated bytes | Bytes/operation |
| --- | ---: | ---: | ---: | ---: |
| Scala trait | 7.534 s | 6,636,424 | 4,023,855,000 | 80.5 |
| Java trait | 6.261 s | 7,986,213 | 0 | 0.0 |

The Java trait removes all measured allocation from the three-query iteration and improves throughput by 20.3% in
this paired run. The checksum and all characterization tests are unchanged. Normal immutable Scala `List` part
storage is traversed through its existing head/tail chain; the published `partList` setter still accepts any `Seq`,
so non-`List` implementations retain an iterator fallback.

The existing Java-trait transformer cannot safely rewrite bytecode that directly reads inherited Minecraft fields or
calls inherited `TileMultipart` methods. A package-private `TRedstoneTileAccess` shim keeps coordinate, `partList`, and
virtual `partMap` access outside the transformed class. This changes no public facade or generated-trait member and
required no generator change.

The two focused steady-state allocation targets identified by this workload are now resolved: `TileMultipart.operate`
and generated redstone queries are effectively allocation-free on their normal immutable-list paths. Further
optimization should follow a new representative profile rather than extending this synthetic workload speculatively.

## Multipart read-path baseline captured 2026-08-28

A consumer-audit sanity check identified two Java-port allocations that the original three-phase workload did not
exercise. Focused tests now pin empty/non-empty `BlockMultipart.getTile`, direct ordered `Seq` indexing, and read
queries over the mutable `Seq` implementations accepted by the public setter. The two matching profile phases measured:

| Phase | Elapsed | Operations/s | Allocated bytes | Bytes/operation |
| --- | ---: | ---: | ---: | ---: |
| `lightValue` | 3.170 s | 15,772,691 | 9,196,067,864 | 183.9 |
| `getTile` | 0.271 s | 184,225,439 | 1,200,000,000 | 24.0 |

`lightValue` pays for `TileMultipart.parts()`'s copied `ArrayList`; `getTile` pays for the Java list wrapper returned by
`jPartList()`. Both are absent from the reference Scala implementation, which reads the published `Seq` directly.
Mutation paths still require a snapshot before publishing a replacement immutable `Seq`.

### Read-path result captured 2026-08-28

The paired run used the same JVM, eight-part tiles, warm-up, iteration count, and checksum:

| Phase | Baseline elapsed | Result elapsed | Baseline B/op | Result B/op | Throughput change |
| --- | ---: | ---: | ---: | ---: | ---: |
| `lightValue` | 3.170 s | 0.278 s | 183.9 | 0.0 | 11.42x |
| `getTile` | 0.271 s | 0.094 s | 24.0 | 0.0 | 2.89x |

Internal read paths now traverse or index the published Scala `Seq` directly. The normal immutable-list light query
walks the existing head/tail chain; arbitrary `Seq` implementations retain an iterator fallback. The public
`jPartList()` bridge remains unchanged for downstream ABI compatibility, and only add/remove paths take mutable
snapshots before publishing a replacement immutable `Seq`.

## JVM Downgrader integration records (2026-09-08 / 2026-09-09)

Per-batch verification records from the scoped modern-Java integration, moved here when `docs/migration/README.md#modern-java-readability-policy`
was folded into the plan. The build arrangement, eligibility rules, fastutil decision and limits are in
[the plan](README.md#modern-java-readability-policy).

### Per-batch results

The completed `JavaTraitRegistration` batch passes a clean build with all 576 JVM tests and the Java 8 Forge run with
all 289 functional tests. All 450 dev-jar classes remain version 52, the jar has no JVM Downgrader runtime API
references, and all 134 generated ASM dump names and hashes match the pre-change manifest. The helper is absent from
joint output and present only in the raw/downgraded modern directories. Its own class bytes changed as expected under
modern javac/JVM Downgrader, but no source signature or consumer-facing class is changed.

The completed `ClassInfoLookup` batch has the same clean-build, 576-test, Java 8 Forge, 450-class/version-52 and
134-dump results. Its source signatures and package-private visibility are unchanged. JVM Downgrader records nest
metadata as annotations for this helper's anonymous callbacks, but the packaged classes contain no executable JVM
Downgrader API reference and the Java 8 runtime needs no added dependency.

The completed `ScalaSignatureParser` batch also passes the clean compiler boundary, all 576 JVM tests, all 289 Java 8
Forge tests and the 134-dump comparison; all 450 packaged classes remain version 52. Its non-private ABI is unchanged
and it has no executable JVM Downgrader API reference. Modern string concatenation in the same internal class lowers
to six private helper methods; these are compiler implementation details, not consumer entry points.

The follow-up `StackAnalyserLogic` batch converts constant classification to a pattern switch expression and removes
fall-through syntax from its opcode switches. It adds no build exclusion and preserves the same 576 JVM tests, 289
Java 8 Forge tests and all 134 generated ASM hashes.

The `HollowMicroblockTraitLogic` and `PostMicroblockClientLogic` batch passes a clean build, all 576 JVM tests, all 289
Java 8 Forge tests and the 134-dump comparison. Their non-private ABI is unchanged, all 450 packaged classes remain
version 52, and neither helper has an executable JVM Downgrader API reference. `PostMicroblockTraitLogic` was rejected
from the batch: the retained Scala declaration does not expose `getShape()` through `PostMicroblock`, so its explicit
`Microblock` cast is required and modern pattern syntax provides no useful replacement.

The `HollowMicroblockClientLogic` slot-renderer batch has the same clean-build, 576-test, 289-test, version-52 and
134-dump results. Its non-private ABI and callback ordering are unchanged, and its packaged classes have no executable
JVM Downgrader API reference.

The `TMicroOcclusionLogic` batch also passes the clean build, all 576 JVM tests, all 289 Java 8 Forge tests, and the
134-dump comparison. Its package-private method descriptors are unchanged, all 450 packaged classes remain version 52,
and the helper has no executable JVM Downgrader API reference.

### Original integration checkpoint evidence

The actual production patch passes normal and clean builds with 398 freshly compiled JVM tests, 398 frozen JVM
consumer tests, and 237 Java 8 Forge tests, with zero failures/errors/skips. Forge's nested build includes both new
tasks. Spotless and checkstyle pass.

All 445 dev-jar classes remain version 52. With matching version metadata, only `StackAnalyserLogic.class` changes;
all retained Scala classes, ScalaSignature payloads, bridges, models, and other classes are byte-for-byte identical.
The helper itself exactly matches the isolated prototype. All 116 generated ASM dump names and hashes match.
The helper has no JVM Downgrader runtime-stub references, and no runtime dependency was added.
The release jar also contains 445 Java 8 classes, and the sources jar contains the exact modern helper source.

Evidence and runnable checks are under ignored `run/jvmdg-trial/`:

- `production-candidate.log`, `production-clean.log`: actual build/Forge verification.
- `final-normal-build.log`: ordinary build and toolchain inventory without the frozen-version override.
- `integrated-comparison.json`, `verify-integration.ps1`: bytecode, packaging, dump, and test-count checks.
- `frozen-consumers.gradle`: tests using the original compiled JVM consumers.
- `src/`, `reference/`, `artifacts/`: original prototype, frozen baseline, and experiment jars.
- `artifacts/integrated/`, `integrated-test-results/`, `integrated-forge-test-results/`: preserved clean-build evidence.
- `production-integration.patch`: the production changes captured for review.
- `initial-root-handoff.md`: the original handoff before this takeover.


At checkpoint `5f0e329b`, the installed GTNHGradle 2.0.24 build classloader used JVM Downgrader engine/plugin **1.3.5**.
The earlier **1.3.6** number identifies the API dependency configured by global mode, not the engine observed in that
build. The integrated helper's exact match with the original prototype confirmed that its transformation was preserved.
