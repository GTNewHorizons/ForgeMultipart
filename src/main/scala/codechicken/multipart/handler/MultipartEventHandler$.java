package codechicken.multipart.handler;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import codechicken.multipart.BlockMultipart;
import codechicken.multipart.TileCache;
import codechicken.multipart.TileMultipart;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.collection.JavaConverters;

public final class MultipartEventHandler$ {

    public static final MultipartEventHandler$ MODULE$ = new MultipartEventHandler$();

    private MultipartEventHandler$() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tileEntityLoad(ChunkDataEvent.Load event) {
        MultipartSaveLoad.loadTiles(event.getChunk());
    }

    @SubscribeEvent
    public void worldUnLoad(WorldEvent.Unload event) {
        MultipartSPH$.MODULE$.onWorldUnload(event.world);
        if (event.world.isRemote) {
            TileCache.clear();
        } else {
            MultipartSaveLoad.loadingWorld_$eq(null);
        }
    }

    @SubscribeEvent
    public void chunkWatch(ChunkWatchEvent.Watch event) {
        MultipartSPH$.MODULE$.onChunkWatch(event.player, event.chunk);
    }

    @SubscribeEvent
    public void chunkUnWatch(ChunkWatchEvent.UnWatch event) {
        MultipartSPH$.MODULE$.onChunkUnWatch(event.player, event.chunk);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void drawBlockHighlight(DrawBlockHighlightEvent event) {
        if (event.target != null && event.target.typeOfHit == MovingObjectType.BLOCK
                && event.player.worldObj.getTileEntity(
                        event.target.blockX,
                        event.target.blockY,
                        event.target.blockZ) instanceof TileMultipart
                && BlockMultipart
                        .drawHighlight(event.player.worldObj, event.player, event.target, event.partialTicks)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
            MultipartSPH$.MODULE$.onTickEnd(JavaConverters.asScalaBufferConverter(players).asScala());
        }
    }
}
