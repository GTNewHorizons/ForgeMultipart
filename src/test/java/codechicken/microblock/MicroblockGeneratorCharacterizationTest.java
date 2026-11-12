package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinFactory;
import codechicken.multipart.asm.ScratchBitSet;

class MicroblockGeneratorCharacterizationTest {

    private static final Set<String> FACADE_METHODS = signatures(
            "baseType()Ljava/lang/Class;",
            "construct(Ljava/util/BitSet;Lscala/collection/Seq;)Ljava/lang/Object;",
            "create(Lcodechicken/microblock/MicroblockClass;IZ)Lcodechicken/microblock/Microblock;",
            "freshBitSet()Ljava/util/BitSet;",
            "getBitSet()Ljava/util/BitSet;",
            "getId(Ljava/lang/String;)I",
            "registerTrait(Ljava/lang/Class;)I",
            "registerTrait(Ljava/lang/String;)I");
    private static final Set<String> COMPANION_METHODS = signatures(
            "codechicken$multipart$asm$ScratchBitSet$$bitSets()Ljava/lang/ThreadLocal;",
            "codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq"
                    + "(Ljava/lang/ThreadLocal;)V",
            "create(Lcodechicken/microblock/MicroblockClass;IZ)Lcodechicken/microblock/Microblock;",
            "freshBitSet()Ljava/util/BitSet;",
            "getBitSet()Ljava/util/BitSet;");
    private static final Set<String> GENERATED_MATERIAL_METHODS = signatures(
            "addTraits(Ljava/util/BitSet;Lcodechicken/microblock/MicroblockClass;Z)V");

    @Test
    void keepsFacadeCompanionAndNestedMaterialTraitSurface() throws Exception {
        assertTrue(Modifier.isPublic(MicroblockGenerator.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroblockGenerator.class.getModifiers()));
        assertSame(Object.class, MicroblockGenerator.class.getSuperclass());
        assertEquals(FACADE_METHODS, publicDeclaredMethods(MicroblockGenerator.class));
        assertEquals(0, MicroblockGenerator.class.getDeclaredFields().length);

        assertTrue(Modifier.isPublic(MicroblockGenerator$.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroblockGenerator$.class.getModifiers()));
        assertSame(ASMMixinFactory.class, MicroblockGenerator$.class.getSuperclass());
        assertArrayEquals(new Class<?>[] { ScratchBitSet.class }, MicroblockGenerator$.class.getInterfaces());
        assertEquals(COMPANION_METHODS, publicDeclaredMethods(MicroblockGenerator$.class));

        Field module = MicroblockGenerator$.class.getDeclaredField("MODULE$");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(MicroblockGenerator$.class, module.getType());
        assertSame(MicroblockGenerator$.MODULE$, module.get(null));

        Field scratch = MicroblockGenerator$.class.getDeclaredField("codechicken$multipart$asm$ScratchBitSet$$bitSets");
        assertSame(ThreadLocal.class, scratch.getType());
        assertTrue(Modifier.isPrivate(scratch.getModifiers()));
        assertEquals(2, MicroblockGenerator$.class.getDeclaredFields().length);

        Constructor<?> constructor = MicroblockGenerator$.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        assertEquals(1, MicroblockGenerator$.class.getDeclaredConstructors().length);

        Class<?> generatedMaterial = MicroblockGenerator.IGeneratedMaterial.class;
        assertTrue(generatedMaterial.isInterface());
        assertTrue(Modifier.isPublic(generatedMaterial.getModifiers()));
        assertTrue(Modifier.isStatic(generatedMaterial.getModifiers()));
        assertSame(MicroblockGenerator.class, generatedMaterial.getDeclaringClass());
        assertEquals(GENERATED_MATERIAL_METHODS, publicDeclaredMethods(generatedMaterial));
        assertEquals(0, generatedMaterial.getDeclaredFields().length);
        assertArrayEquals(new Class<?>[] { generatedMaterial }, MicroblockGenerator.class.getDeclaredClasses());

        assertSame(Microblock.class, MicroblockGenerator.baseType());
        assertSame(Microblock.class, MicroblockGenerator$.MODULE$.baseType());
    }

