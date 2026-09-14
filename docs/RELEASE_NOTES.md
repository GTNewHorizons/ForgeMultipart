# Release notes — the Java port

ForgeMultipart's implementation moved from Scala to Java. This page is for mod authors who depend on FMP.

**If you ship a compiled jar and do not rebuild it, nothing here affects you.** Every runtime interface, method
descriptor and reflective name the audited consumers use was preserved deliberately, and the retained Scala bridges
still link. The compatibility ledger for what did change is [JAVA_MIGRATION_DIVERGENCES.md](../JAVA_MIGRATION_DIVERGENCES.md).

**If you rebuild against the new dev jar, read on.** Recompiling is the case that breaks, and one class of break is
silent. There are three kinds, in descending order of how badly they fail. The [consumer migration checklist](api/MIGRATION_CHECKLIST.md)
turns the cases below into concrete compile, runtime, release and pack-adoption gates.

## 1. Linkage errors: the generated tile traits

Six types were Scala traits, which compile to an interface plus a `$class` helper. They are now concrete Java classes
in the dev jar. Forge's transformer rewrites them back into interfaces at load time, so **existing binaries are
unaffected** — they already call interface methods, and the runtime type is still an interface.

Source compiled against the untransformed dev jar is the problem. `javac` and `scalac` see a class, emit
`invokevirtual` or `getfield`, and the JVM then rejects it against the runtime interface.

| Type | Recompiled call fails with |
| --- | --- |
| `scalatraits.TRedstoneTile` | `IncompatibleClassChangeError` |
| `scalatraits.TSlottedTile` | `NoSuchFieldError` on `v_partMap` |
| `scalatraits.TTileChangeTile` | `IncompatibleClassChangeError` |
| `scalatraits.TFluidHandlerTile` | `IncompatibleClassChangeError` |
| `scalatraits.TRandomDisplayTickTile` | `IncompatibleClassChangeError` |
| `TileMultipartClient` | `IncompatibleClassChangeError` |

**The fix is to target the stable capability interface instead of the raw dev-jar class.** The existing
`IRedstoneTile.openConnections` and `TileMultipart.partMap` contracts work on both old and new FMP. The new
`TileMultipart.refreshPartSlots` method requires an FMP release containing this Java API:

Note the package: the types that break are in `codechicken.multipart.scalatraits`, the replacements are not.

| Instead of | Use |
| --- | --- |
| `scalatraits.TRedstoneTile.openConnections(side)` | `codechicken.multipart.IRedstoneTile.openConnections(side)` |
| casting to `scalatraits.TSlottedTile` and writing `v_partMap` | `codechicken.multipart.TileMultipart.refreshPartSlots(part)` |
| reading `scalatraits.TSlottedTile.partMap(slot)` | `codechicken.multipart.TileMultipart.partMap(slot)` |

`refreshPartSlots` clears value-equal entries from the live array and dispatches the bind chain once. Keep your own
validation and state change before the call, and your sound, notification, description and scheduling order after it.
See [stable tile capability access](api/TILE_TRAIT_ACCESS.md) for the full contract and worked examples.

### Known affected consumers

Confirmed still present on each project's default branch:

| Mod | Call site | Member |
| --- | --- | --- |
| ProjectRed | `transmission/redwires.scala` | `TRedstoneTile.openConnections` |
| OpenComputers | `integration/fmp/PrintPart.scala` | `TSlottedTile.v_partMap` |

`TTileChangeTile`, `TFluidHandlerTile` and `TRandomDisplayTickTile` have no known consumer references.

## 2. Silent behavior changes: recompiled Scala trait composition

**This is the dangerous one — it compiles cleanly and does the wrong thing at runtime.**

A Java interface cannot supply a Scala trait's class supertype or its linearization. When a superclass method and an
interface default both apply, the superclass wins. So recompiling `extends TMultiPart with TCuboidPart` silently
selects `TMultiPart`'s empty methods instead of the cuboid implementations, and your part loses its collision boxes
rather than failing to build.

Where the implementation is now an interface default, forward explicitly:

