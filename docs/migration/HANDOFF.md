# Java migration - working handoff

Start here. Work in this migration checkout on **`algent/java`**, based on `master`; inspect
branch, status and recent commits before editing and preserve existing work. The detached review worktree is not the
migration checkout; `codex/tile-compatibility-fixes` was deleted after its fixes reached `algent/java`.

| Document | Purpose |
| --- | --- |
| [Plan](README.md) | Phase gates, API/runtime/modern-Java policy, build arrangement and the performance protocol |
| [Compatibility](COMPATIBILITY.md) | Binary ABI inventory plus the source-level consumer audit and adoption ledger |
| [Divergences](DIVERGENCES.md) | Intentional effective compatibility differences |
| [Manual checks](MANUAL_CHECKS.md) | Client/release checks with concrete item examples |
| [Java API index](../API.md) | Consumer entry points, guides and compiling examples |
| [Release notes](../RELEASE_NOTES.md) | What breaks for consumers who rebuild, and the supported replacements |
| [History](HISTORY.md) | Dated completed-port findings and reference evidence; read as needed |

## Current state and next target

**577 plain-JVM tests and 290 Java 8 Forge tests pass, with zero failures/errors/skips.** Sources total **230 Java
files and 9 Scala files / 747 nonblank Scala lines**. The packaged inventory has 451 classes.

The agreed milestone order is the documented consumer-facing Java API, then consumer release and adoption, then final
Scala removal; see the plan's API migration design and Phases 8-10. Per-change fixes, their regression cases and their
recorded ABI evidence are in [the history](HISTORY.md); do not restate them here.

Direct typed calls are the intended end state for supported integrations. Optional dependencies should isolate typed
compatibility code behind presence/version checks; reflection snippets in earlier guides are temporary legacy
interoperability options, not a reason to leave consumers depending on reflection. Add purposeful missing capabilities
where needed, without exposing unrelated internals. Keep existing private fields/bridges until release and adoption.

Naming correction: the planned `loadParts(Collection)` overload made javac require `scala.collection.Iterable` even
for Java arguments. The distinct `loadPartList` name allows compilation without Scala on the example's classpath.
The old `registerParts` family also required `scala.collection.Seq`/`scala.Function2` for Java array/factory arguments;
`registerPartFactory` compiles without Scala. Keep this gate for remaining APIs; import/bytecode checks alone are insufficient.
Internal storage writes and reconstruction still dispatch through the old setter/loader hooks, not the Java siblings.

The [API index](../API.md) orients consumers to entry points, compiling examples and migration status. Material
enumeration and tile read/traversal/loading/storage APIs are also complete. No supplied consumer directly calls the
two-argument tile occlusion hook; existing `canAddPart`/`canReplacePart` calls need no rename. GuideNH and Schematica
have Java loading replacements, but consumer source changes, other reflection/extension contracts, releases and pack
adoption remain pending. Checkouts remain reference-only. The installed `+719` pack scan retains the `+678` floor:
27 consumers, 35 inherited types, 255 members, 76 other types and 20 strings.

**Next priority: move the completed FMP-side Java replacements into Phase 10 consumer patches and releases.** Reuse
the documented Java surface and preserve each consumer's lifecycle, state and failure policy. ProjectRed's illuminated
microblocks remain the representative external extension case. Consumer mods may remain Scala internally while
adopting this API. The next bounded source migration can replace Iguana's `ItemSaw.harvestLevel` reflection with the
supported setter and order that work before ForgeMicroblock post-init; its supplied checkout remains reference-only.
Converter registration/lifecycle and stable tile capability access guidance are complete.
The illuminated microblock example supplies the representative Java extension, but physical-client construction,
connector-dependent halos, lighting and consumer adoption still need their recorded checks.

