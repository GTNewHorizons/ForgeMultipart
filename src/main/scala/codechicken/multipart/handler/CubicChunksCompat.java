package codechicken.multipart.handler;

import net.minecraftforge.common.MinecraftForge;

import com.cardinalstar.cubicchunks.api.event.CubeEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class CubicChunksCompat {

    private static final CubicChunksCompat INSTANCE = new CubicChunksCompat();

    private CubicChunksCompat() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTileEntitiesLoad(CubeEvent.DataLoad event) {
        MultipartSaveLoad.loadTiles(event.world, event.cube.cubeTileEntityMap);
    }
}
