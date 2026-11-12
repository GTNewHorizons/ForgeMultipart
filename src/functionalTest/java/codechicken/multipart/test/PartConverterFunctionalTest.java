package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.examples.BlockConversionExample;

class PartConverterFunctionalTest {

    @Test
    void javaExampleRegistersConvertsAndPreservesStateThroughNbtAndPackets() {
        World world = MinecraftServer.getServer().worldServers[0];
        BlockCoord pos = new BlockCoord(60, 200, 48);
        Block source = ForgeMultipartFunctionalTestMod.illuminatedLamp;
        world.getChunkFromBlockCoords(pos.x, pos.z);
        BlockConversionExample converter = (BlockConversionExample) MultiPartRegistry
                .getPartFactory(BlockConversionExample.PART_TYPE);
        assertEquals(Collections.singletonList(source), converter.blockTypes());
        try {
            world.setBlock(pos.x, pos.y, pos.z, Blocks.stone, 0, 0);
            assertNull(converter.convert(world, pos));
            world.setBlock(pos.x, pos.y, pos.z, source, 11, 0);
            TMultiPart converted = MultiPartRegistry.convertBlock(world, pos, source);
            assertTrue(converted instanceof BlockConversionExample.ConvertedPart);
            assertNull(converted.tile());
            assertNotSame(converted, MultiPartRegistry.convertBlock(world, pos, source));
            assertSame(source, world.getBlock(pos.x, pos.y, pos.z));
            NBTTagCompound saved = new NBTTagCompound();
            converted.save(saved);
            assertEquals(11, saved.getInteger("metadata"));

            TMultiPart restored = MultiPartRegistry.loadPart(converted.getType(), saved);
            assertNotSame(converted, restored);
            assertNull(restored.tile());
            restored.load(saved);
            NBTTagCompound copy = new NBTTagCompound();
            restored.save(copy);
            assertEquals(saved, copy);

            PacketCustom outgoing = new PacketCustom("test", 1);
            MultiPartRegistry.writePartID(outgoing, converted);
            converted.writeDesc(outgoing);
            outgoing.writeByte(37);
            PacketCustom incoming = new PacketCustom(outgoing.getByteBuf().copy());
            TMultiPart client = MultiPartRegistry.readPart(incoming);
            assertNotSame(converted, client);
            assertNull(client.tile());
            client.readDesc(incoming);
            copy = new NBTTagCompound();
            client.save(copy);
            assertEquals(saved, copy);
            assertEquals(37, incoming.readUByte());

            TMultiPart added = new MultipartGeneratorFunctionalTest.PlainPart();
            assertTrue(TileMultipart.canPlacePart(world, pos, added));
            TileMultipart installed = TileMultipart.addPart(world, pos, added);
            TMultiPart committed = installed.jPartList().get(0);
            assertEquals(converted.getType(), committed.getType());
            assertNotSame(converted, committed);
            copy = new NBTTagCompound();
            committed.save(copy);
            assertEquals(saved, copy);
            assertSame(installed, committed.tile());
            assertSame(installed, world.getTileEntity(pos.x, pos.y, pos.z));
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
        }
    }

    @Test
    void probesLeaveOriginalTileIntactAndCommitRunsConversionHooksInOrder() {
        World world = MinecraftServer.getServer().worldServers[0];
        BlockCoord pos = new BlockCoord(58, 200, 48);
        world.getChunkFromBlockCoords(pos.x, pos.z);
        world.setBlock(pos.x, pos.y, pos.z, Blocks.chest, 0, 0);
        TileEntity original = world.getTileEntity(pos.x, pos.y, pos.z);
        List<String> events = new ArrayList<>();
        List<TMultiPart> attempts = new ArrayList<>();
        MultiPartRegistry.registerConverter(new IPartConverter() {

            @Override
            public Iterable<Block> blockTypes() {
                return Collections.singletonList(Blocks.chest);
            }

            @Override
            public TMultiPart convert(World supplied, BlockCoord location) {
                if (supplied != world || !pos.equals(location)) return null;
                assertSame(original, world.getTileEntity(pos.x, pos.y, pos.z));
                TMultiPart part = new MultipartGeneratorFunctionalTest.PlainPart() {

                    @Override
                    public void invalidateConvertedTile() {
                        assertSame(original, world.getTileEntity(pos.x, pos.y, pos.z));
                        assertSame(Blocks.chest, world.getBlock(pos.x, pos.y, pos.z));
                        events.add("invalidate");
                    }

                    @Override
                    public void onConverted() {
                        assertSame(tile(), world.getTileEntity(pos.x, pos.y, pos.z));
                        assertTrue(original.isInvalid());
                        events.add("converted");
                        super.onConverted();
                    }

                    @Override
                    public void onAdded() {
                        events.add("converted-added");
                        super.onAdded();
                    }
                };
                attempts.add(part);
                return part;
            }
        });
        try {
            TMultiPart direct = MultiPartRegistry.convertBlock(world, pos, Blocks.chest);
            assertNull(direct.tile());
            TMultiPart rejected = new MultipartGeneratorFunctionalTest.PlainPart() {

                @Override
                public boolean occlusionTest(TMultiPart other) {
                    return false;
                }
            };
            assertFalse(TileMultipart.canPlacePart(world, pos, rejected));
            TMultiPart added = new MultipartGeneratorFunctionalTest.PlainPart() {

                @Override
                public void onAdded() {
                    events.add("new-added");
                    super.onAdded();
                }
            };
            assertTrue(TileMultipart.canPlacePart(world, pos, added));
            assertEquals(3, attempts.size());
            assertTrue(events.isEmpty());
            assertSame(original, world.getTileEntity(pos.x, pos.y, pos.z));
            assertFalse(original.isInvalid());
            assertNull(added.tile());
            TileMultipart installed = TileMultipart.addPart(world, pos, added);
            assertEquals(4, attempts.size());
            assertEquals(Arrays.asList("invalidate", "converted", "converted-added", "new-added"), events);
            assertSame(installed, world.getTileEntity(pos.x, pos.y, pos.z));
            assertSame(attempts.get(3), installed.jPartList().get(0));
            assertSame(added, installed.jPartList().get(1));
            for (int i = 0; i < 3; i++) {
                assertNotSame(attempts.get(i), attempts.get(3));
            }
            assertSame(installed, TileMultipart.getOrConvertTile(world, pos));
            assertEquals(4, attempts.size(), "An installed tile bypasses converters");
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
        }
    }
}
