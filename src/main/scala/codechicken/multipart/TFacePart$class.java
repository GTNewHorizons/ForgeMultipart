package codechicken.multipart;

/**
 * Binary bridge for Scala implementations compiled against the original trait. Their forwarders call these statics
 * directly, so the bodies cannot delegate back to the instance methods without recursing.
 */
@Deprecated
public abstract class TFacePart$class {

    private TFacePart$class() {}

    public static boolean solid(TFacePart part, int side) {
        return true;
    }

    public static int redstoneConductionMap(TFacePart part) {
        return 0;
    }

    public static void $init$(TFacePart part) {}
}
