package codechicken.multipart.compat

import java.util.function.Supplier

import codechicken.multipart.asm.ScalaSignature

class ReferenceScalaClassSymbolFull {
  def ref(
      sig: ScalaSignature,
      owners: Supplier[ScalaSignature#SymbolRef],
      names: Supplier[String]
  ): ScalaSignature#ClassSymbolRef = new sig.ClassSymbolRef {
    def name = names.get()
    def owner = owners.get().asInstanceOf[sig.SymbolRef]
    def flags = 0
    def infoId = 0
  }

  def full(ref: ScalaSignature#ClassSymbolRef): String = ref.full
}
