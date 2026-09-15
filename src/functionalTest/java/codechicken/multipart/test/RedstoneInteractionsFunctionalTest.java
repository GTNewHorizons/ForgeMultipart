package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.IRedstonePart;
import codechicken.multipart.IRedstoneTile;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.RedstoneInteractions;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** World-dependent redstone routing and the generated tile selected by the IRedstonePart marker. */
class RedstoneInteractionsFunctionalTest {

    private static final BlockCoord HOST = new BlockCoord(40, 200, 40);
    private static final BlockCoord EAST = new BlockCoord(41, 200, 40);
    private static final BlockCoord WIRE = new BlockCoord(44, 200, 40);

    @Test
    void vanillaConnectionMasksPreserveEverySpecialCaseAfterBlocksInitialize() {
        World world = world();
        ConnectableBlock connectable = new ConnectableBlock(true);

        assertEquals(6, RedstoneInteractions.fullVanillaBlocks().size());
        assertTrue(RedstoneInteractions.fullVanillaBlocks().contains(Blocks.redstone_torch));
        assertTrue(RedstoneInteractions.fullVanillaBlocks().contains(Blocks.unlit_redstone_torch));
        assertTrue(RedstoneInteractions.fullVanillaBlocks().contains(Blocks.lever));
        assertTrue(RedstoneInteractions.fullVanillaBlocks().contains(Blocks.stone_button));
        assertTrue(RedstoneInteractions.fullVanillaBlocks().contains(Blocks.wooden_button));
        assertTrue(RedstoneInteractions.fullVanillaBlocks().contains(Blocks.redstone_block));

        assertEquals(0x1F, RedstoneInteractions.vanillaConnectionMask(Blocks.redstone_torch, world, 1, 2, 3, 0, false));
        assertEquals(0, RedstoneInteractions.vanillaConnectionMask(Blocks.redstone_wire, world, 1, 2, 3, 0, true));
        assertEquals(4, RedstoneInteractions.vanillaConnectionMask(Blocks.redstone_wire, world, 1, 2, 3, 2, false));
        assertEquals(0x1F, RedstoneInteractions.vanillaConnectionMask(Blocks.redstone_wire, world, 1, 2, 3, 2, true));
        assertEquals(4, RedstoneInteractions.vanillaConnectionMask(Blocks.powered_repeater, world, 1, 2, 3, 2, false));
        assertEquals(0, RedstoneInteractions.vanillaConnectionMask(Blocks.powered_repeater, world, 1, 2, 3, 4, false));
        assertEquals(0x1F, RedstoneInteractions.vanillaConnectionMask(connectable, world, 1, 2, 3, 5, false));
        assertEquals(1, connectable.lastSide, "Side 5 is translated to vanilla side 1");
        assertEquals(
                0,
                RedstoneInteractions.vanillaConnectionMask(new ConnectableBlock(false), world, 1, 2, 3, 5, false));
        assertEquals(
                0x1F,
                RedstoneInteractions.vanillaConnectionMask(new ConnectableBlock(false), world, 1, 2, 3, 5, true));
    }

    @Test
    void partPowerUsesTheGeneratedRedstoneTileAndNeighborConnector() {
        World world = world();
        clear(world, HOST);
        clear(world, EAST);
        RedstonePart part = new RedstonePart();
        TileMultipart tile = MultipartHelper.createTileFromParts(Collections.<TMultiPart>singletonList(part));
        ConnectorTile connector = new ConnectorTile();

        try {
            tile.setWorldObj(world);
            tile.xCoord = HOST.x;
            tile.yCoord = HOST.y;
            tile.zCoord = HOST.z;
            assertTrue(tile instanceof IRedstoneTile);

            assertTrue(world.setBlock(EAST.x, EAST.y, EAST.z, Blocks.chest, 0, 3));
            world.setTileEntity(EAST.x, EAST.y, EAST.z, connector);
            assertSame(connector, world.getTileEntity(EAST.x, EAST.y, EAST.z));

            assertEquals(431, RedstoneInteractions.getPowerTo(part, 5));
            assertEquals(4, connector.lastSide);
            assertEquals(0x1F, connector.lastMask);
            assertEquals(207, RedstoneInteractions.getPower(world, EAST.x, EAST.y, EAST.z, 2, 7));
        } finally {
            clear(world, HOST);
            clear(world, EAST);
        }
    }

    @Test
    void vanillaWirePowerUsesMetadataWhenIndirectPowerIsLower() {
        World world = world();
        clear(world, WIRE);
        world.setBlockToAir(WIRE.x, WIRE.y - 1, WIRE.z);

        try {
            assertTrue(world.setBlock(WIRE.x, WIRE.y - 1, WIRE.z, Blocks.stone, 0, 3));
            assertTrue(world.setBlock(WIRE.x, WIRE.y, WIRE.z, Blocks.redstone_wire, 12, 2));
            world.setBlockMetadataWithNotify(WIRE.x, WIRE.y, WIRE.z, 12, 2);

            assertEquals(12, RedstoneInteractions.getPower(world, WIRE.x, WIRE.y, WIRE.z, 1, 0x1F));
            assertEquals(0, RedstoneInteractions.getPower(world, WIRE.x, WIRE.y, WIRE.z, 1, 0));
        } finally {
            clear(world, WIRE);
            world.setBlockToAir(WIRE.x, WIRE.y - 1, WIRE.z);
        }
    }

    private static World world() {
        World world = MinecraftServer.getServer().worldServers[0];
        world.getChunkFromBlockCoords(HOST.x, HOST.z);
        return world;
    }

    private static void clear(World world, BlockCoord pos) {
        world.removeTileEntity(pos.x, pos.y, pos.z);
        world.setBlockToAir(pos.x, pos.y, pos.z);
    }

    private static final class RedstonePart extends TMultiPart implements IRedstonePart {

        @Override
        public String getType() {
            return "test:redstone";
        }

        @Override
        public boolean doesTick() {
            return false;
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
            return true;
        }
    }

    private static final class ConnectorTile extends TileEntity implements codechicken.multipart.IRedstoneConnector {

        private int lastSide = -1;
        private int lastMask = -1;

        @Override
        public int getConnectionMask(int side) {
            return 0;
        }

        @Override
        public int weakPowerLevel(int side, int mask) {
            lastSide = side;
            lastMask = mask;
            return side * 100 + mask;
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
        public boolean canConnectRedstone(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
            lastSide = side;
            return connects;
        }
    }
}
