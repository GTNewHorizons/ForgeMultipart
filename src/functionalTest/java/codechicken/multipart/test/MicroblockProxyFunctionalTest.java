package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import org.junit.jupiter.api.Test;

import codechicken.microblock.MicroRecipe$;
import codechicken.microblock.handler.MicroblockProxy;
import codechicken.microblock.handler.MicroblockProxy$;
import codechicken.microblock.handler.MicroblockProxy_clientImpl;
import codechicken.microblock.handler.MicroblockProxy_serverImpl;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import scala.collection.Iterator;

class MicroblockProxyFunctionalTest {

    @Test
    void dedicatedServerStripsClientLifecycleAndResolvesTheCompanionToServerMethods() throws Exception {
        assertSame(MicroblockProxy$.MODULE$, MicroblockProxy$.class.getField("MODULE$").get(null));
        assertThrows(NoSuchMethodError.class, () -> MicroblockProxy.init());
        assertThrows(NoSuchMethodError.class, () -> MicroblockProxy.postInit());
        assertThrows(NoClassDefFoundError.class, () -> MicroblockProxy_clientImpl.class.getDeclaredMethods());

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle init = lookup.findVirtual(MicroblockProxy$.class, "init", MethodType.methodType(void.class));
        MethodHandle postInit = lookup
                .findVirtual(MicroblockProxy$.class, "postInit", MethodType.methodType(void.class));
        assertSame(MicroblockProxy_serverImpl.class, lookup.revealDirect(init).getDeclaringClass());
        assertSame(MicroblockProxy_serverImpl.class, lookup.revealDirect(postInit).getDeclaringClass());

        assertNotNull(MicroblockProxy.itemMicro());
        assertNotNull(MicroblockProxy.sawStone());
        assertNotNull(MicroblockProxy.sawIron());
        assertNotNull(MicroblockProxy.sawDiamond());
        assertNotNull(MicroblockProxy.stoneRod());
        assertEquals(3, MicroblockProxy$.MODULE$.saws().size());
    }

    @Test
    void registersItemsOreSawOrderAndRecipes() {
        assertFalse(Loader.isModLoaded("dreamcraft"));
        assertSame(MicroblockProxy.itemMicro(), GameRegistry.findItem("ForgeMicroblock", "microblock"));
        assertSame(MicroblockProxy.sawStone(), GameRegistry.findItem("ForgeMicroblock", "sawStone"));
        assertSame(MicroblockProxy.sawIron(), GameRegistry.findItem("ForgeMicroblock", "sawIron"));
        assertSame(MicroblockProxy.sawDiamond(), GameRegistry.findItem("ForgeMicroblock", "sawDiamond"));
        assertSame(MicroblockProxy.stoneRod(), GameRegistry.findItem("ForgeMicroblock", "stoneRod"));

        assertTrue(containsItem(OreDictionary.getOres("rodStone"), MicroblockProxy.stoneRod()));

        Iterator<Item> saws = MicroblockProxy$.MODULE$.saws().iterator();
        assertSame(MicroblockProxy.sawStone(), saws.next());
        assertSame(MicroblockProxy.sawIron(), saws.next());
        assertSame(MicroblockProxy.sawDiamond(), saws.next());
        assertFalse(saws.hasNext());

        @SuppressWarnings("unchecked")
        List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
        int microRecipe = recipes.indexOf(MicroRecipe$.MODULE$);
        int stoneRod = recipeIndex(recipes, MicroblockProxy.stoneRod(), 4);
        int sawStone = recipeIndex(recipes, MicroblockProxy.sawStone(), 1);
        int sawIron = recipeIndex(recipes, MicroblockProxy.sawIron(), 1);
        int sawDiamond = recipeIndex(recipes, MicroblockProxy.sawDiamond(), 1);
        assertTrue(microRecipe >= 0);
        assertTrue(stoneRod >= 0);
        assertTrue(sawStone >= 0);
        assertTrue(sawIron >= 0);
        assertTrue(sawDiamond >= 0);
    }

    private static boolean containsItem(List<ItemStack> stacks, Item item) {
        for (ItemStack stack : stacks) {
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static int recipeIndex(List<IRecipe> recipes, Item item, int stackSize) {
        for (int index = 0; index < recipes.size(); index++) {
            ItemStack output = recipes.get(index).getRecipeOutput();
            if (output != null && output.getItem() == item && output.stackSize == stackSize) {
                return index;
            }
        }
        return -1;
    }
}
