package codechicken.microblock;

import codechicken.lib.vec.Vector3;

public final class FacePlacementGrid {

    private FacePlacementGrid() {}

    public static void glTransformFace(Vector3 hit, int side) {
        FacePlacementGrid$.MODULE$.glTransformFace(hit, side);
    }

    public static void render(Vector3 hit, int side) {
        FacePlacementGrid$.MODULE$.render(hit, side);
    }

    public static int getHitSlot(Vector3 hit, int side) {
        return FacePlacementGrid$.MODULE$.getHitSlot(hit, side);
    }

    public static void drawLines() {
        FacePlacementGrid$.MODULE$.drawLines();
    }
}
