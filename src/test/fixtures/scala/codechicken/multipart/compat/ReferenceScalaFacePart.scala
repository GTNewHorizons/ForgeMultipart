package codechicken.multipart.compat

import codechicken.multipart.{TFacePart, TMultiPart}

/** Mixes in TFacePart without overriding either supplied member, so the compiled forwarders call
  * TFacePart$class.solid, TFacePart$class.redstoneConductionMap and TFacePart$class.$init$.
  */
class ReferenceScalaFacePart extends TMultiPart with TFacePart {
  def getType = "test:reference-face"

  def getSlotMask = 0x3f
}
