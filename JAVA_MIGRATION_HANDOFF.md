# Java migration — working handoff

Start here when picking the migration up in a fresh session. This file holds the operational knowledge: how to work,
what breaks, and what is left. The other documents hold the reasoning.

| Document | What it is |
| --- | --- |
| `JAVA_MIGRATION.md` | The plan, phase state, and a running findings log |
| `JAVA_MIGRATION_ABI_INVENTORY.md` | Which downstream mods use what. **The authority on whether anything is load-bearing** |
| `JAVA_MIGRATION_DIVERGENCES.md` | Every intentional difference from the reference, one entry per port |
| `JAVA_MIGRATION_MANUAL_CHECKS.md` | What no automated test can cover, and must be checked by hand in a client |

Branch: `algent/java`. Base: `master`. 50 commits so far.

## The one rule that matters

**Check `JAVA_MIGRATION_ABI_INVENTORY.md` before writing a compatibility bridge.** Several were written speculatively
early on and later deleted. The inventory is generated from real bytecode, not guesswork, and it is the authority.

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

Test result counts (the Gradle output does not print them):

```bash
awk -F'"' '/<testsuite /{t+=$4;f+=$8;e+=$10} END{print "tests="t" failures="f" errors="e}' build/test-results/test/TEST-*.xml
```

```bash
grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' run/server/junit-out/TEST-*.xml
```

Current baseline: **120 plain-JVM tests, 21 Forge server tests, all passing.**

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
`2026-08-14+678` (241 jars, 28 consumers). Diff against it; anything present there but absent from the port is a
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

**Java 8 target.** No `List.of`, no `var`, no switch expressions in main or test sources.

**Two test classes sharing global registry state** must guard their registrations, and the registries' error paths call
a logger that is null until `preInit`, so they cannot run headless at all.

## What is done

All eight load-bearing `$class` helpers from the inventory, both registries, and the two central types:

`IDWriter`, `PartialOcclusionTest`/`JPartialOcclusion`, `TCuboidPart`/`JCuboidPart`, the `TNormalOcclusion` unit,
`TFacePart`, the `TIconHitEffects` unit, `TItemMultiPart`/`JItemMultiPart`, `TEdgePart`, `Saw`, `MicroMaterialRegistry`,
`MultiPartRegistry`, `TileMultipart`, `TMultiPart`, `TickScheduler`, `BlockMultipart`.

Plus the six marker interfaces: `TSlottedPart`, `IRandomDisplayTick`, `INeighborTileChange`, `TRandomUpdateTick`,
`ISidedHollowConnect`, `IMicroMaterialRender`, plus `MultipartHelper`, `TileCache`, `PacketScheduler` and the `ControlKeyModifer` pair.

70 Java files, 51 Scala files, ~6,471 Scala lines left (non-blank; that is the metric this figure has always used).

## What is left, and in what order

**Low risk, good next steps.** The two `package.scala` objects. After those the low-risk list is empty and the
next work is the medium group.

`IRedstonePart.scala` is misleadingly named and is **not** a marker-trait file. It holds six traits plus
`RedstoneInteractions`, whose `MODULE$` is load-bearing, so it is its own piece of work at medium risk.

**Medium.** The `handler` packages on both sides, `MultipartRenderer`, `MicroRecipe`, `ItemMicroPart`,
`MicroblockPlacement`, `PlacementGrids`, `BlockMicroMaterial`, `MissingMicroMaterial`, `ConfigContent`,
`DefaultContent`.

**Phase 5, needs the `registerJavaTrait` path.** `TileMultipartClient` and everything in `multipart/scalatraits/`.
These are registered with the ASM generator; converting them is a different mechanism with documented restrictions.
Do the pilot deliberately, as its own piece of work.

**Phase 6/7, last.** `Microblock` and the microblock shape hierarchy, `MicroblockGenerator`, `MultipartGenerator`, and
all of `multipart/asm/`. The ASM subsystem should be last; freeze generated-class fixtures before touching it.

## Known gaps

- The full manual checklist has not been completed. Limited smoke testing has been performed in the GTNH full pack, but individual checklist items remain unrecorded and unverified.
  Every port since `TCuboidPart` has deferred something to it, and `BlockMultipart` leans on it hardest: breaking, selection boxes, collision, pick block, activation, particles and
  light are covered by nothing automated. Run it in a real client before this branch goes near a release.
- No CPU or allocation profiles exist. Phase 4 has not started, so there is no baseline to compare against.
- NBT and packet layout fixtures exist only for the material id carrier.
- `TileMultipart`'s `partList` still republishes an immutable Scala `Seq` on every mutation, and `operate` still
  allocates a `Function1` per call. Both are faithful to the reference and both are deliberately left for Phase 4,
  which needs profiling first.
- Phase 1 never properly started. GTNHLib is currently commented out in `dependencies.gradle`; it had been added at
  `api` scope to satisfy a test compile rather than because a migration change needed it.
