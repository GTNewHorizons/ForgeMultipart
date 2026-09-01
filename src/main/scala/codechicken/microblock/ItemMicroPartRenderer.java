package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import scala.collection.Seq;

public final class ItemMicroPartRenderer {

    private ItemMicroPartRenderer() {}

    public static boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return ItemMicroPartRenderer$.MODULE$.handleRenderType(item, type);
    }

    public static boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return ItemMicroPartRenderer$.MODULE$.shouldUseRenderHelper(type, item, helper);
    }

    public static void renderItem(ItemRenderType type, ItemStack item, Seq<Object> data) {
        ItemMicroPartRenderer$.MODULE$.renderItem(type, item, data);
    }

    public static boolean renderHighlight(EntityPlayer player, ItemStack stack, MovingObjectPosition hit) {
        return ItemMicroPartRenderer$.MODULE$.renderHighlight(player, stack, hit);
    }
}
