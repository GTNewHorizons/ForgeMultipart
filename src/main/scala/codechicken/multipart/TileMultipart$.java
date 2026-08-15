package codechicken.multipart;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import scala.Tuple2;

/**
 * Scala companion singleton. Retained because compiled Scala consumers read MODULE$ and call these instance methods.
 */
public final class TileMultipart$ {

    public static final TileMultipart$ MODULE$ = new TileMultipart$();

    private TileMultipart$() {}

    public int renderID() {
        return TileMultipart.renderID();
    }

    public void renderID_$eq(int value) {
        TileMultipart.renderID_$eq(value);
    }

    public TileMultipart getOrConvertTile(World world, BlockCoord pos) {
        return TileMultipart.getOrConvertTile(world, pos);
    }

    public Tuple2<TileMultipart, Object> getOrConvertTile2(World world, BlockCoord pos) {
        return TileMultipart.getOrConvertTile2(world, pos);
    }

    public TileMultipart getTile(World world, BlockCoord pos) {
        return TileMultipart.getTile(world, pos);
    }

    public boolean checkNoEntityCollision(World world, BlockCoord pos, TMultiPart part) {
        return TileMultipart.checkNoEntityCollision(world, pos, part);
    }

    public boolean canPlacePart(World world, BlockCoord pos, TMultiPart part) {
        return TileMultipart.canPlacePart(world, pos, part);
    }

    public boolean replaceable(World world, BlockCoord pos) {
        return TileMultipart.replaceable(world, pos);
    }

    public TileMultipart addPart(World world, BlockCoord pos, TMultiPart part) {
        return TileMultipart.addPart(world, pos, part);
    }

    public void handleDescPacket(World world, BlockCoord pos, PacketCustom packet) {
        TileMultipart.handleDescPacket(world, pos, packet);
    }

    public void handlePacket(BlockCoord pos, World world, int i, PacketCustom packet) {
        TileMultipart.handlePacket(pos, world, i, packet);
    }

    public TileMultipart createFromNBT(NBTTagCompound tag) {
        return TileMultipart.createFromNBT(tag);
    }

    public void dropItem(ItemStack stack, World world, Vector3 pos) {
        TileMultipart.dropItem(stack, world, pos);
    }
}
