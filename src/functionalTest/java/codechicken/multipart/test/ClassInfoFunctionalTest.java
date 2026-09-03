package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo$;
import codechicken.multipart.asm.ASMMixinCompiler.MethodInfo;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.mutable.Map;

class ClassInfoFunctionalTest {

    @Test
    void choosesJavaNodesAndReflectionFallbackWithTheirDistinctRootSemantics() throws Exception {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        ClassInfo object = compiler.getClassInfo(Object.class);
        assertEquals(
                "codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ReflectionClassInfo",
                object.getClass().getName());
        assertEquals("java/lang/Object", object.name());
        assertFalse(object.superClass().isDefined());
        assertTrue(object.interfaces().isEmpty());
        assertEquals(Object.class.getMethods().length, object.methods().size());

        ClassInfo child = compiler.getClassInfo(LoadedChild.class);
        assertEquals("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ClassNodeInfo", child.getClass().getName());
        assertSame(compiler.getClassInfo(LoadedParent.class), child.superClass().get());
        assertEquals(Arrays.asList("java/lang/Runnable", "java/lang/AutoCloseable"), names(child.interfaces()));
        MethodInfo inherited = child.findPublicImpl("inherited", "()I").get();
        assertSame(compiler.getClassInfo(LoadedParent.class), inherited.owner());
        assertTrue(child.findPublicImpl("protectedMethod", "()V").isDefined());
        assertFalse(child.findPublicImpl("privateMethod", "()V").isDefined());
        assertFalse(child.findPublicImpl("run", "()V").isDefined());

        ClassInfo reflected = construct("ReflectionClassInfo", Class.class, LoadedChild.class);
        assertSame(child.superClass().get(), reflected.superClass().get());
        assertEquals(names(child.interfaces()), names(reflected.interfaces()));
        // Bytecode nodes preserve Some(null) for a null superclass; reflection represents the root as None.
        ClassNode root = new ClassNode();
        root.name = "test/Root";
        Option<ClassInfo> superClass = construct("ClassNodeInfo", ClassNode.class, root).superClass();
        assertTrue(superClass.isDefined());
        assertNull(superClass.get());
        compiler.remClassInfo((String) null);
    }

    @Test
    void cachesExactStringKeysAndUsesOnlyTheNameOfSuppliedNodes() {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        String slash = Type.getInternalName(CacheSubject.class), dotted = CacheSubject.class.getName();
        compiler.remClassInfo(slash);
        compiler.remClassInfo(dotted);
        try {
            ClassInfo first = compiler.getClassInfo(slash);
            assertSame(first, compiler.getClassInfo(CacheSubject.class));
            assertSame(first, ASMMixinCompiler.getClassInfo(slash));
            ClassNode misleading = new ClassNode();
            misleading.name = slash;
            misleading.superName = "test/Missing";
            assertSame(first, compiler.getClassInfo(misleading));
            assertTrue(first.methods().exists(new scala.runtime.AbstractFunction1<MethodInfo, Object>() {

                @Override
                public Object apply(MethodInfo method) {
                    return method.name().equals("actual");
                }
            }));
            ClassInfo alias = compiler.getClassInfo(dotted);
            assertNotSame(first, alias);
            assertEquals(first.name(), alias.name());
            assertSame(alias, compiler.getClassInfo(dotted));
            assertSame(first, compiler.remClassInfo(slash).get());
            assertFalse(compiler.remClassInfo(slash).isDefined());
            assertNotSame(first, compiler.getClassInfo(slash));
            assertSame(alias, compiler.getClassInfo(dotted));
        } finally {
            compiler.remClassInfo(slash);
            compiler.remClassInfo(dotted);
        }
    }

    @Test
    void cachesNullResultsButDoesNotCacheFailuresOrTreatUncachedLookupsAsCached() throws Exception {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        Map<String, ClassInfo> cache = state("infoCache");
        Option<ClassInfo> previousNull = cache.remove(null);
        String missing = "codechicken/multipart/test/metadata/DefinitelyAbsent";
        try {
            assertNull(compiler.getClassInfo((Class<?>) null));
            assertFalse(cache.contains(null));
            assertThrows(NullPointerException.class, () -> compiler.getClassInfo((ClassNode) null));
            assertNull(compiler.getClassInfo((String) null));
            assertTrue(cache.contains(null));
            assertTrue(compiler.remClassInfo((String) null).isDefined());
            assertThrows(ClassNotFoundException.class, () -> compiler.getClassInfo(missing));
            assertFalse(cache.contains(missing));
            assertThrows(ClassNotFoundException.class, () -> compiler.getClassInfo(missing));

            String name = Type.getInternalName(CacheSubject.class);
            ClassInfo first = ClassInfo$.MODULE$.obtainInfo(name);
            ClassInfo second = ClassInfo$.MODULE$.obtainInfo(name);
            assertNotSame(first, second);
            assertEquals(first.name(), second.name());
        } finally {
            cache.remove(null);
            if (previousNull.isDefined()) cache.put(null, previousNull.get());
            compiler.remClassInfo(missing);
        }
    }