    @Test
    void keepsReplaceableClearedThreadLocalScratchBitSet() throws Exception {
        MicroblockGenerator$ generator = MicroblockGenerator$.MODULE$;
        ThreadLocal<BitSet> original = generator.codechicken$multipart$asm$ScratchBitSet$$bitSets();
        ThreadLocal<BitSet> replacement = new ThreadLocal<>();
        generator.codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
                replacement);
        try {
            assertSame(replacement, generator.codechicken$multipart$asm$ScratchBitSet$$bitSets());
            BitSet first = generator.getBitSet();
            assertSame(first, MicroblockGenerator.getBitSet());
            first.set(7);
            assertSame(first, MicroblockGenerator.freshBitSet());
            assertTrue(first.isEmpty());

            AtomicReference<BitSet> otherFirst = new AtomicReference<>();
            AtomicReference<BitSet> otherFresh = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                BitSet bitSet = generator.getBitSet();
                bitSet.set(11);
                otherFirst.set(bitSet);
                otherFresh.set(generator.freshBitSet());
            });
            thread.start();
            thread.join();

            assertSame(otherFirst.get(), otherFresh.get());
            assertTrue(otherFresh.get().isEmpty());
            assertFalse(first == otherFirst.get());
        } finally {
            generator
                    .codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
                            original);
        }
    }

    @Test
    void keepsLoadBearingCompanionAndGenerationCalls() throws IOException {
        ClassNode facade = classNode(MicroblockGenerator.class);
        assertCall(
                method(
                        facade,
                        "create",
                        "(Lcodechicken/microblock/MicroblockClass;IZ)Lcodechicken/microblock/Microblock;"),
                Opcodes.INVOKEVIRTUAL,
                "codechicken/microblock/MicroblockGenerator$",
                "create",
                "(Lcodechicken/microblock/MicroblockClass;IZ)Lcodechicken/microblock/Microblock;");

        ClassNode companion = classNode(MicroblockGenerator$.class);
        MethodNode create = method(
                companion,
                "create",
                "(Lcodechicken/microblock/MicroblockClass;IZ)Lcodechicken/microblock/Microblock;");
        assertCall(
                create,
                Opcodes.INVOKESTATIC,
                "codechicken/microblock/MicroMaterialRegistry",
                "getMaterial",
                "(I)Lcodechicken/microblock/MicroMaterialRegistry$IMicroMaterial;");
        assertCall(
                create,
                Opcodes.INVOKEINTERFACE,
                "codechicken/microblock/MicroblockGenerator$IGeneratedMaterial",
                "addTraits",
                "(Ljava/util/BitSet;Lcodechicken/microblock/MicroblockClass;Z)V");
        assertCall(
                create,
                Opcodes.INVOKEVIRTUAL,
                "codechicken/microblock/MicroblockGenerator$",
                "construct",
                "(Ljava/util/BitSet;Lscala/collection/Seq;)Ljava/lang/Object;");

        MethodNode constructor = method(companion, "<init>", "()V");
        assertCall(
                constructor,
                Opcodes.INVOKESPECIAL,
                "codechicken/multipart/asm/ASMMixinFactory",
                "<init>",
                "(Ljava/lang/Class;Lscala/collection/Seq;)V");
        assertCall(
                constructor,
                Opcodes.INVOKESTATIC,
                "codechicken/multipart/asm/ScratchBitSet$class",
                "$init$",
                "(Lcodechicken/multipart/asm/ScratchBitSet;)V");
    }

    private static ClassNode classNode(Class<?> type) throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(type.getName()).accept(node, 0);
        return node;
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> methods = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                methods.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return methods;
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }

    private static MethodNode method(ClassNode type, String name, String descriptor) {
        for (MethodNode method : type.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + name + descriptor);
    }

    private static void assertCall(MethodNode method, int opcode, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == opcode && owner.equals(call.owner)
                        && name.equals(call.name)
                        && descriptor.equals(call.desc)) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing call " + owner + '.' + name + descriptor);
    }
}
