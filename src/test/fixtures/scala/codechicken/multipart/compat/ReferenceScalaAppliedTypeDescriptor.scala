package codechicken.multipart.compat

import java.util.function.Supplier

import codechicken.multipart.asm.ScalaSignature

class ReferenceScalaAppliedTypeDescriptor {
  def ref(
      sig: ScalaSignature,
      names: Supplier[String],
      arguments: List[ScalaSignature#TypeRef]
  ): ScalaSignature#TypeRefType =
    new sig.TypeRefType(
      null,
      null,
      arguments.asInstanceOf[List[sig.TypeRef]]
    ) {
      override def name = names.get()
    }

  def descriptor(ref: ScalaSignature#TypeRefType): String = ref.jDesc
}
