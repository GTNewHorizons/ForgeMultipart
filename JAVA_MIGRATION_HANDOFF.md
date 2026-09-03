# Java migration — working handoff

Start here. Work in `C:\Users\Algent\IdeaProjects\ForgeMultipart` on **`algent/java`**, based on `master`; inspect
branch, status and recent commits before editing and preserve existing work. The detached review worktree is not the
migration checkout; `codex/tile-compatibility-fixes` was deleted after its fixes reached `algent/java`.

| Document | Purpose |
| --- | --- |
| [Plan](JAVA_MIGRATION.md) | Phase gates, API policy and upstream cleanup |
| [ABI inventory](JAVA_MIGRATION_ABI_INVENTORY.md) | Shipping binary names/descriptors and reflective constraints |
| [Consumer audit](JAVA_MIGRATION_CONSUMER_AUDIT.md) | Runtime behavior, data/lifecycle contracts and consumer source |
| [Divergences](JAVA_MIGRATION_DIVERGENCES.md) | Intentional effective compatibility differences |
| [Manual checks](JAVA_MIGRATION_MANUAL_CHECKS.md) | Client/release checks with concrete item examples |
| [Profile](JAVA_MIGRATION_PROFILE.md) | Measured workloads, results and rerun commands |
| [History](docs/migration/HISTORY.md) | Dated completed-port findings and reference evidence; read as needed |

## Current state and next target

**333 plain-JVM tests and 211 Java 8 Forge tests pass, with zero failures/errors/skips.** Sources total **214 Java
files and 9 Scala files / 1,188 nonblank Scala lines**. The packaged inventory has 437 classes.

Latest bounded compiler change: Java mixins now filter opposite-side fields, methods and constructors before metadata
or bytecode rewriting. The Forge test first characterized the previous retention of visible/invisible annotations,
state and constructor initialization (`a481859`). Current-side members remain; when the no-argument constructor is absent,
an empty `$init$` satisfies the generated initializer contract without executing the opposite-side body. A regression
also passes input through Forge's real side transformer before registration. Clean verification after stopping Gradle
matched 432 class APIs, all five compiler closures and 3,643 non-target method bodies. All 112 reference dump names
remain; 109 hashes are exact and only the trait, helper and composite from the SideOnly fixture differ. Four outputs
are new for the already-stripped-constructor fixture. Local evidence is in
`run/migration-java-side-only-reference/`; `clean` preserves this ignored directory.

**Next: port `microblock/MicroblockTraits.scala` as the first generated microblock trait group.** Both Java-mixin
prerequisites are complete. Characterize its interface/helper ABI, initialization, generated common/client dispatch,
render/effect routing, slots, partial-occlusion boxes and item class ID before conversion. Keep the external ProjectRed
Scala-trait fixture green and retain the ScalaSignature path-dependent model bridges.

Remaining Scala units:

| Files under `src/main/scala/codechicken` | Why they remain |
| --- | --- |
| `multipart/asm/ASMMixinCompiler.scala` | Retained nested models, construction callbacks and Scala entry-point shell |
| `multipart/asm/ScalaSignature.scala` | Named models, primitive/erased bridges and five generic inner-construction branches |
| `multipart/asm/StackAnalyser.scala` | Class/companion/model shell over Java `StackAnalyserLogic` |
| `microblock/MicroblockTraits.scala`, `FaceMicroblockTraits.scala`, `CornerMicroblockTraits.scala`, `EdgeMicroblockTraits.scala`, `HollowMicroblockTraits.scala`, `TMicroOcclusion.scala` | Generated traits; port in dependency order now that both Java-mixin prerequisites are complete |

Both generators, both registries, core tile/part classes, ordinary microblock helpers/factories, handlers, networking,
placement/render helpers and built-in tile traits are Java. The low-risk queue and immediate consumer gate are
complete. Per-port evidence lives in the history; do not repeat it here as unfinished work.

## Required workflow

The user explicitly requested characterization and regression tests for **every** migration target. That authorization
persists. Six early ASM extractions lacked characterization; their backfill is complete. Do not repeat that gap.

