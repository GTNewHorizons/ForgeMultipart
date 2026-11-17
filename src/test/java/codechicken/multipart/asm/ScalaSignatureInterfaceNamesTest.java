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
import codechicken.multipart.asm.ScalaSignature.ClassType;
import codechicken.multipart.asm.ScalaSignature.TypeRef;
import scala.collection.JavaConversions;
import scala.collection.immutable.Nil$;

class ScalaSignatureInterfaceNamesTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void classAndObjectSymbolsSkipTheParentAndKeepInterfaceOrder() throws Throwable {
        ScalaSignature sig = signature();
        TypeRef parent = named(sig, () -> { throw new AssertionError("Parent name must not be read"); });
        ClassType info = classType(
                sig,
                scalaList(
                        parent,
                        sig.new ThisType(sig.new ExternalSymbol("pkg.First")),
                        sig.new ThisType(sig.new ExternalSymbol("scala.AnyRef")),
                        sig.new SingleType(null, sig.new ExternalSymbol("pkg.Module"))));
        ClassSymbolRef klass = sig.new ClassSymbol("unused", null, 0, -1) {

            @Override
            public ClassType info() {
                return info;
            }
        };
        ClassSymbolRef object = sig.new ObjectSymbol("unused", null, 0, -1) {

            @Override
            public ClassType info() {
                return info;
            }
        };
        for (ClassSymbolRef symbol : new ClassSymbolRef[] { klass, object }) {
            scala.collection.immutable.List<?> result = (scala.collection.immutable.List<?>) FIXTURE.names
                    .invoke(symbol);
            assertEquals(Arrays.asList("pkg/First", "java/lang/Object", "pkg/Module$"), javaList(result));
        }
    }

    @Test
    void emptyAndParentOnlyHierarchiesReturnTheEmptyListSingleton() throws Throwable {
        ScalaSignature sig = signature();
        for (scala.collection.immutable.List<TypeRef> parents : Arrays
                .asList(scalaList(new TypeRef[0]), scalaList((TypeRef) null))) {
            ClassType info = classType(sig, parents);
            ClassSymbolRef symbol = symbol(sig, () -> info);
            assertSame(Nil$.MODULE$, symbol.jInterfaces());
        }
    }

    @Test
    void virtualInfoAndInterfacesAreReadOnceBeforeOrderedJavaNames() throws Throwable {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        String firstName = new String("literal.Name");
        TypeRef first = named(sig, () -> {
            calls.add("first.jName");
            return firstName;
        });
        TypeRef second = named(sig, () -> {
            calls.add("second.jName");
            return null;
        });
        ClassType info = info(sig, () -> {
            calls.add("interfaces");
            return scalaList(first, second, first);
        });
        ClassSymbolRef symbol = symbol(sig, () -> {
            calls.add("info");
            return info;
        });
        scala.collection.immutable.List<?> result = symbol.jInterfaces();
        assertEquals(Arrays.asList(firstName, null, firstName), javaList(result));
        assertSame(firstName, result.apply(0));
        assertSame(firstName, result.apply(2));
        assertEquals(Arrays.asList("info", "interfaces", "first.jName", "second.jName", "first.jName"), calls);
    }

    @Test
    void repeatedQueriesRereadInfoInterfacesAndNamesWithoutChangingPreviousResults() throws Throwable {
        ScalaSignature sig = signature();
        int[] reads = { 0, 0, 0 };
        TypeRef ref = named(sig, () -> "name" + ++reads[2]);
        ClassType info = info(sig, () -> ++reads[1] == 1 ? scalaList(ref, ref) : scalaList(ref));
        ClassSymbolRef symbol = symbol(sig, () -> {
            reads[0]++;
            return info;
        });
        scala.collection.immutable.List<?> first = symbol.jInterfaces();
        assertEquals(Arrays.asList("name1", "name2"), javaList(first));
        assertEquals(Arrays.asList("name3"), javaList(symbol.jInterfaces()));
        assertEquals(Arrays.asList("name1", "name2"), javaList(first));
        assertArrayEquals(new int[] { 2, 2, 3 }, reads);
    }

    @Test
    void getterFailuresStopEvaluationWithoutWrappingOrRetrying() throws Throwable {
        ScalaSignature sig = signature();
        List<String> order = Arrays.asList("info", "interfaces", "first", "second");
        RuntimeException failure = new IllegalStateException("interface getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            TypeRef first = named(sig, () -> read(calls, "first", "A", failing, failure));
            TypeRef second = named(sig, () -> read(calls, "second", "B", failing, failure));
            ClassType info = info(sig, () -> read(calls, "interfaces", scalaList(first, second), failing, failure));
            ClassSymbolRef symbol = symbol(sig, () -> read(calls, "info", info, failing, failure));
            assertSame(failure, assertThrows(RuntimeException.class, symbol::jInterfaces));
            assertEquals(order.subList(0, order.indexOf(failing) + 1), calls);
        }
    }

    @Test
    void nullInfoListsAndElementsKeepTheirOriginalFailureBoundaries() throws Throwable {
        ScalaSignature sig = signature();
        assertThrows(NullPointerException.class, symbol(sig, () -> null)::jInterfaces);
        ClassType nullInterfaces = info(sig, () -> null);
        assertThrows(NullPointerException.class, symbol(sig, () -> nullInterfaces)::jInterfaces);
        ClassType nullParents = classType(sig, null);
        assertThrows(NullPointerException.class, symbol(sig, () -> nullParents)::jInterfaces);
        List<String> calls = new ArrayList<>();
        ClassType nullElement = info(sig, () -> scalaList(named(sig, () -> {
            calls.add("first");
            return "A";
        }), null, named(sig, () -> { throw new AssertionError("Name read after null element"); })));
        assertThrows(NullPointerException.class, symbol(sig, () -> nullElement)::jInterfaces);
        assertEquals(Arrays.asList("first"), calls);
    }

    @Test
    void legacyStaticTraitBridgeNeedsOnlyVirtualInfoWithoutAnOuterInstance() throws Throwable {
        ScalaSignature sig = signature();
        ClassType info = info(sig, () -> scalaList(named(sig, () -> "custom/Interface")));
        List<String> calls = new ArrayList<>();
        ClassSymbolRef ref = (ClassSymbolRef) Proxy.newProxyInstance(
                ClassSymbolRef.class.getClassLoader(),
                new Class<?>[] { ClassSymbolRef.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    assertEquals("info", method.getName());
                    return info;
                });
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$ClassSymbolRef$class"),
                "jInterfaces",
                MethodType.methodType(scala.collection.immutable.List.class, ClassSymbolRef.class));
        scala.collection.immutable.List<?> result = (scala.collection.immutable.List<?>) bridge.invokeExact(ref);
        assertEquals(Arrays.asList("custom/Interface"), javaList(result));
        assertEquals(Arrays.asList("info"), calls);
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static <T> T read(List<String> calls, String name, T value, String failing, RuntimeException failure) {
        calls.add(name);
        if (name.equals(failing)) throw failure;
        return value;
    }

    private static TypeRef named(ScalaSignature sig, Supplier<String> name) {
        return sig.new ThisType(null) {

            @Override
            public String jName() {
                return name.get();
            }
        };
    }

    private static ClassType classType(ScalaSignature sig, scala.collection.immutable.List<TypeRef> parents)
            throws Exception {
        return (ClassType) ClassType.class.getConstructors()[0].newInstance(sig, null, parents);
    }

    private static ClassType info(ScalaSignature sig, Supplier<scala.collection.immutable.List<TypeRef>> interfaces)
            throws Throwable {
        return (ClassType) FIXTURE.info.invoke(sig, interfaces);
    }

    private static ClassSymbolRef symbol(ScalaSignature sig, Supplier<ClassType> info) throws Throwable {
        return (ClassSymbolRef) FIXTURE.symbol.invoke(sig, info);
    }

    private static List<?> javaList(scala.collection.immutable.List<?> list) {
        return JavaConversions.seqAsJavaList(list);
    }

    @SafeVarargs
    private static <T> scala.collection.immutable.List<T> scalaList(T... values) {
        return JavaConversions.asScalaBuffer(Arrays.asList(values)).toList();
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle info;
        final MethodHandle symbol;
        final MethodHandle names;

        Fixture() {
            super(ScalaSignatureInterfaceNamesTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaInterfaceNames";
                for (String suffix : new String[] { "", "$$anon$1", "$$anon$2" }) {
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
                info = MethodHandles.publicLookup()
                        .unreflect(type.getMethod("info", ScalaSignature.class, Supplier.class)).bindTo(instance);
                symbol = MethodHandles.publicLookup()
                        .unreflect(type.getMethod("symbol", ScalaSignature.class, Supplier.class)).bindTo(instance);
                names = MethodHandles.publicLookup().unreflect(type.getMethod("names", ClassSymbolRef.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
