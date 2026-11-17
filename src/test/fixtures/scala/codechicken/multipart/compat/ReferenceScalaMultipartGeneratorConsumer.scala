package codechicken.multipart.compat

import codechicken.multipart.{MultipartGenerator, TMultiPart, TileMultipart}
import net.minecraft.tileentity.TileEntity

/** Frozen against Scala: companion-only generation and singleton pass-through registration. */
class ReferenceScalaMultipartGeneratorConsumer {
  def create(tile: TileEntity, parts: Iterable[TMultiPart], client: Boolean): TileMultipart =
    MultipartGenerator.generateCompositeTile(tile, parts, client)

  def register(name: String): Unit = MultipartGenerator.registerPassThroughInterface(name)
}