1. Read both consumer audits before changing any compatibility surface.
2. Write meaningful tests on the untouched implementation. Use plain JVM tests for isolated behavior and Forge for
   initialization-dependent code. Run them, then commit separately as `test: characterize X`.
3. Save the reference dev jar, source, reports and relevant generated outputs under ignored `run/migration-X-reference/`
   before editing. Freeze compiled consumers where the audit shows a load-bearing bridge; do not recompile those
   fixtures against the port. No stashing or checkout switching is necessary when the baseline is saved first.
4. Port only the bounded unit. Preserve binary names, descriptors, modifiers, Scala-facing bridges, virtual dispatch,
   cache/state/serialization behavior and failure ordering. Keep bug fixes and compiler algorithm changes separate.
5. Run formatting/checkstyle/build and Forge, then compare APIs and generated output. Repeat from a clean build;
   stop the Gradle daemon first on Windows. Do not weaken tests to make the port pass.
6. Update the history and this handoff. Add a divergence only for a new effective difference; shared compiler artifacts
   already have one ledger entry. Commit separately as `refactor: port X to Java` on `algent/java`.
7. Rebuild after the final commit and verify all five `@Mod` versions in both dev/release jars match the clean version
   in their filenames. Keep `compileScala.scalaCompileOptions.force = true`: Zinc otherwise leaves stale Java
   annotation values when `Tags.VERSION` changes. Reconsider it only with the planned source-layout cleanup.

```powershell
.\gradlew.bat spotlessApply checkstyleTest build
.\gradlew.bat runFunctionalTestServer
.\gradlew.bat --stop
.\gradlew.bat clean spotlessApply checkstyleTest build
.\gradlew.bat runFunctionalTestServer
```

JUnit XML: `build/test-results/test/TEST-*.xml` and `run/server/junit-out/TEST-*.xml`. Count tests, failures, errors and
skips; do not infer them from Gradle's task summary. The Forge runner validates its own reports, and its dependent CI
job must stay required. The ignored local server EULA is already accepted.

Generated dumps: `run/server/asm/multipart/**/*.txt`; enumerate recursively and assert a nonzero expected count.
Compare names and hashes first. Investigate differences before normalizing only proven debug/private-name changes.
The current baseline emits 116 dumps, including the deterministic definition, compiler and feature fixtures.

### Binary and fixture checks

Compare member **names and descriptors together**, with modifiers, generic signatures, private reflective fields and
the emitted class inventory; descriptor counts alone miss changes. Inspect `javap -p -s` or ASM output. Existing
ignored reference directories contain comparison scripts; adapt their explicit target exclusions and expected counts.
Preserve model serialization and ScalaSignature bridges; routine unreferenced closure removal is covered in the ledger.

Re-scan a pack with `java tools/AbiScan.java "<instance>/.minecraft/mods" ForgeMultipart` using JDK 17+. Diff against
`src/test/fixtures/abi/gtnh-daily-678-consumers.txt` (27 consumers); the source audit adds the `+700` release provenance
and UtilitiesInExcess. The clone root is `E:\Development\GTNH\Projects\ForgeMultipart_Java_Port\fmp-consumers`.

For a new frozen Scala consumer, run Scala 2.11.5 under **Java 8**, using the reference jar plus the main compile
classpath and `scala.tools.nsc.Main -target:jvm-1.8`. Encode the compiled bytes under `src/test/resources/compat/`,
record source/class SHA-256s in `src/test/fixtures/README.md`, and follow an existing `*BinaryCompatibilityTest`.
Compilation against the migrated artifact would defeat the binary-compatibility check.

## Retained compiler constraints

- **External Scala traits remain supported.** ProjectRed registers `LightMicroblock` through
  `MicroblockGenerator.registerTrait`; keep its ScalaSignature ingestion, `$class` helper and generated dispatch.
  The Java path now supports its `@SideOnly` requirement, but retiring this external Scala path still needs a released
  ProjectRed Java rewrite. Do not flatten generated microblock inheritance to bypass that consumer transition.
- **ScalaSignature models cannot be mechanically replaced.** Primitive literal `value()` methods coexist with
  erased `Object value()` bridges; Java source cannot declare both. Five generic inner constructors also need their
  retained Scala construction branches and exact outer-instance bindings.
