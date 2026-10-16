package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;

/** Corner behavior behind the retained Scala inheritance metadata and helper bridges. */
final class CornerMicroblockTraitLogic {

    private CornerMicroblockTraitLogic() {}

    static void setShape(CornerMicroblock part, int size, int slot) {
        ((Microblock) part).shape_$eq((byte) (size << 4 | (slot - 7)));
    }

    static Cuboid6 getBounds(CornerMicroblock part) {
        return CornerMicroClass.aBounds()[((Microblock) part).shape()];
    }

    static int getSlot(CornerMicroblock part) {
        return ((Microblock) part).getShape() + 7;
    }
}
