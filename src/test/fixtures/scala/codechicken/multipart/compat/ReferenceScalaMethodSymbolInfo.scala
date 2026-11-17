package codechicken.multipart.compat

import codechicken.multipart.asm.ScalaSignature

class ReferenceScalaMethodSymbolInfo {
  def info(ref: ScalaSignature#MethodSymbol): ScalaSignature#TMethodType = ref.info
}
