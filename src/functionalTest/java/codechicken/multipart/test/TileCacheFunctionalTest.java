package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TileCache;
import codechicken.multipart.TileMultipart;

/**
 * findTile is the whole point of the cache and needs a world, so none of it is reachable headless. Every case here uses
 * a coordinate with no block in it, which is what drives BlockMultipart.getTile to null and hands control to the cache.
 */
class TileCacheFunctionalTest {

    private static final BlockCoord EMPTY = new BlockCoord(1000, 80, 1000);

    @BeforeEach
    @AfterEach
    void clearCache() {
        TileCache.clear();
    }

    @Test
    void recoversACachedTileWhenTheWorldHasNone() {
        TileMultipart tile = tileAt(EMPTY);
        TileCache.add(tile);

        assertSame(tile, TileCache.findTile(world(), EMPTY));
    }

    /**
     * The removed flag only suppresses the warning. Both branches return the tile, because the Some pattern matches
     * whatever the flag is -- the trailing null case in the reference is unreachable.
     */
    @Test
    void stillReturnsATileThatWasFlaggedRemoved() {
        TileMultipart tile = tileAt(EMPTY);
        TileCache.add(tile);
        TileCache.remove(tile);

        assertSame(tile, TileCache.findTile(world(), EMPTY));
    }

    @Test
    void throwsWhenTheWorldAndTheCacheBothComeUpEmpty() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> TileCache.findTile(world(), EMPTY));

        assertTrue(
                thrown.getMessage().startsWith("DC: Client multipart @"),
                "Unexpected message: " + thrown.getMessage());
    }

    private static TileMultipart tileAt(BlockCoord pos) {
        TileMultipart tile = new TileMultipart();
        tile.xCoord = pos.x;
        tile.yCoord = pos.y;
        tile.zCoord = pos.z;
        return tile;
    }

    private static World world() {
        World world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        return world;
    }
}
