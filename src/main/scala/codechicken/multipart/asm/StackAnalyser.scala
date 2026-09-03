package codechicken.multipart.asm

import scala.collection.mutable.{Map => MMap, ListBuffer => MList}
import org.objectweb.asm.tree._
import org.objectweb.asm.Opcodes._
import org.objectweb.asm.Type
import org.objectweb.asm.Type._
import scala.collection.JavaConversions._

object StackAnalyser {
  def width(t: Type): Int = t.getSize
  def width(s: String): Int = width(Type.getType(s))
  def width(it: Iterable[Type]): Int = it.foldLeft(0)(_ + width(_))

  abstract class StackEntry(implicit val insn: AbstractInsnNode) {
    def getType: Type
  }
  abstract class LocalEntry {
    def getType: Type
  }

  case class This(owner: Type) extends LocalEntry {
    def getType = owner
  }
  case class Param(i: Int, t: Type) extends LocalEntry {
    def getType = t
  }
  case class Store(e: StackEntry)(implicit val insn: AbstractInsnNode)
      extends LocalEntry {
    def getType = e.getType
  }

  case class Const(c: Any)(implicit insn: AbstractInsnNode) extends StackEntry {
    def getType = c match {
      case o: Byte    => BYTE_TYPE
      case o: Short   => SHORT_TYPE
      case o: Int     => INT_TYPE
      case o: Long    => LONG_TYPE
      case o: Float   => FLOAT_TYPE
      case o: Double  => DOUBLE_TYPE
      case o: Char    => CHAR_TYPE
      case o: Boolean => BOOLEAN_TYPE
      case o: String  => getObjectType("java/lang/String")
      case null       => getObjectType("java/lang/Object")
      case _ => throw new IllegalArgumentException("Unknown const " + c)
    }
  }
  case class Load(e: LocalEntry)(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = e.getType
  }
  case class UnaryOp(op: Int, e: StackEntry)(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = e.getType
  }
  case class BinaryOp(op: Int, e2: StackEntry, e1: StackEntry)(implicit
      insn: AbstractInsnNode
  ) extends StackEntry {
    def getType = e1.getType
  }
  case class PrimitiveCast(e: StackEntry, t: Type)(implicit
      insn: AbstractInsnNode
  ) extends StackEntry {
    def getType = t
  }
  case class ReturnAddress()(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = INT_TYPE
  }
  case class GetField(obj: StackEntry, field: FieldInsnNode)(implicit
      insn: AbstractInsnNode
  ) extends StackEntry {
    def getType = Type.getType(field.desc)
  }
  case class Invoke(
      op: Int,
      params: Array[StackEntry],
      obj: StackEntry,
      method: MethodInsnNode
  )(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = Type.getMethodType(method.desc).getReturnType
  }
  case class New(t: Type)(implicit insn: AbstractInsnNode) extends StackEntry {
    def getType = t
  }
  case class NewArray(len: StackEntry, t: Type)(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = t
  }
  case class ArrayLength(array: StackEntry)(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = INT_TYPE
  }
  case class ArrayLoad(index: StackEntry, e: StackEntry)(implicit
      insn: AbstractInsnNode
  ) extends StackEntry {
    def getType = e.getType.getElementType
  }
  case class Cast(obj: StackEntry, t: Type)(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = t
  }
  case class NewMultiArray(sizes: Array[StackEntry], t: Type)(implicit
      insn: AbstractInsnNode
  ) extends StackEntry {
    def getType = t
  }
  case class CaughtException(t: Type)(implicit insn: AbstractInsnNode)
      extends StackEntry {
    def getType = t
  }
}

class StackAnalyser(val owner: Type, val m: MethodNode) {
  import StackAnalyser._

  val stack = MList[StackEntry]()
  val locals = MList[LocalEntry]()
  private val catchHandlers = MMap[LabelNode, TryCatchBlockNode]()

  {
    if ((m.access & ACC_STATIC) == 0)
      pushL(This(owner))

    val ptypes = getArgumentTypes(m.desc)
    for (i <- 0 until ptypes.length)
      pushL(Param(i, ptypes(i)))

    m.tryCatchBlocks.foreach(b => catchHandlers.put(b.handler, b))
  }

  def pushL(entry: LocalEntry) = setL(locals.size, entry)

  def setL(i: Int, entry: LocalEntry): Unit =
    StackAnalyserLogic.setL(this, i, entry)

  def push(entry: StackEntry) = insert(0, entry)

  def _pop(i: Int = 0) = stack.remove(stack.size - i - 1)

  def pop(i: Int = 0): StackEntry = StackAnalyserLogic.pop(this, i)

  def peek(i: Int = 0) = stack(stack.size - i - 1)

  def insert(i: Int, entry: StackEntry): Unit =
    StackAnalyserLogic.insert(this, i, entry)

  def popArgs(desc: String): Array[StackEntry] =
    StackAnalyserLogic.popArgs(this, desc)

  def visitInsn(ainsn: AbstractInsnNode): Unit =
    StackAnalyserLogic.visitInsn(this, ainsn)
}
