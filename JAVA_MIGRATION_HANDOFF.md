# Java migration — working handoff

Start here when picking the migration up in a fresh session. This file holds the operational knowledge: how to work,
what breaks, and what is left. The other documents hold the reasoning.

| Document | What it is |
| --- | --- |
| `JAVA_MIGRATION.md` | The plan, phase state, and a running findings log |
| `JAVA_MIGRATION_ABI_INVENTORY.md` | Which downstream mods use what. **The authority on whether anything is load-bearing** |
| `JAVA_MIGRATION_CONSUMER_AUDIT.md` | How every consumer uses FMP at runtime: data, lifecycle, reflection, and generated tiles |
| `JAVA_MIGRATION_DIVERGENCES.md` | Every intentional difference from the reference, one entry per port |
| `JAVA_MIGRATION_MANUAL_CHECKS.md` | What no automated test can cover, and must be checked by hand in a client |
| `JAVA_MIGRATION_PROFILE.md` | The focused baseline, first measured result, findings, and exact rerun command |

Branch: `algent/java`. Base: `master`. 142 commits including the API-cleanup plan and separate characterization and
port commits through the Edge/Post microblock factories.

## The one rule that matters

**Check both consumer audits before changing a compatibility surface.** The ABI inventory is the authority on what
must link; the source-level audit is the authority on behavior, serialized data, reflection, and mixin field access.

## Workflow per type

Followed for every port so far. It has caught three real ABI breaks that inspection missed.

1. **Characterize first, commit separately.** Write tests against the untouched Scala and confirm they pass. Commit as
   `test: characterize X` before touching the implementation.
2. **Freeze a binary consumer** if the inventory shows a load-bearing `$class` or singleton. Recipe below.
3. **Port.** Delete the `.scala`, add the `.java`.
4. **Verify.** Same tests must pass unchanged, plus the ABI diff below.
5. **Document** in `JAVA_MIGRATION_DIVERGENCES.md`, then commit as `refactor: port X to Java`.

If a characterization test will not compile after the port, that is a signal, not an inconvenience. Twice it was a real
ABI change (`MissingMicroMaterial` forwarders, the `ACC_SYNTHETIC` super accessor). Investigate before adjusting the
test, and if you do adjust it, re-run it against the stashed Scala to prove the baseline still holds.

## Commands

```bash
./gradlew spotlessApply checkstyleTest build
```

```bash
./gradlew test
```

```bash
./gradlew runFunctionalTestServer
```

Focused CPU/allocation profile from PowerShell:

```powershell
.\gradlew.bat runFunctionalTestServer "-Pforgemultipart.profileFunctionalTests=true"
```

See `JAVA_MIGRATION_PROFILE.md` before comparing its ignored `.jfr` and text outputs.

Test result counts (the Gradle output does not print them):

```bash
awk -F'"' '/<testsuite /{t+=$4;f+=$8;e+=$10} END{print "tests="t" failures="f" errors="e}' build/test-results/test/TEST-*.xml
```

```bash
grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' run/server/junit-out/TEST-*.xml
```

Current baseline: **250 plain-JVM tests and 108 Java 8 Forge dedicated-server tests passing.** The ignored local server
EULA is accepted in this checkout. GitHub Actions runs the same self-validating Forge suite in a dependent job after
the shared GTNH build; keep both jobs required.

### ABI diff against the reference

Build a reference jar at the pre-port commit, then diff. This is the check that matters most.

```bash
git stash push -u -- src/main && ./gradlew build -x test -q && cp "$(ls -t build/libs/*dev.jar | head -1)" /tmp/ref.jar && git stash pop && ./gradlew build -x test -q
```

Then, for each converted type, compare public members by **name** and by **descriptor** separately. Descriptor counts
alone are ambiguous, and `javap` renders varargs and generics in ways that look like losses but are not:

```bash
diff <(javap -p -cp /tmp/ref.jar codechicken.multipart.X | grep "  public" | sort) <(javap -p -cp "$NEW" codechicken.multipart.X | grep "  public" | sort)
```

```bash
diff <(javap -p -s -cp /tmp/ref.jar codechicken.multipart.X | grep descriptor | sort) <(javap -p -s -cp "$NEW" codechicken.multipart.X | grep descriptor | sort)
```

Also diff the emitted class list, to catch removed `$$anonfun$` artifacts that belong in the divergence log:

