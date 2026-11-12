package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.multipart.asm.StackAnalyser.Const;
import scala.runtime.BoxedUnit;

class StackAnalyserConstTest {

    @Test
    void classifiesEveryBoxedPrimitiveWithoutNumericCoercion() {
        Object[][] values = { { Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE },
                { Short.MIN_VALUE, (short) 0, Short.MAX_VALUE }, { Integer.MIN_VALUE, 0, Integer.MAX_VALUE },
                { Long.MIN_VALUE, 0L, Long.MAX_VALUE }, { -0.0f, Float.NaN, Float.POSITIVE_INFINITY },
                { -0.0d, Double.NaN, Double.NEGATIVE_INFINITY }, { Character.MIN_VALUE, 'x', Character.MAX_VALUE },
                { false, true } };
        Type[] types = { Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE,
                Type.DOUBLE_TYPE, Type.CHAR_TYPE, Type.BOOLEAN_TYPE };
        for (int i = 0; i < values.length; i++) {
            for (Object value : values[i]) assertSame(types[i], new Const(value, null).getType());
        }
    }

    @Test
    void nullAndStringsProduceFreshObjectTypes() {
        for (Object value : new Object[] { null, "", "text", "[I" }) {
            Const constant = new Const(value, null);
            Type first = constant.getType();
            Type second = constant.getType();
            assertEquals(Type.getObjectType(value == null ? "java/lang/Object" : "java/lang/String"), first);
            assertEquals(first, second);
            assertNotSame(first, second);
        }
    }

    @Test
    void rejectsOtherNumbersTypesUnitsArraysAndStringLikeObjects() {
        for (Object value : new Object[] { BigInteger.ONE, Type.INT_TYPE, BoxedUnit.UNIT, new int[] { 1, 2 },
                new char[] { 'x' }, new Object[] { "text" }, new StringBuilder("text") }) {
            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Const(value, null).getType());
            assertEquals("Unknown const " + value, error.getMessage());
        }
    }

    @Test
    void successfulQueriesReadVirtualConstantOnceAndNeverCacheIt() {
        Object[] values = { (byte) 1, (short) 2, 3, 4L, 5f, 6d, 'c', true, "text", null };
        Type[] types = { Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE,
                Type.DOUBLE_TYPE, Type.CHAR_TYPE, Type.BOOLEAN_TYPE, Type.getObjectType("java/lang/String"),
                Type.getObjectType("java/lang/Object") };
        int[] reads = { 0 };
        Const constant = new Const(new Object(), null) {

            @Override
            public Object c() {
                return values[reads[0]++];
            }
        };
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], constant.getType());
            assertEquals(i + 1, reads[0]);
        }
    }

    @Test
    void unsupportedQueriesRereadVirtualConstantForTheMessageWithoutReclassifying() {
        for (Object replacement : new Object[] { "replacement", null, 42 }) {
            int[] reads = { 0 };
            Const constant = new Const("unused", null) {

                @Override
                public Object c() {
                    return reads[0]++ == 0 ? new Object() : replacement;
                }
            };
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, constant::getType);
            assertEquals("Unknown const " + replacement, error.getMessage());
            assertEquals(2, reads[0]);
        }
    }

    @Test
    void formatsOnlyTheSecondValueOnceEvenWhenItsToStringReturnsNull() {
        for (String text : new String[] { "rendered", null }) {
            List<String> calls = new ArrayList<>();
            Object first = new Object() {

                @Override
                public String toString() {
                    throw new AssertionError("The matched value must not be formatted");
                }
            };
            Object second = new Object() {

                @Override
                public String toString() {
                    calls.add("format");
                    return text;
                }
            };
            Const constant = new Const(null, null) {

                @Override
                public Object c() {
                    calls.add("read");
                    return calls.size() == 1 ? first : second;
                }
            };
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, constant::getType);
            assertEquals("Unknown const " + text, error.getMessage());
            assertEquals(Arrays.asList("read", "read", "format"), calls);
        }
    }

    @Test
    void propagatesFirstAndSecondAccessorFailuresWithoutWrappingOrRetrying() {
        for (int failAt : new int[] { 1, 2 }) {
            RuntimeException failure = new IllegalStateException("accessor");
            int[] reads = { 0 };
            Const constant = new Const(1, null) {

                @Override
                public Object c() {
                    if (++reads[0] == failAt) throw failure;
                    return new Object();
                }
            };
            assertSame(failure, assertThrows(RuntimeException.class, constant::getType));
            assertEquals(failAt, reads[0]);
        }
    }

    @Test
    void propagatesFormattingFailureAfterBothAccessorReads() {
        RuntimeException failure = new IllegalStateException("format");
        List<String> calls = new ArrayList<>();
        Object value = new Object() {

            @Override
            public String toString() {
                calls.add("format");
                throw failure;
            }
        };
        Const constant = new Const(1, null) {

            @Override
            public Object c() {
                calls.add("read");
                return value;
            }
        };
        assertSame(failure, assertThrows(RuntimeException.class, constant::getType));
        assertEquals(Arrays.asList("read", "read", "format"), calls);
    }
}
