package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry.IPartConverter;

/**
 * Part registration needs FML's active mod container, so it cannot run headless; that half of the registry is
 * characterized in the Forge server suite instead. Converter registration and dispatch touch neither FML nor the
 * logger, so they are covered here.
 */
class MultiPartRegistryCharacterizationTest {

    @Test
    void noPartsAreLoadedBeforeRegistrationRuns() {
        assertFalse(MultiPartRegistry.loaded());
    }

    @Test
    void unknownModContainerThrowsRatherThanReturningNull() {
        assertThrows(NoSuchElementException.class, () -> MultiPartRegistry.getModContainer("test:nosuchpart"));
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
}
