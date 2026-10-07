package codechicken.multipart.asm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.launchwrapper.LaunchClassLoader;

import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

/** Reflection and storage initialization for the retained compiler singleton. */
final class CompilerBootstrap {

    private CompilerBootstrap() {}

    static LaunchClassLoader loader(Class<?> owner) {
        return (LaunchClassLoader) owner.getClassLoader();
    }

    static Method defineClassMethod() throws NoSuchMethodException {
        return ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
    }

    static Method runTransformersMethod() throws NoSuchMethodException {
        return LaunchClassLoader.class.getDeclaredMethod("runTransformers", String.class, String.class, byte[].class);
    }

    static Field transformerExceptionsField() throws NoSuchFieldException {
        return LaunchClassLoader.class.getDeclaredField("transformerExceptions");
    }

    static void open(AccessibleObject... members) {
        for (AccessibleObject member : members) member.setAccessible(true);
    }

    static <K, V> Map<K, V> mutableMap() {
        return new HashMap<>();
    }

    static void warmupSanityChecker() {
        ASMMixinCompiler$.MODULE$.getBytes("cpw/mods/fml/common/asm/FMLSanityChecker");
    }
}
