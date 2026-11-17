package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.multipart.ControlKeyModifer;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.handler.MultipartMod;
import codechicken.multipart.handler.MultipartMod$;
import codechicken.multipart.handler.MultipartProxy;
import codechicken.multipart.handler.MultipartSaveLoad;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

class MultipartModFunctionalTest {

    @Test
    void fmlUsesTheCompanionAndCompletesTheMultipartLifecycle() {
        ModContainer container = Loader.instance().getIndexedModList().get("ForgeMultipart");
        assertNotNull(container);
        assertSame(MultipartMod$.MODULE$, container.getMod());
        assertTrue(MultiPartRegistry.loaded());
        assertNotNull(MultipartProxy.block());
        assertNotNull(MultipartProxy.logger());
    }

    @Test
    void serverStoppedClearsTransientWorldAndControlState() {
        World originalWorld = MultipartSaveLoad.loadingWorld();
        Map<EntityPlayer, Boolean> controls = ControlKeyModifer.map();
        Map<EntityPlayer, Boolean> originalControls = new HashMap<>(controls);
        try {
            MultipartSaveLoad.loadingWorld_$eq(MinecraftServer.getServer().worldServers[0]);
            controls.put(null, Boolean.TRUE);

            MultipartMod.serverStopped(null);

            assertNull(MultipartSaveLoad.loadingWorld());
            assertTrue(controls.isEmpty());
        } finally {
            MultipartSaveLoad.loadingWorld_$eq(originalWorld);
            controls.clear();
            controls.putAll(originalControls);
        }
    }
}
