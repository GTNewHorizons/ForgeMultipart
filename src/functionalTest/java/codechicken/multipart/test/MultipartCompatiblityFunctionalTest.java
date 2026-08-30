package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.multipart.handler.MCPCCompatModule;
import codechicken.multipart.handler.MultipartCompatiblity;
import cpw.mods.fml.common.FMLCommonHandler;
import scala.Function4;
import scala.runtime.AbstractFunction4;

class MultipartCompatiblityFunctionalTest {

    @Test
    void loadLeavesTheCallbackUntouchedOutsideMcpc() {
        assertFalse(FMLCommonHandler.instance().getModName().toLowerCase(Locale.ROOT).contains("mcpc"));
        Function4<World, Object, Object, Object, Object> original = MultipartCompatiblity.canAddPart();
        Function4<World, Object, Object, Object, Object> replacement = new DenyPlacement();
        try {
            MultipartCompatiblity.canAddPart_$eq(replacement);
            MultipartCompatiblity.load();
            assertSame(replacement, MultipartCompatiblity.canAddPart());
        } finally {
            MultipartCompatiblity.canAddPart_$eq(original);
        }
    }

    @Test
    void missingMcpcHookIsLoggedAndLeavesTheCallbackUntouched() {
        assertThrows(
                NoSuchMethodException.class,
                () -> World.class.getDeclaredMethod("canPlaceMultipart", Block.class, int.class, int.class, int.class));
        Function4<World, Object, Object, Object, Object> original = MultipartCompatiblity.canAddPart();
        Function4<World, Object, Object, Object, Object> replacement = new DenyPlacement();
        try {
            MultipartCompatiblity.canAddPart_$eq(replacement);
            assertDoesNotThrow(MCPCCompatModule::load);
            assertSame(replacement, MultipartCompatiblity.canAddPart());
        } finally {
            MultipartCompatiblity.canAddPart_$eq(original);
        }
    }

    private static final class DenyPlacement extends AbstractFunction4<World, Object, Object, Object, Object> {

        @Override
        public Object apply(World world, Object x, Object y, Object z) {
            return Boolean.FALSE;
        }
    }
}
