package codechicken.multipart;

import codechicken.lib.vec.Cuboid6;

/** Java callbacks for partial occlusion testing. */
public interface JPartialOcclusion {

    /** Returns the partial occlusion boxes. */
    Iterable<Cuboid6> getPartialOcclusionBoxes();

    /** Returns whether this part may be completely obscured. */
    default boolean allowCompleteOcclusion() {
        return false;
    }
}
