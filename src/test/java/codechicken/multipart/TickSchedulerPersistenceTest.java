package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

class TickSchedulerPersistenceTest {

    @Test
    @SuppressWarnings("unchecked")
    void restoredTicksResumeAtTheirDeadlineAndRunOnlyOnce() {
        Chunk chunk = new Chunk(null, 0, 0);
        int[] calls = { 0 };
        TMultiPart part = new TMultiPart() {

            @Override
            public String getType() {
                return "test:restored-tick";
            }

            @Override
            public void scheduledTick() {
                calls[0]++;
            }
        };
        TileMultipart tile = new TileMultipart();
        tile.setPartList(Collections.singletonList(part));
        part.bind(tile);
        chunk.chunkTileEntityMap.put(new ChunkPosition(0, 0, 0), tile);

        TickScheduler.ChunkTickScheduler original = new TickScheduler.ChunkTickScheduler(
                chunk,
                new TickScheduler.WorldTickScheduler(null));
        original.scheduleTick(part, 1, false);
        NBTTagCompound data = new NBTTagCompound();
        original.saveData(data);
        original.unload();

        TickScheduler.WorldTickScheduler world = new TickScheduler.WorldTickScheduler(null);
        TickScheduler.ChunkTickScheduler restored = new TickScheduler.ChunkTickScheduler(chunk, world);
        restored.loadData(data);
        world.preTick();
        world.postTick();
        assertEquals(0, calls[0]);
        world.preTick();
        world.postTick();
        assertEquals(1, calls[0]);
        world.preTick();
        world.postTick();
        assertEquals(1, calls[0]);
    }
}
