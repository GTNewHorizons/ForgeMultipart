package codechicken.multipart.asm;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.Predef$;
import scala.Tuple2;

/** Descriptor and instruction helpers behind the retained compiler companion. */
final class ASMBridgeEmitter {

    private ASMBridgeEmitter() {}

    static Tuple2<String, String> seperateDesc(String nameDesc) {
        int split = nameDesc.indexOf('(');
        return new Tuple2<>(nameDesc.substring(0, split), nameDesc.substring(split));
    }

    static String staticDesc(String owner, String desc) {
        Type method = getMethodType(desc);
        Type result = method.getReturnType();
        Type receiver = getType("L" + owner + ";");
        Type[] args = method.getArgumentTypes();
        Type[] staticArgs = new Type[args.length + 1];
        staticArgs[0] = receiver;
        System.arraycopy(args, 0, staticArgs, 1, args.length);
        return getMethodDescriptor(result, staticArgs);
    }

    static void finishBridgeCall(MethodVisitor mv, String mvdesc, int opcode, String owner, String name, String desc) {
        Type[] args = getArgumentTypes(mvdesc);
        Type result = getReturnType(mvdesc);
        int localIndex = 1;
        for (Type arg : args) {
            mv.visitVarInsn(arg.getOpcode(ILOAD), localIndex);
            localIndex += StackAnalyser$.MODULE$.width(arg);
        }
        mv.visitMethodInsn(opcode, owner, name, desc, opcode == INVOKEINTERFACE);
        mv.visitInsn(result.getOpcode(IRETURN));
        mv.visitMaxs(
                Math.max(
                        StackAnalyser$.MODULE$.width(Predef$.MODULE$.wrapRefArray(args)) + 1,
                        StackAnalyser$.MODULE$.width(result)),
                StackAnalyser$.MODULE$.width(Predef$.MODULE$.wrapRefArray(args)) + 1);
    }

    static void writeBridge(MethodVisitor mv, String mvdesc, int opcode, String owner, String name, String desc) {
        mv.visitVarInsn(ALOAD, 0);
        finishBridgeCall(mv, mvdesc, opcode, owner, name, desc);
    }

    static void writeStaticBridge(MethodNode mv, String name, MixinInfo mixin) {
        writeBridge(mv, mv.desc, INVOKESTATIC, mixin.tname(), name, staticDesc(mixin.name(), mv.desc));
    }
}