    @Test
    void internalDefineInvalidatesOnlyTheNormalizedMetadataKey() throws Exception {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        String slash = "codechicken/multipart/test/metadata/CacheProbe", dotted = slash.replace('/', '.');
        Map<String, byte[]> bytes = state("traitByteMap");
        Map<String, ClassInfo> cache = state("infoCache");
        Option<byte[]> oldBytes = bytes.get(slash);
        Option<ClassInfo> oldSlash = cache.get(slash), oldDotted = cache.get(dotted);
        try {
            compiler.internalDefine(slash, definition(slash, "before"));
            ClassInfo before = compiler.getClassInfo(slash);
            ClassInfo alias = compiler.getClassInfo(dotted);
            assertTrue(before.findPublicImpl("before", "()V").isDefined());
            compiler.internalDefine(dotted, definition(slash, "after"));
            ClassInfo after = compiler.getClassInfo(slash);
            assertNotSame(before, after);
            assertTrue(after.findPublicImpl("after", "()V").isDefined());
            assertFalse(after.findPublicImpl("before", "()V").isDefined());
            assertSame(alias, compiler.getClassInfo(dotted));
            assertTrue(alias.findPublicImpl("before", "()V").isDefined());
            assertEquals("after", compiler.classNode(slash).methods.get(0).name);
        } finally {
            restore(bytes, slash, oldBytes);
            restore(cache, slash, oldSlash);
            restore(cache, dotted, oldDotted);
        }
    }

    @Test
    void readsScalaTraitAndCompanionMetadataFromTheRetainedSignatures() throws Exception {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        String traitName = "codechicken/multipart/test/ExternalScalaMicroblockFixture";
        ClassInfo trait = compiler.getClassInfo(traitName);
        assertEquals("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ScalaClassInfo", trait.getClass().getName());
        assertTrue(trait.isScala());
        assertTrue(trait.isTrait());
        assertFalse(trait.isObject());
        assertEquals("codechicken/microblock/Microblock", trait.superClass().get().name());
        codechicken.multipart.asm.ScalaSignature signature = (codechicken.multipart.asm.ScalaSignature) trait.getClass()
                .getMethod("sig").invoke(trait);
        assertTrue(signature.findClass(traitName.replace('/', '.')).isDefined());
        assertSame(
                trait.getClass().getMethod("csym").invoke(trait).getClass(),
                signature.findClass(traitName.replace('/', '.')).get().getClass());
        assertEquals(traitName, trait.moduleName());

        String module = "codechicken/multipart/asm/StackAnalyser$";
        ClassInfo info = compiler.getClassInfo(module);
        assertEquals("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ScalaClassInfo", info.getClass().getName());
        assertTrue(info.isScala());
        assertTrue(info.isObject());
        assertFalse(info.isTrait());
        // The companion is resolved from the base class's signature and bytecode node.
        assertEquals(module.substring(0, module.length() - 1), info.name());
        assertEquals(info.name(), info.moduleName());
        // The current decoder cannot interpret this object's TypeRefType as a ClassType.
        assertThrows(ClassCastException.class, info::superClass);
        assertThrows(ClassCastException.class, info::interfaces);
        assertTrue(
                ((codechicken.multipart.asm.ScalaSignature.ClassSymbolRef) info.getClass().getMethod("csym")
                        .invoke(info)).isObject());
    }

    private static ClassInfo construct(String nestedName, Class<?> parameter, Object value) throws Exception {
        return (ClassInfo) Class.forName("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$" + nestedName)
                .getConstructor(parameter).newInstance(value);
    }

    private static List<String> names(Iterable<ClassInfo> infos) {
        List<String> result = new ArrayList<>();
        Iterator<ClassInfo> iterator = infos.iterator();
        while (iterator.hasNext()) result.add(iterator.next().name());
        return result;
    }

    private static byte[] definition(String name, String method) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_6, ACC_PUBLIC, name, null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor body = writer.visitMethod(ACC_PUBLIC, method, "()V", null, null);
        body.visitCode();
        body.visitInsn(RETURN);
        body.visitMaxs(0, 1);
        body.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> state(String name) throws Exception {
        Field field = ASMMixinCompiler$.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, T>) field.get(ASMMixinCompiler$.MODULE$);
    }

    private static <T> void restore(Map<String, T> map, String key, Option<T> old) {
        map.remove(key);
        if (old.isDefined()) map.put(key, old.get());
    }

    public static class LoadedParent {

        public int inherited() {
            return 3;
        }

        protected void protectedMethod() {}

        private void privateMethod() {}
    }

    public abstract static class LoadedChild extends LoadedParent implements Runnable, AutoCloseable {
    }

    public static class CacheSubject {

        public void actual() {}
    }
}
