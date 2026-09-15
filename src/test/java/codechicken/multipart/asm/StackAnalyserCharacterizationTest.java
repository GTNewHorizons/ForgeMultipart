package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import codechicken.multipart.asm.StackAnalyser.ArrayLength;
import codechicken.multipart.asm.StackAnalyser.ArrayLoad;
import codechicken.multipart.asm.StackAnalyser.BinaryOp;
import codechicken.multipart.asm.StackAnalyser.Cast;
import codechicken.multipart.asm.StackAnalyser.CaughtException;
import codechicken.multipart.asm.StackAnalyser.Const;
import codechicken.multipart.asm.StackAnalyser.Const$;
import codechicken.multipart.asm.StackAnalyser.GetField;
import codechicken.multipart.asm.StackAnalyser.Invoke;
import codechicken.multipart.asm.StackAnalyser.Load;
import codechicken.multipart.asm.StackAnalyser.LocalEntry;
import codechicken.multipart.asm.StackAnalyser.New;
import codechicken.multipart.asm.StackAnalyser.NewArray;
import codechicken.multipart.asm.StackAnalyser.NewMultiArray;
import codechicken.multipart.asm.StackAnalyser.Param;
import codechicken.multipart.asm.StackAnalyser.PrimitiveCast;
import codechicken.multipart.asm.StackAnalyser.ReturnAddress;
import codechicken.multipart.asm.StackAnalyser.StackEntry;
import codechicken.multipart.asm.StackAnalyser.Store;
import codechicken.multipart.asm.StackAnalyser.This;
import codechicken.multipart.asm.StackAnalyser.UnaryOp;
import scala.collection.mutable.ListBuffer;

class StackAnalyserCharacterizationTest {

    private static final Type OWNER = Type.getObjectType("test/Owner");

    @Test
    void initializesReceiverParametersAndWideLocalAliases() {
        StackAnalyser a = analyser(0, "(IJD[Ljava/lang/String;)V");
        assertEquals(7, a.locals().size());
        assertEquals(OWNER, ((This) a.locals().apply(0)).owner());
        assertEquals(0, ((Param) a.locals().apply(1)).i());
        assertEquals(1, ((Param) a.locals().apply(2)).i());
        assertSame(a.locals().apply(2), a.locals().apply(3));
        assertSame(a.locals().apply(4), a.locals().apply(5));
        assertEquals(Type.getType("[Ljava/lang/String;"), a.locals().apply(6).getType());
        assertTrue(a.stack().isEmpty());
        assertEquals(0, analyser(ACC_STATIC, "()V").locals().size());
        assertSame(OWNER, a.owner());
        assertEquals("(IJD[Ljava/lang/String;)V", a.m().desc);

        Param wide = new Param(8, Type.LONG_TYPE);
        a.setL(9, wide);
        assertNull(a.locals().apply(7));
        assertNull(a.locals().apply(8));
        assertSame(wide, a.locals().apply(9));
        assertSame(wide, a.locals().apply(10));
        Param narrow = new Param(9, Type.INT_TYPE);
        a.setL(9, narrow);
        assertSame(narrow, a.locals().apply(9));
        assertSame(wide, a.locals().apply(10)); // A narrow overwrite leaves the second wide slot intact.
    }

    @Test
    void measuresWidthsThroughBothFacadeAndCompanion() {
        ListBuffer<Type> types = new ListBuffer<>();
        for (Type t : new Type[] { Type.VOID_TYPE, Type.INT_TYPE, Type.LONG_TYPE, OWNER, Type.DOUBLE_TYPE }) {
            types.$plus$eq(t);
            assertEquals(t.getSize(), StackAnalyser.width(t));
            assertEquals(t.getSize(), StackAnalyser$.MODULE$.width(t.getDescriptor()));
        }
        assertEquals(6, StackAnalyser.width(types));
        assertEquals(6, StackAnalyser$.MODULE$.width(types));
        assertEquals(0, StackAnalyser.width(new ListBuffer<Type>()));
    }

