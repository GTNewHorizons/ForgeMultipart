package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.MethodInfo;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.IterableView;
import scala.collection.Iterator;
import scala.collection.mutable.ListBuffer;

class ClassInfoCharacterizationTest {

    @Test
    void traversesOwnMethodsThenSuperclassThenInterfacesWithoutDeduplicating() {
        StubClass root = new StubClass("Root"), parent = new StubClass("Parent"), first = new StubClass("First"),
                second = new StubClass("Second"), child = new StubClass("Child");
        StubMethod shared = root.add("shared", "()V", ACC_PUBLIC);
        StubMethod inherited = parent.add("run", "()V", ACC_PUBLIC);
        StubMethod own = child.add("run", "()V", ACC_PUBLIC);
        StubMethod iface = first.add("run", "(I)V", ACC_PUBLIC);
        parent.parent = Option.apply(root);
        first.parents.$plus$eq(root);
        second.parents.$plus$eq(root);
        child.parent = Option.apply(parent);
        child.parents.$plus$eq(first);
        child.parents.$plus$eq(second);

        assertEquals(Arrays.asList(own, inherited, shared, iface, shared, shared), list(child.allMethods()));
        assertEquals(Arrays.asList(inherited, shared, iface, shared, shared), list(child.parentMethods()));
        assertSame(own, child.findPublicImpl("run", "()V").get());
        assertSame(iface, child.findPublicImpl("run", "(I)V").get());
        assertFalse(child.findPublicImpl("missing", "()V").isDefined());
        assertFalse(child.isScala());
        assertFalse(child.isTrait());
        assertFalse(child.isObject());
        assertEquals("Child", child.moduleName());
        assertEquals("StubClass(Child)", child.toString());
        assertEquals("Child.run()V", own.toString());
    }

    @Test
    void capturesParentIdentitiesButReadsTheirMethodsLazilyOnEachTraversal() {
        StubClass parent = new StubClass("Parent"), iface = new StubClass("Interface"), child = new StubClass("Child");
        StubMethod original = parent.add("original", "()V", ACC_PUBLIC);
        child.parent = Option.apply(parent);
        child.parents.$plus$eq(iface);

        IterableView<MethodInfo, Iterable<?>> view = child.parentMethods();
        assertEquals(0, parent.methodReads);
        assertEquals(0, iface.methodReads);
        child.parent = Option.empty();
        child.parents.clear();
        StubMethod later = iface.add("later", "()V", ACC_PUBLIC);
        assertEquals(Arrays.asList(original, later), list(view));
        int reads = parent.methodReads;
        parent.declared.clear();
        assertEquals(Arrays.asList(later), list(view));
        assertTrue(parent.methodReads > reads);
        assertTrue(list(child.parentMethods()).isEmpty());
    }

    @Test
    void allMethodsMaterializesStrictInputsButKeepsAViewInputLazy() {
        StubClass parent = new StubClass("Parent"), child = new StubClass("Child");
        StubMethod own = child.add("own", "()V", ACC_PUBLIC), inherited = parent.add("inherited", "()V", ACC_PUBLIC);
        child.parent = Option.apply(parent);
        Iterable<MethodInfo> snapshot = child.allMethods();
        assertTrue(parent.methodReads > 0);
        child.declared.clear();
        parent.declared.clear();
        assertEquals(Arrays.asList(own, inherited), list(snapshot));

        child.useView = true;
        Iterable<MethodInfo> live = child.allMethods();
        StubMethod added = child.add("added", "()V", ACC_PUBLIC);
        assertEquals(Arrays.asList(added), list(live));
    }

