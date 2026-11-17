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

class ScalaSignatureTypeNameTest {

    @Test
    void replacesOnlyDotsWithoutInterpretingDescriptorsOrPrimitiveNames() {
        ScalaSignature sig = signature();
        String[][] cases = { { "java.lang.String", "java/lang/String" }, { "pkg.Name$Inner", "pkg/Name$Inner" },
                { ".pkg..Name.", "/pkg//Name/" }, { "pkg/Name", "pkg/Name" }, { "", "" }, { "pkg\\Name", "pkg\\Name" },
                { "[Ljava.lang.String;", "[Ljava/lang/String;" }, { "scala.Int", "scala/Int" },
                { "scala.Unit", "scala/Unit" }, { "scala.Array", "scala/Array" },
                { "caf\u00e9.Name", "caf\u00e9/Name" }, { "scala.Predef.String", "scala/Predef/String" } };
        for (String[] c : cases) assertEquals(c[1], type(sig, c[0]).jName());
    }

    @Test
    void mapsOnlyExactNormalizedAnyAliasesAndPreservesOtherStringIdentities() {
        ScalaSignature sig = signature();
        for (String name : new String[] { "scala.Any", "scala/Any", "scala.AnyRef", "scala/AnyRef" }) {
            assertSame("java/lang/Object", type(sig, name).jName());
        }
        for (String value : new String[] { "scala/any", "scala/Anyref", "scala/AnyRef$", "scala/Any/", "Any",
                "java/lang/Object", "scala/Nothing", "" }) {
            String name = new String(value);
            assertSame(name, type(sig, name).jName());
        }
    }

    @Test
    void readsVirtualNameOncePerQueryWithoutAccessingTheSymbol() {
        ScalaSignature sig = signature();
        String[] names = { "scala.Any", "pkg.Name", "other/Name" };
        int[] reads = { 0 };
        TypeRef ref = sig.new ThisType(null) {

            @Override
            public String name() {
                return names[reads[0]++];
            }

            @Override
            public SymbolRef sym() {
                throw new AssertionError("An overridden name must bypass symbol lookup");
            }
        };
        assertEquals("java/lang/Object", ref.jName());
        assertEquals(1, reads[0]);
        assertEquals("pkg/Name", ref.jName());
        assertEquals(2, reads[0]);
        assertSame(names[2], ref.jName());
        assertEquals(3, reads[0]);
    }

    @Test
    void defaultNameReadsVirtualSymbolAndFullOnceInOrder() {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        int[] reads = { 0 };
        SymbolRef symbol = sig.new ExternalSymbol("stored") {

            @Override
            public String full() {
                calls.add("full");
                return ++reads[0] == 1 ? "scala.AnyRef" : "pkg.Name";
            }
        };
        TypeRef ref = sig.new ThisType(null) {

            @Override
            public SymbolRef sym() {
                calls.add("sym");
                return symbol;
            }
        };
        assertEquals("java/lang/Object", ref.jName());
        assertEquals("pkg/Name", ref.jName());
        assertEquals(Arrays.asList("sym", "full", "sym", "full"), calls);
    }

    @Test
    void propagatesNullsAndGetterFailuresWithoutWrappingOrRetrying() {
        ScalaSignature sig = signature();
        assertThrows(NullPointerException.class, () -> sig.new ThisType(null).jName());
        assertThrows(NullPointerException.class, () -> type(sig, null).jName());
        RuntimeException failure = new IllegalStateException("getter");
        int[] reads = { 0 };
        TypeRef badName = sig.new ThisType(null) {

            @Override
            public String name() {
                reads[0]++;
                throw failure;
            }
        };
        TypeRef badSymbol = sig.new ThisType(null) {

            @Override
            public SymbolRef sym() {
                reads[0]++;
                throw failure;
            }
        };
        TypeRef badFull = sig.new ThisType(sig.new ExternalSymbol(null) {

            @Override
            public String full() {
                reads[0]++;
                throw failure;
            }
        });
        for (TypeRef ref : new TypeRef[] { badName, badSymbol, badFull }) {
            assertSame(failure, assertThrows(RuntimeException.class, ref::jName));
        }
        assertEquals(3, reads[0]);
    }

    @Test
    void singleTypeSuperCallsAndNoTypeKeepTheirExistingNamesAndDescriptorRouting() {
        ScalaSignature sig = signature();
        for (String name : new String[] { "scala.AnyRef", "pkg.Name", "pkg.Name$" }) {
            String expected = name.equals("scala.AnyRef") ? "java/lang/Object$" : name.replace('.', '/') + "$";
            int[] reads = { 0 };
            TypeRef ref = sig.new SingleType(null, null) {

                @Override
                public String name() {
                    reads[0]++;
                    return name;
                }
            };
            assertEquals(expected, ref.jName());
            assertEquals(1, reads[0]);
            assertEquals("L" + expected + ";", ref.jDesc());
            assertEquals(3, reads[0]);
        }
        assertEquals("<no type>", sig.NoType().jName());
        assertEquals("L<no type>;", sig.NoType().jDesc());
    }

    @Test
    void legacyStaticTraitBridgeUsesOnlyVirtualNameAndNeedsNoOuterInstance() throws Throwable {
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$TypeRef$class"),
                "jName",
                MethodType.methodType(String.class, TypeRef.class));
        List<String> calls = new ArrayList<>();
        TypeRef ref = (TypeRef) Proxy.newProxyInstance(
                TypeRef.class.getClassLoader(),
                new Class<?>[] { TypeRef.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    assertEquals("name", method.getName());
                    return "pkg.Name";
                });
        assertEquals("pkg/Name", (String) bridge.invokeExact(ref));
        assertEquals(Arrays.asList("name"), calls);
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static TypeRef type(ScalaSignature sig, String name) {
        return sig.new ThisType(sig.new ExternalSymbol(name));
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }
}
