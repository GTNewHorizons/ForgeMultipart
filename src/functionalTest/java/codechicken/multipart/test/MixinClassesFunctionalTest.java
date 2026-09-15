package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import net.minecraft.launchwrapper.LaunchClassLoader;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.FieldMixin;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import codechicken.multipart.asm.DebugPrinter$;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.Map;

class MixinClassesFunctionalTest {

    static final ASMMixinCompiler$ COMPILER = ASMMixinCompiler$.MODULE$;
    static final String PREFIX = "codechicken/multipart/test/composition/";

    @Test
    void emptyTraitsResolveTheBaseAndIgnoreTheGeneratedName() throws Exception {
        try (Scope scope = new Scope()) {
            Class<?> base = scope.define(base("EmptyBase"));
            assertSame(base, COMPILER.mixinClasses(null, Type.getInternalName(base), seq()));
            assertSame(base, COMPILER.mixinClasses("ignored", base.getName(), seq()));
            assertFalse(scope.bytes.contains("ignored"));
        }
    }

    @Test
    void diamondParentsInitializeOnceAndSuperCallsFollowLinearization() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = base("DiamondBase");
            scope.define(base);
            MixinInfo parent = scope.trait("Parent", base.name, seq(), 1);
            MixinInfo left = scope.trait("Left", base.name, seq(parent), 10);
            MixinInfo right = scope.trait("Right", base.name, seq(parent), 100);
            Object value = COMPILER.mixinClasses(PREFIX + "Diamond", base.name, seq(left.name(), right.name()))
                    .getConstructor(int.class).newInstance(7);
            assertEquals("BParentLeftRight", value.getClass().getField("trace").get(value));
            assertEquals(118, value.getClass().getMethod("score").invoke(value));
            assertEquals("Right", value.getClass().getMethod("winner").invoke(value));
            assertEquals(
                    Arrays.asList(left.name().replace('/', '.'), right.name().replace('/', '.')),
                    Arrays.asList(
                            value.getClass().getInterfaces()[0].getName(),
                            value.getClass().getInterfaces()[1].getName()));
            Object reversed = COMPILER.mixinClasses(PREFIX + "Reversed", base.name, seq(right.name(), left.name()))
                    .getConstructor(int.class).newInstance(3);
            assertEquals("BParentRightLeft", reversed.getClass().getField("trace").get(reversed));
            assertEquals(114, reversed.getClass().getMethod("score").invoke(reversed));
            assertEquals("Left", reversed.getClass().getMethod("winner").invoke(reversed));
        }
    }

    @Test
    void selectedConstructorForwardsArgumentsAndDoesNotCopyExceptionsOrOtherOverloads() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = base("ConstructorBase");
            scope.define(base);
            MixinInfo trait = scope.trait("ConstructorTrait", base.name, seq(), 2);
            Class<?> result = COMPILER.mixinClasses(PREFIX + "Constructor", base.name, seq(trait.name()));
            assertEquals(1, result.getDeclaredConstructors().length);
            assertArrayEquals(new Class<?>[0], result.getConstructor(int.class).getExceptionTypes());
            Object value = result.getConstructor(int.class).newInstance(41);
            assertEquals(43, result.getMethod("score").invoke(value));
            assertThrows(NoSuchMethodException.class, result::getConstructor);
        }
    }

    @Test
    void fieldsRetainPrivateStorageMangledAccessorsAndNarrowValueCategories() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = base("FieldsBase");
            scope.define(base);
            FieldMixin[] fields = { new FieldMixin("flag", "Z", ACC_PUBLIC), new FieldMixin("byteValue", "B", 0),
                    new FieldMixin("shortValue", "S", 0), new FieldMixin("charValue", "C", 0),
                    new FieldMixin("integer", "I", ACC_PRIVATE), new FieldMixin("real", "F", 0),
                    new FieldMixin("object", "Ljava/lang/Object;", 0), new FieldMixin("array", "[I", 0) };
            MixinInfo trait = scope.trait("FieldsTrait", base.name, seq(), 1, fields);
            Class<?> result = COMPILER.mixinClasses(PREFIX + "Fields", base.name, seq(trait.name()));
            Object first = result.getConstructor(int.class).newInstance(0);
            Object second = result.getConstructor(int.class).newInstance(0);
            Object marker = new Object();
            Object[] values = { true, (byte) -12, (short) 1234, '\u03bb', -99, 1.25F, marker, new int[] { 2, 7 } };
            Class<?>[] types = { boolean.class, byte.class, short.class, char.class, int.class, float.class,
                    Object.class, int[].class };
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].accessName(trait.name());
                assertEquals(Modifier.PRIVATE, result.getDeclaredField(name).getModifiers());
                Method setter = result.getMethod(name + "_$eq", types[i]);
                Method getter = result.getMethod(name);
                Object initial = getter.invoke(second);
                setter.invoke(first, values[i]);
                assertEquals(values[i], getter.invoke(first));
                assertEquals(initial, getter.invoke(second));
            }
            assertThrows(NoSuchMethodException.class, () -> result.getMethod("integer"));
        }
    }

    @Test
    void wideFieldGettersRetainTheExistingStackOverflowVerificationFailure() throws Exception {
        for (String descriptor : Arrays.asList("J", "D")) {
            try (Scope scope = new Scope()) {
                ClassNode base = base("WideBase" + descriptor);
                scope.define(base);
                MixinInfo trait = scope
                        .trait("WideTrait" + descriptor, base.name, seq(), 1, new FieldMixin("wide", descriptor, 0));
                Class<?> generated = COMPILER.mixinClasses(PREFIX + "Wide" + descriptor, base.name, seq(trait.name()));
                VerifyError failure = assertThrows(VerifyError.class, () -> generated.getConstructor(int.class));
                assertTrue(failure.getMessage().contains("Operand stack overflow"));
                ClassNode output = COMPILER.classNode(PREFIX + "Wide" + descriptor);
                MethodNode getter = (MethodNode) output.methods.stream()
                        .filter(m -> ((MethodNode) m).name.equals("wide")).findFirst().get();
                assertEquals(1, getter.maxStack);
            }
        }
    }

    @Test
    void covariantBridgeDispatchesToTheWinningTraitAndKeepsItsOwnExceptions() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = base("CovariantBase");
            MethodVisitor value = base.visitMethod(
                    ACC_PUBLIC,
                    "winner",
                    "()Ljava/lang/Object;",
                    null,
                    new String[] { "java/io/IOException" });
            value.visitLdcInsn("base");
            value.visitInsn(ARETURN);
            value.visitMaxs(1, 1);
            Class<?> baseClass = scope.define(base);
            MixinInfo trait = scope.trait("CovariantTrait", base.name, seq(), 1);
            Class<?> result = COMPILER.mixinClasses(PREFIX + "Covariant", base.name, seq(trait.name()));
            Object instance = result.getConstructor(int.class).newInstance(0);
            assertEquals("CovariantTrait", baseClass.getMethod("winner").invoke(instance));
            Method bridge = Arrays.stream(result.getDeclaredMethods()).filter(Method::isBridge).findFirst().get();
            assertTrue(bridge.isSynthetic());
            assertEquals(Object.class, bridge.getReturnType());
            assertArrayEquals(new Class<?>[] { java.io.IOException.class }, bridge.getExceptionTypes());
            assertArrayEquals(
                    new Class<?>[] { IllegalStateException.class },
                    result.getMethod("winner").getExceptionTypes());
        }
    }

    @Test
    void missingMixinFailsBeforeBaseLookupAndMissingConstructorBeforeDefinition() throws Exception {
        try (Scope scope = new Scope()) {
            java.util.NoSuchElementException missing = assertThrows(
                    java.util.NoSuchElementException.class,
                    () -> COMPILER.mixinClasses(PREFIX + "Missing", "missing/base", seq("missing/trait")));
            assertTrue(missing.getMessage().contains("missing/trait"));
            assertFalse(scope.info.contains("missing/base"));
            ClassNode base = base("NoConstructorBase");
            base.methods.clear();
            scope.define(base);
            MixinInfo trait = scope.trait("NoConstructorTrait", base.name, seq(), 1);
            assertEquals(
                    "None.get",
                    assertThrows(
                            java.util.NoSuchElementException.class,
                            () -> COMPILER.mixinClasses(PREFIX + "NoConstructor", base.name, seq(trait.name())))
                                    .getMessage());
            assertFalse(scope.bytes.contains(PREFIX + "NoConstructor"));
        }
    }

    @Test
    void missingSuperFailsBeforeDefinitionButDuplicateInterfacesReachTheJvmAndCache() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = base("FailureBase");
            scope.define(base);
            MixinInfo trait = scope.trait("FailureTrait", base.name, seq(), 1);
            MixinInfo invalid = new MixinInfo(
                    trait.name(),
                    trait.parent(),
                    trait.parentTraits(),
                    trait.fields(),
                    trait.methods(),
                    seq("absent()I"));
            scope.mixins.put(trait.name(), invalid);
            assertEquals(
                    "None.get",
                    assertThrows(
                            java.util.NoSuchElementException.class,
                            () -> COMPILER.mixinClasses(PREFIX + "MissingSuper", base.name, seq(trait.name())))
                                    .getMessage());
            assertFalse(scope.bytes.contains(PREFIX + "MissingSuper"));
            scope.mixins.put(trait.name(), trait);
            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> COMPILER.mixinClasses(PREFIX + "Duplicate", base.name, seq(trait.name(), trait.name())));
            assertInstanceOf(ClassFormatError.class, failure.getCause());
            assertTrue(scope.bytes.contains(PREFIX + "Duplicate"));
            assertEquals(
                    Arrays.asList(trait.name(), trait.name()),
                    COMPILER.classNode(PREFIX + "Duplicate").interfaces);
        }
    }

    static ClassNode base(String suffix) {
        ClassNode node = node(PREFIX + suffix, "java/lang/Object", ACC_PUBLIC);
        node.visitField(ACC_PUBLIC, "trace", "Ljava/lang/String;", null, null);
        node.visitField(ACC_PUBLIC, "seed", "I", null, null);
        MethodVisitor constructor = node
                .visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, new String[] { "java/io/IOException" });
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitLdcInsn("B");
        constructor.visitFieldInsn(PUTFIELD, node.name, "trace", "Ljava/lang/String;");
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitVarInsn(ILOAD, 1);
        constructor.visitFieldInsn(PUTFIELD, node.name, "seed", "I");
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(2, 2);
        MethodVisitor empty = node.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        empty.visitVarInsn(ALOAD, 0);
        empty.visitInsn(ICONST_0);
        empty.visitMethodInsn(INVOKESPECIAL, node.name, "<init>", "(I)V", false);
        empty.visitInsn(RETURN);
        empty.visitMaxs(2, 1);
        MethodVisitor score = node.visitMethod(ACC_PUBLIC, "score", "()I", null, null);
        score.visitVarInsn(ALOAD, 0);
        score.visitFieldInsn(GETFIELD, node.name, "seed", "I");
        score.visitInsn(IRETURN);
        score.visitMaxs(1, 1);
        return node;
    }

    static ClassNode node(String name, String parent, int access) {
        ClassNode node = new ClassNode();
        node.visit(V1_6, access, name, null, parent, null);
        return node;
    }

    @SafeVarargs
    static <T> Seq<T> seq(T... values) {
        return JavaConversions.asScalaBuffer(Arrays.asList(values)).toList();
    }

    static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    static class Scope implements AutoCloseable {

        final Field loaderField = field(ASMMixinCompiler$.class, "cl");
        final Object previousLoader = loaderField.get(COMPILER);
        final LaunchClassLoader loader = new LaunchClassLoader(new URL[0]) {

            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                Class<?> loaded = findLoadedClass(name);
                return loaded == null ? super.findClass(name) : loaded;
            }
        };
        final Map<String, byte[]> bytes = cache("traitByteMap");
        final Map<String, ClassInfo> info = cache("infoCache");
        final Map<String, MixinInfo> mixins = cache("mixinMap");
        final java.util.Map<String, byte[]> oldBytes = new HashMap<>(JavaConversions.mapAsJavaMap(bytes));
        final java.util.Map<String, ClassInfo> oldInfo = new HashMap<>(JavaConversions.mapAsJavaMap(info));
        final java.util.Map<String, MixinInfo> oldMixins = new HashMap<>(JavaConversions.mapAsJavaMap(mixins));
        final Field debug = field(DebugPrinter$.class, "debug");
        final boolean oldDebug = DebugPrinter$.MODULE$.debug();
        final Field counter = field(DebugPrinter$.class, "permGenUsed");
        final int oldCounter = counter.getInt(DebugPrinter$.MODULE$);

        Scope() throws Exception {
            loaderField.set(COMPILER, loader);
            debug.setBoolean(DebugPrinter$.MODULE$, true);
        }

        Class<?> define(ClassNode node) {
            return COMPILER.define(node.name, ASMHelper.createBytes(node, 0));
        }

        MixinInfo trait(String suffix, String base, Seq<MixinInfo> parents, int increment, FieldMixin... fields) {
            String name = PREFIX + suffix;
            ClassNode contract = node(name, "java/lang/Object", ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
            for (MixinInfo parent : JavaConversions.seqAsJavaList(parents)) contract.interfaces.add(parent.name());
            MethodNode score = (MethodNode) contract.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "score", "()I", null, null);
            MethodNode winner = (MethodNode) contract.visitMethod(
                    ACC_PUBLIC | ACC_ABSTRACT,
                    "winner",
                    "()Ljava/lang/String;",
                    null,
                    new String[] { "java/lang/IllegalStateException" });
            String superName = name.replace('/', '$') + "$$super$score";
            contract.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, superName, "()I", null, null);
            for (FieldMixin f : fields) {
                contract.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, f.accessName(name), "()" + f.desc(), null, null);
                contract.visitMethod(
                        ACC_PUBLIC | ACC_ABSTRACT,
                        f.accessName(name) + "_$eq",
                        "(" + f.desc() + ")V",
                        null,
                        null);
            }
            define(contract);
            ClassNode helper = node(name + "$class", "java/lang/Object", ACC_PUBLIC);
            MethodVisitor init = helper.visitMethod(ACC_PUBLIC | ACC_STATIC, "$init$", "(L" + name + ";)V", null, null);
            init.visitVarInsn(ALOAD, 0);
            init.visitTypeInsn(CHECKCAST, base);
            init.visitInsn(DUP);
            init.visitFieldInsn(GETFIELD, base, "trace", "Ljava/lang/String;");
            init.visitLdcInsn(suffix);
            init.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/String",
                    "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    false);
            init.visitFieldInsn(PUTFIELD, base, "trace", "Ljava/lang/String;");
            init.visitInsn(RETURN);
            init.visitMaxs(3, 1);
            MethodVisitor value = helper.visitMethod(ACC_PUBLIC | ACC_STATIC, "score", "(L" + name + ";)I", null, null);
            value.visitVarInsn(ALOAD, 0);
            value.visitMethodInsn(INVOKEINTERFACE, name, superName, "()I", true);
            value.visitLdcInsn(increment);
            value.visitInsn(IADD);
            value.visitInsn(IRETURN);
            value.visitMaxs(2, 1);
            MethodVisitor winning = helper
                    .visitMethod(ACC_PUBLIC | ACC_STATIC, "winner", "(L" + name + ";)Ljava/lang/String;", null, null);
            winning.visitLdcInsn(suffix);
            winning.visitInsn(ARETURN);
            winning.visitMaxs(1, 1);
            define(helper);
            MixinInfo result = new MixinInfo(name, base, parents, seq(fields), seq(score, winner), seq("score()I"));
            mixins.put(name, result);
            return result;
        }

        @SuppressWarnings("unchecked")
        private <T> Map<String, T> cache(String name) throws Exception {
            return (Map<String, T>) field(ASMMixinCompiler$.class, name).get(COMPILER);
        }

        @Override
        public void close() throws Exception {
            loaderField.set(COMPILER, previousLoader);
            debug.setBoolean(DebugPrinter$.MODULE$, oldDebug);
            counter.setInt(DebugPrinter$.MODULE$, oldCounter);
            bytes.clear();
            oldBytes.forEach(bytes::put);
            info.clear();
            oldInfo.forEach(info::put);
            mixins.clear();
            oldMixins.forEach(mixins::put);
            loader.close();
        }
    }
}
