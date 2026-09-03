package codechicken.multipart.test;

import static codechicken.multipart.test.MixinClassesFunctionalTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;

class AbstractJavaMixinFunctionalTest {

    private static final String ROOT = "codechicken/multipart/test/abstractmixin/";

    @Test
    void abstractInputIsRejectedBeforeItsMissingParentOrMethodsAreInspected() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode input = node(ROOT + "Rejected", ROOT + "MissingParent", ACC_PUBLIC | ACC_ABSTRACT);
            input.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "required", "()I", null, null);
            MethodVisitor malformed = input.visitMethod(ACC_PUBLIC, "malformed", "()[I", null, null);
            malformed.visitInsn(ICONST_1);
            malformed.visitIntInsn(NEWARRAY, T_INT);
            malformed.visitInsn(ARETURN);
            malformed.visitMaxs(1, 1);

            assertEquals(
                    "Cannot register abstract class " + input.name + " as a java mixin trait. Use scala",
                    assertThrows(IllegalArgumentException.class, () -> COMPILER.registerJavaTrait(input)).getMessage());
            assertFalse(scope.info.contains(input.superName));
            assertFalse(scope.bytes.contains(input.name));
            assertFalse(scope.bytes.contains(input.name + "$class"));
            assertFalse(scope.mixins.contains(input.name));
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
}
