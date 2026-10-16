package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;

/** Edge behavior behind the retained Scala inheritance metadata and helper bridges. */
final class EdgeMicroblockTraitLogic {

    private EdgeMicroblockTraitLogic() {}

    static void setShape(EdgeMicroblock part, int size, int slot) {
        ((Microblock) part).shape_$eq((byte) (size << 4 | (slot - 15)));
    }

    static Cuboid6 getBounds(EdgeMicroblock part) {
        return EdgeMicroClass.aBounds()[((Microblock) part).shape()];
    }

    static int getSlot(EdgeMicroblock part) {
        return ((Microblock) part).getShape() + 15;
    }
}
