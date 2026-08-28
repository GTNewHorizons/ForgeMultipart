package codechicken.multipart;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.common.DimensionManager;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.world.ChunkExtension;
import codechicken.lib.world.WorldExtension;
import codechicken.multipart.handler.MultipartProxy;

/**
 * Used for scheduling delayed callbacks to parts. Do not use this for redstone applications that require precise
 * timing. If 2 parts are both scheduled for an update on the same tick, there is no guarantee which one will update
 * first. These parts should not depend on a state of another part that may have changed before/after them.
 */
public final class TickScheduler {

    private TickScheduler() {}

    public static class PartTickEntry {

        private final TMultiPart part;
        private long time;
        private boolean random;

        public PartTickEntry(TMultiPart part, long time, boolean random) {
            this.part = part;
            this.time = time;
            this.random = random;
        }

        public PartTickEntry(TMultiPart part, int ticks) {
            this(part, ticks, false);
        }

        public TMultiPart part() {
            return part;
        }

        public long time() {
            return time;
        }

        public void time_$eq(long time) {
            this.time = time;
        }

        public boolean random() {
            return random;
        }

        public void random_$eq(boolean random) {
            this.random = random;
        }
    }

    static class WorldTickScheduler extends WorldExtension {

        private long schedTime = 0L;
        private Set<ChunkTickScheduler> tickChunks = new HashSet<>();
        private boolean processing = false;
        private final List<PartTickEntry> pending = new ArrayList<>();

        WorldTickScheduler(World world) {
            super(world);
        }

        void scheduleTick(TMultiPart part, int ticks, boolean random) {
            if (processing) {
                pending.add(new PartTickEntry(part, schedTime + ticks, random));
            } else {
                doScheduleTick(part, schedTime + ticks, random);
            }
        }

        void doScheduleTick(TMultiPart part, long time, boolean random) {
            if (part.tile() != null) {
                ((ChunkTickScheduler) getChunkExtension(part.tile().xCoord >> 4, part.tile().zCoord >> 4))
                        .scheduleTick(part, time, random);
            }
        }

        void loadRandom(TRandomUpdateTick part) {
            scheduleTick((TMultiPart) part, nextRandomTick(), true);
        }

        @Override
        public void preTick() {
            processing = true;
        }

        @Override
        public void postTick() {
            if (!tickChunks.isEmpty()) {
                Set<ChunkTickScheduler> remaining = new HashSet<>();
                for (ChunkTickScheduler chunk : tickChunks) {
                    if (chunk.processTicks()) {
                        remaining.add(chunk);
                    }
                }
                tickChunks = remaining;
            }

            processing = false;
            for (PartTickEntry e : pending) {
                doScheduleTick(e.part(), e.time(), e.random());
            }
            pending.clear();

            schedTime += 1;
        }

        File saveDir() {
            // Calling DimensionManager.getCurrentSaveRootDirectory too early breaks game saves, we have a world
            // reference, use it
            if (world.provider.dimensionId == 0) {
                return ((SaveHandler) world.getSaveHandler()).getWorldDirectory();
            }

            return new File(DimensionManager.getCurrentSaveRootDirectory(), world.provider.getSaveFolder());
        }

        File saveFile() {
            return new File(saveDir(), "multipart.dat");
        }

        @Override
        public void load() {
            try {
                FileInputStream in = new FileInputStream(saveFile());
                loadTag(CompressedStreamTools.readCompressed(in));
                in.close();
            } catch (Exception e) {
                // Matches the reference, which swallows any failure to read the saved schedule.
            }

            loadTag(new NBTTagCompound());
        }

        void loadTag(NBTTagCompound tag) {
            if (tag.hasKey("schedTime")) {
                schedTime = tag.getLong("schedTime");
            } else {
                schedTime = world.getTotalWorldTime();
            }
        }

        NBTTagCompound saveTag() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("schedTime", schedTime);
            return tag;
        }

