# Compatibility fixtures

Files below this directory are frozen baselines, not a Gradle source set. They must be reviewed when they change
rather than regenerated automatically, otherwise an accidental change silently redefines the baseline.

## `abi/`

`gtnh-daily-678-consumers.txt` is the constant-pool scan of every mod jar in GTNH daily `2026-08-14+678`, listing
every downstream reference into `codechicken/multipart` and `codechicken/microblock`. Regenerate with
`tools/AbiScan.java` and diff against this file at every public-API phase; a member present here but absent from the
port is a linkage break in a shipping mod. See `JAVA_MIGRATION_ABI_INVENTORY.md` for the analysis.

## Precompiled binary consumers

When a port changes descriptors that the ABI inventory shows are load-bearing, freeze a consumer compiled against the
reference dev jar here as a class file and load it directly in a test. Recompiling a consumer against the port would
hide exactly the linkage failure the fixture exists to catch.

None are currently retained. `ReferenceScalaPartialOcclusion` was removed together with the `JPartialOcclusion$class`
bridge once the inventory showed no downstream consumer of that helper. The trait helpers that do need this treatment
are listed in `JAVA_MIGRATION_ABI_INVENTORY.md`.
