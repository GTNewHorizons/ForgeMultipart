package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
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

import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

class MicroblockClassCharacterizationTest {

    private static final Set<String> MICROBLOCK_CLASS_METHODS = signatures(
            "baseTrait()Ljava/lang/Class;",
            "baseTraitId()I",
            "clientTrait()Ljava/lang/Class;",
            "clientTraitId()I",
            "create(ZI)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lcodechicken/lib/data/MCDataInput;)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lcodechicken/lib/data/MCDataInput;)Lcodechicken/multipart/TMultiPart;",
            "createPart(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)Lcodechicken/multipart/TMultiPart;",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F",
            "register()V");
    private static final Set<String> COMMON_CLASS_METHODS = signatures(
            "classes()[Lcodechicken/microblock/CommonMicroClass;",
            "getClassId()I",
            "getMicroClass(I)Lcodechicken/microblock/CommonMicroClass;",
            "itemSlot()I",
            "placementProperties()Lcodechicken/microblock/PlacementProperties;",
            "register(I)V",
            "registerMicroClass(Lcodechicken/microblock/CommonMicroClass;I)V");
    private static final Set<String> COMMON_COMPANION_METHODS = signatures(
            "classes()[Lcodechicken/microblock/CommonMicroClass;",
            "getMicroClass(I)Lcodechicken/microblock/CommonMicroClass;",
            "registerMicroClass(Lcodechicken/microblock/CommonMicroClass;I)V");

    @Test
    void keepsClassHierarchyCallableSurfaceAndSideAnnotations() throws Exception {
        assertAbstractClass(
                MicroblockClass.class,
                Object.class,
                new Class<?>[] { IPartFactory2.class },
                MICROBLOCK_CLASS_METHODS);
        assertAbstractClass(CommonMicroClass.class, MicroblockClass.class, new Class<?>[0], COMMON_CLASS_METHODS);
        assertConstructor(MicroblockClass.class);
        assertConstructor(CommonMicroClass.class);

        assertTrue(Modifier.isPublic(CommonMicroClass$.class.getModifiers()));
        assertTrue(Modifier.isFinal(CommonMicroClass$.class.getModifiers()));
        assertEquals(COMMON_COMPANION_METHODS, publicDeclaredMethods(CommonMicroClass$.class));

        assertField(MicroblockClass.class, "baseTraitId", int.class, Modifier.PRIVATE | Modifier.FINAL);
        Field clientTraitId = assertField(MicroblockClass.class, "clientTraitId", int.class, Modifier.PRIVATE);
        assertEquals(Side.CLIENT, clientTraitId.getAnnotation(SideOnly.class).value());
        assertField(MicroblockClass.class, "bitmap$0", boolean.class, Modifier.PRIVATE | Modifier.VOLATILE);
        assertEquals(3, MicroblockClass.class.getDeclaredFields().length);
        assertField(CommonMicroClass.class, "classId", int.class, Modifier.PRIVATE);
        assertEquals(1, CommonMicroClass.class.getDeclaredFields().length);

        SideOnly clientTrait = MicroblockClass.class.getDeclaredMethod("clientTrait").getAnnotation(SideOnly.class);
        assertEquals(Side.CLIENT, clientTrait.value());

        Field module = assertField(
                CommonMicroClass$.class,
                "MODULE$",
                CommonMicroClass$.class,
                Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        assertSame(CommonMicroClass$.MODULE$, module.get(null));
        assertField(CommonMicroClass$.class, "classes", CommonMicroClass[].class, Modifier.PRIVATE | Modifier.FINAL);
        assertEquals(2, CommonMicroClass$.class.getDeclaredFields().length);
    }

    @Test
    void registersAndLooksUpCommonClassesBeforeRejectingDuplicates() throws Exception {
        int id = 251;
        CommonMicroClass[] classes = CommonMicroClass.classes();
        assertSame(classes, CommonMicroClass$.MODULE$.classes());
        CommonMicroClass original = classes[id];
        assertNull(original);

        RecordingCommonMicroClass first = allocateInstance(RecordingCommonMicroClass.class);
        RecordingCommonMicroClass second = allocateInstance(RecordingCommonMicroClass.class);
        first.name = "first";
        first.idDuringRegistration = -1;
        second.name = "second";
        try {
            first.register(id);
            assertEquals(1, first.registrations);
            assertEquals(0, first.idDuringRegistration);
            assertEquals(id, first.getClassId());
            assertSame(first, CommonMicroClass.getMicroClass(id << 8 | 0xA5));
            assertSame(first, CommonMicroClass$.MODULE$.getMicroClass(id << 8 | 0x5A));

            IllegalArgumentException duplicate = assertThrows(
                    IllegalArgumentException.class,
                    () -> CommonMicroClass.registerMicroClass(second, id));
            assertEquals(
                    "Microblock class id " + id + " is already taken by first when adding second",
                    duplicate.getMessage());
            assertSame(first, classes[id]);
        } finally {
            classes[id] = original;
        }
    }

    @Test
    void keepsFactoryCallsOnTheLoadBearingGeneratorCompanion() throws IOException {
        ClassNode type = new ClassNode();
        new ClassReader(MicroblockClass.class.getName()).accept(type, 0);

        assertCall(
                method(type, "<init>", "()V"),
                Opcodes.INVOKEVIRTUAL,
                "codechicken/microblock/MicroblockGenerator$",
                "registerTrait",
                "(Ljava/lang/Class;)I");
        assertCall(
                method(type, "clientTraitId$lzycompute", "()I"),
                Opcodes.INVOKEVIRTUAL,
                "codechicken/microblock/MicroblockGenerator$",
                "registerTrait",
                "(Ljava/lang/Class;)I");
        assertCall(
                method(type, "create", "(ZI)Lcodechicken/microblock/Microblock;"),
                Opcodes.INVOKEVIRTUAL,
                "codechicken/microblock/MicroblockGenerator$",
                "create",
                "(Lcodechicken/microblock/MicroblockClass;IZ)Lcodechicken/microblock/Microblock;");
        assertCall(
                method(type, "register", "()V"),
                Opcodes.INVOKESTATIC,
                "codechicken/multipart/MultiPartRegistry",
                "registerParts",
                "(Lcodechicken/multipart/MultiPartRegistry$IPartFactory2;[Ljava/lang/String;)V");
    }

    private static void assertAbstractClass(Class<?> type, Class<?> superclass, Class<?>[] interfaces,
            Set<String> methods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isAbstract(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertArrayEquals(interfaces, type.getInterfaces());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertConstructor(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        assertTrue(Modifier.isPublic(constructor.getModifiers()));
        assertEquals(1, type.getDeclaredConstructors().length);
    }

    private static Field assertField(Class<?> owner, String name, Class<?> type, int modifiers) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertEquals(modifiers, field.getModifiers());
        return field;
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

    @SuppressWarnings("unchecked")
    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (T) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> Class<? extends T> rawClass(Class<?> type) {
        return (Class) type;
    }

    private static final class RecordingCommonMicroClass extends CommonMicroClass {

        private String name;
        private int registrations;
        private int idDuringRegistration = -1;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<? extends Microblock> baseTrait() {
            return rawClass(CornerMicroblock.class);
        }

        @Override
        public Class<? extends MicroblockClient> clientTrait() {
            return rawClass(CommonMicroblockClient.class);
        }

        @Override
        public float getResistanceFactor() {
            return 1;
        }

        @Override
        public int itemSlot() {
            return 0;
        }

        @Override
        public PlacementProperties placementProperties() {
            return null;
        }

        @Override
        public void register() {
            registrations++;
            idDuringRegistration = getClassId();
        }
    }
}
