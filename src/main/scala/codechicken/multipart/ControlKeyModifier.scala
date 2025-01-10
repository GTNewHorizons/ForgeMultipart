package codechicken.multipart

import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import net.minecraft.client.Minecraft
import codechicken.lib.packet.PacketCustom
import codechicken.multipart.handler.MultipartCPH
import scala.collection.mutable.HashMap
import net.minecraft.entity.player.EntityPlayer
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent

/** A class that maintains a map server<->client of which players are holding
  * the control (or placement modifier key) much like sneaking.
  */
object ControlKeyModifer {
  implicit def playerControlValue(p: EntityPlayer) = new ControlKeyValue(p)

  class ControlKeyValue(p: EntityPlayer) {
    def isControlDown = map(p)
  }

  val map = HashMap[EntityPlayer, Boolean]().withDefaultValue(false)

  /** Implicit static for Java users.
    */
  def isControlDown(p: EntityPlayer) = p.isControlDown
}

/** Key Handler implementation
  */
object ControlKeyHandler
    extends KeyBinding(
      "key.control",
      Keyboard.KEY_NONE,
      "Forge Multipart"
    ) {
  import ControlKeyModifer._
  var wasPressed = false
  var oldPlayer: EntityPlayer = null

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  def tick(event: ClientTickEvent) {
    val pressed = getIsKeyPressed
    if (pressed != wasPressed) {
      wasPressed = pressed
      if (Minecraft.getMinecraft.getNetHandler != null) {
        map.put(Minecraft.getMinecraft.thePlayer, pressed)
        val packet = new PacketCustom(MultipartCPH.channel, 1)
        packet.writeBoolean(pressed)
        packet.sendToServer()
      }
    }
    if (oldPlayer != null) {
      if (oldPlayer != Minecraft.getMinecraft.thePlayer) {
        map.remove(oldPlayer)
        oldPlayer = Minecraft.getMinecraft.thePlayer
      }
    } else {
      oldPlayer = Minecraft.getMinecraft.thePlayer
    }
  }
}
