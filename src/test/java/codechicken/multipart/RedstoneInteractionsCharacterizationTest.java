package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Rotation;

/** Pure redstone masks, routing precedence, and the Scala-facing ABI shared by several downstream mods. */
class RedstoneInteractionsCharacterizationTest {

    @Test
    void theSixInterfacesKeepTheirExactAbstractShape() {
        assertInterface(
                IRedstonePart.class,
                new Class<?>[0],
                "canConnectRedstone(int)boolean",
                "strongPowerLevel(int)int",
                "weakPowerLevel(int)int");
        assertInterface(IFaceRedstonePart.class, new Class<?>[] { IRedstonePart.class }, "getFace()int");
        assertInterface(IMaskedRedstonePart.class, new Class<?>[] { IRedstonePart.class }, "getConnectionMask(int)int");
        assertInterface(
                IRedstoneConnector.class,
                new Class<?>[0],
                "getConnectionMask(int)int",
                "weakPowerLevel(int,int)int");
        assertInterface(IRedstoneTile.class, new Class<?>[] { IRedstoneConnector.class }, "openConnections(int)int");
        assertInterface(
                IRedstoneConnectorBlock.class,
                new Class<?>[0],
                "getConnectionMask(net.minecraft.world.IBlockAccess,int,int,int,int)int",
                "weakPowerLevel(net.minecraft.world.IBlockAccess,int,int,int,int,int)int");
    }

