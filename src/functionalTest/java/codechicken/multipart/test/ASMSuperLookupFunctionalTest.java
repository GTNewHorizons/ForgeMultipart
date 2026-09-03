package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.MethodInfo;
import codechicken.multipart.asm.StackAnalyser;
import codechicken.multipart.asm.StackAnalyser.Const;
import codechicken.multipart.asm.StackAnalyser.Load;
import codechicken.multipart.asm.StackAnalyser.Param;
import codechicken.multipart.asm.StackAnalyser.StackEntry;
import codechicken.multipart.asm.StackAnalyser.Store;
import codechicken.multipart.asm.StackAnalyser.This;
import scala.Option;
import scala.Some;
import scala.collection.Iterable;
import scala.collection.mutable.Map;

class ASMSuperLookupFunctionalTest {

    private static final Type OWNER = Type.getType(Child.class);

    @Test
    void rejectsOwnOwnerAndWrongNamesBeforeReadingTheDescriptorOrStack() {
        StackAnalyser stack = stack("target");
        stack.m().name = null;
        assertFalse(ASMMixinCompiler.getSuper(call(OWNER.getInternalName(), null, null), stack).isDefined());
        stack.m().name = "different";
        assertFalse(ASMMixinCompiler.getSuper(call(null, "target", null), stack).isDefined());
        assertFalse(ASMMixinCompiler.getSuper(call(null, null, null), stack).isDefined());
        stack.m().name = "target";
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.getSuper(call(null, "target", null), stack));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> ASMMixinCompiler.getSuper(call(null, "target", "()I"), stack));
        stack.m().name = null;
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.getSuper(call(null, "target", "()I"), stack));
    }

    @Test
    void stripsTheGreedyScalaSuperPrefixButRequiresACharacterBeforeIt() {
        for (String name : new String[] { "target", "owner$$super$target", "first$$super$second$$super$target" }) {
            MethodInfo result = lookup(name, "target", "()I").get();
            assertEquals(Type.getInternalName(Parent.class), result.owner().name());
            assertEquals("target", result.name());
        }
        assertFalse(lookup("$$super$target", "target", "()I").isDefined());
        assertFalse(lookup("prefix$$super$targetSuffix", "target", "()I").isDefined());
        assertFalse(lookup("target", "owner$$super$target", "()I").isDefined());
    }

    @Test
    void startsAtTheAnalysedOwnersSuperclassAndSelectsTheExactConcreteNonPrivateSignature() {
        assertEquals(Type.getInternalName(Parent.class), lookup("target", "target", "()I").get().owner().name());
        StackAnalyser overload = stack("target");
        overload.push(receiver());
        overload.push(new Const(3, null));
        MethodInfo result = ASMMixinCompiler.getSuper(call("unrelated/Owner", "target", "(I)I"), overload).get();
        assertEquals(Type.getInternalName(Grandparent.class), result.owner().name());
        assertEquals("(I)I", result.desc());
        assertEquals(
                Type.getInternalName(Grandparent.class),
                lookup("inherited", "inherited", "()I").get().owner().name());
        assertTrue(lookup("protectedTarget", "protectedTarget", "()I").isDefined());
        assertFalse(lookup("hidden", "hidden", "()I").isDefined());
        assertFalse(lookup("abstractTarget", "abstractTarget", "()I").isDefined());
        assertFalse(lookup("childOnly", "childOnly", "()I").isDefined());
        assertFalse(lookup("target", "target", "()J").isDefined());
        StackAnalyser root = new StackAnalyser(Type.getType(Object.class), method("target"));
        root.push(receiver());
        assertFalse(ASMMixinCompiler.getSuper(call("unrelated/Owner", "target", "()I"), root).isDefined());
    }

    @Test
    void requiresALoadOfThisButDoesNotValidateItsTypeTheInvocationOwnerOrOpcode() {
        StackEntry[] rejected = { null, new Const(null, null), new Load(null, null),
                new Load(new Param(0, OWNER), null), new Load(new Store(receiver(), null), null) };
        for (StackEntry entry : rejected) {
            StackAnalyser stack = stack("target");
            stack.stack().$plus$eq(entry);
            assertFalse(ASMMixinCompiler.getSuper(call("unrelated/Owner", "target", "()I"), stack).isDefined());
        }
        for (int opcode : new int[] { INVOKESPECIAL, INVOKEVIRTUAL, INVOKESTATIC, INVOKEINTERFACE }) {
            StackAnalyser stack = stack("target");
            // The pattern does not read This.owner or the entry's type.
            stack.stack().$plus$eq(new Load(new This(null), null));
            MethodInsnNode call = new MethodInsnNode(opcode, null, "target", "()I", opcode == INVOKEINTERFACE);
            assertEquals(
                    Type.getInternalName(Parent.class),
                    ASMMixinCompiler$.MODULE$.getSuper(call, stack).get().owner().name());
            assertEquals(1, stack.stack().size());
        }
    }

    @Test
    void retainsArgumentCountIndexingEvenWhenWideValuesOccupyTwoStackSlots() {
        StackAnalyser narrow = stack("two");
        Load receiver = receiver();
        narrow.push(receiver);
        narrow.push(new Const(1, null));
        narrow.push(new Const(2, null));
        assertTrue(ASMMixinCompiler.getSuper(call("parent", "two", "(II)I"), narrow).isDefined());
        assertEquals(3, narrow.stack().size());
        assertSame(receiver, narrow.peek(2));
        for (Object value : new Object[] { 5L, 2.5D }) {
            StackAnalyser wide = stack("wide");
            wide.push(receiver);
            Const argument = new Const(value, null);
            wide.push(argument);
            String desc = value instanceof Long ? "(J)I" : "(D)I";
            assertSame(argument, wide.peek(1));
            assertFalse(ASMMixinCompiler.getSuper(call("parent", "wide", desc), wide).isDefined());
            assertEquals(3, wide.stack().size());
            assertSame(receiver, wide.peek(2));
        }
    }

    @Test
    void preservesVirtualReadsAndUsesTheDescriptorAfterTheSuperclassCallback() throws Exception {
        List<String> events = new ArrayList<>();
        MethodInsnNode call = call("parent", "target", "()I");
        Type switched = Type.getObjectType("codechicken/multipart/test/superlookup/Switched");
        Option<MethodInfo> selected = new Some<>(lookup("target", "target", "()I").get());
        ClassInfo parent = new TestInfo() {

            @Override
            public Option<MethodInfo> findPublicImpl(String name, String desc) {
                events.add("find:" + name + desc);
                return selected;
            }
        };
        ClassInfo info = new TestInfo() {

            @Override
            public Option<ClassInfo> superClass() {
                events.add("super:" + call.desc);
                call.desc = "(J)I";
                call.name = "changed";
                return new Some<>(parent);
            }
        };
        StackAnalyser stack = new StackAnalyser(OWNER, method("target")) {

            private boolean switchedOwner;

            @Override
            public Type owner() {
                events.add("owner:" + switchedOwner);
                return switchedOwner ? switched : OWNER;
            }

            @Override
            public StackEntry peek(int index) {
                events.add("peek:" + index);
                switchedOwner = true;
                call.desc = "(I)I";
                return new Load(new This(null), null) {

                    @Override
                    public codechicken.multipart.asm.StackAnalyser.LocalEntry e() {
                        events.add("load");
                        return super.e();
                    }
                };
            }
        };
        events.clear(); // Discard constructor reads; characterize only the query.
        Map<String, ClassInfo> cache = cache();
        Option<ClassInfo> previous = cache.put(switched.getInternalName(), info);
        try {
            assertSame(selected, ASMMixinCompiler.getSuper(call, stack));
            assertEquals(
                    Arrays.asList("owner:false", "peek:0", "load", "owner:true", "super:(I)I", "find:target(J)I"),
                    events);
            cache.put(switched.getInternalName(), new TestInfo() {

                @Override
                public Option<ClassInfo> superClass() {
                    return new Some<>(null);
                }
            });
            call.name = "target";
            assertThrows(NullPointerException.class, () -> ASMMixinCompiler.getSuper(call, stack));
        } finally {
            cache.remove(switched.getInternalName());
            if (previous.isDefined()) cache.put(switched.getInternalName(), previous.get());
        }
    }

    private static Option<MethodInfo> lookup(String methodName, String targetName, String desc) {
        StackAnalyser stack = stack(methodName);
        stack.push(receiver());
        return ASMMixinCompiler.getSuper(call("unrelated/Owner", targetName, desc), stack);
    }

    private static StackAnalyser stack(String name) {
        return new StackAnalyser(OWNER, method(name));
    }

    private static MethodNode method(String name) {
        return new MethodNode(ACC_PUBLIC, name, "()I", null, null);
    }

    private static MethodInsnNode call(String owner, String name, String desc) {
        return new MethodInsnNode(INVOKESPECIAL, owner, name, desc, false);
    }

    private static Load receiver() {
        return new Load(new This(OWNER), null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClassInfo> cache() throws Exception {
        Field field = ASMMixinCompiler$.class.getDeclaredField("infoCache");
        field.setAccessible(true);
        return (Map<String, ClassInfo>) field.get(ASMMixinCompiler$.MODULE$);
    }

    private abstract static class TestInfo extends ClassInfo {

        @Override
        public String name() {
            throw new AssertionError("Unexpected name read");
        }

        @Override
        public Option<ClassInfo> superClass() {
            throw new AssertionError("Unexpected superclass read");
        }

        @Override
        public Iterable<ClassInfo> interfaces() {
            throw new AssertionError("Unexpected interface read");
        }

        @Override
        public Iterable<MethodInfo> methods() {
            throw new AssertionError("Unexpected method read");
        }
    }

    public static class Grandparent {

        public int inherited() {
            return 1;
        }

        public int target(int argument) {
            return argument;
        }
    }

    public abstract static class Parent extends Grandparent {

        public int target() {
            return 2;
        }

        protected int protectedTarget() {
            return 3;
        }

        private int hidden() {
            return 4;
        }

        public abstract int abstractTarget();

        public int two(int first, int second) {
            return first + second;
        }

        public int wide(long argument) {
            return (int) argument;
        }

        public int wide(double argument) {
            return (int) argument;
        }
    }

    public abstract static class Child extends Parent {

        @Override
        public int target() {
            return 5;
        }

        public int childOnly() {
            return 6;
        }
    }
}
