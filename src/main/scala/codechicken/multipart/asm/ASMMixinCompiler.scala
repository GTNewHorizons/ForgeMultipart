package codechicken.multipart.asm

import scala.collection.mutable.{Map => MMap, ListBuffer => MList, Set => MSet}
import java.util.{Set => JSet}
import scala.collection.JavaConversions._
import org.objectweb.asm.tree._
import org.objectweb.asm.Opcodes._
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.MethodVisitor
import Type._
import codechicken.lib.asm.ASMHelper._
import codechicken.lib.asm.{InsnListSection, InsnComparator, ObfMapping}
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import net.minecraft.launchwrapper.LaunchClassLoader
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import ASMImplicits._
import cpw.mods.fml.relauncher.FMLLaunchHandler

object ASMMixinCompiler {
  val cl = getClass.getClassLoader.asInstanceOf[LaunchClassLoader]
  val m_defineClass = classOf[ClassLoader].getDeclaredMethod(
    "defineClass",
    classOf[Array[Byte]],
    Integer.TYPE,
    Integer.TYPE
  )
  val m_runTransformers = classOf[LaunchClassLoader].getDeclaredMethod(
    "runTransformers",
    classOf[String],
    classOf[String],
    classOf[Array[Byte]]
  )
  val f_transformerExceptions =
    classOf[LaunchClassLoader].getDeclaredField("transformerExceptions")
  m_defineClass.setAccessible(true)
  m_runTransformers.setAccessible(true)
  f_transformerExceptions.setAccessible(true)

  private val traitByteMap = MMap[String, Array[Byte]]()
  private val mixinMap = MMap[String, MixinInfo]()

  def define(name: String, bytes: Array[Byte]) = {
    internalDefine(name, bytes)
    DebugPrinter$.MODULE$.defined(name, bytes)

    try {
      m_defineClass
        .invoke(cl, bytes, 0: Integer, bytes.length: Integer)
        .asInstanceOf[Class[_]]
    } catch {
      case link: LinkageError if link.getMessage.contains("duplicate") =>
        throw new IllegalStateException(
          "class with name: " + name + " already loaded. Do not reference your java mixin classes before registering",
          link
        )
    }
  }

  getBytes("cpw/mods/fml/common/asm/FMLSanityChecker")

  def getBytes(name: String): Array[Byte] = {
    val jName = name.replace('/', '.')
    if (jName == "java.lang.Object")
      return null

    def useTransformers = f_transformerExceptions
      .get(cl)
      .asInstanceOf[JSet[String]]
      .find(jName.startsWith)
      .isEmpty

    val obfName =
      if (ObfMapping.obfuscated)
        FMLDeobfuscatingRemapper.INSTANCE.unmap(name).replace('/', '.')
      else jName
    val bytes = cl.getClassBytes(obfName)
    if (bytes != null && useTransformers)
      return m_runTransformers
        .invoke(cl, jName, obfName, bytes)
        .asInstanceOf[Array[Byte]]

    return bytes
  }

  def internalDefine(name$ : String, bytes: Array[Byte]) {
    val name = nodeName(name$)
    traitByteMap.put(name, bytes)
    remClassInfo(name)
    DebugPrinter$.MODULE$.dump(name, bytes)
  }

  def classNode(name$ : String) = {
    val name = nodeName(name$)
    traitByteMap.getOrElseUpdate(name, getBytes(name)) match {
      case null => null
      case v    => createClassNode(v, ClassReader.EXPAND_FRAMES)
    }
  }

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

  private val infoCache = MMap[String, ClassInfo]()

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
    if (traits.isEmpty)
      return cl.findClass(superClass.name.replace('/', '.'))

    val startTime = System.currentTimeMillis

    val baseTraits = traits.map(mixinMap)
    val mixinInfos = baseTraits.flatMap(_.linearise).distinct
    val baseInfo = getClassInfo(superClass)
    val traitInfos = mixinInfos.map(i => getClassInfo(i.name))

    val cnode = new ClassNode()
    // implements list
    cnode.visit(
      V1_6,
      ACC_PUBLIC,
      name,
      null,
      superClass,
      baseTraits.map(_.name).toArray[String]
    )

