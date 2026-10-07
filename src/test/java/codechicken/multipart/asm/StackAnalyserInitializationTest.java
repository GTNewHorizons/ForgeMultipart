package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import codechicken.multipart.asm.StackAnalyser.LocalEntry;
import codechicken.multipart.asm.StackAnalyser.Param;
import codechicken.multipart.asm.StackAnalyser.This;
import scala.collection.JavaConversions;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

class StackAnalyserInitializationTest {

    private static final Type OWNER = Type.getObjectType("test/Owner");

    @Test
    void usesConstructorArgumentsAndVirtualPushesWithDescriptorReadBetweenReceiverAndParameters() {
        MethodNode method = method(0, "()V");
        TryCatchBlockNode handler = handler(new LabelNode());
        List<String> calls = new ArrayList<>();
        StackAnalyser analyser = new StackAnalyser(OWNER, method) {

            @Override
            public Type owner() {
                throw new AssertionError("Constructor must use its owner argument");
            }

            @Override
            public MethodNode m() {
                throw new AssertionError("Constructor must use its method argument");
            }

            @Override
            public void pushL(LocalEntry entry) {
                assertTrue(stack().isEmpty());
                assertTrue(codechicken$multipart$asm$StackAnalyser$$catchHandlers().isEmpty());
                calls.add("push:" + entry.getType().getDescriptor());
                if (entry instanceof This) {
                    assertSame(OWNER, ((This) entry).owner());
                    method.desc = "(JD[Ljava/lang/String;)V";
                } else {
                    // Parsing is complete before the first parameter callback. Handlers are read afterwards.
                    method.desc = null;
                    method.tryCatchBlocks = Arrays.asList(handler);
                }
                super.pushL(entry);
            }

            @Override
            public void setL(int slot, LocalEntry entry) {
                calls.add("set:" + slot);
                super.setL(slot, entry);
            }
        };
        assertEquals(
                Arrays.asList(
                        "push:Ltest/Owner;",
                        "set:0",
                        "push:J",
                        "set:1",
                        "push:D",
                        "set:3",
                        "push:[Ljava/lang/String;",
                        "set:5"),
                calls);
        assertEquals(6, analyser.locals().size());
        assertSame(analyser.locals().apply(1), analyser.locals().apply(2));
        assertSame(analyser.locals().apply(3), analyser.locals().apply(4));
        assertSame(handler, analyser.codechicken$multipart$asm$StackAnalyser$$catchHandlers().apply(handler.handler));
    }

    @Test
    void staticParametersUseFormalIndicesAndWideSlotsWithoutAReceiver() {
        StackAnalyser analyser = new StackAnalyser(null, method(ACC_STATIC, "(JDI[[J)V"));
        assertEquals(6, analyser.locals().size());
        int[] slots = { 0, 2, 4, 5 };
        Type[] types = { Type.LONG_TYPE, Type.DOUBLE_TYPE, Type.INT_TYPE, Type.getType("[[J") };
        for (int i = 0; i < slots.length; i++) {
            Param parameter = (Param) analyser.locals().apply(slots[i]);
            assertEquals(i, parameter.i());
            assertEquals(types[i], parameter.t());
        }
        assertSame(analyser.locals().apply(0), analyser.locals().apply(1));
        assertSame(analyser.locals().apply(2), analyser.locals().apply(3));
        assertNull(analyser.owner());
    }

    @Test
    void malformedDescriptorsFailAfterTheReceiverButBeforeAnyParameterOrHandler() {
        for (String descriptor : new String[] { null, "(I", "(ILjava/lang/String;" }) {
            for (int access : new int[] { 0, ACC_STATIC }) {
                MethodNode method = method(access, descriptor);
                method.tryCatchBlocks = null;
                List<StackAnalyser> receivers = new ArrayList<>();
                RuntimeException failure = assertThrows(RuntimeException.class, () -> new StackAnalyser(OWNER, method) {

                    @Override
                    public void pushL(LocalEntry entry) {
                        assertInstanceOf(This.class, entry);
                        receivers.add(this);
                        super.pushL(entry);
                    }
                });
                assertEquals(
                        descriptor == null ? NullPointerException.class : ArrayIndexOutOfBoundsException.class,
                        failure.getClass());
                assertEquals(access == 0 ? 1 : 0, receivers.size());
                if (access == 0) {
                    assertEquals(1, receivers.get(0).locals().size());
                    assertTrue(receivers.get(0).codechicken$multipart$asm$StackAnalyser$$catchHandlers().isEmpty());
                }
            }
        }
    }

    @Test
    void receiverCallbackFailurePrecedesDescriptorParsing() {
        RuntimeException sentinel = new RuntimeException("receiver");
        List<StackAnalyser> receivers = new ArrayList<>();
        assertSame(sentinel, assertThrows(RuntimeException.class, () -> new StackAnalyser(OWNER, method(0, null)) {

            @Override
            public void pushL(LocalEntry entry) {
                receivers.add(this);
                super.pushL(entry);
                throw sentinel;
            }
        }));
        assertEquals(1, receivers.size());
        assertEquals(1, receivers.get(0).locals().size());
        assertTrue(receivers.get(0).stack().isEmpty());
        assertTrue(receivers.get(0).codechicken$multipart$asm$StackAnalyser$$catchHandlers().isEmpty());
    }

