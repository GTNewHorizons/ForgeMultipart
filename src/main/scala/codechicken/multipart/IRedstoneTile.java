package codechicken.multipart;

/** Internal interface for multipart tiles hosting {@link IRedstonePart}. */
public interface IRedstoneTile extends IRedstoneConnector {

    int openConnections(int side);
}
