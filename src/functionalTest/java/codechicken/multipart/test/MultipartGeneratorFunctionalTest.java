package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.MultipartGenerator$;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.handler.MultipartProxy;
import codechicken.multipart.handler.MultipartSaveLoad;
import codechicken.multipart.minecraft.TorchPart;
import codechicken.multipart.scalatraits.TSlottedTile;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.Map;

public class MultipartGeneratorFunctionalTest {

    @Test
    void cachesCompleteHierarchySeparatelyPerSideAndKeepsFirstRegistration() {
        MultipartGenerator$ generator = MultipartGenerator$.MODULE$;
        String root = RootMarker.class.getName();
        String slotted = "codechicken.multipart.scalatraits.TSlottedTile";
        String redstone = "codechicken.multipart.scalatraits.TRedstoneTile";
        String tileChange = "codechicken.multipart.scalatraits.TTileChangeTile";
        MultipartGenerator.registerTrait(root, redstone, slotted);
        HierarchyPart part = new HierarchyPart();
        BitSet server = generator.codechicken$multipart$MultipartGenerator$$traitsForPart(part, false);
        BitSet client = generator.codechicken$multipart$MultipartGenerator$$traitsForPart(part, true);
        assertEquals(bits(slotted), server);
        assertEquals(bits(redstone), client);
        assertNotSame(server, client);
        assertSame(
                server,
                generator.codechicken$multipart$MultipartGenerator$$traitsForPart(new HierarchyPart(), false));

        MultipartGenerator.registerTrait(root, slotted, redstone);
        assertEquals(slotted.replace('.', '/'), map(false).apply(root.replace('.', '/')));
        assertEquals(redstone.replace('.', '/'), map(true).apply(root.replace('.', '/')));
        MultipartGenerator.registerTrait(LateMarker.class.getName(), tileChange, null);
        assertSame(client, generator.codechicken$multipart$MultipartGenerator$$traitsForPart(part, true));
        assertEquals(bits(redstone), client, "Late registration does not invalidate an already-cached part class");
        BitSet fresh = bits(redstone, tileChange);
        assertEquals(
                fresh,
                generator.codechicken$multipart$MultipartGenerator$$traitsForPart(new FreshHierarchyPart(), true));
        assertEquals(
                bits(slotted),
                generator.codechicken$multipart$MultipartGenerator$$traitsForPart(new FreshHierarchyPart(), false));

        String missing = "codechicken.multipart.test.NonexistentGeneratorTrait";
        assertThrows(
                ClassNotFoundException.class,
                () -> MultipartGenerator.registerTrait(FailedMarker.class.getName(), null, missing));
        assertEquals(
                missing.replace('.', '/'),
                map(false).apply(FailedMarker.class.getName().replace('.', '/')),
                "The reference records a side mapping before resolving its trait class");
        String passThrough = GeneratorPassThroughFixture.class.getName().replace('.', '/');
        assertTrue(map(false).contains(passThrough));
        assertFalse(map(true).contains(passThrough));
    }

