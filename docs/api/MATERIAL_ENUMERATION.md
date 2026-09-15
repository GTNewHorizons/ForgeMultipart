# Enumerating microblock materials from Java

[API index](../API.md)

The `algent/java` branch adds `MicroMaterialRegistry.materialCount()`. Use it with the existing
`materialName(int)` and `getMaterial(int)` methods to enumerate materials without importing Scala types.
This API is not yet tied to a published release; consumers must require an FMP version containing the addition.

## Example

The following method copies the active material names in numeric ID order:

```java
public static List<String> materialNames() {
    int count = MicroMaterialRegistry.materialCount();
    List<String> names = new ArrayList<>(count);
    for (int id = 0; id < count; id++) {
        names.add(MicroMaterialRegistry.materialName(id));
    }
    return names;
}
```

Imports and the enclosing class are in the [compiling example](../../src/test/java/codechicken/microblock/examples/MaterialEnumerationExample.java).
It is compiled and exercised by the normal `test` task. It also compiles with only the FMP dev jar and Java 8 APIs on
the compile classpath; this does not remove FMP's runtime dependencies. Use `MicroMaterialRegistry.getMaterial(id)` in the loop when
you also need the `IMicroMaterial`, for example to obtain its item. That returns the registered instance, not a copy.
The example's list is an independent copy; editing it does not change the registry.

## Timing, order and identity

- Enumerate after ForgeMicroblock has built its ID map during post-initialization. A consumer's own post-init handler
  needs suitable mod ordering if it enumerates there; later lifecycle/game callbacks can use the initialized map.
  Consumers must not call `setupIDMap()` themselves to force readiness.
- On a multiplayer client, enumeration of the connected server's IDs requires a completed, successful FMP handshake.
  FMP also rebuilds the local map before server startup. Reacquire IDs after map replacement.
- Use the game/lifecycle thread. The calls are live lookups, not an atomic snapshot: do not rebuild the map or perform
  registration/handshake work from inside an enumeration loop.
- Local setup orders IDs by material name. A multiplayer client adopts server order; do not sort or filter IDs when
  preserving a mapping. Store material names in saved data and resolve their numeric IDs for the active map.
- `materialCount()` counts ID slots, including the missing-material placeholder when present. It throws
  `IllegalStateException` before initialization and returns zero for an initialized empty map.
- A failed handshake can leave unresolved slots. The count includes them; `materialName(id)` and `getMaterial(id)`
  retain their existing `NullPointerException` for such slots. FMP disconnects the client for missing mappings.
  The count is not a handshake-success indicator, and callers should not silently skip unresolved IDs.
- The existing indexed lookups throw `ArrayIndexOutOfBoundsException` outside the active ID range and
  `NullPointerException` before map initialization.

## Migrating existing consumers

| Legacy expression | Java API |
| --- | --- |
| `getIdMap().length` | `materialCount()` |
| `getIdMap()[id]._1()` | `materialName(id)` |
| `getIdMap()[id]._2()` | `getMaterial(id)` |
| Loop over the tuple array | Loop over IDs from zero to `materialCount()`, exclusive |

For UtilitiesInExcess, apply this to `FMPRecipeLoader.run()` and `UEMultipartItem.getSubItems()`: replace each tuple
loop with an ID loop and use `materialName(id)` wherever the tuple name was used. Preserve its recipe/item ordering,
filters and material-name NBT. A material object, when needed, is available from `getMaterial(id)`.
Extra Utilities' NEI handlers use both tuple fields; both indexed replacements are available.

The static and companion `getIdMap()` methods are deprecated, with their original descriptors and live-array
behavior retained. This change does not authorize removal of either method. The
[consumer adoption ledger](../migration/COMPATIBILITY.md#java-api-adoption-ledger) records the migrations and
released pack versions still needed.

## Validation

The JVM tests cover the legacy array contract, initialization, server ordering, empty maps, unresolved slots, the
compiled Java example and the frozen Scala consumer. The Forge test checks that the Java enumeration matches the
initialized registry and the material-ID handshake payload. The public addition does not change packet or NBT formats.
