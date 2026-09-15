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
setter call. Keep FMP-typed code in an optional compatibility class loaded only after the usual mod-presence and
supported-version checks.

## State and lifecycle

The setter changes only cutting strength. It does not recalculate the saw's durability or alter existing item-stack
damage. This matches Iguana's current post-construction field mutation. The integer is intentionally unrestricted,
like the constructor and legacy field; the consumer owns its tier mapping.

FMP calculates `MicroMaterialRegistry.getMaxCuttingStrength()` once during ForgeMicroblock post-initialization.
Startup integrations that add or relevel saws must finish first so the cached maximum sees the final tiers. Iguana's
optional ordering should therefore include `before:ForgeMicroblock` while retaining its existing FMP presence gate.
Call the setter on the main initialization thread, before recipes can be used.

The method is specific to `ItemSaw`. Other `Saw` implementations may derive strength from each `ItemStack` and do
not promise mutable global strength.

## Legacy compatibility

The private `harvestLevel: int` field remains in place for existing Iguana releases. It is mutable so the supported
setter and legacy reflection update the same storage. New integrations should use the public getter and setter and
should not catch and ignore API failures.

Removing the private field still requires a released Iguana migration, target-pack adoption and a fresh consumer
scan. The source patch, release and pack update are downstream work; the reference checkout was not modified.

## Validation

The baseline test reproduces Iguana's boxed reflective read/write on the untouched implementation. Regression tests
run both that path and the direct setter, checking the public getter, stack cutting strength, maximum saw strength and
unchanged durability. A pack with Iguana still needs the recorded cutting-tier check before release.
