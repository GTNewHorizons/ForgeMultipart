package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import net.minecraft.server.MinecraftServer;

import org.junit.jupiter.api.Test;

import codechicken.multipart.TileMultipart;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.scalatraits.TSlottedTile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import scala.collection.immutable.Nil$;

class ForgeEnvironmentSmokeTest {

    @Test
    void reachesServerStartedWithWorldAndMultipartModsLoaded() {
        assertTrue(ForgeMultipartFunctionalTestMod.preInitialized);
        assertTrue(ForgeMultipartFunctionalTestMod.initialized);
        assertTrue(ForgeMultipartFunctionalTestMod.postInitialized);
        assertTrue(ForgeMultipartFunctionalTestMod.serverAboutToStart);
        assertTrue(ForgeMultipartFunctionalTestMod.serverStarted);
        assertTrue(FMLCommonHandler.instance().getSide().isServer());
        assertTrue(Loader.isModLoaded("ForgeMultipart"));
        assertTrue(Loader.isModLoaded("ForgeMicroblock"));
        assertTrue(Loader.isModLoaded("McMultipart"));

        MinecraftServer server = MinecraftServer.getServer();
        assertNotNull(server);
        assertNotNull(server.worldServers);
        assertTrue(server.worldServers.length > 0);
        assertNotNull(server.worldServers[0]);
    }

    @Test
    void generatesAndCachesSlottedTileClass() {
        int traitId = MultipartMixinFactory.getId(TSlottedTile.class.getName().replace('.', '/'));
        assertFalse(traitId < 0);

        BitSet traits = new BitSet();
        traits.set(traitId);
        Object first = MultipartMixinFactory.construct(traits, Nil$.MODULE$);
        Object second = MultipartMixinFactory.construct(traits, Nil$.MODULE$);

        assertTrue(first instanceof TileMultipart);
        assertTrue(first instanceof TSlottedTile);
        assertNotSame(first, second);
        assertSame(first.getClass(), second.getClass());
        assertEquals(27, ((TSlottedTile) first).v_partMap().length);
    }
}
