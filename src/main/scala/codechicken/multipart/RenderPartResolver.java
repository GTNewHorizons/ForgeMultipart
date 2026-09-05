package codechicken.multipart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.raytracer.ExtendedMOP;
import net.minecraft.world.IBlockAccess;
import scala.Tuple2;

/**
 * A helper class for resolving parts so we don't do a double look up
 */
public final class RenderPartResolver {

    private static TMultiPart cachedPart;
    private static boolean attemptedResolution;

    public static TMultiPart resolve(IBlockAccess world, int x, int y, int z) {
        if (attemptedResolution) {
            return cachedPart;
        }

        attemptedResolution = true;
        cachedPart = null;

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

        int index = (Integer) indexObject;
        TileMultipartClient tile = getMultipartTile(world, x, y, z);
        if (tile == null) {
            return null;
        }

        // Needed because scala is stupid and the client tile doesn't expose what we need.
        TileMultipart multipart = (TileMultipart) tile;

        if (index < 0 || index >= multipart.jPartList().size()) {
            return null;
        }
        cachedPart = multipart.jPartList().get(index);

        return cachedPart;
    }

    private static TileMultipartClient getMultipartTile(IBlockAccess world, int x, int y, int z) {
        if (world.getTileEntity(x, y, z) instanceof TileMultipartClient) {
            return (TileMultipartClient) world.getTileEntity(x, y, z);
        }
        return null;
    }

    public static void clear() {
        cachedPart = null;
        attemptedResolution = false;
    }
}
