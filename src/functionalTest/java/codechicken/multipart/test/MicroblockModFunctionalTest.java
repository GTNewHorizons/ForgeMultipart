package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.handler.MicroblockMod;
import codechicken.microblock.handler.MicroblockMod$;
import codechicken.microblock.handler.MicroblockProxy;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

class MicroblockModFunctionalTest {

    @Test
    void fmlUsesTheCompanionAndCompletesTheMicroblockLifecycle() {
        ModContainer container = Loader.instance().getIndexedModList().get("ForgeMicroblock");
        assertNotNull(container);
        assertSame(MicroblockMod$.MODULE$, container.getMod());

        assertNotNull(MicroblockProxy.logger());
        assertNotNull(MicroblockProxy.itemMicro());
        assertNotNull(MicroblockProxy.sawStone());
        assertNotNull(MicroblockProxy.sawIron());
        assertNotNull(MicroblockProxy.sawDiamond());
        assertNotNull(MicroblockProxy.stoneRod());
        assertNotNull(MicroMaterialRegistry.getIdMap());
        assertTrue(MicroMaterialRegistry.getIdMap().length > 0);
        assertNotNull(MicroMaterialRegistry.getMaterial(MicroMaterialRegistry.getMissingId()));
        assertTrue(MicroMaterialRegistry.getMaxCuttingStrength() >= 3);
        assertNull(MicroblockMod.angelicaCompat());
    }
}
