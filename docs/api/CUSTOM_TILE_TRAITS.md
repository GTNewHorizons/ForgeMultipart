# Custom generated tile traits from Java

[API index](../API.md) · [Stable capability access](TILE_TRAIT_ACCESS.md) · [Composite generation](COMPOSITE_GENERATION.md)

A custom tile trait adds behavior to every generated `TileMultipart` that contains a part implementing a chosen marker
interface. Use it for tile-wide aggregation or lifecycle behavior. If the tile only needs to expose one interface and
forward its methods to one implementing part, use `MultipartGenerator.registerPassThroughInterface` instead; FMP
already generates that forwarding and rejects a second implementor through occlusion.

The complete compiling example has four small types:

- [CustomTileTraitPart.java](../../src/functionalTest/java/codechicken/multipart/examples/CustomTileTraitPart.java) is
  the marker and contribution contract implemented by requesting parts.
- [CustomTileTraitCapability.java](../../src/functionalTest/java/codechicken/multipart/examples/CustomTileTraitCapability.java)
  is the stable interface used by callers.
- [CustomTileTrait.java](../../src/functionalTest/java/codechicken/multipart/examples/CustomTileTrait.java) is the raw
  input class FMP transforms, plus its ordinary helper.
- [CustomTileTraitExample.java](../../src/functionalTest/java/codechicken/multipart/examples/CustomTileTraitExample.java)
  registers and accesses the capability.

These files live in the functional-test source set and are examples, not classes shipped in the production artifact.

## Registration and selection

Register during common mod initialization, before FMP first inspects any part class implementing the marker:

```java
private static final String TRAIT = "example.multipart.ContributionTileTrait";

public static void register() {
    MultipartGenerator.registerTrait(ContributionPart.class.getName(), TRAIT);
}
```

Keep the trait name as a string literal. `ContributionTileTrait.class.getName()` loads the untransformed class before
FMP can rewrite it. The marker is an ordinary interface, so its class literal is safe. Names may contain dots or
slashes.

The two-argument overload installs the same trait on client and server generated tiles. Use
`registerTrait(marker, clientTrait, serverTrait)` when the sides differ; either trait name may be null. The order is
client first, then server. Register a parent Java trait before a child trait.

Registration is global for the running game. A second mapping for the same marker and side logs an error and leaves
the first mapping active. Registration loads and validates the trait bytes, so missing or invalid traits fail at that
point. It does not register a part type, construct a tile, load parts or bind ownership.

When FMP first sees a concrete part class, it records every registered marker that class implements. Composite classes
are then cached by the exact selected trait set and side. Registering after that inspection is too late for the cached
part class.

## Trait and stable capability

The transformed input is a top-level class extending `TileMultipart`. It may be abstract and needs a no-argument
constructor. Put the API that other code calls on a separate ordinary interface:

```java
public interface ContributionTileCapability {
    int totalContribution();
}

public abstract class ContributionTileTrait extends TileMultipart
        implements ContributionTileCapability {

    @Override
    public int totalContribution() {
        return ContributionTileTraitAccess.total(this);
    }
}
```

At runtime FMP rewrites `ContributionTileTrait` into an interface and copies its method bodies into a generated helper.
Calling through the untransformed class can therefore emit invalid class method or field bytecode. Callers use the
stable capability instead:

```java
int total = tile instanceof ContributionTileCapability
        ? ((ContributionTileCapability) tile).totalContribution()
        : 0;
```

Keep the absent-capability behavior in the caller-facing helper. The example returns zero for null and ordinary tiles.
Adding or removing a requesting part may replace the tile with another generated class, so retain the tile returned by
FMP's normal placement and removal APIs.

## Transformer-safe method bodies

The current Java trait transformer supports the example's small delegating method. Keep substantial work in an
ordinary top-level helper outside the transformed class:

```java
final class ContributionTileTraitAccess {
    static int total(Object receiver) {
        TileMultipart tile = (TileMultipart) receiver;
        int total = 0;
        for (TMultiPart part : tile.jPartList()) {
            if (part instanceof ContributionPart) {
                total += ((ContributionPart) part).contribution();
            }
        }
        return total;
    }
}
```

The helper parameter must be `Object`. If it is typed as the raw trait, javac can remove the cast to `TileMultipart`;
after transformation the verifier then sees an interface receiver where the bytecode expected a class. The helper is
also where inherited field/method access and field access on method arguments belong, because the transformer can
otherwise mistake those reads for trait-owned state.

Keep the transformed class free of nested, local and anonymous classes, lambdas and compiler-generated inner-class
helpers. Primitive-array allocation is also unsupported in a trait body. Put those operations in the ordinary helper.
Client-only members may use Forge's `@SideOnly(Side.CLIENT)`; FMP removes opposite-side members before collecting the
trait. Test the resulting generated class on each selected side.

## State and lifecycle

Prefer derived behavior when it is cheap enough. The example sums the current `jPartList()` and owns no tile state,
which avoids NBT, packet, invalidation and transition rules.

If a trait declares fields, FMP generates `copyFrom(TileMultipart)` when the trait does not declare one. It copies each
non-transient field only when the source tile has the same trait; transient fields are excluded. Persistent or
client-synchronized state still needs explicit NBT/description handling. A custom `copyFrom` replaces the generated
field copier, so it must preserve the normal superclass chain and define behavior for sources without the trait.

Cached aggregates must also follow every lifecycle path that can change them. Override the relevant `bindPart`,
`partRemoved` and `clearParts` hooks, call `super` in the established order, and cover tile promotion/demotion and
loading with a focused Forge test. Do not use a custom cache unless measurement shows the derived scan is too costly.

`MultipartGenerator.generateCompositeTile` only chooses or constructs the empty composite class. If using that advanced
API directly, prepare world/position or saved state and then call `loadPartList`; ordinary placement performs the
required transition itself. See the [staged generation guide](COMPOSITE_GENERATION.md) for ownership and failure rules.

## Validation and adoption

[CustomTileTraitFunctionalTest.java](../../src/functionalTest/java/codechicken/multipart/test/CustomTileTraitFunctionalTest.java)
registers the real example during Forge initialization. It checks server and client selections, transformed capability
dispatch, mixed-part aggregation, part binding, exact tile reuse and generated-class caching. The four example sources
also compile as Java 8 against the packaged dev artifact without Scala.

No supplied active consumer directly registers a custom `MultipartGenerator` tile trait; their current generated-tile
extensions use built-in traits or pass-through interfaces. This guide establishes the supported Java extension pattern
and does not count any consumer as migrated. Existing Scala trait inputs and binary contracts remain supported until
their release and target-pack adoption gates pass.
