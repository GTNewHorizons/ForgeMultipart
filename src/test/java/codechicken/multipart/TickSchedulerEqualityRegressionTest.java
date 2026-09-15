package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

class TickSchedulerEqualityRegressionTest {

    @Test
    void equalPartsShareOneEntryAndKeepTheOriginalCallbackTarget() {
        TickScheduler.ChunkTickScheduler scheduler = scheduler();
        EqualPart first = new EqualPart("same");
        EqualPart equal = new EqualPart("same");
        EqualPart different = new EqualPart("different");

        scheduler.scheduleTick(first, 0, false);
        scheduler.scheduleTick(equal, 0, false);
        scheduler.scheduleTick(different, 0, false);

        assertFalse(scheduler.processTicks());
        assertEquals(1, first.calls);
        assertEquals(0, equal.calls);
        assertEquals(1, different.calls);
    }

    @Test
    void anEqualPartCanPromoteARandomEntryToAnImmediateScheduledTick() {
        TickScheduler.ChunkTickScheduler scheduler = scheduler();
        EqualPart first = new EqualPart("same");
        EqualPart equal = new EqualPart("same");

        scheduler.scheduleTick(first, 100, true);
        scheduler.scheduleTick(equal, 0, false);

        assertFalse(scheduler.processTicks());
        assertEquals(1, first.calls);
        assertEquals(0, equal.calls);
    }

    @Test
    void anEqualPartDoesNotReplaceAnExistingExplicitTick() {
        TickScheduler.ChunkTickScheduler scheduler = scheduler();
        EqualPart first = new EqualPart("same");
        EqualPart equal = new EqualPart("same");

        scheduler.scheduleTick(first, 100, false);
        scheduler.scheduleTick(equal, 0, false);

        assertTrue(scheduler.processTicks());
        assertEquals(0, first.calls);
        assertEquals(0, equal.calls);
    }

    private static TickScheduler.ChunkTickScheduler scheduler() {
        return new TickScheduler.ChunkTickScheduler(new Chunk(null, 0, 0), new TickScheduler.WorldTickScheduler(null));
    }

    private static final class EqualPart extends TMultiPart {

        private final String key;
        int calls;

        EqualPart(String key) {
            this.key = key;
            bind(new TileMultipart());
        }

        @Override
        public String getType() {
            return "test:equal-scheduled";
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualPart && key.equals(((EqualPart) other).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public void scheduledTick() {
            calls++;
        }
    }
}
