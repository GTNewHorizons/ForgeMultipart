package codechicken.multipart.asm

import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConversions._
import org.objectweb.asm.tree._
import org.objectweb.asm.Opcodes._
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type._
import codechicken.lib.asm.ASMHelper._
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import net.minecraft.launchwrapper.LaunchClassLoader
import ASMImplicits._

object ASMMixinCompiler {
  val cl = CompilerBootstrap.loader(getClass)
  val m_defineClass = CompilerBootstrap.defineClassMethod()
  val m_runTransformers = CompilerBootstrap.runTransformersMethod()
  val f_transformerExceptions =
    CompilerBootstrap.transformerExceptionsField()
  CompilerBootstrap.open(
    m_defineClass,
    m_runTransformers,
    f_transformerExceptions
  )

  private val traitByteMap = CompilerBootstrap.mutableMap[String, Array[Byte]]()
  private val mixinMap = CompilerBootstrap.mutableMap[String, MixinInfo]()

  def define(name: String, bytes: Array[Byte]) = ClassBytes.define(name, bytes)

  CompilerBootstrap.warmupSanityChecker()

  def getBytes(name: String): Array[Byte] = ClassBytes.getBytes(name)

  def internalDefine(name$ : String, bytes: Array[Byte]) =
    ClassBytes.internalDefine(traitByteMap, name$, bytes)

  def classNode(name$ : String) = ClassBytes.classNode(traitByteMap, name$)

  def getMixinInfo(name: String) = mixinMap.get(name)

  case class FieldMixin(name: String, desc: String, access: Int) {
    def accessName(owner: String) = if ((access & ACC_PRIVATE) != 0)
      owner.replace('/', '$') + "$$" + name
    else
      name
  }

  case class MixinInfo(
      name: String,
      parent: String,
      parentTraits: Seq[MixinInfo],
      fields: Seq[FieldMixin],
      methods: Seq[MethodNode],
      supers: Seq[String]
  ) {
    def linearise: Seq[MixinInfo] = parentTraits.flatMap(_.linearise) :+ this
    def tname = name + "$class"
  }

  abstract class MethodInfo {
    def owner: ClassInfo
    def name: String
    def desc: String
    def exceptions: Array[String]
    def isPrivate: Boolean
    def isAbstract: Boolean

    override def toString = owner.name + "." + name + desc
  }

  abstract class ClassInfo {
    def name: String
    def superClass: Option[ClassInfo]
    def interfaces: Iterable[ClassInfo]
    def methods: Iterable[MethodInfo]

    override def toString =
      getClass.getName.replaceAll(".+[\\$\\.]", "") + "(" + name + ")"

    // javac reads view()'s generic return as Object instead of its IterableView descriptor.
    def parentMethods =
      ClassInfoLookup.parentMethods((superClass ++ interfaces).view)
    def allMethods: Iterable[MethodInfo] = ClassInfoLookup.allMethods(this)
    def findPublicImpl(name: String, desc: String) =
      ClassInfoLookup.findPublicImpl(this, name, desc)

    def isScala = false
    def isTrait = false
    def isObject = false
    def moduleName = name
  }

  private val infoCache = CompilerBootstrap.mutableMap[String, ClassInfo]()

  def remClassInfo(name: String) = infoCache.remove(name)
  implicit def getClassInfo(name: String) =
    ClassInfoLookup.getClassInfo(infoCache, name)
  implicit def getClassInfo(cnode: ClassNode): ClassInfo = getClassInfo(
    cnode.name
  )
  implicit def getClassInfo(clazz: Class[_]): ClassInfo =
    if (clazz == null) null else getClassInfo(nodeName(clazz.getName))

  object ClassInfo {
    class ReflectionClassInfo(clazz: Class[_]) extends ClassInfo {
      case class ReflectionMethodInfo(method: Method) extends MethodInfo {
        def owner = ReflectionClassInfo.this
        def name = method.getName
        def desc = getType(method).getDescriptor
        def exceptions = ClassInfoLookup.exceptionNames(method)
        def isPrivate = Modifier.isPrivate(method.getModifiers)
        def isAbstract = Modifier.isAbstract(method.getModifiers)
      }

      def name = nodeName(clazz.getName)
      def superClass = Option(clazz.getSuperclass)
      def interfaces = ClassInfoLookup.reflectionInterfaces(clazz)
      def methods =
        ClassInfoLookup.reflectionMethods(clazz, ReflectionMethodInfo(_))
    }

