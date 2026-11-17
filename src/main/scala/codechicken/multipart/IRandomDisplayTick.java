package codechicken.multipart;

import java.util.Random;

/**
 * Interface for parts that need random display ticks (torches).
 * <p>
 * Marker interface for TRandomDisplayTickTile.
 */
public interface IRandomDisplayTick {

    /** Called on a random display tick. */
    void randomDisplayTick(Random random);
}
