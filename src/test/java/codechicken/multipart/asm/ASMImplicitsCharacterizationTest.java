package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class ASMImplicitsCharacterizationTest {

    @Test
    void keepsFacadeCompanionAndValueClassSurfaces() throws Exception {
        Set<String> facadeMethods = signatures(
                "ExtBitSet(Ljava/util/BitSet;)Ljava/util/BitSet;",
                "ExtClass(Ljava/lang/Class;)Ljava/lang/Class;",
                "nodeName(Ljava/lang/String;)Ljava/lang/String;");
        assertShape(ASMImplicits.class, Modifier.PUBLIC | Modifier.FINAL);
        assertShape(ASMImplicits$.class, Modifier.PUBLIC | Modifier.FINAL);
        assertEquals(facadeMethods, methods(ASMImplicits.class, Modifier.PUBLIC | Modifier.STATIC));
        assertEquals(facadeMethods, methods(ASMImplicits$.class, Modifier.PUBLIC));
        assertEquals(0, ASMImplicits.class.getDeclaredFields().length);
        assertModule(ASMImplicits$.class, ASMImplicits$.MODULE$);
        assertTrue(Modifier.isPrivate(ASMImplicits$.class.getDeclaredConstructor().getModifiers()));

        assertWrapper(ASMImplicits.ExtBitSet.class, BitSet.class, "bitset");
        assertEquals(
                signatures(
                        "bitset()Ljava/util/BitSet;",
                        "set(Ljava/util/BitSet;)Ljava/util/BitSet;",
                        "copy()Ljava/util/BitSet;",
                        "hashCode()I",
                        "equals(Ljava/lang/Object;)Z"),
                methods(ASMImplicits.ExtBitSet.class, Modifier.PUBLIC));
        assertWrapper(ASMImplicits.ExtClass.class, Class.class, "clazz");
        assertEquals(
                signatures(
                        "clazz()Ljava/lang/Class;",
                        "nodeName()Ljava/lang/String;",
                        "hashCode()I",
                        "equals(Ljava/lang/Object;)Z"),
                methods(ASMImplicits.ExtClass.class, Modifier.PUBLIC));

        assertShape(ASMImplicits.ExtBitSet$.class, Modifier.PUBLIC | Modifier.STATIC);
        assertShape(ASMImplicits.ExtClass$.class, Modifier.PUBLIC | Modifier.STATIC);
        assertModule(ASMImplicits.ExtBitSet$.class, ASMImplicits.ExtBitSet$.MODULE$);
        assertModule(ASMImplicits.ExtClass$.class, ASMImplicits.ExtClass$.MODULE$);
        assertEquals(Modifier.PUBLIC, ASMImplicits.ExtBitSet$.class.getDeclaredConstructor().getModifiers());
        assertEquals(Modifier.PUBLIC, ASMImplicits.ExtClass$.class.getDeclaredConstructor().getModifiers());
        assertEquals(
                signatures(
                        "set$extension(Ljava/util/BitSet;Ljava/util/BitSet;)Ljava/util/BitSet;",
                        "copy$extension(Ljava/util/BitSet;)Ljava/util/BitSet;",
                        "hashCode$extension(Ljava/util/BitSet;)I",
                        "equals$extension(Ljava/util/BitSet;Ljava/lang/Object;)Z"),
                methods(ASMImplicits.ExtBitSet$.class, Modifier.PUBLIC | Modifier.FINAL));
        assertEquals(
                signatures(
                        "nodeName$extension(Ljava/lang/Class;)Ljava/lang/String;",
                        "hashCode$extension(Ljava/lang/Class;)I",
                        "equals$extension(Ljava/lang/Class;Ljava/lang/Object;)Z"),
                methods(ASMImplicits.ExtClass$.class, Modifier.PUBLIC | Modifier.FINAL));
    }

    @Test
    void convertsNamesWithoutChangingTheIdentityConversionsOrNullRules() {
        BitSet bits = new BitSet();
        assertSame(bits, ASMImplicits.ExtBitSet(bits));
        assertSame(bits, ASMImplicits$.MODULE$.ExtBitSet(bits));
        assertNull(ASMImplicits.ExtBitSet(null));
        assertSame(String.class, ASMImplicits.ExtClass(String.class));
        assertSame(String.class, ASMImplicits$.MODULE$.ExtClass(String.class));
        assertNull(ASMImplicits.ExtClass(null));
        assertNull(ASMImplicits.nodeName(null));
        assertNull(ASMImplicits$.MODULE$.nodeName(null));
        assertEquals("", ASMImplicits.nodeName(""));
        assertEquals("a//b$C", ASMImplicits.nodeName("a..b$C"));
        String internal = "already/internal$Name";
        assertSame(internal, ASMImplicits.nodeName(internal));
        Class<?>[] types = { String.class, String[].class, int.class, int[][].class, void.class,
                ASMImplicits.ExtClass.class };
        for (Class<?> type : types) {
            String expected = type.getName().replace('.', '/');
            assertEquals(expected, ASMImplicits.nodeName(type.getName()));
            assertEquals(expected, ASMImplicits$.MODULE$.nodeName(type.getName()));
            assertEquals(expected, ASMImplicits.ExtClass$.MODULE$.nodeName$extension(type));
            ASMImplicits.ExtClass wrapped = new ASMImplicits.ExtClass(type);
            assertSame(type, wrapped.clazz());
            assertEquals(expected, wrapped.nodeName());
        }
        assertThrows(NullPointerException.class, () -> ASMImplicits.ExtClass$.MODULE$.nodeName$extension(null));
        assertThrows(NullPointerException.class, () -> new ASMImplicits.ExtClass(null).nodeName());
    }

    @Test
    void replacementClearsBeforeOrIncludingAliasingAndNullFailure() {
        TrackingBitSet destination = new TrackingBitSet();
        destination.set(4096);
        BitSet source = bits(0, 63, 64, 127);
        ASMImplicits.ExtBitSet wrapper = new ASMImplicits.ExtBitSet(destination);
        assertSame(destination, wrapper.bitset());
        assertSame(destination, wrapper.set(source));
        assertEquals("clear,or,", destination.calls);
        assertEquals(source, destination);
        assertEquals(bits(0, 63, 64, 127), source);

        destination.calls = "";
        assertSame(destination, ASMImplicits.ExtBitSet$.MODULE$.set$extension(destination, destination));
        assertTrue(destination.isEmpty(), "Self-replacement clears the set before reading it");
        assertEquals("clear,or,", destination.calls);

        destination.set(7);
        destination.calls = "";
        assertThrows(NullPointerException.class, () -> wrapper.set(null));
        assertTrue(destination.isEmpty(), "Null source failure happens after clearing the destination");
        assertEquals("clear,or,", destination.calls);
        assertThrows(NullPointerException.class, () -> ASMImplicits.ExtBitSet$.MODULE$.set$extension(null, source));
    }

    @Test
    void copiesIntoIndependentPlainBitSetsInsteadOfCloningTheSource() {
        TrackingBitSet source = new TrackingBitSet();
        source.set(0);
        source.set(64);
        source.set(4096);
        BitSet expected = bits(0, 64, 4096);
        BitSet first = ASMImplicits.ExtBitSet$.MODULE$.copy$extension(source);
        BitSet second = new ASMImplicits.ExtBitSet(source).copy();
        assertNotSame(first, second);
        for (BitSet copy : new BitSet[] { first, second }) {
            assertSame(BitSet.class, copy.getClass());
            assertNotSame(source, copy);
            assertEquals(expected, copy);
            copy.clear();
            assertEquals(expected, source);
        }
        assertEquals("", source.calls);
        BitSet empty = ASMImplicits.ExtBitSet$.MODULE$.copy$extension(new BitSet(8192));
        assertTrue(empty.isEmpty());
        assertEquals(new BitSet().size(), empty.size(), "Copy does not retain spare source capacity");
        assertThrows(NullPointerException.class, () -> ASMImplicits.ExtBitSet$.MODULE$.copy$extension(null));
    }

    @Test
    void boxedValueClassesKeepValueEqualityAndNullHashFailures() {
        BitSet value = bits(1, 65);
        ASMImplicits.ExtBitSet wrapper = new ASMImplicits.ExtBitSet(value);
        assertEquals(wrapper, new ASMImplicits.ExtBitSet(bits(1, 65)));
        assertFalse(wrapper.equals(new ASMImplicits.ExtBitSet(bits(1))));
        assertFalse(wrapper.equals(value));
        assertFalse(wrapper.equals(null));
        assertEquals(value.hashCode(), wrapper.hashCode());
        assertTrue(ASMImplicits.ExtBitSet$.MODULE$.equals$extension(value, wrapper));
        assertEquals(new ASMImplicits.ExtBitSet(null), new ASMImplicits.ExtBitSet(null));
        assertFalse(ASMImplicits.ExtBitSet$.MODULE$.equals$extension(null, wrapper));
        assertThrows(NullPointerException.class, () -> new ASMImplicits.ExtBitSet(null).hashCode());

        ASMImplicits.ExtClass clazz = new ASMImplicits.ExtClass(String.class);
        assertEquals(clazz, new ASMImplicits.ExtClass(String.class));
        assertFalse(clazz.equals(new ASMImplicits.ExtClass(Object.class)));
        assertFalse(clazz.equals(String.class));
        assertFalse(clazz.equals(null));
        assertEquals(String.class.hashCode(), clazz.hashCode());
        assertTrue(ASMImplicits.ExtClass$.MODULE$.equals$extension(String.class, clazz));
        assertEquals(new ASMImplicits.ExtClass(null), new ASMImplicits.ExtClass(null));
        assertFalse(ASMImplicits.ExtClass$.MODULE$.equals$extension(null, clazz));
        assertThrows(NullPointerException.class, () -> new ASMImplicits.ExtClass(null).hashCode());
    }

    private static void assertShape(Class<?> type, int modifiers) {
        assertEquals(modifiers, type.getModifiers());
        assertSame(Object.class, type.getSuperclass());
        assertEquals(0, type.getInterfaces().length);
    }

    private static void assertModule(Class<?> type, Object instance) throws Exception {
        assertEquals(1, type.getDeclaredFields().length);
        Field module = type.getDeclaredField("MODULE$");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(type, module.getType());
        assertSame(instance, module.get(null));
    }

    private static void assertWrapper(Class<?> type, Class<?> valueType, String fieldName) throws Exception {
        assertShape(type, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        assertSame(ASMImplicits.class, type.getDeclaringClass());
        assertEquals(1, type.getDeclaredFields().length);
        Field field = type.getDeclaredField(fieldName);
        assertEquals(Modifier.PRIVATE | Modifier.FINAL, field.getModifiers());
        assertSame(valueType, field.getType());
        assertEquals(Modifier.PUBLIC, type.getDeclaredConstructor(valueType).getModifiers());
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

    private static BitSet bits(int... positions) {
        BitSet bits = new BitSet();
        for (int position : positions) bits.set(position);
        return bits;
    }

    private static class TrackingBitSet extends BitSet {

        private String calls = "";

        @Override
        public void clear() {
            calls += "clear,";
            super.clear();
        }

        @Override
        public void or(BitSet set) {
            calls += "or,";
            super.or(set);
        }

        @Override
        public Object clone() {
            throw new AssertionError("Copy must construct a plain BitSet, not invoke clone");
        }
    }
}
