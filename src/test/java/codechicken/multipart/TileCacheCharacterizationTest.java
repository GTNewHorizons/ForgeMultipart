package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;

/**
 * The cache is global mutable state, so every test clears it on both sides. findTile needs a world and is covered by
 * the Forge server suite instead.
 */
class TileCacheCharacterizationTest {

    @BeforeEach
    @AfterEach
    void clearCache() {
        TileCache.clear();
    }

    @Test
    void addStoresTheTileAtItsOwnCoordinatesAndNotFlaggedRemoved() {
        TileMultipart tile = tileAt(3, 4, 5);

        TileCache.add(tile);

        TileCache.FlaggedTile entry = entryAt(new BlockCoord(3, 4, 5));
        assertNotNull(entry);
        assertSame(tile, entry.t());
        assertFalse(entry.removed());
    }

    @Test
    void removeStoresTheSameTileFlaggedRemoved() {
        TileMultipart tile = tileAt(3, 4, 5);
        TileCache.add(tile);

        TileCache.remove(tile);

        TileCache.FlaggedTile entry = entryAt(new BlockCoord(3, 4, 5));
        assertNotNull(entry);
        assertSame(tile, entry.t());
        assertTrue(entry.removed());
        assertEquals(1, cacheSize(), "remove replaces the entry rather than dropping it");
    }

    /** The cache is keyed by coordinate value, which is what makes a lookup from a fresh BlockCoord work at all. */
    @Test
    void entriesAreKeyedByCoordinateValueNotByIdentity() {
        TileMultipart tile = tileAt(-7, 200, 12);
        TileCache.add(tile);

        assertNotNull(entryAt(new BlockCoord(-7, 200, 12)));
        assertNull(entryAt(new BlockCoord(-7, 200, 13)));
    }

    @Test
    void aSecondTileAtTheSameCoordinateReplacesTheFirst() {
        TileMultipart first = tileAt(1, 1, 1);
        TileMultipart second = tileAt(1, 1, 1);
        TileCache.add(first);

        TileCache.add(second);

        assertSame(second, entryAt(new BlockCoord(1, 1, 1)).t());
        assertEquals(1, cacheSize());
    }

    @Test
    void separateCoordinatesAreHeldIndependently() {
        TileCache.add(tileAt(0, 0, 0));
        TileCache.add(tileAt(0, 1, 0));

        assertEquals(2, cacheSize());
        assertNotNull(entryAt(new BlockCoord(0, 0, 0)));
        assertNotNull(entryAt(new BlockCoord(0, 1, 0)));
    }

    @Test
    void anUnknownCoordinateResolvesToNothing() {
        assertNull(entryAt(new BlockCoord(9, 9, 9)));
    }

    @Test
    void clearEmptiesTheCache() {
        TileCache.add(tileAt(2, 2, 2));

        TileCache.clear();

        assertEquals(0, cacheSize());
        assertNull(entryAt(new BlockCoord(2, 2, 2)));
    }

    private static TileMultipart tileAt(int x, int y, int z) {
        TileMultipart tile = new TileMultipart();
        tile.xCoord = x;
        tile.yCoord = y;
        tile.zCoord = z;
        return tile;
    }

    // The two adapters below are the only places the cache's collection types are named.

    private static TileCache.FlaggedTile entryAt(BlockCoord coord) {
        scala.Option<TileCache.FlaggedTile> entry = TileCache.apply(coord);
        return entry.isDefined() ? entry.get() : null;
    }

    private static int cacheSize() {
        return TileCache.map().size();
    }
}
