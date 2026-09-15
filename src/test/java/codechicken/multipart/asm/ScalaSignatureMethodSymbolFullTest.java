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
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.MethodSymbol;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;
import codechicken.multipart.asm.ScalaSignature.TMethodType;

class ScalaSignatureMethodSymbolFullTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void methodSymbolsJoinTheirOwnerAndName() {
        ScalaSignature sig = signature();
        assertEquals(
                "pkg.Owner.method",
                sig.new MethodSymbol("method", sig.new ExternalSymbol("pkg.Owner"), 0, 0).full());
        assertEquals("<no symbol>.<init>", sig.new MethodSymbol("<init>", sig.NoSymbol(), 0, 0).full());
        assertEquals("..a,b(c)", sig.new MethodSymbol("a,b(c)", sig.new ExternalSymbol("."), 0, 0).full());
    }

    @Test
    void nullFullNameAndNameAreFormattedLiterally() {
        ScalaSignature sig = signature();
        SymbolRef nullFull = sig.new ExternalSymbol("unused") {

            @Override
            public String full() {
                return null;
            }
        };
        assertEquals("null.method", sig.new MethodSymbol("method", nullFull, 0, 0).full());
        assertEquals("pkg.Owner.null", sig.new MethodSymbol(null, sig.new ExternalSymbol("pkg.Owner"), 0, 0).full());
    }

    @Test
    void virtualOwnerFullAndNameAreReadInOrderOnEveryFrozenCallerQuery() throws Throwable {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        int[] query = { 0 };
        SymbolRef owner = sig.new ExternalSymbol("unused") {

            @Override
            public String full() {
                calls.add("owner.full");
                return "Owner" + query[0];
            }
        };
        MethodSymbol ref = ref(sig, () -> {
            calls.add("owner");
            return owner;
        }, () -> {
            calls.add("name");
            return "method" + query[0]++;
        });
        assertEquals("Owner0.method0", full(ref));
        assertEquals("Owner1.method1", full(ref));
        assertEquals(Arrays.asList("owner", "owner.full", "name", "owner", "owner.full", "name"), calls);
    }

    @Test
    void failuresStopAtTheSelectedOwnerOrNameRead() throws Throwable {
        ScalaSignature sig = signature();
        List<String> order = Arrays.asList("owner", "owner.full", "name");
        RuntimeException failure = new IllegalStateException("method symbol full getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            SymbolRef owner = sig.new ExternalSymbol("unused") {

                @Override
                public String full() {
                    return read(calls, "owner.full", "Owner", failing, failure);
                }
            };
            MethodSymbol ref = ref(
                    sig,
                    () -> read(calls, "owner", owner, failing, failure),
                    () -> read(calls, "name", "method", failing, failure));
            assertSame(failure, assertThrows(RuntimeException.class, () -> full(ref)));
            assertEquals(order.subList(0, order.indexOf(failing) + 1), calls);
        }
    }

    @Test
    void nullOwnerFailsBeforeReadingTheName() throws Throwable {
        ScalaSignature sig = signature();
        int[] nameReads = { 0 };
        MethodSymbol ref = ref(sig, () -> null, () -> {
            nameReads[0]++;
            return "method";
        });
        assertThrows(NullPointerException.class, () -> full(ref));
        assertEquals(0, nameReads[0]);
    }

    @Test
    void fullDoesNotReadOwnerStringOrDerivedMethodMembers() {
        ScalaSignature sig = signature();
        SymbolRef owner = sig.new ExternalSymbol("Owner") {

            @Override
            public String toString() {
                throw new AssertionError("owner.toString");
            }
        };
        MethodSymbol ref = sig.new MethodSymbol("method", owner, 0, 0) {

            @Override
            public int flags() {
                throw new AssertionError("flags");
            }

            @Override
            public int infoId() {
                throw new AssertionError("infoId");
            }

            @Override
            public TMethodType info() {
                throw new AssertionError("info");
            }

            @Override
            public String jDesc() {
                throw new AssertionError("jDesc");
            }

            @Override
            public String toString() {
                throw new AssertionError("toString");
            }
        };
        assertEquals("Owner.method", ref.full());
    }

    @Test
    void frozenScalaCallerKeepsItsDescriptorAndNullReceiverFailure() throws Throwable {
        ScalaSignature sig = signature();
        MethodSymbol ref = sig.new MethodSymbol("method", sig.new ExternalSymbol("Owner"), 0, 0);
        assertEquals("Owner.method", full(ref));
        assertThrows(NullPointerException.class, () -> full(null));
    }

    private static <T> T read(List<String> calls, String name, T value, String failing, RuntimeException failure) {
        calls.add(name);
        if (name.equals(failing)) throw failure;
        return value;
    }

    private static MethodSymbol ref(ScalaSignature sig, Supplier<SymbolRef> owners, Supplier<String> names)
            throws Throwable {
        return sig.new MethodSymbol(null, null, 0, 0) {

            @Override
            public String name() {
                return names.get();
            }

            @Override
            public SymbolRef owner() {
                return owners.get();
            }
        };
    }

    private static String full(MethodSymbol ref) throws Throwable {
        return (String) FIXTURE.full.invoke(ref);
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle full;

        Fixture() {
            super(ScalaSignatureMethodSymbolFullTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaMethodSymbolFull";
                byte[] bytes;
                try (InputStream stream = Objects.requireNonNull(getResourceAsStream("compat/" + base + ".class.b64"));
                        Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
                Class<?> type = loadClass("codechicken.multipart.compat." + base);
                Object instance = type.getConstructor().newInstance();
                full = MethodHandles.publicLookup().unreflect(type.getMethod("full", MethodSymbol.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
