package codechicken.multipart.asm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.asm.ObfMapping;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import scala.collection.JavaConversions;
import scala.collection.mutable.Map;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;

/** Class-byte loading and caching behind the retained compiler singleton and Scala entry points. */
final class ClassBytes {

    private ClassBytes() {}

    static Class<?> define(String name, byte[] bytes) throws IllegalAccessException, InvocationTargetException {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        compiler.internalDefine(name, bytes);
        DebugPrinter$.MODULE$.defined(name, bytes);
        try {
            return (Class<?>) compiler.m_defineClass().invoke(compiler.cl(), bytes, 0, bytes.length);
        } catch (LinkageError link) {
            // Reflective failures normally remain wrapped; retain the original direct-error guard.
            if (link.getMessage().contains("duplicate")) {
                throw new IllegalStateException(
                        "class with name: " + name
                                + " already loaded. Do not reference your java mixin classes before registering",
                        link);
            }
            throw link;
        }
    }

    static byte[] getBytes(String name) throws IOException, IllegalAccessException, InvocationTargetException {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        final String javaName = name.replace('/', '.');
        if (javaName.equals("java.lang.Object")) return null;
        String obfuscatedName = ObfMapping.obfuscated ? FMLDeobfuscatingRemapper.INSTANCE.unmap(name).replace('/', '.')
                : javaName;
        byte[] bytes = compiler.cl().getClassBytes(obfuscatedName);
        if (bytes != null && useTransformers(compiler, javaName)) {
            return (byte[]) compiler.m_runTransformers().invoke(compiler.cl(), javaName, obfuscatedName, bytes);
        }
        return bytes;
    }

    @SuppressWarnings("unchecked")
    private static boolean useTransformers(ASMMixinCompiler$ compiler, final String name)
            throws IllegalAccessException {
        Set<String> exclusions = (Set<String>) compiler.f_transformerExceptions().get(compiler.cl());
        return JavaConversions.asScalaSet(exclusions).find(new AbstractFunction1<String, Object>() {

            @Override
            public Object apply(String prefix) {
                return name.startsWith(prefix);
            }
        }).isEmpty();
    }

    static void internalDefine(Map<String, byte[]> cache, String name, byte[] bytes) {
        String normalized = ASMImplicits.nodeName(name);
        cache.put(normalized, bytes);
        ASMMixinCompiler$.MODULE$.remClassInfo(normalized);
        DebugPrinter$.MODULE$.dump(normalized, bytes);
    }

    static ClassNode classNode(Map<String, byte[]> cache, String name) {
        final String normalized = ASMImplicits.nodeName(name);
        byte[] bytes = cache.getOrElseUpdate(normalized, new AbstractFunction0<byte[]>() {

            @Override
            public byte[] apply() {
                return ASMMixinCompiler$.MODULE$.getBytes(normalized);
            }
        });
        return bytes == null ? null : ASMHelper.createClassNode(bytes, ClassReader.EXPAND_FRAMES);
    }
}
