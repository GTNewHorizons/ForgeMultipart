package codechicken.microblock.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.microblock.ItemMicroPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.collection.mutable.MutableList;

class MicroblockProxyCharacterizationTest {

    private static final Set<String> SERVER_METHODS = new TreeSet<>(
            Arrays.asList(
                    "addSawRecipe(Lnet/minecraft/item/Item;Lnet/minecraft/item/Item;)V",
                    "createSaw(Lcodechicken/lib/config/ConfigFile;Ljava/lang/String;I)Lnet/minecraft/item/Item;",
                    "init()V",
                    "itemMicro()Lcodechicken/microblock/ItemMicroPart;",
                    "itemMicro_$eq(Lcodechicken/microblock/ItemMicroPart;)V",
                    "logger()Lorg/apache/logging/log4j/Logger;",
                    "logger_$eq(Lorg/apache/logging/log4j/Logger;)V",
                    "postInit()V",
                    "preInit(Lorg/apache/logging/log4j/Logger;)V",
                    "sawDiamond()Lnet/minecraft/item/Item;",
                    "sawDiamond_$eq(Lnet/minecraft/item/Item;)V",
                    "sawIron()Lnet/minecraft/item/Item;",
                    "sawIron_$eq(Lnet/minecraft/item/Item;)V",
                    "sawStone()Lnet/minecraft/item/Item;",
                    "sawStone_$eq(Lnet/minecraft/item/Item;)V",
                    "saws()Lscala/collection/mutable/MutableList;",
                    "saws_$eq(Lscala/collection/mutable/MutableList;)V",
                    "stoneRod()Lnet/minecraft/item/Item;",
                    "stoneRod_$eq(Lnet/minecraft/item/Item;)V",
                    "useSawIcons()Z",
                    "useSawIcons_$eq(Z)V"));
    private static final Set<String> CLIENT_METHODS = new TreeSet<>(
            Arrays.asList("init()V", "postInit()V", "renderBlocks()Lnet/minecraft/client/renderer/RenderBlocks;"));
    private static final Set<String> FACADE_METHODS = new TreeSet<>();
    private static final Set<String> CLIENT_ONLY_METHODS = new TreeSet<>(Arrays.asList("init()V", "postInit()V"));

    static {
        FACADE_METHODS.addAll(SERVER_METHODS);
        FACADE_METHODS.remove("saws()Lscala/collection/mutable/MutableList;");
        FACADE_METHODS.remove("saws_$eq(Lscala/collection/mutable/MutableList;)V");
        FACADE_METHODS.add("renderBlocks()Lnet/minecraft/client/renderer/RenderBlocks;");
    }

    @Test
    void keepsTheFourTypeHierarchyAndPublicSurfaces() throws Exception {
        assertEquals(Object.class, MicroblockProxy_serverImpl.class.getSuperclass());
        assertEquals(MicroblockProxy_serverImpl.class, MicroblockProxy_clientImpl.class.getSuperclass());
        assertEquals(Object.class, MicroblockProxy.class.getSuperclass());
        assertEquals(MicroblockProxy_clientImpl.class, MicroblockProxy$.class.getSuperclass());
        assertFalse(Modifier.isFinal(MicroblockProxy_serverImpl.class.getModifiers()));
        assertFalse(Modifier.isFinal(MicroblockProxy_clientImpl.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroblockProxy.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroblockProxy$.class.getModifiers()));

        assertEquals(SERVER_METHODS, publicDeclaredMethodSignatures(MicroblockProxy_serverImpl.class));
        assertEquals(CLIENT_METHODS, publicDeclaredMethodSignatures(MicroblockProxy_clientImpl.class));
        assertEquals(FACADE_METHODS, publicDeclaredMethodSignatures(MicroblockProxy.class));
        assertEquals(new TreeSet<String>(), publicDeclaredMethodSignatures(MicroblockProxy$.class));

        for (Method method : MicroblockProxy.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
        }

        Field module = MicroblockProxy$.class.getField("MODULE$");
        assertSame(MicroblockProxy$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MicroblockProxy$.MODULE$, module.get(null));
    }

