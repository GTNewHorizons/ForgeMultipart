package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.handler.MultipartEventHandler;
import codechicken.multipart.handler.MultipartEventHandler$;
import codechicken.multipart.handler.MultipartSPH$;
import codechicken.multipart.handler.MultipartSaveLoad;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.TickEvent;

class MultipartEventHandlerFunctionalTest {

    @Test
    void bothEventBusesUseTheCompanionIdentity() throws Exception {
        assertSame(MultipartEventHandler$.MODULE$, registeredCompanion(FMLCommonHandler.instance().bus()));
        assertSame(MultipartEventHandler$.MODULE$, registeredCompanion(MinecraftForge.EVENT_BUS));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void chunkLoadAndWorldUnloadMaintainTransientServerState() {
        WorldServer world = world();
        World originalLoadingWorld = MultipartSaveLoad.loadingWorld();
        scala.collection.mutable.Map updates = MultipartSPH$.MODULE$
                .codechicken$multipart$handler$MultipartSPH$$updateMap();
        try {
            MultipartSaveLoad.loadingWorld_$eq(null);
            Chunk chunk = new Chunk(world, 75, 75);

            MultipartEventHandler.tileEntityLoad(new ChunkDataEvent.Load(chunk, new NBTTagCompound()));

            assertSame(world, MultipartSaveLoad.loadingWorld());
            MultipartSPH$.MODULE$.getTileStream(world, new BlockCoord(1200, 70, 1200));
            assertTrue(updates.contains(world));

            MultipartEventHandler.worldUnLoad(new WorldEvent.Unload(world));

            assertNull(MultipartSaveLoad.loadingWorld());
            assertFalse(updates.contains(world));
        } finally {
            updates.remove(world);
            MultipartSaveLoad.loadingWorld_$eq(originalLoadingWorld);
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void watchChangesAreAppliedOnlyAtTheEndOfTheServerTick() {
        WorldServer world = world();
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        assertNotNull(player);
        player.setEntityId(-123456789);
        Integer playerId = player.getEntityId();
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(0, 0);
        scala.collection.mutable.Map newWatchers = MultipartSPH$.MODULE$
                .codechicken$multipart$handler$MultipartSPH$$newWatchers();
        scala.collection.mutable.HashMap chunkWatchers = MultipartSPH$.MODULE$
                .codechicken$multipart$handler$MultipartSPH$$chunkWatchers();
        List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;

        newWatchers.remove(playerId);
        chunkWatchers.remove(playerId);
        players.add(player);
        try {
            MultipartEventHandler.chunkWatch(new ChunkWatchEvent.Watch(chunk, player));
            assertTrue(newWatchers.contains(playerId));

            MultipartEventHandler.serverTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
            assertTrue(newWatchers.contains(playerId));
            assertFalse(chunkWatchers.contains(playerId));

            MultipartEventHandler.serverTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            assertFalse(newWatchers.contains(playerId));
            assertTrue(((scala.collection.Set) chunkWatchers.apply(playerId)).contains(chunk));

            MultipartEventHandler.chunkUnWatch(new ChunkWatchEvent.UnWatch(chunk, player));
            assertFalse(chunkWatchers.contains(playerId));
        } finally {
            players.remove(player);
            newWatchers.remove(playerId);
            chunkWatchers.remove(playerId);
        }
    }

    private static Object registeredCompanion(EventBus bus) throws Exception {
        Field listenersField = EventBus.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        Map<?, ?> listeners = (Map<?, ?>) listenersField.get(bus);
        for (Object target : listeners.keySet()) {
            if (target == MultipartEventHandler$.MODULE$) {
                return target;
            }
        }
        return null;
    }

    private static WorldServer world() {
        WorldServer world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        assertFalse(world.isRemote);
        return world;
    }
}