Progress: all ten Phase 9.1 table rows and Phase 9.2's internal markers are complete, plus Schematica registry lookup,
staged Java tile generation, microblock creation, typed material access, the Java illuminated extension example, slot
refresh, custom Java tile-trait authoring, button orientation mapping and saw-strength mutation. The identified
FMP-side reflection gaps now have typed replacements; consumer patches, releases and pack adoption still precede
final Scala removal and client/pack release validation.

Extra Utilities remains an active supported consumer. UtilitiesInExcess is the intended replacement, but the support
switch awaits approval and actual target-pack adoption. Retain existing contracts until those gates pass. The
[overall remaining-work summary](README.md#remaining-work-overall) separates FMP work from downstream gates.

A separate [Phase 4b performance pass](README.md#phase-4b--measured-performance-pass) is planned once the API
and representative extension workloads are stable, alongside consumer migration. Use fresh realistic profiles and
repeated paired runs; distinguish FMP implementation gains from migrated-consumer gains, covering hot paths plus
startup, transitions, rendering, network and memory costs, following the measurement protocol recorded there. The
historical Phase 4 results do not replace that pass, and no new performance gain has been measured for this update.

Pause mechanical extraction of retained Scala shells unless it enables that API, fixes a demonstrated issue or has
a measured benefit. `ScalaSignature.ClassSymbolRef.info` remains an optional bounded extraction, not the default next
task. Keep case-class/product/serialization shapes and simple model accessors supported until their users are retired.
The external ProjectRed Scala-trait fixture and ScalaSignature model bridges remain required. Actual client
generation, GPU output and full-pack checks remain manual.

Prefer modern syntax where it improves readability and the compilation boundary supports it; it need not wait for
Scala removal. Defer a specific change if Scala parsing, compile order, ABI or downgrader/runtime support blocks it,
recording the blocker and revisit condition. Keep the global modern-syntax setting disabled while it breaks Scala
compilation. The build arrangement, eligibility rules and expansion constraints are in the plan's
[modern Java readability policy](README.md#modern-java-readability-policy).

The initial Java API can operate over retained Scala storage and compatibility shells. Record legacy FMP uses,
replacements, consumer releases, target-pack adoption and verification in the consumer audit. Source patches alone
do not permit removal. Once the adoption gates pass, replace or retire FMP's remaining internal Scala users before
dropping its compiler/runtime dependency. Other mods' own Scala requirements are outside this removal target.

Remaining Scala units:

| Files under `src/main/scala/codechicken` | Why they remain |
| --- | --- |
| `multipart/asm/ASMMixinCompiler.scala` | Retained nested models, construction callbacks and Scala entry-point shell |
| `multipart/asm/ScalaSignature.scala` | Named models, primitive/erased bridges and five generic inner-construction branches |
| `multipart/asm/StackAnalyser.scala` | Class/companion/models over Java `StackAnalyserLogic`; state, handler callback, bridges and model queries remain Scala |
| `microblock/MicroblockTraits.scala`, `FaceMicroblockTraits.scala`, `CornerMicroblockTraits.scala`, `EdgeMicroblockTraits.scala` | Nine retained state/inheritance/bridge shells over Java implementation, including post lifecycle/super dispatch |
| `microblock/HollowMicroblockTraits.scala` | Hollow server/client inheritance, initializer and super shells over Java behavior |
| `microblock/TMicroOcclusion.scala` | Occlusion/client inheritance, state/accessor and lifecycle/super shells over Java behavior |

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
Archived test mods predating the illuminated fixture lamp must use a separate disposable world; reusing the current
test world triggers Forge's missing-mapping prompt. Restore server.properties after such archived runs.

Generated dumps: `run/server/asm/multipart/**/*.txt`; enumerate recursively and assert a nonzero expected count.
Compare names and hashes first. Investigate differences before normalizing only proven debug/private-name changes.
The current baseline emits 134 dumps, including the deterministic definition, compiler and feature fixtures.

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

- **Microblock trait inheritance still needs Scala metadata.** Abstract Java mixins and Java `@SideOnly` filtering
  are supported, but that does not supply Scala multiple-trait linearization. `getAndRegisterParentTraits` collects
  Scala-signature traits, while Java registration only incorporates an already registered superclass trait, not
  implemented Scala interfaces. `CommonMicroblockClient` combines three trait parents, including the still-Scala
  `TMicroOcclusionClient`. Keep its declarations and bridges while extracting behavior; replacing them outright needs
  a separately characterized compiler/inheritance strategy. Extra Utilities, ForgeRelocationFMP and GuideNH also use
  these runtime interface types.
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
For retained Scala traits, keep the accessor and its call in Scala instead. `PostMicroblockTraitLogic.occlusionResult`
returns 0/1 for a completed decision or -1 for the Scala bridge to invoke the original super chain; no callback
allocation, eager super evaluation or new public accessor is needed.

**Recompiled Scala consumers.** These are source-only breaks; binaries are fine. Expect and document them:
`x.partList(i)` becomes `x.partList.apply(i)`; assignment to a Java accessor needs `_$eq`; a Scala `object` becoming a
Java class loses `apply` sugar and must be referenced as `X$.MODULE$` when passed as a value; `Tuple2` members typed
`Boolean` in Scala arrive as `Object` and need casting; and Scala 2.11 will not adapt a lambda to
`Function1[_, BoxedUnit]`, so `operate { p => ... }` needs an explicit `AbstractFunction1`. A Java static array getter
also loses Scala property/indexing composition: the converted Face, Corner, Edge, and Post bounds facades need an
extra empty argument list before the index when recompiling, while existing bytecode remains compatible.

**A companion object can be load-bearing without a single bytecode reference.** The inventory's 17 `MODULE$` list is
not the whole test - check the reflective string constants too. `MultipartHelper$` is in neither the `MODULE$` list nor
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
proves the other branch did reach it. Use `LinkageError`, not the exact type - the first attempt raises
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

**Preserve inferred Scala return types.** Adding `: String` to `FieldMixin.accessName` changes its ScalaSignature
payload despite preserving the JVM descriptor. Let the Java delegate's return type remain inferred and compare the
metadata as well as callable signatures.

**Java 8 target.** Joint main sources and tests cannot use `List.of`, `var` or switch expressions. The scoped
`StackAnalyserLogic` stage supports verified Java 21 method bodies; its packaged output must still target Java 8.
These are current compilation constraints, not a preference for old syntax. Move additional source units onto the
scoped path only when compatible; otherwise retain the working syntax and record why modernization is deferred.

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

- The full [manual checklist](MANUAL_CHECKS.md) is still open. The user confirmed the ProjectRed
  placement crash is fixed (`36e1a58`), but that does not establish all rendering, particles, selection/collision,
  pick-block, activation or lighting behavior. The headless renderer tests cover compiled dispatch, not GPU output.
- Existing binaries retain the runtime tile interfaces. Recompilation against raw Java mixin inputs can emit invalid
  class/field opcodes after Forge transforms them. [Stable capability guidance](../api/TILE_TRAIT_ACCESS.md) covers
  redstone, ordinary base/interface calls and slot refresh; [custom authoring guidance](../api/CUSTOM_TILE_TRAITS.md)
  covers marker registration, stable capabilities and transformer constraints. The server pass-through fixture does
  not cover client exclusion.
- Run representative full-pack CPU/allocation and packaged modern-Java validation before performance/release claims.
  Focused results and reruns are in the profile. The current deobfuscated dedicated-server harness uses Java 8.
- Core NBT, descriptions and one-byte shape updates are frozen; characterize each remaining shape's state immediately
  before changing it. Keep material/type registration order and handshake formats intact.
- The optional successful MCPC compatibility hook still requires an actual patched `World` to exercise end to end.
- Keep the plan's pre-merge gates: source relocation where joint compilation permits it, version-guard review, README
  refresh, durable-document organization and recorded client checks. History is now under `docs/migration/`; relocating
  the other durable documents remains open. GTNHLib/UniMixins are added only when a concrete change needs them.