    @Test
    void keepsSlotBasedStackMutationAndPartialFailures() {
        StackAnalyser a = empty();
        Const one = constant(1), wide = constant(2L);
        a.push(one);
        a.push(wide);
        assertSlots(a, one, wide, wide);
        assertSame(wide, a.pop(0));
        assertSame(one, a.peek(0));
        a.push(new New(Type.VOID_TYPE, null));
        assertSlots(a, one);
        a.insert(1, wide);
        assertSlots(a, wide, wide, one);
        assertSame(wide, a.pop(1));
        assertSame(one, a._pop(0));
        assertThrows(IndexOutOfBoundsException.class, () -> a.pop(0));

        a.stack().$plus$eq(one);
        a.stack().$plus$eq(wide);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> a.pop(0));
        assertEquals("Wide stack entry elems don't match (Const(2),Const(1)", error.getMessage());
        assertSlots(a, one); // The first slot was removed before validation failed.

        a.stack().clear();
        Const equalWide = new Const(2L, new InsnNode(NOP));
        a.stack().$plus$eq(equalWide);
        a.stack().$plus$eq(wide);
        assertSame(wide, a.pop(0)); // Pair validation uses Scala equality, not identity.
        assertTrue(a.stack().isEmpty());
    }

    @Test
    void freezesEveryDuplicationOpcodeIncludingWideSlotQuirks() {
        int[] ops = { POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP };
        String[] expected = { "123", "12", "12344", "12434", "14234", "123434", "134234", "341234", "1243" };
        for (int i = 0; i < ops.length; i++) {
            StackAnalyser a = empty();
            for (int n = 1; n <= 4; n++) a.push(constant(n));
            a.visitInsn(new InsnNode(ops[i]));
            assertEquals(expected[i], values(a), "opcode " + ops[i]);
        }
        StackAnalyser a = empty();
        Const wide = constant(7L);
        a.push(wide);
        a.visitInsn(new InsnNode(DUP2));
        assertSlots(a, wide, wide, wide, wide, wide, wide);
        a.visitInsn(new InsnNode(POP2));
        assertSlots(a, wide, wide, wide, wide);
    }

    @Test
    void recordsConstantValuesTypesAndInstructionIdentity() {
        Object[] values = { null, -1, 0, 1, 2, 3, 4, 5, 0L, 1L, 0f, 1f, 2f, 0d, 1d };
        for (int i = 0; i < values.length; i++) {
            StackAnalyser a = empty();
            InsnNode insn = new InsnNode(ACONST_NULL + i);
            a.visitInsn(insn);
            Const value = (Const) a.pop(0);
            assertEquals(values[i], value.c());
            assertSame(insn, value.insn());
            assertTrue(a.stack().isEmpty());
        }
        StackAnalyser a = empty();
        a.visitInsn(new IntInsnNode(BIPUSH, 255));
        assertEquals((byte) -1, ((Const) a.peek(0)).c());
        assertEquals(Type.BYTE_TYPE, a.pop(0).getType());
        a.visitInsn(new IntInsnNode(SIPUSH, 65535));
        assertEquals((short) -1, ((Const) a.peek(0)).c());
        assertEquals(Type.SHORT_TYPE, a.pop(0).getType());
        LdcInsnNode ldc = new LdcInsnNode("text");
        a.visitInsn(ldc);
        assertSame(ldc, a.peek(0).insn());
        assertEquals(Type.getObjectType("java/lang/String"), a.pop(0).getType());
        Object[] extra = { (byte) 1, (short) 2, 'c', true };
        Type[] types = { Type.BYTE_TYPE, Type.SHORT_TYPE, Type.CHAR_TYPE, Type.BOOLEAN_TYPE };
        for (int i = 0; i < extra.length; i++) assertEquals(types[i], constant(extra[i]).getType());
        assertThrows(IllegalArgumentException.class, () -> a.visitInsn(new LdcInsnNode(OWNER)));
        assertTrue(a.stack().isEmpty());
    }

    @Test
    void retainsCaseClassEqualityCopyProductsAndImplicitInstructionBinding() {
        InsnNode first = new InsnNode(NOP), second = new InsnNode(RETURN);
        Const c = new Const(12, first);
        Const copy = c.copy(c.copy$default$1(), second);
        assertEquals(c, copy);
        assertEquals(c.hashCode(), copy.hashCode());
        assertSame(first, c.insn());
        assertSame(second, copy.insn());
        assertEquals("Const(12)", copy.toString());
        assertEquals(1, copy.productArity());
        assertEquals(12, copy.productElement(0));
        assertThrows(IndexOutOfBoundsException.class, () -> copy.productElement(1));
        assertEquals(12, Const$.MODULE$.unapply(copy).get());
        assertFalse(Const$.MODULE$.unapply(null).isDefined());
        assertEquals(c, Const$.MODULE$.apply(12, second));
        assertEquals(new ReturnAddress(first), new ReturnAddress(second));
        assertEquals(Type.INT_TYPE, new ReturnAddress(first).getType());
    }

    @Test
    void loadsStoresAndIncrementsPreserveExpressionTrees() {
        StackAnalyser a = analyser(0, "(J)V");
        LocalEntry receiver = a.locals().apply(0);
        VarInsnNode load = new VarInsnNode(ALOAD, 0);
        a.visitInsn(load);
        assertSame(receiver, ((Load) a.peek(0)).e());
        assertSame(load, a.peek(0).insn());
        VarInsnNode store = new VarInsnNode(ASTORE, 5);
        StackEntry loaded = a.peek(0);
        a.visitInsn(store);
        Store stored = (Store) a.locals().apply(5);
        assertSame(loaded, stored.e());
        assertSame(store, stored.insn());
        assertEquals(OWNER, stored.getType());
        a.setL(3, new Param(2, Type.INT_TYPE));
        LocalEntry before = a.locals().apply(3);
        IincInsnNode inc = new IincInsnNode(3, -4);
        a.visitInsn(inc);
        Store incremented = (Store) a.locals().apply(3);
        BinaryOp sum = (BinaryOp) incremented.e();
        assertEquals(IINC, sum.op());
        assertEquals(-4, ((Const) sum.e2()).c());
        assertSame(before, ((Load) sum.e1()).e());
        for (StackEntry e : new StackEntry[] { sum, sum.e1(), sum.e2() }) assertSame(inc, e.insn());
        assertSame(inc, incremented.insn());
        assertTrue(a.stack().isEmpty());
    }

    @Test
    void arithmeticRetainsOperandOrderAndCurrentResultTypes() {
        for (int op = IADD; op <= DCMPG; op++) {
            if (op == IINC) continue; // IINC has its own instruction node, covered by the local-store test.
            StackAnalyser a = empty();
            Const left = constant(3), right = constant(5);
            a.push(left);
            InsnNode insn = new InsnNode(op);
            if (op <= DREM || op >= ISHL && op <= LXOR || op >= LCMP) {
                a.push(right);
                a.visitInsn(insn);
                BinaryOp result = (BinaryOp) a.peek(0);
                assertSame(left, result.e1());
                assertSame(right, result.e2());
                assertEquals(op, result.op());
                assertEquals(Type.INT_TYPE, result.getType());
            } else if (op <= DNEG) {
                a.visitInsn(insn);
                UnaryOp result = (UnaryOp) a.peek(0);
                assertSame(left, result.e());
                assertEquals(op, result.op());
            } else {
                a.visitInsn(insn);
                PrimitiveCast result = (PrimitiveCast) a.peek(0);
                assertSame(left, result.e());
                Type expected = Type.DOUBLE_TYPE;
                if (op == I2L || op == F2L || op == D2L) expected = Type.LONG_TYPE;
                if (op == I2F || op == L2F || op == D2F) expected = Type.FLOAT_TYPE;
                if (op == I2B) expected = Type.BYTE_TYPE;
                if (op == I2C) expected = Type.CHAR_TYPE;
                if (op == I2S) expected = Type.SHORT_TYPE;
                assertEquals(expected, result.getType(), "opcode " + op);
                assertEquals(expected.getSize(), a.stack().size());
            }
            assertSame(insn, a.peek(0).insn());
        }
        StackAnalyser a = empty();
        a.push(constant(1L));
        a.push(constant(2L));
        a.visitInsn(new InsnNode(LCMP));
        assertEquals(Type.LONG_TYPE, a.peek(0).getType());
        assertEquals(2, a.stack().size());
    }

    @Test
    void arrayOperationsKeepExistingTypesAndDimensionOrder() {
        StackAnalyser a = empty();
        StackEntry array = new New(Type.getType("[I"), null), index = constant(2), value = constant(9);
        for (int op = IALOAD; op <= SALOAD; op++) {
            a.push(array);
            a.push(index);
            a.visitInsn(new InsnNode(op));
            ArrayLoad result = (ArrayLoad) a.pop(0);
            assertSame(array, result.e());
            assertSame(index, result.index());
            assertEquals(Type.INT_TYPE, result.getType());
        }
        for (int op = IASTORE; op <= SASTORE; op++) {
            a.push(array);
            a.push(index);
            a.push(value);
            a.visitInsn(new InsnNode(op));
            assertTrue(a.stack().isEmpty());
        }
        a.push(array);
        a.visitInsn(new InsnNode(ARRAYLENGTH));
        assertSame(array, ((ArrayLength) a.pop(0)).array());
        a.push(index);
        a.visitInsn(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        NewArray created = (NewArray) a.pop(0);
        assertSame(index, created.len());
        assertEquals("[java/lang/String", created.getType().getDescriptor());
        a.push(index);
        a.push(value);
        MultiANewArrayInsnNode multi = new MultiANewArrayInsnNode("[[I", 2);
        a.visitInsn(multi);
        NewMultiArray result = (NewMultiArray) a.pop(0);
        assertArrayEquals(new StackEntry[] { value, index }, result.sizes());
        assertSame(multi, result.insn());
        assertEquals(Type.getType("[[I"), result.getType());
    }

    @Test
    void objectAndFieldOperationsPreserveReceiversAndProvenance() {
        StackAnalyser a = empty();
        a.visitInsn(new TypeInsnNode(NEW, "test/Owner"));
        New obj = (New) a.pop(0);
        assertEquals(OWNER, obj.getType());
        a.push(obj);
        a.visitInsn(new TypeInsnNode(CHECKCAST, "test/Other"));
        Cast cast = (Cast) a.pop(0);
        assertSame(obj, cast.obj());
        assertEquals(Type.getObjectType("test/Other"), cast.getType());
        a.push(obj);
        a.visitInsn(new TypeInsnNode(INSTANCEOF, "test/Other"));
        UnaryOp check = (UnaryOp) a.pop(0);
        assertSame(obj, check.e());
        assertEquals(OWNER, check.getType()); // Existing analysis reports the input type.
        for (int op : new int[] { GETSTATIC, GETFIELD }) {
            if (op == GETFIELD) a.push(obj);
            FieldInsnNode field = new FieldInsnNode(op, "test/Owner", "value", "J");
            a.visitInsn(field);
            GetField result = (GetField) a.pop(0);
            assertSame(op == GETFIELD ? obj : null, result.obj());
            assertSame(field, result.field());
            assertSame(field, result.insn());
            assertEquals(Type.LONG_TYPE, result.getType());
        }
        for (int op : new int[] { PUTSTATIC, PUTFIELD }) {
            if (op == PUTFIELD) a.push(obj);
            a.push(constant(3L));
            a.visitInsn(new FieldInsnNode(op, "test/Owner", "value", "J"));
            assertTrue(a.stack().isEmpty());
        }
    }

    @Test
    void callsPopArgumentsInDeclarationOrderAndDiscardVoidResults() {
        for (int op : new int[] { INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE, INVOKESTATIC }) {
            StackAnalyser a = empty();
            Const obj = constant("receiver"), first = constant(2), last = constant(3L);
            if (op != INVOKESTATIC) a.push(obj);
            a.push(first);
            a.push(last);
            MethodInsnNode call = new MethodInsnNode(op, "test/Owner", "call", "(IJ)D", op == INVOKEINTERFACE);
            a.visitInsn(call);
            Invoke result = (Invoke) a.pop(0);
            assertArrayEquals(new StackEntry[] { first, last }, result.params());
            assertSame(op == INVOKESTATIC ? null : obj, result.obj());
            assertSame(call, result.method());
            assertSame(call, result.insn());
            assertEquals(op, result.op());
            assertEquals(Type.DOUBLE_TYPE, result.getType());
            assertTrue(a.stack().isEmpty());
            a.visitInsn(new MethodInsnNode(INVOKESTATIC, "test/Owner", "nothing", "()V", false));
            assertTrue(a.stack().isEmpty());
        }
        StackAnalyser a = empty();
        Const first = constant(2), last = constant(3L);
        a.push(first);
        a.push(last);
        assertArrayEquals(new StackEntry[] { first, last }, a.popArgs("(IJ)V"));
    }

    @Test
    void branchesReturnsAndSwitchesConsumeOnlyTheirCurrentOperands() {
        for (int op = IFEQ; op <= IF_ACMPNE; op++) {
            StackAnalyser a = empty();
            Const bottom = constant(1);
            a.push(bottom);
            a.push(constant(2));
            if (op >= IF_ICMPEQ) a.push(constant(3));
            a.visitInsn(new JumpInsnNode(op, new LabelNode()));
            assertSlots(a, bottom);
        }
        for (int op : new int[] { IFNULL, IFNONNULL, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, ATHROW, MONITORENTER,
                MONITOREXIT }) {
            StackAnalyser a = empty();
            a.push(constant(4L));
            a.visitInsn(op == IFNULL || op == IFNONNULL ? new JumpInsnNode(op, new LabelNode()) : new InsnNode(op));
            assertTrue(a.stack().isEmpty());
        }
        StackAnalyser a = empty();
        a.push(constant(2));
        a.visitInsn(new TableSwitchInsnNode(0, 0, new LabelNode(), new LabelNode()));
        a.push(constant(3));
        a.visitInsn(new LookupSwitchInsnNode(new LabelNode(), new int[] { 1 }, new LabelNode[] { new LabelNode() }));
        assertTrue(a.stack().isEmpty());
        JumpInsnNode jsr = new JumpInsnNode(JSR, new LabelNode());
        a.visitInsn(jsr);
        assertSame(jsr, ((ReturnAddress) a.peek(0)).insn());
        a.visitInsn(new JumpInsnNode(GOTO, new LabelNode()));
        a.visitInsn(new InsnNode(RETURN));
        assertEquals(1, a.stack().size());
    }

    @Test
    void handlerLabelsUseLastRegistrationAndDoNotResetExistingStack() {
        MethodNode m = method(ACC_STATIC, "()V");
        LabelNode handler = new LabelNode();
        TryCatchBlockNode first = new TryCatchBlockNode(
                new LabelNode(),
                new LabelNode(),
                handler,
                "Ljava/lang/Exception;");
        TryCatchBlockNode last = new TryCatchBlockNode(
                new LabelNode(),
                new LabelNode(),
                handler,
                "Ljava/lang/Throwable;");
        m.tryCatchBlocks.add(first);
        m.tryCatchBlocks.add(last);
        StackAnalyser a = new StackAnalyser(OWNER, m);
        assertSame(last, a.codechicken$multipart$asm$StackAnalyser$$catchHandlers().apply(handler));
        Const bottom = constant(4);
        a.push(bottom);
        a.visitInsn(new LabelNode());
        assertSlots(a, bottom);
        a.visitInsn(handler);
        CaughtException caught = (CaughtException) a.pop(0);
        assertEquals(Type.getObjectType("java/lang/Throwable"), caught.getType());
        assertSame(handler, caught.insn());
        assertSlots(a, bottom);
        last.type = null;
        assertThrows(NullPointerException.class, () -> a.visitInsn(handler));
        assertSlots(a, bottom);
    }

    @Test
    void unsupportedOpcodesRetainFailureAndNoOpBoundaries() {
        StackAnalyser a = empty();
        Const count = constant(5);
        a.push(count);
        scala.MatchError error = assertThrows(
                scala.MatchError.class,
                () -> a.visitInsn(new IntInsnNode(NEWARRAY, T_INT)));
        assertEquals("188 (of class java.lang.Integer)", error.getMessage());
        assertSlots(a, count);
        assertThrows(scala.MatchError.class, () -> a.visitInsn(new VarInsnNode(RET, 0)));
        for (AbstractInsnNode ignored : new AbstractInsnNode[] { new InsnNode(NOP), new InsnNode(255),
                new FrameNode(F_SAME, 0, null, 0, null), new LineNumberNode(12, new LabelNode()),
                new InvokeDynamicInsnNode("call", "()V", null), null }) {
            a.visitInsn(ignored);
            assertSlots(a, count);
        }
        // The unreachable TypeInsnNode spelling of NEWARRAY is nevertheless part of current dispatch.
        a.visitInsn(new TypeInsnNode(NEWARRAY, "[I"));
        NewArray result = (NewArray) a.pop(0);
        assertSame(count, result.len());
        assertEquals(Type.getType("[I"), result.getType());
    }

    @Test
    void internalCallsStillDispatchThroughOverridesAndDefaultArgumentMethods() {
        List<String> calls = new ArrayList<>();
        StackAnalyser a = new StackAnalyser(OWNER, method(ACC_STATIC, "()V")) {

            @Override
            public void insert(int i, StackEntry e) {
                calls.add("insert:" + i);
                super.insert(i, e);
            }

            @Override
            public int pop$default$1() {
                calls.add("default");
                return 1;
            }

            @Override
            public StackEntry pop(int i) {
                calls.add("pop:" + i);
                return super.pop(i);
            }

            @Override
            public StackEntry _pop(int i) {
                calls.add("raw:" + i);
                return super._pop(i);
            }

            @Override
            public void setL(int i, LocalEntry e) {
                calls.add("local:" + i);
                super.setL(i, e);
            }
        };
        Const one = constant(1), two = constant(2);
        a.push(one);
        a.push(two);
        a.visitInsn(new VarInsnNode(ISTORE, 0));
        assertSame(one, ((Store) a.locals().apply(0)).e());
        assertSlots(a, two);
        assertEquals(Arrays.asList("insert:0", "insert:0", "default", "pop:1", "raw:1", "local:0"), calls);
        assertEquals(0, a._pop$default$1());
        assertEquals(0, a.peek$default$1());
    }

    private static StackAnalyser empty() {
        return analyser(ACC_STATIC, "()V");
    }

    private static StackAnalyser analyser(int access, String desc) {
        return new StackAnalyser(OWNER, method(access, desc));
    }

    private static MethodNode method(int access, String desc) {
        return new MethodNode(ASM5, access, "test", desc, null, null);
    }

    private static Const constant(Object value) {
        return new Const(value, null);
    }

    private static void assertSlots(StackAnalyser a, StackEntry... expected) {
        assertEquals(expected.length, a.stack().size());
        for (int i = 0; i < expected.length; i++) assertSame(expected[i], a.stack().apply(i), "slot " + i);
    }

    private static String values(StackAnalyser a) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < a.stack().size(); i++) result.append(((Const) a.stack().apply(i)).c());
        return result.toString();
    }
}
