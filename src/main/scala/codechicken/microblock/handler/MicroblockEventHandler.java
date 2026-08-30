package codechicken.microblock.handler;

import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.TextureStitchEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MicroblockEventHandler {

    private MicroblockEventHandler() {}

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void postTextureStitch(TextureStitchEvent.Post event) {
        MicroblockEventHandler$.MODULE$.postTextureStitch(event);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void drawBlockHighlight(DrawBlockHighlightEvent event) {
        MicroblockEventHandler$.MODULE$.drawBlockHighlight(event);
    }
}