- **ClassInfo bridges remain Scala.** Javac expects an extra `$` in nested `ClassInfo$` model names; its interpretation
  of `IterableLike.view()`'s generic `Object` return emits a missing descriptor instead of `IterableView`. Preserve
  construction/access callbacks, the initial Scala `.view`, lazy collection builders and virtual dispatch. Keep
  path-dependent symbol types out of Java-callable signatures. Joint helpers resolve `ASMMixinCompiler$.MODULE$`
  internally because Scala cannot resolve a Java method parameter naming its own generated `$` companion class.
- **StackAnalyser's shell stays coordinated with its companion/models.** Scala 2.11.5 rejects a Java class beside
  its same-name Scala object. A full shell port needs a coordinated binary/model/serialization bridge strategy.
- **Characterized algorithm quirks stay separate from this port.** Primitive `NEWARRAY` throws `MatchError: 188`;
  wide argument counts can misindex `getSuper`; Scala `String` alias parameters decode as `Lscala/Predef/String;`.
  Successful external fixtures use `java.lang.String`; failing alias fixtures retain the original failure.
- **Cache and error quirks are intentional preservation.** Metadata keys distinguish dotted/slashed strings, while
  byte-cache publication normalizes and invalidates only that key. Parse failures keep cached bytes; null results
  are cached, load failures are retried, and dump failures follow publication. Ordinary `define` duplicates remain
  `InvocationTargetException`-wrapped; only direct LinkageErrors reach its case-sensitive duplicate handler, whose
  null message still throws. Do not unwrap or repair these during mechanical extraction.

## Gotchas, all discovered the hard way

**Default methods vs superclass.** Emit a Java `default` only when **no class in an implementor's superclass chain
declares the same member**. A superclass method always beats an interface default on the JVM, and the failure is
silent. `TFacePart`, `TEdgePart`, `Saw`, `JIconHitEffects.getBreakingIcon` and `IMicroMaterial`'s three members passed
this test. `TCuboidPart`, `TNormalOcclusion`, `TIconHitEffects`, `TItemMultiPart.onItemUse` and
`TRandomUpdateTick.onWorldJoin` failed it and had to stay abstract with explicit forwarders in implementors.

**Audit existing `super` call sites, not just missing overrides.** `PostMicroblock` already overrode `occlusionTest`
and ended with `super.occlusionTest(npart)`, which used to route through the trait. After the port it reached the
superclass directly and silently dropped the box test, while still compiling.

**A trait gaining a default changes its concrete implementors.** `ItemSaw` and `MissingMicroMaterial` stopped
declaring members they never overrode, because Scala no longer emits a forwarder. Diff those classes too.

**Scala trait extending a class is a bare interface in bytecode.** It carries none of the class's members, so Java
code must cast. Seen with `JIconHitEffects`, `TRandomUpdateTick`, `TileMultipartClient`. There will be more.

**Trait super accessors are `ACC_SYNTHETIC`.** `javac` cannot see or implement them. Where one is load-bearing, emit
it as a non-synthetic `default` returning the identity for the chain, as
`codechicken$multipart$TNormalOcclusion$$super$occlusionTest` does.

**Recompiled Scala consumers.** These are source-only breaks; binaries are fine. Expect and document them:
`x.partList(i)` becomes `x.partList.apply(i)`; assignment to a Java accessor needs `_$eq`; a Scala `object` becoming a
Java class loses `apply` sugar and must be referenced as `X$.MODULE$` when passed as a value; `Tuple2` members typed
`Boolean` in Scala arrive as `Object` and need casting; and Scala 2.11 will not adapt a lambda to
`Function1[_, BoxedUnit]`, so `operate { p => ... }` needs an explicit `AbstractFunction1`. A Java static array getter
also loses Scala property/indexing composition: the converted Face, Corner, Edge, and Post bounds facades need an
extra empty argument list before the index when recompiling, while existing bytecode remains compatible.

**A companion object can be load-bearing without a single bytecode reference.** The inventory's 17 `MODULE$` list is
not the whole test — check the reflective string constants too. `MultipartHelper$` is in neither the `MODULE$` list nor
any consumer's constant pool as a type, but guidenh names it as a string, so it was kept. `IconHitEffects$` was in
neither and was dropped. Check both lists before deleting a companion.

