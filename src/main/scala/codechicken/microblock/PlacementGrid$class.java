package codechicken.microblock;

import org.lwjgl.opengl.GL11;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;

/** Binary bridge for Scala implementations compiled against the original trait. */
@Deprecated
public abstract class PlacementGrid$class {

    private PlacementGrid$class() {}

    public static void render(PlacementGrid grid, Vector3 hit, int side) {
        grid.glTransformFace(hit, side);
        GL11.glLineWidth(2);
        GL11.glColor4f(0, 0, 0, 1);
        GL11.glBegin(GL11.GL_LINES);
        grid.drawLines();
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    public static void drawLines(PlacementGrid grid) {}

    public static void glTransformFace(PlacementGrid grid, Vector3 hit, int side) {
        BlockCoord pos = new BlockCoord(hit);
        GL11.glPushMatrix();
        GL11.glTranslated(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        Rotation.sideRotations[side].glApply();
        Vector3 rotatedHit = new Vector3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5).subtract(hit)
                .apply(Rotation.sideRotations[side ^ 1].inverse());
        GL11.glTranslated(0, rotatedHit.y - 0.002, 0);
    }

    public static void $init$(PlacementGrid grid) {}
}