    @Test
    void reusesOnlyExactTraitSetsAndSnapshotsRegisteredClasses() {
        MultipartGenerator$ generator = MultipartGenerator$.MODULE$;
        BitSet registration = new BitSet();
        generator.registerTileClass(RegisteredTile.class, registration);
        registration.set(4095);
        BitSet scratch = MultipartGenerator.getBitSet();
        scratch.set(4095);
        RegisteredTile registered = new RegisteredTile();
        assertSame(registered, generator.generateCompositeTile(registered, parts(), false));
        assertSame(scratch, MultipartGenerator.getBitSet());
        assertTrue(scratch.isEmpty(), "Every construction clears the reused scratch set");
        assertEquals("savedMultipart", MultipartSaveLoad.getClassToNameMap().get(RegisteredTile.class));

        SlottedPart slotted = new SlottedPart();
        TileMultipart plain = generator.generateCompositeTile(null, parts(), false);
        assertSame(plain, generator.generateCompositeTile(plain, parts(new PlainPart()), false));
        TileMultipart upgraded = generator.generateCompositeTile(plain, parts(slotted), false);
        assertNotSame(plain, upgraded);
        assertTrue(upgraded instanceof TSlottedTile);
        assertSame(
                upgraded,
                generator.generateCompositeTile(upgraded, parts(slotted, new PlainPart(), slotted), false));
        assertSame(
                upgraded.getClass(),
                generator.generateCompositeTile(null, parts(new PlainPart(), slotted), false).getClass());
        TileMultipart client = generator.generateCompositeTile(upgraded, parts(slotted), true);
        assertTrue(client instanceof TileMultipartClient);
        assertNotSame(upgraded, client);
        assertSame(client, generator.generateCompositeTile(client, parts(slotted), true));
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> generator.generateCompositeTile(new UnregisteredTile(), parts(), false));
    }

    @Test
    void upgradesAndDowngradesLiveTilesWithoutLosingParts() {
        World world = MinecraftServer.getServer().worldServers[0];
        BlockCoord pos = new BlockCoord(48, 200, 48);
        world.getChunkFromBlockCoords(pos.x, pos.z);
        world.setBlockToAir(pos.x, pos.y, pos.z);
        PlainPart first = new PlainPart();
        SlottedPart second = new SlottedPart();
        try {
            TileMultipart plain = TileMultipart.addPart(world, pos, first);
            TileMultipart upgraded = TileMultipart.addPart(world, pos, second);
            assertNotSame(plain, upgraded);
            assertTrue(plain.isInvalid());
            assertTrue(upgraded instanceof TSlottedTile);
            assertSame(upgraded, world.getTileEntity(pos.x, pos.y, pos.z));
            assertEquals(Arrays.asList(first, second), upgraded.jPartList());
            assertSame(upgraded, first.tile());
            assertSame(second, upgraded.partMap(3));

            TileMultipart downgraded = upgraded.remPart(second);
            assertTrue(upgraded.isInvalid());
            assertNotSame(upgraded, downgraded);
            assertSame(plain.getClass(), downgraded.getClass());
            assertSame(downgraded, world.getTileEntity(pos.x, pos.y, pos.z));
            assertSame(downgraded, first.tile());
            assertEquals(Collections.singletonList(first), downgraded.jPartList());
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
        }
    }

    @Test
    void convertsVanillaBlockBeforeAppendingTheNewPart() {
        World world = MinecraftServer.getServer().worldServers[0];
        BlockCoord pos = new BlockCoord(50, 200, 48);
        world.getChunkFromBlockCoords(pos.x, pos.z);
        world.setBlock(pos.x, pos.y - 1, pos.z, Blocks.stone, 0, 0);
        world.setBlock(pos.x, pos.y, pos.z, Blocks.torch, 5, 0);
        PlainPart added = new PlainPart();
        try {
            TileMultipart tile = TileMultipart.addPart(world, pos, added);
            assertSame(MultipartProxy.block(), world.getBlock(pos.x, pos.y, pos.z));
            assertSame(tile, world.getTileEntity(pos.x, pos.y, pos.z));
            assertEquals(2, tile.partList().size());
            assertTrue(tile.partList().apply(0) instanceof TorchPart);
            assertSame(added, tile.partList().apply(1));
            assertSame(tile, tile.partList().apply(0).tile());
        } finally {
            world.setBlockToAir(pos.x, pos.y, pos.z);
            world.setBlockToAir(pos.x, pos.y - 1, pos.z);
        }
    }

    @Test
    void frozenScalaConsumerLinksCompanionGenerationAndDefaultPassThroughRegistration() throws Exception {
        byte[] bytes;
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/compat/ReferenceScalaMultipartGeneratorConsumer.class.b64"));
                Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name())) {
            bytes = Base64.getMimeDecoder().decode(scanner.useDelimiter("\\A").next());
        }
        Class<?> type = new FixtureClassLoader(MultipartGenerator.class.getClassLoader()).define(bytes);
        Object consumer = type.getDeclaredConstructor().newInstance();
        type.getMethod("register", String.class).invoke(consumer, FrozenPassThrough.class.getName());
        String key = FrozenPassThrough.class.getName().replace('.', '/');
        assertTrue(map(false).contains(key));
        assertEquals(map(false).apply(key), map(true).apply(key));
        Seq<TMultiPart> parts = parts(new PassThroughPart());
        TileMultipart tile = (TileMultipart) type
                .getMethod("create", TileEntity.class, scala.collection.Iterable.class, boolean.class)
                .invoke(consumer, null, parts, false);
        tile.loadParts(parts);
        assertEquals(37, ((FrozenPassThrough) tile).value());
    }

    private static Map<String, String> map(boolean client) {
        return MultipartGenerator$.MODULE$.codechicken$multipart$MultipartGenerator$$interfaceTraitMap(client);
    }

    private static BitSet bits(String... names) {
        BitSet bits = new BitSet();
        for (String name : names) bits.set(MultipartMixinFactory.getId(name.replace('.', '/')));
        return bits;
    }

    private static Seq<TMultiPart> parts(TMultiPart... parts) {
        return JavaConversions.asScalaBuffer(Arrays.asList(parts)).toList();
    }

    public interface RootMarker {
    }

    public interface ChildMarker extends RootMarker {
    }

    public interface LateMarker {
    }

    public interface FailedMarker {
    }

    public interface FrozenPassThrough {

        int value();
    }

    public static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "mc_torch";
        }

        @Override
        public boolean doesTick() {
            return false;
        }
    }

    public static class ParentPart extends PlainPart implements ChildMarker {
    }

    public static class HierarchyPart extends ParentPart implements LateMarker {
    }

    public static class FreshHierarchyPart extends HierarchyPart {
    }

    public static class SlottedPart extends PlainPart implements TSlottedPart {

        @Override
        public int getSlotMask() {
            return 1 << 3;
        }
    }

    public static class PassThroughPart extends PlainPart implements FrozenPassThrough {

        @Override
        public int value() {
            return 37;
        }
    }

    public static class RegisteredTile extends TileMultipart {
    }

    public static class UnregisteredTile extends TileMultipart {
    }

    private static final class FixtureClassLoader extends ClassLoader {

        private FixtureClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }
}
