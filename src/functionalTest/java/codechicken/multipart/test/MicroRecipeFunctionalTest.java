package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import codechicken.microblock.ItemMicroPart;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroRecipe;
import codechicken.microblock.Saw;
import codechicken.microblock.handler.MicroblockProxy;
import scala.Tuple3;

/**
 * Recipe behavior needs Forge startup because the item and material registries are initialized by the mod lifecycle.
 */
class MicroRecipeFunctionalTest {

    @Test
    void recipeMetadataMaterialLookupAndCreationRoundTrip() {
        int material = glassMaterial();
        ItemStack recipeOutput = MicroRecipe.getRecipeOutput();

        assertEquals(9, MicroRecipe.getRecipeSize());
        assertMicro(recipeOutput, 1, 0, 1, MicroMaterialRegistry.materialID("minecraft:stone"));

        ItemStack source = MicroMaterialRegistry.getMaterial(material).getItem();
        ItemStack fullBlock = MicroRecipe.create(3, 0, 8, material);
        assertNotSame(source, fullBlock);
        assertEquals(3, fullBlock.stackSize);
        assertEquals(material, MicroRecipe.findMaterial(fullBlock));
        assertEquals(0, MicroRecipe.microClass(fullBlock));
        assertEquals(8, MicroRecipe.microSize(fullBlock));

        ItemStack tagged = source.copy();
        tagged.setTagCompound((NBTTagCompound) recipeOutput.getTagCompound().copy());
        assertEquals(-1, MicroRecipe.findMaterial(tagged));
    }

    @Test
    void hollowRecipeWinsOverTheSimultaneouslyValidGluingRecipe() {
        int material = glassMaterial();
        InventoryCrafting crafting = grid();
        for (int slot = 0; slot < 9; slot++) {
            if (slot != 4) {
                crafting.setInventorySlotContents(slot, micro(0, 1, material));
            }
        }

        assertMicro(MicroRecipe.getHollowResult(crafting), 8, 1, 1, material);
        assertStack(MicroRecipe.getGluingResult(crafting), 1, 0, 8, material);
        assertMicro(MicroRecipe.getCraftingResult(crafting), 8, 1, 1, material);
        assertTrue(MicroRecipe.matches(crafting, null));
        assertFalse(MicroRecipe.matches(grid(), null));
    }

    @Test
    void gluingHandlesCoverHollowEdgeAndCornerForms() {
        int material = glassMaterial();

        assertMicro(MicroRecipe.getGluingResult(filled(2, 0, 1, material)), 1, 0, 2, material);
        assertMicro(MicroRecipe.getGluingResult(filled(2, 1, 1, material)), 1, 1, 2, material);
        assertMicro(MicroRecipe.getGluingResult(filled(2, 2, 1, material)), 1, 3, 1, material);
        assertMicro(MicroRecipe.getGluingResult(filled(4, 2, 1, material)), 1, 0, 1, material);
        assertMicro(MicroRecipe.getGluingResult(filled(2, 3, 1, material)), 1, 0, 1, material);
        assertStack(MicroRecipe.getGluingResult(filled(2, 0, 4, material)), 1, 0, 8, material);

        InventoryCrafting mixedEdges = grid();
        mixedEdges.setInventorySlotContents(0, micro(2, 1, material));
        mixedEdges.setInventorySlotContents(1, micro(2, 2, material));
        assertNull(MicroRecipe.getGluingResult(mixedEdges));
        assertNull(MicroRecipe.getGluingResult(filled(1, 0, 1, material)));
    }

