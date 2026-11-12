package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import scala.Predef$;
import scala.collection.Seq;

/**
 * Registration and construction need the mixin compiler, which cannot run outside Forge; the Forge suite covers those
 * paths. What is checked here is the surface downstream mods link against and the state the factory owns.
 */
class ASMMixinFactoryCharacterizationTest {

    @Test
    void keepsExactPublicSurface() throws Exception {
        assertEquals(Modifier.PUBLIC, ASMMixinFactory.class.getModifiers());
        assertSame(Object.class, ASMMixinFactory.class.getSuperclass());
        assertEquals(0, ASMMixinFactory.class.getInterfaces().length);
        assertEquals(
                signatures(
                        "baseType()Ljava/lang/Class;",
                        "construct(Ljava/util/BitSet;Lscala/collection/Seq;)Ljava/lang/Object;",
                        "getId(Ljava/lang/String;)I",
                        "registerTrait(Ljava/lang/Class;)I",
                        "registerTrait(Ljava/lang/String;)I",
                        "onCompiled(Ljava/lang/Class;Ljava/util/BitSet;)V",
                        "autoCompleteJavaTrait(Lorg/objectweb/asm/tree/ClassNode;)V",
                        "codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1"
                                + "(Lcodechicken/multipart/asm/ASMMixinCompiler$ClassInfo;)"
                                + "Lcodechicken/multipart/asm/ASMMixinCompiler$ClassInfo;",
                        "codechicken$multipart$asm$ASMMixinFactory$$checkParent$1"
                                + "(Lcodechicken/multipart/asm/ASMMixinCompiler$ClassInfo;Ljava/lang/String;)Z"),
                publicMethods(ASMMixinFactory.class));

        assertEquals(1, ASMMixinFactory.class.getDeclaredConstructors().length);
        assertEquals(
                "(Ljava/lang/Class;Lscala/collection/Seq;)V",
                Type.getConstructorDescriptor(ASMMixinFactory.class.getDeclaredConstructor(Class.class, Seq.class)));

        // MicroblockGenerator and MultipartMixinFactory extend this, and both override the two callbacks.
        for (String name : new String[] { "onCompiled", "autoCompleteJavaTrait" }) {
            for (Method method : ASMMixinFactory.class.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    assertTrue(Modifier.isPublic(method.getModifiers()));
                    assertFalse(Modifier.isFinal(method.getModifiers()));
                }
            }
        }
    }

    @Test
    void keepsTheSixFieldsThatHoldTheTraitAndClassCaches() throws Exception {
        assertField("baseType", Class.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("paramTypes", Seq.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("traitMap", scala.collection.mutable.Map.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("traits", scala.collection.mutable.ArrayBuffer.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("classMap", scala.collection.mutable.Map.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("ugenid", int.class, Modifier.PRIVATE);
        assertEquals(6, ASMMixinFactory.class.getDeclaredFields().length);
    }

    @Test
    void generatesClassNamesFromTheBaseTypeInRegistrationOrder() throws Exception {
        ASMMixinFactory<String> factory = factory(String.class);
        assertSame(String.class, factory.baseType());

        Method nextName = ASMMixinFactory.class.getDeclaredMethod("nextName");
        nextName.setAccessible(true);
        assertEquals("String_cmp$$0", nextName.invoke(factory));
        assertEquals("String_cmp$$1", nextName.invoke(factory));
        assertEquals("String_cmp$$2", nextName.invoke(factory));

        assertEquals("Object_cmp$$0", nextName.invoke(factory(Object.class)));
    }

    @Test
    void reportsAnUnregisteredTraitAsAMissingMapKey() {
        ASMMixinFactory<String> factory = factory(String.class);
        NoSuchElementException error = assertThrows(NoSuchElementException.class, () -> factory.getId("test/Missing"));
        assertEquals("key not found: test/Missing", error.getMessage());
    }

    @Test
    void leavesBothCallbacksAsNoOps() {
        ASMMixinFactory<String> factory = factory(String.class);
        ClassNode cnode = new ClassNode();
        cnode.visit(50, 0, "test/JTrait", null, "java/lang/Object", null);

        factory.autoCompleteJavaTrait(cnode);
        factory.onCompiled(String.class, new BitSet());

        assertEquals(0, cnode.methods.size());
    }

    private static <T> ASMMixinFactory<T> factory(Class<T> baseType) {
        return new ASMMixinFactory<>(baseType, Predef$.MODULE$.wrapRefArray(new Class<?>[0]));
    }

    private static void assertField(String name, Class<?> type, int modifiers) throws Exception {
        Field field = ASMMixinFactory.class.getDeclaredField(name);
        assertSame(type, field.getType());
        assertEquals(modifiers, field.getModifiers());
    }

    private static Set<String> publicMethods(Class<?> type) {
        Set<String> result = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                result.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return result;
    }

    private static Set<String> signatures(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }
}
