# Saw cutting strength

[Java API index](../API.md)

`ItemSaw.setHarvestLevel(int)` changes an existing FMP saw's cutting strength without reflection. The value is read
by `harvestLevel()`, `getCuttingStrength(ItemStack)` and `getMaxCuttingStrength()`, so recipes and FMP's 3D saw
renderer observe the same updated tier.

IguanaTweaksTConstruct can replace its private-field lookup and write with direct calls:

```java
ItemSaw saw = (ItemSaw) item;
int oldStrength = saw.harvestLevel();
int updatedStrength = HarvestLevelTweaks.getUpdatedHarvestLevel(oldStrength);
saw.setHarvestLevel(updatedStrength);
```

The [compiling example](../../src/test/java/codechicken/microblock/examples/SawStrengthExample.java) contains the
setter call.

Iguana's client proxy also reads the three built-in saws and the renderer mode through `MicroblockProxy$.MODULE$`.
The Java facade exposes the same state directly:

```java
Item stoneSaw = MicroblockProxy.sawStone();
Item ironSaw = MicroblockProxy.sawIron();
Item diamondSaw = MicroblockProxy.sawDiamond();
if (!MicroblockProxy.useSawIcons()) {
    // Register the existing 3D saw renderers.
}
```

Keep the companion access only for already released binaries; rebuilt Java consumers should use the static facade.

## State and lifecycle

The setter changes only cutting strength. It does not recalculate the saw's durability or alter existing item-stack
damage. This matches Iguana's current post-construction field mutation. The integer is intentionally unrestricted,
like the constructor and legacy field; the consumer owns its tier mapping.

FMP calculates `MicroMaterialRegistry.getMaxCuttingStrength()` during ForgeMicroblock post-initialization and, since
FMP 2.0.1, again on `FMLLoadCompleteEvent`, after every mod's post-init. With 2.0.1 or later, startup integrations that
add or relevel saws only have to finish within their own post-init (Iguana's `after:*` post-init qualifies); depend on
`ForgeMultipart@[2.0.1,)` when relying on this. FMP 2.0.0 calculates it only in its own post-init. Don't add
`before:ForgeMicroblock` for this: combined with `after:*` it makes FML's mod sorter fail with a
`ModSortingException`. `MicroRecipe.canCut` is the only reader of the cached maximum and only runs when crafting. Call
the setter on the main initialization thread, before recipes can be used.

The method is specific to `ItemSaw`. Other `Saw` implementations may derive strength from each `ItemStack` and do
not promise mutable global strength.

## Legacy compatibility

The private `harvestLevel: int` field remains in place for existing Iguana releases. It is mutable so the supported
setter and legacy reflection update the same storage. New integrations should use the public getter and setter and
should not catch and ignore API failures.