    @Test
    void keepsTheMutableFieldsAndLazyRendererShape() throws Exception {
        assertEquals(8, MicroblockProxy_serverImpl.class.getDeclaredFields().length);
        assertPrivateField(MicroblockProxy_serverImpl.class, "logger", Logger.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "itemMicro", ItemMicroPart.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "sawStone", Item.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "sawIron", Item.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "sawDiamond", Item.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "stoneRod", Item.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "useSawIcons", boolean.class, false);
        assertPrivateField(MicroblockProxy_serverImpl.class, "saws", MutableList.class, false);

        assertEquals(2, MicroblockProxy_clientImpl.class.getDeclaredFields().length);
        assertPrivateField(MicroblockProxy_clientImpl.class, "renderBlocks", RenderBlocks.class, true);
        Field bitmap = assertPrivateField(MicroblockProxy_clientImpl.class, "bitmap$0", boolean.class, false);
        assertTrue(Modifier.isVolatile(bitmap.getModifiers()));

        Method lazyCompute = MicroblockProxy_clientImpl.class.getDeclaredMethod("renderBlocks$lzycompute");
        assertSame(RenderBlocks.class, lazyCompute.getReturnType());
        assertTrue(Modifier.isPrivate(lazyCompute.getModifiers()));
        assertNull(lazyCompute.getAnnotation(SideOnly.class));
    }

    @Test
    void keepsOnlyTheClientLifecycleOverridesSideOnly() {
        assertSideOnlyMethods(MicroblockProxy_serverImpl.class, new TreeSet<String>());
        assertSideOnlyMethods(MicroblockProxy_clientImpl.class, CLIENT_ONLY_METHODS);
        assertSideOnlyMethods(MicroblockProxy.class, CLIENT_ONLY_METHODS);
        assertSideOnlyMethods(MicroblockProxy$.class, new TreeSet<String>());

        assertNull(MicroblockProxy_serverImpl.class.getAnnotation(SideOnly.class));
        assertNull(MicroblockProxy_clientImpl.class.getAnnotation(SideOnly.class));
        assertNull(MicroblockProxy.class.getAnnotation(SideOnly.class));
        assertNull(MicroblockProxy$.class.getAnnotation(SideOnly.class));
    }

    @Test
    void facadeAndCompanionShareStateWhileServerInstancesKeepIndependentSawLists() {
        Logger originalLogger = MicroblockProxy.logger();
        boolean originalIcons = MicroblockProxy.useSawIcons();
        Logger marker = LogManager.getLogger("MicroblockProxyCharacterizationTest.marker");
        try {
            MicroblockProxy.logger_$eq(marker);
            MicroblockProxy.useSawIcons_$eq(!originalIcons);
            assertSame(marker, MicroblockProxy$.MODULE$.logger());
            assertEquals(!originalIcons, MicroblockProxy$.MODULE$.useSawIcons());
        } finally {
            MicroblockProxy.logger_$eq(originalLogger);
            MicroblockProxy.useSawIcons_$eq(originalIcons);
        }

        MicroblockProxy_serverImpl first = new MicroblockProxy_serverImpl();
        MicroblockProxy_serverImpl second = new MicroblockProxy_serverImpl();
        first.logger_$eq(marker);
        assertSame(marker, first.logger());
        assertNull(second.logger());
        assertNotNull(first.saws());
        assertTrue(first.saws().isEmpty());
        assertTrue(second.saws().isEmpty());
        assertNotSame(first.saws(), second.saws());
    }

    private static Field assertPrivateField(Class<?> owner, String name, Class<?> type, boolean clientOnly)
            throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        SideOnly annotation = field.getAnnotation(SideOnly.class);
        if (clientOnly) {
            assertNotNull(annotation);
            assertEquals(Side.CLIENT, annotation.value());
        } else {
            assertNull(annotation);
        }
        return field;
    }

    private static void assertSideOnlyMethods(Class<?> type, Set<String> expected) {
        Set<String> actual = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            SideOnly annotation = method.getAnnotation(SideOnly.class);
            if (annotation != null) {
                assertEquals(Side.CLIENT, annotation.value(), method.toString());
                actual.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        assertEquals(expected, actual);
    }

    private static Set<String> publicDeclaredMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
