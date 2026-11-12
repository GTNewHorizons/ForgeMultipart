package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

class TickSchedulerCallbackRegressionTest {

    @Test
    void callbacksCanUnloadTheirChunksWithoutLosingOtherScheduledTicks() {
        TickScheduler.WorldTickScheduler world = new TickScheduler.WorldTickScheduler(null);
        int[] unloadedCalls = { 0 };
        for (int i = 0; i < 3; i++) {
            TickScheduler.ChunkTickScheduler chunk = new TickScheduler.ChunkTickScheduler(new Chunk(null, i, 0), world);
            chunk.scheduleTick(part(() -> {
                unloadedCalls[0]++;
                chunk.unload();
            }), 0, false);
        }
        int[] retainedCalls = { 0 };
        TickScheduler.ChunkTickScheduler retained = new TickScheduler.ChunkTickScheduler(new Chunk(null, 3, 0), world);
        retained.scheduleTick(part(() -> retainedCalls[0]++), 1, false);

        world.preTick();
        world.postTick();
        assertEquals(3, unloadedCalls[0]);
        assertEquals(0, retainedCalls[0]);

        world.preTick();
        world.postTick();
        world.preTick();
        world.postTick();
        assertEquals(3, unloadedCalls[0]);
        assertEquals(1, retainedCalls[0]);
    }

    private static TMultiPart part(Runnable callback) {
        TMultiPart part = new TMultiPart() {

            @Override
            public String getType() {
                return "test:scheduled-callback";
            }

            @Override
            public void scheduledTick() {
                callback.run();
            }
        };
        part.bind(new TileMultipart());
        return part;
    }
}
