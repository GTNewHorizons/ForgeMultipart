# Java migration — focused performance baseline and first result

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

Next characterize and port the complete `IRedstonePart.scala` unit and eliminate the measured redstone
`IntRef`/closure paths without splitting its load-bearing `RedstoneInteractions$` singleton.
