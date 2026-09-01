package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.lwjgl.opengl.GL11;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.TextureUtils;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import scala.collection.Seq;

/** Scala-compatible renderer singleton retained for registration and compiled consumers. */
public final class ItemMicroPartRenderer$ implements IItemRenderer {

    public static final ItemMicroPartRenderer$ MODULE$ = new ItemMicroPartRenderer$();

    private ItemMicroPartRenderer$() {}

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        renderItemPart(type, item);
    }

    public void renderItem(ItemRenderType type, ItemStack item, Seq<Object> data) {
        renderItemPart(type, item);
    }

    private void renderItemPart(ItemRenderType type, ItemStack item) {
        IMicroMaterial material = ItemMicroPart.getMaterial(item);
        CommonMicroClass mcrClass = CommonMicroClass.getMicroClass(item.getItemDamage());
        int size = item.getItemDamage() & 0xFF;
        if (material == null || mcrClass == null) {
            return;
        }

        GL11.glPushMatrix();
        if (type == ItemRenderType.ENTITY) {
            GL11.glScaled(0.5, 0.5, 0.5);
        }
        if (type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY) {
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        }
        TextureUtils.bindAtlas(0);
        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.useNormals = true;
        state.pullLightmapInstance();
        state.startDrawingInstance();
        Microblock part = mcrClass.create(true, ItemMicroPart.getMaterialID(item));
        MicroblockRender.renderItem(part, size, mcrClass.itemSlot());
        state.drawInstance();
        GL11.glPopMatrix();
    }

    public boolean renderHighlight(EntityPlayer player, ItemStack stack, MovingObjectPosition hit) {
        int material = ItemMicroPart.getMaterialID(stack);
        CommonMicroClass mcrClass = CommonMicroClass.getMicroClass(stack.getItemDamage());
        int size = stack.getItemDamage() & 0xFF;
        if (material < 0 || mcrClass == null) {
            return false;
        }
        return MicroMaterialRegistry.renderHighlight(player, hit, mcrClass, size, material);
    }
}
