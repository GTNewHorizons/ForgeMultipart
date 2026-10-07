package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

public final class MicroblockRender {

    private MicroblockRender() {}

    public static void renderItem(Microblock part, int size, int slot) {
        MicroblockRender$.MODULE$.renderItem(part, size, slot);
    }

    public static void renderHighlight(EntityPlayer player, MovingObjectPosition hit, CommonMicroClass mcrClass,
            int size, int material) {
        MicroblockRender$.MODULE$.renderHighlight(player, hit, mcrClass, size, material);
    }

    public static ThreadLocal<BlockFace> face() {
        return MicroblockRender$.MODULE$.face();
    }

    public static void renderCuboid(Vector3 pos, IMicroMaterial material, int pass, Cuboid6 bounds, int faces) {
        MicroblockRender$.MODULE$.renderCuboid(pos, material, pass, bounds, faces);
    }
}