    @Test
    void findsFirstConcreteNonPrivateMatchWithShortCircuitingAndVirtualDispatch() {
        List<String> calls = new ArrayList<>();
        StubClass owner = new StubClass("Owner");
        ListBuffer<MethodInfo> candidates = new ListBuffer<>();
        candidates.$plus$eq(tracked(owner, "different", "()V", 0, calls));
        candidates.$plus$eq(tracked(owner, "run", "(I)V", 0, calls));
        candidates.$plus$eq(tracked(owner, "run", "()V", ACC_ABSTRACT, calls));
        candidates.$plus$eq(tracked(owner, "run", "()V", ACC_PRIVATE, calls));
        MethodInfo protectedMethod = tracked(owner, "run", "()V", ACC_PROTECTED, calls);
        candidates.$plus$eq(protectedMethod);
        candidates.$plus$eq(null); // Finding a match must not evaluate a later candidate.
        StubClass query = new StubClass("Query") {

            @Override
            public Iterable<MethodInfo> allMethods() {
                calls.add("allMethods");
                return candidates;
            }
        };
        assertSame(protectedMethod, query.findPublicImpl("run", "()V").get());
        assertEquals(
                Arrays.asList(
                        "allMethods",
                        "name",
                        "name",
                        "desc",
                        "name",
                        "desc",
                        "abstract",
                        "name",
                        "desc",
                        "abstract",
                        "private",
                        "name",
                        "desc",
                        "abstract",
                        "private"),
                calls);
        owner.add(null, null, 0);
        assertTrue(owner.findPublicImpl(null, null).isDefined());
        assertThrows(NullPointerException.class, () -> query.findPublicImpl("missing", "()V"));
    }

    @Test
    void nodeMethodWrappersFollowMutableMetadataAndRetainTheirOuterOwner() throws Exception {
        ClassNode node = new ClassNode();
        node.name = "test/Node";
        MethodNode method = new MethodNode(ACC_PRIVATE, "read", "(I)J", null, new String[] { "java/io/IOException" });
        node.methods.add(method);
        ClassInfo info = nodeInfo(node);
        MethodInfo wrapped = info.methods().head();
        assertSame(node, call(info, "cnode"));
        assertSame(info, wrapped.owner());
        assertSame(method, call(wrapped, "mnode"));
        assertEquals("test/Node.read(I)J", wrapped.toString());
        assertTrue(wrapped.isPrivate());
        assertFalse(wrapped.isAbstract());
        assertArrayEquals(new String[] { "java/io/IOException" }, wrapped.exceptions());
        String[] exceptions = wrapped.exceptions();
        exceptions[0] = "changed";
        assertArrayEquals(new String[] { "java/io/IOException" }, wrapped.exceptions());

        method.name = "changed";
        method.desc = "()V";
        method.access = ACC_ABSTRACT | ACC_PUBLIC;
        method.exceptions.add("java/lang/Exception");
        node.name = "test/Renamed";
        assertEquals("test/Renamed.changed()V", wrapped.toString());
        assertFalse(wrapped.isPrivate());
        assertTrue(wrapped.isAbstract());
        assertArrayEquals(new String[] { "java/io/IOException", "java/lang/Exception" }, wrapped.exceptions());
        assertEquals(wrapped, copy(wrapped, MethodNode.class, call(wrapped, "copy$default$1")));
        assertSame(info, copy(wrapped, MethodNode.class, method).owner());
        assertEquals(1, ((scala.Product) wrapped).productArity());
        assertSame(method, ((scala.Product) wrapped).productElement(0));
        assertNotEquals(wrapped, nodeInfo(node).methods().head());
        Iterable<MethodInfo> previous = info.methods();
        node.methods.add(new MethodNode(ACC_PUBLIC, "added", "()I", null, null));
        assertEquals(1, previous.size());
        assertEquals(2, info.methods().size());
        assertNotSame(wrapped, info.methods().head());
    }

