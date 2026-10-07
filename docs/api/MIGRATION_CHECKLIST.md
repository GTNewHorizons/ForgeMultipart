# Consumer migration checklist

[API index](../API.md) · [Java-port release notes](../RELEASE_NOTES.md)

Use this when rebuilding a mod against the Java-port artifact. It is a release checklist, not a replacement for the
method-specific guides linked from the API index.

## Before editing

- [ ] Inventory source and reflective references to `codechicken.multipart` and `codechicken.microblock`, including
  inherited traits, companion objects, `$class` helpers and private fields.
- [ ] Preserve published part/material IDs, NBT keys and packet field order unless the change includes an explicit data
  migration or protocol break.
- [ ] Identify the exact FMP artifact used to compile and run. New Java APIs on `algent/java` have no minimum published
  version yet; require a release that actually contains every method used.
- [ ] If FMP is optional, keep FMP-typed code in a compatibility class loaded only after checking mod presence and a
  supported version. Keep FMP types out of classes loaded unconditionally.

## Change the integration

- [ ] Prefer the documented direct API over companion lookup, private-field access or new reflection.
- [ ] Call generated tile behavior through `TileMultipart` or a stable capability interface such as `IRedstoneTile`,
  `IInventory`, `ISidedInventory` or `IFluidHandler`. Do not compile method or field access against raw classes under
  `codechicken.multipart.scalatraits` or `TileMultipartClient`; Forge transforms those classes into interfaces.
- [ ] For recompiled Scala traits, audit every method formerly supplied by trait linearization. A successful compile is
  insufficient: Java superclass methods can silently win over interface defaults. Apply the explicit forwarders in
  the [release notes](../RELEASE_NOTES.md#2-silent-behavior-changes-recompiled-scala-trait-composition).
- [ ] Use `canPlacePart` followed by `addPart` for normal server placement, and `remPart` for removal. Retain the tile
  returned by additions/removals because generated capabilities can replace the container; final removal returns null.
- [ ] Use `save`/`load` for server persistence and `writeDesc`/`readDesc` or a symmetric custom update for clients.
  Mark persistent state dirty, synchronize client-visible state and notify parts/neighbors only when that state needs
  each effect. See the [custom-part lifecycle](CUSTOM_PARTS.md).
- [ ] Treat `generateCompositeTile`, `setPartList` and `loadPartList` as reconstruction operations. Follow their state,
  binding, installation and notification responsibilities instead of substituting them for ordinary placement.

## Verify the rebuilt consumer

- [ ] Compile against the same FMP artifact required at runtime, then inspect the rebuilt bytecode for references to raw
  transformed trait classes, removed Scala-shaped entry points and legacy private fields.
- [ ] Test optional integration with FMP absent and present. Test a dedicated server separately from a physical client;
  headless Forge coverage does not establish client class stripping, rendering or GPU behavior.
- [ ] Load representative pre-migration saves and exercise placement, updates, removal, chunk reload and movement for
  every changed part or integration. Verify NBT and packet state, generated capabilities and callback order.
- [ ] Run the rebuilt jar in the target pack with the actual interacting mods. Record manual limits instead of treating
  source inspection or isolated tests as full-pack proof.

## Close adoption before removing compatibility

A source patch proves only that the migration was written. Keep the old bridge, descriptor, field or Scala runtime
path until all applicable gates are complete:

1. The consumer patch is merged and its rebuilt jar passes the checks above.
2. A consumer release containing that jar exists.
3. The target pack requires the compatible FMP and consumer releases.
4. A fresh consumer scan confirms no supported released jar still uses the legacy contract.

Documented Java entry points are intended to survive later Scala removal. Undocumented public members, raw transformed
classes and Scala-shaped bridges do not gain that guarantee merely because current source can call them.
