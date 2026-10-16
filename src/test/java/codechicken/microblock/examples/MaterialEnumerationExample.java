package codechicken.microblock.examples;

import java.util.ArrayList;
import java.util.List;

import codechicken.microblock.MicroMaterialRegistry;

/** Compiling example for docs/api/MATERIAL_ENUMERATION.md; call after material IDs are ready. */
public final class MaterialEnumerationExample {

    private MaterialEnumerationExample() {}

    public static List<String> materialNames() {
        int count = MicroMaterialRegistry.materialCount();
        List<String> names = new ArrayList<>(count);
        for (int id = 0; id < count; id++) {
            names.add(MicroMaterialRegistry.materialName(id));
        }
        return names;
    }
}
