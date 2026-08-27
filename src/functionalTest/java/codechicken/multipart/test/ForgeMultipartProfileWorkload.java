package codechicken.multipart.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import codechicken.multipart.IRedstonePart;
import codechicken.multipart.IRedstoneTile;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/** Fixed workload used only by the opt-in JFR baseline run. */
final class ForgeMultipartProfileWorkload {

    private static final int PART_COUNT = 8;
    private static final int WARMUP_ITERATIONS = 1_000_000;
    private static final int PROFILE_ITERATIONS = 50_000_000;
    private static volatile long sink;

    private ForgeMultipartProfileWorkload() {}

    static void run() {
        List<TMultiPart> tickingParts = new ArrayList<>();
        for (int i = 0; i < PART_COUNT; i++) {
            tickingParts.add(new ProfilePart());
        }
        TileMultipart tickingTile = MultipartHelper.createTileFromParts(tickingParts);

        List<TMultiPart> redstoneParts = new ArrayList<>();
        for (int i = 0; i < PART_COUNT; i++) {
            redstoneParts.add(new RedstoneProfilePart(i));
        }
        TileMultipart redstoneTile = MultipartHelper.createTileFromParts(redstoneParts);
        IRedstoneTile redstone = (IRedstoneTile) redstoneTile;
        CountingFunction operation = new CountingFunction();
        com.sun.management.ThreadMXBean allocationBean = allocationBean();
        long threadId = Thread.currentThread().getId();

        profileUpdateEntity(tickingTile, WARMUP_ITERATIONS);
        profileOperate(tickingTile, operation, WARMUP_ITERATIONS);
        profileRedstone(redstoneTile, redstone, WARMUP_ITERATIONS);

        long updateStartMillis = System.currentTimeMillis();
        long updateStartBytes = allocationBean.getThreadAllocatedBytes(threadId);
        long updateStart = System.nanoTime();
        profileUpdateEntity(tickingTile, PROFILE_ITERATIONS);
        long updateNanos = System.nanoTime() - updateStart;
        long updateBytes = allocationBean.getThreadAllocatedBytes(threadId) - updateStartBytes;

        long operateStartMillis = System.currentTimeMillis();
        long operateStartBytes = allocationBean.getThreadAllocatedBytes(threadId);
        long operateStart = System.nanoTime();
        profileOperate(tickingTile, operation, PROFILE_ITERATIONS);
        long operateNanos = System.nanoTime() - operateStart;
        long operateBytes = allocationBean.getThreadAllocatedBytes(threadId) - operateStartBytes;

        long redstoneStartMillis = System.currentTimeMillis();
        long redstoneStartBytes = allocationBean.getThreadAllocatedBytes(threadId);
        long redstoneStart = System.nanoTime();
        long redstoneChecksum = profileRedstone(redstoneTile, redstone, PROFILE_ITERATIONS);
        long redstoneNanos = System.nanoTime() - redstoneStart;
        long redstoneBytes = allocationBean.getThreadAllocatedBytes(threadId) - redstoneStartBytes;

        long updateChecksum = 0;
        for (TMultiPart part : tickingParts) {
            updateChecksum += ((ProfilePart) part).updates;
        }
        sink = updateChecksum + operation.applications + redstoneChecksum;
        writeResults(
                updateStartMillis,
                updateNanos,
                updateBytes,
                operateStartMillis,
                operateNanos,
                operateBytes,
                redstoneStartMillis,
                redstoneNanos,
                redstoneBytes,
                sink);
    }

    private static void profileUpdateEntity(TileMultipart tile, int iterations) {
        for (int i = 0; i < iterations; i++) {
            tile.updateEntity();
        }
    }

    private static void profileOperate(TileMultipart tile, CountingFunction operation, int iterations) {
        for (int i = 0; i < iterations; i++) {
            tile.operate(operation);
        }
    }

    private static long profileRedstone(TileMultipart tile, IRedstoneTile redstone, int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            int side = i % 6;
            checksum += tile.strongPowerLevel(side);
            checksum += redstone.getConnectionMask(side);
            checksum += redstone.weakPowerLevel(side, 0x1F);
        }
        return checksum;
    }

    private static void writeResults(long updateStartMillis, long updateNanos, long updateBytes,
            long operateStartMillis, long operateNanos, long operateBytes, long redstoneStartMillis, long redstoneNanos,
            long redstoneBytes, long checksum) {
        File output = new File("forgemultipart-profile.txt");
        try (PrintWriter writer = new PrintWriter(output)) {
            writer.printf(Locale.ROOT, "java=%s%n", System.getProperty("java.runtime.version"));
            writer.printf(Locale.ROOT, "parts=%d%n", PART_COUNT);
            writer.printf(Locale.ROOT, "warmupIterations=%d%n", WARMUP_ITERATIONS);
            writer.printf(Locale.ROOT, "profileIterations=%d%n", PROFILE_ITERATIONS);
            writeTiming(writer, "updateEntity", updateStartMillis, updateNanos, updateBytes);
            writeTiming(writer, "operate", operateStartMillis, operateNanos, operateBytes);
            writeTiming(writer, "redstoneQueries", redstoneStartMillis, redstoneNanos, redstoneBytes);
            writer.printf(Locale.ROOT, "checksum=%d%n", checksum);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not write ForgeMultipart profile results", e);
        }
    }

    private static void writeTiming(PrintWriter writer, String name, long startMillis, long nanos,
            long allocatedBytes) {
        double seconds = nanos / 1_000_000_000d;
        writer.printf(Locale.ROOT, "%s.startEpochMillis=%d%n", name, startMillis);
        writer.printf(Locale.ROOT, "%s.nanos=%d%n", name, nanos);
        writer.printf(Locale.ROOT, "%s.operationsPerSecond=%.0f%n", name, PROFILE_ITERATIONS / seconds);
        writer.printf(Locale.ROOT, "%s.allocatedBytes=%d%n", name, allocatedBytes);
        writer.printf(
                Locale.ROOT,
                "%s.allocatedBytesPerOperation=%.1f%n",
                name,
                allocatedBytes / (double) PROFILE_ITERATIONS);
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean)) {
            throw new IllegalStateException("The profiling JVM does not expose thread allocation counters");
        }
        com.sun.management.ThreadMXBean allocationBean = (com.sun.management.ThreadMXBean) bean;
        if (!allocationBean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("The profiling JVM does not support thread allocation counters");
        }
        if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        }
        return allocationBean;
    }

    private static class ProfilePart extends TMultiPart {

        private int updates;

        @Override
        public String getType() {
            return "profile_part";
        }

        @Override
        public boolean doesTick() {
            return true;
        }

        @Override
        public void update() {
            updates++;
        }
    }

    private static final class RedstoneProfilePart extends ProfilePart implements IRedstonePart {

        private final int power;

        private RedstoneProfilePart(int power) {
            this.power = power;
        }

        @Override
        public int strongPowerLevel(int side) {
            return power + side;
        }

        @Override
        public int weakPowerLevel(int side) {
            return power + side;
        }

        @Override
        public boolean canConnectRedstone(int side) {
            return true;
        }
    }

    private static final class CountingFunction extends AbstractFunction1<TMultiPart, BoxedUnit> {

        private long applications;

        @Override
        public BoxedUnit apply(TMultiPart part) {
            applications++;
            return BoxedUnit.UNIT;
        }
    }
}
