package codechicken.microblock;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import codechicken.lib.render.CCModel;
import scala.collection.Seq;

public final class ItemSawRenderer {

    private ItemSawRenderer() {}

    public static Map<String, CCModel> models() {
        return ItemSawRenderer$.MODULE$.models();
    }

    public static CCModel handle() {
        return ItemSawRenderer$.MODULE$.handle();
    }

    public static CCModel holder() {
        return ItemSawRenderer$.MODULE$.holder();
    }

    public static CCModel blade() {
        return ItemSawRenderer$.MODULE$.blade();
    }

    public static boolean handleRenderType(ItemStack item, ItemRenderType renderType) {
        return ItemSawRenderer$.MODULE$.handleRenderType(item, renderType);
    }

    public static boolean shouldUseRenderHelper(ItemRenderType renderType, ItemStack item, ItemRendererHelper helper) {
        return ItemSawRenderer$.MODULE$.shouldUseRenderHelper(renderType, item, helper);
    }

    public static void renderItem(ItemRenderType renderType, ItemStack item, Seq<Object> data) {
        ItemSawRenderer$.MODULE$.renderItem(renderType, item, data);
    }
}