```bash
diff <(unzip -Z1 /tmp/ref.jar | grep X | sort) <(unzip -Z1 "$NEW" | grep X | sort)
```

### Re-running the downstream inventory

```bash
java tools/AbiScan.java "<instance>/.minecraft/mods" ForgeMultipart
```

Needs JDK 17+. The frozen baseline is `src/test/fixtures/abi/gtnh-daily-678-consumers.txt`, from GTNH daily
`2026-08-14+678` (241 jars, 27 consumers). Diff against it; anything present there but absent from the port is a
linkage break in a shipping mod.

### Building a frozen Scala consumer fixture

Scala 2.11.5 must run under **Java 8** — it cannot find `java.lang.Object` on a modern JDK.

1. Build a reference dev jar at the pre-port commit.
2. Get the compile classpath once, via an init script registering a task that prints
   `sourceSets.main.compileClasspath.asPath`.
3. Compile with `scala.tools.nsc.Main`, `-target:jvm-1.8`, classpath = reference jar + that classpath.
4. `base64 -w 76` the class into `src/test/resources/compat/`, and record both SHA-256s in
   `src/test/fixtures/README.md`.

Nine fixtures exist already; copy the pattern from any `*BinaryCompatibilityTest`. They decode and `defineClass` the
frozen bytes, so recompiling against the port would defeat the point.

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
has no scope spanning a package and its siblings. `PacketScheduler.sendScheduled` hit this; `MultipartGenerator` and
`MicroblockGenerator` will too.

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

## What is done

All eight load-bearing `$class` helpers from the inventory, both registries, and the two central types:

`IDWriter`, `PartialOcclusionTest`/`JPartialOcclusion`, `TCuboidPart`/`JCuboidPart`, the `TNormalOcclusion` unit,
`TFacePart`, the `TIconHitEffects` unit, `TItemMultiPart`/`JItemMultiPart`, `TEdgePart`, `Saw`, `MicroMaterialRegistry`,
`MultiPartRegistry`, `TileMultipart`, `TMultiPart`, `TickScheduler`, `BlockMultipart`, the complete
`IRedstonePart`/`RedstoneInteractions` unit, `MicroRecipe`, and the `TPartialOcclusionTile`, `TSlottedTile`, and
`TRedstoneTile`, `TTileChangeTile`, `TFluidHandlerTile`, `TIInventoryTile`/`JInventoryTile`, `TileMultipartClient`, and
`TRandomDisplayTickTile` Java-trait ports, plus `MultipartCompatiblity`/`MCPCCompatModule`, `MultipartMod`,
`MultipartEventHandler`, `MicroblockMod`, `MicroblockEventHandler`, and the complete `MicroblockPH`/
`MicroblockCPH`/`MicroblockSPH` and `MultipartPH`/`MultipartCPH`/`MultipartSPH` packet-handler units, plus
`MultipartSaveLoad`, `MissingMicroMaterial`, `DefaultContent`, `GrassMicroMaterial`/`TopMicroMaterial`,
`ItemMicroPart` plus its renderer, `ItemSaw` plus its renderer, `MicroblockPlacement` plus its executable-placement and
property types, and `PlacementGrids`, plus `BlockMicroMaterial`, `MaterialRenderHelper`, `ConfigContent`, and
`AngelicaCompat`.
The complete `MultipartProxy` and `MicroblockProxy` server/client hierarchies and static facades are also Java.

Plus the six marker interfaces: `TSlottedPart`, `IRandomDisplayTick`, `INeighborTileChange`, `TRandomUpdateTick`,
`ISidedHollowConnect`, `IMicroMaterialRender`, plus `MultipartHelper`, `TileCache`, `PacketScheduler` and the `ControlKeyModifer` pair.

Both `package.scala` objects are gone, removed rather than ported. `MultipartRenderer` is done.

`TTileChangeTile` is the fourth generated tile written in Java, and the first with both mutable state and inherited
member access. Its runtime interface keeps the exact 13 methods, the flag lifecycle and coordinate filter are frozen by
seven Forge tests, and `INeighborTileChange` — the surface shipping jars actually link against — is byte-identical
across the port. It is also where the two shim constraints in the gotchas list were found.

`rayTraceAll`'s index production is now characterized, closing the gap the read-path cleanup left: the index written
into `ExtendedMOP.data` is what `reduceMOP` hands back to every click, activate, harvest and pick block.

181 Java files, 16 Scala files, ~3,001 Scala lines left (non-blank; that is the metric this figure has always used).

