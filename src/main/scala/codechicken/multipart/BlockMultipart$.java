package codechicken.multipart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import codechicken.lib.raytracer.ExtendedMOP;
import scala.Tuple2;

/**
 * Scala companion singleton. Retained because compiled Scala consumers read MODULE$ and call these instance methods.
 */
public final class BlockMultipart$ {

    public static final BlockMultipart$ MODULE$ = new BlockMultipart$();

    private BlockMultipart$() {}

    public TileMultipart getTile(IBlockAccess world, int x, int y, int z) {
        return BlockMultipart.getTile(world, x, y, z);
    }

    public TileMultipartClient getClientTile(IBlockAccess world, int x, int y, int z) {
        return BlockMultipart.getClientTile(world, x, y, z);
    }

    public Tuple2<Object, ExtendedMOP> reduceMOP(MovingObjectPosition hit) {
        return BlockMultipart.reduceMOP(hit);
    }

    public boolean drawHighlight(World world, EntityPlayer player, MovingObjectPosition hit, float frame) {
        return BlockMultipart.drawHighlight(world, player, hit, frame);
    }
}