**Do not call a side-only Scala object's static facade from common Java.** `MultipartProxy.postInit()` exists in the
raw jar but inherits `@SideOnly(CLIENT)` from the client override, so Forge strips that forwarder on a dedicated server
and a Java call fails with `NoSuchMethodError`. Call `MultipartProxy$.MODULE$.postInit()` as the Scala reference did;
after side stripping, virtual resolution reaches the server superclass implementation.

**Generated version constants require full Zinc recompilation.** `Tags.VERSION` is a compile-time constant embedded in
the joint-compiled Java `@Mod` annotations. Gradle notices the generated classpath change, but Zinc does not invalidate
those Java consumers. `compileScala.scalaCompileOptions.force = true` keeps incremental jars current; do not remove the
version assertions or this build guard without replacing both with an equivalent mechanism.

**A Scala-to-Java source replacement needs one clean verification.** Zinc can retain the deleted Scala source's class
files while compiling the new Java file with the same binary name. An incremental test and jar can then exercise and
package the old implementation despite a green build. `GrassMicroMaterial` exposed this: only `clean` changed the
class `SourceFile` from Scala to Java. Stop the Gradle daemon first if Windows holds `build/rfg/recompiled_minecraft`
open, then run the focused test and ABI comparison from the clean output.

**Classes that cannot class-initialize headless make good probes.** `MultipartSaveLoad` reflects into `TileEntity`'s
static maps through `ObfMapping` and always throws under a plain JVM. That turns "did this branch reach the loader?"
into an assertion: returning normally proves the guard short-circuited, and `assertThrows(LinkageError.class, ...)`
proves the other branch did reach it. Use `LinkageError`, not the exact type — the first attempt raises
`ExceptionInInitializerError` and every later one raises `NoClassDefFoundError`, so test order would otherwise matter.

**Where a trait member cannot be a default, use an interface static.** `TScheduledPacketPart.read` is shadowed by
`TMultiPart.read`, so the dispatch that lived in the `$class` helper became
`TScheduledPacketPart.readMask(part, packet)`, with a one-line forwarder documented in the javadoc. Same shape as a
`$class` bridge, but idiomatic and callable from Java.

**`private[multipart]` becomes public, not package-private.** It reaches `codechicken.multipart.handler` too, and Java
has no scope spanning a package and its siblings. `PacketScheduler.sendScheduled` and the generator companions keep
those methods public; do not add static forwarders for originally companion-only methods.

**A Forge `FakePlayer` unlocks the server half of player-dependent code.**
`FakePlayerFactory.getMinecraft(worldServer)` needs no connection and its world is not remote, which is the branch a
server takes. It made `ControlKeyModifer.isControlDown` testable. The client branch of anything stays manual.

**A Scala package object cannot be ported.** It compiles to a class named `package`, and that is a Java keyword, so
no Java source can declare or name it. Both were removed by inlining their single member. Any future one has the same
three options: leave it in Scala, invent a Java type to hold its members, or inline it.

**Scala's uniform access hides field versus no-arg method.** `renderer.hasOverrideBlockTexture` reads the same in
Scala either way; it is a method on `RenderBlocks` and Java needs the parentheses. It fails at compile time rather than
silently, but expect it in every remaining renderer conversion.

**Java 8 target.** No `List.of`, no `var`, no switch expressions in main or test sources.

**Two test classes sharing global registry state** must guard their registrations, and the registries' error paths call
a logger that is null until `preInit`, so they cannot run headless at all.

**Java traits cannot directly touch inherited members with the current transformer.** An inherited field read is
mistaken for trait-owned state, and an inherited virtual call takes a broken cast-rewrite path. `TRedstoneTile` keeps
coordinate, `partList`, and virtual `partMap` access in package-private `TRedstoneTileAccess`; use that pattern or fix
and characterize the generator before converting another trait with the same bytecode shape.

