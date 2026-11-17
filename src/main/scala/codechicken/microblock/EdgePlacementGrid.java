package codechicken.microblock;

import codechicken.lib.vec.Vector3;

public final class EdgePlacementGrid {

    private EdgePlacementGrid() {}

    public static void glTransformFace(Vector3 hit, int side) {
        EdgePlacementGrid$.MODULE$.glTransformFace(hit, side);
    }

    public static void render(Vector3 hit, int side) {
        EdgePlacementGrid$.MODULE$.render(hit, side);
    }

    public static int getHitSlot(Vector3 hit, int side) {
        return EdgePlacementGrid$.MODULE$.getHitSlot(hit, side);
    }

    public static void drawLines() {
        EdgePlacementGrid$.MODULE$.drawLines();
    }
}
