package codechicken.microblock.handler;

import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.TextureStitchEvent;

import org.lwjgl.opengl.GL11;

import codechicken.lib.render.RenderUtils;
import codechicken.microblock.ItemMicroPartRenderer$;
import codechicken.microblock.MicroMaterialRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MicroblockEventHandler$ {

    public static final MicroblockEventHandler$ MODULE$ = new MicroblockEventHandler$();

    private MicroblockEventHandler$() {}

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void postTextureStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() == 0) {
            MicroMaterialRegistry.loadIcons();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void drawBlockHighlight(DrawBlockHighlightEvent event) {
        if (event.currentItem != null && event.currentItem.getItem() == MicroblockProxy$.MODULE$.itemMicro()
                && event.target != null
                && event.target.typeOfHit == MovingObjectType.BLOCK) {
            GL11.glPushMatrix();
            RenderUtils.translateToWorldCoords(event.player, event.partialTicks);
            if (ItemMicroPartRenderer$.MODULE$.renderHighlight(event.player, event.currentItem, event.target)) {
                event.setCanceled(true);
            }
            GL11.glPopMatrix();
        }
    }
}
