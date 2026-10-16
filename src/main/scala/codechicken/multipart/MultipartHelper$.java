package codechicken.multipart;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Scala companion singleton, kept because guidenh reflects on this name as well as on MultipartHelper. Pure forwarders.
 */
public final class MultipartHelper$ {

    public static final MultipartHelper$ MODULE$ = new MultipartHelper$();

    private MultipartHelper$() {}

    public TileEntity createTileFromNBT(World world, NBTTagCompound tag) {
        return MultipartHelper.createTileFromNBT(world, tag);
    }

    public void sendDescPacket(World world, TileEntity tile) {
        MultipartHelper.sendDescPacket(world, tile);
    }

    public void registerTileConverter(MultipartHelper.IPartTileConverter<?> converter) {
        MultipartHelper.registerTileConverter(converter);
    }

    public TileMultipart createTileFromParts(Iterable<TMultiPart> parts) {
        return MultipartHelper.createTileFromParts(parts);
    }
}
