package codechicken.multipart;

/** A face-attached redstone part whose connections reduce to the edge between two faces. */
public interface IFaceRedstonePart extends IRedstonePart {

    int getFace();
}
