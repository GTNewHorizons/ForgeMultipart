package codechicken.microblock.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

class MicroblockEventHandlerCharacterizationTest {

    private static final Set<String> EVENT_METHODS = new TreeSet<>(
            Arrays.asList(
                    "drawBlockHighlight(Lnet/minecraftforge/client/event/DrawBlockHighlightEvent;)V",
                    "postTextureStitch(Lnet/minecraftforge/client/event/TextureStitchEvent$Post;)V"));

    @Test
    void keepsBothSingletonTypesAndEventDescriptors() throws Exception {
        assertHandlerType(MicroblockEventHandler.class, true);
        assertHandlerType(MicroblockEventHandler$.class, false);

        assertEquals(0, MicroblockEventHandler.class.getFields().length);
        Field module = MicroblockEventHandler$.class.getField("MODULE$");
        assertSame(MicroblockEventHandler$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MicroblockEventHandler$.MODULE$, module.get(null));
    }

    @Test
    void keepsBothClientOnlyEventAnnotations() {
        assertEventAnnotations(MicroblockEventHandler.class);
        assertEventAnnotations(MicroblockEventHandler$.class);
    }

    private static void assertHandlerType(Class<?> type, boolean staticMethods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertNull(type.getAnnotation(SideOnly.class));
        assertEquals(EVENT_METHODS, publicMethodSignatures(type));

        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertEquals(staticMethods, Modifier.isStatic(method.getModifiers()));
            }
        }
    }

    private static void assertEventAnnotations(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            SubscribeEvent subscribe = method.getAnnotation(SubscribeEvent.class);
            assertNotNull(subscribe, method.toString());
            assertEquals(EventPriority.NORMAL, subscribe.priority(), method.toString());
            assertFalse(subscribe.receiveCanceled(), method.toString());

            SideOnly sideOnly = method.getAnnotation(SideOnly.class);
            assertNotNull(sideOnly, method.toString());
            assertEquals(Side.CLIENT, sideOnly.value(), method.toString());
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
