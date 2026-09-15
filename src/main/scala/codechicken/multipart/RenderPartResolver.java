package codechicken.multipart;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;

import codechicken.lib.raytracer.ExtendedMOP;
import scala.Tuple2;

/** Resolves the multipart selected by Minecraft's current hit result. */
public final class RenderPartResolver {

    public static TMultiPart resolve(IBlockAccess world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        // Make sure the hit is actually this block.
        MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;

        if (hit == null || hit.blockX != x || hit.blockY != y || hit.blockZ != z) {
            return null;
        }

        Object data = ExtendedMOP.getData(hit);

        if (!(data instanceof Tuple2)) {
            return null;
        }
        Tuple2<?, ?> hitInfo = (Tuple2<?, ?>) data;
        Object indexObject = hitInfo._1();
        if (!(indexObject instanceof Integer)) {
            return null;
        }

        TileMultipart tile = TileMultipart.class.cast(BlockMultipart.getClientTile(world, x, y, z));
        if (tile == null) {
            return null;
        }

        List<TMultiPart> parts = tile.jPartList();
        int index = (Integer) indexObject;
        if (index < 0 || index >= parts.size()) {
            return null;
        }
        return parts.get(index);
    }
}
