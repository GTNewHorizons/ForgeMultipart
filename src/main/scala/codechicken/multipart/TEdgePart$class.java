package codechicken.multipart;

/**
 * Binary bridge for Scala implementations compiled against the original trait. Their forwarders call these statics
 * directly, so the body cannot delegate back to the instance method without recursing.
 */
@Deprecated
public abstract class TEdgePart$class {

    private TEdgePart$class() {}

    public static boolean conductsRedstone(TEdgePart part) {
        return false;
    }

    public static void $init$(TEdgePart part) {}
}
