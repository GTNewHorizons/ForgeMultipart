package codechicken.microblock;

import org.lwjgl.opengl.GL11;

import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;

public final class CornerPlacementGrid$ implements PlacementGrid {

    public static final CornerPlacementGrid$ MODULE$ = new CornerPlacementGrid$();

    private CornerPlacementGrid$() {}

    @Override
    public void render(Vector3 hit, int side) {
        PlacementGrid.super.render(hit, side);
    }

    @Override
    public void glTransformFace(Vector3 hit, int side) {
        PlacementGrid.super.glTransformFace(hit, side);
    }

    @Override
    public void drawLines() {
        GL11.glVertex3d(-0.5, 0, -0.5);
        GL11.glVertex3d(-0.5, 0, 0.5);

        GL11.glVertex3d(-0.5, 0, 0.5);
        GL11.glVertex3d(0.5, 0, 0.5);

        GL11.glVertex3d(0.5, 0, 0.5);
        GL11.glVertex3d(0.5, 0, -0.5);

        GL11.glVertex3d(0.5, 0, -0.5);
        GL11.glVertex3d(-0.5, 0, -0.5);

        GL11.glVertex3d(0, 0, -0.5);
        GL11.glVertex3d(0, 0, 0.5);

        GL11.glVertex3d(-0.5, 0, 0);
        GL11.glVertex3d(0.5, 0, 0);
    }

    @Override
    public int getHitSlot(Vector3 hit, int side) {
        int side1 = ((side & 6) + 3) % 6;
        int side2 = ((side & 6) + 5) % 6;
        double u = hit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes[side1]);
        double v = hit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes[side2]);
        int positiveU = u >= 0 ? 1 : 0;
        int positiveV = v >= 0 ? 1 : 0;
        int normal = (side & 1) ^ 1;
        return 7 + (normal << (side >> 1) | positiveU << (side1 >> 1) | positiveV << (side2 >> 1));
    }
}
