# ForgeMultipart migration status

The core runtime implementation and the audited consumer-facing Java replacements are implemented.
Consumer patches, released versions and target-pack adoption are still pending; API availability does not
mean a consumer has migrated. The first Java release retains supported legacy binaries and FMP's Scala bridges.

## Documentation

| Document | Purpose |
| --- | --- |
| [Java API](../API.md) and [release notes](../RELEASE_NOTES.md) | Supported replacements, examples and source migration hazards |
| [Compatibility audit](COMPATIBILITY.md) | Frozen ABI inventory, source revisions, reflective contracts and consumer findings |
| [Consumer adoption ledger](COMPATIBILITY.md#java-api-adoption-ledger) | Legacy contract, replacement, source patch, release and target-pack adoption |
| [Divergences](DIVERGENCES.md) | Intentional effective compatibility differences |
| [Manual release checks](MANUAL_CHECKS.md) | Outstanding client, pack and release validation |
| [Maintainer guide](MAINTAINERS.md) | Build boundaries, compiler constraints, verification and performance protocol |

Use this page for remaining work, not a running diary. Record consumer progress in the adoption ledger and
release results in the manual checklist. Completed-work narratives remain in git history. Test counts and
artifact inventories should come from the reports for the revision being reviewed.

## Remaining work

1. **Migrate consumers.** Use the existing typed APIs, with optional integrations gated on presence/version.
   Preserve each consumer's state, ordering, failure policy and initialization timing. Record the source revision,
   first released version, minimum FMP dependency and actual target-pack artifact for each legacy contract.
   Consumer mods may remain Scala internally. Reference checkouts are not evidence of a shipped migration.
2. **Validate client and pack behavior.** Complete the manual checklist, including illuminated microblock
   construction/halos, previews, rendering, particles, lighting, interactions, old worlds, multiplayer and movement.
   Test optional dependencies both absent and present. The successful MCPC hook needs an actual patched World
   where that platform is supported. Headless Forge tests do not establish GPU or full-pack correctness.
3. **Measure performance.** Once representative API/extension workloads are stable, profile realistic scenes and
   make only justified changes. Separate FMP implementation gains from consumer API migration gains. Follow the
   [measurement protocol](MAINTAINERS.md#performance-validation); historical focused results are not whole-pack claims.
4. **Finish repository/release preparation.** Relocate Java sources only where remaining Scala dependencies allow
   it; reassess the forced Scala compilation/version guard after relocation. Refresh the root contributor README.
   Validate Java 8 artifacts and packaged/obfuscated artifacts on the supported modern runtime. Modern syntax
   expansion is optional and must respect the [build boundary](MAINTAINERS.md#build-and-runtime-boundaries).
5. **Remove FMP's Scala requirement last.** Complete the gates below before retiring bridges, compiler support
   or dependencies. Other mods' independent Scala requirements are outside this target.

Extra Utilities 1.2.12 remains an active supported consumer. Switching support to UtilitiesInExcess requires an
explicit support decision and actual pack adoption, including migration of the replacement's FMP contracts.
GTNHLib/fastutil, Sponge Mixins and a replacement runtime generator are conditional choices, not migration tasks
in themselves. Add them only for a demonstrated need.

## Scala removal gates

- Every affected adoption-ledger row is verified against the actual target-pack jars, including reflective fields,
  mixin targets, compiler-generated helpers and external Scala traits. Unmodifiable consumers must be retained or replaced.
- A deprecation/removal window and a release permitted to break the old ABI are agreed. That window is still open;
  adding a replacement or deprecation is not permission to delete a bridge.
- FMP's own Scala storage, retained models, trait inputs and legacy callers are replaced or retired. In particular,
  ProjectRed's released Java extension alone does not eliminate FMP's internal need for Scala-signature decoding.
- Shipped and generated FMP classes, descriptors and runtime loading paths contain no remaining Scala requirements;
  Scala compilation and compiler/runtime dependencies can then be removed.
- Migrated consumers pass old-world, packet, client and dedicated-server validation against the resulting artifact.

## Retained implementation

| Sources under `src/main/scala/codechicken` | Remaining dependency |
| --- | --- |
| `multipart/asm/ASMMixinCompiler.scala` | Nested models, construction callbacks and Scala entry shell |
| `multipart/asm/ScalaSignature.scala` | Named models, primitive/erased bridges and generic inner construction |
| `multipart/asm/StackAnalyser.scala` | Coordinated class/companion/models, state and handler callbacks |
| `microblock/MicroblockTraits.scala`, `FaceMicroblockTraits.scala`, `CornerMicroblockTraits.scala`, `EdgeMicroblockTraits.scala` | State, inheritance, accessor and super-call shells over Java behavior |
| `microblock/HollowMicroblockTraits.scala`, `TMicroOcclusion.scala` | Server/client inheritance, initializers, state and lifecycle bridges |

Java sources also retain Scala-backed storage and compatibility references. A source-file count does not measure
removal readiness. Further extraction should enable a consumer migration, fix a demonstrated issue or provide a
measured benefit; mechanical shell conversion is not the next milestone.
