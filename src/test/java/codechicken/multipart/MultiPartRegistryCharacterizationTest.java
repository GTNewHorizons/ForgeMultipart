package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import cpw.mods.fml.relauncher.ReflectionHelper;
import scala.Option;

/**
 * Part registration needs FML's active mod container, so it cannot run headless; that half of the registry is
 * characterized in the Forge server suite instead. Converter dispatch and the Schematica reflection contract touch
 * neither FML nor the logger, so they are covered here.
 */
class MultiPartRegistryCharacterizationTest {

    @Test
    void javaFactoryLookupTracksRegistryWithoutConstructingParts() {
        checkFactoryLookup(MultiPartRegistry::getPartFactory);
    }

    @Test
    void reflectedFactoryLookupTracksRegistryWithoutConstructingParts() throws IllegalAccessException {
        checkFactoryLookup(reflectedFactoryLookup());
    }

    @SuppressWarnings("unchecked")
    private static Function<String, IPartFactory2> reflectedFactoryLookup() throws IllegalAccessException {
        Field field = ReflectionHelper
                .findField(MultiPartRegistry$.class, "codechicken$multipart$MultiPartRegistry$$typeMap");
        scala.collection.mutable.Map<String, IPartFactory2> map = (scala.collection.mutable.Map<String, IPartFactory2>) field
                .get(MultiPartRegistry$.MODULE$);
        return name -> {
            Option<IPartFactory2> factory = map.get(name);
            return factory.isEmpty() ? null : factory.get();
        };
    }

    private static void checkFactoryLookup(Function<String, IPartFactory2> lookup) {
        String name = "test:factory_lookup";
        TestFactory first = new TestFactory(new TestPart());
        TestFactory second = new TestFactory(new TestPart());
        assertNull(lookup.apply(name));
        assertNull(lookup.apply(""));
        try {
            MultiPartRegistry.typeMapBacking().put(name, first);
            assertSame(first, lookup.apply(new String(name)));
            assertNull(lookup.apply("TEST:factory_lookup"));
            MultiPartRegistry.typeMapBacking().put(name, second);
            assertSame(second, lookup.apply(name));
            MultiPartRegistry.typeMapBacking().remove(name);
            assertNull(lookup.apply(name));
            assertEquals(0, first.calls);
            assertEquals(0, second.calls);
        } finally {
            MultiPartRegistry.typeMapBacking().remove(name);
        }
    }

    @Test
    void noPartsAreLoadedBeforeRegistrationRuns() {
        assertFalse(MultiPartRegistry.loaded());
    }

