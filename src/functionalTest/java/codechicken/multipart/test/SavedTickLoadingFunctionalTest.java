package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

import codechicken.lib.world.ChunkExtension;
import codechicken.multipart.TickScheduler;
import codechicken.multipart.TileMultipart;

class SavedTickLoadingFunctionalTest {

    @Test
    @SuppressWarnings("unchecked")
    void missingPartsKeepSurvivingTicksAtTheirOriginalIndices() throws Exception {
        NBTTagList parts = new NBTTagList();
        for (String type : new String[] { "test:missing-part", "mc_button", "mc_button" }) {
            NBTTagCompound part = new NBTTagCompound();
            part.setString("id", type);
            part.setByte("meta", (byte) 1);
            parts.appendTag(part);
        }
        NBTTagCompound savedTile = new NBTTagCompound();
        savedTile.setTag("parts", parts);
        TileMultipart[] loaded = new TileMultipart[1];
        ExpectedErrorLog.silence(() -> loaded[0] = TileMultipart.createFromNBT(savedTile));
        TileMultipart tile = loaded[0];
        assertEquals(2, tile.jPartList().size());

        Chunk chunk = new Chunk(null, 0, 0);
        chunk.chunkTileEntityMap.put(new ChunkPosition(0, 0, 0), tile);
        NBTTagList ticks = new NBTTagList();
        for (int index : new int[] { 0, 1, 2, 99 }) {
            NBTTagCompound tick = new NBTTagCompound();
            tick.setByte("i", (byte) index);
            tick.setLong("time", index + 10);
            ticks.appendTag(tick);
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("multipartTicks", ticks);
        ChunkExtension scheduler = TickScheduler.createChunkExtension(chunk, TickScheduler.createWorldExtension(null));
        scheduler.loadData(data);

        Field tickList = scheduler.getClass().getDeclaredField("tickList");
        tickList.setAccessible(true);
        List<TickScheduler.PartTickEntry> restored = (List<TickScheduler.PartTickEntry>) tickList.get(scheduler);
        assertEquals(2, restored.size());
        assertSame(tile.jPartList().get(0), restored.get(0).part());
        assertEquals(11, restored.get(0).time());
        assertSame(tile.jPartList().get(1), restored.get(1).part());
        assertEquals(12, restored.get(1).time());

        tile.onChunkLoad();
        NBTTagCompound resaved = new NBTTagCompound();
        scheduler.saveData(resaved);
        scheduler.loadData(resaved);
        restored = (List<TickScheduler.PartTickEntry>) tickList.get(scheduler);
        assertEquals(2, restored.size());
        assertSame(tile.jPartList().get(0), restored.get(0).part());
        assertSame(tile.jPartList().get(1), restored.get(1).part());
    }
}
