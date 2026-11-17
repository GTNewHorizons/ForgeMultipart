package codechicken.multipart.examples;

import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockGenerator;

/** Forge-tested compiling example for docs/api/MICROBLOCK_CREATION.md. */
public final class MicroblockCreationExample {

    private MicroblockCreationExample() {}

    /** Recreates the family/material/shape on the requested side; does not clone other custom state or bindings. */
    public static Microblock recreateForSide(Microblock source, boolean client) {
        MicroblockClass microClass = source.microClass();
        int material = source.material();
        byte shape = source.shape();
        Microblock created = MicroblockGenerator.create(microClass, material, client);
        created.setShape(shape >> 4, shape & 0xF);
        return created;
    }
}
