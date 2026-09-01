package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.junit.jupiter.api.Test;

import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.MicroMaterialRegistry;

class BlockMicroMaterialFunctionalTest {

    @Test
    void dedicatedServerKeepsCommonMaterialBehaviorAndStripsClientMembers() throws Exception {
        BlockMicroMaterial material = (BlockMicroMaterial) MicroMaterialRegistry.getMaterial("minecraft:stone");
        assertSame(Blocks.stone, material.block());
        assertEquals(0, material.meta());
        assertEquals("minecraft:stone", material.blockKey());

        ItemStack item = material.getItem();
        assertSame(Item.getItemFromBlock(Blocks.stone), item.getItem());
        assertEquals(1, item.stackSize);
        assertEquals(0, item.getItemDamage());
        assertEquals(item.getDisplayName(), material.getLocalizedName());
        assertFalse(material.isTransparent());
        assertTrue(new BlockMicroMaterial(Blocks.glass, 0).isTransparent());
        assertEquals(Blocks.glowstone.getLightValue(), new BlockMicroMaterial(Blocks.glowstone, 0).getLightValue());
        assertEquals(Blocks.stone.getHarvestLevel(0), material.getCutterStrength());
        assertSame(Blocks.stone.stepSound, material.getSound());
        assertEquals(Blocks.stone.getExplosionResistance(null), material.explosionResistance(null));
        WorldServer world = MinecraftServer.getServer().worldServers[0];
        EntityPlayer player = FakePlayerFactory.getMinecraft(world);
        float hardness = Blocks.stone.getBlockHardness(null, 0, 0, 0);
        float expected = player.getBreakSpeed(Blocks.stone, false, 0, 0, -1, 0) / hardness;
        assertEquals(expected, material.getStrength(player));

        assertThrows(NoSuchFieldException.class, () -> BlockMicroMaterial.class.getDeclaredField("icont"));
        assertThrows(NoSuchMethodException.class, () -> BlockMicroMaterial.class.getDeclaredMethod("loadIcons"));
        assertThrows(
                NoSuchMethodException.class,
                () -> BlockMicroMaterial.class.getDeclaredMethod("getBreakingIcon", int.class));
        assertSame(Blocks.stone, BlockMicroMaterial.class.getDeclaredMethod("block").invoke(material));
    }
}