    @Test
    void theStaticFacadeAndCompanionKeepTheirPublishedEntryPoints() throws Exception {
        Set<String> expected = new TreeSet<>(
                Arrays.asList(
                        "connectionMask(codechicken.multipart.TMultiPart,int)int",
                        "fullVanillaBlocks()scala.collection.immutable.Set",
                        "getConnectionMask(net.minecraft.world.IBlockAccess,int,int,int,int,boolean)int",
                        "getPower(net.minecraft.world.World,int,int,int,int,int)int",
                        "getPowerTo(codechicken.multipart.TMultiPart,int)int",
                        "getPowerTo(net.minecraft.world.World,int,int,int,int,int)int",
                        "otherConnectionMask(net.minecraft.world.IBlockAccess,int,int,int,int,boolean)int",
                        "sideVanillaMap()[I",
                        "vanillaConnectionMask(net.minecraft.block.Block,net.minecraft.world.IBlockAccess,int,int,int,int,boolean)int",
                        "vanillaSideMap()[I",
                        "vanillaToSide(int)int"));

        assertEquals(expected, publicSignatures(RedstoneInteractions.class, true));
        assertEquals(expected, publicSignatures(RedstoneInteractions$.class, false));
        assertTrue(Modifier.isFinal(RedstoneInteractions.class.getModifiers()));
        assertTrue(Modifier.isFinal(RedstoneInteractions$.class.getModifiers()));

        Field module = RedstoneInteractions$.class.getField("MODULE$");
        assertSame(RedstoneInteractions$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(RedstoneInteractions$.MODULE$, module.get(null));
    }

    @Test
    void sideMapsAndFullVanillaBlocksStayPublishedByIdentity() {
        assertArrayEquals(new int[] { -2, -1, 0, 2, 3, 1 }, RedstoneInteractions.vanillaSideMap());
        assertArrayEquals(new int[] { 1, 2, 5, 3, 4 }, RedstoneInteractions.sideVanillaMap());
        assertSame(RedstoneInteractions$.MODULE$.vanillaSideMap(), RedstoneInteractions.vanillaSideMap());
        assertSame(RedstoneInteractions$.MODULE$.sideVanillaMap(), RedstoneInteractions.sideVanillaMap());
        assertSame(RedstoneInteractions$.MODULE$.fullVanillaBlocks(), RedstoneInteractions.fullVanillaBlocks());
        assertEquals(5, RedstoneInteractions.vanillaToSide(1));
    }

    @Test
    void partConnectionMasksHonorConnectabilityAndPartKind() {
        assertEquals(0, RedstoneInteractions.connectionMask(new NonRedstonePart(), 2));
        assertEquals(0, RedstoneInteractions.connectionMask(new RedstonePart(false), 2));
        assertEquals(0x1F, RedstoneInteractions.connectionMask(new RedstonePart(true), 2));
        assertEquals(0x0B, RedstoneInteractions.connectionMask(new MaskedPart(0x0B), 2));

        assertEquals(0x10, RedstoneInteractions.connectionMask(new FacePart(3), 2));
        assertEquals(1 << Rotation.rotationTo(0, 2), RedstoneInteractions.connectionMask(new FacePart(2), 0));
        assertEquals(
                1 << Rotation.rotationTo(0, 2),
                RedstoneInteractions.connectionMask(new FaceMaskedPart(2, 0x0B), 0),
                "Face routing takes precedence when a part also supplies a custom mask");
    }

    @Test
    void connectionLookupPrefersConnectorTilesThenBlocksThenVanilla() {
        ConnectorTile tile = new ConnectorTile(0x12);
        ConnectorBlock block = new ConnectorBlock(0x0A);

        assertEquals(0x12, RedstoneInteractions.getConnectionMask(access(tile, block, 0, null), 1, 2, 3, 4, false));
        assertEquals(4, tile.lastSide);
        assertEquals(-1, block.lastSide, "The block must not be queried when the tile handled the request");

        assertEquals(0x0A, RedstoneInteractions.getConnectionMask(access(null, block, 0, null), 1, 2, 3, 5, false));
        assertEquals(5, block.lastSide);
        ConnectableBlock connectable = new ConnectableBlock(true);
        assertEquals(
                0x1F,
                RedstoneInteractions.getConnectionMask(access(null, connectable, 0, null), 1, 2, 3, 5, false));
        assertEquals(1, connectable.lastSide, "Side 5 is translated to vanilla side 1");
    }

    @Test
    void otherConnectionMaskOffsetsCoordinatesAndFlipsTheSide() {
        int[] queried = new int[3];
        ConnectorTile tile = new ConnectorTile(0x15);
        IBlockAccess world = access(tile, Blocks.air, 0, queried);

        assertEquals(0x15, RedstoneInteractions.otherConnectionMask(world, 10, 20, 30, 5, false));
        assertArrayEquals(new int[] { 11, 20, 30 }, queried);
        assertEquals(4, tile.lastSide);
    }

    private static void assertInterface(Class<?> type, Class<?>[] parents, String... signatures) {
        assertTrue(type.isInterface(), type.getName());
        assertArrayEquals(parents, type.getInterfaces(), type.getName());
        for (Method method : type.getDeclaredMethods()) {
            assertTrue(Modifier.isPublic(method.getModifiers()), method.toString());
            assertTrue(Modifier.isAbstract(method.getModifiers()), method.toString());
        }
        assertEquals(new TreeSet<>(Arrays.asList(signatures)), publicSignatures(type, false));
    }

    private static Set<String> publicSignatures(Class<?> type, boolean requireStatic) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertEquals(requireStatic, Modifier.isStatic(method.getModifiers()), method.toString());
            signatures.add(signature(method));
        }
        return signatures;
    }

    private static String signature(Method method) {
        StringBuilder out = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(parameters[i].getName());
        }
        return out.append(')').append(method.getReturnType().getName()).toString();
    }

    private static IBlockAccess access(TileEntity tile, Block block, int metadata, int[] queried) {
        return (IBlockAccess) Proxy.newProxyInstance(
                IBlockAccess.class.getClassLoader(),
                new Class<?>[] { IBlockAccess.class },
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getTileEntity":
                            if (queried != null) {
                                queried[0] = (Integer) arguments[0];
                                queried[1] = (Integer) arguments[1];
                                queried[2] = (Integer) arguments[2];
                            }
                            return tile;
                        case "getBlock":
                            return block;
                        case "getBlockMetadata":
                            return metadata;
                        default:
                            return method.getReturnType() == boolean.class ? false : 0;
                    }
                });
    }

    private static class NonRedstonePart extends TMultiPart {

        @Override
        public String getType() {
            return "test:non_redstone";
        }
    }

    private static class RedstonePart extends NonRedstonePart implements IRedstonePart {

        private final boolean connects;

        private RedstonePart(boolean connects) {
            this.connects = connects;
        }

        @Override
        public int strongPowerLevel(int side) {
            return 0;
        }

        @Override
        public int weakPowerLevel(int side) {
            return 0;
        }

        @Override
        public boolean canConnectRedstone(int side) {
            return connects;
        }
    }

    private static final class FacePart extends RedstonePart implements IFaceRedstonePart {

        private final int face;

        private FacePart(int face) {
            super(true);
            this.face = face;
        }

        @Override
        public int getFace() {
            return face;
        }
    }

    private static final class MaskedPart extends RedstonePart implements IMaskedRedstonePart {

        private final int mask;

        private MaskedPart(int mask) {
            super(true);
            this.mask = mask;
        }

        @Override
        public int getConnectionMask(int side) {
            return mask;
        }
    }

    private static final class FaceMaskedPart extends RedstonePart implements IFaceRedstonePart, IMaskedRedstonePart {

        private final int face;
        private final int mask;

        private FaceMaskedPart(int face, int mask) {
            super(true);
            this.face = face;
            this.mask = mask;
        }

        @Override
        public int getFace() {
            return face;
        }

        @Override
        public int getConnectionMask(int side) {
            return mask;
        }
    }

    private static final class ConnectorTile extends TileEntity implements IRedstoneConnector {

        private final int mask;
        private int lastSide = -1;

        private ConnectorTile(int mask) {
            this.mask = mask;
        }

        @Override
        public int getConnectionMask(int side) {
            lastSide = side;
            return mask;
        }

        @Override
        public int weakPowerLevel(int side, int mask) {
            return 0;
        }
    }

    private static final class ConnectorBlock extends Block implements IRedstoneConnectorBlock {

        private final int mask;
        private int lastSide = -1;

        private ConnectorBlock(int mask) {
            super(Material.rock);
            this.mask = mask;
        }

        @Override
        public int getConnectionMask(IBlockAccess world, int x, int y, int z, int side) {
            lastSide = side;
            return mask;
        }

        @Override
        public int weakPowerLevel(IBlockAccess world, int x, int y, int z, int side, int mask) {
            return 0;
        }
    }

    private static final class ConnectableBlock extends Block {

        private final boolean connects;
        private int lastSide = -1;

        private ConnectableBlock(boolean connects) {
            super(Material.rock);
            this.connects = connects;
        }

        @Override
        public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
            lastSide = side;
            return connects;
        }
    }
}
