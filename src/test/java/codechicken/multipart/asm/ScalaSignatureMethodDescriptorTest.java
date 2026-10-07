package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.MethodSymbol;
import codechicken.multipart.asm.ScalaSignature.TMethodType;
import codechicken.multipart.asm.ScalaSignature.TypeRef;
import scala.collection.JavaConversions;

class ScalaSignatureMethodDescriptorTest {

    @Test
    void parameterlessAndMethodModelsAssembleLiteralDescriptorsInOrder() throws Exception {
        ScalaSignature sig = signature();
        assertEquals("()V", sig.new ParameterlessType(namedType(sig, "scala.Unit")).jDesc());
        List<MethodSymbol> parameters = new ArrayList<>();
        for (String name : new String[] { "scala.Int", "scala.Long", "scala.Char", "java.lang.String" }) {
            parameters.add(parameter(sig, () -> sig.new ParameterlessType(namedType(sig, name))));
        }
        TMethodType ref = (TMethodType) ScalaSignature.MethodType.class.getConstructors()[0]
                .newInstance(sig, namedType(sig, "scala.Boolean"), JavaConversions.asScalaBuffer(parameters).toList());
        assertEquals("(IJLscala/Char;Ljava/lang/String;)Z", ref.jDesc());
    }

    @Test
    void readsParameterInfoAndReturnTypesBeforeTheMethodReturnType() {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        TypeRef[] result = { descriptor(sig, () -> "old") };
        List<MethodSymbol> parameters = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            final int i = index;
            TypeRef type = descriptor(sig, () -> {
                calls.add(i + ".jDesc");
                result[0] = descriptor(sig, () -> {
                    calls.add("result.jDesc");
                    return "R";
                });
                return i == 0 ? "I" : "J";
            });
            TMethodType info = method(
                    sig,
                    () -> { throw new AssertionError("Parameter params must not be read"); },
                    () -> {
                        calls.add(i + ".returnType");
                        return type;
                    });
            parameters.add(parameter(sig, () -> {
                calls.add(i + ".info");
                return info;
            }));
        }
        TMethodType ref = method(sig, () -> {
            calls.add("params");
            return JavaConversions.asScalaBuffer(parameters).toList();
        }, () -> {
            calls.add("returnType");
            return result[0];
        });
        assertEquals("(IJ)R", ref.jDesc());
        assertEquals(
                Arrays.asList(
                        "params",
                        "0.info",
                        "0.returnType",
                        "0.jDesc",
                        "1.info",
                        "1.returnType",
                        "1.jDesc",
                        "returnType",
                        "result.jDesc"),
                calls);
    }

    @Test
    void repeatedParametersAndQueriesRereadVirtualInfoWithoutCaching() {
        ScalaSignature sig = signature();
        int[] reads = { 0 };
        MethodSymbol param = parameter(sig, () -> {
            String value = Integer.toString(++reads[0]);
            return sig.new ParameterlessType(descriptor(sig, () -> value));
        });
        TMethodType ref = method(sig, () -> scalaList(param, param), () -> descriptor(sig, () -> "V"));
        assertEquals("(12)V", ref.jDesc());
        assertEquals("(34)V", ref.jDesc());
        assertEquals(4, reads[0]);
    }

    @Test
    void nullAndUnvalidatedDescriptorsAreConcatenatedLiterally() {
        ScalaSignature sig = signature();
        MethodSymbol missingArray = parameter(sig, () -> sig.new ParameterlessType(namedType(sig, "scala.Array")));
        MethodSymbol literal = parameter(sig, () -> sig.new ParameterlessType(descriptor(sig, () -> ")invalid(")));
        TMethodType ref = method(sig, () -> scalaList(missingArray, literal), () -> descriptor(sig, () -> null));
        assertEquals("(null)invalid()null", ref.jDesc());
        assertEquals("()null", sig.new ParameterlessType(descriptor(sig, () -> null)).jDesc());
    }

    @Test
    void propagatesEachGetterFailureWithoutReadingLaterGetters() {
        ScalaSignature sig = signature();
        List<String> order = Arrays
                .asList("params", "info", "parameterReturn", "parameterDescriptor", "returnType", "returnDescriptor");
        RuntimeException failure = new IllegalStateException("method descriptor getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            TypeRef argument = descriptor(sig, () -> read(calls, "parameterDescriptor", "I", failing, failure));
            TMethodType info = method(
                    sig,
                    () -> { throw new AssertionError("Unexpected parameter params"); },
                    () -> read(calls, "parameterReturn", argument, failing, failure));
            MethodSymbol parameter = parameter(sig, () -> read(calls, "info", info, failing, failure));
            TypeRef result = descriptor(sig, () -> read(calls, "returnDescriptor", "V", failing, failure));
            TMethodType ref = method(
                    sig,
                    () -> read(calls, "params", scalaList(parameter), failing, failure),
                    () -> read(calls, "returnType", result, failing, failure));
            assertSame(failure, assertThrows(RuntimeException.class, ref::jDesc));
            assertEquals(order.subList(0, order.indexOf(failing) + 1), calls);
        }
    }

    @Test
    void nullParameterChainsFailBeforeReadingTheMethodReturnType() {
        ScalaSignature sig = signature();
        List<scala.collection.immutable.List<MethodSymbol>> cases = Arrays.asList(
                null,
                scalaList((MethodSymbol) null),
                scalaList(parameter(sig, () -> null)),
                scalaList(parameter(sig, () -> sig.new ParameterlessType(null))));
        for (scala.collection.immutable.List<MethodSymbol> params : cases) {
            TMethodType ref = method(
                    sig,
                    () -> params,
                    () -> { throw new AssertionError("Return type read too soon"); });
            assertThrows(NullPointerException.class, ref::jDesc);
        }
        assertThrows(NullPointerException.class, () -> sig.new ParameterlessType(null).jDesc());
    }

    @Test
    void legacyStaticTraitBridgeUsesVirtualMembersWithoutAnOuterInstance() throws Throwable {
        ScalaSignature sig = signature();
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$TMethodType$class"),
                "jDesc",
                MethodType.methodType(String.class, TMethodType.class));
        List<String> calls = new ArrayList<>();
        TMethodType ref = (TMethodType) Proxy.newProxyInstance(
                TMethodType.class.getClassLoader(),
                new Class<?>[] { TMethodType.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getName().equals("params"))
                        return scalaList(parameter(sig, () -> sig.new ParameterlessType(namedType(sig, "scala.Int"))));
                    assertEquals("returnType", method.getName());
                    return namedType(sig, "scala.Unit");
                });
        assertEquals("(I)V", (String) bridge.invokeExact(ref));
        assertEquals(Arrays.asList("params", "returnType"), calls);
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static <T> T read(List<String> calls, String name, T value, String failing, RuntimeException failure) {
        calls.add(name);
        if (name.equals(failing)) throw failure;
        return value;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static TMethodType method(ScalaSignature sig,
            Supplier<scala.collection.immutable.List<MethodSymbol>> params, Supplier<TypeRef> result) {
        return sig.new ParameterlessType(null) {

            @Override
            public scala.collection.immutable.List params() {
                return params.get();
            }

            @Override
            public TypeRef returnType() {
                return result.get();
            }
        };
    }

    private static MethodSymbol parameter(ScalaSignature sig, Supplier<TMethodType> info) {
        return sig.new MethodSymbol("unused", null, 0, -1) {

            @Override
            public TMethodType info() {
                return info.get();
            }

            @Override
            public String jDesc() {
                throw new AssertionError("Parameter jDesc must not be used");
            }
        };
    }

    private static TypeRef descriptor(ScalaSignature sig, Supplier<String> value) {
        return sig.new ThisType(null) {

            @Override
            public String jDesc() {
                return value.get();
            }
        };
    }

    private static TypeRef namedType(ScalaSignature sig, String name) {
        return sig.new ThisType(sig.new ExternalSymbol(name));
    }

    @SafeVarargs
    private static <T> scala.collection.immutable.List<T> scalaList(T... values) {
        return JavaConversions.asScalaBuffer(Arrays.asList(values)).toList();
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }
}
