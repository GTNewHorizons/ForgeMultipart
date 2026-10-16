package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class MicroRecipeBinaryCompatibilityTest {

    @Test
    void staticFacadeAndCompanionKeepTheirCompletePublishedShape() throws Exception {
        Set<String> expected = new TreeSet<>(
                Arrays.asList(
                        "canCut(codechicken.microblock.Saw,net.minecraft.item.ItemStack,int)boolean",
                        "create(int,int,int,int)net.minecraft.item.ItemStack",
                        "findMaterial(net.minecraft.item.ItemStack)int",
                        "getCraftingResult(net.minecraft.inventory.InventoryCrafting)net.minecraft.item.ItemStack",
                        "getGluingResult(net.minecraft.inventory.InventoryCrafting)net.minecraft.item.ItemStack",
                        "getHollowFillResult(net.minecraft.inventory.InventoryCrafting)net.minecraft.item.ItemStack",
                        "getHollowResult(net.minecraft.inventory.InventoryCrafting)net.minecraft.item.ItemStack",
                        "getRecipeOutput()net.minecraft.item.ItemStack",
                        "getRecipeSize()int",
                        "getSaw(net.minecraft.inventory.InventoryCrafting)scala.Tuple3",
                        "getSplittingResult(net.minecraft.inventory.InventoryCrafting)net.minecraft.item.ItemStack",
                        "getThinningResult(net.minecraft.inventory.InventoryCrafting)net.minecraft.item.ItemStack",
                        "matches(net.minecraft.inventory.InventoryCrafting,net.minecraft.world.World)boolean",
                        "microClass(net.minecraft.item.ItemStack)int",
                        "microMaterial(net.minecraft.item.ItemStack)int",
                        "microSize(net.minecraft.item.ItemStack)int",
                        "splitMap()scala.collection.immutable.Map"));

        assertEquals(expected, publicSignatures(MicroRecipe.class, true));
        assertEquals(expected, publicSignatures(MicroRecipe$.class, false));
        assertTrue(Modifier.isFinal(MicroRecipe.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroRecipe$.class.getModifiers()));

        Field module = MicroRecipe$.class.getField("MODULE$");
        assertSame(MicroRecipe$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MicroRecipe$.MODULE$, module.get(null));
    }

    @Test
    void splittingMapKeepsScalaTypeValuesAndFacadeIdentity() {
        assertSame(MicroRecipe$.MODULE$.splitMap(), MicroRecipe.splitMap());
        assertEquals(3, ((Number) MicroRecipe.splitMap().apply(0)).intValue());
        assertEquals(3, ((Number) MicroRecipe.splitMap().apply(1)).intValue());
        assertEquals(2, ((Number) MicroRecipe.splitMap().apply(3)).intValue());
        assertEquals(3, MicroRecipe.splitMap().size());
    }

    private static Set<String> publicSignatures(Class<?> type, boolean requireStatic) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertEquals(requireStatic, Modifier.isStatic(method.getModifiers()), method.toString());
            signatures.add(signature(method));
        }
        return signatures;
    }

    private static String signature(Method method) {
        StringBuilder out = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(parameters[i].getName());
        }
        return out.append(')').append(method.getReturnType().getName()).toString();
    }
}
