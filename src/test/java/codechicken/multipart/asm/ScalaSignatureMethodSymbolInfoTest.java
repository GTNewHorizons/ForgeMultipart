package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.MethodSymbol;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;
import codechicken.multipart.asm.ScalaSignature.TMethodType;

class ScalaSignatureMethodSymbolInfoTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void returnsEvaluatedMethodTypeAndNullLiterally() throws Throwable {
        TMethodType expected = method();
        ProbeSignature sig = signature(index -> index == 37 ? expected : null, new ArrayList<>());
        assertSame(expected, info(sig.new MethodSymbol(null, null, 0, 37)));
        assertNull(info(sig.new MethodSymbol(null, null, 0, -1)));
    }

    @Test
    void readsInfoIdThenEvalOnEveryQuery() throws Throwable {
        List<String> calls = new ArrayList<>();
        TMethodType expected = method();
        ProbeSignature sig = signature(index -> expected, calls);
        MethodSymbol ref = sig.new MethodSymbol(null, null, 0, 0) {

            @Override
            public int infoId() {
                calls.add("infoId");
                return 19;
            }
        };
        assertSame(expected, info(ref));
        assertSame(expected, info(ref));
        assertEquals(Arrays.asList("infoId", "eval:19", "infoId", "eval:19"), calls);
    }

    @Test
    void changingInfoIdsAndResultsAreNotCached() throws Throwable {
        TMethodType first = method();
        TMethodType second = method();
        int[] query = { 0 };
        ProbeSignature sig = signature(index -> index == 4 ? first : second, new ArrayList<>());
        MethodSymbol ref = sig.new MethodSymbol(null, null, 0, 0) {

            @Override
            public int infoId() {
                return query[0]++ == 0 ? 4 : 9;
            }
        };
        assertSame(first, info(ref));
        assertSame(second, info(ref));
        assertEquals(2, query[0]);
    }

    @Test
    void rejectsAnEvaluatedNonMethodType() {
        ProbeSignature sig = signature(index -> "not a method type", new ArrayList<>());
        assertThrows(ClassCastException.class, () -> info(sig.new MethodSymbol(null, null, 0, 0)));
    }

    @Test
    void failuresStopAtInfoIdOrEval() {
        RuntimeException failure = new IllegalStateException("method info lookup");
        List<String> calls = new ArrayList<>();
        ProbeSignature sig = signature(index -> {
            calls.add("value");
            throw failure;
        }, calls);

        MethodSymbol idFailure = sig.new MethodSymbol(null, null, 0, 0) {

            @Override
            public int infoId() {
                calls.add("infoId");
                throw failure;
            }
        };
        assertSame(failure, assertThrows(RuntimeException.class, () -> info(idFailure)));
        assertEquals(Arrays.asList("infoId"), calls);

        calls.clear();
        MethodSymbol evalFailure = sig.new MethodSymbol(null, null, 0, 8) {

            @Override
            public int infoId() {
                calls.add("infoId");
                return 8;
            }
        };
        assertSame(failure, assertThrows(RuntimeException.class, () -> info(evalFailure)));
        assertEquals(Arrays.asList("infoId", "eval:8", "value"), calls);
    }

    @Test
    void infoDoesNotReadOtherSymbolOrMethodTypeMembers() throws Throwable {
        TMethodType expected = method();
        ProbeSignature sig = signature(index -> expected, new ArrayList<>());
        MethodSymbol ref = sig.new MethodSymbol(null, null, 0, 3) {

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
            public String full() {
                throw new AssertionError("full");
            }

            @Override
            public String jDesc() {
                throw new AssertionError("jDesc");
            }
        };
        assertSame(expected, info(ref));
    }

    @Test
    void frozenScalaCallerKeepsItsReturnDescriptorAndNullReceiverFailure() throws Throwable {
        assertEquals(TMethodType.class, FIXTURE.returnType);
        assertThrows(NullPointerException.class, () -> info(null));
    }

    private static TMethodType method() {
        return (TMethodType) java.lang.reflect.Proxy.newProxyInstance(
                TMethodType.class.getClassLoader(),
                new Class<?>[] { TMethodType.class },
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
    }

    private static TMethodType info(MethodSymbol ref) throws Throwable {
        return (TMethodType) FIXTURE.info.invoke(ref);
    }

    private static ProbeSignature signature(IntFunction<Object> values, List<String> calls) {
        return new ProbeSignature(values, calls);
    }

    private static final class ProbeSignature extends ScalaSignature {

        private final IntFunction<Object> values;
        private final List<String> calls;

        ProbeSignature(IntFunction<Object> values, List<String> calls) {
            super(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
            this.values = values;
            this.calls = calls;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T evalT(int index) {
            calls.add("eval:" + index);
            return (T) values.apply(index);
        }
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle info;
        final Class<?> returnType;

        Fixture() {
            super(ScalaSignatureMethodSymbolInfoTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaMethodSymbolInfo";
                byte[] bytes;
                try (InputStream stream = Objects.requireNonNull(getResourceAsStream("compat/" + base + ".class.b64"));
                        Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                Class<?> type = defineClass(null, bytes, 0, bytes.length);
                Object instance = type.getConstructor().newInstance();
                java.lang.reflect.Method method = type.getMethod("info", MethodSymbol.class);
                returnType = method.getReturnType();
                info = MethodHandles.publicLookup().unreflect(method).bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
