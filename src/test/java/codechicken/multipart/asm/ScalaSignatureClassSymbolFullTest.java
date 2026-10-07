package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import codechicken.multipart.asm.ScalaSignature.ClassSymbolRef;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;

class ScalaSignatureClassSymbolFullTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void concreteClassAndObjectSymbolsJoinTheirOwnerAndName() {
        ScalaSignature sig = signature();
        assertEquals("pkg.Owner.Name", sig.new ClassSymbol("Name", sig.new ExternalSymbol("pkg.Owner"), 0, 0).full());
        assertEquals("<no symbol>.Module", sig.new ObjectSymbol("Module", sig.NoSymbol(), 0, 0).full());
        assertEquals("..a,b(c)", sig.new ClassSymbol("a,b(c)", sig.new ExternalSymbol("."), 0, 0).full());
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
        assertEquals("null.Name", sig.new ClassSymbol("Name", nullFull, 0, 0).full());
        assertEquals("pkg.Owner.null", sig.new ClassSymbol(null, sig.new ExternalSymbol("pkg.Owner"), 0, 0).full());
    }

    @Test
    void frozenScalaImplementorReadsOwnerFullAndNameInOrderOnEveryQuery() throws Throwable {
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
        ClassSymbolRef ref = ref(sig, () -> {
            calls.add("owner");
            return owner;
        }, () -> {
            calls.add("name");
            return "Name" + query[0]++;
        });
        assertEquals("Owner0.Name0", full(ref));
        assertEquals("Owner1.Name1", full(ref));
        assertEquals(Arrays.asList("owner", "owner.full", "name", "owner", "owner.full", "name"), calls);
    }

    @Test
    void failuresStopAtTheSelectedOwnerOrNameRead() throws Throwable {
        ScalaSignature sig = signature();
        List<String> order = Arrays.asList("owner", "owner.full", "name");
        RuntimeException failure = new IllegalStateException("class symbol full getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            SymbolRef owner = sig.new ExternalSymbol("unused") {

                @Override
                public String full() {
                    return read(calls, "owner.full", "Owner", failing, failure);
                }
            };
            ClassSymbolRef ref = ref(
                    sig,
                    () -> read(calls, "owner", owner, failing, failure),
                    () -> read(calls, "name", "Name", failing, failure));
            assertSame(failure, assertThrows(RuntimeException.class, () -> full(ref)));
            assertEquals(order.subList(0, order.indexOf(failing) + 1), calls);
        }
    }

    @Test
    void nullOwnerFailsBeforeReadingTheName() throws Throwable {
        ScalaSignature sig = signature();
        int[] nameReads = { 0 };
        ClassSymbolRef ref = ref(sig, () -> null, () -> {
            nameReads[0]++;
            return "Name";
        });
        assertThrows(NullPointerException.class, () -> full(ref));
        assertEquals(0, nameReads[0]);
    }

    @Test
    void fullDoesNotReadOwnerStringOrOtherSymbolMembers() {
        ScalaSignature sig = signature();
        SymbolRef owner = sig.new ExternalSymbol("Owner") {

            @Override
            public String toString() {
                throw new AssertionError("owner.toString");
            }
        };
        ClassSymbolRef ref = sig.new ClassSymbol("Name", owner, 0, 0) {

            @Override
            public int flags() {
                throw new AssertionError("flags");
            }

            @Override
            public int infoId() {
                throw new AssertionError("infoId");
            }

            @Override
            public String toString() {
                throw new AssertionError("toString");
            }
        };
        assertEquals("Owner.Name", ref.full());
    }

    @Test
    void legacyStaticTraitBridgeKeepsItsDescriptorAndNullReceiverFailure() throws Throwable {
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$ClassSymbolRef$class"),
                "full",
                MethodType.methodType(String.class, ClassSymbolRef.class));
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        SymbolRef owner = sig.new ExternalSymbol("unused") {

            @Override
            public String full() {
                calls.add("owner.full");
                return "Owner";
            }
        };
        ClassSymbolRef ref = (ClassSymbolRef) Proxy.newProxyInstance(
                ClassSymbolRef.class.getClassLoader(),
                new Class<?>[] { ClassSymbolRef.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getName().equals("owner")) return owner;
                    if (method.getName().equals("name")) return "Name";
                    throw new AssertionError(method.getName());
                });
        assertEquals("Owner.Name", (String) bridge.invokeExact(ref));
        assertEquals(Arrays.asList("owner", "owner.full", "name"), calls);
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static <T> T read(List<String> calls, String name, T value, String failing, RuntimeException failure) {
        calls.add(name);
        if (name.equals(failing)) throw failure;
        return value;
    }

    private static ClassSymbolRef ref(ScalaSignature sig, Supplier<SymbolRef> owners, Supplier<String> names)
            throws Throwable {
        return (ClassSymbolRef) FIXTURE.ref.invoke(sig, owners, names);
    }

    private static String full(ClassSymbolRef ref) throws Throwable {
        return (String) FIXTURE.full.invoke(ref);
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle ref;
        final MethodHandle full;

        Fixture() {
            super(ScalaSignatureClassSymbolFullTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaClassSymbolFull";
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
                        .unreflect(type.getMethod("ref", ScalaSignature.class, Supplier.class, Supplier.class))
                        .bindTo(instance);
                full = MethodHandles.publicLookup().unreflect(type.getMethod("full", ClassSymbolRef.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
