package codechicken.multipart.examples;

import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClass;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;

/** Forge-tested compiling example for docs/api/FACTORY_LOOKUP.md. */
public final class PartFactoryLookupExample {

    private PartFactoryLookupExample() {}

    /** Creates an unbound microblock; the caller still loads its NBT and prepares the containing tile. */
    public static Microblock createMicroblock(String type, boolean client, int material) {
        IPartFactory2 factory = MultiPartRegistry.getPartFactory(type);
        if (!(factory instanceof MicroblockClass)) {
            return null;
        }
        return ((MicroblockClass) factory).create(client, material);
    }
}
