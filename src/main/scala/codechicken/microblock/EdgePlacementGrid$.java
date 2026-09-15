package codechicken.microblock;

import org.lwjgl.opengl.GL11;

import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.PartMap;

public final class EdgePlacementGrid$ implements PlacementGrid {

    public static final EdgePlacementGrid$ MODULE$ = new EdgePlacementGrid$();

    private EdgePlacementGrid$() {}

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

        GL11.glVertex3d(0.25, 0, -0.5);
        GL11.glVertex3d(0.25, 0, 0.5);

        GL11.glVertex3d(-0.25, 0, -0.5);
        GL11.glVertex3d(-0.25, 0, 0.5);

        GL11.glVertex3d(-0.5, 0, 0.25);
        GL11.glVertex3d(0.5, 0, 0.25);

        GL11.glVertex3d(-0.5, 0, -0.25);
        GL11.glVertex3d(0.5, 0, -0.25);
    }

    @Override
    public int getHitSlot(Vector3 hit, int side) {
        int side1 = (side + 2) % 6;
        int side2 = (side + 4) % 6;
        double u = hit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes[side1]);
        double v = hit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes[side2]);

        if (Math.abs(u) < 4 / 16d && Math.abs(v) < 4 / 16d) {
            return -1;
        }
        if (Math.abs(u) > 4 / 16d && Math.abs(v) > 4 / 16d) {
            return PartMap.edgeBetween(u > 0 ? side1 : side1 ^ 1, v > 0 ? side2 : side2 ^ 1);
        }

        int edgeSide;
        if (Math.abs(u) > Math.abs(v)) {
            edgeSide = u > 0 ? side1 : side1 ^ 1;
        } else {
            edgeSide = v > 0 ? side2 : side2 ^ 1;
        }
        return PartMap.edgeBetween(side ^ 1, edgeSide);
    }
}
