package codechicken.microblock.handler;

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

import codechicken.microblock.AngelicaCompat;
import codechicken.multipart.Tags;
import cpw.mods.fml.common.Mod;

class MicroblockModCharacterizationTest {

    private static final Set<String> EVENT_METHOD_NAMES = new TreeSet<>(
            Arrays.asList("beforeServerStart", "handleIMC", "init", "postInit", "preInit"));

    private static final Set<String> PUBLIC_METHODS = new TreeSet<>(
            Arrays.asList(
                    "angelicaCompat()Lcodechicken/microblock/AngelicaCompat;",
                    "angelicaCompat_$eq(Lcodechicken/microblock/AngelicaCompat;)V",
                    "beforeServerStart(Lcpw/mods/fml/common/event/FMLServerAboutToStartEvent;)V",
                    "handleIMC(Lcpw/mods/fml/common/event/FMLInterModComms$IMCEvent;)V",
                    "init(Lcpw/mods/fml/common/event/FMLInitializationEvent;)V",
                    "postInit(Lcpw/mods/fml/common/event/FMLPostInitializationEvent;)V",
                    "preInit(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V"));

    @Test
    void keepsBothAnnotatedSingletonTypesAndPublicMethods() throws Exception {
        assertModType(MicroblockMod.class, true);
        assertModType(MicroblockMod$.class, false);

        assertEquals(0, MicroblockMod.class.getFields().length);
        Field module = MicroblockMod$.class.getField("MODULE$");
        assertSame(MicroblockMod$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MicroblockMod$.MODULE$, module.get(null));

        Field compat = MicroblockMod$.class.getDeclaredField("angelicaCompat");
        assertSame(AngelicaCompat.class, compat.getType());
        assertTrue(Modifier.isPrivate(compat.getModifiers()));
    }

    @Test
    void staticAndCompanionAccessorsShareAngelicaCompat() {
        AngelicaCompat original = MicroblockMod.angelicaCompat();
        AngelicaCompat marker = new AngelicaCompat();
        try {
            assertSame(original, MicroblockMod$.MODULE$.angelicaCompat());

            MicroblockMod.angelicaCompat_$eq(marker);
            assertSame(marker, MicroblockMod$.MODULE$.angelicaCompat());

            MicroblockMod$.MODULE$.angelicaCompat_$eq(original);
            assertSame(original, MicroblockMod.angelicaCompat());
        } finally {
            MicroblockMod.angelicaCompat_$eq(original);
        }
    }

    private static void assertModType(Class<?> type, boolean staticMethods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertEquals(PUBLIC_METHODS, publicMethodSignatures(type));

        Mod mod = type.getAnnotation(Mod.class);
        assertEquals("ForgeMicroblock", mod.modid());
        assertEquals("Forge Microblocks", mod.name());
        assertEquals("[1.7.10]", mod.acceptedMinecraftVersions());
        assertEquals("required-after:CodeChickenCore@[1.4.3,);required-after:ForgeMultipart", mod.dependencies());
        assertEquals(Tags.VERSION, mod.version());
        assertEquals("scala", mod.modLanguage());

        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertEquals(
                        EVENT_METHOD_NAMES.contains(method.getName()),
                        method.isAnnotationPresent(Mod.EventHandler.class),
                        method.toString());
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
