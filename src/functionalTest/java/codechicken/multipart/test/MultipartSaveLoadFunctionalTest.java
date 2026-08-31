package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

import codechicken.multipart.MultipartHelper;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.handler.MultipartSaveLoad;
import codechicken.multipart.handler.MultipartSaveLoad$;
import codechicken.multipart.minecraft.ButtonPart;
import codechicken.multipart.minecraft.TorchPart;

class MultipartSaveLoadFunctionalTest {

    @Test
    void referenceScalaConsumerStillLinksToTheCompanionState() throws Exception {
        World original = MultipartSaveLoad.loadingWorld();
        World world = world();
        try {
            Class<?> fixtureClass = new FixtureClassLoader(MultipartSaveLoad.class.getClassLoader())
                    .define(loadFixture());
            Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

            fixtureClass.getMethod("loadingWorld_$eq", World.class).invoke(fixture, world);

            assertSame(world, MultipartSaveLoad.loadingWorld());
            assertSame(world, MultipartSaveLoad$.MODULE$.loadingWorld());
            assertSame(world, fixtureClass.getMethod("loadingWorld").invoke(fixture));
            assertSame(MultipartSaveLoad.converters(), MultipartSaveLoad$.MODULE$.converters());
        } finally {
            MultipartSaveLoad.loadingWorld_$eq(original);
        }
    }

    @Test
    void hooksTheDummyTileAndGeneratedTileNamesIntoVanillaMaps() {
        MultipartSaveLoad.hookLoader();

        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("id", "savedMultipart");
        saved.setInteger("x", 7);
        saved.setInteger("y", 8);
        saved.setInteger("z", 9);
        TileEntity dummy = TileEntity.createAndLoadEntity(saved);

        assertNotNull(dummy);
        assertEquals("codechicken.multipart.handler.MultipartSaveLoad$TileNBTContainer", dummy.getClass().getName());
        assertSame(saved, tag(dummy));
        assertEquals(7, dummy.xCoord);
        assertEquals(8, dummy.yCoord);
        assertEquals(9, dummy.zCoord);

        MultipartSaveLoad.registerTileClass(RegisteredTile.class);
        assertEquals("savedMultipart", MultipartSaveLoad.getClassToNameMap().get(RegisteredTile.class));
        NBTTagCompound registered = new NBTTagCompound();
        new RegisteredTile().writeToNBT(registered);
        assertEquals("savedMultipart", registered.getString("id"));
    }

    @Test
    void leavesUnclaimedTilesRemovesEmptyConversionsAndUsesTheFirstMatch() {
        World world = world();
        Chunk chunk = new Chunk(world, 40, 40);
        UnclaimedTile unclaimed = tile(new UnclaimedTile(), world);
        ConvertedTile converted = tile(new ConvertedTile(), world);
        DeletedTile deleted = tile(new DeletedTile(), world);
        ChunkPosition unclaimedPosition = put(chunk, 1, unclaimed);
        ChunkPosition convertedPosition = put(chunk, 2, converted);
        ChunkPosition deletedPosition = put(chunk, 3, deleted);

        RecordingConverter first = new RecordingConverter();
        RejectingSecondConverter second = new RejectingSecondConverter();
        MultipartHelper.registerTileConverter(first);
        MultipartHelper.registerTileConverter(second);
        MultipartHelper.registerTileConverter(new DeletingConverter());

        MultipartSaveLoad.loadTiles(chunk);

        assertSame(world, MultipartSaveLoad.loadingWorld());
        assertSame(unclaimed, chunk.chunkTileEntityMap.get(unclaimedPosition));
        assertFalse(chunk.chunkTileEntityMap.containsKey(deletedPosition));
        TileMultipart replacement = assertInstanceOf(
                TileMultipart.class,
                chunk.chunkTileEntityMap.get(convertedPosition));
        assertSame(world, replacement.getWorldObj());
        assertEquals(1, replacement.jPartList().size());
        assertSame(first.part, replacement.jPartList().get(0));
        assertEquals(1, first.calls);
        assertEquals(0, second.calls);
    }

