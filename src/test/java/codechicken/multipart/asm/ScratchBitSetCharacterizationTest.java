package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class ScratchBitSetCharacterizationTest {

    @Test
    void keepsExactInterfaceAndStaticHelperSurface() {
        assertEquals(Modifier.PUBLIC | Modifier.INTERFACE | Modifier.ABSTRACT, ScratchBitSet.class.getModifiers());
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT, ScratchBitSet$class.class.getModifiers());
        assertSame(Object.class, ScratchBitSet$class.class.getSuperclass());
        for (Class<?> type : new Class<?>[] { ScratchBitSet.class, ScratchBitSet$class.class }) {
            assertEquals(0, type.getDeclaredFields().length);
            assertEquals(0, type.getInterfaces().length);
        }
        assertEquals(
                signatures(
                        "codechicken$multipart$asm$ScratchBitSet$$bitSets()Ljava/lang/ThreadLocal;",
                        "codechicken$multipart$asm$ScratchBitSet$_setter_$"
                                + "codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(Ljava/lang/ThreadLocal;)V",
                        "getBitSet()Ljava/util/BitSet;",
                        "freshBitSet()Ljava/util/BitSet;"),
                methods(ScratchBitSet.class, Modifier.PUBLIC | Modifier.ABSTRACT));
        assertEquals(
                signatures(
                        "$init$(Lcodechicken/multipart/asm/ScratchBitSet;)V",
                        "getBitSet(Lcodechicken/multipart/asm/ScratchBitSet;)Ljava/util/BitSet;",
                        "freshBitSet(Lcodechicken/multipart/asm/ScratchBitSet;)Ljava/util/BitSet;"),
                methods(ScratchBitSet$class.class, Modifier.PUBLIC | Modifier.STATIC));
    }

    @Test
    void lazilyAllocatesReusesAndReplacesPerOwnerStorage() {
        Scratch owner = new Scratch();
        Scratch other = new Scratch();
        ThreadLocal<BitSet> original = owner.storage;
        assertEquals(1, owner.setterCalls);
        assertNull(original.get());
        assertNotSame(original, other.storage);

        BitSet first = owner.getBitSet();
        assertEquals(2, owner.getterCalls, "The empty path re-reads the accessor before storing the new set");
        assertSame(first, original.get());
        first.set(4096);
        assertSame(first, owner.getBitSet());
        assertEquals(3, owner.getterCalls);
        assertTrue(first.get(4096), "Reading must not clear existing bits");
        assertNotSame(first, other.getBitSet());
        assertSame(first, owner.freshBitSet());
        assertTrue(first.isEmpty());

        BitSet supplied = new BitSet();
        supplied.set(7);
        ThreadLocal<BitSet> replacement = new ThreadLocal<>();
        replacement.set(supplied);
        owner.codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
                replacement);
        assertSame(supplied, owner.getBitSet());
        assertSame(supplied, owner.freshBitSet());
        assertTrue(supplied.isEmpty());
        assertSame(first, original.get());

        ScratchBitSet$class.$init$(owner);
        assertEquals(3, owner.setterCalls);
        assertNotSame(replacement, owner.storage);
        assertNull(owner.storage.get(), "Reinitialization replaces storage without eagerly allocating a BitSet");
        assertNotSame(supplied, owner.getBitSet());
    }

    @Test
    void isolatesThreadsWithoutClearingAnotherThreadsBits() throws Exception {
        Scratch owner = new Scratch();
        BitSet main = owner.getBitSet();
        main.set(13);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<BitSet> result = executor.submit(() -> {
                assertNull(owner.storage.get(), "Scratch state is not inherited by another thread");
                BitSet worker = owner.getBitSet();
                assertTrue(worker.isEmpty());
                worker.set(17);
                assertSame(worker, owner.getBitSet());
                assertSame(worker, owner.freshBitSet());
                assertTrue(worker.isEmpty());
                return worker;
            });
            assertNotSame(main, result.get(10, TimeUnit.SECONDS));
            assertSame(main, owner.getBitSet());
            assertEquals(1, main.cardinality());
            assertTrue(main.get(13));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void freshHelperDispatchesThroughTheOwnersGetOverride() {
        BitSet supplied = new BitSet();
        supplied.set(23);
        Scratch owner = new Scratch() {

            @Override
            public BitSet getBitSet() {
                return supplied;
            }
        };
        assertSame(supplied, ScratchBitSet$class.freshBitSet(owner));
        assertTrue(supplied.isEmpty());
        assertNull(owner.storage.get());
        assertEquals(0, owner.getterCalls, "freshBitSet must not bypass the public getBitSet override");
    }

    private static Set<String> signatures(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    private static Set<String> methods(Class<?> type, int modifiers) {
        Set<String> result = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            assertEquals(modifiers, method.getModifiers());
            result.add(method.getName() + Type.getMethodDescriptor(method));
        }
        return result;
    }

    private static class Scratch implements ScratchBitSet {

        private ThreadLocal<BitSet> storage;
        private int getterCalls;
        private int setterCalls;

        private Scratch() {
            ScratchBitSet$class.$init$(this);
        }

        @Override
        public ThreadLocal<BitSet> codechicken$multipart$asm$ScratchBitSet$$bitSets() {
            getterCalls++;
            return storage;
        }

        @Override
        public void codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
                ThreadLocal value) {
            setterCalls++;
            storage = value;
        }

        @Override
        public BitSet getBitSet() {
            return ScratchBitSet$class.getBitSet(this);
        }

        @Override
        public BitSet freshBitSet() {
            return ScratchBitSet$class.freshBitSet(this);
        }
    }
}