| Trait | What to do when recompiling |
| --- | --- |
| `TCuboidPart` | Extend `JCuboidPart`, or forward subparts, collision boxes and breaking rendering to its statics |
| `TNormalOcclusion` | Combine `NormalOcclusionTest.apply(this, other)` with the intended super chain explicitly. Audit existing `super.occlusionTest` calls: they no longer route through the old trait |
| `TIconHitEffects` | Forward `addHitEffects` and `addDestroyEffects` to `IconHitEffects` |
| `TItemMultiPart` | Forward `onItemUse` to `JItemMultiPart`; the inherited `Item` method otherwise wins |
| `TRandomUpdateTick` | Implement `onWorldJoin` as `TickScheduler.loadRandomTick(this)` |
| `TScheduledPacketPart` | Forward `read` to `TScheduledPacketPart.readMask(this, packet)` |
| `ScratchBitSet` | Implement both methods and the storage accessors, and initialize storage explicitly |

Previously compiled calls into **retained** `$class` bridges still link; this applies only to recompilation.

## 3. Compile errors: Scala source syntax

These fail loudly at build time, so they are the safe category. Existing bytecode keeps working.

- An `object` used as a value needs `X$.MODULE$` where the companion is retained. Static forwarders alone do not
  supply a singleton value.
- Application sugar becomes an explicit `.apply(...)`, e.g. `NormalOcclusionTest.apply(...)`.
- `x.partList(i)` becomes `x.partList.apply(i)`.
- Scala property assignment needs the Java `_$eq(...)` method, e.g. `renderID`, `loadingWorld`, microblock `shape`.
- `Tuple2` members typed `Boolean` in Scala arrive as `Object` and need a cast.
- Scala 2.11 will not adapt a lambda to `Function1[_, BoxedUnit]`, so `operate { p => ... }` needs an explicit
  `AbstractFunction1`. Prefer the Java `forEachPart(Consumer)` entry point.
- The Face, Corner, Edge and Post bounds facades are Java static array getters now, so they need an extra empty
  argument list before the index.
- `ByteCodeReader.advance(length)(value)` becomes `advance(length, value)`.
- `ASMImplicits` no longer provides implicit or value-class syntax; call the retained `$extension` entry points
  explicitly.
- `ASMMixinFactory` constructor and `construct` arguments need an explicit Scala `Seq`, not varargs.

One runtime trap that is not a compile error: **do not call a side-only Scala object's static facade from common
Java.** `MultipartProxy.postInit()` exists in the raw jar but inherits `@SideOnly(CLIENT)`, so Forge strips the
forwarder on a dedicated server and the call fails with `NoSuchMethodError`. Call `MultipartProxy$.MODULE$.postInit()`
as the Scala reference did.

## Adopting the Java API

You do not have to convert your mod to Java. A Scala mod can call the Java surface directly, and the documented
entry points compile with no Scala on the classpath. Start at the [Java API index](API.md).

Direct typed calls are the intended end state. Reflection snippets in older guides are legacy interoperability
options, not the migration target. For optional integration, isolate FMP-typed code in a compatibility class loaded
only after mod-presence and version checks, and test both the absent and present cases.

## Will I have to migrate again?

No, if you migrate to the documented Java surface. That is the point of shipping it alongside the old one.

FMP still retains Scala storage, runtime and compatibility bridges. A later release removes them, and that release
removes the **Scala-shaped** entry points: `partList(): scala.collection.Seq`, `operate(Function1)`,
`registerParts(Seq)`, the `$class` trait helpers and the `MODULE$` companions. The Java siblings that replace them
stay. So:

| What you depend on | What the Scala-removal release does to it |
| --- | --- |
| The documented Java surface: `jPartList`, `forEachPart`, `registerPartFactory`, `refreshPartSlots`, `IRedstoneTile` | Unaffected. One migration, done. |
| The Scala-shaped entry points | Removed. This is the migration being asked for now. |
| A raw dev-jar class such as `scalatraits.TRedstoneTile` | Wrong target. See below. |

**The one way to migrate twice is to aim at the wrong type.** If a rebuild fails on `TRedstoneTile` and the fix looks
like "cast to the class the dev jar actually shows", that produces the `IncompatibleClassChangeError` in section 1 and
still has to be redone against `IRedstoneTile` afterwards. Always target the stable capability interface, never the
raw class the dev jar exposes for a transformed trait.

Two honest limits on that guarantee. It covers the surface that is documented here and in the
[compatibility audit](../JAVA_MIGRATION_COMPATIBILITY.md); a dependency in neither can still be caught out, which is
why removal waits on evidence of adoption in released consumer jars rather than on source patches alone. And FMP's own
source layout will change when Scala goes, since the Java sources currently live under `src/main/scala` for joint
compilation. That moves no class, package, descriptor or reflective name, so it is invisible to consumers; only
source-attachment paths in an IDE change.
