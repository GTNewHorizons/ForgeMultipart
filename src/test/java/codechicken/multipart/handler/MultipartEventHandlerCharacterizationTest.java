package codechicken.multipart.handler;

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

class MultipartEventHandlerCharacterizationTest {

    private static final Set<String> EVENT_METHODS = new TreeSet<>(
            Arrays.asList(
                    "chunkUnWatch(Lnet/minecraftforge/event/world/ChunkWatchEvent$UnWatch;)V",
                    "chunkWatch(Lnet/minecraftforge/event/world/ChunkWatchEvent$Watch;)V",
                    "drawBlockHighlight(Lnet/minecraftforge/client/event/DrawBlockHighlightEvent;)V",
                    "serverTick(Lcpw/mods/fml/common/gameevent/TickEvent$ServerTickEvent;)V",
                    "tileEntityLoad(Lnet/minecraftforge/event/world/ChunkDataEvent$Load;)V",
                    "worldUnLoad(Lnet/minecraftforge/event/world/WorldEvent$Unload;)V"));

    @Test
    void keepsBothSingletonTypesAndEventDescriptors() throws Exception {
        assertHandlerType(MultipartEventHandler.class, true);
        assertHandlerType(MultipartEventHandler$.class, false);

        assertEquals(0, MultipartEventHandler.class.getFields().length);
        Field module = MultipartEventHandler$.class.getField("MODULE$");
        assertSame(MultipartEventHandler$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MultipartEventHandler$.MODULE$, module.get(null));
    }

    @Test
    void keepsEventPrioritiesAndClientOnlyHighlight() {
        assertEventAnnotations(MultipartEventHandler.class);
        assertEventAnnotations(MultipartEventHandler$.class);
    }

    private static void assertHandlerType(Class<?> type, boolean staticMethods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
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
            assertEquals(
                    method.getName().equals("tileEntityLoad") ? EventPriority.HIGHEST : EventPriority.NORMAL,
                    subscribe.priority(),
                    method.toString());
            assertFalse(subscribe.receiveCanceled(), method.toString());

            SideOnly sideOnly = method.getAnnotation(SideOnly.class);
            if (method.getName().equals("drawBlockHighlight")) {
                assertNotNull(sideOnly, method.toString());
                assertEquals(Side.CLIENT, sideOnly.value());
            } else {
                assertNull(sideOnly, method.toString());
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
