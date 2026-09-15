package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import org.junit.jupiter.api.Test;

import codechicken.microblock.handler.MicroblockEventHandler;
import codechicken.microblock.handler.MicroblockEventHandler$;
import cpw.mods.fml.common.eventhandler.EventBus;

class MicroblockEventHandlerFunctionalTest {

    @Test
    void dedicatedServerRegistersTheCompanionAfterStrippingClientHandlers() throws Exception {
        assertEquals(0, MicroblockEventHandler.class.getDeclaredMethods().length);
        assertEquals(0, MicroblockEventHandler$.class.getDeclaredMethods().length);
        assertTrue(listenerOwners(MinecraftForge.EVENT_BUS).containsKey(MicroblockEventHandler$.MODULE$));
    }

    private static Map<?, ?> listenerOwners(EventBus bus) throws Exception {
        Field ownersField = EventBus.class.getDeclaredField("listenerOwners");
        ownersField.setAccessible(true);
        return (Map<?, ?>) ownersField.get(bus);
    }
}
