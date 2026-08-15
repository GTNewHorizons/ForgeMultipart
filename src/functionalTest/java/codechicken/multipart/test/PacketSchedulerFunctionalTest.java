package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.IScheduledPacketPart;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.PacketScheduler;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/**
 * schedulePacket needs a part bound to a tile in a non-remote world, and sendScheduled needs a real write stream, so
 * none of this runs headless. Tests execute inside the server-started event, so no tick can interleave and flush the
 * map between a schedule and a send.
 */
class PacketSchedulerFunctionalTest {

    @Test
    void repeatedSchedulesAccumulateIntoOneMask() {
        RecordingPart part = boundPart(1);

        PacketScheduler.schedulePacket(part, 0x1L);
        PacketScheduler.schedulePacket(part, 0x2L);
        PacketScheduler.schedulePacket(part, 0x2L);
        sendScheduled();

        assertEquals(Collections.singletonList(0x3L), part.written, "masks are OR-ed, and the part is written once");
    }

    @Test
    void sendingClearsTheScheduleSoTheNextTickWritesNothing() {
        RecordingPart part = boundPart(1);
        PacketScheduler.schedulePacket(part, 0x5L);

        sendScheduled();
        sendScheduled();

        assertEquals(Collections.singletonList(0x5L), part.written);
    }

    @Test
    void aPartThatLostItsTileIsSkippedButStillCleared() {
        RecordingPart part = boundPart(1);
        PacketScheduler.schedulePacket(part, 0x7L);
        part.tile_$eq(null);

        sendScheduled();

        assertTrue(part.written.isEmpty(), "a part with no tile must be skipped");
    }

    @Test
    void everyDocumentedMaskWidthIsAccepted() {
        for (int width : new int[] { 1, 2, 4, 8 }) {
            RecordingPart part = boundPart(width);
            PacketScheduler.schedulePacket(part, 0x1L);

            sendScheduled();

            assertEquals(Collections.singletonList(0x1L), part.written, "maskWidth " + width + " must be accepted");
        }
    }

    /** The reference matches on 1, 2, 4 and 8 with no fallback, so anything else fails rather than writing nothing. */
    @Test
    void anUndocumentedMaskWidthIsRejected() {
        RecordingPart part = boundPart(3);
        PacketScheduler.schedulePacket(part, 0x1L);

        assertThrows(RuntimeException.class, PacketSchedulerFunctionalTest::sendScheduled);

        // Leave no scheduled entry behind for the tests that follow.
        part.tile_$eq(null);
        try {
            sendScheduled();
        } catch (RuntimeException ignored) {
            // already drained
        }
    }

    // The only place sendScheduled's owner is named.
    private static void sendScheduled() {
        PacketScheduler.sendScheduled();
    }

    private static RecordingPart boundPart(int maskWidth) {
        RecordingPart part = new RecordingPart(maskWidth);
        TileMultipart tile = MultipartHelper.createTileFromParts(Collections.<TMultiPart>singletonList(part));
        assertNotNull(tile);
        World world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        tile.setWorldObj(world);
        tile.xCoord = 1100;
        tile.yCoord = 70;
        tile.zCoord = 1100;
        return part;
    }

    private static final class RecordingPart extends TMultiPart implements IScheduledPacketPart {

        private final int maskWidth;
        final List<Long> written = new ArrayList<>();

        RecordingPart(int maskWidth) {
            this.maskWidth = maskWidth;
        }

        @Override
        public String getType() {
            return "test:scheduled";
        }

        @Override
        public int maskWidth() {
            return maskWidth;
        }

        @Override
        public void writeScheduled(long mask, MCDataOutput packet) {
            written.add(mask);
        }

        @Override
        public void readScheduled(long mask, MCDataInput packet) {}
    }
}
