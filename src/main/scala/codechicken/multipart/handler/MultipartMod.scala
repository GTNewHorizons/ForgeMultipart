package codechicken.multipart.handler

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{
  FMLInitializationEvent,
  FMLPostInitializationEvent,
  FMLPreInitializationEvent,
  FMLServerAboutToStartEvent,
  FMLServerStoppedEvent
}
import cpw.mods.fml.common.Mod.EventHandler
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

  @EventHandler
  def serverStopped(event: FMLServerStoppedEvent) {
    MultipartSaveLoad.loadingWorld = null;
  }
}
