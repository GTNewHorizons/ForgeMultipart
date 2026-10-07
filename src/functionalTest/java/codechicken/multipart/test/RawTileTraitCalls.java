package codechicken.multipart.test;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.scalatraits.TRedstoneTile;
import codechicken.multipart.scalatraits.TSlottedTile;

/** Deliberately unsafe dev-jar callers: runtime traits are interfaces, unlike these raw Java compiler inputs. */
public final class RawTileTraitCalls {

    private RawTileTraitCalls() {}

    public static int openConnections(TileMultipart tile, int side) {
        return ((TRedstoneTile) tile).openConnections(side);
    }

    public static TMultiPart[] slots(TileMultipart tile) {
        return ((TSlottedTile) tile).v_partMap;
    }
}
