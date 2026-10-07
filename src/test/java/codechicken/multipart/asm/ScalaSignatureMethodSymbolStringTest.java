package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import codechicken.multipart.asm.ScalaSignature.SymbolRef;
import codechicken.multipart.asm.ScalaSignature.TMethodType;

class ScalaSignatureMethodSymbolStringTest {

    @Test
    void formatsConcreteMethodSymbolsWithTheFixedProductName() {
        ScalaSignature sig = signature();
        SymbolRef owner = sig.new ExternalSymbol("pkg.Owner");
        assertEquals("MethodSymbol(run,pkg.Owner,200,7)", sig.new MethodSymbol("run", owner, 0x200, 7).toString());
    }

    @Test
    void formatsValuesLiterallyWithLowercaseTwosComplementHexadecimalFlags() {
        ScalaSignature sig = signature();
        Object[][] cases = { { "a,b(c)", null, 15, -12, "MethodSymbol(a,b(c),null,f,-12)" },
                { null, sig.NoSymbol(), -1, Integer.MIN_VALUE, "MethodSymbol(null,NoSymbol,ffffffff,-2147483648)" },
                { "", sig.new ExternalSymbol(""), Integer.MIN_VALUE, Integer.MAX_VALUE,
                        "MethodSymbol(,,80000000,2147483647)" } };
        for (Object[] c : cases) {
            assertEquals(
                    c[4],
                    sig.new MethodSymbol((String) c[0], (SymbolRef) c[1], (Integer) c[2], (Integer) c[3]).toString());
        }
    }

    @Test
    void anonymousSubclassesStillUseTheFixedMethodSymbolPrefix() {
        ScalaSignature sig = signature();
        ScalaSignature.MethodSymbol symbol = sig.new MethodSymbol("Name", null, 0, 0) {};
        assertFalse(symbol.getClass().getName().endsWith("$MethodSymbol"));
        assertEquals("MethodSymbol(Name,null,0,0)", symbol.toString());
    }

    @Test
    void readsVirtualFieldsOnceInTheirOriginalOrderOnEveryQuery() {
        ScalaSignature sig = signature();
        List<String> calls = new ArrayList<>();
        int[] query = { 0 };
        ScalaSignature.MethodSymbol symbol = sig.new MethodSymbol(null, null, 0, 0) {

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
        assertEquals("MethodSymbol(Name0,Owner0,0,0)", symbol.toString());
        assertEquals("MethodSymbol(Name1,Owner1,1,1)", symbol.toString());
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
        ScalaSignature.MethodSymbol symbol = sig.new MethodSymbol("Name", null, 0, 1) {

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
        assertEquals("MethodSymbol(Name,null,0,1)", symbol.toString());
        assertEquals(Arrays.asList("owner", "owner.toString", "flags"), calls);
    }

    @Test
    void failuresPropagateAtEachGetterAndDuringOwnerFormattingWithoutLaterReads() {
        ScalaSignature sig = signature();
        List<String> order = Arrays.asList("name", "owner", "owner.toString", "flags", "infoId");
        RuntimeException failure = new IllegalStateException("method symbol string getter");
        for (String failing : order) {
            List<String> calls = new ArrayList<>();
            SymbolRef owner = sig.new ExternalSymbol("unused") {

                @Override
                public String toString() {
                    return read(calls, "owner.toString", "Owner", failing, failure);
                }
            };
            ScalaSignature.MethodSymbol symbol = sig.new MethodSymbol(null, null, 0, 0) {

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
    void formattingDoesNotReadDerivedSymbolOrMethodInformation() {
        ScalaSignature sig = signature();
        ScalaSignature.MethodSymbol symbol = sig.new MethodSymbol("Name", null, 0, -1) {

            @Override
            public String full() {
                throw new AssertionError("full");
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
            public boolean hasFlag(int flag) {
                throw new AssertionError("hasFlag");
            }
        };
        assertEquals("MethodSymbol(Name,null,0,-1)", symbol.toString());
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
