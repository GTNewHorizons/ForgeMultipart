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
import codechicken.multipart.asm.ScalaSignature.ClassSymbolRef;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;

class ScalaSignatureClassSymbolStringTest {

    @Test
    void concreteClassAndObjectSymbolsUseTheirTrimmedRuntimeNames() {
        ScalaSignature sig = signature();
        SymbolRef owner = sig.new ExternalSymbol("pkg.Owner");
        assertEquals("ClassSymbol(Name,pkg.Owner,0,7)", sig.new ClassSymbol("Name", owner, 0, 7).toString());
        assertEquals(
                "ObjectSymbol(Module,pkg.Owner,400,9)",
                sig.new ObjectSymbol("Module", owner, 0x400, 9).toString());
    }

    @Test
    void formatsValuesLiterallyWithLowercaseTwosComplementHexadecimalFlags() {
        ScalaSignature sig = signature();
        Object[][] cases = { { "a,b(c)", null, 15, -12, "ClassSymbol(a,b(c),null,f,-12)" },
                { null, sig.NoSymbol(), -1, Integer.MIN_VALUE, "ClassSymbol(null,NoSymbol,ffffffff,-2147483648)" },
                { "", sig.new ExternalSymbol(""), Integer.MIN_VALUE, Integer.MAX_VALUE,
                        "ClassSymbol(,,80000000,2147483647)" } };
        for (Object[] c : cases) {
            assertEquals(
                    c[4],
                    sig.new ClassSymbol((String) c[0], (SymbolRef) c[1], (Integer) c[2], (Integer) c[3]).toString());
        }
    }

    @Test
    void readsVirtualFieldsOnceInTheirOriginalOrderOnEveryQuery() {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        int[] query = { 0 };
        ClassSymbolRef symbol = sig.new ClassSymbol(null, null, 0, 0) {

            @Override
            public String name() {
                calls.add("name");
                return "Name" + query[0];
            }

            @Override
            public SymbolRef owner() {
                calls.add("owner");
                return sig.new ExternalSymbol("Owner" + query[0]);
            }

            @Override
            public int flags() {
                calls.add("flags");
                return query[0];
            }

            @Override
            public int infoId() {
                calls.add("infoId");
                return query[0]++;
            }
        };
        String prefix = symbol.getClass().getName().replaceAll(".+\\$", "");
        assertEquals(prefix + "(Name0,Owner0,0,0)", symbol.toString());
        assertEquals(prefix + "(Name1,Owner1,1,1)", symbol.toString());
        assertEquals(Arrays.asList("name", "owner", "flags", "infoId", "name", "owner", "flags", "infoId"), calls);
    }

    @Test
    void ownerFormattingHappensAfterTheOwnerGetterAndAcceptsANullResult() {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        SymbolRef owner = sig.new ExternalSymbol("unused") {

            @Override
            public String toString() {
                calls.add("owner.toString");
                return null;
            }
        };
        ClassSymbolRef symbol = sig.new ClassSymbol("Name", null, 0, 1) {

            @Override
            public SymbolRef owner() {
                calls.add("owner");
                return owner;
            }

            @Override
            public int flags() {
                calls.add("flags");
                return 0;
            }
        };
        String prefix = symbol.getClass().getName().replaceAll(".+\\$", "");
        assertEquals(prefix + "(Name,null,0,1)", symbol.toString());
        assertEquals(Arrays.asList("owner", "owner.toString", "flags"), calls);
    }

    @Test
    void failuresPropagateAtEachGetterAndDuringOwnerFormattingWithoutLaterReads() {
        ScalaSignature sig = signature();
        List<String> order = Arrays.asList("name", "owner", "owner.toString", "flags", "infoId");
        RuntimeException failure = new IllegalStateException("symbol string getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            SymbolRef owner = sig.new ExternalSymbol("unused") {

                @Override
                public String toString() {
                    return read(calls, "owner.toString", "Owner", failing, failure);
                }
            };
            ClassSymbolRef symbol = sig.new ClassSymbol(null, null, 0, 0) {

                @Override
                public String name() {
                    return read(calls, "name", "Name", failing, failure);
                }

                @Override
                public SymbolRef owner() {
                    return read(calls, "owner", owner, failing, failure);
                }

                @Override
                public int flags() {
                    return read(calls, "flags", 15, failing, failure);
                }

                @Override
                public int infoId() {
                    return read(calls, "infoId", 4, failing, failure);
                }
            };
            assertSame(failure, assertThrows(RuntimeException.class, symbol::toString));
            assertEquals(order.subList(0, order.indexOf(failing) + 1), calls);
        }
    }

    @Test
    void anonymousSubclassNamesUseOnlyTheSuffixAfterTheLastDollar() {
        ScalaSignature sig = signature();
        ClassSymbolRef symbol = sig.new ClassSymbol("Name", null, 0, 0) {};
        String expected = symbol.getClass().getName().replaceAll(".+\\$", "");
        assertFalse(expected.contains("."));
        assertFalse(expected.contains("$"));
        assertEquals(expected + "(Name,null,0,0)", symbol.toString());
    }

    @Test
    void legacyStaticTraitBridgeNeedsNoOuterInstanceAndRejectsNullReceivers() throws Throwable {
        MethodHandle bridge = MethodHandles.lookup().findStatic(
                Class.forName("codechicken.multipart.asm.ScalaSignature$ClassSymbolRef$class"),
                "toString",
                MethodType.methodType(String.class, ClassSymbolRef.class));
        List<String> calls = new ArrayList<>();
        ClassSymbolRef symbol = (ClassSymbolRef) Proxy.newProxyInstance(
                ClassSymbolRef.class.getClassLoader(),
                new Class<?>[] { ClassSymbolRef.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    switch (method.getName()) {
                        case "name":
                            return "Proxy";
                        case "owner":
                            return null;
                        case "flags":
                            return 0xabcdef;
                        case "infoId":
                            return -3;
                        default:
                            throw new AssertionError(method.getName());
                    }
                });
        String runtimeName = symbol.getClass().getName().replaceAll(".+\\$", "");
        assertEquals(runtimeName + "(Proxy,null,abcdef,-3)", (String) bridge.invokeExact(symbol));
        assertEquals(Arrays.asList("name", "owner", "flags", "infoId"), calls);
        assertThrows(NullPointerException.class, () -> bridge.invokeWithArguments(new Object[] { null }));
    }

    private static <T> T read(List<String> calls, String name, T value, String failing, RuntimeException failure) {
        calls.add(name);
        if (name.equals(failing)) throw failure;
        return value;
    }

    private static ScalaSignature signature() {
        return new ScalaSignature(new Bytes(new byte[] { 5, 0, 0 }, 0, 3));
    }
}
