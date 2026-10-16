package codechicken.microblock;

import org.lwjgl.opengl.GL11;

import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;

public class FaceEdgeGrid implements PlacementGrid {

    private final double size;

    public FaceEdgeGrid(double size) {
        this.size = size;
    }

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

        GL11.glVertex3d(0.5, 0, 0.5);
        GL11.glVertex3d(size, 0, size);

        GL11.glVertex3d(-0.5, 0, 0.5);
        GL11.glVertex3d(-size, 0, size);

        GL11.glVertex3d(0.5, 0, -0.5);
        GL11.glVertex3d(size, 0, -size);

        GL11.glVertex3d(-0.5, 0, -0.5);
        GL11.glVertex3d(-size, 0, -size);

        GL11.glVertex3d(-size, 0, -size);
        GL11.glVertex3d(-size, 0, size);

        GL11.glVertex3d(-size, 0, size);
        GL11.glVertex3d(size, 0, size);

        GL11.glVertex3d(size, 0, size);
        GL11.glVertex3d(size, 0, -size);

        GL11.glVertex3d(size, 0, -size);
        GL11.glVertex3d(-size, 0, -size);
    }

    @Override
    public int getHitSlot(Vector3 hit, int side) {
        int side1 = (side + 2) % 6;
        int side2 = (side + 4) % 6;
        double u = hit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes[side1]);
        double v = hit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes[side2]);

        if (Math.abs(u) < size && Math.abs(v) < size) {
            return side ^ 1;
        }
        if (Math.abs(u) > Math.abs(v)) {
            return u > 0 ? side1 : side1 ^ 1;
        }
        return v > 0 ? side2 : side2 ^ 1;
    }
}
