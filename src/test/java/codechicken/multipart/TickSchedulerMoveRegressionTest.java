package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

class TickSchedulerMoveRegressionTest {

    @Test
    void movedTileKeepsItsScheduledTickAfterTheSourceChunkUnloads() {
        MappedScheduler world = new MappedScheduler();
        TickScheduler.ChunkTickScheduler source = world.chunk(0, 0);
        world.chunk(1, 0);
        int[] calls = { 0 };
        TileMultipart tile = tile(0);
        TMultiPart part = part(tile, () -> calls[0]++);
        source.scheduleTick(part, 2, false);

        tile.xCoord = 16;
        world.moveTile(tile);
        source.unload();

        tick(world, 4);
        assertEquals(1, calls[0]);
    }

    @Test
    void moveRequestedFromACallbackIsAppliedAfterProcessing() {
        MappedScheduler world = new MappedScheduler();
        TickScheduler.ChunkTickScheduler source = world.chunk(0, 0);
        world.chunk(1, 0);
        int[] calls = { 0 };
        TileMultipart moved = tile(0);
        TMultiPart late = part(moved, () -> calls[0]++);
        TMultiPart mover = part(tile(0), () -> {
            moved.xCoord = 16;
            world.moveTile(moved);
        });
        source.scheduleTick(mover, 0, false);
        source.scheduleTick(late, 2, false);

        tick(world, 1);
        source.unload();
        tick(world, 3);
        assertEquals(1, calls[0]);
    }

    private static void tick(TickScheduler.WorldTickScheduler world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.preTick();
            world.postTick();
        }
    }

    private static TileMultipart tile(int x) {
        TileMultipart tile = new TileMultipart();
        tile.xCoord = x;
        return tile;
    }

    private static TMultiPart part(TileMultipart tile, Runnable callback) {
        TMultiPart part = new TMultiPart() {

            @Override
            public String getType() {
                return "test:moved-callback";
            }

            @Override
            public void scheduledTick() {
                callback.run();
            }
        };
        part.bind(tile);
        return part;
    }

    private static final class MappedScheduler extends TickScheduler.WorldTickScheduler {

        private final Map<Long, TickScheduler.ChunkTickScheduler> chunks = new HashMap<>();

        MappedScheduler() {
            super(null);
        }

        TickScheduler.ChunkTickScheduler chunk(int x, int z) {
            TickScheduler.ChunkTickScheduler chunk = new TickScheduler.ChunkTickScheduler(new Chunk(null, x, z), this);
            chunks.put(key(x, z), chunk);
            return chunk;
        }

        @Override
        TickScheduler.ChunkTickScheduler chunkScheduler(int chunkX, int chunkZ) {
            return chunks.get(key(chunkX, chunkZ));
        }

        private static long key(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }
    }
}