    @Test
    void parameterCallbackFailureKeepsPublishedWideSlotsAndDefersHandlers() {
        MethodNode method = method(ACC_STATIC, "(IJD)V");
        method.tryCatchBlocks.add(handler(new LabelNode()));
        RuntimeException sentinel = new RuntimeException("parameter");
        List<StackAnalyser> captured = new ArrayList<>();
        assertSame(sentinel, assertThrows(RuntimeException.class, () -> new StackAnalyser(OWNER, method) {

            @Override
            public void pushL(LocalEntry entry) {
                super.pushL(entry);
                if (((Param) entry).i() == 1) {
                    captured.add(this);
                    throw sentinel;
                }
            }
        }));
        StackAnalyser analyser = captured.get(0);
        assertEquals(3, analyser.locals().size());
        assertSame(analyser.locals().apply(1), analyser.locals().apply(2));
        assertTrue(analyser.codechicken$multipart$asm$StackAnalyser$$catchHandlers().isEmpty());
    }

    @Test
    void handlersUseLastDuplicateIncludingNullKeysAndRetainTheOriginalNodes() {
        LabelNode label = new LabelNode();
        TryCatchBlockNode first = handler(label), last = handler(label);
        TryCatchBlockNode nullFirst = handler(null), nullLast = handler(null);
        MethodNode method = method(ACC_STATIC, "()V");
        method.tryCatchBlocks = new ArrayList<>(Arrays.asList(first, nullFirst, last, nullLast));
        StackAnalyser analyser = new StackAnalyser(OWNER, method);
        Map<LabelNode, TryCatchBlockNode> handlers = analyser.codechicken$multipart$asm$StackAnalyser$$catchHandlers();
        assertEquals(2, handlers.size());
        assertSame(last, handlers.apply(label));
        assertSame(nullLast, handlers.apply(null));
        last.handler = new LabelNode();
        last.type = "changed";
        method.tryCatchBlocks.clear();
        assertSame(last, handlers.apply(label));
        assertEquals("changed", handlers.apply(label).type);
        assertFalse(handlers.contains(last.handler));
    }

    @Test
    void eachHandlerReadsTheVirtualMapBeforeItsKeyAndFailureKeepsEarlierEntries() {
        TryCatchBlockNode first = handler(new LabelNode()), second = handler(new LabelNode());
        MethodNode method = method(ACC_STATIC, "()V");
        method.tryCatchBlocks = Arrays.asList(first, second, null);
        Map<LabelNode, TryCatchBlockNode> firstMap = new HashMap<>(), secondMap = new HashMap<>();
        List<Integer> reads = new ArrayList<>();
        assertThrows(NullPointerException.class, () -> new StackAnalyser(OWNER, method) {

            @Override
            public Map<LabelNode, TryCatchBlockNode> codechicken$multipart$asm$StackAnalyser$$catchHandlers() {
                reads.add(reads.size());
                return reads.size() == 1 ? firstMap : secondMap;
            }
        });
        assertEquals(Arrays.asList(0, 1, 2), reads);
        assertEquals(1, firstMap.size());
        assertEquals(1, secondMap.size());
        assertSame(first, firstMap.apply(first.handler));
        assertSame(second, secondMap.apply(second.handler));
    }

    @Test
    void nullHandlerListFailsAfterAllLocalsArePublished() {
        MethodNode method = method(0, "(J)V");
        method.tryCatchBlocks = null;
        List<StackAnalyser> captured = new ArrayList<>();
        assertThrows(NullPointerException.class, () -> new StackAnalyser(OWNER, method) {

            @Override
            public void pushL(LocalEntry entry) {
                captured.add(this);
                super.pushL(entry);
            }
        });
        assertEquals(2, captured.size());
        assertSame(captured.get(0), captured.get(1));
        assertEquals(3, captured.get(0).locals().size());
        assertTrue(captured.get(0).codechicken$multipart$asm$StackAnalyser$$catchHandlers().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void scalaBackedHandlerListsRetainTheirForeachDispatch() {
        List<String> calls = new ArrayList<>();
        ArrayBuffer<TryCatchBlockNode> entries = new ArrayBuffer<>();
        Buffer<TryCatchBlockNode> buffer = (Buffer<TryCatchBlockNode>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Buffer.class },
                (proxy, called, arguments) -> {
                    if (called.getName().equals("foreach")) calls.add("foreach");
                    return called.invoke(entries, arguments);
                });
        TryCatchBlockNode handler = handler(new LabelNode());
        entries.$plus$eq(handler);
        MethodNode method = method(ACC_STATIC, "()V");
        method.tryCatchBlocks = JavaConversions.bufferAsJavaList(buffer);
        StackAnalyser analyser = new StackAnalyser(OWNER, method);
        assertEquals(Arrays.asList("foreach"), calls);
        assertSame(handler, analyser.codechicken$multipart$asm$StackAnalyser$$catchHandlers().apply(handler.handler));
    }

    private static MethodNode method(int access, String descriptor) {
        return new MethodNode(ASM5, access, "test", descriptor, null, null);
    }

    private static TryCatchBlockNode handler(LabelNode label) {
        return new TryCatchBlockNode(new LabelNode(), new LabelNode(), label, "Ljava/lang/Exception;");
    }
}
