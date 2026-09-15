package codechicken.multipart;

/** A tile whose redstone connections are split into an edge-and-center mask for each side. */
public interface IRedstoneConnector {

    int getConnectionMask(int side);

    int weakPowerLevel(int side, int mask);
}
