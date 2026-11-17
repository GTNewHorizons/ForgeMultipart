package codechicken.multipart;

/**
 * Mixin interface for parts that want to be notified of neighbor tile change events (comparators or inventory
 * maintainers).
 */
public interface INeighborTileChange {

    /** Returns whether this part needs calls for tile changes through one solid block. */
    boolean weakTileChanges();

    /** Callback for neighbor tile changes, from same function in Block. */
    void onNeighborTileChanged(int side, boolean weak);
}
