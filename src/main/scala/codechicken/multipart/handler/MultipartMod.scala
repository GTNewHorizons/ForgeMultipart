package codechicken.multipart.handler

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent
import codechicken.multipart.MultiPartRegistry
import codechicken.multipart.Tags

@Mod(
  modid = "ForgeMultipart",
  name = "Forge Multipart",
  acceptedMinecraftVersions = "[1.7.10]",
  version = Tags.VERSION,
  modLanguage = "scala"
)
object MultipartMod {
  @EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    MultipartProxy.preInit(event.getModConfigurationDirectory, event.getModLog)
  }

  @EventHandler
  def init(event: FMLInitializationEvent) {
    MultipartProxy.init()
  }

  @EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    if (MultiPartRegistry.required) {
      MultiPartRegistry.postInit()
      MultipartProxy.postInit()
    }
  }

  @EventHandler
  def beforeServerStart(event: FMLServerAboutToStartEvent) {
    MultiPartRegistry.beforeServerStart()
  }
}
