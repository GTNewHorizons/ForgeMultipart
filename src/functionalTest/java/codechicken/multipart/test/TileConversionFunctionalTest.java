package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileConversionResult;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipart$;
import codechicken.multipart.examples.TileConversionExample;
import codechicken.multipart.minecraft.TorchPart;
import scala.Tuple2;

class TileConversionFunctionalTest {

    @Test
    void javaExistingTileIsReturnedWithoutConversion() {
        checkExisting(TileConversionFunctionalTest::javaLookup);
    }

    @Test
    void javaNonConvertibleBlocksReturnNullAndFalse() {
        checkMissing(TileConversionFunctionalTest::javaLookup);
    }

    @Test
    void javaConversionCreatesBoundPlaceholdersWithoutInstallingThem() {
        checkConverted(TileConversionFunctionalTest::javaLookup);
    }

    private static Tuple2<TileMultipart, Object> javaLookup(World world, BlockCoord pos) {
        TileConversionResult result = TileMultipart.getOrConvertTileResult(world, pos);
        TileMultipart example = TileConversionExample.findConvertedPlaceholder(world, pos);
        if (result.isConverted()) {
            assertNotSame(result.getTile(), example);
            assertSame(world, example.getWorldObj());
            assertEquals(pos, new BlockCoord(example));
        } else {
            assertNull(example);
        }
        return new Tuple2<>(result.getTile(), result.isConverted());
    }

    @Test
    void legacyExistingTileIsReturnedWithoutConversion() {
        checkExisting(TileMultipart::getOrConvertTile2);
        checkExisting(TileMultipart$.MODULE$::getOrConvertTile2);
    }

    @Test
    void legacyNonConvertibleBlocksReturnNullAndFalse() {
        checkMissing(TileMultipart::getOrConvertTile2);
        checkMissing(TileMultipart$.MODULE$::getOrConvertTile2);
    }

    @Test
    void legacyConversionCreatesBoundPlaceholdersWithoutInstallingThem() {
        checkConverted(TileMultipart::getOrConvertTile2);
        checkConverted(TileMultipart$.MODULE$::getOrConvertTile2);
    }

    private static void checkExisting(BiFunction<World, BlockCoord, Tuple2<TileMultipart, Object>> lookup) {
        World world = world();
        BlockCoord pos = new BlockCoord(54, 200, 48);
        world.setBlockToAir(pos.x, pos.y, pos.z);
        try {
            TMultiPart part = new MultipartGeneratorFunctionalTest.PlainPart();
            TileMultipart tile = TileMultipart.addPart(world, pos, part);
            Tuple2<TileMultipart, Object> result = lookup.apply(world, pos);
            assertSame(tile, result._1());
            assertEquals(Boolean.FALSE, result._2());
            assertSame(tile, TileMultipart.getOrConvertTile(world, pos));
            assertSame(tile, world.getTileEntity(pos.x, pos.y, pos.z));
            assertEquals(1, tile.jPartList().size());
            assertSame(part, tile.jPartList().get(0));
            assertSame(tile, part.tile());
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
        }
    }

    private static void checkMissing(BiFunction<World, BlockCoord, Tuple2<TileMultipart, Object>> lookup) {
        World world = world();
        BlockCoord pos = new BlockCoord(54, 200, 48);
        try {
            for (Block block : new Block[] { Blocks.air, Blocks.stone, Blocks.chest }) {
                world.setBlock(pos.x, pos.y, pos.z, block, 0, 0);
                TileEntity existing = world.getTileEntity(pos.x, pos.y, pos.z);
                Tuple2<TileMultipart, Object> result = lookup.apply(world, pos);
                assertNull(result._1());
                assertEquals(Boolean.FALSE, result._2());
                assertNull(TileMultipart.getOrConvertTile(world, pos));
                assertSame(block, world.getBlock(pos.x, pos.y, pos.z));
                assertSame(existing, world.getTileEntity(pos.x, pos.y, pos.z));
            }
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
        }
    }

    private static void checkConverted(BiFunction<World, BlockCoord, Tuple2<TileMultipart, Object>> lookup) {
        World world = world();
        BlockCoord pos = new BlockCoord(54, 200, 48);
        world.setBlock(pos.x, pos.y - 1, pos.z, Blocks.stone, 0, 0);
        world.setBlock(pos.x, pos.y, pos.z, Blocks.torch, 5, 0);
        try {
            Tuple2<TileMultipart, Object> result = lookup.apply(world, pos);
            assertEquals(Boolean.TRUE, result._2());
            TileMultipart tile = result._1();
            assertSame(world, tile.getWorldObj());
            assertEquals(pos, new BlockCoord(tile));
            assertFalse(tile.isInvalid());
            assertEquals(1, tile.jPartList().size());
            TMultiPart part = tile.jPartList().get(0);
            assertTrue(part instanceof TorchPart);
            assertSame(tile, part.tile());
            assertSame(Blocks.torch, world.getBlock(pos.x, pos.y, pos.z));
            assertEquals(5, world.getBlockMetadata(pos.x, pos.y, pos.z));
            assertNull(world.getTileEntity(pos.x, pos.y, pos.z));

            Tuple2<TileMultipart, Object> repeated = lookup.apply(world, pos);
            assertEquals(Boolean.TRUE, repeated._2());
            assertNotSame(tile, repeated._1());
            assertNotSame(part, repeated._1().jPartList().get(0));
            assertNull(TileMultipart.getTile(world, pos));

            TMultiPart added = new MultipartGeneratorFunctionalTest.PlainPart();
            TileMultipart installed = TileMultipart.addPart(world, pos, added);
            assertNotSame(tile, installed);
            assertSame(installed, world.getTileEntity(pos.x, pos.y, pos.z));
            assertEquals(2, installed.jPartList().size());
            assertSame(added, installed.jPartList().get(1));
            assertEquals(Boolean.TRUE, result._2(), "A result records the lookup, not subsequent world state");
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
            world.setBlockToAir(pos.x, pos.y - 1, pos.z);
        }
    }

    private static World world() {
        World world = MinecraftServer.getServer().worldServers[0];
        world.getChunkFromBlockCoords(54, 48);
        return world;
    }
}
