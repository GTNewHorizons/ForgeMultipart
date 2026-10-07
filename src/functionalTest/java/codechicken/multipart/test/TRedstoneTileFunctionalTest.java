package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Rotation;
import codechicken.multipart.IMaskedRedstonePart;
import codechicken.multipart.IRedstoneConnector;
import codechicken.multipart.IRedstoneTile;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.PartMap;
import codechicken.multipart.TEdgePart;
import codechicken.multipart.TFacePart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.JavaConversions;

/** Consumer-visible shape and behavior of the generated redstone-tile trait. */
class TRedstoneTileFunctionalTest {

    private static final String TRAIT_NAME = "codechicken.multipart.scalatraits.TRedstoneTile";
    private static final BlockCoord EAST = new BlockCoord(49, 200, 48);

    @Test
    void generatedTraitKeepsItsRuntimeInterfaceAndClassCacheShape() throws Exception {
        TileMultipart first = tile(new MaskedPowerPart(0x1F, 1, 2));
        TileMultipart second = tile(new MaskedPowerPart(0x1F, 3, 4));
        Class<?> trait = Class.forName(TRAIT_NAME);

        assertTrue(trait.isInterface());
        assertArrayEquals(new Class<?>[] { IRedstoneTile.class }, trait.getInterfaces());
        assertTrue(trait.isInstance(first));
        assertNotSame(first, second);
        assertSame(first.getClass(), second.getClass());
        assertEquals(0, trait.getDeclaredFields().length);

        Set<String> signatures = new TreeSet<>();
        for (Method method : trait.getDeclaredMethods()) {
            signatures.add(method.getName() + Type.getMethodDescriptor(method));
        }
        assertEquals(
                new TreeSet<>(
                        Arrays.asList(
                                "canConnectRedstone(I)Z",
                                "getConnectionMask(I)I",
                                "openConnections(I)I",
                                "redstoneConductionE(I)Z",
                                "redstoneConductionF(I)I",
                                "strongPowerLevel(I)I",
                                "weakPowerLevel(I)I",
                                "weakPowerLevel(II)I")),
                signatures);
    }

    @Test
    void conductionAndPowerQueriesPreserveMasksAndMutableSeqInputs() throws Exception {
        int side = 0;
        int blockedEdge = PartMap.edgeBetween(side, Rotation.rotateSide(side & 6, 0));
        int openEdge = PartMap.edgeBetween(side, Rotation.rotateSide(side & 6, 2));
        FaceCoverPart face = new FaceCoverPart(side, 0x15);
        EdgeCoverPart edge = new EdgeCoverPart(blockedEdge, false);
        MaskedPowerPart edgePower = new MaskedPowerPart(0x04, 7, 8);
        MaskedPowerPart centerPower = new MaskedPowerPart(0x10, 13, 12);
        MaskedPowerPart blockedPower = new MaskedPowerPart(0x02, 99, 99);
        PlainPart plain = new PlainPart();
        ArrayList<TMultiPart> parts = new ArrayList<>(
                Arrays.<TMultiPart>asList(face, edge, edgePower, centerPower, blockedPower, plain));
        TileMultipart tile = MultipartHelper.createTileFromParts(parts);
        IRedstoneTile redstone = (IRedstoneTile) tile;
        Class<?> trait = Class.forName(TRAIT_NAME);

        // Scala callers can replace partList with any Seq, not only the immutable List used by normal mutations.
        tile.partList_$eq(JavaConversions.asScalaBuffer(parts));

        assertEquals(0x15, invokeInt(trait, tile, "redstoneConductionF", side));
        assertFalse(invokeBoolean(trait, tile, "redstoneConductionE", blockedEdge));
        assertTrue(invokeBoolean(trait, tile, "redstoneConductionE", openEdge));
        assertEquals(0x14, redstone.openConnections(side));
        assertEquals(0x14, redstone.getConnectionMask(side));
        assertEquals(99, tile.strongPowerLevel(side));
        assertEquals(12, redstone.weakPowerLevel(side, 0x14));
        assertEquals(8, redstone.weakPowerLevel(side, 0x04));
        assertEquals(0, redstone.weakPowerLevel(side, 0x02));

        assertEquals(1, edgePower.strongCalls);
        assertEquals(1, centerPower.strongCalls);
        assertEquals(1, blockedPower.strongCalls);
        assertEquals(2, edgePower.weakCalls);
        assertEquals(1, centerPower.weakCalls);
        assertEquals(0, blockedPower.weakCalls);
    }

