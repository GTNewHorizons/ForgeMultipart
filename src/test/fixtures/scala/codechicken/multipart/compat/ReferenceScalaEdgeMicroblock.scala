package codechicken.multipart.compat

import codechicken.microblock.{EdgeMicroblock, Microblock}

/** Frozen edge-trait forwarders with observable virtual shape access. */
class ReferenceScalaEdgeMicroblock extends Microblock(29) with EdgeMicroblock {
  var writes = 0
  var writtenShape: Byte = 0
  var reads = 0
  var overrideShape = false
  var selectedShape = 0

  override def shape_$eq(value: Byte) {
    writes += 1
    writtenShape = value
    super.shape_$eq(value)
  }

  override def getShape = {
    reads += 1
    if (overrideShape) selectedShape else super.getShape
  }
}
