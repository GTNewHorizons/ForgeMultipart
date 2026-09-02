package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.multipart.asm.ScalaSignature.Bytes;

class ByteCodeReaderCharacterizationTest {

    @Test
    void keepsExactReaderSurface() throws Exception {
        assertEquals(Modifier.PUBLIC, ByteCodeReader.class.getModifiers());
        assertSame(Object.class, ByteCodeReader.class.getSuperclass());
        assertEquals(0, ByteCodeReader.class.getInterfaces().length);
        assertEquals(
                signatures(
                        "advance(ILjava/lang/Object;)Ljava/lang/Object;",
                        "bc()Lcodechicken/multipart/asm/ScalaSignature$Bytes;",
                        "more()Z",
                        "pos()I",
                        "pos_$eq(I)V",
                        "readByte()B",
                        "readLong()J",
                        "readNat()I",
                        "readString(I)Ljava/lang/String;"),
                publicMethods(ByteCodeReader.class));

        Field bc = ByteCodeReader.class.getDeclaredField("bc");
        assertEquals(Modifier.PRIVATE | Modifier.FINAL, bc.getModifiers());
        assertSame(Bytes.class, bc.getType());
        Field pos = ByteCodeReader.class.getDeclaredField("pos");
        assertEquals(Modifier.PRIVATE, pos.getModifiers());
        assertSame(int.class, pos.getType());

        assertEquals(1, ByteCodeReader.class.getDeclaredConstructors().length);
        assertEquals(
                "(Lcodechicken/multipart/asm/ScalaSignature$Bytes;)V",
                Type.getConstructorDescriptor(ByteCodeReader.class.getDeclaredConstructor(Bytes.class)));
    }

    @Test
    void startsAtTheSectionOffsetAndTracksItsOwnPosition() {
        Bytes bytes = new Bytes(bytes(1, 2, 3, 4), 1, 2);
        ByteCodeReader reader = bytes.reader();

        assertSame(bytes, reader.bc());
        assertEquals(1, reader.pos());
        assertTrue(reader.more());

        assertEquals((byte) 2, reader.readByte());
        assertEquals(2, reader.pos());
        assertTrue(reader.more());

        assertEquals((byte) 3, reader.readByte());
        assertEquals(3, reader.pos());
        assertFalse(reader.more());

        reader.pos_$eq(1);
        assertTrue(reader.more());
        assertEquals((byte) 2, reader.readByte());
    }

    @Test
    void readByteKeepsSignedValues() {
        ByteCodeReader reader = new Bytes(bytes(0x80, 0xff, 0x7f), 0, 3).reader();
        assertEquals((byte) -128, reader.readByte());
        assertEquals((byte) -1, reader.readByte());
        assertEquals((byte) 127, reader.readByte());
    }

    @Test
    void readNatDecodesSevenBitGroupsAndOverflowsSilently() {
        ByteCodeReader reader = new Bytes(bytes(0x7f, 0x81, 0x00, 0xff, 0xff, 0x7f), 0, 6).reader();
        assertEquals(127, reader.readNat());
        assertEquals(128, reader.readNat());
        assertEquals(2097151, reader.readNat());

        // Five continuation groups overflow an int rather than failing.
        assertEquals(-1, new Bytes(bytes(0x8f, 0xff, 0xff, 0xff, 0x7f), 0, 5).reader().readNat());
    }

    @Test
    void readLongConsumesTheRestOfTheSectionUnsigned() {
        assertEquals(0x01ffL, new Bytes(bytes(1, 0xff), 0, 2).reader().readLong());
        assertEquals(0L, new Bytes(bytes(1, 2), 0, 0).reader().readLong());

        Bytes section = new Bytes(bytes(9, 1, 2, 9), 1, 2);
        ByteCodeReader reader = section.reader();
        assertEquals(0x0102L, reader.readLong());
        assertEquals(3, reader.pos());
    }

    @Test
    void readStringUsesTheDefaultCharsetAndClampsToTheArray() {
        ByteCodeReader reader = new Bytes("abcd".getBytes(StandardCharsets.UTF_8), 1, 3).reader();
        assertEquals("bc", reader.readString(2));
        assertEquals(3, reader.pos());

        // A section longer than its array yields the remaining bytes but still advances by the requested length.
        ByteCodeReader wide = new Bytes("abc".getBytes(StandardCharsets.UTF_8), 0, 10).reader();
        assertEquals("abc", wide.readString(5));
        assertEquals(5, wide.pos());
        assertEquals("", wide.readString(0));
    }

    @Test
    void advanceRefusesToLeaveTheSectionAndReturnsItsArgument() {
        ByteCodeReader reader = new Bytes(bytes(1, 2, 3, 4), 1, 2).reader();
        Object marker = new Object();
        assertSame(marker, reader.advance(2, marker));
        assertEquals(3, reader.pos());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> reader.advance(1, marker));
        assertEquals("Ran off the end of bytecode", error.getMessage());
        assertEquals(3, reader.pos());
        assertThrows(IllegalArgumentException.class, reader::readByte);
        assertThrows(IllegalArgumentException.class, () -> reader.readString(1));
    }

    private static byte[] bytes(int... values) {
        byte[] array = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = (byte) values[i];
        }
        return array;
    }

    private static Set<String> publicMethods(Class<?> type) {
        Set<String> actual = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                actual.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return actual;
    }

    private static Set<String> signatures(String... expected) {
        return new TreeSet<>(Arrays.asList(expected));
    }
}
