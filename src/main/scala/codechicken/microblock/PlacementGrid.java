package codechicken.microblock;

import codechicken.lib.vec.Vector3;

public interface PlacementGrid {

    int getHitSlot(Vector3 hit, int side);

    default void render(Vector3 hit, int side) {
        PlacementGrid$class.render(this, hit, side);
    }

    default void drawLines() {}

    default void glTransformFace(Vector3 hit, int side) {
        PlacementGrid$class.glTransformFace(this, hit, side);
    }
}
