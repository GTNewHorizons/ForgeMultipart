package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

class TickSchedulerPersistenceRegressionTest {

    @Test
    void savedTicksResumeAfterChunkReloadAndFireOnlyOnce() {
        TickScheduler.WorldTickScheduler world = new TickScheduler.WorldTickScheduler(null);
        Chunk chunk = new Chunk(null, 2, -3);
        TileMultipart tile = new TileMultipart();
        tile.xCoord = 35;
        tile.yCoord = 64;
        tile.zCoord = -44;
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
        tile.setPartList(Collections.singletonList(part));
        part.bind(tile);
        chunk.chunkTileEntityMap.put(new ChunkPosition(3, 64, 4), tile);
        TickScheduler.ChunkTickScheduler scheduler = new TickScheduler.ChunkTickScheduler(chunk, world);
        scheduler.scheduleTick(part, 2, false);
        NBTTagCompound saved = new NBTTagCompound();
        scheduler.saveData(saved);
        assertEquals(1, saved.getTagList("multipartTicks", 10).tagCount());
        scheduler.unload();
        world = new TickScheduler.WorldTickScheduler(null);
        TickScheduler.ChunkTickScheduler restored = new TickScheduler.ChunkTickScheduler(chunk, world);
        restored.loadData(saved);
        restored.scheduleTick(part, 0, false);
        for (int tick = 0; tick < 4; tick++) {
            world.preTick();
            world.postTick();
            assertEquals(tick < 2 ? 0 : 1, calls[0]);
        }
    }

}
