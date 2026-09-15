package codechicken.multipart.test;

import static codechicken.multipart.test.MixinClassesFunctionalTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;

class AbstractJavaMixinFunctionalTest {

    private static final String ROOT = "codechicken/multipart/test/abstractmixin/";

    @Test
    void abstractLayerKeepsItsContractAndConcreteChildCompletesTheComposite() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = abstractIntBase("LayerBase");
            Class<?> baseClass = scope.define(base);
            ClassNode layer = abstractInput("Layer", base.name);
            byte[] before = ASMHelper.createBytes(layer, 0);
            COMPILER.registerJavaTrait(layer);

            assertArrayEquals(before, ASMHelper.createBytes(layer, 0));
            ClassNode contract = COMPILER.classNode(layer.name);
            MethodNode required = method(contract, "required");
            assertEquals(ACC_PUBLIC | ACC_ABSTRACT, required.access);
            assertEquals(Arrays.asList("java/io/IOException"), required.exceptions);
            ClassNode helper = COMPILER.classNode(layer.name + "$class");
            assertFalse(helper.methods.stream().anyMatch(m -> ((MethodNode) m).name.equals("required")));
            MixinInfo layerInfo = COMPILER.getMixinInfo(layer.name).get();
            assertEquals(base.name, layerInfo.parent());
            assertEquals(1, layerInfo.methods().size());
            assertEquals("fromMixin", layerInfo.methods().apply(0).name);

            ClassNode child = concreteInput("Child", layer.name);
            COMPILER.registerJavaTrait(child);
            MixinInfo childInfo = COMPILER.getMixinInfo(child.name).get();
            assertEquals(base.name, childInfo.parent());
            assertEquals(1, childInfo.parentTraits().size());
            assertSame(layerInfo, childInfo.parentTraits().apply(0));