    @Test
    void reflectionWrappersPreserveJdkOrderDescriptorsExceptionsAndCaseClassOwner() throws Exception {
        ClassInfo info = reflectionInfo(Reflected.class);
        Method[] reflected = Reflected.class.getMethods();
        List<MethodInfo> methods = list(info.methods());
        assertEquals(reflected.length, methods.size());
        for (int i = 0; i < reflected.length; i++) {
            assertSame(info, methods.get(i).owner());
            assertEquals(reflected[i].getName(), methods.get(i).name());
            assertEquals(Type.getMethodDescriptor(reflected[i]), methods.get(i).desc());
        }
        Method method = Reflected.class.getDeclaredMethod("read", long.class, String[].class);
        MethodInfo wrapped = reflectionMethod(info, method);
        assertEquals("(J[Ljava/lang/String;)D", wrapped.desc());
        assertArrayEquals(
                new String[] { "java/io/IOException", "java/lang/ReflectiveOperationException" },
                wrapped.exceptions());
        assertFalse(wrapped.isPrivate());
        assertTrue(wrapped.isAbstract());
        assertSame(info, copy(wrapped, Method.class, call(wrapped, "copy$default$1")).owner());
        assertEquals(wrapped, copy(wrapped, Method.class, method));
        assertNotEquals(wrapped, reflectionMethod(reflectionInfo(Reflected.class), method));
        assertSame(method, ((scala.Product) wrapped).productElement(0));
        assertEquals(info.name() + ".read(J[Ljava/lang/String;)D", wrapped.toString());
        MethodInfo hidden = reflectionMethod(info, Reflected.class.getDeclaredMethod("hidden"));
        assertTrue(hidden.isPrivate());
        assertFalse(hidden.isAbstract());
    }

    // Scala nests these classes under ClassInfo$ without the extra '$' javac expects in their binary names.
    private static ClassInfo nodeInfo(ClassNode node) throws Exception {
        return (ClassInfo) Class.forName("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ClassNodeInfo")
                .getConstructor(ClassNode.class).newInstance(node);
    }

    private static ClassInfo reflectionInfo(Class<?> type) throws Exception {
        return (ClassInfo) Class.forName("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ReflectionClassInfo")
                .getConstructor(Class.class).newInstance(type);
    }

    private static MethodInfo reflectionMethod(ClassInfo owner, Method method) throws Exception {
        Object factory = call(owner, "ReflectionMethodInfo");
        return (MethodInfo) factory.getClass().getMethod("apply", Method.class).invoke(factory, method);
    }

    private static Object call(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static MethodInfo copy(MethodInfo source, Class<?> parameter, Object value) throws Exception {
        return (MethodInfo) source.getClass().getMethod("copy", parameter).invoke(source, value);
    }

    private static StubMethod tracked(StubClass owner, String name, String desc, int access, List<String> calls) {
        return new StubMethod(owner, name, desc, access) {

            @Override
            public String name() {
                calls.add("name");
                return super.name();
            }

            @Override
            public String desc() {
                calls.add("desc");
                return super.desc();
            }

            @Override
            public boolean isAbstract() {
                calls.add("abstract");
                return super.isAbstract();
            }

            @Override
            public boolean isPrivate() {
                calls.add("private");
                return super.isPrivate();
            }
        };
    }

    private static <T> List<T> list(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        Iterator<T> iterator = values.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }

    private static class StubClass extends ClassInfo {

        private final String name;
        private Option<ClassInfo> parent = Option.empty();
        private final ListBuffer<ClassInfo> parents = new ListBuffer<>();
        private final ListBuffer<MethodInfo> declared = new ListBuffer<>();
        private int methodReads;
        private boolean useView;

        private StubClass(String name) {
            this.name = name;
        }

        private StubMethod add(String method, String desc, int access) {
            StubMethod result = new StubMethod(this, method, desc, access);
            declared.$plus$eq(result);
            return result;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Option<ClassInfo> superClass() {
            return parent;
        }

        @Override
        public Iterable<ClassInfo> interfaces() {
            return parents;
        }

        @Override
        public Iterable<MethodInfo> methods() {
            methodReads++;
            return useView ? scala.collection.IterableLike$class.view(declared) : declared;
        }
    }

    private static class StubMethod extends MethodInfo {

        private final StubClass owner;
        private final String name, desc;
        private final int access;

        private StubMethod(StubClass owner, String name, String desc, int access) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.access = access;
        }

        @Override
        public ClassInfo owner() {
            return owner;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String desc() {
            return desc;
        }

        @Override
        public String[] exceptions() {
            return new String[0];
        }

        @Override
        public boolean isPrivate() {
            return (access & ACC_PRIVATE) != 0;
        }

        @Override
        public boolean isAbstract() {
            return (access & ACC_ABSTRACT) != 0;
        }
    }

    public abstract static class Reflected {

        public abstract double read(long value, String[] names) throws IOException, ReflectiveOperationException;

        private void hidden() {}
    }
}
