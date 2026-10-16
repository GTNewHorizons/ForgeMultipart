package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.MultipartGenerator$;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import codechicken.multipart.examples.CompositeGenerationExample;
import codechicken.multipart.scalatraits.TSlottedTile;
import scala.collection.JavaConversions;

class CompositeGenerationFunctionalTest {

    @Test
    void javaGenerationSelectsCapabilitiesWithoutLoadingOrCopyingTiles() {
        checkStaging(MultipartGenerator::generateCompositeTile);
    }

    @Test
    void javaGenerationReadsInputOnceAndPropagatesFailuresBeforeReuse() {
        checkInput(MultipartGenerator::generateCompositeTile);
    }

    @Test
    void javaExactReflectionAcceptsJavaParts() throws Exception {
        Method method = MultipartGenerator.class
                .getMethod("generateCompositeTile", TileEntity.class, Iterable.class, boolean.class);
        TileMultipart tile = (TileMultipart) method.invoke(null, null, Collections.emptyList(), true);
        assertTrue(tile instanceof TileMultipartClient);
        assertSame(tile, method.invoke(null, tile, Collections.emptyList(), true));
        assertNull(tile.getWorldObj());
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void javaExamplePreparesWorldAndCoordinatesBeforeLoadingWithoutInstallation() {
        for (boolean client : new boolean[] { false, true }) {
            World world = client ? null : MinecraftServer.getServer().worldServers[0];
            BlockCoord pos = new BlockCoord(78, 200, 48);
            TileEntity installed = world == null ? null : world.getTileEntity(pos.x, pos.y, pos.z);
            TMultiPart part = new MultipartGeneratorFunctionalTest.SlottedPart();
            List<TMultiPart> parts = Collections.singletonList(part);
            TileMultipart tile = CompositeGenerationExample.createUninstalledTile(world, pos, parts, client);
            assertEquals(client, tile instanceof TileMultipartClient);
            assertSame(world, tile.getWorldObj());
            assertEquals(pos, new BlockCoord(tile));
            assertSame(tile, part.tile());
            assertSame(part, tile.partMap(3));
            assertEquals(parts, tile.jPartList());
            if (world != null) assertSame(installed, world.getTileEntity(pos.x, pos.y, pos.z));
        }
    }

    @Test
    void legacyGenerationSelectsCapabilitiesWithoutLoadingOrCopyingTiles() {
        checkStaging(CompositeGenerationFunctionalTest::legacyGenerate);
    }

    @Test
    void legacyGenerationReadsInputOnceAndPropagatesFailuresBeforeReuse() {
        checkInput(CompositeGenerationFunctionalTest::legacyGenerate);
    }

    @Test
    void legacyExactReflectionAndScalaArgumentsKeepTheCompanionPath() throws Exception {
        Object parts = JavaConversions.asScalaBuffer(Collections.<TMultiPart>emptyList()).toList();
        Method legacy = MultipartGenerator$.class
                .getMethod("generateCompositeTile", TileEntity.class, scala.collection.Iterable.class, boolean.class);
        TileMultipart tile = (TileMultipart) legacy.invoke(MultipartGenerator$.MODULE$, null, parts, true);
        assertTrue(tile instanceof TileMultipartClient);
        // GuideNH selects by argument assignability before falling back to the companion.
        for (Method method : MultipartGenerator.class.getMethods()) {
            if (method.getName().equals("generateCompositeTile") && method.getParameterCount() == 3) {
                assertFalse(method.getParameterTypes()[1].isInstance(parts));
            }
        }
    }

    private static TileMultipart legacyGenerate(TileEntity tile, Iterable<TMultiPart> parts, boolean client) {
        return MultipartGenerator$.MODULE$
                .generateCompositeTile(tile, JavaConversions.iterableAsScalaIterable(parts), client);
    }

    private static void checkStaging(Generator generate) {
        for (boolean client : new boolean[] { false, true }) {
            TMultiPart part = new MultipartGeneratorFunctionalTest.SlottedPart();
            List<TMultiPart> parts = Collections.singletonList(part);
            TileMultipart source = generate.apply(null, parts, client);
            assertEquals(client, source instanceof TileMultipartClient);
            assertTrue(source instanceof TSlottedTile);
            assertTrue(source.jPartList().isEmpty());
            assertNull(source.partMap(3));
            assertNull(part.tile());
            source.loadPartList(parts);
            source.xCoord = 78;
            source.yCoord = 200;
            source.zCoord = 48;
            source.setWorldObj(MinecraftServer.getServer().worldServers[0]);
            assertSame(source, generate.apply(source, parts, client));
            assertSame(source, generate.apply(source, Arrays.asList(part, part), client));
            assertEquals(parts, source.jPartList());
            assertSame(part, source.partMap(3));
            assertSame(source, part.tile());

            TileMultipart replacement = generate.apply(source, parts, !client);
            assertNotSame(source, replacement);
            assertEquals(!client, replacement instanceof TileMultipartClient);
            assertTrue(replacement instanceof TSlottedTile);
            assertTrue(replacement.jPartList().isEmpty());
            assertNull(replacement.partMap(3));
            assertNull(replacement.getWorldObj());
            assertEquals(0, replacement.xCoord);
            assertEquals(0, replacement.yCoord);
            assertEquals(0, replacement.zCoord);
            assertFalse(source.isInvalid());
            assertSame(source, part.tile());
            assertEquals(parts, source.jPartList());

            TileMultipart empty = generate.apply(source, Collections.emptyList(), client);
            assertNotSame(source, empty);
            assertFalse(empty instanceof TSlottedTile);
            assertTrue(empty.jPartList().isEmpty());
            assertSame(empty, generate.apply(empty, Collections.emptyList(), client));
            assertSame(empty.getClass(), generate.apply(new TileEntity(), Collections.emptyList(), client).getClass());
            replacement.loadPartList(parts);
            assertSame(replacement, part.tile());
            assertSame(part, replacement.partMap(3));
            assertEquals(parts, source.jPartList(), "Loading the replacement does not clear the old tile");
        }
    }

    private static void checkInput(Generator generate) {
        List<TMultiPart> parts = Collections.singletonList(new MultipartGeneratorFunctionalTest.SlottedPart());
        AtomicInteger iterations = new AtomicInteger();
        Iterable<TMultiPart> counted = () -> {
            iterations.incrementAndGet();
            return parts.iterator();
        };
        TileMultipart tile = generate.apply(null, counted, false);
        assertEquals(1, iterations.get());
        assertSame(tile, generate.apply(tile, counted, false));
        assertEquals(2, iterations.get(), "Even tile reuse evaluates the supplied parts");
        IllegalStateException failure = new IllegalStateException("input failed");
        Iterable<TMultiPart> broken = () -> { throw failure; };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> generate.apply(tile, broken, false)));
        assertThrows(NullPointerException.class, () -> generate.apply(tile, null, false));
        assertThrows(NullPointerException.class, () -> generate.apply(tile, Collections.singletonList(null), false));
        int before = iterations.get();
        assertThrows(
                NoSuchElementException.class,
                () -> generate.apply(new MultipartGeneratorFunctionalTest.UnregisteredTile(), counted, false));
        assertEquals(before + 1, iterations.get(), "Parts are read before an unregistered tile class is rejected");
        assertTrue(tile.jPartList().isEmpty());
    }

    private interface Generator {

        TileMultipart apply(TileEntity tile, Iterable<TMultiPart> parts, boolean client);
    }
}
