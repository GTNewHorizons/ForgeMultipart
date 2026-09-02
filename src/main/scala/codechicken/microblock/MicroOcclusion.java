package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;

public final class MicroOcclusion {

    private MicroOcclusion() {}

    public static void shrink(Cuboid6 renderBounds, Cuboid6 bounds, int side) {
        MicroOcclusion$.MODULE$.shrink(renderBounds, bounds, side);
    }

    public static int shrinkFrom(JMicroShrinkRender part, JMicroShrinkRender other, Cuboid6 renderBounds) {
        return MicroOcclusion$.MODULE$.shrinkFrom(part, other, renderBounds);
    }

    public static int shrink(JMicroShrinkRender part, Cuboid6 renderBounds, int slots) {
        return MicroOcclusion$.MODULE$.shrink(part, renderBounds, slots);
    }

    public static int shrinkSide(int firstSlot, int secondSlot) {
        return MicroOcclusion$.MODULE$.shrinkSide(firstSlot, secondSlot);
    }

    public static int recalcBounds(JMicroShrinkRender part, Cuboid6 renderBounds) {
        return MicroOcclusion$.MODULE$.recalcBounds(part, renderBounds);
    }

    public static int shapePriority(int slot) {
        return MicroOcclusion$.MODULE$.shapePriority(slot);
    }

    public static boolean shrinkTest(JMicroShrinkRender first, JMicroShrinkRender second) {
        return MicroOcclusion$.MODULE$.shrinkTest(first, second);
    }
}
