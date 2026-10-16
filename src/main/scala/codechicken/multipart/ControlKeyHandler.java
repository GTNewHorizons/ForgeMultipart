package codechicken.multipart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import codechicken.lib.packet.PacketCustom;
import codechicken.multipart.handler.MultipartCPH;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Key handler implementation. Client only: it extends a client class, so it must never be loaded on a server.
 * <p>
 * The reference was a Scala object, which is a singleton reached as {@code ControlKeyHandler$.MODULE$}. Java has no
 * object sugar, so the single instance both the key registry and the event bus need is {@link #INSTANCE}.
 */
@SideOnly(Side.CLIENT)
public class ControlKeyHandler extends KeyBinding {

    public static final ControlKeyHandler INSTANCE = new ControlKeyHandler();

    private boolean wasPressed;

    private ControlKeyHandler() {
        super("key.control", Keyboard.KEY_NONE, "Forge Multipart");
    }

    public boolean wasPressed() {
        return wasPressed;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void tick(ClientTickEvent event) {
        boolean pressed = getIsKeyPressed();
        if (pressed != wasPressed) {
            wasPressed = pressed;
            if (Minecraft.getMinecraft().getNetHandler() != null) {
                ControlKeyModifer.setClientPressing(pressed);
                PacketCustom packet = new PacketCustom(MultipartCPH.channel(), 1);
                packet.writeBoolean(pressed);
                packet.sendToServer();
            }
        }
    }
}
