package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Surface and bytecode smoke checks only. Generation reaches Forge through ObfMapping; the Forge suite checks copyFrom
 * guards and emitted instructions, generated tiles and pass-through delegation.
 */
class MultipartMixinFactoryCharacterizationTest {

    @Test
    void keepsExactFacadeSurface() throws Exception {
        assertEquals(Modifier.PUBLIC | Modifier.FINAL, MultipartMixinFactory.class.getModifiers());
        assertSame(Object.class, MultipartMixinFactory.class.getSuperclass());
        // onCompiled, autoCompleteJavaTrait and both mangled helpers are the ledger's additive forwarders.
        assertEquals(
                signatures(
                        "baseType()Ljava/lang/Class;",
                        "construct(Ljava/util/BitSet;Lscala/collection/Seq;)Ljava/lang/Object;",
                        "getId(Ljava/lang/String;)I",
                        "registerTrait(Ljava/lang/Class;)I",
                        "registerTrait(Ljava/lang/String;)I",
                        "onCompiled(Ljava/lang/Class;Ljava/util/BitSet;)V",
                        "autoCompleteJavaTrait(Lorg/objectweb/asm/tree/ClassNode;)V",
                        "generatePassThroughTrait(Ljava/lang/String;)Ljava/lang/String;",
                        "codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1"
                                + "(Lcodechicken/multipart/asm/ASMMixinCompiler$ClassInfo;)"
                                + "Lcodechicken/multipart/asm/ASMMixinCompiler$ClassInfo;",
                        "codechicken$multipart$asm$ASMMixinFactory$$checkParent$1"
                                + "(Lcodechicken/multipart/asm/ASMMixinCompiler$ClassInfo;Ljava/lang/String;)Z"),
                publicMethods(MultipartMixinFactory.class));
        for (Method method : MultipartMixinFactory.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()));
        }
        assertEquals(0, MultipartMixinFactory.class.getDeclaredFields().length);
        assertTrue(Modifier.isPrivate(MultipartMixinFactory.class.getDeclaredConstructor().getModifiers()));
    }

    @Test
    void keepsTheCompanionAsATileMultipartFactory() throws Exception {
        assertEquals(Modifier.PUBLIC | Modifier.FINAL, MultipartMixinFactory$.class.getModifiers());
        assertSame(ASMMixinFactory.class, MultipartMixinFactory$.class.getSuperclass());
        assertEquals(
                signatures(
                        "onCompiled(Ljava/lang/Class;Ljava/util/BitSet;)V",
                        "autoCompleteJavaTrait(Lorg/objectweb/asm/tree/ClassNode;)V",
                        "generatePassThroughTrait(Ljava/lang/String;)Ljava/lang/String;",
                        "codechicken$multipart$asm$MultipartMixinFactory$$methods$1"
                                + "(Lorg/objectweb/asm/tree/ClassNode;)Lscala/collection/immutable/Map;",
                        "codechicken$multipart$asm$MultipartMixinFactory$$generatePassThroughMethod$1"
                                + "(Lorg/objectweb/asm/tree/MethodNode;Ljava/lang/String;Ljava/lang/String;"
                                + "Ljava/lang/String;Ljava/lang/String;Lcodechicken/lib/asm/CC_ClassWriter;"
                                + "Lscala/runtime/ObjectRef;)V"),
                publicMethods(MultipartMixinFactory$.class));

        Field module = MultipartMixinFactory$.class.getDeclaredField("MODULE$");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(MultipartMixinFactory$.class, module.getType());
        assertEquals(1, MultipartMixinFactory$.class.getDeclaredFields().length);
        assertTrue(Modifier.isPrivate(MultipartMixinFactory$.class.getDeclaredConstructor().getModifiers()));

        ClassNode node = companion();
        assertTrue(typeConstants(node, "<init>").contains("codechicken/multipart/TileMultipart"));
        assertCall(node, "onCompiled", "codechicken/multipart/MultipartGenerator$", "registerTileClass");
    }

    @Test
    void namesAndFillsThePassThroughTrait() throws Exception {
        ClassNode node = companion();
        Set<String> constants = constants(node, "generatePassThroughTrait");
        for (String constant : new String[] { "T", "$$PassThrough", "impl", "L", ";", "bindPart",
                "(Lcodechicken/multipart/TMultiPart;)V", "partRemoved", "(Lcodechicken/multipart/TMultiPart;I)V",
                "canAddPart", "(Lcodechicken/multipart/TMultiPart;)Z", "<init>", "()V",
                "codechicken/multipart/TileMultipart", "Unable to generate pass through trait for: ",
                " class not found.", " is not an interface." }) {
            assertTrue(constants.contains(constant), "missing constant " + constant);
        }
        assertCall(node, "generatePassThroughTrait", "codechicken/multipart/asm/ASMMixinCompiler$", "classNode");
        assertCall(node, "generatePassThroughTrait", "codechicken/multipart/asm/ASMMixinCompiler$", "internalDefine");
        assertCall(
                node,
                "generatePassThroughTrait",
                "codechicken/multipart/asm/MultipartMixinFactory$",
                "registerTrait");
        assertCall(
                node,
                "codechicken$multipart$asm$MultipartMixinFactory$$generatePassThroughMethod$1",
                "codechicken/multipart/asm/ASMMixinCompiler$",
                "finishBridgeCall");
    }

    private static ClassNode companion() throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(MultipartMixinFactory$.class.getName()).accept(node, 0);
        return node;
    }

    private static Set<String> constants(ClassNode node, String methodName) {
        Set<String> found = new TreeSet<>();
        for (AbstractInsnNode instruction : instructions(node, methodName)) {
            if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof String) {
                found.add((String) ((LdcInsnNode) instruction).cst);
            }
        }
        return found;
    }

    private static Set<String> typeConstants(ClassNode node, String methodName) {
        Set<String> found = new TreeSet<>();
        for (AbstractInsnNode instruction : instructions(node, methodName)) {
            if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof Type) {
                found.add(((Type) ((LdcInsnNode) instruction).cst).getInternalName());
            }
        }
        return found;
    }

    private static void assertCall(ClassNode node, String methodName, String owner, String name) {
        for (AbstractInsnNode instruction : instructions(node, methodName)) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals(owner) && (name == null || call.name.equals(name))) return;
            }
        }
        throw new AssertionError("Missing call " + owner + '.' + name + " in " + methodName);
    }

    private static AbstractInsnNode[] instructions(ClassNode node, String methodName) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(methodName)) return method.instructions.toArray();
        }
        throw new AssertionError("Missing method " + methodName);
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