## What is left, and in what order

**The low-risk queue is empty.** Pick the next piece deliberately rather than off the top of a list.

**The Schematica registry blocker is repaired.** `MultiPartRegistry$` again has the exact private field
`codechicken$multipart$MultiPartRegistry$$typeMap` with a Scala mutable-map descriptor. It is a live wrapper over the
canonical Java map, and `MultiPartRegistryCharacterizationTest` reproduces Schematica's reflective lookup and proves
both views reach the same factory.

The source-only member guards are also complete: GuideNH's mixin fields, Et Futurum's button arrays, Iguana's saw
field, and Galacticraft's name-only registration lookup are pinned by `ConsumerReflectionCompatibilityTest`.

Tile list/order/slot behavior and the live move lifecycle are now frozen by focused plain-JVM and Forge tests. They
pin ordered `parts`/`id` NBT, published list views, slot rebinding, add/remove/replace behavior, and the relocation
sequence (`onWorldSeparate`, then `onMoved`/`onWorldJoin`) while preserving the original generated tile.

The same ordered torch/button pair now supplies a compact NBT fixture and an exact logical chunk-description fixture.
The latter pins FMP's framing and payload before CodeChickenLib applies deferred transport compression.

The immediate compatibility gate is complete. `ForgeEnvironmentSmokeTest` now freezes built-in and external Scala
trait generation, Java-trait rewriting/dispatch, generated-class reuse, and a server-only pass-through interface's
overloads, single-implementor rule, copy/rebind behavior, and removal cleanup.

The first profiled optimization is complete. Focused tests freeze `operate` behavior when callbacks add or detach
parts, and its normal immutable-`List` path now traverses the captured list directly without changing the public Scala
`Seq`/`Function1` ABI. The same profile fell from about 184 B/call to 0.05 B/call for `updateEntity` and 0 B/call for
`operate`, with roughly 4.3x higher throughput. Full results and rerun commands are in
`JAVA_MIGRATION_PROFILE.md`.

The complete `IRedstonePart.scala` unit is now Java. All six interfaces retain their exact inheritance and descriptors;
`RedstoneInteractions` retains every static forwarder, while `RedstoneInteractions$.MODULE$` remains the load-bearing
implementation singleton. Pure masks, vanilla special cases, routing precedence, real-world power, and generated
redstone-tile selection are characterized.

`TRedstoneTile` has now completed that Phase 5 path. ProjectRed's direct `openConnections` call and Extra Utilities'
one-argument `weakPowerLevel` call are covered, the runtime interface remains field-free with the exact same eight
methods, and the paired workload fell from 80.5 B to 0.0 B per three-query iteration with 20.3% higher throughput and
the same checksum. Its package-private access shim is required by current inherited-member transformer limitations.

The Java-port-only multipart read allocations are also resolved. Focused tests pin mutable-`Seq` reads and
`BlockMultipart.getTile`; internal tile/block/renderer/scheduler paths now use the published `Seq` directly while the
public `jPartList()` bridge remains. In the paired workload, `getLightValue` fell from 183.9 B to 0.0 B per call and
`BlockMultipart.getTile` from 24.0 B to 0.0 B per call. Mutable snapshots remain only where add/remove publishes a
replacement immutable `Seq`.

`MicroRecipe` is now Java. The complete 17-method static/companion surface, immutable Scala split map, exact material
and tag matching, all five recipe forms, class-specific gluing, saw position, and hollow-over-gluing precedence are
frozen. Its internal scans are ordinary loops, and only the published `getSaw` call still constructs its required
`scala.Tuple3`. The in-repo Scala registration now names `MicroRecipe$.MODULE$` explicitly.

`TPartialOcclusionTile` is now the first built-in generated tile implementation written directly in Java. Four
plain-JVM tests freeze its input class and behavior. The Forge harness proves that `registerJavaTrait` still rewrites it
to the exact three-method runtime interface, preserves override/super dispatch, and reuses the generated composite
class. It has no fields or lifecycle callbacks, so this is not evidence for stateful traits.

`TSlottedTile` is now Java and supplies that stateful evidence. The generated runtime interface still has the exact 13
methods, including public array getter/setter and five super accessors. Each tile receives a distinct 27-entry array;
copying shares the source array exactly as before; external array mutation plus `bindPart`, clear/removal behavior,
occupied-slot rejection, value equality, actual add/remove/move lifecycle, and generated-class caching are all green.

