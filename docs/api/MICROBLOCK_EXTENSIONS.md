# Extend generated microblocks from Java

Use `MicroblockGenerator.registerTrait(String)` with a Java source trait, then add its returned ID from an
`IGeneratedMaterial.addTraits` callback. The existing public API selects the Java compiler path automatically;
consumers do not need reflection, Scala types, companion lookup or direct compiler calls.

This example follows ProjectRed's illuminated microblock contract. It comprises three compiling Java files:

- [IlluminatedMicroblockTrait](../../src/functionalTest/java/codechicken/multipart/examples/IlluminatedMicroblockTrait.java):
  the generated extension's dynamic-render flag, light value and client render callback.
- [IlluminatedMicroMaterial](../../src/functionalTest/java/codechicken/multipart/examples/IlluminatedMicroMaterial.java):
  block-backed material and material-provided trait selection, including cutter metadata handling.
- [IlluminatedMicroblockExample](../../src/functionalTest/java/codechicken/multipart/examples/IlluminatedMicroblockExample.java):
  registration, ordinary Java implementation, configuration supplier and halo-renderer adapter.

Copy/adapt these into the consumer's own package; update the literal registration name accordingly. They are example
sources in FMP's functional-test source set, not production classes shipped for consumers to depend on.

## Registration and compilation

1. Register the consumer's lamp block first. During mod initialization, call `registerMaterials(lamp)` once, on both
   physical sides and before FMP creates the material-ID map. The helper registers the trait before materials.
2. Wire `dimLampParts` to a supplier of the consumer's current configuration. On the physical client, set
   `haloRenderer` to its halo queue before rendering; ProjectRed can forward to `RenderHalo.addLight(x, y, z, colour, box)`.
   Keep renderer-only dependencies out of common initialization. The helper supplies no renderer fallback.
3. Retain the consumer's existing material names, metadata 16 through 31, configuration defaults and block identity.
   The test fixture uses its own lamp block; a real ProjectRed migration uses ProjectRed's registered lamp.
4. Create parts through the [public microblock creation API](MICROBLOCK_CREATION.md). The material adds the same
   extension ID for every shape and both sides, without clearing the borrowed bit set. The generator also selects
   the shape's built-in traits and client trait where applicable. Repeated construction reuses the generated class,
   not the instance. Changing a part's material afterward does not regenerate its traits.

**Register the Java trait by name before loading it.** A Java class literal is appropriate for retained Scala
interfaces, but can load the untransformed Java input too early. Name-based registration is a supported generator
operation, not consumer reflection. IDs are local to this generator and must not be persisted or reused as tile-trait IDs.

The Java input extends `Microblock`, is abstract, and has a public no-argument constructor calling `super(0)`.
FMP discards that direct super call; the composite's real constructor receives the requested material ID. No extra
trait state is needed for this extension. Keep the input top-level, without nested/anonymous classes or lambdas.

After registration FMP rewrites that class into an interface. Do not instantiate it, subclass its raw class from
ordinary consumer Java, or invoke inherited members through that raw class type. `instanceof` works after
registration; dispatch uses stable `Microblock` methods. For new extension-specific methods, declare a separate
ordinary capability interface rather than emitting class calls against the transformed input.

The existing transformer cannot safely handle all inherited field/method accesses in a trait body. Its thin overrides
therefore delegate to an ordinary helper using an **Object parameter**, then cast to `Microblock` in that helper.
This forces the required runtime cast and keeps loops, allocation and callbacks outside the transformed input.
The three example files compile with Scala excluded from the Java classpath; they use Java 8 syntax because that is
the verified joint-compilation/test boundary. See the [compiler constraints](../migration/MAINTAINERS.md#retained-compiler-constraints).

## Behavioral contract

- The dynamic-render flag is always true. The client callback submits halos only on pass zero. It uses the part's
  tile coordinates, not the supplied camera-relative render position; colour is material metadata minus 16.
- Cutter strength asks the block for the harvest level at `meta() % 16`, preserving the illuminated metadata bit's
  exclusion. Ordinary block/meta access remains virtual through the supported methods.
- With dimming enabled, collect all siblings carrying this extension, sum `getSize() / 8.0`, multiply by 15,
  truncate to an integer, then cap at 15. Capture the matching parts before invoking size callbacks. With dimming
  disabled, any matching sibling gives light 15, otherwise zero. Do not substitute volume for encoded size.
- Read `tile().jPartList()` without detached-part filtering: this preserves the reference's membership behavior.
  The part must be bound before querying light; an unbound part fails rather than inventing a light value. Callback
  failures propagate. This example adds no stored per-part state, NBT keys or packet fields.
- Ordinary halos copy collision boxes and expand them by 0.025; source boxes remain untouched. Hollow halos instead
  use four rotated strips around the current connector opening. Their middle strips are trimmed along the opening,
  so simply expanding hollow collision boxes does not reproduce the same geometry. Halo boxes are newly allocated
  and may be retained by the renderer queue. Configuration and renderer access run on the owning game/render thread.

`@SideOnly(CLIENT)` belongs on the trait's render override. The common light flag/value remain present on the server.
The helper uses common geometry types, allowing headless verification; only the supplied halo renderer needs a client.
This is a behavior-preserving migration example, not a performance optimization or a replacement rendering engine.

## Validation and adoption

The separately committed baseline proves the retained external Scala trait composes with face, hollow, corner, edge
and post shapes, with fresh instances, class reuse, correct material identity and no implicit binding. The Java
example's Forge tests cover initialization registration, all 16 material variants, both callback-side inputs, all
five generated shape families, persistence, light aggregation and server stripping of the client override.
Geometry tests cover ordinary collision-box ownership and hollow halos on all six faces.

A headless compiler probe retains the actual compiled client method in a uniquely named test trait by removing its
side annotation from the test input. It checks generated dispatch, pass filtering and queued coordinates/colour/bounds.
That is **not** a physical-client construction, side-selection or GPU test. Existing side-filtering tests cover the
compiler separately; actual client generation, connector-dependent halos, lighting and previews remain required
checks in the [manual checklist](../migration/MANUAL_CHECKS.md).

ProjectRed's reference checkout is unchanged. Its migration must preserve registration order/names, wire the existing
configuration and halo queue, release the consumer, and validate the released jar in the pack. Keep ProjectRed's
external Scala trait and FMP's Scala-signature support until adoption and removal gates pass; the example does not
close consumer adoption or every other extension contract. See the [API index](../API.md).
