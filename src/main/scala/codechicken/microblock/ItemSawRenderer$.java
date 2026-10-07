package codechicken.microblock;

import java.util.Map;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.lwjgl.opengl.GL11;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.render.uv.UVTranslation;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.SwapYZ;
import codechicken.lib.vec.TransformationList;
import codechicken.lib.vec.Translation;
import codechicken.microblock.handler.MicroblockProxy;
import scala.Predef$;
import scala.collection.Seq;

public final class ItemSawRenderer$ implements IItemRenderer {

    public static final ItemSawRenderer$ MODULE$ = new ItemSawRenderer$();

    private final Map<String, CCModel> models = CCModel
            .parseObjModels(new ResourceLocation("microblock", "models/saw.obj"), 7, new SwapYZ());
    private final CCModel handle = models.get("Handle");
    private final CCModel holder = models.get("BladeSupport");
    private final CCModel blade = models.get("Blade");

    private ItemSawRenderer$() {}

    public Map<String, CCModel> models() {
        return models;
    }

    public CCModel handle() {
        return handle;
    }

    public CCModel holder() {
        return holder;
    }

    public CCModel blade() {
        return blade;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType renderType) {
        return !MicroblockProxy.useSawIcons()
                || TextureUtils.isMissing(item.getIconIndex(), TextureMap.locationItemsTexture);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType renderType, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void renderItem(ItemRenderType renderType, ItemStack item, Object[] data) {
        renderItem(renderType, item, (Seq<Object>) (Seq<?>) Predef$.MODULE$.wrapRefArray(data));
    }

    public void renderItem(ItemRenderType renderType, ItemStack item, Seq<Object> data) {
        TransformationList transformation;
        if (renderType == ItemRenderType.INVENTORY) {
            transformation = new TransformationList(
                    new Scale(1.8),
                    new Translation(0, 0, -0.6),
                    new Rotation(-Math.PI / 4, 1, 0, 0),
                    new Rotation(Math.PI * 3 / 4, 0, 1, 0));
        } else if (renderType == ItemRenderType.ENTITY) {
            transformation = new TransformationList(
                    new Scale(1),
                    new Translation(0, 0, -0.25),
                    new Rotation(-Math.PI / 4, 1, 0, 0));
        } else if (renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            transformation = new TransformationList(
                    new Scale(1.5),
                    new Rotation(-Math.PI / 3, 1, 0, 0),
                    new Rotation(Math.PI * 3 / 4, 0, 1, 0),
                    new Translation(0.5, 0.5, 0.5));
        } else if (renderType == ItemRenderType.EQUIPPED) {
            transformation = new TransformationList(
                    new Scale(1.5),
                    new Rotation(-Math.PI / 5, 1, 0, 0),
                    new Rotation(-Math.PI * 3 / 4, 0, 1, 0),
                    new Translation(0.75, 0.5, 0.75));
        } else {
            return;
        }

        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.useNormals = true;
        state.pullLightmapInstance();
        CCRenderState.changeTexture("microblock:textures/items/saw.png");
        state.startDrawingInstance();
        handle.render(transformation);
        holder.render(transformation);
        state.drawInstance();
        GL11.glDisable(GL11.GL_CULL_FACE);
        state.startDrawingInstance();
        blade.render(
                transformation,
                new UVTranslation(0, (((Saw) item.getItem()).getCuttingStrength(item) - 1) * 4 / 64D));
        state.drawInstance();
        GL11.glEnable(GL11.GL_CULL_FACE);
    }
}
