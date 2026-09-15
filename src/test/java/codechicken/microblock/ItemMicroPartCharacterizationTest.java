package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.IItemRenderer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.microblock.handler.MicroblockProxy;

class ItemMicroPartCharacterizationTest {

    private static final String MATERIAL = "test:item_micro_part";
    private static final int INVALID_CLASS_DAMAGE = 255 << 8 | 1;

    private static final Set<String> ITEM_METHODS = new TreeSet<>(
            Arrays.asList(
                    "getItemStackDisplayName(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;",
                    "getSubItems(Lnet/minecraft/item/Item;Lnet/minecraft/creativetab/CreativeTabs;Ljava/util/List;)V",
                    "onItemUse(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z",
                    "registerIcons(Lnet/minecraft/client/renderer/texture/IIconRegister;)V"));
    private static final Set<String> HELPERS = new TreeSet<>(
            Arrays.asList(
                    "checkTagCompound(Lnet/minecraft/item/ItemStack;)V",
                    "create(II)Lnet/minecraft/item/ItemStack;",
                    "create(IILjava/lang/String;)Lnet/minecraft/item/ItemStack;",
                    "create(ILjava/lang/String;)Lnet/minecraft/item/ItemStack;",
                    "getMaterial(Lnet/minecraft/item/ItemStack;)Lcodechicken/microblock/MicroMaterialRegistry$IMicroMaterial;",
                    "getMaterialID(Lnet/minecraft/item/ItemStack;)I"));
    private static final Set<String> RENDERER = new TreeSet<>(
            Arrays.asList(
                    "handleRenderType(Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;)Z",
                    "renderHighlight(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/MovingObjectPosition;)Z",
                    "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V",
                    "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;Lscala/collection/Seq;)V",
                    "shouldUseRenderHelper(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRendererHelper;)Z"));

    @BeforeAll
    static void registerMaterial() {
        if (MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
        }
        if (MicroMaterialRegistry.getMaterial(MATERIAL) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MATERIAL);
        }
        MicroMaterialRegistry$.MODULE$.setupIDMap();
    }

    @Test
    void keepsTheItemCompanionAndRendererSurfaces() throws Exception {
        assertEquals(ITEM_METHODS, publicDeclaredMethods(ItemMicroPart.class, false));
        assertEquals(HELPERS, publicDeclaredMethods(ItemMicroPart.class, true));
        assertEquals(HELPERS, publicDeclaredMethods(ItemMicroPart$.class, false));
        assertEquals(RENDERER, publicDeclaredMethods(ItemMicroPartRenderer$.class, false));

        Set<String> facade = new TreeSet<>(RENDERER);
        facade.remove(
                "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V");
        assertEquals(facade, publicDeclaredMethods(ItemMicroPartRenderer.class, true));
        assertArrayEquals(new Class<?>[] { IItemRenderer.class }, ItemMicroPartRenderer$.class.getInterfaces());

        Field itemModule = ItemMicroPart$.class.getField("MODULE$");
        assertTrue(Modifier.isStatic(itemModule.getModifiers()));
        assertTrue(Modifier.isFinal(itemModule.getModifiers()));
        assertSame(ItemMicroPart$.MODULE$, itemModule.get(null));

        Field rendererModule = ItemMicroPartRenderer$.class.getField("MODULE$");
        assertTrue(Modifier.isStatic(rendererModule.getModifiers()));
        assertTrue(Modifier.isFinal(rendererModule.getModifiers()));
        assertSame(ItemMicroPartRenderer$.MODULE$, rendererModule.get(null));

        ItemMicroPartRenderer$.class
                .getDeclaredMethod("renderItem", IItemRenderer.ItemRenderType.class, ItemStack.class, Object[].class);
    }

    @Test
    void constructorSetsTheStableItemIdentity() {
        ItemMicroPart item = new ItemMicroPart();

        assertEquals("item.microblock", item.getUnlocalizedName());
        assertTrue(item.getHasSubtypes());
    }

    @Test
    void tagHelpersCreatePreserveAndResolveMaterialTags() {
        ItemMicroPart item = new ItemMicroPart();
        ItemStack blank = new ItemStack(item, 1, INVALID_CLASS_DAMAGE);
        assertFalse(blank.hasTagCompound());

        assertNull(ItemMicroPart.getMaterial(blank));
        assertTrue(blank.hasTagCompound());
        NBTTagCompound original = blank.getTagCompound();
        ItemMicroPart.checkTagCompound(blank);
        assertSame(original, blank.getTagCompound());
        assertEquals(MicroMaterialRegistry.getMissingId(), ItemMicroPart.getMaterialID(blank));

        blank.getTagCompound().setString("mat", "test:no_such_material");
        assertSame(MissingMicroMaterial$.MODULE$, ItemMicroPart.getMaterial(blank));

        blank.getTagCompound().setString("mat", MATERIAL);
        assertSame(MicroMaterialRegistry.getMaterial(MATERIAL), ItemMicroPart.getMaterial(blank));
        assertEquals(MicroMaterialRegistry.materialID(MATERIAL), ItemMicroPart.getMaterialID(blank));
    }

    @Test
    void creationOverloadsPreserveItemAmountDamageAndMaterialName() {
        ItemMicroPart previous = MicroblockProxy.itemMicro();
        ItemMicroPart item = new ItemMicroPart();
        MicroblockProxy.itemMicro_$eq(item);
        try {
            ItemStack named = ItemMicroPart.create(3, 0x402, MATERIAL);
            assertSame(item, named.getItem());
            assertEquals(3, named.stackSize);
            assertEquals(0x402, named.getItemDamage());
            assertEquals(MATERIAL, named.getTagCompound().getString("mat"));

            int material = MicroMaterialRegistry.materialID(MATERIAL);
            ItemStack numbered = ItemMicroPart.create(0x201, material);
            assertEquals(1, numbered.stackSize);
            assertEquals(0x201, numbered.getItemDamage());
            assertEquals(MATERIAL, numbered.getTagCompound().getString("mat"));
        } finally {
            MicroblockProxy.itemMicro_$eq(previous);
        }
    }

    @Test
    void invalidMicroClassShortCircuitsNamesPlacementAndRendering() {
        ItemMicroPart item = new ItemMicroPart();
        ItemStack stack = new ItemStack(item, 1, INVALID_CLASS_DAMAGE);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("mat", MATERIAL);

        assertEquals("Unnamed", item.getItemStackDisplayName(stack));
        assertFalse(item.onItemUse(stack, null, null, 0, 0, 0, 0, 0, 0, 0));
        assertTrue(ItemMicroPartRenderer.handleRenderType(stack, IItemRenderer.ItemRenderType.INVENTORY));
        assertTrue(
                ItemMicroPartRenderer.shouldUseRenderHelper(
                        IItemRenderer.ItemRenderType.INVENTORY,
                        stack,
                        IItemRenderer.ItemRendererHelper.INVENTORY_BLOCK));
        assertFalse(ItemMicroPartRenderer.renderHighlight(null, stack, null));
        assertDoesNotThrow(
                () -> ((IItemRenderer) ItemMicroPartRenderer$.MODULE$)
                        .renderItem(IItemRenderer.ItemRenderType.INVENTORY, stack, new Object[0]));
    }

    private static Set<String> publicDeclaredMethods(Class<?> type, boolean staticMethods) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()) == staticMethods) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