`TFluidHandlerTile` is now Java. Seven behavior tests freeze ordered tank binding, shared-list copying, removal and
clear behavior, flattened tank information, capability short-circuiting, decreasing fill copies, and both simulated
drain forms. Its generated runtime interface still extends exactly `IFluidHandler` and keeps the same 16 methods,
field initialization, setter rebinding, and class-cache behavior. No shipping consumer references its removed raw
`$class` or closure classes. A narrow access helper is required for `FluidStack.amount` because the transformer treats
all direct field reads inside a Java trait as trait-owned state.

`TIInventoryTile` and `JInventoryTile` are now Java without collapsing their distinct roles. AE2 still casts generated
tiles to the public 28-method `TIInventoryTile` interface and calls `rebuildSlotMap`; the registered `JInventoryTile`
still rewrites to a 36-method child interface carrying its private-state accessors and both super-bridge layers. Seven
behavior tests freeze inventory-list sharing, flattened routing, sided offsets and the direct rebuild call. The
generated composite retains its two private prefixed fields and per-tile initialization. Primitive `int[]` creation is
isolated in `JInventoryTileAccess` because the transformer cannot analyze `NEWARRAY`.

`TileMultipartClient` and `TRandomDisplayTickTile` complete the built-in Java-trait queue. Six focused behavior tests
freeze render-cache order, bounds, lazy initialization, dynamic short-circuiting, and display-tick dispatch. The Forge
shape guard pins the exact 16-method base interface, one-method child interface, inheritance, generated private fields,
and class caching. GuideNH only loads `TileMultipartClient` by name for `isInstanceOf`; no audited consumer references
either removed `$class` helper. The true client rendering and particle paths remain on the manual checklist.

`MultipartCompatiblity` and `MCPCCompatModule` are now Java. Their two static facades, two companion singletons and
mutable Scala `Function4` callback retain their exact public descriptors. Three plain-JVM tests freeze the raw shape,
default allow behavior and shared setter identity; two Forge tests freeze non-MCPC loading and the logged missing-hook
fallback. The optional successful MCPC hook still needs a real patched `World` implementation to exercise end to end.

`MultipartMod` is now Java while remaining a Scala-language FML mod. Both annotated class names, all ten lifecycle
methods and `MultipartMod$.MODULE$` retain their exact public descriptors and annotations. FML still uses the companion
as the mod instance, and `MultipartPH.channel()` still returns that same companion with its original descriptor. Two
plain-JVM and two Forge tests freeze the shape, identity, completed lifecycle and server-stop cleanup.

`MultipartEventHandler` is now Java. Both singleton class names, all twelve public event methods, every event priority,
and the client-only highlight boundary retain their exact shape. The proxy now names `MultipartEventHandler$.MODULE$`
explicitly, and that same companion remains registered on both event buses. Two plain-JVM and three Forge tests freeze
the ABI, annotations, bus identity, load/unload cleanup, chunk watches and END-phase tick dispatch.

`MicroblockMod` is now Java while remaining a Scala-language FML mod. Both annotated singleton class names, all ten
lifecycle/IMC methods, the mutable `angelicaCompat` accessors and `MODULE$` retain their exact public descriptors and
annotations. FML still uses the companion as its mod instance and completes the full microblock server lifecycle. Two
plain-JVM and one Forge test freeze the shape, shared compatibility hook, companion identity and initialized registry.

`MicroblockEventHandler` is now Java. Both singleton class names, all four public event methods, both event annotations
and both client-only boundaries retain their exact shape. The proxy registers `MicroblockEventHandler$.MODULE$`
explicitly. Two plain-JVM tests freeze the raw ABI; one Forge test proves the client methods are stripped on a
dedicated server while the companion still reaches event-bus registration.

The ForgeMicroblock packet-handler unit is now Java. `MicroblockPH` keeps its channel accessor; both static facades,
both companion singletons and their three CodeChickenLib packet interfaces retain their exact public descriptors.
Seven plain-JVM tests freeze the raw ABI, integrated-server skip, ordered disconnect, unknown-type failure and no-op
server callback. The Forge handshake test freezes the exact channel, type and material-ID payload and passes on the
Java 8 dedicated server.