    @Test
    void rebuildsSavedDummyTilesAndLeavesForeignDummiesUntouched() {
        World world = world();
        Chunk chunk = new Chunk(world, 41, 41);
        TileMultipart savedTile = MultipartHelper
                .createTileFromParts(Arrays.asList(new TorchPart(5), new ButtonPart(1)));
        NBTTagCompound savedTag = new NBTTagCompound();
        savedTile.writeToNBT(savedTag);
        TileEntity savedDummy = dummy(savedTag, world);
        ChunkPosition savedPosition = put(chunk, 1, savedDummy);

        NBTTagCompound foreignTag = new NBTTagCompound();
        foreignTag.setString("id", "foreign");
        TileEntity foreignDummy = dummy(foreignTag, world);
        ChunkPosition foreignPosition = put(chunk, 2, foreignDummy);

        MultipartSaveLoad.loadTiles(chunk);

        TileMultipart rebuilt = assertInstanceOf(TileMultipart.class, chunk.chunkTileEntityMap.get(savedPosition));
        assertSame(world, rebuilt.getWorldObj());
        assertEquals(2, rebuilt.jPartList().size());
        assertEquals("mc_torch", rebuilt.jPartList().get(0).getType());
        assertEquals("mc_button", rebuilt.jPartList().get(1).getType());
        assertSame(foreignDummy, chunk.chunkTileEntityMap.get(foreignPosition));
    }

    private static TileEntity dummy(NBTTagCompound tag, World world) {
        try {
            TileEntity dummy = (TileEntity) Class
                    .forName("codechicken.multipart.handler.MultipartSaveLoad$TileNBTContainer")
                    .getDeclaredConstructor().newInstance();
            dummy.readFromNBT(tag);
            dummy.setWorldObj(world);
            return dummy;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object tag(TileEntity tile) {
        try {
            return tile.getClass().getMethod("tag").invoke(tile);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static <T extends TileEntity> T tile(T tile, World world) {
        tile.setWorldObj(world);
        return tile;
    }

    private static ChunkPosition put(Chunk chunk, int offset, TileEntity tile) {
        ChunkPosition position = new ChunkPosition(chunk.xPosition * 16 + offset, 64, chunk.zPosition * 16 + offset);
        chunk.chunkTileEntityMap.put(position, tile);
        return position;
    }

    private static WorldServer world() {
        WorldServer world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        assertFalse(world.isRemote);
        return world;
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                MultipartSaveLoadFunctionalTest.class
                        .getResourceAsStream("/compat/ReferenceScalaMultipartSaveLoadConsumer.class.b64"));
        try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
            return Base64.getMimeDecoder().decode(scanner.next());
        }
    }

    private static final class FixtureClassLoader extends ClassLoader {

        private FixtureClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(byte[] bytecode) {
            return defineClass(null, bytecode, 0, bytecode.length);
        }
    }

    private static class RegisteredTile extends TileEntity {
    }

    private static final class UnclaimedTile extends TileEntity {
    }

    private static final class ConvertedTile extends TileEntity {
    }

    private static final class DeletedTile extends TileEntity {
    }

    private static final class RecordingConverter extends MultipartHelper.IPartTileConverter<ConvertedTile> {

        private final TMultiPart part = new TorchPart(3);
        private int calls;

        private RecordingConverter() {
            super(ConvertedTile.class);
        }

        @Override
        public TMultiPart convertOne(ConvertedTile tile) {
            calls++;
            return part;
        }
    }

    private static final class RejectingSecondConverter extends MultipartHelper.IPartTileConverter<ConvertedTile> {

        private int calls;

        private RejectingSecondConverter() {
            super(ConvertedTile.class);
        }

        @Override
        public TMultiPart convertOne(ConvertedTile tile) {
            calls++;
            return new ButtonPart(1);
        }
    }

    private static final class DeletingConverter extends MultipartHelper.IPartTileConverter<DeletedTile> {

        private DeletingConverter() {
            super(DeletedTile.class);
        }

        @Override
        public Iterable<TMultiPart> convertMulti(DeletedTile tile) {
            return Collections.emptyList();
        }

        @Override
        public TMultiPart convertOne(DeletedTile tile) {
            throw new AssertionError("convertMulti supplies the deletion result");
        }
    }
}
