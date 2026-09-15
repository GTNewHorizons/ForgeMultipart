package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSIENT;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.multipart.asm.MultipartMixinFactory$;

class MultipartMixinFactoryFunctionalTest {

    private static final String TILE = "codechicken/multipart/TileMultipart";
    private static final String COPY_DESC = "(L" + TILE + ";)V";

    @Test
    void leavesEmptyAndTransientOnlyTraitsUntouched() {
        for (boolean transientField : new boolean[] { false, true }) {
            ClassNode node = trait();
            if (transientField) node.visitField(ACC_PUBLIC | ACC_TRANSIENT, "cache", "I", null, null);
            byte[] before = ASMHelper.createBytes(node, 0);

            MultipartMixinFactory$.MODULE$.autoCompleteJavaTrait(node);

            assertEquals(0, node.methods.size());
            assertArrayEquals(before, ASMHelper.createBytes(node, 0));
        }
    }

    @Test
    void preservesAnExistingCopyFromWithoutEmittingAnything() {
        ClassNode node = trait();
        node.visitField(ACC_PUBLIC, "saved", "I", null, null);
        MethodNode existing = new MethodNode(ACC_PUBLIC, "copyFrom", COPY_DESC, null, null);
        existing.visitInsn(NOP);
        existing.visitInsn(RETURN);
        existing.visitMaxs(0, 2);
        node.methods.add(existing);
        byte[] before = ASMHelper.createBytes(node, 0);

        MultipartMixinFactory$.MODULE$.autoCompleteJavaTrait(node);

        assertEquals(1, node.methods.size());
        assertSame(existing, node.methods.get(0));
        assertArrayEquals(before, ASMHelper.createBytes(node, 0));
    }

    @Test
    void emitsSuperCallThenTypeGuardAndOnlyNonTransientFieldCopies() {
        ClassNode node = trait();
        node.visitField(ACC_PUBLIC, "first", "I", null, null);
        node.visitField(ACC_PUBLIC | ACC_TRANSIENT, "cache", "I", null, null);
        node.visitField(ACC_PUBLIC, "second", "Ljava/lang/String;", null, null);

        MultipartMixinFactory$.MODULE$.autoCompleteJavaTrait(node);

        assertEquals(1, node.methods.size());
        MethodNode copy = node.methods.get(0);
        assertEquals("copyFrom", copy.name);
        assertEquals(COPY_DESC, copy.desc);
        assertEquals(ACC_PUBLIC, copy.access);
        assertEquals(2, copy.maxStack);
        assertEquals(2, copy.maxLocals);
        List<Integer> opcodes = new ArrayList<>();
        List<Integer> locals = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        for (AbstractInsnNode instruction : copy.instructions.toArray()) {
            if (instruction.getOpcode() >= 0) opcodes.add(instruction.getOpcode());
            if (instruction instanceof VarInsnNode) locals.add(((VarInsnNode) instruction).var);
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                assertEquals(TILE, call.owner);
                assertEquals("copyFrom", call.name);
                assertEquals(COPY_DESC, call.desc);
            }
            if (instruction instanceof TypeInsnNode) {
                assertEquals(node.name, ((TypeInsnNode) instruction).desc);
            }
            if (instruction instanceof JumpInsnNode) {
                assertEquals(RETURN, ((JumpInsnNode) instruction).label.getNext().getOpcode());
            }
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                assertEquals(node.name, field.owner);
                fields.add(field.name + ':' + field.desc);
            }
        }
        assertEquals(
                Arrays.asList(
                        ALOAD,
                        ALOAD,
                        INVOKESPECIAL,
                        ALOAD,
                        INSTANCEOF,
                        IFEQ,
                        ALOAD,
                        ALOAD,
                        GETFIELD,
                        PUTFIELD,
                        ALOAD,
                        ALOAD,
                        GETFIELD,
                        PUTFIELD,
                        RETURN),
                opcodes);
        assertEquals(Arrays.asList(0, 1, 1, 0, 1, 0, 1), locals);
        assertEquals(
                Arrays.asList("first:I", "first:I", "second:Ljava/lang/String;", "second:Ljava/lang/String;"),
                fields);

        byte[] before = ASMHelper.createBytes(node, 0);
        MultipartMixinFactory$.MODULE$.autoCompleteJavaTrait(node);
        assertArrayEquals(before, ASMHelper.createBytes(node, 0), "A second completion must not duplicate copyFrom");
    }

    private static ClassNode trait() {
        ClassNode node = new ClassNode();
        node.visit(V1_6, ACC_PUBLIC, "test/CopyFromFixture", null, TILE, null);
        return node;
    }
}
