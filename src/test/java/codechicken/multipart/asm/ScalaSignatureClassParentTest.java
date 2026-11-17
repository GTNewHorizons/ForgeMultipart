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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.ClassSymbolRef;
import codechicken.multipart.asm.ScalaSignature.ClassType;
import codechicken.multipart.asm.ScalaSignature.TypeRef;
import scala.collection.JavaConversions;

class ScalaSignatureClassParentTest {

    private static final Fixture FIXTURE = new Fixture();

    @Test
    void concreteClassAndObjectSymbolsReturnTheFirstParentName() throws Throwable {
        ScalaSignature sig = signature();
        ClassType info = classType(sig, scalaList(type(sig, () -> "pkg/Parent")));
        ClassSymbolRef klass = sig.new ClassSymbol("Name", null, 0, 0) {

            @Override
            public ClassType info() {
                return info;
            }
        };
        ClassSymbolRef object = sig.new ObjectSymbol("Module", null, 0, 0) {

            @Override
            public ClassType info() {
                return info;
            }
        };
        assertEquals("pkg/Parent", klass.jParent());
        assertEquals("pkg/Parent", object.jParent());
    }

    @Test
    void frozenScalaBridgeReadsInfoParentAndNameInOrderOnEveryQuery() throws Throwable {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        int[] query = { 0 };
        TypeRef parent = type(sig, () -> {
            calls.add("jName");
            return "Parent" + query[0]++;
        });
        ClassType info = info(sig, () -> {
            calls.add("parent");
            return parent;
        });
        ClassSymbolRef ref = ref(sig, () -> {
            calls.add("info");
            return info;
        });
        assertEquals("Parent0", parent(ref));
        assertEquals("Parent1", parent(ref));
        assertEquals(Arrays.asList("info", "parent", "jName", "info", "parent", "jName"), calls);
    }

    @Test
    void defaultClassTypeParentUsesTheHeadWithoutReadingInterfaces() throws Throwable {
        ScalaSignature sig = signature();
        TypeRef first = type(sig, () -> "First");
        TypeRef second = type(sig, () -> { throw new AssertionError("interface jName"); });
        ClassType info = classType(sig, scalaList(first, second));
        ClassSymbolRef ref = ref(sig, () -> info);
        assertEquals("First", parent(ref));
    }

    @Test
    void emptyParentListKeepsItsNoSuchElementFailure() throws Throwable {
        ScalaSignature sig = signature();
        ClassType info = classType(sig, scalaList(new TypeRef[0]));
        ClassSymbolRef ref = ref(sig, () -> info);
        assertThrows(NoSuchElementException.class, () -> parent(ref));
    }

    @Test
    void nullInfoAndParentFailWhileNullNamesPassThrough() throws Throwable {
        ScalaSignature sig = signature();
        assertThrows(NullPointerException.class, () -> parent(ref(sig, () -> null)));
        ClassType nullParent = info(sig, () -> null);
        assertThrows(NullPointerException.class, () -> parent(ref(sig, () -> nullParent)));
        ClassType nullName = info(sig, () -> type(sig, () -> null));
        assertNull(parent(ref(sig, () -> nullName)));
    }

    @Test
    void failuresStopAtTheSelectedInfoParentOrNameRead() throws Throwable {
        ScalaSignature sig = signature();
        List<String> order = Arrays.asList("info", "parent", "jName");
        RuntimeException failure = new IllegalStateException("class parent getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            TypeRef parent = type(sig, () -> read(calls, "jName", "Parent", failing, failure));
            ClassType info = info(sig, () -> read(calls, "parent", parent, failing, failure));
            ClassSymbolRef ref = ref(sig, () -> read(calls, "info", info, failing, failure));
            assertSame(failure, assertThrows(RuntimeException.class, () -> parent(ref)));
            assertEquals(order.subList(0, order.indexOf(failing) + 1), calls);
        }
    }

    @Test
    void legacyStaticTraitBridgeKeepsItsDescriptorAndNullReceiverFailure() throws Throwable {
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$ClassSymbolRef$class"),
                "jParent",
                MethodType.methodType(String.class, ClassSymbolRef.class));
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        TypeRef parent = type(sig, () -> {
            calls.add("jName");
            return "Parent";
        });
        ClassType info = info(sig, () -> {
            calls.add("parent");
            return parent;
        });
        ClassSymbolRef ref = (ClassSymbolRef) Proxy.newProxyInstance(
                ClassSymbolRef.class.getClassLoader(),
                new Class<?>[] { ClassSymbolRef.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getName().equals("info")) return info;
                    throw new AssertionError(method.getName());
                });
        assertEquals("Parent", (String) bridge.invokeExact(ref));
        assertEquals(Arrays.asList("info", "parent", "jName"), calls);
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static <T> T read(List<String> calls, String name, T value, String failing, RuntimeException failure) {
        calls.add(name);
        if (name.equals(failing)) throw failure;
        return value;
    }

    private static TypeRef type(ScalaSignature sig, Supplier<String> names) {
        return sig.new ThisType(null) {

            @Override
            public String jName() {
                return names.get();
            }
        };
    }

    private static ClassType info(ScalaSignature sig, Supplier<TypeRef> parents) throws Throwable {
        return (ClassType) FIXTURE.info.invoke(sig, parents);
    }

    private static ClassType classType(ScalaSignature sig, scala.collection.immutable.List<TypeRef> parents)
            throws Throwable {
        return (ClassType) FIXTURE.classType.invoke(sig, parents);
    }

    private static ClassSymbolRef ref(ScalaSignature sig, Supplier<ClassType> infos) throws Throwable {
        return (ClassSymbolRef) FIXTURE.ref.invoke(sig, infos);
    }

    private static String parent(ClassSymbolRef ref) throws Throwable {
        return (String) FIXTURE.parent.invoke(ref);
    }

    @SafeVarargs
    private static <T> scala.collection.immutable.List<T> scalaList(T... values) {
        return JavaConversions.asScalaBuffer(Arrays.asList(values)).toList();
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }

    private static final class Fixture extends ClassLoader {

        final MethodHandle classType;
        final MethodHandle info;
        final MethodHandle ref;
        final MethodHandle parent;

        Fixture() {
            super(ScalaSignatureClassParentTest.class.getClassLoader());
            try {
                String base = "ReferenceScalaClassParent";
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
                classType = MethodHandles.publicLookup().unreflect(
                        type.getMethod("classType", ScalaSignature.class, scala.collection.immutable.List.class))
                        .bindTo(instance);
                info = MethodHandles.publicLookup()
                        .unreflect(type.getMethod("info", ScalaSignature.class, Supplier.class)).bindTo(instance);
                ref = MethodHandles.publicLookup()
                        .unreflect(type.getMethod("ref", ScalaSignature.class, Supplier.class)).bindTo(instance);
                parent = MethodHandles.publicLookup().unreflect(type.getMethod("parent", ClassSymbolRef.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
