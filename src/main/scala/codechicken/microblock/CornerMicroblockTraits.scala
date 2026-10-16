package codechicken.microblock

// Retain Scala inheritance metadata and helper bridges for generated and external mixins.
trait CornerMicroblock extends CommonMicroblock {
  override def setShape(size: Int, slot: Int) =
    CornerMicroblockTraitLogic.setShape(this, size, slot)

  def microClass = CornerMicroClass$.MODULE$

  def getBounds = CornerMicroblockTraitLogic.getBounds(this)

  override def getSlot = CornerMicroblockTraitLogic.getSlot(this)
}
