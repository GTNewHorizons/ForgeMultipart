package codechicken.microblock

trait CornerMicroblock extends CommonMicroblock {
  override def setShape(size: Int, slot: Int) = shape_$eq(
    (size << 4 | (slot - 7)).toByte
  )

  def microClass = CornerMicroClass$.MODULE$

  def getBounds = CornerMicroClass.aBounds()(shape)

  override def getSlot = getShape + 7
}
