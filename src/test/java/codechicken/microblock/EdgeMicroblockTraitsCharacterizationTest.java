package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/** Executes edge-trait forwarders compiled against the untouched Scala implementation. */
class EdgeMicroblockTraitsCharacterizationTest {

    @Test
    void packsSupportedEdgesThroughTheVirtualShapeSetterExactlyOnce() throws Exception {
        Fixture f = new Fixture();
        assertEquals(29, f.part.material());
        assertEquals(0, f.part.shape());
        int writes = 0;
        for (int size = 1; size <= 7; size++) {
            for (int edge = 0; edge < 12; edge++) {
                f.part.setShape(size, edge + 15);
                byte expected = (byte) (size * 16 + edge);
                assertEquals(expected, f.part.shape());
                assertEquals(expected, f.get("writtenShape"));
                assertEquals(++writes, f.get("writes"));
                assertEquals(size, f.part.getSize());
                assertEquals(edge + 15, f.edge.getSlot());
                assertEquals(1 << (edge + 15), f.edge.getSlotMask());
                assertFalse(f.edge.conductsRedstone());
                assertEquals(29, f.part.material());
            }
        }
    }

    @Test
    void preservesTruncationAndOutOfRangeSlotBitsWithoutValidation() throws Exception {
        Fixture f = new Fixture();
        int[][] cases = { { 1, 14, -1, -1, 30 }, { 8, 15, -128, -8, 15 }, { 16, 15, 0, 0, 15 }, { 0, 31, 16, 1, 15 },
                { -1, 26, -5, -1, 26 }, { Integer.MAX_VALUE, Integer.MIN_VALUE, -15, -1, 16 } };
        for (int[] value : cases) {
            f.part.setShape(value[0], value[1]);
            assertEquals((byte) value[2], f.part.shape());
            assertEquals(value[3], f.part.getSize());
            assertEquals(value[4], f.edge.getSlot());
        }
        assertEquals(cases.length, f.get("writes"));
    }

    @Test
    void slotDecodingUsesTheVirtualGetterAndKeepsIntegerOverflow() throws Exception {
        Fixture f = new Fixture();
        f.set("overrideShape", boolean.class, true);
        int[][] cases = { { -16, -1 }, { 0, 15 }, { 17, 32 }, { Integer.MAX_VALUE, Integer.MIN_VALUE + 14 } };
        int reads = 0;
        for (int[] value : cases) {
            f.set("selectedShape", int.class, value[0]);
            assertEquals(value[1], f.edge.getSlot());
            assertEquals(++reads, f.get("reads"));
            assertEquals(1 << value[1], f.edge.getSlotMask());
            assertEquals(++reads, f.get("reads"));
        }
        assertEquals(0, f.get("writes"));
        assertEquals(0, f.part.shape());
    }

    private static final class Fixture extends ClassLoader {

        final Microblock part;
        final EdgeMicroblock edge;

        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            InputStream input = Objects
                    .requireNonNull(getClass().getResourceAsStream("/compat/ReferenceScalaEdgeMicroblock.class.b64"));
            byte[] bytes;
            try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                bytes = Base64.getMimeDecoder().decode(scanner.next());
            }
            part = (Microblock) defineClass(null, bytes, 0, bytes.length).getConstructor().newInstance();
            edge = (EdgeMicroblock) part;
        }

        Object get(String name) throws Exception {
            return part.getClass().getMethod(name).invoke(part);
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }
    }
}
