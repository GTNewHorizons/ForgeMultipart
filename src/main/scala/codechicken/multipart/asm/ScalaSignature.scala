package codechicken.multipart.asm

import ScalaSignature._

object ScalaSignature {
  object Bytes {
    def apply(arr: Array[Byte]): Bytes = Bytes(arr, 0, arr.length)
  }

  case class Bytes(arr: Array[Byte], pos: Int, len: Int) {
    def reader = new ByteCodeReader(this)
    def section = ScalaSignatureParser.section(this)
  }

  trait Flags {
    def hasFlag(flag: Int): Boolean

    def isPrivate = hasFlag(0x00000004)
    def isProtected = hasFlag(0x00000008)
    def isAbstract = hasFlag(0x00000080)
    def isDeferred = hasFlag(0x00000100)
    // abstract for methods
    def isMethod = hasFlag(0x00000200)
    def isModule = hasFlag(0x00000400)
    // object module class
    def isInterface = hasFlag(0x00000800)
    def isParam = hasFlag(0x00002000)
    def isStatic = hasFlag(0x00800000)
    def isTrait = hasFlag(0x02000000)
    def isAccessor = hasFlag(0x08000000)
  }
}

class ScalaSignature(val bytes: Bytes) {
  val major = bytes.arr(0).toInt
  val minor = bytes.arr(1).toInt
  val table =
    ScalaSignatureParser.readTable(this, bytes).asInstanceOf[Array[SigEntry]]

  trait SymbolRef extends Flags {
    def full: String
    def flags: Int
    def hasFlag(flag: Int) = (flags & flag) != 0
  }

  trait ClassSymbolRef extends SymbolRef {
    def name: String
    def owner: SymbolRef
    def flags: Int
    def infoId: Int

    def full = ScalaSignatureParser.classSymbolFull(this)
    override def toString = ScalaSignatureParser.classSymbolString(this)

    def isObject = false
    def info: ClassType = evalT(infoId)
    def jParent = ScalaSignatureParser.classParentName(this)
    def jInterfaces = ScalaSignatureParser.interfaceNames(this)
  }

  case class ClassSymbol(
      name: String,
      owner: SymbolRef,
      flags: Int,
      infoId: Int
  ) extends ClassSymbolRef

  case class ObjectSymbol(
      name: String,
      owner: SymbolRef,
      flags: Int,
      infoId: Int
  ) extends ClassSymbolRef {
    override def isObject = true
  }

  case class MethodSymbol(
      name: String,
      owner: SymbolRef,
      flags: Int,
      infoId: Int
  ) extends SymbolRef {
    override def toString = ScalaSignatureParser.methodSymbolString(this)
    def full = ScalaSignatureParser.methodSymbolFull(this)

    def info: TMethodType =
      ScalaSignatureParser
        .methodSymbolInfo(ScalaSignature.this, this)
        .asInstanceOf[TMethodType]
    def jDesc = ScalaSignatureParser.methodSymbolDescriptor(this)
  }

  case class ExternalSymbol(name: String) extends SymbolRef {
    override def toString = name
    def full = name
    def flags = 0
  }

  case object NoSymbol extends SymbolRef {
    def full = "<no symbol>"
    def flags = 0
  }

  trait TMethodType {
    def jDesc = ScalaSignatureParser.methodDescriptor(this)
    def returnType: TypeRef
    def params: List[MethodSymbol]
  }

  case class ClassType(owner: SymbolRef, parents: List[TypeRef]) {
    def parent = parents.head
    def interfaces = parents.drop(1)
  }

  case class MethodType(returnType: TypeRef, params: List[MethodSymbol])
      extends TMethodType

  case class ParameterlessType(returnType: TypeRef) extends TMethodType {
    def params = List()
  }

  trait TypeRef {
    def sym: SymbolRef
    def name = sym.full

    def jName = ScalaSignatureParser.typeName(name)

    def jDesc = ScalaSignatureParser.typeDescriptor(this)
  }

  case class TypeRefType(owner: TypeRef, sym: SymbolRef, typArgs: List[TypeRef])
      extends TMethodType
      with TypeRef {
    def params = List()

    def returnType = this

    override def jDesc = ScalaSignatureParser.appliedTypeDescriptor(this)
  }

  case class ThisType(sym: SymbolRef) extends TypeRef

  case class SingleType(owner: TypeRef, sym: SymbolRef) extends TypeRef {
    override def jName = super.jName + "$"
  }

  case object NoType extends TypeRef {
    def sym = null
    override def name = "<no type>"
  }

  case class SigEntry(index: Int, start: Int, bytes: Bytes) {
    def id = bytes.arr(start)
    def delete() = bytes.arr(start) = 3
    override def toString =
      "SigEntry(" + index + "," + id + "," + bytes.len + " bytes)"
  }

  trait Literal {
    def value: Any
  }
  case class BooleanLiteral(value: Boolean) extends Literal
  case class ByteLiteral(value: Byte) extends Literal
  case class ShortLiteral(value: Short) extends Literal
  case class CharLiteral(value: Char) extends Literal
  case class IntLiteral(value: Int) extends Literal
  case class LongLiteral(value: Long) extends Literal
  case class FloatLiteral(value: Float) extends Literal
  case class DoubleLiteral(value: Double) extends Literal
  case object NullLiteral extends Literal {
    override def value = null
  }
  case class StringLiteral(value: String) extends Literal
  case class TypeLiteral(value: TypeRef) extends Literal
  case class EnumLiteral(value: ExternalSymbol) extends Literal
  case class ArrayLiteral(value: List[_]) extends Literal

  case class AnnotationInfo(
      owner: SymbolRef,
      annType: TypeRef,
      values: Map[String, Literal]
  ) {
    def getValue[T](name: String) = values(name).asInstanceOf[T]
  }

  def evalS(i: Int): String = ScalaSignatureParser.evalS(this, i)

  def evalT[T](i: Int) = eval(i).asInstanceOf[T]

  def evalList[T](bcr: ByteCodeReader): List[Nothing] =
    ScalaSignatureParser.evalList(this, bcr).asInstanceOf[List[Nothing]]

  def eval(i: Int): Any = {
    val e = table(i)
    val bcr = e.bytes.reader
    val id = e.id
    def evalT[T] = this.evalT[T](bcr.readNat)
    def evalList[T] = this.evalList[T](bcr)

    // Java misreads the outer parameter in these Scala 2.11 generic constructors.
    id match {
      case 16 => TypeRefType(evalT, evalT, evalList)
      case 19 => ClassType(evalT, evalList)
      case 20 => MethodType(evalT, evalList)
      case 40 =>
        AnnotationInfo(
          evalT,
          evalT,
          evalList.grouped(2).map(g => (g(0), g(1))).toMap
        )
      case 44 => ArrayLiteral(evalList)
      case _  => ScalaSignatureParser.eval(this, i, e, bcr, id)
    }
  }

  def collect[T](id: Int): scala.collection.immutable.IndexedSeq[T] =
    ScalaSignatureParser.collect[T](this, id)

  def findObject(name: String): Option[ObjectSymbol] =
    ScalaSignatureParser
      .findObject(this, name)
      .asInstanceOf[Option[ObjectSymbol]]
  def findClass(name: String): Option[ClassSymbol] =
    ScalaSignatureParser.findClass(this, name).asInstanceOf[Option[ClassSymbol]]
}
