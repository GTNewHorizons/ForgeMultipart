package codechicken.multipart.compat

import java.util.function.Supplier

import codechicken.multipart.asm.ScalaSignature

class ReferenceScalaInterfaceNames {
  def info(
      sig: ScalaSignature,
      read: Supplier[List[ScalaSignature#TypeRef]]
  ): ScalaSignature#ClassType = new sig.ClassType(null, Nil) {
    override def interfaces = read.get().asInstanceOf[List[sig.TypeRef]]
  }

  def symbol(
      sig: ScalaSignature,
      read: Supplier[ScalaSignature#ClassType]
  ): ScalaSignature#ClassSymbolRef = new sig.ClassSymbol("unused", null, 0, -1) {
    override def info = read.get().asInstanceOf[sig.ClassType]
  }

  def names(ref: ScalaSignature#ClassSymbolRef): List[String] = ref.jInterfaces
}
