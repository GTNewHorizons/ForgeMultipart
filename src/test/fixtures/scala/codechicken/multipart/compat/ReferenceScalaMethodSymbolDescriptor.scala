package codechicken.multipart.compat

import java.util.function.Supplier

import codechicken.multipart.asm.ScalaSignature

class ReferenceScalaMethodSymbolDescriptor {
  def ref(
      sig: ScalaSignature,
      infos: Supplier[ScalaSignature#TMethodType]
  ): ScalaSignature#MethodSymbol = new sig.MethodSymbol(null, null, 0, 0) {
    override def info = infos.get().asInstanceOf[sig.TMethodType]
  }

  def descriptor(ref: ScalaSignature#MethodSymbol): String = ref.jDesc
}
