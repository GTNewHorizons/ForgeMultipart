package codechicken.multipart.handler;

import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MultipartEventHandler {

    private MultipartEventHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tileEntityLoad(ChunkDataEvent.Load event) {
        MultipartEventHandler$.MODULE$.tileEntityLoad(event);
    }

    @SubscribeEvent
    public static void worldUnLoad(WorldEvent.Unload event) {
        MultipartEventHandler$.MODULE$.worldUnLoad(event);
    }

    @SubscribeEvent
    public static void chunkWatch(ChunkWatchEvent.Watch event) {
        MultipartEventHandler$.MODULE$.chunkWatch(event);
    }

    @SubscribeEvent
    public static void chunkUnWatch(ChunkWatchEvent.UnWatch event) {
        MultipartEventHandler$.MODULE$.chunkUnWatch(event);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void drawBlockHighlight(DrawBlockHighlightEvent event) {
        MultipartEventHandler$.MODULE$.drawBlockHighlight(event);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        MultipartEventHandler$.MODULE$.serverTick(event);
    }
}
