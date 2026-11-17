package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.TypeRef;
import codechicken.multipart.asm.ScalaSignature.TypeRefType;
import scala.collection.JavaConversions;

class ScalaSignatureAppliedTypeDescriptorTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void arrayBranchReadsOneNameAndTheFirstArgumentDescriptor() throws Throwable {
        ScalaSignature sig = signature();
        int[] reads = { 0, 0 };
        TypeRef argument = descriptor(sig, () -> {
            reads[1]++;
            return "I";
        });
        TypeRefType ref = ref(sig, () -> {
            reads[0]++;
            return "scala.Array";
        }, scalaList(argument));
        assertEquals("[I", descriptor(ref));
        assertArrayEquals(new int[] { 1, 1 }, reads);
    }

    @Test
    void fallbackRereadsVirtualNameThroughTheBaseDescriptor() throws Throwable {
        ScalaSignature sig = signature();
        String[][] names = { { "pkg.First", "scala.Int", null, "I" },
                { "scala.Int", "pkg.Second", "pkg.Final", "Lpkg/Final;" },
                { null, "pkg.Reference", "scala.AnyRef", "Ljava/lang/Object;" } };
        int[] expectedReads = { 2, 3, 3 };
        for (int i = 0; i < names.length; i++) {
            String[] c = names[i];
            int[] reads = { 0 };
            TypeRefType ref = ref(sig, () -> c[reads[0]++], null);
            assertEquals(c[3], descriptor(ref));
            assertEquals(expectedReads[i], reads[0]);
        }
    }

    @Test
    void repeatedQueriesDoNotCacheBranchNamesOrArgumentDescriptors() throws Throwable {
        ScalaSignature sig = signature();
        int[] reads = { 0, 0 };
        TypeRef argument = descriptor(sig, () -> Integer.toString(++reads[1]));
        TypeRefType ref = ref(sig, () -> {
            reads[0]++;
            return "scala.Array";
        }, scalaList(argument));
        assertEquals("[1", descriptor(ref));
        assertEquals("[2", descriptor(ref));
        assertArrayEquals(new int[] { 2, 2 }, reads);
    }

    @Test
    void arrayBranchPreservesNullAndMalformedArgumentBehavior() throws Throwable {
        ScalaSignature sig = signature();
        assertEquals("[null", descriptor(ref(sig, () -> "scala.Array", scalaList(descriptor(sig, () -> null)))));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> descriptor(ref(sig, () -> "scala.Array", scalaList(new TypeRef[0]))));
        assertThrows(NullPointerException.class, () -> descriptor(ref(sig, () -> "scala.Array", null)));
        assertThrows(
                NullPointerException.class,
                () -> descriptor(ref(sig, () -> "scala.Array", scalaList((TypeRef) null))));
    }

    @Test
    void failuresStopAtTheSelectedNameOrDescriptorRead() throws Throwable {
        ScalaSignature sig = signature();
        RuntimeException failure = new IllegalStateException("applied descriptor getter");
        int[] reads = { 0 };
        TypeRefType firstName = ref(sig, () -> { throw failure; }, null);
        assertSame(failure, assertThrows(RuntimeException.class, () -> descriptor(firstName)));
        TypeRefType secondName = ref(sig, () -> {
            if (++reads[0] == 2) throw failure;
            return "pkg.Name";
        }, null);
        assertSame(failure, assertThrows(RuntimeException.class, () -> descriptor(secondName)));
        assertEquals(2, reads[0]);
        TypeRefType argument = ref(sig, () -> "scala.Array", scalaList(descriptor(sig, () -> { throw failure; })));
        assertSame(failure, assertThrows(RuntimeException.class, () -> descriptor(argument)));
    }

    @Test
    void nonArrayFallbackNeverTouchesTheArgumentList() throws Throwable {
        ScalaSignature sig = signature();
        assertEquals("Ljava/lang/String;", descriptor(ref(sig, () -> "java.lang.String", null)));
    }

    @Test
    void retainedMethodTypeViewUsesTheSameAppliedReturnDescriptor() throws Throwable {
        ScalaSignature sig = signature();
        TypeRefType ref = ref(sig, () -> "scala.Array", scalaList(descriptor(sig, () -> "J")));
        assertEquals("[J", ref.jDesc());
        assertTrue(ref.params().isEmpty());
        assertSame(ref, ref.returnType());
    }

    private static TypeRef descriptor(ScalaSignature sig, Supplier<String> value) {
        return sig.new ThisType(null) {

            @Override
            public String jDesc() {
                return value.get();
            }
        };
    }

    private static TypeRefType ref(ScalaSignature sig, Supplier<String> name,
            scala.collection.immutable.List<TypeRef> arguments) throws Throwable {
        return (TypeRefType) FIXTURE.ref.invoke(sig, name, arguments);
    }

    private static String descriptor(TypeRefType ref) throws Throwable {
        return (String) FIXTURE.descriptor.invoke(ref);
    }

    @SafeVarargs
    private static <T> scala.collection.immutable.List<T> scalaList(T... values) {
        return JavaConversions.asScalaBuffer(Arrays.asList(values)).toList();
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle ref;
        final MethodHandle descriptor;

        Fixture() {
            super(ScalaSignatureAppliedTypeDescriptorTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaAppliedTypeDescriptor";
                for (String suffix : new String[] { "", "$$anon$1" }) {
                    byte[] bytes;
                    try (InputStream stream = Objects
                            .requireNonNull(getResourceAsStream("compat/" + base + suffix + ".class.b64"));
                            Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                        bytes = Base64.getMimeDecoder().decode(scanner.next());
                    }
                    defineClass(null, bytes, 0, bytes.length);
                }
                Class<?> type = loadClass("codechicken.multipart.compat." + base);
                Object instance = type.getConstructor().newInstance();
                ref = MethodHandles.publicLookup()
                        .unreflect(
                                type.getMethod(
                                        "ref",
                                        ScalaSignature.class,
                                        Supplier.class,
                                        scala.collection.immutable.List.class))
                        .bindTo(instance);
                descriptor = MethodHandles.publicLookup().unreflect(type.getMethod("descriptor", TypeRefType.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
