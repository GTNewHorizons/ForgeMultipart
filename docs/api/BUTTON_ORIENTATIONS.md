# Multipart button orientations

[Java API index](../API.md)

`ButtonPart.setOrientation(int, ForgeDirection)` maps a button's orientation metadata to the multipart face it
occupies. It updates the metadata-to-face and face-to-metadata lookups together, so placement, face slots, support
checks and strong redstone output continue to agree.

Et Futurum Requiem can enable its floor and ceiling button metadata during common initialization with direct calls:

```java
ButtonPart.setOrientation(0, ForgeDirection.UP);
ButtonPart.setOrientation(5, ForgeDirection.DOWN);
```

The [compiling example](../../src/functionalTest/java/codechicken/multipart/examples/ButtonOrientationExample.java)
contains the same two calls. Keep FMP-typed code in an optional compatibility class loaded only after the usual
mod-presence and supported-version checks.

## Metadata and face meaning

Pass only the orientation metadata from the low three bits, from `0` through `7`. Do not include bit `8`, which records
the pressed state, or bit `16`, which selects a wooden button. The face is the standard Forge direction returned by
`ButtonPart.getFace()`: `UP` for a ceiling button and `DOWN` for a floor button. `UNKNOWN`, `null` and out-of-range
metadata are rejected with `IllegalArgumentException` before either map changes.

The mapping is one-to-one. Setting an existing metadata value to another face clears its old reverse entry; assigning
a face already used by another metadata value clears that old forward entry. Repeating an existing pair is harmless.

Call the method during common mod initialization on each physical side, before worlds load button parts or players can
place them. The mappings are process-global. Changing one later also changes how existing button metadata is
interpreted for slots, support and redstone notification, so runtime reconfiguration is unsupported.

This method does not define a vanilla button block's bounds. `ButtonPart` captures all 16 button block bounds during
class initialization, after normal class transformations; a mod adding metadata orientations must still make its
button block expose the matching bounds.

## Migrating Et Futurum Requiem

Replace the reflective reads, four array writes and reflective field assignments with the two calls above. Do not
catch and ignore API failures: an invalid metadata or direction is a configuration error that should remain visible.
Keep Et Futurum's existing FMP presence/version gate so older FMP versions never load the typed compatibility class.

The public mutable `metaSideMap` and `sideMetaMap` fields remain binary and reflective compatibility surfaces for old
releases. New integrations should not read, replace or mutate them directly. Their removal still requires a released
Et Futurum migration, target-pack adoption and a fresh consumer scan.

## Validation

Forge characterization first freezes the old Et Futurum mutation and verifies exact metadata and placement on all six
faces. API tests verify the two direct calls produce the same arrays, displaced pairs remain one-to-one, invalid input
is atomic, and the example compiles. A physical client with Et Futurum must still complete the
[all-face button check](../migration/MANUAL_CHECKS.md) before release.
