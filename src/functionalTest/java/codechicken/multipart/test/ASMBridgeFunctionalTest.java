package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.Tuple2;

class ASMBridgeFunctionalTest {

    @Test
    void splitsAtTheFirstParenthesisWithoutValidatingTheDescriptor() {
        assertEquals(
                new Tuple2<>("method", "(JD)Ljava/lang/String;"),
                ASMMixinCompiler.seperateDesc("method(JD)Ljava/lang/String;"));
        assertEquals(new Tuple2<>("", "()V"), ASMMixinCompiler$.MODULE$.seperateDesc("()V"));
        assertEquals(
                new Tuple2<>("odd.name", "(not(a descriptor"),
                ASMMixinCompiler.seperateDesc("odd.name(not(a descriptor"));
        assertThrows(StringIndexOutOfBoundsException.class, () -> ASMMixinCompiler.seperateDesc("missing"));
        assertThrows(StringIndexOutOfBoundsException.class, () -> ASMMixinCompiler.seperateDesc(""));
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.seperateDesc(null));
    }

    @Test
    void prependsAnObjectReceiverAndPreservesAllArgumentAndReturnTypes() {
        for (String result : new String[] { "V", "Z", "B", "C", "S", "I", "F", "J", "D", "Ljava/lang/Object;",
                "[[I" }) {
            assertEquals("(Ltest/Owner;)" + result, ASMMixinCompiler.staticDesc("test/Owner", "()" + result));
            assertEquals(
                    "(Ltest/Owner;ZBCSIFJD[ILjava/lang/String;[[D)" + result,
                    ASMMixinCompiler$.MODULE$.staticDesc("test/Owner", "(ZBCSIFJD[ILjava/lang/String;[[D)" + result));
        }
        assertEquals("(Ltest.Owner;)V", ASMMixinCompiler.staticDesc("test.Owner", "()V"));
        assertEquals("(Lnull;)V", ASMMixinCompiler.staticDesc(null, "()V"));
        assertEquals("(L;)V", ASMMixinCompiler.staticDesc("", "()V"));
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.staticDesc("test/Owner", null));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ASMMixinCompiler.staticDesc("test/Owner", "()"));
    }

    @Test
    void loadsMixedArgumentsInSlotOrderAndUsesTheBridgeDescriptorForItsReturnAndMaxima() {
        Trace trace = new Trace();
        ASMMixinCompiler.writeBridge(
                trace,
                "(ZBCSIFJD[ILjava/lang/String;[[D)D",
                INVOKESTATIC,
                "test/Helper",
                "target",
                "(Ljava/lang/Object;)V");
        assertEquals(
                Arrays.asList(
                        "var " + ALOAD + " 0",
                        "var " + ILOAD + " 1",
                        "var " + ILOAD + " 2",
                        "var " + ILOAD + " 3",
                        "var " + ILOAD + " 4",
                        "var " + ILOAD + " 5",
                        "var " + FLOAD + " 6",
                        "var " + LLOAD + " 7",
                        "var " + DLOAD + " 9",
                        "var " + ALOAD + " 11",
                        "var " + ALOAD + " 12",
                        "var " + ALOAD + " 13",
                        "call " + INVOKESTATIC + " test/Helper target (Ljava/lang/Object;)V false",
                        "insn " + DRETURN,
                        "max 14 14"),
                trace.events);
    }

    @Test
    void handlesEveryReturnCategoryIncludingWideResultsWithoutArguments() {
        String[] returns = { "V", "Z", "B", "C", "S", "I", "F", "J", "D", "Ljava/lang/Object;", "[J" };
        int[] opcodes = { RETURN, IRETURN, IRETURN, IRETURN, IRETURN, IRETURN, FRETURN, LRETURN, DRETURN, ARETURN,
                ARETURN };
        int[] stacks = { 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1 };
        for (int i = 0; i < returns.length; i++) {
            Trace trace = new Trace();
            String desc = "()" + returns[i];
            ASMMixinCompiler.finishBridgeCall(trace, desc, INVOKEVIRTUAL, "test/Owner", "value", desc);
            assertEquals(
                    Arrays.asList(
                            "call " + INVOKEVIRTUAL + " test/Owner value " + desc + " false",
                            "insn " + opcodes[i],
                            "max " + stacks[i] + " 1"),
                    trace.events,
                    desc);
        }
    }

    @Test
    void derivesTheInterfaceFlagOnlyFromTheInvocationOpcodeAndLeavesTheReceiverToTheCaller() {
        for (int opcode : new int[] { INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE }) {
            Trace trace = new Trace();
            trace.visitVarInsn(ALOAD, 0);
            trace.visitFieldInsn(GETFIELD, "test/Wrapper", "delegate", "Ltest/Contract;");
            ASMMixinCompiler$.MODULE$.finishBridgeCall(trace, "(J)I", opcode, "test/Contract", "call", "(J)I");
            assertEquals(
                    Arrays.asList(
                            "var " + ALOAD + " 0",
                            "field " + GETFIELD + " test/Wrapper delegate Ltest/Contract;",
                            "var " + LLOAD + " 1",
                            "call " + opcode + " test/Contract call (J)I " + (opcode == INVOKEINTERFACE),
                            "insn " + IRETURN,
                            "max 3 3"),
                    trace.events);
        }
    }

    @Test
    void preservesPartialVisitorOutputAndPropagatesTheOriginalFailure() {
        Trace completed = new Trace();
        ASMMixinCompiler.writeBridge(completed, "(JD)I", INVOKEVIRTUAL, "test/Owner", "call", "(JD)I");
        for (int event = 1; event <= completed.events.size(); event++) {
            Trace failing = new Trace();
            failing.failAt = event;
            assertSame(
                    failing.failure,
                    assertThrows(
                            IllegalStateException.class,
                            () -> ASMMixinCompiler
                                    .writeBridge(failing, "(JD)I", INVOKEVIRTUAL, "test/Owner", "call", "(JD)I")));
            assertEquals(completed.events.subList(0, event), failing.events);
        }
        Trace finish = new Trace();
        assertThrows(
                NullPointerException.class,
                () -> ASMMixinCompiler.finishBridgeCall(finish, null, INVOKEVIRTUAL, "owner", "name", "()V"));
        assertTrue(finish.events.isEmpty());
        Trace bridge = new Trace();
        assertThrows(
                NullPointerException.class,
                () -> ASMMixinCompiler.writeBridge(bridge, null, INVOKEVIRTUAL, "owner", "name", "()V"));
        assertEquals(Arrays.asList("var " + ALOAD + " 0"), bridge.events);
    }

    @Test
    void staticBridgeReadsOverridableMixinMetadataAndDescriptorsInTheOriginalOrder() {
        MethodNode method = new MethodNode(ACC_PUBLIC, "bridge", "(I)I", null, null);
        List<String> reads = new ArrayList<>();
        MixinInfo mixin = new MixinInfo("unused", null, null, null, null, null) {

            @Override
            public String tname() {
                reads.add("helper:" + method.desc);
                method.desc = "(J)J";
                return "test/Helper";
            }

            @Override
            public String name() {
                reads.add("receiver:" + method.desc);
                method.desc = "(D)D";
                return "test/Receiver";
            }
        };
        ASMMixinCompiler.writeStaticBridge(method, "target", mixin);
        Trace trace = new Trace();
        method.accept(trace);
        assertEquals(Arrays.asList("helper:(I)I", "receiver:(J)J"), reads);
        assertEquals(
                Arrays.asList(
                        "var " + ALOAD + " 0",
                        "var " + ILOAD + " 1",
                        "call " + INVOKESTATIC + " test/Helper target (Ltest/Receiver;D)D false",
                        "insn " + IRETURN,
                        "max 2 2"),
                trace.events);
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.writeStaticBridge(method, "target", null));
    }

    @Test
    void emittedBridgesExecuteVirtualSpecialInterfaceAndScalaStyleStaticDispatch() throws Exception {
        String parent = Type.getInternalName(Receiver.class);
        String owner = "codechicken/multipart/test/bridge/ExecutableBridge";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_6, ACC_PUBLIC, owner, null, parent, null);
        MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ASMMixinCompiler.writeBridge(constructor, "()V", INVOKESPECIAL, parent, "<init>", "()V");
        constructor.visitEnd();
        MethodVisitor value = writer.visitMethod(ACC_PUBLIC, "value", "()I", null, null);
        value.visitIntInsn(BIPUSH, 99);
        value.visitInsn(IRETURN);
        value.visitMaxs(1, 1);
        value.visitEnd();
        addBridge(writer, "viaVirtual", "()I", INVOKEVIRTUAL, parent, "value");
        addBridge(writer, "viaSpecial", "()I", INVOKESPECIAL, parent, "value");
        String desc = "(JDLjava/lang/String;[I)Ljava/lang/String;";
        addBridge(writer, "viaInterface", desc, INVOKEINTERFACE, Type.getInternalName(Contract.class), "combine");
        MethodNode staticBridge = new MethodNode(ACC_PUBLIC, "viaStatic", desc, null, null);
        ASMMixinCompiler
                .writeStaticBridge(staticBridge, "combine", new MixinInfo(parent, null, null, null, null, null));
        staticBridge.accept(writer);
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        Class<?> generated = new ClassLoader(getClass().getClassLoader()) {

            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
        Object receiver = generated.getConstructor().newInstance();
        assertEquals(99, generated.getMethod("viaVirtual").invoke(receiver));
        assertEquals(17, generated.getMethod("viaSpecial").invoke(receiver));
        Class<?>[] parameters = { long.class, double.class, String.class, int[].class };
        assertEquals(
                "instance:99:1234567890123:2.5:abc:7",
                generated.getMethod("viaInterface", parameters)
                        .invoke(receiver, 1234567890123L, 2.5, "abc", new int[] { 7 }));
        assertEquals(
                "static:99:1234567890123:2.5:abc:7",
                generated.getMethod("viaStatic", parameters)
                        .invoke(receiver, 1234567890123L, 2.5, "abc", new int[] { 7 }));
    }

    private static void addBridge(ClassWriter writer, String name, String desc, int opcode, String owner,
            String target) {
        MethodVisitor method = writer.visitMethod(ACC_PUBLIC, name, desc, null, null);
        ASMMixinCompiler.writeBridge(method, desc, opcode, owner, target, desc);
        method.visitEnd();
    }

    private static class Trace extends MethodVisitor {

        final List<String> events = new ArrayList<>();
        final IllegalStateException failure = new IllegalStateException("visitor failure");
        int failAt;

        Trace() {
            super(ASM5);
        }

        private void record(String event) {
            events.add(event);
            if (events.size() == failAt) throw failure;
        }

        @Override
        public void visitVarInsn(int opcode, int local) {
            record("var " + opcode + " " + local);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            record("field " + opcode + " " + owner + " " + name + " " + desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            record("call " + opcode + " " + owner + " " + name + " " + desc + " " + itf);
        }

        @Override
        public void visitInsn(int opcode) {
            record("insn " + opcode);
        }

        @Override
        public void visitMaxs(int stack, int locals) {
            record("max " + stack + " " + locals);
        }
    }

    public interface Contract {

        String combine(long wide, double number, String text, int[] array);
    }

    public static class Receiver implements Contract {

        public int value() {
            return 17;
        }

        @Override
        public String combine(long wide, double number, String text, int[] array) {
            return "instance:" + value() + ":" + wide + ":" + number + ":" + text + ":" + array[0];
        }
    }

    public static class Receiver$class {

        public static String combine(Receiver receiver, long wide, double number, String text, int[] array) {
            return "static:" + receiver.value() + ":" + wide + ":" + number + ":" + text + ":" + array[0];
        }
    }
}
