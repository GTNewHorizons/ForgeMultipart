package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;
import codechicken.multipart.asm.ScalaSignature.TypeRef;
import codechicken.multipart.asm.ScalaSignature.TypeRefType;
import scala.collection.JavaConversions;

class ScalaSignatureTypeDescriptorTest {

    @Test
    void exactPrimitiveAndArrayNamesBypassVirtualJavaName() {
        String[][] cases = { { "scala.Array", null }, { "scala.Long", "J" }, { "scala.Int", "I" },
                { "scala.Short", "S" }, { "scala.Byte", "B" }, { "scala.Double", "D" }, { "scala.Float", "F" },
                { "scala.Boolean", "Z" }, { "scala.Unit", "V" } };
        ScalaSignature sig = signature();
        for (String[] c : cases) {
            int[] reads = { 0 };
            TypeRef ref = sig.new ThisType(null) {

                @Override
                public String name() {
                    reads[0]++;
                    return new String(c[0]);
                }

                @Override
                public String jName() {
                    throw new AssertionError("Matched names must not read jName");
                }
            };
            assertSame(c[1], ref.jDesc());
            assertEquals(1, reads[0]);
        }
    }

    @Test
    void unmatchedNamesKeepLegacyReferenceDescriptorsIncludingChar() {
        String[][] cases = { { "scala.Char", "Lscala/Char;" }, { "scala.Nothing", "Lscala/Nothing;" },
                { "scala.Any", "Ljava/lang/Object;" }, { "scala.AnyRef", "Ljava/lang/Object;" },
                { "scala.Predef.String", "Lscala/Predef/String;" }, { "scala/Int", "Lscala/Int;" },
                { "scala/Array", "Lscala/Array;" }, { "scala.int", "Lscala/int;" }, { "", "L;" }, { "[I", "L[I;" },
                { "java.lang.String", "Ljava/lang/String;" } };
        ScalaSignature sig = signature();
        for (String[] c : cases) assertEquals(c[1], type(sig, c[0]).jDesc());
        assertEquals("L<no type>;", sig.NoType().jDesc());
        assertEquals("Lpkg/Module$;", sig.new SingleType(null, sig.new ExternalSymbol("pkg.Module")).jDesc());
    }

    @Test
    void fallbackUsesVirtualJavaNameOnceEvenForNullNamesAndResults() {
        ScalaSignature sig = signature();
        for (String name : new String[] { "pkg.Name", null }) {
            for (String javaName : new String[] { "custom/Name$", null }) {
                List<String> calls = new ArrayList<>();
                TypeRef ref = sig.new ThisType(null) {

                    @Override
                    public String name() {
                        calls.add("name");
                        return name;
                    }

                    @Override
                    public String jName() {
                        calls.add("jName");
                        return javaName;
                    }
                };
                assertEquals("L" + javaName + ";", ref.jDesc());
                assertEquals(Arrays.asList("name", "jName"), calls);
            }
        }
    }

    @Test
    void defaultFallbackRereadsNameAndSymbolInsteadOfReusingTheMatchedValue() {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        String[] names = { "pkg.First", "scala.AnyRef", "scala.Int" };
        int[] reads = { 0 };
        SymbolRef symbol = sig.new ExternalSymbol(null) {

            @Override
            public String full() {
                calls.add("full");
                return names[reads[0]++];
            }
        };
        TypeRef ref = sig.new ThisType(null) {

            @Override
            public SymbolRef sym() {
                calls.add("sym");
                return symbol;
            }
        };
        assertEquals("Ljava/lang/Object;", ref.jDesc());
        assertEquals("I", ref.jDesc());
        assertEquals(Arrays.asList("sym", "full", "sym", "full", "sym", "full"), calls);
    }

    @Test
    void failuresPropagateAtTheFirstGetterWithoutRetrying() {
        ScalaSignature sig = signature();
        RuntimeException failure = new IllegalStateException("descriptor getter");
        for (boolean failName : new boolean[] { true, false }) {
            List<String> calls = new ArrayList<>();
            TypeRef ref = sig.new ThisType(null) {

                @Override
                public String name() {
                    calls.add("name");
                    if (failName) throw failure;
                    return "pkg.Name";
                }

                @Override
                public String jName() {
                    calls.add("jName");
                    throw failure;
                }
            };
            assertSame(failure, assertThrows(RuntimeException.class, ref::jDesc));
            assertEquals(failName ? Arrays.asList("name") : Arrays.asList("name", "jName"), calls);
        }
        assertThrows(NullPointerException.class, () -> sig.new ThisType(null).jDesc());
        assertThrows(NullPointerException.class, () -> type(sig, null).jDesc());
    }

    @Test
    void retainedArrayOverrideUsesOnlyTheFirstArgumentAndPreservesFailures() throws Exception {
        ScalaSignature sig = signature();
        TypeRef integer = type(sig, "scala.Int");
        TypeRef array = array(sig, integer, null);
        assertEquals("[I", array.jDesc());
        assertEquals("[[I", array(sig, array).jDesc());
        assertEquals("[null", array(sig, type(sig, "scala.Array")).jDesc());
        assertThrows(IndexOutOfBoundsException.class, () -> array(sig).jDesc());
        assertThrows(NullPointerException.class, () -> array(sig, (TypeRef) null).jDesc());
    }

    @Test
    void legacyStaticTraitBridgeNeedsOnlyNameAndFallbackJavaName() throws Throwable {
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$TypeRef$class"),
                "jDesc",
                MethodType.methodType(String.class, TypeRef.class));
        for (String name : new String[] { "scala.Int", "pkg.Name", null }) {
            List<String> calls = new ArrayList<>();
            TypeRef ref = (TypeRef) Proxy.newProxyInstance(
                    TypeRef.class.getClassLoader(),
                    new Class<?>[] { TypeRef.class },
                    (proxy, method, args) -> {
                        calls.add(method.getName());
                        if (method.getName().equals("name")) return name;
                        assertEquals("jName", method.getName());
                        return "override/Name";
                    });
            assertEquals("scala.Int".equals(name) ? "I" : "Loverride/Name;", (String) bridge.invokeExact(ref));
            assertEquals("scala.Int".equals(name) ? Arrays.asList("name") : Arrays.asList("name", "jName"), calls);
        }
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static TypeRef array(ScalaSignature sig, TypeRef... arguments) throws Exception {
        return (TypeRef) TypeRefType.class.getConstructors()[0].newInstance(
                sig,
                null,
                sig.new ExternalSymbol("scala.Array"),
                JavaConversions.asScalaBuffer(Arrays.asList(arguments)).toList());
    }

    private static TypeRef type(ScalaSignature sig, String name) {
        return sig.new ThisType(sig.new ExternalSymbol(name));
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }
}
