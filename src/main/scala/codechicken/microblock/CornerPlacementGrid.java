package codechicken.microblock;

import codechicken.lib.vec.Vector3;

public final class CornerPlacementGrid {

    private CornerPlacementGrid() {}

    public static void glTransformFace(Vector3 hit, int side) {
        CornerPlacementGrid$.MODULE$.glTransformFace(hit, side);
    }

    public static void render(Vector3 hit, int side) {
        CornerPlacementGrid$.MODULE$.render(hit, side);
    }

    public static int getHitSlot(Vector3 hit, int side) {
        return CornerPlacementGrid$.MODULE$.getHitSlot(hit, side);
    }

    public static void drawLines() {
        CornerPlacementGrid$.MODULE$.drawLines();
    }
}