    val cinit = baseInfo.methods.find(_.name == "<init>").get
    val minit = cnode
      .visitMethod(ACC_PUBLIC, "<init>", cinit.desc, null, null)
      .asInstanceOf[MethodNode]
    writeBridge(
      minit,
      cinit.desc,
      INVOKESPECIAL,
      superClass,
      "<init>",
      cinit.desc
    )
    minit.instructions.remove(
      minit.instructions.getLast
    ) // remove the RETURN from writeBridge

    val prevInfos = MList[MixinInfo]()

    mixinInfos.foreach { t =>
      minit.visitVarInsn(ALOAD, 0)
      minit.visitMethodInsn(
        INVOKESTATIC,
        t.tname,
        "$init$",
        "(L" + t.name + ";)V",
        false
      )

      t.fields.foreach { f =>
        val fv = cnode
          .visitField(ACC_PRIVATE, f.accessName(t.name), f.desc, null, null)
          .asInstanceOf[FieldNode]

        val ftype = getType(fv.desc)
        var mv =
          cnode.visitMethod(ACC_PUBLIC, fv.name, "()" + f.desc, null, null)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(GETFIELD, name, fv.name, fv.desc)
        mv.visitInsn(ftype.getOpcode(IRETURN))
        mv.visitMaxs(1, 1)

        mv = cnode.visitMethod(
          ACC_PUBLIC,
          fv.name + "_$eq",
          "(" + f.desc + ")V",
          null,
          null
        )
        mv.visitVarInsn(ALOAD, 0)
        mv.visitVarInsn(ftype.getOpcode(ILOAD), 1)
        mv.visitFieldInsn(PUTFIELD, name, fv.name, fv.desc)
        mv.visitInsn(RETURN)
        mv.visitMaxs(width(ftype) + 1, width(ftype) + 1)
      }

      t.supers.foreach { s =>
        val (name, desc) = seperateDesc(s)
        val mv = cnode
          .visitMethod(
            ACC_PUBLIC,
            t.name.replace('/', '$') + "$$super$" + name,
            desc,
            null,
            null
          )
          .asInstanceOf[MethodNode]

        prevInfos.reverse.find(
          _.methods.exists(m => m.name == name && m.desc == desc)
        ) match {
          // each super goes to the one before
          case Some(st) => writeStaticBridge(mv, name, st)
          case None =>
            writeBridge(
              mv,
              desc,
              INVOKESPECIAL,
              baseInfo.findPublicImpl(name, desc).get.owner.name,
              name,
              desc
            )
        }
      }

      prevInfos += t
    }

    val methodSigs = MSet[String]()
    mixinInfos.reverse.foreach { t => // last trait gets first pick on methods
      t.methods.foreach { m =>
        if (!methodSigs(m.name + m.desc)) {
          val mv = cnode
            .visitMethod(
              ACC_PUBLIC,
              m.name,
              m.desc,
              null,
              Array(m.exceptions: _*)
            )
            .asInstanceOf[MethodNode]
          writeStaticBridge(mv, m.name, t)
          methodSigs += m.name + m.desc
        }
      }
    }

    minit.visitInsn(RETURN)

    // generate synthetic bridge methods for covariant return types
    def allParents(info: ClassInfo): Iterable[ClassInfo] =
      info +: (info.superClass ++ info.interfaces).toSeq.flatMap(allParents)
    val allParentInfos = (baseInfo +: traitInfos).flatMap(allParents).distinct
    val allParentMethods = allParentInfos.flatMap(_.methods)
    methodSigs.toSeq.foreach { nameDesc =>
      val (name, desc) = seperateDesc(nameDesc)
      val pDesc = desc.substring(0, desc.lastIndexOf(')') + 1)

      allParentMethods
        .filter(m => m.name == name && m.desc.startsWith(pDesc))
        .foreach { m =>
          if (!methodSigs(m.name + m.desc)) {
            val mv = cnode
              .visitMethod(
                ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE,
                m.name,
                m.desc,
                null,
                m.exceptions
              )
              .asInstanceOf[MethodNode]
            writeBridge(mv, mv.desc, INVOKEVIRTUAL, cnode.name, name, desc)
            methodSigs += m.name + m.desc
          }
        }
    }

