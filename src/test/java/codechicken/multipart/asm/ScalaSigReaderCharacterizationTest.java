package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import scala.Option;

class ScalaSigReaderCharacterizationTest {

    private static final String SIGNATURE_DESC = "Lscala/reflect/ScalaSignature;";

    @Test
    void keepsExactFacadeAndCompanionSurface() throws Exception {
        Set<String> expected = signatures(
                "decode(Ljava/lang/String;)[B",
                "encode([B)Ljava/lang/String;",
                "read(Lorg/objectweb/asm/tree/AnnotationNode;)Lcodechicken/multipart/asm/ScalaSignature;",
                "write(Lcodechicken/multipart/asm/ScalaSignature;Lorg/objectweb/asm/tree/AnnotationNode;)Ljava/lang/Object;",
                "ann(Lorg/objectweb/asm/tree/ClassNode;)Lscala/Option;");
        for (Class<?> type : new Class<?>[] { ScalaSigReader.class, ScalaSigReader$.class }) {
            assertEquals(Modifier.PUBLIC | Modifier.FINAL, type.getModifiers());
            assertSame(Object.class, type.getSuperclass());
            assertEquals(0, type.getInterfaces().length);
            assertEquals(expected, publicMethods(type));
        }
        for (Method method : ScalaSigReader.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()));
        }

        assertEquals(0, ScalaSigReader.class.getDeclaredFields().length);
        Field module = ScalaSigReader$.class.getDeclaredField("MODULE$");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(ScalaSigReader$.class, module.getType());
        assertSame(ScalaSigReader$.MODULE$, module.get(null));
        assertTrue(Modifier.isPrivate(ScalaSigReader$.class.getDeclaredConstructor().getModifiers()));
    }

    @Test
    void encodingDropsTheTrailingByteAndSurvivesADecodeRoundTrip() {
        byte[] payload = { 0, 1, 2, 3, 100, -1, -128, 127 };
        String encoded = ScalaSigReader.encode(payload);

        assertEquals(9, encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            assertTrue(encoded.charAt(i) <= 0x7f);
        }
        assertArrayEquals(payload, ScalaSigReader.decode(encoded));

        assertEquals("", ScalaSigReader.encode(new byte[0]));
        assertArrayEquals(new byte[0], ScalaSigReader.decode(""));

        // The dropped trailing group is empty for a short payload, but carries the high bits of a long one.
        byte[] single = { 42 };
        assertArrayEquals(single, ScalaSigReader.decode(ScalaSigReader.encode(single)));
        byte[] highBit = { 0, (byte) 0xfe };
        assertFalse(Arrays.equals(highBit, ScalaSigReader.decode(ScalaSigReader.encode(highBit))));
    }

    @Test
    void encodingLeavesItsInputAlone() {
        byte[] payload = { 1, 2, 3 };
        byte[] original = payload.clone();
        assertNotSame(payload, ScalaSigReader.decode(ScalaSigReader.encode(payload)));
        assertArrayEquals(original, payload);
    }

    @Test
    void annReturnsTheFirstScalaSignatureAnnotationOrNone() {
        ClassNode cnode = new ClassNode();
        assertFalse(ScalaSigReader.ann(cnode).isDefined());

        cnode.visibleAnnotations = new ArrayList<>();
        cnode.visibleAnnotations.add(new AnnotationNode("Lscala/reflect/ScalaLongSignature;"));
        assertFalse(ScalaSigReader.ann(cnode).isDefined());

        AnnotationNode first = new AnnotationNode(SIGNATURE_DESC);
        AnnotationNode second = new AnnotationNode(SIGNATURE_DESC);
        cnode.visibleAnnotations.add(first);
        cnode.visibleAnnotations.add(second);
        Option<AnnotationNode> found = ScalaSigReader.ann(cnode);
        assertTrue(found.isDefined());
        assertSame(first, found.get());
    }

    @Test
    void readsAndRewritesTheAnnotationValueOfAFrozenScalaClass() {
        ClassNode cnode = new ClassNode();
        new ClassReader(fixture()).accept(cnode, 0);
        AnnotationNode ann = ScalaSigReader.ann(cnode).get();
        String encoded = (String) ann.values.get(1);

        ScalaSignature sig = ScalaSigReader.read(ann);
        assertEquals(0, sig.bytes().pos());
        assertEquals(sig.bytes().arr().length, sig.bytes().len());
        assertArrayEquals(ScalaSigReader.decode(encoded), sig.bytes().arr());

        // write returns the replaced list element and re-encodes to the same string.
        Object replaced = ScalaSigReader.write(sig, ann);
        assertEquals(encoded, replaced);
        assertEquals(encoded, ann.values.get(1));
    }

    @Test
    void readFailsOnAnAnnotationWithoutAValueList() {
        assertThrows(Exception.class, () -> ScalaSigReader.read(new AnnotationNode(SIGNATURE_DESC)));

        AnnotationNode wrongType = new AnnotationNode(SIGNATURE_DESC);
        List<Object> values = new ArrayList<>();
        values.add("bytes");
        values.add(Integer.valueOf(1));
        wrongType.values = values;
        assertThrows(ClassCastException.class, () -> ScalaSigReader.read(wrongType));
    }

    private static byte[] fixture() {
        InputStream input = Objects.requireNonNull(
                ScalaSigReaderCharacterizationTest.class
                        .getResourceAsStream("/compat/ReferenceScalaEdgePart.class.b64"));
        try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
            return Base64.getMimeDecoder().decode(scanner.next());
        }
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
