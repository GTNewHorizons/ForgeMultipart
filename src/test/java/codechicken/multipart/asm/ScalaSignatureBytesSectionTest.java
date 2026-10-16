package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ScalaSignature.Bytes;

class ScalaSignatureBytesSectionTest {

    @Test
    void clampsDropAndTakeIndependentlyIncludingIntegerExtremes() {
        byte[] source = { 1, 2, 3, 4 };
        int[][] cases = { { 0, 4, 0, 4 }, { 1, 2, 1, 3 }, { -1, 2, 0, 2 },
                { Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 4 }, { 1, Integer.MAX_VALUE, 1, 4 }, { 3, 10, 3, 4 },
                { 4, 1, 4, 4 }, { 5, 10, 4, 4 }, { Integer.MAX_VALUE, Integer.MAX_VALUE, 4, 4 }, { 1, -1, 1, 1 },
                { 1, Integer.MIN_VALUE, 1, 1 }, { 0, 0, 0, 0 }, { -1, -1, 0, 0 } };
        for (int[] c : cases) {
            assertArrayEquals(Arrays.copyOfRange(source, c[2], c[3]), new Bytes(source, c[0], c[1]).section());
        }
    }

    @Test
    void alwaysReturnsFreshArraysWithoutAliasingTheSourceOrOtherResults() {
        for (byte[] source : new byte[][] { {}, { 1, 2, 3 } }) {
            for (int length : new int[] { 0, Integer.MAX_VALUE }) {
                Bytes bytes = new Bytes(source, 0, length);
                byte[] first = bytes.section();
                byte[] second = bytes.section();
                assertNotSame(source, first);
                assertNotSame(first, second);
                assertArrayEquals(first, second);
                if (first.length != 0) {
                    first[0] = 9;
                    assertEquals(1, source[0]);
                    assertEquals(1, second[0]);
                    source[0] = 8;
                    assertEquals(1, second[0]);
                    assertEquals(8, bytes.section()[0]);
                }
            }
        }
    }

    @Test
    void readsVirtualGettersOnceAndCopiesAfterPositionButBeforeLength() {
        for (int position : new int[] { -1, 0, 1 }) {
            byte[] source = { 1, 2, 3, 4 };
            List<String> calls = new ArrayList<>();
            Bytes bytes = new Bytes(null, 99, 99) {

                @Override
                public byte[] arr() {
                    calls.add("arr");
                    return source;
                }

                @Override
                public int pos() {
                    calls.add("pos");
                    source[1] = 22;
                    return position;
                }

                @Override
                public int len() {
                    calls.add("len");
                    Arrays.fill(source, (byte) 0);
                    return 2;
                }
            };
            assertArrayEquals(position == 1 ? new byte[] { 22, 3 } : new byte[] { 1, 22 }, bytes.section());
            assertEquals(Arrays.asList("arr", "pos", "len"), calls);
        }
    }

    @Test
    void nullArrayStillReadsPositionBeforeFailingAndNeverReadsLength() {
        for (boolean positionFails : new boolean[] { false, true }) {
            RuntimeException failure = new IllegalStateException("pos");
            List<String> calls = new ArrayList<>();
            Bytes bytes = new Bytes(null, 0, 0) {

                @Override
                public byte[] arr() {
                    calls.add("arr");
                    return null;
                }

                @Override
                public int pos() {
                    calls.add("pos");
                    if (positionFails) throw failure;
                    return 0;
                }

                @Override
                public int len() {
                    throw new AssertionError("Length must not be read after drop fails");
                }
            };
            RuntimeException actual = assertThrows(RuntimeException.class, bytes::section);
            if (positionFails) assertSame(failure, actual);
            else assertInstanceOf(NullPointerException.class, actual);
            assertEquals(Arrays.asList("arr", "pos"), calls);
        }
    }

    @Test
    void propagatesEveryGetterFailureWithoutWrappingOrReadingLaterGetters() {
        for (String failed : Arrays.asList("arr", "pos", "len")) {
            RuntimeException failure = new IllegalStateException(failed);
            List<String> calls = new ArrayList<>();
            Bytes bytes = new Bytes(null, 0, 0) {

                @Override
                public byte[] arr() {
                    calls.add("arr");
                    if (failed.equals("arr")) throw failure;
                    return new byte[] { 1, 2 };
                }

                @Override
                public int pos() {
                    calls.add("pos");
                    if (failed.equals("pos")) throw failure;
                    return 0;
                }

                @Override
                public int len() {
                    calls.add("len");
                    throw failure;
                }
            };
            assertSame(failure, assertThrows(RuntimeException.class, bytes::section));
            List<String> order = Arrays.asList("arr", "pos", "len");
            assertEquals(order.subList(0, order.indexOf(failed) + 1), calls);
        }
    }

    @Test
    void stillReadsLengthWhenDropHasAlreadyProducedAnEmptyArray() {
        RuntimeException failure = new IllegalStateException("len");
        for (byte[] source : new byte[][] { {}, { 1 } }) {
            Bytes bytes = new Bytes(source, Integer.MAX_VALUE, 0) {

                @Override
                public int len() {
                    throw failure;
                }
            };
            assertSame(failure, assertThrows(RuntimeException.class, bytes::section));
        }
    }
}
