package codechicken.multipart.asm;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_TRANSIENT;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.asm.CC_ClassWriter;
import codechicken.lib.asm.ObfMapping;
import codechicken.multipart.MultipartGenerator$;
import codechicken.multipart.TileMultipart;
import scala.Predef$;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;
import scala.collection.mutable.ArrayBuffer;
import scala.runtime.ObjectRef;

public final class MultipartMixinFactory$ extends ASMMixinFactory<TileMultipart> {

    public static final MultipartMixinFactory$ MODULE$ = new MultipartMixinFactory$();

    private MultipartMixinFactory$() {
        super(TileMultipart.class, Predef$.MODULE$.wrapRefArray(new Class<?>[0]));
    }

    @Override
    public void autoCompleteJavaTrait(ClassNode cnode) {
        List<FieldNode> copyFields = new ArrayList<>();
        for (FieldNode field : cnode.fields) {
            if ((field.access & ACC_TRANSIENT) == 0) copyFields.add(field);
        }
        if (copyFields.isEmpty() || ASMHelper
                .findMethod(new ObfMapping(cnode.name, "copyFrom", "(Lcodechicken/multipart/TileMultipart;)V"), cnode)
                != null) {
            return;
        }

        MethodVisitor mv = cnode
                .visitMethod(ACC_PUBLIC, "copyFrom", "(Lcodechicken/multipart/TileMultipart;)V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "codechicken/multipart/TileMultipart",
                "copyFrom",
                "(Lcodechicken/multipart/TileMultipart;)V");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(INSTANCEOF, cnode.name);
        Label end = new Label();
        mv.visitJumpInsn(IFEQ, end);

        for (FieldNode field : copyFields) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, cnode.name, field.name, field.desc);
            mv.visitFieldInsn(PUTFIELD, cnode.name, field.name, field.desc);
        }

        mv.visitLabel(end);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
    }

    public String generatePassThroughTrait(String s_interface) {
        String iname = ASMImplicits.nodeName(s_interface);
        String simpleName = iname.substring(iname.lastIndexOf('/') + 1);
        if (simpleName.startsWith("I")) simpleName = simpleName.substring(1);
        String tname = "T" + simpleName + "$$PassThrough";
        String vname = "impl";
        String idesc = "L" + iname + ";";

        ClassNode inode = ASMMixinCompiler$.MODULE$.classNode(s_interface);
        if (inode == null) {
            ASMHelper.logger.error("Unable to generate pass through trait for: " + s_interface + " class not found.");
            return null;
        }
        if ((inode.access & ACC_INTERFACE) == 0) {
            throw new IllegalArgumentException(s_interface + " is not an interface.");
        }

        CC_ClassWriter cw = new CC_ClassWriter(0);
        cw.visit(
                V1_6,
                ACC_PUBLIC | ACC_SUPER,
                tname,
                null,
                "codechicken/multipart/TileMultipart",
                new String[] { iname });

        FieldVisitor fv = cw.visitField(ACC_PRIVATE, vname, idesc, null, null);
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "codechicken/multipart/TileMultipart", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "bindPart", "(Lcodechicken/multipart/TMultiPart;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "codechicken/multipart/TileMultipart",
                "bindPart",
                "(Lcodechicken/multipart/TMultiPart;)V");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(INSTANCEOF, iname);
        Label bindEnd = new Label();
        mv.visitJumpInsn(IFEQ, bindEnd);
        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, iname);
        mv.visitFieldInsn(PUTFIELD, tname, vname, idesc);
        mv.visitLabel(bindEnd);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "partRemoved", "(Lcodechicken/multipart/TMultiPart;I)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "codechicken/multipart/TileMultipart",
                "partRemoved",
                "(Lcodechicken/multipart/TMultiPart;I)V");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, tname, vname, idesc);
        Label removeEnd = new Label();
        mv.visitJumpInsn(IF_ACMPNE, removeEnd);
        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitFieldInsn(PUTFIELD, tname, vname, idesc);
        mv.visitLabel(removeEnd);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "canAddPart", "(Lcodechicken/multipart/TMultiPart;)Z", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, tname, vname, idesc);
        Label canAdd = new Label();
        mv.visitJumpInsn(IFNULL, canAdd);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(INSTANCEOF, iname);
        mv.visitJumpInsn(IFEQ, canAdd);
        mv.visitLabel(new Label());
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitLabel(canAdd);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "codechicken/multipart/TileMultipart",
                "canAddPart",
                "(Lcodechicken/multipart/TMultiPart;)Z");
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        ObjectRef<MethodVisitor> visitor = ObjectRef.create(mv);
        Iterator<?> methods = codechicken$multipart$asm$MultipartMixinFactory$$methods$1(inode).values().iterator();
        while (methods.hasNext()) {
            codechicken$multipart$asm$MultipartMixinFactory$$generatePassThroughMethod$1(
                    (MethodNode) methods.next(),
                    iname,
                    tname,
                    vname,
                    idesc,
                    cw,
                    visitor);
        }

        cw.visitEnd();
        ASMMixinCompiler$.MODULE$.internalDefine(tname, cw.toByteArray());
        registerTrait(tname);
        return tname;
    }

    @Override
    public void onCompiled(Class<? extends TileMultipart> clazz, BitSet traitSet) {
        MultipartGenerator$.MODULE$.registerTileClass(clazz, traitSet);
    }

    // Keep Scala's map representation, inherited override precedence and bytecode emission order.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public final Map codechicken$multipart$asm$MultipartMixinFactory$$methods$1(ClassNode cnode) {
        Map<String, MethodNode> methods = Map$.MODULE$.empty();
        for (MethodNode method : cnode.methods) {
            methods = methods.$plus(new Tuple2<>(method.name + method.desc, method));
        }
        if (cnode.interfaces == null) return methods;

        ArrayBuffer<Tuple2<String, MethodNode>> inherited = new ArrayBuffer<>();
        for (String name : cnode.interfaces) {
            inherited.$plus$plus$eq(
                    MODULE$.codechicken$multipart$asm$MultipartMixinFactory$$methods$1(
                            ASMMixinCompiler$.MODULE$.classNode(name)));
        }
        return methods.$plus$plus(inherited);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public final void codechicken$multipart$asm$MultipartMixinFactory$$generatePassThroughMethod$1(MethodNode method,
            String iname, String tname, String vname, String idesc, CC_ClassWriter cw, ObjectRef mv) {
        mv.elem = cw.visitMethod(
                ACC_PUBLIC,
                method.name,
                method.desc,
                method.signature,
                method.exceptions.toArray(new String[0]));
        ((MethodVisitor) mv.elem).visitVarInsn(ALOAD, 0);
        ((MethodVisitor) mv.elem).visitFieldInsn(GETFIELD, tname, vname, idesc);
        ASMMixinCompiler$.MODULE$.finishBridgeCall(
                (MethodVisitor) mv.elem,
                method.desc,
                INVOKEINTERFACE,
                iname,
                method.name,
                method.desc);
    }
}
