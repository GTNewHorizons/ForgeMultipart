package codechicken.multipart;

import codechicken.lib.vec.Cuboid6;

/**
 * Java interface containing callbacks for normal occlusion testing. Make sure to override occlusionTest as in
 * {@link TNormalOcclusion}.
 */
public interface JNormalOcclusion {

    /** Return a list of normal occlusion boxes. */
    Iterable<Cuboid6> getOcclusionBoxes();
}
