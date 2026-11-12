package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class ByteCodecsCharacterizationTest {

    @Test
    void keepsExactFacadeAndCompanionSurface() throws Exception {
        Set<String> expected = new TreeSet<>(
                Arrays.asList(
                        "avoidZero([B)[B",
                        "regenerateZero([B)I",
                        "encode8to7([B)[B",
                        "decode7to8([BI)I",
                        "encode([B)[B",
                        "decode([B)I"));
        for (Class<?> type : new Class<?>[] { ByteCodecs.class, ByteCodecs$.class }) {
            assertEquals(Modifier.PUBLIC | Modifier.FINAL, type.getModifiers());
            assertSame(Object.class, type.getSuperclass());
            assertEquals(0, type.getInterfaces().length);
            Set<String> actual = new TreeSet<>();
            for (Method method : type.getDeclaredMethods()) {
                assertEquals(Modifier.PUBLIC | (type == ByteCodecs.class ? Modifier.STATIC : 0), method.getModifiers());
                actual.add(method.getName() + Type.getMethodDescriptor(method));
            }
            assertEquals(expected, actual);
        }
        assertEquals(0, ByteCodecs.class.getDeclaredFields().length);
        assertEquals(1, ByteCodecs$.class.getDeclaredFields().length);
        Field module = ByteCodecs$.class.getDeclaredField("MODULE$");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(ByteCodecs$.class, module.getType());
        assertSame(ByteCodecs$.MODULE$, module.get(null));
        assertTrue(Modifier.isPrivate(ByteCodecs$.class.getDeclaredConstructor().getModifiers()));
    }

    @Test
    void zeroEscapingPreservesSignedByteAndInPlaceSemantics() {
        byte[] input = bytes(0, 1, 126, 127, 128, 255);
        byte[] original = input.clone();
        byte[] escaped = ByteCodecs.avoidZero(input);
        assertArrayEquals(bytes(1, 2, 127, 192, 128, 129, 0), escaped);
        assertArrayEquals(original, input);
        assertNotSame(input, escaped);
        assertArrayEquals(escaped, ByteCodecs$.MODULE$.avoidZero(input));
        assertEquals(6, ByteCodecs.regenerateZero(escaped));
        assertArrayEquals(bytes(0, 1, 126, 127, 128, 127, 0), escaped);

        for (int value = 0; value < 256; value++) {
            byte[] expected = value == 127 ? bytes(192, 128) : bytes(value + 1);
            assertArrayEquals(expected, ByteCodecs.avoidZero(bytes(value)));
            byte[] encoded = expected.clone();
            if (value == 191) {
                assertThrows(ArrayIndexOutOfBoundsException.class, () -> ByteCodecs$.MODULE$.regenerateZero(encoded));
                assertArrayEquals(expected, encoded);
                continue;
            }
            assertEquals(1, ByteCodecs$.MODULE$.regenerateZero(encoded));
            assertEquals((byte) (value == 255 ? 127 : value), encoded[0]);
        }
        byte[] nonEscape = bytes(192, 1, 0);
        assertEquals(3, ByteCodecs.regenerateZero(nonEscape));
        assertArrayEquals(bytes(191, 0, 127), nonEscape);
    }

    @Test
    void goldenPackingAndRoundTripsCoverEveryByteAndRemainder() {
        assertArrayEquals(bytes(1, 4, 12, 0), ByteCodecs.encode8to7(bytes(1, 2, 3)));
        assertArrayEquals(bytes(2, 5, 13, 1), ByteCodecs.encode(bytes(1, 2, 3)));
        byte[] documented = bytes(2, 5, 13, 1);
        assertEquals(4, ByteCodecs.decode(documented));
        assertArrayEquals(bytes(1, 2, 3, 0), documented);
        for (int value = 0; value < 256; value++) checkPacking(bytes(value));
        Random random = new Random(0xB17C0DEL);
        for (int length = 0; length <= 128; length++) {
            byte[] input = new byte[length];
            checkPacking(input);
            Arrays.fill(input, (byte) 255);
            checkPacking(input);
            random.nextBytes(input);
            checkPacking(input);
        }
    }

    @Test
    void decodingUsesOnlyTheRequestedPrefixAndKeepsSignedShifts() {
        byte[] prefix = bytes(1, 4, 12, 0, 55, 66, 77);
        assertEquals(4, ByteCodecs.decode7to8(prefix, 4));
        assertArrayEquals(bytes(1, 2, 3, 0, 55, 66, 77), prefix);
        byte[] signedTail = bytes(0, 128);
        assertEquals(2, ByteCodecs.decode7to8(signedTail, 2));
        assertArrayEquals(bytes(0, 192), signedTail);
        byte[] signedBlock = bytes(0, 128, 128, 128, 128, 128, 128, 128);
        assertEquals(7, ByteCodecs$.MODULE$.decode7to8(signedBlock, 8));
        assertArrayEquals(bytes(0, 192, 224, 240, 248, 252, 254, 128), signedBlock);
    }

    @Test
    void malformedInputKeepsPartialMutationAndUncheckedFailures() {
        byte[] truncatedEscape = bytes(1, 192);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ByteCodecs.regenerateZero(truncatedEscape));
        assertArrayEquals(bytes(0, 192), truncatedEscape);
        byte[] truncatedDecode = bytes(1, 192);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ByteCodecs.decode(truncatedDecode));
        assertArrayEquals(bytes(0, 192), truncatedDecode);
        byte[] excessiveLength = bytes(7, 3);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ByteCodecs.decode7to8(excessiveLength, 3));
        assertArrayEquals(bytes(135, 3), excessiveLength);
        assertEquals(0, ByteCodecs.decode7to8(null, 0));
        assertEquals(0, ByteCodecs.decode7to8(null, -2));
        assertEquals(-1, ByteCodecs.decode7to8(null, -3));
        assertThrows(NullPointerException.class, () -> ByteCodecs.encode(null));
        assertThrows(NullPointerException.class, () -> ByteCodecs.decode(null));
    }

    private static void checkPacking(byte[] input) {
        byte[] original = input.clone();
        byte[] expected = new byte[(input.length * 8 + 6) / 7];
        // Independent bit-by-bit oracle for the reference's unrolled byte-packing algorithm.
        for (int bit = 0; bit < input.length * 8; bit++) {
            expected[bit / 7] |= ((input[bit / 8] >>> (bit % 8)) & 1) << (bit % 7);
        }
        byte[] packed = ByteCodecs.encode8to7(input);
        assertArrayEquals(expected, packed);
        assertArrayEquals(packed, ByteCodecs$.MODULE$.encode8to7(input));
        assertArrayEquals(original, input);
        assertNotSame(input, packed);
        byte[] decoded = packed.clone();
        int decodedLength = ByteCodecs.decode7to8(decoded, packed.length);
        assertEquals(input.length + (input.length % 7 == 0 ? 0 : 1), decodedLength);
        assertArrayEquals(Arrays.copyOf(input, decodedLength), Arrays.copyOf(decoded, decodedLength));
        assertArrayEquals(
                Arrays.copyOfRange(packed, decodedLength, packed.length),
                Arrays.copyOfRange(decoded, decodedLength, decoded.length));
        byte[] escaped = ByteCodecs.encode(input);
        assertArrayEquals(ByteCodecs.avoidZero(expected), escaped);
        assertArrayEquals(escaped, ByteCodecs$.MODULE$.encode(input));
        assertEquals(decodedLength, ByteCodecs$.MODULE$.decode(escaped));
        assertArrayEquals(Arrays.copyOf(input, decodedLength), Arrays.copyOf(escaped, decodedLength));
        assertArrayEquals(original, input);
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (byte) values[i];
        return result;
    }
}