    @Test
    void worldFacingQueriesUseTheNeighborMaskAndVanillaSideMapping() {
        World world = MinecraftServer.getServer().worldServers[0];
        world.getChunkFromBlockCoords(EAST.x, EAST.z);
        clear(world, EAST);
        MaskedPowerPart part = new MaskedPowerPart(0x04, 0, 11);
        TileMultipart tile = tile(part);
        MaskConnector connector = new MaskConnector(0x04);

        try {
            tile.setWorldObj(world);
            tile.xCoord = EAST.x - 1;
            tile.yCoord = EAST.y;
            tile.zCoord = EAST.z;
            assertTrue(world.setBlock(EAST.x, EAST.y, EAST.z, Blocks.chest, 0, 3));
            world.setTileEntity(EAST.x, EAST.y, EAST.z, connector);

            assertEquals(11, tile.weakPowerLevel(5));
            assertEquals(4, connector.lastSide);
            assertTrue(tile.canConnectRedstone(1), "Vanilla side 1 maps to multipart side 5");
            assertEquals(4, connector.lastSide);

            connector.mask = 0x08;
            assertEquals(0, tile.weakPowerLevel(5));
            assertFalse(tile.canConnectRedstone(1));
        } finally {
            clear(world, EAST);
        }
    }

    private static TileMultipart tile(TMultiPart... parts) {
        return MultipartHelper.createTileFromParts(Arrays.asList(parts));
    }

    private static int invokeInt(Class<?> trait, TileMultipart tile, String method, int argument) throws Exception {
        return ((Number) trait.getMethod(method, int.class).invoke(tile, argument)).intValue();
    }

    private static boolean invokeBoolean(Class<?> trait, TileMultipart tile, String method, int argument)
            throws Exception {
        return (Boolean) trait.getMethod(method, int.class).invoke(tile, argument);
    }

    private static void clear(World world, BlockCoord pos) {
        world.removeTileEntity(pos.x, pos.y, pos.z);
        world.setBlockToAir(pos.x, pos.y, pos.z);
    }

    private static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "redstone_test:plain";
        }
    }

    private static final class FaceCoverPart extends PlainPart implements TFacePart {

        private final int side;
        private final int conductionMap;

        private FaceCoverPart(int side, int conductionMap) {
            this.side = side;
            this.conductionMap = conductionMap;
        }

        @Override
        public int getSlotMask() {
            return 1 << side;
        }

        @Override
        public int redstoneConductionMap() {
            return conductionMap;
        }
    }

    private static final class EdgeCoverPart extends PlainPart implements TEdgePart {

        private final int slot;
        private final boolean conducts;

        private EdgeCoverPart(int slot, boolean conducts) {
            this.slot = slot;
            this.conducts = conducts;
        }

        @Override
        public int getSlotMask() {
            return 1 << slot;
        }

        @Override
        public boolean conductsRedstone() {
            return conducts;
        }
    }

    private static final class MaskedPowerPart extends PlainPart implements IMaskedRedstonePart {

        private final int mask;
        private final int strongPower;
        private final int weakPower;
        private int strongCalls;
        private int weakCalls;

        private MaskedPowerPart(int mask, int strongPower, int weakPower) {
            this.mask = mask;
            this.strongPower = strongPower;
            this.weakPower = weakPower;
        }

        @Override
        public int getConnectionMask(int side) {
            return mask;
        }

        @Override
        public int strongPowerLevel(int side) {
            strongCalls++;
            return strongPower;
        }

        @Override
        public int weakPowerLevel(int side) {
            weakCalls++;
            return weakPower;
        }

        @Override
        public boolean canConnectRedstone(int side) {
            return true;
        }
    }

    private static final class MaskConnector extends TileEntity implements IRedstoneConnector {

        private int mask;
        private int lastSide = -1;

        private MaskConnector(int mask) {
            this.mask = mask;
        }

        @Override
        public int getConnectionMask(int side) {
            lastSide = side;
            return mask;
        }

        @Override
        public int weakPowerLevel(int side, int mask) {
            throw new AssertionError("Not used by tile output queries");
        }
    }
}
