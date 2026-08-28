# Java migration — focused performance baseline and results

This is the reproducible pre-optimization baseline for Phase 4. It runs inside the real deobfuscated Forge dedicated
server, using the same generated tiles as the functional suite. It is intentionally focused rather than a claim about
whole-pack TPS.

## Re-run

The local server EULA must already contain `eula=true`. From PowerShell at the repository root:

```powershell
.\gradlew.bat runFunctionalTestServer "-Pforgemultipart.profileFunctionalTests=true"
```

The property is quoted because PowerShell otherwise splits the dotted Gradle property. Normal
`runFunctionalTestServer` runs remain unchanged. Profile mode overwrites these ignored files:

- `run/server/forgemultipart-profile.txt` — exact timings and per-thread allocation counts;
- `run/server/forgemultipart-baseline.jfr` — JFR CPU, allocation-site, and GC events.

Useful JDK commands after the run:

```powershell
Get-Content run/server/forgemultipart-profile.txt
jfr summary run/server/forgemultipart-baseline.jfr
jfr view --width 220 hot-methods run/server/forgemultipart-baseline.jfr
jfr view thread-allocation run/server/forgemultipart-baseline.jfr
```

Open the `.jfr` in Java Mission Control for flame graphs and allocation-site inspection. The `startEpochMillis` and
`nanos` values in the text file define the exact time window for each phase. Java 8 recordings expose TLAB allocation
events rather than the newer `ObjectAllocationSample`, so `jfr view allocation-by-site` from a recent JDK is not a
valid summary of this recording.

## Workload

The fixture uses Zulu OpenJDK 8, eight parts per tile, 1,000,000 warm-up iterations, then 50,000,000 measured
iterations per phase:

- `updateEntity`: one `TileMultipart.updateEntity()` call, dispatching `update()` to eight ticking parts;
- `operate`: one `TileMultipart.operate(Function1)` call with one reused function and eight bound parts;
- `lightValue`: one `TileMultipart.getLightValue()` call across eight parts;
- `getTile`: one `BlockMultipart.getTile()` lookup returning the same non-empty eight-part tile;
- `redstoneQueries`: one `strongPowerLevel`, one `getConnectionMask`, and one masked `weakPowerLevel` call on a
  generated `TRedstoneTile` containing eight `IRedstonePart` implementations.

The checksum consumes all observable work. Allocation bytes come from HotSpot's per-thread allocation counter, not
from sampled JFR events. JFR remains the source for CPU samples and allocation-site ranking.

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
