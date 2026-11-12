package codechicken.multipart;

/**
 * Supported redstone capability of generated multipart tiles hosting {@link IRedstonePart}. Use this stable Java
 * interface for consumer calls, rather than the transformed TRedstoneTile implementation class. Not every multipart
 * tile implements it; test instanceof when the caller does not already require this capability. Other connector
 * implementations may implement only {@link IRedstoneConnector}.
 */
public interface IRedstoneTile extends IRedstoneConnector {

    /**
     * Returns the geometrically open connection mask for a Minecraft side (0..5): bits 0..3 are the four rotations
     * around that side and bit 4 is the center. Face and edge parts can obstruct these connections. This is not the
     * mask of connections actually offered by contained redstone parts; use getConnectionMask for that query. Reads
     * current part/slot state without caching or notifications. The caller supplies a valid side and uses the world's
     * thread; this interface adds no validation or synchronization.
     */
    int openConnections(int side);
}