            Class<?> composite = COMPILER.mixinClasses(ROOT + "LayerComposite", base.name, seq(child.name));
            assertFalse(Modifier.isAbstract(composite.getModifiers()));
            Object value = composite.getConstructor(int.class).newInstance(41);
            assertEquals(41, composite.getField("seed").get(value));
            assertEquals(23, composite.getMethod("required").invoke(value));
            assertEquals(23, composite.getMethod("fromMixin").invoke(value));
            assertEquals(23, baseClass.getMethod("fromBase").invoke(value));
            String fieldName = layerInfo.fields().apply(0).accessName(layer.name);
            assertEquals(11, composite.getMethod(fieldName).invoke(value));
        }
    }

    @Test
    void abstractConstructorsStillRejectParametersAndMissingDirectSuperCalls() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode arguments = node(ROOT + "Arguments", "java/lang/Object", ACC_PUBLIC | ACC_ABSTRACT);
            arguments.visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, null);
            assertEquals(
                    "Constructor arguments are not permitted " + arguments.name + " as a mixin trait",
                    assertThrows(IllegalArgumentException.class, () -> COMPILER.registerJavaTrait(arguments))
                            .getMessage());

            ClassNode invalid = node(ROOT + "Invalid", ROOT + "ExpectedBase", ACC_PUBLIC | ACC_ABSTRACT);
            MethodVisitor constructor = invalid.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            constructor.visitInsn(RETURN);
            constructor.visitMaxs(1, 1);
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> COMPILER.registerJavaTrait(invalid));
            assertTrue(failure.getMessage().startsWith("Invalid constructor insn sequence " + invalid.name + "\n"));

            for (ClassNode input : Arrays.asList(arguments, invalid)) {
                assertFalse(scope.bytes.contains(input.name));
                assertFalse(scope.bytes.contains(input.name + "$class"));
                assertFalse(scope.mixins.contains(input.name));
            }
        }
    }

    @Test
    void concreteMixinAlreadyTargetsAnAbstractBaseAndPreservesVirtualDispatch() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode base = abstractBase("Base");
            Class<?> baseClass = scope.define(base);
            ClassNode input = concreteInput("Concrete", base.name);
            COMPILER.registerJavaTrait(input);

            MixinInfo info = COMPILER.getMixinInfo(input.name).get();
            assertEquals(base.name, info.parent());
            assertEquals(2, info.methods().size());
            Class<?> composite = COMPILER.mixinClasses(ROOT + "Composite", base.name, seq(input.name));
            assertFalse(Modifier.isAbstract(composite.getModifiers()));
            Object value = composite.getConstructor().newInstance();
            assertEquals(23, composite.getMethod("required").invoke(value));
            assertEquals(23, composite.getMethod("fromMixin").invoke(value));
            assertEquals(23, baseClass.getMethod("fromBase").invoke(value));
        }
    }

    private static ClassNode abstractBase(String suffix) {
        ClassNode base = node(ROOT + suffix, "java/lang/Object", ACC_PUBLIC | ACC_ABSTRACT);
        MethodVisitor constructor = base.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        base.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "required", "()I", null, null);
        MethodVisitor fromBase = base.visitMethod(ACC_PUBLIC, "fromBase", "()I", null, null);
        fromBase.visitVarInsn(ALOAD, 0);
        fromBase.visitMethodInsn(INVOKEVIRTUAL, base.name, "required", "()I", false);
        fromBase.visitInsn(IRETURN);
        fromBase.visitMaxs(1, 1);
        return base;
    }

    private static ClassNode abstractIntBase(String suffix) {
        ClassNode base = node(ROOT + suffix, "java/lang/Object", ACC_PUBLIC | ACC_ABSTRACT);
        base.visitField(ACC_PUBLIC, "seed", "I", null, null);
        MethodVisitor constructor = base.visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitVarInsn(ILOAD, 1);
        constructor.visitFieldInsn(PUTFIELD, base.name, "seed", "I");
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(2, 2);
        base.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "required", "()I", null, null);
        MethodVisitor fromBase = base.visitMethod(ACC_PUBLIC, "fromBase", "()I", null, null);
        fromBase.visitVarInsn(ALOAD, 0);
        fromBase.visitMethodInsn(INVOKEVIRTUAL, base.name, "required", "()I", false);
        fromBase.visitInsn(IRETURN);
        fromBase.visitMaxs(1, 1);
        return base;
    }

    private static ClassNode abstractInput(String suffix, String parent) {
        ClassNode input = node(ROOT + suffix, parent, ACC_PUBLIC | ACC_ABSTRACT);
        input.visitField(ACC_PRIVATE, "initialized", "I", null, null);
        MethodVisitor constructor = input.visitMethod(ACC_PROTECTED, "<init>", "()V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitInsn(ICONST_0);
        constructor.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "(I)V", false);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitIntInsn(BIPUSH, 11);
        constructor.visitFieldInsn(PUTFIELD, input.name, "initialized", "I");
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(2, 1);
        input.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "required", "()I", null, new String[] { "java/io/IOException" });
        MethodVisitor fromMixin = input.visitMethod(ACC_PUBLIC, "fromMixin", "()I", null, null);
        fromMixin.visitVarInsn(ALOAD, 0);
        fromMixin.visitMethodInsn(INVOKEVIRTUAL, input.name, "required", "()I", false);
        fromMixin.visitInsn(IRETURN);
        fromMixin.visitMaxs(1, 1);
        return input;
    }

    private static ClassNode concreteInput(String suffix, String parent) {
        ClassNode input = node(ROOT + suffix, parent, ACC_PUBLIC);
        MethodVisitor constructor = input.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        MethodVisitor required = input.visitMethod(ACC_PUBLIC, "required", "()I", null, null);
        required.visitIntInsn(BIPUSH, 23);
        required.visitInsn(IRETURN);
        required.visitMaxs(1, 1);
        MethodVisitor fromMixin = input.visitMethod(ACC_PUBLIC, "fromMixin", "()I", null, null);
        fromMixin.visitVarInsn(ALOAD, 0);
        fromMixin.visitMethodInsn(INVOKEVIRTUAL, input.name, "required", "()I", false);
        fromMixin.visitInsn(IRETURN);
        fromMixin.visitMaxs(1, 1);
        return input;
    }

    private static MethodNode method(ClassNode owner, String name) {
        return (MethodNode) owner.methods.stream().filter(m -> ((MethodNode) m).name.equals(name)).findFirst().get();
    }
}