**Java traits also cannot directly read fields owned by method arguments.** The transformer rewrites every `GETFIELD`
as trait-owned state without checking its owner. `TFluidHandlerTile` therefore keeps `FluidStack.amount` reads and
writes in package-private `TFluidHandlerTileAccess`. A static helper outside the transformed class is enough when no
trait-to-superclass cast is involved.

**Java traits cannot allocate primitive arrays.** The stack analyzer has no case for JVM opcode `NEWARRAY` and throws
`MatchError: 188` while registering the trait. `JInventoryTile` therefore converts its collected sided slots to
`int[]` in package-private `JInventoryTileAccess`. Reference arrays use `ANEWARRAY` and remain safe inside traits.

**Registered Java traits may now extend another registered Java trait.** Register the parent first. The transformer
adds the parent runtime interface and linearization entry, while parent validation resolves through it to the concrete
`TileMultipart` base. This path is characterized by `TRandomDisplayTickTile`; do not generalize it to arbitrary class
inheritance without another fixture.

**Mark runtime-only Java-trait fields transient when Scala did not copy them.** `autoCompleteJavaTrait` copies every
ordinary field through generated `copyFrom`, but now excludes transient fields. `TileMultipartClient` uses this for
render caches and its derived dynamic flag, preserving the Scala trait's absence of a `copyFrom` override.

**Do not invoke a registered mixin type directly from Java source.** The untransformed input is a class, so javac emits
class opcodes that become invalid when Forge rewrites it to an interface. In-repo client calls use additive no-op hooks
on `TileMultipart` so generated overrides dispatch through the stable superclass. Dedicated-server tests use targeted
method handles for trait-only methods; ordinary reflection enumerates the client-only `RenderBlocks` descriptor and
triggers the side transformer.

**Type the access shim's parameters `Object`.** `TTileChangeTileAccess` first typed them as the trait itself and cast
to `TileMultipart` inside. Because the untransformed trait extends `TileMultipart`, javac elided the cast, and once
Forge rewrote the trait to an interface the verifier rejected the `getfield`. `TRedstoneTileAccess` only escapes this
because `IRedstoneTile` is unrelated to `TileMultipart`, forcing a real `checkcast`. `Object` always works.

**A Java mixin trait may not carry an inner class.** `registerJavaTrait` throws on a non-empty `InnerClasses`
attribute. An anonymous `AbstractFunction1` for an `operate` callback, a lambda, or a string switch all trip it. Put
the callback in the access shim as a named class, as `TTileChangeTileAccess.NeighborTileChanged` does.

**Abstract Java mixins declare contracts without helper bodies.** Their abstract methods stay on the generated
interface so concrete mixin methods can dispatch through them, but do not enter `MixinInfo.methods` until a later
mixin implements them. Their source constructor must still be no-argument; it may directly call a superclass
constructor with arguments. Registration removes that call and its argument evaluation because the generated
composite has already invoked the real base constructor, then preserves the remaining field initialization.

## Release gaps and cleanup

- The full [manual checklist](JAVA_MIGRATION_MANUAL_CHECKS.md) is still open. The user confirmed the ProjectRed
  placement crash is fixed (`36e1a58`), but that does not establish all rendering, particles, selection/collision,
  pick-block, activation or lighting behavior. The headless renderer tests cover compiled dispatch, not GPU output.
- Existing binaries retain the runtime tile interfaces. Recompilation against raw Java mixin inputs can emit invalid
  class/field opcodes after Forge transforms them; provide transformed compile stubs or source guidance before
  claiming source-compatible rebuilds. The server pass-through fixture does not cover client exclusion.
- Run representative full-pack CPU/allocation and packaged modern-Java validation before performance/release claims.
  Focused results and reruns are in the profile. The current deobfuscated dedicated-server harness uses Java 8.
- Core NBT, descriptions and one-byte shape updates are frozen; characterize each remaining shape's state immediately
  before changing it. Keep material/type registration order and handshake formats intact.
- The optional successful MCPC compatibility hook still requires an actual patched `World` to exercise end to end.
- Keep the plan's pre-merge gates: source relocation where joint compilation permits it, version-guard review, README
  refresh, durable-document organization and recorded client checks. History is now under `docs/migration/`; relocating
  the other durable documents remains open. GTNHLib/UniMixins are added only when a concrete change needs them.
