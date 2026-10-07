package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import net.minecraft.block.Block;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.data.MCDataOutputWrapper;

class TMultiPartTileAccessorRegressionTest {

    @Test
    void bindingUsesTheOverriddenSetterIncludingUnbinding() {
        VirtualPart part = new VirtualPart();
        RecordingTile tile = new RecordingTile();

        part.bind(tile);
        assertSame(tile, part.current);
        assertEquals(1, part.binds);
        part.bind(null);
        assertNull(part.current);
        assertEquals(2, part.binds);
    }

    @Test
    void convenienceMethodsUseTheTileSuppliedByTheOverride() {
        VirtualPart part = new VirtualPart();
        RecordingTile tile = new RecordingTile();
        part.current = tile;
        tile.xCoord = 42;
        tile.yCoord = 73;
        tile.zCoord = -4;

        assertSame(tile, part.getTile());
        assertEquals(42, part.x());
        assertEquals(73, part.y());
        assertEquals(-4, part.z());
        assertNull(part.world());
        assertEquals(1, tile.worldReads);
        assertSame(tile.stream, part.getWriteStream());
        assertSame(part, tile.streamPart);
        assertNull(part.collisionRayTrace(Vec3.createVectorHelper(0, 0, 0), Vec3.createVectorHelper(1, 1, 1)));
        assertEquals(1, tile.blockReads);
    }

    @Test
    void descriptionUpdatesFollowTheTilePublishedByReadDescAndSkipAnUnboundPart() {
        VirtualPart part = new VirtualPart();
        RecordingTile original = new RecordingTile();
        RecordingTile replacement = new RecordingTile();
        part.current = original;
        part.onRead = () -> part.current = replacement;

        part.read(null);
        assertEquals(0, original.renderUpdates);
        assertEquals(1, replacement.renderUpdates);
        part.sendDescUpdate();
        assertEquals(0, original.bytes.size());
        assertArrayEquals(new byte[] { 7 }, replacement.bytes.toByteArray());

        part.current = null;
        assertNull(part.world());
        part.sendDescUpdate();
        assertEquals(1, part.descriptionsWritten);
    }

    private static final class VirtualPart extends TMultiPart {

        TileMultipart current;
        int binds;
        int descriptionsWritten;
        Runnable onRead;

        @Override
        public String getType() {
            return "test:virtual-tile";
        }

        @Override
        public TileMultipart tile() {
            return current;
        }

        @Override
        public void tile_$eq(TileMultipart value) {
            binds++;
            current = value;
        }

        @Override
        public void readDesc(MCDataInput packet) {
            onRead.run();
        }

        @Override
        public void writeDesc(MCDataOutput packet) {
            descriptionsWritten++;
            packet.writeByte(7);
        }
    }

    private static final class RecordingTile extends TileMultipart {

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final MCDataOutput stream = new MCDataOutputWrapper(new DataOutputStream(bytes));
        TMultiPart streamPart;
        int worldReads;
        int blockReads;
        int renderUpdates;

        @Override
        public World getWorldObj() {
            worldReads++;
            return null;
        }

        @Override
        public Block getBlockType() {
            blockReads++;
            return null;
        }

        @Override
        public MCDataOutput getWriteStream(TMultiPart part) {
            streamPart = part;
            return stream;
        }

        @Override
        public void markRender() {
            renderUpdates++;
        }
    }
}
