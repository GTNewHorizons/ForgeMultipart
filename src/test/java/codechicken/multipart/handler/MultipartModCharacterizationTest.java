package codechicken.multipart.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.multipart.Tags;
import cpw.mods.fml.common.Mod;

class MultipartModCharacterizationTest {

    private static final Set<String> EVENT_METHODS = new TreeSet<>(
            Arrays.asList(
                    "beforeServerStart(Lcpw/mods/fml/common/event/FMLServerAboutToStartEvent;)V",
                    "init(Lcpw/mods/fml/common/event/FMLInitializationEvent;)V",
                    "postInit(Lcpw/mods/fml/common/event/FMLPostInitializationEvent;)V",
                    "preInit(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V",
                    "serverStopped(Lcpw/mods/fml/common/event/FMLServerStoppedEvent;)V"));

    @Test
    void keepsBothAnnotatedSingletonTypesAndLifecycleMethods() throws Exception {
        assertModType(MultipartMod.class, true);
        assertModType(MultipartMod$.class, false);

        assertEquals(0, MultipartMod.class.getFields().length);
        Field module = MultipartMod$.class.getField("MODULE$");
        assertSame(MultipartMod$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MultipartMod$.MODULE$, module.get(null));
    }

    @Test
    void packetHandlerChannelUsesTheModCompanionIdentity() throws Exception {
        assertSame(MultipartMod$.MODULE$, new MultipartPH().channel());
        assertSame(MultipartMod$.class, MultipartPH.class.getDeclaredMethod("channel").getReturnType());
        assertSame(MultipartMod$.class, MultipartPH.class.getDeclaredField("channel").getType());
    }

    private static void assertModType(Class<?> type, boolean staticMethods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertEquals(EVENT_METHODS, publicMethodSignatures(type));

        Mod mod = type.getAnnotation(Mod.class);
        assertEquals("ForgeMultipart", mod.modid());
        assertEquals("Forge Multipart", mod.name());
        assertEquals("[1.7.10]", mod.acceptedMinecraftVersions());
        assertEquals(Tags.VERSION, mod.version());
        assertEquals("scala", mod.modLanguage());

        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertTrue(method.isAnnotationPresent(Mod.EventHandler.class));
                assertEquals(staticMethods, Modifier.isStatic(method.getModifiers()));
            }
        }
    }

    private static Set<String> publicMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