    @Test
    void thinningCutsRawMaterialsAndMicroblocksVertically() {
        int material = glassMaterial();
        TestSaw firstSaw = new TestSaw(100);
        TestSaw laterSaw = new TestSaw(100);
        InventoryCrafting raw = grid();
        raw.setInventorySlotContents(0, new ItemStack(firstSaw));
        raw.setInventorySlotContents(3, MicroMaterialRegistry.getMaterial(material).getItem());
        raw.setInventorySlotContents(8, new ItemStack(laterSaw));

        Tuple3<Saw, Object, Object> found = MicroRecipe.getSaw(raw);
        assertSame(firstSaw, found._1());
        assertEquals(0, ((Number) found._2()).intValue());
        assertEquals(0, ((Number) found._3()).intValue());
        raw.setInventorySlotContents(8, null);

        assertStack(MicroRecipe.getThinningResult(raw), 2, 0, 4, material);
        assertStack(MicroRecipe.getCraftingResult(raw), 2, 0, 4, material);

        InventoryCrafting hollow = grid();
        hollow.setInventorySlotContents(1, new ItemStack(firstSaw));
        hollow.setInventorySlotContents(4, micro(1, 4, material));
        assertMicro(MicroRecipe.getThinningResult(hollow), 2, 1, 2, material);
        hollow.setInventorySlotContents(8, micro(0, 1, material));
        assertNull(MicroRecipe.getThinningResult(hollow));
    }

    @Test
    void splittingMapsEachSupportedClassHorizontally() {
        int material = glassMaterial();
        TestSaw saw = new TestSaw(100);
        int[][] mappings = { { 0, 3 }, { 1, 3 }, { 3, 2 } };

        for (int[] mapping : mappings) {
            InventoryCrafting crafting = grid();
            crafting.setInventorySlotContents(3, new ItemStack(saw));
            crafting.setInventorySlotContents(4, micro(mapping[0], 2, material));
            assertMicro(MicroRecipe.getSplittingResult(crafting), 2, mapping[1], 2, material);
            assertMicro(MicroRecipe.getCraftingResult(crafting), 2, mapping[1], 2, material);
        }

        InventoryCrafting unsupported = grid();
        unsupported.setInventorySlotContents(0, new ItemStack(saw));
        unsupported.setInventorySlotContents(1, micro(2, 1, material));
        assertNull(MicroRecipe.getSplittingResult(unsupported));
    }

    @Test
    void hollowFillTurnsExactlyOneHollowPartBackIntoACover() {
        int material = glassMaterial();
        InventoryCrafting crafting = grid();
        crafting.setInventorySlotContents(8, micro(1, 4, material));

        assertMicro(MicroRecipe.getHollowFillResult(crafting), 1, 0, 4, material);
        assertMicro(MicroRecipe.getCraftingResult(crafting), 1, 0, 4, material);

        crafting.setInventorySlotContents(0, micro(1, 4, material));
        assertNull(MicroRecipe.getHollowFillResult(crafting));
    }

    private static int glassMaterial() {
        return MicroRecipe.findMaterial(new ItemStack(Blocks.glass));
    }

    private static ItemStack micro(int microClass, int size, int material) {
        return ItemMicroPart.create(microClass << 8 | size, material);
    }

    private static InventoryCrafting filled(int count, int microClass, int size, int material) {
        InventoryCrafting crafting = grid();
        for (int slot = 0; slot < count; slot++) {
            crafting.setInventorySlotContents(slot, micro(microClass, size, material));
        }
        return crafting;
    }

    private static InventoryCrafting grid() {
        return new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
    }

    private static void assertMicro(ItemStack stack, int amount, int microClass, int size, int material) {
        assertSame(MicroblockProxy.itemMicro(), stack.getItem());
        assertStack(stack, amount, microClass, size, material);
    }

    private static void assertStack(ItemStack stack, int amount, int microClass, int size, int material) {
        assertEquals(amount, stack.stackSize);
        assertEquals(microClass, MicroRecipe.microClass(stack));
        assertEquals(size, MicroRecipe.microSize(stack));
        assertEquals(material, MicroRecipe.microMaterial(stack));
    }

    private static final class TestSaw extends Item implements Saw {

        private final int strength;

        private TestSaw(int strength) {
            this.strength = strength;
        }

        @Override
        public int getCuttingStrength(ItemStack item) {
            return strength;
        }
    }
}
