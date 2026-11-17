package codechicken.multipart.compat

import java.util.function.Supplier

import codechicken.multipart.asm.ScalaSignature

class ReferenceScalaClassParent {
  def classType(
      sig: ScalaSignature,
      parents: List[ScalaSignature#TypeRef]
  ): ScalaSignature#ClassType = new sig.ClassType(null, parents.asInstanceOf[List[sig.TypeRef]])

  def info(
      sig: ScalaSignature,
      read: Supplier[ScalaSignature#TypeRef]
  ): ScalaSignature#ClassType = new sig.ClassType(null, Nil) {
    override def parent = read.get().asInstanceOf[sig.TypeRef]
  }

  def ref(
      sig: ScalaSignature,
      infos: Supplier[ScalaSignature#ClassType]
  ): ScalaSignature#ClassSymbolRef = new sig.ClassSymbolRef {
    def name = "unused"
    def owner = null
    def flags = 0
    def infoId = 0
    override def info = infos.get().asInstanceOf[sig.ClassType]
  }

  def parent(ref: ScalaSignature#ClassSymbolRef): String = ref.jParent
}
