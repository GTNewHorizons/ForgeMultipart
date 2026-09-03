package codechicken.multipart.test;

import static codechicken.multipart.test.MixinClassesFunctionalTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.MatchError;
import scala.collection.JavaConversions;

class JavaTraitRegistrationFunctionalTest {

    private static final String ROOT = "codechicken/multipart/test/javatrait/";

    @Test
    void rejectsInterfacesThenInnerClassesThenAbstractClassesWithoutPublishing() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode input = input("Rejected", "java/lang/Object");
            input.access |= ACC_INTERFACE | ACC_ABSTRACT;
            input.visitInnerClass(input.name + "$Nested", input.name, "Nested", ACC_PUBLIC);
            assertFailure(
                    input,
                    "Cannot register java interface " + input.name
                            + " as a mixin trait. Try register passThroughInterface");
            input.access &= ~ACC_INTERFACE;
            assertFailure(
                    input,
                    "Inner classes are not permitted for " + input.name + " as a java mixin trait. Use scala");
            input.innerClasses.clear();
            assertFailure(input, "Cannot register abstract class " + input.name + " as a java mixin trait. Use scala");
            assertFalse(scope.bytes.contains(input.name));
            assertFalse(scope.bytes.contains(input.name + "$class"));
            assertFalse(scope.mixins.contains(input.name));
        }
    }

    @Test
    void executesInitializationFieldAccessVirtualSelfCallsAndSuperDispatchWithoutMutatingInput() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode parent = base("JavaBase");
            scope.define(parent);
            ClassNode input = input("Executable", parent.name);
            input.sourceFile = "OriginalJavaTrait.java";
            input.visitField(ACC_PRIVATE, "count", "I", null, null);
            MethodNode constructor = method(input, "<init>");
            constructor.instructions.remove(constructor.instructions.getLast());
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitIntInsn(BIPUSH, 12);
            constructor.visitFieldInsn(PUTFIELD, input.name, "count", "I");
            constructor.visitInsn(RETURN);
            constructor.visitMaxs(2, 1);
            MethodVisitor increment = input
                    .visitMethod(ACC_PROTECTED, "increment", "(I)I", null, new String[] { "java/io/IOException" });
            increment.visitVarInsn(ALOAD, 0);
            increment.visitInsn(DUP);
            increment.visitFieldInsn(GETFIELD, input.name, "count", "I");
            increment.visitVarInsn(ILOAD, 1);
            increment.visitInsn(IADD);
            increment.visitFieldInsn(PUTFIELD, input.name, "count", "I");
            increment.visitVarInsn(ALOAD, 0);
            increment.visitFieldInsn(GETFIELD, input.name, "count", "I");
            increment.visitInsn(IRETURN);
            increment.visitMaxs(3, 2);
            MethodVisitor call = input.visitMethod(ACC_PUBLIC, "call", "()I", null, null);
            call.visitVarInsn(ALOAD, 0);
            call.visitInsn(ICONST_3);
            call.visitMethodInsn(INVOKEVIRTUAL, input.name, "increment", "(I)I", false);
            call.visitInsn(IRETURN);
            call.visitMaxs(2, 1);
            MethodVisitor score = input.visitMethod(ACC_PUBLIC, "score", "()I", null, null);
            score.visitVarInsn(ALOAD, 0);
            score.visitMethodInsn(INVOKESPECIAL, parent.name, "score", "()I", false);
            score.visitInsn(ICONST_2);
            score.visitInsn(IADD);
            score.visitInsn(IRETURN);
            score.visitMaxs(2, 1);
            byte[] before = ASMHelper.createBytes(input, 0);
            COMPILER.internalDefine(input.name, before);
            COMPILER.registerJavaTrait(input);
            assertArrayEquals(before, ASMHelper.createBytes(input, 0));
            MixinInfo info = COMPILER.getMixinInfo(input.name).get();
            assertEquals(parent.name, info.parent());
            assertEquals(Arrays.asList("score()I"), JavaConversions.seqAsJavaList(info.supers()));
            assertEquals(Arrays.asList("increment(I)I", "call()I", "score()I"), signatures(info));
            assertEquals("OriginalJavaTrait.java", COMPILER.classNode(input.name + "$class").sourceFile);
            Object result = COMPILER.mixinClasses(ROOT + "ExecutableComposite", parent.name, seq(input.name))
                    .getConstructor(int.class).newInstance(20);
            assertEquals(15, result.getClass().getMethod("call").invoke(result));
            assertEquals(18, result.getClass().getMethod("call").invoke(result));
            assertEquals(22, result.getClass().getMethod("score").invoke(result));
            assertEquals(18, result.getClass().getMethod(input.name.replace('/', '$') + "$$count").invoke(result));
            assertArrayEquals(
                    new Class<?>[] { java.io.IOException.class },
                    result.getClass().getMethod("increment", int.class).getExceptionTypes());
        }
    }

    @Test
    void matchingAccessorsAreSkippedBeforeConversionIncludingAnOtherwiseInvalidBody() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode parent = base("AccessorJavaBase");
            scope.define(parent);
            ClassNode input = input("Accessor", parent.name);
            input.visitField(ACC_PUBLIC, "value", "I", null, null);
            MethodVisitor getter = input.visitMethod(ACC_PUBLIC, "value", "()I", null, null);
            getter.visitInsn(ICONST_1);
            getter.visitIntInsn(NEWARRAY, T_INT);
            getter.visitInsn(ARETURN);
            getter.visitMaxs(1, 1);
            constant(input, "value", "(I)I", 91, ACC_PUBLIC);
            constant(input, "hidden", "()I", 6, ACC_PRIVATE);
            COMPILER.registerJavaTrait(input);
            MixinInfo info = COMPILER.getMixinInfo(input.name).get();
            assertEquals(Arrays.asList("value(I)I"), signatures(info));
            ClassNode helper = COMPILER.classNode(input.name + "$class");
            assertEquals(ACC_PRIVATE | ACC_STATIC, method(helper, "hidden").access);
            assertEquals(1, helper.methods.stream().filter(m -> ((MethodNode) m).name.equals("value")).count());
            Object result = COMPILER.mixinClasses(ROOT + "AccessorComposite", parent.name, seq(input.name))
                    .getConstructor(int.class).newInstance(0);
            result.getClass().getMethod("value_$eq", int.class).invoke(result, 28);
            assertEquals(28, result.getClass().getMethod("value").invoke(result));
            assertEquals(91, result.getClass().getMethod("value", int.class).invoke(result, 8));
        }
    }

    @Test
    void parentTraitsRetainTheirBaseAndDeduplicateTheImplementedInterface() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode parent = base("ParentJavaBase");
            scope.define(parent);
            ClassNode first = input("Parent", parent.name);
            constant(first, "fromParent", "()I", 17, ACC_PUBLIC);
            COMPILER.registerJavaTrait(first);
            MixinInfo firstInfo = COMPILER.getMixinInfo(first.name).get();
            ClassNode second = input("Child", first.name);
            second.interfaces.add(first.name);
            constant(second, "fromChild", "()I", 23, ACC_PUBLIC);
            COMPILER.registerJavaTrait(second);
            MixinInfo childInfo = COMPILER.getMixinInfo(second.name).get();
            assertEquals(parent.name, childInfo.parent());
            assertEquals(1, childInfo.parentTraits().size());
            assertSame(firstInfo, childInfo.parentTraits().apply(0));
            assertEquals(Arrays.asList(first.name), COMPILER.classNode(second.name).interfaces);
            Object value = COMPILER.mixinClasses(ROOT + "ChildComposite", parent.name, seq(second.name))
                    .getConstructor(int.class).newInstance(9);
            assertEquals(17, value.getClass().getMethod("fromParent").invoke(value));
            assertEquals(23, value.getClass().getMethod("fromChild").invoke(value));
            assertEquals(9, value.getClass().getMethod("score").invoke(value));
        }
    }

    @Test
    void sideOnlyAnnotationsDoNotStripJavaMethodsOnTheServer() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode parent = base("SideJavaBase");
            scope.define(parent);
            ClassNode input = input("SideOnly", parent.name);
            constant(input, "clientMarked", "()I", 37, ACC_PUBLIC);
            method(input, "clientMarked").visitAnnotation("Lcpw/mods/fml/relauncher/SideOnly;", true)
                    .visitEnum("value", "Lcpw/mods/fml/relauncher/Side;", "CLIENT");
            COMPILER.registerJavaTrait(input);
            Object value = COMPILER.mixinClasses(ROOT + "SideComposite", parent.name, seq(input.name))
                    .getConstructor(int.class).newInstance(0);
            assertEquals(37, value.getClass().getMethod("clientMarked").invoke(value));
        }
    }

    @Test
    void constructorArgumentsStaticInitializersAndInvalidSuperSequencesFailBeforePublishing() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode args = input("Arguments", "java/lang/Object");
            method(args, "<init>").desc = "(I)V";
            assertFailure(args, "Constructor arguments are not permitted " + args.name + " as a mixin trait");
            ClassNode statics = input("StaticInitializer", "java/lang/Object");
            constant(statics, "earlier", "()I", 5, ACC_PUBLIC);
            MethodVisitor clinit = statics.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.visitInsn(RETURN);
            assertFailure(statics, "Static initialisers are not permitted " + statics.name + " as a mixin trait");
            ClassNode invalid = input("InvalidConstructor", "java/lang/Object");
            MethodNode constructor = method(invalid, "<init>");
            constructor.instructions.remove(constructor.instructions.getFirst());
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> COMPILER.registerJavaTrait(invalid));
            assertTrue(failure.getMessage().startsWith("Invalid constructor insn sequence " + invalid.name + "\n"));
            for (ClassNode node : Arrays.asList(args, statics, invalid)) {
                assertFalse(scope.bytes.contains(node.name));
                assertFalse(scope.bytes.contains(node.name + "$class"));
                assertFalse(scope.mixins.contains(node.name));
            }
        }
    }

    @Test
    void primitiveArrayAndForeignFieldFailuresRetainTheirOriginalTypesAndInput() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode arrays = input("PrimitiveArray", "java/lang/Object");
            MethodVisitor array = arrays.visitMethod(ACC_PUBLIC, "array", "()[I", null, null);
            array.visitInsn(ICONST_1);
            array.visitIntInsn(NEWARRAY, T_INT);
            array.visitInsn(ARETURN);
            array.visitMaxs(1, 1);
            byte[] before = ASMHelper.createBytes(arrays, 0);
            assertEquals(
                    "188 (of class java.lang.Integer)",
                    assertThrows(MatchError.class, () -> COMPILER.registerJavaTrait(arrays)).getMessage());
            assertArrayEquals(before, ASMHelper.createBytes(arrays, 0));
            ClassNode foreign = input("ForeignField", "java/lang/Object");
            MethodVisitor read = foreign.visitMethod(ACC_PUBLIC, "read", "(Ljava/awt/Point;)I", null, null);
            read.visitVarInsn(ALOAD, 1);
            read.visitFieldInsn(GETFIELD, "java/awt/Point", "x", "I");
            read.visitInsn(IRETURN);
            read.visitMaxs(1, 2);
            assertEquals(
                    "key not found: x",
                    assertThrows(java.util.NoSuchElementException.class, () -> COMPILER.registerJavaTrait(foreign))
                            .getMessage());
            assertFalse(scope.mixins.contains(arrays.name));
            assertFalse(scope.mixins.contains(foreign.name));
        }
    }

    @Test
    void inheritedVirtualCallsKeepTheExistingDescriptorShapedCast() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode parent = base("InheritedJavaBase");
            scope.define(parent);
            ClassNode input = input("Inherited", parent.name);
            MethodVisitor read = input.visitMethod(ACC_PUBLIC, "read", "()I", null, null);
            read.visitVarInsn(ALOAD, 0);
            read.visitMethodInsn(INVOKEVIRTUAL, input.name, "score", "()I", false);
            read.visitInsn(IRETURN);
            read.visitMaxs(1, 1);
            COMPILER.internalDefine(input.name, ASMHelper.createBytes(input, 0));
            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> COMPILER.registerJavaTrait(input));
            assertInstanceOf(ClassFormatError.class, failure.getCause());
            ClassNode helper = COMPILER.classNode(input.name + "$class");
            MethodNode output = method(helper, "read");
            AbstractInsnNode cast = output.instructions.getFirst().getNext();
            assertEquals("L" + parent.name + ";", ((TypeInsnNode) cast).desc);
            assertEquals(parent.name, ((MethodInsnNode) cast.getNext()).owner);
            assertFalse(scope.mixins.contains(input.name));
        }
    }

    @Test
    void definitionFailureKeepsHelperAndByteCachesButDoesNotPublishMixinMetadata() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode input = input("AlreadyLoaded", "java/lang/Object");
            scope.define(input);
            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> COMPILER.registerJavaTrait(input));
            assertInstanceOf(LinkageError.class, failure.getCause());
            assertTrue(scope.bytes.contains(input.name + "$class"));
            assertEquals(ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, COMPILER.classNode(input.name).access);
            assertFalse(scope.mixins.contains(input.name));
            assertNotNull(scope.loader.findClass((input.name + "$class").replace('/', '.')));
        }
    }

    private static ClassNode input(String suffix, String parent) {
        ClassNode node = node(ROOT + suffix, parent, ACC_PUBLIC);
        MethodVisitor constructor = node.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        return node;
    }

    private static void constant(ClassNode node, String name, String desc, int value, int access) {
        MethodVisitor method = node.visitMethod(access, name, desc, null, null);
        method.visitLdcInsn(value);
        method.visitInsn(IRETURN);
        method.visitMaxs(1, desc.equals("(I)I") ? 2 : 1);
    }

    private static MethodNode method(ClassNode node, String name) {
        return (MethodNode) node.methods.stream().filter(m -> ((MethodNode) m).name.equals(name)).findFirst().get();
    }

    private static List<String> signatures(MixinInfo info) {
        return JavaConversions.seqAsJavaList(info.methods()).stream().map(m -> m.name + m.desc)
                .collect(Collectors.toList());
    }

    private static void assertFailure(ClassNode input, String message) {
        assertEquals(
                message,
                assertThrows(IllegalArgumentException.class, () -> COMPILER.registerJavaTrait(input)).getMessage());
    }
}
