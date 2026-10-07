package codechicken.multipart.compat

import codechicken.microblock.{CornerMicroblock, Microblock}

/** Frozen corner-trait forwarders with observable virtual shape access. */
class ReferenceScalaCornerMicroblock extends Microblock(29) with CornerMicroblock {
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
