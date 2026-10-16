package codechicken.multipart;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.lib.world.ChunkExtension;
import codechicken.lib.world.WorldExtension;
import codechicken.lib.world.WorldExtensionInstantiator;

/**
 * Scala companion singleton. This one is not just a forwarder: WorldExtensionManager registers the instance itself, so
 * the instantiator hooks have to live here.
 */
public final class TickScheduler$ extends WorldExtensionInstantiator {

    public static final TickScheduler$ MODULE$ = new TickScheduler$();

    private TickScheduler$() {}

    @Override
    public WorldExtension createWorldExtension(World world) {
        return TickScheduler.createWorldExtension(world);
    }

    @Override
    public ChunkExtension createChunkExtension(Chunk chunk, WorldExtension world) {
        return TickScheduler.createChunkExtension(chunk, world);
    }

    public void loadRandomTick(TRandomUpdateTick part) {
        TickScheduler.loadRandomTick(part);
    }

    public void scheduleTick(TMultiPart part, int ticks) {
        TickScheduler.scheduleTick(part, ticks);
    }

    public long getSchedulerTime(World world) {
        return TickScheduler.getSchedulerTime(world);
    }
}
