package codechicken.multipart;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.handler.MultipartProxy;

/**
 * In order to maintain tight client/server synchronisation without bandwidth overhead, all data written must be read.
 * Sometimes the client tile is removed from the chunk before the packet arrives. This class maintains a reference to
 * all multipart tiles. The original design placed tiles that were invalidated into a map that was cleared upon arrival
 * of the update packet. Due to other factors that have eluded identification, references to all tiles held until they
 * are replaced or the world is reloaded.
 */
public final class TileCache {

    private static final Map<BlockCoord, FlaggedTile> map = new HashMap<>();

    private TileCache() {}

    public static Map<BlockCoord, FlaggedTile> map() {
        return map;
    }

    public static void add(TileMultipart t) {
        map.put(new BlockCoord(t), new FlaggedTile(t, false));
    }

    public static void remove(TileMultipart t) {
        map.put(new BlockCoord(t), new FlaggedTile(t, true));
    }

    public static FlaggedTile apply(BlockCoord c) {
        return map.get(c);
    }

    public static void clear() {
        map.clear();
    }

    public static TileMultipart findTile(World world, BlockCoord c) {
        TileMultipart inWorld = BlockMultipart.getTile(world, c.x, c.y, c.z);
        if (inWorld != null) {
            return inWorld;
        }

        FlaggedTile entry = map.get(c);
        if (entry == null) {
            throw new RuntimeException("DC: Client multipart @" + c + " not found");
        }

        // A tile flagged removed is still returned; the flag only says the warning is expected.
        if (!entry.removed()) {
            MultipartProxy.logger().warn(
                    "Client multipart @" + c
                            + " vanished from world but was recovered. If possible causes can be identified, please report to the github issue tracker.");
        }
        return entry.t();
    }

    public static final class FlaggedTile {

        private final TileMultipart t;
        private final boolean removed;

        public FlaggedTile(TileMultipart t, boolean removed) {
            this.t = t;
            this.removed = removed;
        }

        public TileMultipart t() {
            return t;
        }

        public boolean removed() {
            return removed;
        }
    }
}