`MultipartSaveLoad` is now Java. Its static facade, load-bearing companion singleton, private state fields and literal
`MultipartSaveLoad$TileNBTContainer` binary name retain their exact public shape. A frozen Scala consumer exercises
ProjectRed's `MODULE$`/`loadingWorld` linkage, and dedicated Forge tests cover reflective vanilla map registration,
converter precedence/deletion and saved multipart reconstruction. Only the compiler-generated `$$anonfun$1`
class disappeared; the downstream inventory contains no reference to it.

`MissingMicroMaterial` is now Java. Its static facade and load-bearing companion retain all 12 public methods, exact
descriptors, `MODULE$`, `IMicroMaterial`, and the three client-only boundaries. Three plain-JVM tests freeze its raw
shape and inert values; one Forge test proves that `DefaultContent` registers the same singleton after client methods
and state are stripped on a dedicated server. The missing-texture render path remains client-manual.

`DefaultContent` is now Java. Its sole static/companion method and both emitted singleton types retain their exact
shape. One plain-JVM test freezes that ABI; two Forge tests freeze all five microblock factories, common IDs, the 103
sorted built-in materials and every legacy remap. The historical `log2`/`leaves2` meta-0-only overload behavior is
explicitly preserved rather than repaired during the port.

`GrassMicroMaterial` and `TopMicroMaterial` are now Java. The grass overlay getter used by UtilitiesInExcess, both
constructors, the top-material default-argument facade and its otherwise synthetic companion retain their exact public
descriptors. Five plain-JVM tests freeze horizontal/side UV routing, grass base/overlay colour routing, construction
and raw ABI; one Forge test freezes registration and the methods that remain after dedicated-server stripping. Actual
icons, tint and face output remain client-manual.

`MultipartProxy` is now Java. The server/client inheritance, three mutable fields and Scala-style accessors, static
facade, `MODULE$` companion and both index conversions retain their exact callable public shape. ProjectRed,
BuildCraftCompat and Iguana Tweaks keep their load-bearing block/config access. Forge strips the two client overrides
and matching static forwarders on a dedicated server; common generated-tile registration therefore calls the
companion directly and resolves to the inherited server implementation. Client renderer, packet, key-binding and
generated-tile renderer registration remain on the manual checklist.

`MicroblockProxy` is now Java. Its four load-bearing types retain all item/saw/logger/config access used by Iguana
Tweaks and in-repo recipes/rendering, including the protected-source/public-bytecode Scala `MutableList` accessors.
The client implementation preserves the exact lazy `RenderBlocks` field/bitmap shape and the reference's unusual
field-only side annotation; Forge strips the two lifecycle overrides and facade forwarders while companion dispatch
falls back to the server implementation. Actual item-renderer registration, client packets and Angelica integration
remain on the manual checklist.

The ForgeMultipart packet-handler unit is now Java. Its shared base, both static facades, both companion singletons,
nested byte stream, `MultipartMod$` channel descriptor and all three mangled state accessors retain their exact public
or reflective shapes. Eleven plain-JVM tests freeze the ABI, ordered registry disconnect, desync disconnect, control
packet, coordinate stream and both update terminators; existing Forge tests retain unload cleanup, deferred watcher
promotion and exact chunk-description framing. Direct loops replace fourteen unreferenced Scala closure/anonymous
classes. Real client description/update application remains on the manual checklist.

`ItemMicroPart` and `ItemMicroPartRenderer` are now Java. The item facade, load-bearing `ItemMicroPart$.MODULE$`,
renderer facade and registered renderer companion retain every callable public descriptor. A frozen Scala 2.11.5
consumer executes the four companion calls used by ProjectRed. Item creation, NBT/material lookup, invalid placement
and render short circuits are frozen headless; the existing Forge recipe and proxy tests cover initialized material
IDs and item registration. The Java renderer routes its transformed `MicroblockClient` operation through
`MicroblockRender.renderItem`, centralizing that load-bearing boundary. The later renderer port's clean-build bytecode
gate proves both calls remain `invokeinterface`. Creative listing, localized names, actual placement,
sound/consumption and client rendering remain on the manual checklist.

`MicroblockPlacement` is now six Java types with the exact retained hierarchy, fields, constructors, companion and
callable descriptors. Three plain-JVM tests freeze that ABI and the property/consumption defaults. Four Forge tests
freeze external placement, internal/opposite-slot selection, expansion in place, custom-placement precedence and
survival consumption using generated face microblocks. The renderer's sole source call now names
`MicroblockPlacement$.MODULE$` explicitly. Full item-use sound and client highlight/control-key feedback remain on the
manual checklist.

