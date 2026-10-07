package codechicken.multipart;

/**
 * Immutable outcome of a tile lookup. The referenced tile remains mutable; this result does not track later world
 * changes or own the tile's lifecycle. Obtain results through {@link TileMultipart#getOrConvertTileResult}.
 */
public final class TileConversionResult {

    private final TileMultipart tile;
    private final boolean converted;

    TileConversionResult(TileMultipart tile, boolean converted) {
        this.tile = tile;
        this.converted = converted;
    }

    /** Returns the existing tile or converted placeholder, or null if neither is available. */
    public TileMultipart getTile() {
        return tile;
    }

    /**
     * Returns true only when this lookup created a converted placeholder. Such a tile has a bound part, world and
     * coordinates, but has not been installed in the world. False covers both an existing tile and a null tile.
     */
    public boolean isConverted() {
        return converted;
    }
}
