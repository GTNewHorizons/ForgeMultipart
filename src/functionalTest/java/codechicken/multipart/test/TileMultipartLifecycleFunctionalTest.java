package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.scalatraits.TSlottedTile;

/** Consumer-visible multipart ordering and lifecycle behavior that requires a real world and generated tile. */
class TileMultipartLifecycleFunctionalTest {

    private static final BlockCoord ADD_REMOVE_POS = new BlockCoord(32, 200, 32);
    private static final BlockCoord MOVE_FROM = new BlockCoord(34, 200, 32);
    private static final BlockCoord MOVE_TO = new BlockCoord(36, 200, 32);

    @Test
    void addAndRemovePreserveOrderSlotsAndPartCallbackOrder() {
        World world = world();
        clear(world, ADD_REMOVE_POS);
        List<String> events = new ArrayList<>();
        RecordingPart first = new RecordingPart("first", 2, events);
        RecordingPart second = new RecordingPart("second", 7, events);

        try {
            TileMultipart tile = TileMultipart.addPart(world, ADD_REMOVE_POS, first);
            assertEquals(Arrays.asList("first.bind", "first.onAdded", "first.onWorldJoin"), events);
            assertEquals(Arrays.asList(first), tile.jPartList());
            assertSame(first, tile.partMap(2));

            events.clear();
            TileMultipart sameTile = TileMultipart.addPart(world, ADD_REMOVE_POS, second);
            assertSame(tile, sameTile);
            assertEquals(
                    Arrays.asList("second.bind", "second.onAdded", "second.onWorldJoin", "first.changed:second"),
                    events);
            assertEquals(Arrays.asList(first, second), tile.jPartList());
            assertSame(first, tile.partMap(2));
            assertSame(second, tile.partMap(7));

            events.clear();
            assertSame(tile, tile.remPart(first));
            assertEquals(
                    Arrays.asList(
                            "first.preRemove",
                            "first.onRemoved",
                            "first.onWorldSeparate",
                            "second.changed:first"),
                    events);
            assertEquals(Arrays.asList(second), tile.jPartList());
            assertNull(tile.partMap(2));
            assertSame(second, tile.partMap(7));
            assertNull(first.tile());
        } finally {
            clear(world, ADD_REMOVE_POS);
        }
    }

    @Test
    void movingTheLiveTileKeepsPartsSlotsAndGeneratedInterfaces() {
        World world = world();
        clear(world, MOVE_FROM);
        clear(world, MOVE_TO);
        List<String> events = new ArrayList<>();
        RecordingPart first = new RecordingPart("first", 3, events);
        RecordingPart second = new RecordingPart("second", 8, events);

        try {
            TileMultipart tile = TileMultipart.addPart(world, MOVE_FROM, first);
            tile = TileMultipart.addPart(world, MOVE_FROM, second);
            Block block = world.getBlock(MOVE_FROM.x, MOVE_FROM.y, MOVE_FROM.z);
            int meta = world.getBlockMetadata(MOVE_FROM.x, MOVE_FROM.y, MOVE_FROM.z);
            events.clear();

            world.removeTileEntity(MOVE_FROM.x, MOVE_FROM.y, MOVE_FROM.z);
            world.setBlockToAir(MOVE_FROM.x, MOVE_FROM.y, MOVE_FROM.z);
            assertTrue(world.setBlock(MOVE_TO.x, MOVE_TO.y, MOVE_TO.z, block, meta, 3));
            tile.xCoord = MOVE_TO.x;
            tile.yCoord = MOVE_TO.y;
            tile.zCoord = MOVE_TO.z;
            tile.validate();
            world.setTileEntity(MOVE_TO.x, MOVE_TO.y, MOVE_TO.z, tile);
            tile.onMoved();

            assertNull(world.getTileEntity(MOVE_FROM.x, MOVE_FROM.y, MOVE_FROM.z));
            assertSame(tile, world.getTileEntity(MOVE_TO.x, MOVE_TO.y, MOVE_TO.z));
            assertTrue(tile instanceof TSlottedTile);
            assertEquals(Arrays.asList(first, second), tile.jPartList());
            assertSame(first, tile.partMap(3));
            assertSame(second, tile.partMap(8));
            assertSame(tile, first.tile());
            assertSame(tile, second.tile());
            assertEquals(
                    Arrays.asList(
                            "first.onWorldSeparate",
                            "second.onWorldSeparate",
                            "first.onMoved",
                            "first.onWorldJoin",
                            "second.onMoved",
                            "second.onWorldJoin"),
                    events);
            assertEquals(MOVE_TO, first.movedTo);
            assertEquals(MOVE_TO, second.movedTo);
        } finally {
            clear(world, MOVE_FROM);
            clear(world, MOVE_TO);
        }
    }

    private static World world() {
        World world = MinecraftServer.getServer().worldServers[0];
        world.getChunkFromBlockCoords(ADD_REMOVE_POS.x, ADD_REMOVE_POS.z);
        return world;
    }

    private static void clear(World world, BlockCoord pos) {
        world.setBlockToAir(pos.x, pos.y, pos.z);
    }

    private static final class RecordingPart extends TMultiPart implements TSlottedPart {

        private final String name;
        private final int slot;
        private final List<String> events;
        private BlockCoord movedTo;

        private RecordingPart(String name, int slot, List<String> events) {
            this.name = name;
            this.slot = slot;
            this.events = events;
        }

        @Override
        public String getType() {
            return "mc_torch";
        }

        @Override
        public int getSlotMask() {
            return 1 << slot;
        }

        @Override
        public boolean doesTick() {
            return false;
        }

        @Override
        public void bind(TileMultipart tile) {
            super.bind(tile);
            events.add(name + ".bind");
        }

        @Override
        public void onAdded() {
            events.add(name + ".onAdded");
            super.onAdded();
        }

        @Override
        public void onWorldJoin() {
            events.add(name + ".onWorldJoin");
        }

        @Override
        public void onPartChanged(TMultiPart part) {
            events.add(name + ".changed:" + ((RecordingPart) part).name);
        }

        @Override
        public void preRemove() {
            events.add(name + ".preRemove");
        }

        @Override
        public void onRemoved() {
            events.add(name + ".onRemoved");
            super.onRemoved();
        }

        @Override
        public void onWorldSeparate() {
            events.add(name + ".onWorldSeparate");
        }

        @Override
        public void onMoved() {
            movedTo = new BlockCoord(tile());
            events.add(name + ".onMoved");
            super.onMoved();
        }
    }
}