`PlacementGrids` is now Java while retaining the trait interface and `$class` binary bridge, `FaceEdgeGrid`, three
static facades and three `MODULE$` companions. Five plain-JVM cases freeze all public surfaces and every face,
corner and edge selection boundary for all six hit sides. ProjectBlue's load-bearing `FacePlacementGrid` static calls
remain unchanged. The reusable trait behavior is now three Java defaults; the concrete grids still declare the same
methods, and old Scala forwarders can still call the bridge. OpenGL guide rendering remains on the manual checklist.

`BlockMicroMaterial` and its render helper are now five Java types. The load-bearing `BlockMicroMaterial$.MODULE$`,
the static facades used by Java consumers, the `(Block, int)` constructor, and the exact private `block`/`meta` fields
targeted by GuideNH all remain. Five plain-JVM cases freeze the complete public/field/annotation shape, material
delegation, thread-local render state and inventory pipeline; a frozen Scala consumer executes both companions. One
Forge case freezes registered-block behavior and the dedicated-server side boundary. Three unreferenced Scala
closure/anonymous classes disappear. Actual icons, face output and Angelica shader material overrides remain on the
manual checklist.

`ConfigContent` is now a Java facade/companion pair. Six plain-JVM cases freeze its exact public and mutable-map
shape, config-file generation and parsing, aliases and metadata ranges, malformed-line recovery, block registration
and IMC validation. The two retained classes keep every callable descriptor; seven unreferenced Scala closure classes
disappear. The existing Forge lifecycle test still covers the real pre-init/init material-registration sequence.

`AngelicaCompat` is now Java. Two plain-JVM cases freeze its non-final public shape, exact `Object`-returning method
descriptors, `CapturingTessellator` guard, Iris calls and caught-`ClassCastException` fallback. Both methods retain the
reference's `BoxedUnit.UNIT` normal result and `Unit$.MODULE$` fallback result; both jars contain the same sole runtime
class. The optional live Angelica render path remains on the manual checklist.

`ItemSaw` and `ItemSawRenderer` are now Java. Four plain-JVM cases freeze all three runtime types, Iguana Tweaks'
reflective private-final `harvestLevel`, default/explicit durability, container behavior, renderer gating and the four
supported render types. Every callable descriptor and runtime class is retained. Actual model and OpenGL output remain
on the manual checklist.

`MicroblockRender` is now a Java facade/companion pair. Four plain-JVM cases freeze the exact callable surface,
thread-local `BlockFace`, live cuboid face-mask traversal, no-placement highlight exit and the generated-trait call
boundary. A clean compile proves Java retains `invokevirtual Microblock.setShape` plus `invokeinterface`
`MicroblockClient.getBounds/render`; the direct port therefore needs no Scala bridge. The two supported runtime types
remain, while three private Scala anonymous/closure artifacts disappear. Actual OpenGL item/highlight output remains
on the manual checklist.

`MicroblockClass`, `CommonMicroClass` and `CommonMicroClass$` are now Java. Three plain-JVM cases freeze their exact
hierarchy, public/private shape, side annotations, common-class registry behavior and generator call descriptors. The
Forge suite exercises eager base-trait registration, lazy client-trait avoidance on a dedicated server, all five
built-in factories and generated parts. The three runtime class names and all callable public descriptors match the
reference, including the GuideNH-pinned `MicroblockGenerator$.create(MicroblockClass, int, boolean)` boundary.

The `Microblock` abstract base and `Microblock$` are now Java. Three plain-JVM cases freeze all eight retained base,
companion, trait and `$class` surfaces plus state, signed shape packing, material behavior, item conversion, core NBT
and packet/update bytes. `MicroblockClient`, `CommonMicroblock` and `CommonMicroblockClient` deliberately remain Scala
in `MicroblockTraits.scala`: this preserves their multiple-inheritance ABI and ProjectRed's existing Scala-trait
generator path without an ASM change. The Forge suite still generates built-in parts and the external Scala fixture.

The face factory and placement singleton are now four Java facade/companion types. Two plain-JVM cases freeze all
eight retained surfaces and placement behavior; one Forge case freezes the 256-entry table's exact 42 populated
bounds, factory identity and generated `FaceMicroblock`/`TFacePart` behavior. `FaceMicroblock` and
`FaceMicroblockClient` deliberately remain Scala in `FaceMicroblockTraits.scala`, preserving their exact generated
trait and `$class` surfaces. Existing binaries retain the same `aBounds(): Cuboid6[]` call; recompiled Scala source
must use `FaceMicroClass.aBounds()(index)`. Only the two private bounds-initializer closures disappear.

