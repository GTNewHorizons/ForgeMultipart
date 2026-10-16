package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.MethodSymbol;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;
import codechicken.multipart.asm.ScalaSignature.TMethodType;

class ScalaSignatureMethodSymbolDescriptorTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void returnsMethodDescriptorsLiterally() throws Throwable {
        ScalaSignature sig = signature();
        for (String value : new String[] { "()V", "(IJ)Ljava/lang/String;", "", "invalid" }) {
            assertEquals(value, descriptor(ref(sig, () -> method(() -> value))));
        }
        assertNull(descriptor(ref(sig, () -> method(() -> null))));
    }

    @Test
    void frozenScalaBridgeReadsInfoThenDescriptorOnEveryQuery() throws Throwable {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        int[] query = { 0 };
        TMethodType info = method(() -> {
            calls.add("jDesc");
            return "descriptor" + query[0]++;
        });
        MethodSymbol ref = ref(sig, () -> {
            calls.add("info");
            return info;
        });
        assertEquals("descriptor0", descriptor(ref));
        assertEquals("descriptor1", descriptor(ref));
        assertEquals(Arrays.asList("info", "jDesc", "info", "jDesc"), calls);
    }

    @Test
    void changingInfoResultsAreNotCached() throws Throwable {
        ScalaSignature sig = signature();
        int[] query = { 0 };
        MethodSymbol ref = ref(sig, () -> {
            int value = query[0]++;
            return method(() -> Integer.toString(value));
        });
        assertEquals("0", descriptor(ref));
        assertEquals("1", descriptor(ref));
        assertEquals(2, query[0]);
    }

    @Test
    void nullInfoKeepsItsNullPointerFailure() throws Throwable {
        ScalaSignature sig = signature();
        assertThrows(NullPointerException.class, () -> descriptor(ref(sig, () -> null)));
    }

    @Test
    void failuresStopAtInfoOrDescriptorRead() throws Throwable {
        ScalaSignature sig = signature();
        RuntimeException failure = new IllegalStateException("method descriptor getter");
        List<String> calls = new ArrayList<>();
        MethodSymbol infoFailure = ref(sig, () -> {
            calls.add("info");
            throw failure;
        });
        assertSame(failure, assertThrows(RuntimeException.class, () -> descriptor(infoFailure)));
        assertEquals(Arrays.asList("info"), calls);

        calls.clear();
        MethodSymbol descriptorFailure = ref(sig, () -> {
            calls.add("info");
            return method(() -> {
                calls.add("jDesc");
                throw failure;
            });
        });
        assertSame(failure, assertThrows(RuntimeException.class, () -> descriptor(descriptorFailure)));
        assertEquals(Arrays.asList("info", "jDesc"), calls);
    }

    @Test
    void descriptorDoesNotReadMethodTypeShapeOrSymbolFields() {
        ScalaSignature sig = signature();
        TMethodType info = method(() -> "()V");
        MethodSymbol ref = sig.new MethodSymbol(null, null, 0, 0) {

            @Override
            public TMethodType info() {
                return info;
            }

            @Override
            public String name() {
                throw new AssertionError("name");
            }

            @Override
            public SymbolRef owner() {
                throw new AssertionError("owner");
            }

            @Override
            public int flags() {
                throw new AssertionError("flags");
            }

            @Override
            public int infoId() {
                throw new AssertionError("infoId");
            }
        };
        assertEquals("()V", ref.jDesc());
    }

    @Test
    void frozenScalaCallerKeepsItsDescriptorAndNullReceiverFailure() throws Throwable {
        ScalaSignature sig = signature();
        MethodSymbol ref = ref(sig, () -> method(() -> "()I"));
        assertEquals("()I", descriptor(ref));
        assertThrows(NullPointerException.class, () -> descriptor(null));
    }

    private static TMethodType method(Supplier<String> descriptors) {
        return (TMethodType) Proxy.newProxyInstance(
                TMethodType.class.getClassLoader(),
                new Class<?>[] { TMethodType.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("jDesc")) return descriptors.get();
                    throw new AssertionError(method.getName());
                });
    }

    private static MethodSymbol ref(ScalaSignature sig, Supplier<TMethodType> infos) throws Throwable {
        return (MethodSymbol) FIXTURE.ref.invoke(sig, infos);
    }

    private static String descriptor(MethodSymbol ref) throws Throwable {
        return (String) FIXTURE.descriptor.invoke(ref);
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle ref;
        final MethodHandle descriptor;

        Fixture() {
            super(ScalaSignatureMethodSymbolDescriptorTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaMethodSymbolDescriptor";
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
                        .unreflect(type.getMethod("ref", ScalaSignature.class, Supplier.class)).bindTo(instance);
                descriptor = MethodHandles.publicLookup().unreflect(type.getMethod("descriptor", MethodSymbol.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
