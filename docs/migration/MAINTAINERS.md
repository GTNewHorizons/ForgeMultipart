# ForgeMultipart maintainer constraints

These constraints explain the retained build and runtime machinery. Consumer migration belongs in the
[adoption ledger](COMPATIBILITY.md#java-api-adoption-ledger); remaining release checks belong in
[MANUAL_CHECKS.md](MANUAL_CHECKS.md). Keep this guide about current mechanisms, not completed-port history.

## Build and runtime boundaries

- Preserve a Java 8 artifact with equivalent startup, gameplay, serialization and network behavior. The target
  modern-runtime policy spans Java 17–26; validate packaged releases on the selected supported runtime. Equal
  throughput across Java versions is not required. The deobfuscated Forge test runner uses Java 8; the current
  modern dedicated-server run tasks are not a substitute for packaged validation.
- Keep `enableModernJavaSyntax = false` while Scala 2.11.5 remains. Normal Java and joint Scala/Java compilation
  use Java 8. Helpers importing retained Scala models belong in the joint `src/main/scala` source set because
  `compileJava` runs before those Scala types exist.
- `scalaCompileOptions.force = true` prevents stale `Tags.VERSION` constants in joint-compiled Java `@Mod`
  annotations. Zinc otherwise misses those consumers when the generated constant changes. Verify all five mod
  annotations in dev/release artifacts against their artifact version; replace the guard only with equivalent coverage.
- Keep registered trait inputs and compatibility facades conservative. Moving `RedstoneInteractions$` or
  `BlockMultipart` pulls transformer-sensitive dependencies; `RenderPartResolver` is needed by joint compilation
  before Java helpers that use it. The explicit `Microblock` cast in `PostMicroblockTraitLogic` is required by the
  retained Scala declaration.
- GTNHLib is compile-only here. Its fastutil compile classpath does not grant a runtime dependency. Declare that
  dependency explicitly before using fastutil in ordinary code; require profiling evidence for such a change.

## Retained compiler constraints

- Scala multiple-trait inheritance still needs metadata. Java registration incorporates an already registered
  superclass trait, not implemented Scala interfaces. `CommonMicroblockClient` combines three parents, including
  `TMicroOcclusionClient`. Preserve state/accessor/super bridges until a characterized replacement exists.
- ProjectRed's external `LightMicroblock` requires ScalaSignature ingestion, `$class` helpers and generated
  dispatch until its replacement is released and adopted. Abstract Java mixins and side filtering do not remove
  that requirement, nor FMP's internal Scala trait inputs.
- ScalaSignature primitive literal `value()` methods coexist with erased `Object value()` bridges that Java
  source cannot declare together. Five generic inner-construction branches require exact outer-instance bindings.
  Preserve inferred Scala return types too: an explicit type can alter ScalaSignature without changing a JVM descriptor.
- Javac interprets nested `ClassInfo$` names and `IterableLike.view()` differently from scalac. Keep construction
  callbacks, the initial Scala `.view`, lazy builders and virtual dispatch; avoid path-dependent symbols or generated
  companion classes in Java-callable signatures. Joint helpers resolve `ASMMixinCompiler$.MODULE$` internally.
- Scala 2.11.5 rejects a Java class beside its same-name Scala object. Port `StackAnalyser`'s class, companion,
  model and serialization contracts together, rather than splitting their public shapes mechanically.
- Preserve characterized algorithm quirks during mechanical conversion: primitive `NEWARRAY` throws
  `MatchError: 188`, wide arguments can misindex `getSuper`, and Scala `String` aliases decode as
  `Lscala/Predef/String;`. Successful external fixtures use `java.lang.String`; fix these separately with regressions.
- Metadata keys distinguish dotted/slashed names; byte-cache publication normalizes and invalidates only that key.
  Parse failures retain bytes, null results are cached, load failures retry, and dump failures follow publication.
  Ordinary duplicate `define` errors remain reflection-wrapped; direct LinkageError handling is case-sensitive and
  its null-message behavior is characterized. Do not silently alter these during extraction.

## Trait and dispatch rules

- A superclass method wins over a Java interface default, silently bypassing former Scala trait behavior. Audit
  implementors and existing `super` call sites; `PostMicroblock.occlusionTest` is a concrete example. Recompiled
  implementors may also lose Scala-generated forwarders when a trait gains a default.
- A Scala trait extending a class is only an interface in bytecode. Synthetic super accessors are invisible to
  javac. Preserve required non-synthetic bridges; retained Scala super calls can stay in Scala using the existing
  result-code pattern instead of eager callbacks. `TScheduledPacketPart.readMask` is the static-helper pattern for
  a trait method shadowed by a superclass.
- `private[multipart]` reaches sibling packages, so Java package-private is not equivalent. Keep required public
  descriptors and companion-only methods. Check reflective string names as well as bytecode references before
  removing companions; `MultipartHelper$` is a reflective dependency.
- Common Java must not use a side-stripped static facade. `MultipartProxy$.MODULE$.postInit()` retains virtual
  server fallback; the corresponding static forwarder is stripped on dedicated servers.
- Registered Java trait classes become interfaces at runtime. Consumer calls must use stable base/capability
  types, not raw class/field opcodes. See [tile access](../api/TILE_TRAIT_ACCESS.md) and
  [custom trait authoring](../api/CUSTOM_TILE_TRAITS.md).
- Put inherited calls, inherited field access, argument-owned field access and primitive-array allocation in an
  ordinary helper outside the trait transformer. Use `Object` parameters when a trait-to-base cast must survive
  javac; a raw subclass parameter can cause javac to omit the required cast.
- Trait inputs cannot carry an `InnerClasses` attribute; anonymous classes, lambdas and string switches need
  transformer-specific scrutiny. Keep callbacks in named helper classes. Register parent Java traits first.
  Mark runtime-only fields `transient` when generated `copyFrom` must not copy them.
- Abstract mixin methods remain interface contracts until implemented by a later mixin. Source constructors must
  be no-argument; registration removes the superclass call and its argument evaluation but retains field initialization.
- Java API examples must compile against the packaged dev jar with Scala excluded. Merely removing imports does
  not prove this: overloaded `loadParts`/`registerParts` still required Scala during resolution, motivating distinct
  `loadPartList`/`registerPartFactory` names. Preserve legacy override dispatch underneath the Java adapters.

## Verification

For a source conversion, characterize the untouched implementation first and preserve the compiled reference
consumers. Compare names and descriptors together, modifiers, generic signatures, reflective private fields,
class inventory, serialized models, packets/NBT and generated interfaces. Keep behavior fixes separate from
mechanical ports. Coverage percentages and signature counts alone are insufficient.

```powershell
.\gradlew.bat spotlessApply checkstyleTest build
.\gradlew.bat runFunctionalTestServer
.\gradlew.bat --stop
.\gradlew.bat clean spotlessApply checkstyleTest build
.\gradlew.bat runFunctionalTestServer
```

A Scala-to-Java replacement needs clean verification: Zinc can retain the removed Scala class and package it
instead of exercising the new source. Stop the daemon if Windows holds build outputs open. Verify packaged
class version 52 and mod-version freshness, including after the release commit.

JUnit reports are under `build/test-results/test/` and `run/server/junit-out/`; inspect counts, failures, errors and
skips. The Forge task rejects missing/failed reports. Keep that CI gate required. Generated dumps are under
`run/server/asm/multipart/`; assert a nonzero expected inventory and investigate differences before normalizing
proven debug/private-name changes. Headless client-body probes do not validate actual client loading or GPU output.

Use Forge for world/registry initialization and FakePlayer cases. Error-path logging needs initialized registries;
shared global state must be restored between tests. Headless class-initialization failures can differ between the
first and subsequent calls, so probes use `LinkageError` rather than one exact subclass. Archived test mods lacking
current registered content need disposable worlds to avoid missing-mapping prompts; restore server configuration.

Run `java tools/AbiScan.java "<instance>/.minecraft/mods" ForgeMultipart` on JDK 17+ and compare full rows with
`src/test/fixtures/abi/gtnh-daily-678-consumers.txt`. Preserve audited source revisions separately from pack versions.
Compile frozen Scala fixtures with Scala 2.11.5 on Java 8 against the reference artifact using
`scala.tools.nsc.Main -target:jvm-1.8`, not against the replacement. Store bytes under `src/test/resources/compat/`
and provenance/hashes in `src/test/fixtures/README.md`.

## Performance validation

Profile typical, busy and explicitly labeled stress scenes: mixed ticking/redstone tiles, placement/occlusion,
chunk transitions and previews, frame moves, client microblocks/halos, multiplayer packets, startup and first-use
class generation. Historical focused results do not establish current whole-pack gains.

Record world/snapshot, pack and consumer/FMP revisions, hardware, JVM/flags/heap, graphics/view distance, part/trait
counts and repeatable actions. Restore equivalent state. Compare baseline versus changed FMP with consumers fixed,
then old versus migrated consumer calls on the same FMP artifact; include a compatible Scala reference if possible.
Keep runtime/configuration fixed and separate cold startup from warmed play, CPU-bound from GPU-bound scenes.

Start with five independent runs per variant, alternate order, match warm-up/measurement/profiling overhead, and
extend noisy runs. Report medians and variability, absolute units and relative differences; derive p95/p99 from
sufficient within-run samples. Include allocation, GC, retained/peak heap, cold latency and packet costs where relevant.
Confirm timing without profiling if overhead matters. A helper ratio is not an FPS/TPS claim.

Before editing, define the hypothesis, measured site and benefit above noise. Keep raw baselines and record commands,
commits, correctness checks, controls, regressions and acceptance decisions. Validate representative pack scenes after
consumer adoption. Below-noise results are valid; no fixed speedup target or speculative optimization is required.

The existing focused harness runs with an accepted local server EULA:

```powershell
.\gradlew.bat runFunctionalTestServer "-Pforgemultipart.profileFunctionalTests=true"
jfr summary run/server/forgemultipart-baseline.jfr
jfr view --width 220 hot-methods run/server/forgemultipart-baseline.jfr
jfr view thread-allocation run/server/forgemultipart-baseline.jfr
```

Profile mode overwrites ignored `run/server/forgemultipart-profile.txt` and `forgemultipart-baseline.jfr`.
The text's `startEpochMillis`/`nanos` identify phase windows; allocation counts use HotSpot per-thread counters.
Java 8 recordings expose TLAB events, not `ObjectAllocationSample`, so recent JDK `allocation-by-site` views are
not a valid summary. Consult `ForgeMultipartProfileWorkload` for current workload sizes and phases.
