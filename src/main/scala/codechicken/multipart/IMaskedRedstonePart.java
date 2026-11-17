package codechicken.multipart;

/** A redstone part that supplies its own side connection masks. */
public interface IMaskedRedstonePart extends IRedstonePart {

    int getConnectionMask(int side);
}
