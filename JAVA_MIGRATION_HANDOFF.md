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

Branch: `algent/java`. Base: `master`. 89 commits including separate characterization and port commits for
`MultipartMod`.

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

Current baseline: **158 plain-JVM tests, 81 Forge server tests, all passing at their last completed runs.**

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

Eight fixtures exist already; copy the pattern from any `*BinaryCompatibilityTest`. They decode and `defineClass` the
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
`Function1[_, BoxedUnit]`, so `operate { p => ... }` needs an explicit `AbstractFunction1`.

**A companion object can be load-bearing without a single bytecode reference.** The inventory's 17 `MODULE$` list is
not the whole test — check the reflective string constants too. `MultipartHelper$` is in neither the `MODULE$` list nor
any consumer's constant pool as a type, but guidenh names it as a string, so it was kept. `IconHitEffects$` was in
neither and was dropped. Check both lists before deleting a companion.

**Do not call a side-only Scala object's static facade from common Java.** `MultipartProxy.postInit()` exists in the
raw jar but inherits `@SideOnly(CLIENT)` from the client override, so Forge strips that forwarder on a dedicated server
and a Java call fails with `NoSuchMethodError`. Call `MultipartProxy$.MODULE$.postInit()` as the Scala reference did;
after side stripping, virtual resolution reaches the server superclass implementation.

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
`TRandomDisplayTickTile` Java-trait ports, plus `MultipartCompatiblity`/`MCPCCompatModule` and `MultipartMod`.

Plus the six marker interfaces: `TSlottedPart`, `IRandomDisplayTick`, `INeighborTileChange`, `TRandomUpdateTick`,
`ISidedHollowConnect`, `IMicroMaterialRender`, plus `MultipartHelper`, `TileCache`, `PacketScheduler` and the `ControlKeyModifer` pair.

Both `package.scala` objects are gone, removed rather than ported. `MultipartRenderer` is done.

`TTileChangeTile` is the fourth generated tile written in Java, and the first with both mutable state and inherited
member access. Its runtime interface keeps the exact 13 methods, the flag lifecycle and coordinate filter are frozen by
seven Forge tests, and `INeighborTileChange` — the surface shipping jars actually link against — is byte-identical
across the port. It is also where the two shim constraints in the gotchas list were found.

`rayTraceAll`'s index production is now characterized, closing the gap the read-path cleanup left: the index written
into `ExtendedMOP.data` is what `reduceMOP` hands back to every click, activate, harvest and pick block.

97 Java files, 36 Scala files, ~5,241 Scala lines left (non-blank; that is the metric this figure has always used).

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

**Medium.** The `handler` packages on both sides, `ItemMicroPart`,
`MicroblockPlacement`, `PlacementGrids`, `BlockMicroMaterial`, `MissingMicroMaterial`, `ConfigContent`,
`DefaultContent`.

**Phase 5 is complete.** There are no Scala files left in `multipart/scalatraits/`. The client pair required the first
narrow generator relaxation: Java-trait parent linearization, explicit field-accessor recognition, and exclusion of
transient runtime caches from generated copying.

Take `multipart/handler/MultipartEventHandler.scala` next. Preserve both singleton types and event-bus identity; freeze
every `@SubscribeEvent` priority plus the client-only highlight annotation before changing it. Exercise load/unload,
chunk watch/unwatch and end-of-server-tick dispatch where the Forge harness can observe them, and keep the
characterization and port commits separate.

**Phase 6/7, last.** `Microblock` and the microblock shape hierarchy, `MicroblockGenerator`, `MultipartGenerator`, and
all of `multipart/asm/`. The ASM subsystem should be last; freeze generated-class fixtures before touching it.

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
- Microblock-specific NBT and packet payloads still need characterization immediately before that subsystem changes;
  the compact core tile/part fixture is complete.
- `TileMultipart` still republishes an immutable Scala `Seq` on every mutation. Its internal read paths now avoid Java
  list copies and wrappers; the remaining mutable snapshots at add/remove sites are intentional.
- The focused synthetic redstone allocation is resolved (80.5 B to 0.0 B per three-query iteration). A representative
  full-pack profile remains the authority for choosing any further optimization target.
- Phase 1 never properly started. GTNHLib is not declared because no migration change currently needs it; choose a
  pack-aligned version only when one does.