    @Test
    void unknownModContainerThrowsRatherThanReturningNull() {
        assertThrows(NoSuchElementException.class, () -> MultiPartRegistry.getModContainer("test:nosuchpart"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void schematicaCanReadTheLiveScalaTypeMap() throws IllegalAccessException {
        Field moduleField = ReflectionHelper.findField(MultiPartRegistry$.class, "MODULE$");
        Field typeMapField = ReflectionHelper
                .findField(MultiPartRegistry$.class, "codechicken$multipart$MultiPartRegistry$$typeMap");
        Object module = moduleField.get(MultiPartRegistry$.class);
        scala.collection.mutable.Map<String, Object> reflectedTypeMap = (scala.collection.mutable.Map<String, Object>) typeMapField
                .get(module);

        String type = "test:schematica_reflection";
        TestPart part = new TestPart();
        TestFactory factory = new TestFactory(part);
        reflectedTypeMap.put(type, factory);
        try {
            Option<Object> reflectedFactory = reflectedTypeMap.get(type);
            assertFalse(reflectedFactory.isEmpty());
            assertSame(factory, reflectedFactory.get());
            assertSame(part, MultiPartRegistry.loadPart(type, new NBTTagCompound()));
        } finally {
            reflectedTypeMap.remove(type);
        }
    }

    @Test
    void convertersAreDispatchedPerBlockAndFirstNonNullWins() {
        Block block = new Block(Material.rock) {};
        Block other = new Block(Material.rock) {};

        RecordingConverter declines = new RecordingConverter(block, null);
        RecordingConverter accepts = new RecordingConverter(block, new TestPart());
        RecordingConverter unrelated = new RecordingConverter(other, new TestPart());

        MultiPartRegistry.registerConverter(declines);
        MultiPartRegistry.registerConverter(accepts);
        MultiPartRegistry.registerConverter(unrelated);

        assertSame(accepts.result, MultiPartRegistry.convertBlock(null, null, block));
        assertEquals(1, declines.calls, "A converter returning null must not stop the scan");
        assertEquals(1, accepts.calls);
        assertEquals(0, unrelated.calls, "Converters registered for another block must not be consulted");
    }

    @Test
    void blockWithNoConverterYieldsNull() {
        assertNull(MultiPartRegistry.convertBlock(null, null, new Block(Material.rock) {}));
    }

    @Test
    void aConverterIsRegisteredForEveryBlockItClaims() {
        Block first = new Block(Material.rock) {};
        Block second = new Block(Material.rock) {};
        TestPart part = new TestPart();

        RecordingConverter converter = new RecordingConverter(Arrays.asList(first, second), part);
        MultiPartRegistry.registerConverter(converter);

        assertSame(part, MultiPartRegistry.convertBlock(null, null, first));
        assertSame(part, MultiPartRegistry.convertBlock(null, null, second));
        assertEquals(2, converter.calls);
    }

    @Test
    void converterRegistrationCapturesBlocksAndRetainsDuplicateEntries() {
        Block original = new Block(Material.rock) {};
        Block later = new Block(Material.rock) {};
        RecordingConverter converter = new RecordingConverter(Arrays.asList(original, original), null);
        MultiPartRegistry.registerConverter(converter);
        converter.blocks.clear();
        converter.blocks.add(later);
        MultiPartRegistry.registerConverter(converter);

        assertNull(MultiPartRegistry.convertBlock(null, null, original));
        assertEquals(2, converter.calls);
        assertNull(MultiPartRegistry.convertBlock(null, null, later));
        assertEquals(3, converter.calls);
    }

    @Test
    void converterReceivesOriginalPositionAndFailureStopsDispatch() {
        Block block = new Block(Material.rock) {};
        BlockCoord pos = new BlockCoord(2, 3, 4);
        IllegalStateException failure = new IllegalStateException("converter failed");
        MultiPartRegistry.registerConverter(new IPartConverter() {

            @Override
            public Iterable<Block> blockTypes() {
                return Arrays.asList(block);
            }

            @Override
            public TMultiPart convert(World world, BlockCoord supplied) {
                assertNull(world);
                assertSame(pos, supplied);
                throw failure;
            }
        });
        RecordingConverter fallback = new RecordingConverter(block, new TestPart());
        MultiPartRegistry.registerConverter(fallback);
        assertSame(
                failure,
                assertThrows(IllegalStateException.class, () -> MultiPartRegistry.convertBlock(null, pos, block)));
        assertEquals(0, fallback.calls);
    }

    private static final class RecordingConverter implements IPartConverter {

        private final List<Block> blocks = new ArrayList<>();
        private final TMultiPart result;
        private int calls;

        private RecordingConverter(Block block, TMultiPart result) {
            this.blocks.add(block);
            this.result = result;
        }

        private RecordingConverter(List<Block> blocks, TMultiPart result) {
            this.blocks.addAll(blocks);
            this.result = result;
        }

        @Override
        public Iterable<Block> blockTypes() {
            return blocks;
        }

        @Override
        public TMultiPart convert(World world, BlockCoord pos) {
            calls++;
            return result;
        }
    }

    private static final class TestPart extends TMultiPart {

        @Override
        public String getType() {
            return "test:converted";
        }
    }

    private static final class TestFactory implements IPartFactory2 {

        private final TMultiPart part;
        private int calls;

        private TestFactory(TMultiPart part) {
            this.part = part;
        }

        @Override
        public TMultiPart createPart(String name, NBTTagCompound nbt) {
            calls++;
            return part;
        }

        @Override
        public TMultiPart createPart(String name, MCDataInput packet) {
            calls++;
            return part;
        }
    }
}