    class ClassNodeInfo(val cnode: ClassNode) extends ClassInfo {
      case class MethodNodeInfoSource(mnode: MethodNode) extends MethodInfo {
        def owner = ClassNodeInfo.this
        def name = mnode.name
        def desc = mnode.desc
        def exceptions = ClassInfoLookup.exceptionNames(mnode.exceptions)
        def isPrivate = (mnode.access & ACC_PRIVATE) != 0
        def isAbstract = (mnode.access & ACC_ABSTRACT) != 0
      }

      def name = cnode.name
      def superClass = Some(cnode.superName)
      def interfaces: Seq[ClassInfo] = ClassInfoLookup.nodeInterfaces(cnode)
      def methods =
        ClassInfoLookup.nodeMethods(cnode.methods, MethodNodeInfoSource)
    }

    class ScalaClassInfo(
        cnode$ : ClassNode,
        val sig: ScalaSignature,
        val csym: ScalaSignature#ClassSymbolRef
    ) extends ClassNodeInfo(cnode$) {
      override def superClass = Some(csym.jParent)
      override def interfaces = ClassInfoLookup.scalaInterfaces(csym)
      override def isScala = true

      override def isTrait = csym.isTrait
      override def isObject = csym.isObject
    }

    private[ASMMixinCompiler] def obtainInfo(name: String): ClassInfo =
      ClassInfoLookup.obtainInfo(
        name,
        (node, sig, symbol) =>
          new ScalaClassInfo(
            node,
            sig,
            symbol.asInstanceOf[ScalaSignature#ClassSymbolRef]
          ),
        node => new ClassNodeInfo(node),
        clazz => new ReflectionClassInfo(clazz)
      )
  }

  import StackAnalyser.width

  def finishBridgeCall(
      mv: MethodVisitor,
      mvdesc: String,
      opcode: Int,
      owner: String,
      name: String,
      desc: String
  ) {
    ASMBridgeEmitter.finishBridgeCall(mv, mvdesc, opcode, owner, name, desc)
  }

  def writeBridge(
      mv: MethodVisitor,
      mvdesc: String,
      opcode: Int,
      owner: String,
      name: String,
      desc: String
  ) {
    ASMBridgeEmitter.writeBridge(mv, mvdesc, opcode, owner, name, desc)
  }

  def writeStaticBridge(mv: MethodNode, mname: String, t: MixinInfo) =
    ASMBridgeEmitter.writeStaticBridge(mv, mname, t)

  def mixinClasses(
      name: String,
      superClass: String,
      traits: Seq[String]
  ): Class[_] = {
    // Retain the compiler-emitted public local-helper binary name.
    def allParents(info: ClassInfo): Iterable[ClassInfo] =
      MixinClassGenerator.allParents(info)
    MixinClassGenerator.mixinClasses(
      name,
      superClass,
      traits,
      mixinMap,
      allParents
    )
  }
  def seperateDesc(nameDesc: String) = ASMBridgeEmitter.seperateDesc(nameDesc)

  def staticDesc(owner: String, desc: String) =
    ASMBridgeEmitter.staticDesc(owner, desc)

  def getSuper(
      minsn: MethodInsnNode,
      stack: StackAnalyser
  ): Option[MethodInfo] = ClassInfoLookup.getSuper(minsn, stack)

  // javac expects an extra '$' in the retained ClassInfo$ nested binary names.
  def getAndRegisterParentTraits(cnode: ClassNode) =
    ScalaTraitRegistration.getAndRegisterParentTraits(
      cnode,
      info =>
        info match {
          case i: ClassInfo.ScalaClassInfo => i.isTrait && !i.csym.isInterface
          case _                           => false
        },
      info => info.asInstanceOf[ClassInfo.ScalaClassInfo].cnode
    )

  def registerJavaTrait(cnode: ClassNode) =
    JavaTraitRegistration.register(cnode, mixinMap)
  def listSideOnly(sig: ScalaSignature) = ClassInfoLookup.listSideOnly(sig)

  def registerScalaTrait(cnode: ClassNode): MixinInfo =
    ScalaTraitRegistration.registerScalaTrait(
      cnode,
      mixinMap,
      info => info.asInstanceOf[ClassInfo.ScalaClassInfo].sig,
      info => info.asInstanceOf[ClassInfo.ScalaClassInfo].csym
    )
}