    val c = define(name, createBytes(cnode, 0))

    DebugPrinter$.MODULE$
      .logger()
      .debug(
        "Generation [" + superClass + " with " + traits.mkString(
          ", "
        ) + "] took " + (System.currentTimeMillis - startTime) + "ms"
      )
    c
  }

  def seperateDesc(nameDesc: String) = ASMBridgeEmitter.seperateDesc(nameDesc)

  def staticDesc(owner: String, desc: String) =
    ASMBridgeEmitter.staticDesc(owner, desc)

  def getSuper(
      minsn: MethodInsnNode,
      stack: StackAnalyser
  ): Option[MethodInfo] = ClassInfoLookup.getSuper(minsn, stack)

  def getAndRegisterParentTraits(cnode: ClassNode) =
    cnode.interfaces.map(getClassInfo).collect {
      case i: ClassInfo.ScalaClassInfo if i.isTrait && !i.csym.isInterface =>
        registerScalaTrait(i.cnode)
    }

  def registerJavaTrait(cnode: ClassNode) {
    if ((cnode.access & ACC_INTERFACE) != 0)
      throw new IllegalArgumentException(
        "Cannot register java interface " + cnode.name + " as a mixin trait. Try register passThroughInterface"
      )
    if (!cnode.innerClasses.isEmpty)
      throw new IllegalArgumentException(
        "Inner classes are not permitted for " + cnode.name + " as a java mixin trait. Use scala"
      )
    if ((cnode.access & ACC_ABSTRACT) != 0)
      throw new IllegalArgumentException(
        "Cannot register abstract class " + cnode.name + " as a java mixin trait. Use scala"
      )

    val parentTrait = getMixinInfo(cnode.superName)
    val fields = cnode.fields
      .map(f => (f.name, FieldMixin(f.name, f.desc, f.access)))
      .toMap
    val supers = MList[String]() // nameDesc to super owner
    val methods = MList[MethodNode]()
    val methodSigs = cnode.methods.map(m => m.name + m.desc).toSet

    /*if ((cnode.access & ACC_ABSTRACT) != 0) {//verify all methods are implemented
            def getInterfaces(cnode:ClassNode):Seq[ClassNode] = cnode.interfaces.map(classNode).flatMap(i => getInterfaces(i) :+ i)
            val interfaces = getInterfaces(cnode).distinct
            val implementedSigs = (cnode.methods.filter(m => (m.access & ACC_ABSTRACT) == 0)++parentTraits.flatMap(_.methods)).map(m => m.name + m.desc).toSet
            val missing = interfaces.flatMap(_.methods).map(m => m.name + m.desc).filterNot(implementedSigs)
            if(!missing.isEmpty)
                throw new IllegalArgumentException("Abstract java trait "+cnode.name+" needs to implement "+missing.mkString(", "))
        }*/

    val inode = new ClassNode() // impl node
    inode.visit(
      V1_6,
      ACC_ABSTRACT | ACC_PUBLIC,
      cnode.name + "$class",
      null,
      "java/lang/Object",
      null
    )
    inode.sourceFile = cnode.sourceFile

    val tnode = new ClassNode() // trait node (interface)
    tnode.visit(
      V1_6,
      ACC_INTERFACE | ACC_ABSTRACT | ACC_PUBLIC,
      cnode.name,
      null,
      "java/lang/Object",
      (cnode.interfaces ++ parentTrait.map(_.name)).distinct.toArray
    )

    def fname(name: String) = fields(name).accessName(cnode.name)

    fields.values.foreach { fnode =>
      tnode.visitMethod(
        ACC_PUBLIC | ACC_ABSTRACT,
        fname(fnode.name),
        "()" + fnode.desc,
        null,
        null
      )
      tnode.visitMethod(
        ACC_PUBLIC | ACC_ABSTRACT,
        fname(fnode.name) + "_$eq",
        "(" + fnode.desc + ")V",
        null,
        null
      )
    }

    def superInsn(minsn: MethodInsnNode) = {
      val bridgeName = cnode.name.replace('/', '$') + "$$super$" + minsn.name
      if (!supers.contains(minsn.name + minsn.desc)) {
        tnode.visitMethod(
          ACC_PUBLIC | ACC_ABSTRACT,
          bridgeName,
          minsn.desc,
          null,
          null
        )
        supers += minsn.name + minsn.desc
      }
      new MethodInsnNode(
        INVOKEINTERFACE,
        cnode.name,
        bridgeName,
        minsn.desc,
        true
      )
    }

    def staticClone(mnode: MethodNode, name: String, access: Int) = {
      val mv = inode
        .visitMethod(
          access | ACC_STATIC,
          name,
          staticDesc(cnode.name, mnode.desc),
          null,
          Array(mnode.exceptions: _*)
        )
        .asInstanceOf[MethodNode]
      copy(mnode, mv)
      mv
    }

    def staticTransform(mnode: MethodNode, base: MethodNode) {
      val stack = new StackAnalyser(
        getType(Type.getObjectType(cnode.name).getDescriptor),
        base
      )
      val insnList = mnode.instructions
      var insn = insnList.getFirst

      def replace(newinsn: AbstractInsnNode) {
        insnList.insert(insn, newinsn)
        insnList.remove(insn)
        insn = newinsn
      }

      // transform
      while (insn != null) {
        insn match {
          case finsn: FieldInsnNode =>
            insn.getOpcode match {
              case GETFIELD =>
                replace(
                  new MethodInsnNode(
                    INVOKEINTERFACE,
                    cnode.name,
                    fname(finsn.name),
                    "()" + finsn.desc,
                    true
                  )
                )
              case PUTFIELD =>
                replace(
                  new MethodInsnNode(
                    INVOKEINTERFACE,
                    cnode.name,
                    fname(finsn.name) + "_$eq",
                    "(" + finsn.desc + ")V",
                    true
                  )
                )
              case _ =>
            }
          case minsn: MethodInsnNode =>
            insn.getOpcode match {
              case INVOKESPECIAL =>
                if (getSuper(minsn, stack).isDefined)
                  replace(superInsn(minsn))
              case INVOKEVIRTUAL =>
                if (minsn.owner == cnode.name) {
                  if (methodSigs.contains(minsn.name + minsn.desc)) { // call the interface method
                    replace(
                      new MethodInsnNode(
                        INVOKEINTERFACE,
                        minsn.owner,
                        minsn.name,
                        minsn.desc,
                        true
                      )
                    )
                  } else {
                    // cast to parent class and call
                    val mType = Type.getMethodType(minsn.desc)
                    val instanceEntry =
                      stack.peek(width(mType.getArgumentTypes))
                    insnList.insert(
                      instanceEntry.insn,
                      new TypeInsnNode(
                        CHECKCAST,
                        Type.getObjectType(cnode.superName).getDescriptor
                      )
                    )
                    minsn.owner = cnode.superName
                  }
                }
              case _ =>
            }
          case _ =>
        }
        stack.visitInsn(insn)
        insn = insn.getNext
      }
    }

    def convertMethod(mnode: MethodNode) {
      if (mnode.name == "<clinit>")
        throw new IllegalArgumentException(
          "Static initialisers are not permitted " + cnode.name + " as a mixin trait"
        )

      if (mnode.name == "<init>") {
        if (mnode.desc != "()V")
          throw new IllegalArgumentException(
            "Constructor arguments are not permitted " + cnode.name + " as a mixin trait"
          )

        val mv = staticClone(mnode, "$init$", ACC_PUBLIC)
        def removeSuperConstructor() {
          val insns = new InsnListSection
          insns.add(new VarInsnNode(ALOAD, 0))
          insns.add(
            new MethodInsnNode(
              INVOKESPECIAL,
              cnode.superName,
              "<init>",
              "()V",
              false
            )
          )

          val minsns = new InsnListSection(mv.instructions)
          val found = InsnComparator.matches(minsns, insns, Set[LabelNode]())
          if (found == null)
            throw new IllegalArgumentException(
              "Invalid constructor insn sequence " + cnode.name + "\n" + minsns
            )
          found.trim(Set[LabelNode]()).remove()
        }
        removeSuperConstructor()
        staticTransform(mv, mnode)
        return
      }

      if ((mnode.access & ACC_PRIVATE) == 0) {
        val mv = tnode.visitMethod(
          ACC_PUBLIC | ACC_ABSTRACT,
          mnode.name,
          mnode.desc,
          null,
          Array(mnode.exceptions: _*)
        )
        methods += mv.asInstanceOf[MethodNode]
      }

      // convert that method!
      val access =
        if ((mnode.access & ACC_PRIVATE) == 0) ACC_PUBLIC else ACC_PRIVATE
      val mv = staticClone(mnode, mnode.name, access)
      staticTransform(mv, mnode)
    }

    def isGeneratedFieldAccessor(mnode: MethodNode) = fields.values.exists {
      field =>
        val name = field.accessName(cnode.name)
        mnode.name == name && mnode.desc == "()" + field.desc ||
        mnode.name == name + "_$eq" && mnode.desc == "(" + field.desc + ")V"
    }

    cnode.methods.filterNot(isGeneratedFieldAccessor).foreach(convertMethod)

    define(inode.name, createBytes(inode, 0))
    define(tnode.name, createBytes(tnode, 0))

    mixinMap.put(
      tnode.name,
      MixinInfo(
        tnode.name,
        parentTrait.map(_.parent).getOrElse(cnode.superName),
        parentTrait.toSeq,
        fields.values.toSeq,
        methods,
        supers
      )
    )
  }

  def listSideOnly(sig: ScalaSignature) = {
    val side = "cpw.mods.fml.relauncher.Side." + FMLLaunchHandler.side.name
    sig
      .collect[sig.AnnotationInfo](40)
      .filter { a =>
        a.annType.name == "cpw.mods.fml.relauncher.SideOnly" &&
        a.getValue[sig.EnumLiteral]("value").value.full != side
      }
      .map(_.owner.full)
      .toSet
  }

  def registerScalaTrait(cnode: ClassNode): MixinInfo = {
    getMixinInfo(cnode.name) match {
      case Some(info) => return info
      case None       =>
    }

    val info = getClassInfo(cnode).asInstanceOf[ClassInfo.ScalaClassInfo]
    val sig = info.sig
    val sideOnly = listSideOnly(sig)

    val parentTraits = getAndRegisterParentTraits(cnode)
    val fieldAccessors = MMap[String, sig.MethodSymbol]()
    val fields = MList[FieldMixin]()
    val methods = MList[MethodNode]()
    val supers = MList[String]()

    val csym = info.csym
    for (sym <- sig.collect[sig.MethodSymbol](8)) {
      if (sym.isParam || sym.owner != csym) {} else if (
        sideOnly(sym.full)
      ) {} else if (sym.isAccessor) {
        fieldAccessors.put(sym.name, sym)
      } else if (sym.isMethod) {
        val desc = sym.jDesc
        if (sym.name.startsWith("super$"))
          supers += sym.name.substring(6) + desc
        else if (!sym.isPrivate && !sym.isDeferred && sym.name != "$init$")
          methods += (cnode.methods.find(m =>
            m.name == sym.name && m.desc == desc
          ) match {
            case Some(m) => m
            case None =>
              throw new IllegalArgumentException(
                "Unable to add mixin trait " + cnode.name + ": " +
                  sym.name + desc + " found in scala signature but not in class file. Most likely an obfuscation issue."
              )
          })
      } else {
        fields += FieldMixin(
          sym.name.trim,
          sym.jDesc,
          if (fieldAccessors(sym.name.trim).isPrivate) ACC_PRIVATE
          else ACC_PUBLIC
        )
      }
    }

    val mixin =
      MixinInfo(cnode.name, csym.jParent, parentTraits, fields, methods, supers)
    mixinMap.put(cnode.name, mixin)
    mixin
  }
}
