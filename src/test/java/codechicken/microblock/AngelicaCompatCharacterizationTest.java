package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

class AngelicaCompatCharacterizationTest {

    private static final Set<String> PUBLIC_METHODS = new TreeSet<>(
            Arrays.asList(
                    "resetShaderMaterialOverride()Ljava/lang/Object;",
                    "setShaderMaterialOverride(Lnet/minecraft/block/Block;I)Ljava/lang/Object;"));

    @Test
    void keepsPublicObjectReturningSurface() {
        assertTrue(Modifier.isPublic(AngelicaCompat.class.getModifiers()));
        assertFalse(Modifier.isFinal(AngelicaCompat.class.getModifiers()));
        assertEquals(Object.class, AngelicaCompat.class.getSuperclass());
        assertEquals(0, AngelicaCompat.class.getInterfaces().length);
        assertEquals(0, AngelicaCompat.class.getDeclaredFields().length);

        Constructor<?>[] constructors = AngelicaCompat.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPublic(constructors[0].getModifiers()));
        assertEquals(0, constructors[0].getParameterTypes().length);

        Set<String> methods = new TreeSet<>();
        for (Method method : AngelicaCompat.class.getDeclaredMethods()) {
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertFalse(Modifier.isStatic(method.getModifiers()));
            methods.add(method.getName() + Type.getMethodDescriptor(method));
        }
        assertEquals(PUBLIC_METHODS, methods);
    }

    @Test
    void keepsCapturingGuardAndClassCastFallback() throws IOException {
        ClassNode type = new ClassNode();
        new ClassReader(AngelicaCompat.class.getName()).accept(type, 0);

        assertHook(
                method(type, "setShaderMaterialOverride", "(Lnet/minecraft/block/Block;I)Ljava/lang/Object;"),
                "setShaderMaterialOverride",
                "(Lnet/minecraft/block/Block;I)V");
        assertHook(
                method(type, "resetShaderMaterialOverride", "()Ljava/lang/Object;"),
                "resetShaderMaterialOverride",
                "()V");
    }

    private static MethodNode method(ClassNode type, String name, String descriptor) {
        for (MethodNode method : type.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + name + descriptor);
    }

    private static void assertHook(MethodNode method, String hookName, String hookDescriptor) {
        assertEquals(1, method.tryCatchBlocks.size());
        TryCatchBlockNode fallback = method.tryCatchBlocks.get(0);
        assertEquals("java/lang/ClassCastException", fallback.type);

        assertTrue(
                hasField(
                        method,
                        Opcodes.GETSTATIC,
                        "net/minecraft/client/renderer/Tessellator",
                        "instance",
                        "Lnet/minecraft/client/renderer/Tessellator;"));
        assertTrue(
                hasType(method, Opcodes.INSTANCEOF, "com/gtnewhorizon/gtnhlib/client/renderer/CapturingTessellator"));
        assertTrue(hasCall(method, "net/coderbot/iris/Iris", hookName, hookDescriptor));
        assertTrue(hasField(method, Opcodes.GETSTATIC, "scala/runtime/BoxedUnit", "UNIT", "Lscala/runtime/BoxedUnit;"));
        assertTrue(hasField(method, Opcodes.GETSTATIC, "scala/Unit$", "MODULE$", "Lscala/Unit$;"));
    }

    private static boolean hasField(MethodNode method, int opcode, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.getOpcode() == opcode && owner.equals(field.owner)
                        && name.equals(field.name)
                        && descriptor.equals(field.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasType(MethodNode method, int opcode, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof TypeInsnNode) {
                TypeInsnNode type = (TypeInsnNode) instruction;
                if (type.getOpcode() == opcode && descriptor.equals(type.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC && owner.equals(call.owner)
                        && name.equals(call.name)
                        && descriptor.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }
}
