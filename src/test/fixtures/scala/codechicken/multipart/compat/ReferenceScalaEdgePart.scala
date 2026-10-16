package codechicken.multipart.compat

import codechicken.multipart.{TEdgePart, TMultiPart}

/** Mixes in TEdgePart without overriding what it supplies, so the compiled forwarder calls
  * TEdgePart$class.conductsRedstone and the constructor calls TEdgePart$class.$init$.
  */
class ReferenceScalaEdgePart extends TMultiPart with TEdgePart {
  def getType = "test:reference-edge"

  def getSlotMask = 1 << 15
}
