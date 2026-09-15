package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import codechicken.multipart.IRedstonePart;
import codechicken.multipart.IRedstoneTile;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.TFacePart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.examples.TileTraitAccessExample;

class TileTraitAccessFunctionalTest {

    @Test
    void javaExampleUsesGeneratedCapabilityAndHandlesAbsentCapability() {
        TileMultipart tile = MultipartHelper.createTileFromParts(Arrays.asList(new PowerPart(), new CoverPart()));
        for (int mask = 0; mask < 32; mask++) {
            assertEquals((mask & 0x15) != 0, TileTraitAccessExample.hasOpenConnection(tile, 0, mask));
        }
        TileMultipart noRedstone = MultipartHelper.createTileFromParts(Arrays.asList(new CoverPart()));
        assertFalse(TileTraitAccessExample.hasOpenConnection(noRedstone, 0, 0x1F));
        assertFalse(TileTraitAccessExample.hasOpenConnection(null, 0, 0x1F));
    }

    @Test
    void rawClassInvocationFailsWhileTheStableRedstoneInterfaceDispatches() throws Exception {
        TileMultipart tile = MultipartHelper.createTileFromParts(Arrays.asList(new PowerPart(), new CoverPart()));
        assertTrue(Class.forName("codechicken.multipart.scalatraits.TRedstoneTile").isInterface());
        assertThrows(IncompatibleClassChangeError.class, () -> RawTileTraitCalls.openConnections(tile, 0));
        IRedstoneTile redstone = (IRedstoneTile) tile;
        assertEquals(0x15, redstone.openConnections(0));
        assertEquals(0x15, redstone.getConnectionMask(0));
        assertEquals(9, redstone.weakPowerLevel(0, 0x10));
        assertEquals(0, redstone.weakPowerLevel(0, 0x02));
        assertEquals(12, tile.strongPowerLevel(0));
    }

    @Test
    void rawFieldAccessFailsWhileTheStableBaseDispatchesSlotReads() throws Exception {
        CoverPart part = new CoverPart();
        TileMultipart tile = MultipartHelper.createTileFromParts(Arrays.asList(part));
        assertTrue(Class.forName("codechicken.multipart.scalatraits.TSlottedTile").isInterface());
        assertThrows(NoSuchFieldError.class, () -> RawTileTraitCalls.slots(tile));
        assertSame(part, tile.partMap(0));
        assertSame(part, tile.jPartList().get(0));
        assertSame(tile, part.tile());
    }

    private static final class CoverPart extends TMultiPart implements TFacePart {

        @Override
        public String getType() {
            return "tile_access:cover";
        }

        @Override
        public int getSlotMask() {
            return 1;
        }

        @Override
        public int redstoneConductionMap() {
            return 0x15;
        }
    }

    private static final class PowerPart extends TMultiPart implements IRedstonePart {

        @Override
        public String getType() {
            return "tile_access:power";
        }

        @Override
        public int strongPowerLevel(int side) {
            return 12;
        }

        @Override
        public int weakPowerLevel(int side) {
            return 9;
        }

        @Override
        public boolean canConnectRedstone(int side) {
            return true;
        }
    }
}
