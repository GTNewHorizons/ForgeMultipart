package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/** Executes corner-trait forwarders compiled against the untouched Scala implementation. */
class CornerMicroblockTraitsCharacterizationTest {

    @Test
    void packsSupportedCornersThroughTheVirtualShapeSetterExactlyOnce() throws Exception {
        Fixture f = new Fixture();
        assertEquals(29, f.part.material());
        assertEquals(0, f.part.shape());
        int writes = 0;
        for (int size = 1; size <= 7; size++) {
            for (int corner = 0; corner < 8; corner++) {
                f.part.setShape(size, corner + 7);
                byte expected = (byte) (size * 16 + corner);
                assertEquals(expected, f.part.shape());
                assertEquals(expected, f.get("writtenShape"));
                assertEquals(++writes, f.get("writes"));
                assertEquals(size, f.part.getSize());
                assertEquals(corner + 7, f.corner.getSlot());
                assertEquals(1 << (corner + 7), f.corner.getSlotMask());
                assertEquals(29, f.part.material());
            }
        }
    }

    @Test
    void preservesTruncationAndOutOfRangeSlotBitsWithoutValidation() throws Exception {
        Fixture f = new Fixture();
        int[][] cases = { { 1, 6, -1, -1, 22 }, { 8, 7, -128, -8, 7 }, { 16, 7, 0, 0, 7 }, { 0, 23, 16, 1, 7 },
                { -1, 14, -9, -1, 14 }, { Integer.MAX_VALUE, Integer.MIN_VALUE, -7, -1, 16 } };
        for (int[] value : cases) {
            f.part.setShape(value[0], value[1]);
            assertEquals((byte) value[2], f.part.shape());
            assertEquals(value[3], f.part.getSize());
            assertEquals(value[4], f.corner.getSlot());
        }
        assertEquals(cases.length, f.get("writes"));
    }

    @Test
    void slotDecodingUsesTheVirtualGetterAndKeepsIntegerOverflow() throws Exception {
        Fixture f = new Fixture();
        f.set("overrideShape", boolean.class, true);
        int[][] cases = { { -8, -1 }, { 0, 7 }, { 25, 32 }, { Integer.MAX_VALUE, Integer.MIN_VALUE + 6 } };
        int reads = 0;
        for (int[] value : cases) {
            f.set("selectedShape", int.class, value[0]);
            assertEquals(value[1], f.corner.getSlot());
            assertEquals(++reads, f.get("reads"));
            assertEquals(1 << value[1], f.corner.getSlotMask());
            assertEquals(++reads, f.get("reads"));
        }
        assertEquals(0, f.get("writes"));
        assertEquals(0, f.part.shape());
    }

    private static final class Fixture extends ClassLoader {

        final Microblock part;
        final CornerMicroblock corner;

        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            InputStream input = Objects
                    .requireNonNull(getClass().getResourceAsStream("/compat/ReferenceScalaCornerMicroblock.class.b64"));
            byte[] bytes;
            try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                bytes = Base64.getMimeDecoder().decode(scanner.next());
            }
            part = (Microblock) defineClass(null, bytes, 0, bytes.length).getConstructor().newInstance();
            corner = (CornerMicroblock) part;
        }

        Object get(String name) throws Exception {
            return part.getClass().getMethod(name).invoke(part);
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }
    }
}