        @Override
        public void save() {
            try {
                File file = saveFile();
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }

                DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));
                CompressedStreamTools.writeCompressed(saveTag(), dout);
                dout.close();
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        int nextRandomTick() {
            return world.rand.nextInt(800) + 800;
        }
    }

    static class ChunkTickScheduler extends ChunkExtension {

        private List<PartTickEntry> tickList = new ArrayList<>();

        ChunkTickScheduler(Chunk chunk, WorldTickScheduler world) {
            super(chunk, world);
        }

        private WorldTickScheduler scheduler() {
            return (WorldTickScheduler) world;
        }

        long schedTime() {
            return scheduler().schedTime;
        }

        void scheduleTick(TMultiPart part, long time, boolean random) {
            for (PartTickEntry e : tickList) {
                if (e.part() == part) {
                    // only override an existing tick if we're going from random to scheduled
                    if (e.random() && !random) {
                        e.time_$eq(time);
                        e.random_$eq(random);
                    }
                    return;
                }
            }
            tickList.add(new PartTickEntry(part, time, random));
            if (tickList.size() == 1) {
                scheduler().tickChunks.add(this);
            }
        }

        int nextRandomTick() {
            return scheduler().nextRandomTick();
        }

        boolean processTicks() {
            List<PartTickEntry> kept = new ArrayList<>();
            for (PartTickEntry e : tickList) {
                if (processTick(e)) {
                    kept.add(e);
                }
            }
            tickList = kept;
            return !tickList.isEmpty();
        }

        boolean processTick(PartTickEntry e) {
            if (e.time() <= schedTime()) {
                if (e.part().tile() != null) {
                    if (e.random() && e.part() instanceof TRandomUpdateTick) {
                        ((TRandomUpdateTick) e.part()).randomUpdate();
                    } else {
                        e.part().scheduledTick();
                    }

                    if (e.part() instanceof TRandomUpdateTick) {
                        e.time_$eq(schedTime() + nextRandomTick());
                        e.random_$eq(true);
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        @Override
        public void saveData(NBTTagCompound data) {
            NBTTagList tagList = new NBTTagList();
            for (PartTickEntry e : tickList) {
                TMultiPart part = e.part();
                if (part.tile() != null && !e.random()) {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setShort("pos", (short) MultipartProxy.indexInChunk(new BlockCoord(part.tile())));
                    tag.setByte("i", (byte) part.tile().partList().indexOf(part));
                    tag.setLong("time", e.time());
                    tagList.appendTag(tag);
                }
            }
            if (tagList.tagCount() > 0) {
                data.setTag("multipartTicks", tagList);
            }
        }

        @Override
        public void loadData(NBTTagCompound data) {
            tickList.clear();
            if (!data.hasKey("multipartTicks")) {
                return;
            }

            NBTTagList tagList = data.getTagList("multipartTicks", 10);
            ChunkCoordIntPair cc = new ChunkCoordIntPair(0, 0);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound tag = tagList.getCompoundTagAt(i);
                BlockCoord pos = MultipartProxy.indexInChunk(cc, tag.getShort("pos"));
                Object tile = chunk.chunkTileEntityMap.get(new ChunkPosition(pos.x, pos.y, pos.z));
                if (tile instanceof TileMultipart) {
                    tickList.add(
                            new PartTickEntry(
                                    ((TileMultipart) tile).partList().apply(tag.getByte("i")),
                                    tag.getLong("time"),
                                    false));
                }
            }
        }

        @Override
        public void unload() {
            if (!tickList.isEmpty()) {
                scheduler().tickChunks.remove(this);
            }
        }
    }

    public static WorldExtension createWorldExtension(World world) {
        return new WorldTickScheduler(world);
    }

    public static ChunkExtension createChunkExtension(Chunk chunk, WorldExtension world) {
        return new ChunkTickScheduler(chunk, (WorldTickScheduler) world);
    }

    public static WorldExtension getExtension(World world) {
        return TickScheduler$.MODULE$.getExtension(world);
    }

    /** Start random ticking for a part. Should be called from TMultiPart.onWorldJoin. */
    public static void loadRandomTick(TRandomUpdateTick part) {
        TMultiPart multiPart = (TMultiPart) part;
        ((WorldTickScheduler) getExtension(multiPart.tile().getWorldObj())).loadRandom(part);
    }

    /** Schedule a tick for part relative to the current time. */
    public static void scheduleTick(TMultiPart part, int ticks) {
        ((WorldTickScheduler) getExtension(part.tile().getWorldObj())).scheduleTick(part, ticks, false);
    }

    /**
     * Returns the current scheduler time. Like the world time, but unaffected by the time set command and other things
     * changing time of day.
     *
     * @deprecated in favor of world.getTotalWorldTime
     */
    @Deprecated
    public static long getSchedulerTime(World world) {
        return ((WorldTickScheduler) getExtension(world)).schedTime;
    }
}
