package codechicken.multipart.examples;

import codechicken.multipart.IRedstoneTile;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Compiling consumer example using a stable capability instead of the raw transformed trait class. */
public final class TileTraitAccessExample {

    private TileTraitAccessExample() {}

    /** Returns false for null/non-redstone tiles; side is 0..5 and mask uses the low five connection bits. */
    public static boolean hasOpenConnection(TileMultipart tile, int side, int mask) {
        return tile instanceof IRedstoneTile && (((IRedstoneTile) tile).openConnections(side) & mask) != 0;
    }

    /** Call after a stored part has validated and applied a new slot mask, before its own notifications. */
    public static void refreshSlots(TileMultipart tile, TMultiPart part) {
        tile.refreshPartSlots(part);
    }
}
