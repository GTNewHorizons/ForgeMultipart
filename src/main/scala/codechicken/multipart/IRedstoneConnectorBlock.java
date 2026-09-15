package codechicken.multipart;

import net.minecraft.world.IBlockAccess;

/** Block counterpart to {@link IRedstoneConnector}. */
public interface IRedstoneConnectorBlock {

    int getConnectionMask(IBlockAccess world, int x, int y, int z, int side);

    int weakPowerLevel(IBlockAccess world, int x, int y, int z, int side, int mask);
}