The corner factory and placement singleton are now four Java facade/companion types. Two plain-JVM cases freeze all
six retained surfaces and all placement mappings; one Forge case freezes the 256-entry table's exact 56 populated
bounds, factory identity and generated shape/slot behavior. `CornerMicroblock` deliberately remains Scala in
`CornerMicroblockTraits.scala`, preserving its exact generated trait and `$class` surfaces. ProjectRed's load-bearing
`CornerMicroClass$.MODULE$.getClassId()` call remains exact. Existing binaries retain the same bounds getter; only the
two private bounds-initializer closures disappear.

The Edge/Post source unit is now six Java facade/companion types plus three retained Scala traits. Two plain-JVM cases
freeze all twelve public surfaces, including Post client state and super accessors. Three Forge cases freeze the 84
edge bounds, 12 post bounds, generated behavior, even-size centre placement and matching-post expansion. ProjectRed's
`EdgeMicroClass$.MODULE$.getClassId()` and UtilitiesInExcess's static `EdgeMicroClass.getClassId()` calls remain exact.
The stateful `PostMicroblockClient` traversal closure remains an exact private-surface match; only four private bounds
initializer closures disappear.

**High.** `HollowMicroblock.scala` is next. Characterize its placement/factory surfaces, bounds and all hollow-size/
side geometry plus the large stateful client-render trait before splitting only the concrete facade/companion code.
Keep both generated traits Scala and do not combine this with `TMicroOcclusion` or generator work.

**Phase 5 is complete.** There are no Scala files left in `multipart/scalatraits/`. The client pair required the first
narrow generator relaxation: Java-trait parent linearization, explicit field-accessor recognition, and exclusion of
transient runtime caches from generated copying.

**Phase 6/7, last.** `Microblock` and the microblock shape hierarchy, `MicroblockGenerator`, `MultipartGenerator`, and
all of `multipart/asm/`. The ASM subsystem should be last; freeze generated-class fixtures before touching it.

**Pre-merge cleanup is explicit in `JAVA_MIGRATION.md`.** It covers relocating eligible Java files out of
`src/main/scala`, reconsidering forced Scala compilation, refreshing the README, organizing the durable migration
documents and completing the manual checklist. Do not let those tasks disappear merely because the source port is
complete.

## Known gaps

- The full manual checklist has not been completed. Limited smoke testing has been performed in the GTNH full pack, but individual checklist items remain unrecorded and unverified.
  Every port since `TCuboidPart` has deferred something to it, and `BlockMultipart` leans on it hardest: breaking, selection boxes, collision, pick block, activation, particles and
  light are covered by nothing automated. Run it in a real client before this branch goes near a release.
- The focused synthetic baseline exists, but a representative full-GTNH-pack CPU/allocation capture is still manual
  work and is required before release claims about real TPS.
- The server side of flag-sensitive pass-through registration is automated; client-side exclusion still belongs to
  the packaged-client/manual compatibility run.
- Shipping consumers compiled against the old `TSlottedTile`, `TRedstoneTile`, `TIInventoryTile`,
  `TileMultipartClient`, and `TRandomDisplayTickTile` interfaces remain binary-compatible
  because Forge still exposes those exact interfaces at runtime. A consumer recompiled against the untransformed dev
  jar instead sees concrete Java mixin inputs and can emit class/field opcodes that are invalid after Forge rewrites
  them to interfaces. Provide a transformed compile stub or downstream source guidance before claiming
  source-compatible rebuilds.
- Core microblock `shape`/`material` NBT plus description and one-byte shape updates are frozen. Characterize any
  shape-specific state or packet behavior immediately before changing its subclass. The registry handshake packet's
  channel, type, count and ordered material names are also frozen.
- `TileMultipart` still republishes an immutable Scala `Seq` on every mutation. Its internal read paths now avoid Java
  list copies and wrappers; the remaining mutable snapshots at add/remove sites are intentional.
- The focused synthetic redstone allocation is resolved (80.5 B to 0.0 B per three-query iteration). A representative
  full-pack profile remains the authority for choosing any further optimization target.
- Phase 1 never properly started. GTNHLib is not declared because no migration change currently needs it; choose a
  pack-aligned version only when one does.
