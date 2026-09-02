package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ScratchBitSet;

class MultipartGeneratorCharacterizationTest {

    private static final Set<String> FACADE = signatures(
            "freshBitSet()Ljava/util/BitSet;",
            "getBitSet()Ljava/util/BitSet;",
            "registerPassThroughInterface(Ljava/lang/String;)V",
            "registerPassThroughInterface(Ljava/lang/String;ZZ)V",
            "registerTrait(Ljava/lang/String;Ljava/lang/String;)V",
            "registerTrait(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
            "silentAddTile(Lnet/minecraft/world/World;Lcodechicken/lib/vec/BlockCoord;Lnet/minecraft/tileentity/TileEntity;)V");

    @Test
    void keepsExactFacadeCompanionAndScalaMapSurfaceWithoutInitializingForge() throws Exception {
        assertEquals(FACADE, publicMethods(MultipartGenerator.class));
        Set<String> companion = new TreeSet<>(FACADE);
        companion.addAll(
                signatures(
                        "codechicken$multipart$asm$ScratchBitSet$$bitSets()Ljava/lang/ThreadLocal;",
                        "codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq"
                                + "(Ljava/lang/ThreadLocal;)V",
                        "codechicken$multipart$MultipartGenerator$$interfaceTraitMap(Z)Lscala/collection/mutable/Map;",
                        "codechicken$multipart$MultipartGenerator$$traitsForPart(Lcodechicken/multipart/TMultiPart;Z)Ljava/util/BitSet;",
                        "addPart(Lnet/minecraft/world/World;Lcodechicken/lib/vec/BlockCoord;Lcodechicken/multipart/TMultiPart;)"
                                + "Lcodechicken/multipart/TileMultipart;",
                        "generateCompositeTile(Lnet/minecraft/tileentity/TileEntity;Lscala/collection/Iterable;Z)"
                                + "Lcodechicken/multipart/TileMultipart;",
                        "partRemoved(Lcodechicken/multipart/TileMultipart;)Lcodechicken/multipart/TileMultipart;",
                        "registerTileClass(Ljava/lang/Class;Ljava/util/BitSet;)V"));
        assertEquals(companion, publicMethods(MultipartGenerator$.class));
        for (Class<?> type : new Class<?>[] { MultipartGenerator.class, MultipartGenerator$.class }) {
            assertTrue(Modifier.isPublic(type.getModifiers()));
            assertTrue(Modifier.isFinal(type.getModifiers()));
            assertSame(Object.class, type.getSuperclass());
        }
        assertArrayEquals(new Class<?>[] { ScratchBitSet.class }, MultipartGenerator$.class.getInterfaces());
        assertEquals(0, MultipartGenerator.class.getDeclaredFields().length);
        assertEquals(8, MultipartGenerator$.class.getDeclaredFields().length);
        for (String name : new String[] { "tileTraitMap", "interfaceTraitMap_c", "interfaceTraitMap_s",
                "partTraitMap_c", "partTraitMap_s" }) {
            Field field = MultipartGenerator$.class.getDeclaredField(name);
            assertSame(scala.collection.mutable.Map.class, field.getType());
            assertEquals(Modifier.PRIVATE | Modifier.FINAL, field.getModifiers());
        }
        Field module = MultipartGenerator$.class.getDeclaredField("MODULE$");
        assertSame(MultipartGenerator$.class, module.getType());
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertTrue(Modifier.isPrivate(MultipartGenerator$.class.getDeclaredConstructor().getModifiers()));
    }

    @Test
    void keepsSideSafeProxyCallbackAndCompanionOnlyGenerationCall() throws Exception {
        assertCall(
                MultipartGenerator$.class,
                "registerTileClass",
                Opcodes.INVOKEVIRTUAL,
                "codechicken/multipart/handler/MultipartProxy$",
                "onTileClassBuilt",
                "(Ljava/lang/Class;)V");
        assertCall(
                codechicken.multipart.asm.MultipartMixinFactory$.class,
                "onCompiled",
                Opcodes.INVOKEVIRTUAL,
                "codechicken/multipart/MultipartGenerator$",
                "registerTileClass",
                "(Ljava/lang/Class;Ljava/util/BitSet;)V");
        assertCall(
                MultipartHelper.class,
                "createTileFromParts",
                Opcodes.INVOKEVIRTUAL,
                "codechicken/multipart/MultipartGenerator$",
                "generateCompositeTile",
                "(Lnet/minecraft/tileentity/TileEntity;Lscala/collection/Iterable;Z)Lcodechicken/multipart/TileMultipart;");
    }

    private static Set<String> publicMethods(Class<?> type) {
        Set<String> result = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()))
                result.add(method.getName() + Type.getMethodDescriptor(method));
        }
        return result;
    }

    private static Set<String> signatures(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    private static void assertCall(Class<?> type, String methodName, int opcode, String owner, String name,
            String descriptor) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(type.getName()).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (!method.name.equals(methodName)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == opcode && call.owner.equals(owner)
                        && call.name.equals(name)
                        && call.desc.equals(descriptor))
                    return;
            }
        }
        throw new AssertionError("Missing call " + owner + '.' + name + descriptor);
    }
}
