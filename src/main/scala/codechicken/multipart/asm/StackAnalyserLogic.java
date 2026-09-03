package codechicken.multipart.asm;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import codechicken.multipart.asm.StackAnalyser.ArrayLength;
import codechicken.multipart.asm.StackAnalyser.ArrayLoad;
import codechicken.multipart.asm.StackAnalyser.BinaryOp;
import codechicken.multipart.asm.StackAnalyser.Cast;
import codechicken.multipart.asm.StackAnalyser.CaughtException;
import codechicken.multipart.asm.StackAnalyser.Const;
import codechicken.multipart.asm.StackAnalyser.GetField;
import codechicken.multipart.asm.StackAnalyser.Invoke;
import codechicken.multipart.asm.StackAnalyser.Load;
import codechicken.multipart.asm.StackAnalyser.LocalEntry;
import codechicken.multipart.asm.StackAnalyser.New;
import codechicken.multipart.asm.StackAnalyser.NewArray;
import codechicken.multipart.asm.StackAnalyser.NewMultiArray;
import codechicken.multipart.asm.StackAnalyser.PrimitiveCast;
import codechicken.multipart.asm.StackAnalyser.ReturnAddress;
import codechicken.multipart.asm.StackAnalyser.StackEntry;
import codechicken.multipart.asm.StackAnalyser.Store;
import codechicken.multipart.asm.StackAnalyser.UnaryOp;
import scala.MatchError;
import scala.None$;
import scala.Option;
import scala.Predef;
import scala.Some;

/** Analyser control flow behind the retained Scala model and default-argument API. */
final class StackAnalyserLogic {

    private StackAnalyserLogic() {}

    static void setL(StackAnalyser a, int i, LocalEntry entry) {
        while (i + entry.getType().getSize() > a.locals().size()) a.locals().$plus$eq(null);
        a.locals().update(i, entry);
        if (entry.getType().getSize() == 2) a.locals().update(i + 1, entry);
    }

    static StackEntry pop(StackAnalyser a, int i) {
        StackEntry entry = a._pop(i);
        if (entry.getType().getSize() == 2) {
            StackEntry other = a.peek(i);
            if (!(other == null ? entry == null : other.equals(entry))) {
                throw new IllegalStateException("Wide stack entry elems don't match (" + entry + "," + a.peek(i));
            }
            a._pop(i);
        }
        return entry;
    }

    static void insert(StackAnalyser a, int i, StackEntry entry) {
        if (entry.getType().getSize() == 0) return;
        a.stack().insert(a.stack().size() - i, Predef.wrapRefArray(new StackEntry[] { entry }));
        if (entry.getType().getSize() == 2) {
            a.stack().insert(a.stack().size() - i, Predef.wrapRefArray(new StackEntry[] { entry }));
        }
    }

    static StackEntry[] popArgs(StackAnalyser a, String desc) {
        StackEntry[] args = new StackEntry[getType(desc).getArgumentTypes().length];
        for (int i = 0; i < args.length; i++) args[args.length - i - 1] = pop(a);
        return args;
    }

    // These calls must retain virtual dispatch through Scala's default-argument getters.
    private static StackEntry pop(StackAnalyser a) {
        return a.pop(a.pop$default$1());
    }

    private static StackEntry peek(StackAnalyser a) {
        return a.peek(a.peek$default$1());
    }

    private static void rawPop(StackAnalyser a) {
        a._pop(a._pop$default$1());
    }

    static void visitInsn(StackAnalyser a, AbstractInsnNode insn) {
        if (insn instanceof InsnNode) {
            int op = insn.getOpcode();
            switch (op) {
                case ACONST_NULL:
                    a.push(new Const(null, insn));
                    break;
                case ICONST_M1:
                    a.push(new Const(-1, insn));
                    break;
                case ICONST_0:
                    a.push(new Const(0, insn));
                    break;
                case ICONST_1:
                    a.push(new Const(1, insn));
                    break;
                case ICONST_2:
                    a.push(new Const(2, insn));
                    break;
                case ICONST_3:
                    a.push(new Const(3, insn));
                    break;
                case ICONST_4:
                    a.push(new Const(4, insn));
                    break;
                case ICONST_5:
                    a.push(new Const(5, insn));
                    break;
                case LCONST_0:
                    a.push(new Const(0L, insn));
                    break;
                case LCONST_1:
                    a.push(new Const(1L, insn));
                    break;
                case FCONST_0:
                    a.push(new Const(0f, insn));
                    break;
                case FCONST_1:
                    a.push(new Const(1f, insn));
                    break;
                case FCONST_2:
                    a.push(new Const(2f, insn));
                    break;
                case DCONST_0:
                    a.push(new Const(0d, insn));
                    break;
                case DCONST_1:
                    a.push(new Const(1d, insn));
                    break;
                case POP:
                    rawPop(a);
                    break;
                case POP2:
                    rawPop(a);
                    rawPop(a);
                    break;
                case DUP:
                    a.push(peek(a));
                    break;
                case DUP_X1:
                    a.insert(2, peek(a));
                    break;
                case DUP_X2:
                    a.insert(3, peek(a));
                    break;
                case DUP2:
                    a.push(a.peek(1));
                    a.push(a.peek(1));
                    break;
                case DUP2_X1:
                    a.insert(3, a.peek(1));
                    a.insert(3, peek(a));
                    break;
                case DUP2_X2:
                    a.insert(4, a.peek(1));
                    a.insert(4, peek(a));
                    break;
                case SWAP:
                    a.push(a.pop(1));
                    break;
                // Preserve the reference's inferred types, including conversions to int reporting double.
                case L2I:
                case F2I:
                case D2I:
                    a.push(new PrimitiveCast(pop(a), DOUBLE_TYPE, insn));
                    break;
                case I2L:
                case F2L:
                case D2L:
                    a.push(new PrimitiveCast(pop(a), LONG_TYPE, insn));
                    break;
                case I2F:
                case L2F:
                case D2F:
                    a.push(new PrimitiveCast(pop(a), FLOAT_TYPE, insn));
                    break;
                case I2D:
                case L2D:
                case F2D:
                    a.push(new PrimitiveCast(pop(a), DOUBLE_TYPE, insn));
                    break;
                case I2B:
                    a.push(new PrimitiveCast(pop(a), BYTE_TYPE, insn));
                    break;
                case I2C:
                    a.push(new PrimitiveCast(pop(a), CHAR_TYPE, insn));
                    break;
                case I2S:
                    a.push(new PrimitiveCast(pop(a), SHORT_TYPE, insn));
                    break;
                case ARRAYLENGTH:
                    a.push(new ArrayLength(pop(a), insn));
                    break;
                case ATHROW:
                case MONITORENTER:
                case MONITOREXIT:
                    pop(a);
                    break;
                default:
                    if (op >= IALOAD && op <= SALOAD) {
                        a.push(new ArrayLoad(pop(a), pop(a), insn));
                    } else if (op >= IASTORE && op <= SASTORE) {
                        pop(a);
                        pop(a);
                        pop(a);
                    } else if (op >= IADD && op <= DREM || op >= ISHL && op <= LXOR || op >= LCMP && op <= DCMPG) {
                        a.push(new BinaryOp(op, pop(a), pop(a), insn));
                    } else if (op >= INEG && op <= DNEG) {
                        a.push(new UnaryOp(op, pop(a), insn));
                    } else if (op >= IRETURN && op <= ARETURN) {
                        pop(a);
                    }
            }
        } else if (insn instanceof IntInsnNode) {
            IntInsnNode intInsn = (IntInsnNode) insn;
            switch (insn.getOpcode()) {
                case BIPUSH:
                    a.push(new Const((byte) intInsn.operand, insn));
                    break;
                case SIPUSH:
                    a.push(new Const((short) intInsn.operand, insn));
                    break;
                default:
                    throw new MatchError(insn.getOpcode());
            }
        } else if (insn instanceof LdcInsnNode) {
            if (insn.getOpcode() != LDC) throw new MatchError(insn.getOpcode());
            a.push(new Const(((LdcInsnNode) insn).cst, insn));
        } else if (insn instanceof VarInsnNode) {
            VarInsnNode varInsn = (VarInsnNode) insn;
            int op = insn.getOpcode();
            if (op >= ILOAD && op <= ALOAD) {
                a.push(new Load(a.locals().apply(varInsn.var), insn));
            } else if (op >= ISTORE && op <= ASTORE) {
                a.setL(varInsn.var, new Store(pop(a), insn));
            } else {
                throw new MatchError(op);
            }
        } else if (insn instanceof IincInsnNode) {
            IincInsnNode incInsn = (IincInsnNode) insn;
            if (insn.getOpcode() != IINC) throw new MatchError(insn.getOpcode());
            a.setL(
                    incInsn.var,
                    new Store(
                            new BinaryOp(
                                    IINC,
                                    new Const(incInsn.incr, insn),
                                    new Load(a.locals().apply(incInsn.var), insn),
                                    insn),
                            insn));
        } else if (insn instanceof JumpInsnNode) {
            int op = insn.getOpcode();
            if (op >= IFEQ && op <= IFLE || op == IFNULL || op == IFNONNULL) {
                pop(a);
            } else if (op >= IF_ICMPEQ && op <= IF_ACMPNE) {
                pop(a);
                pop(a);
            } else if (op == JSR) {
                a.push(new ReturnAddress(insn));
            } else if (op != GOTO) {
                throw new MatchError(op);
            }
        } else if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
            pop(a);
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode field = (FieldInsnNode) insn;
            switch (insn.getOpcode()) {
                case GETSTATIC:
                    a.push(new GetField(null, field, insn));
                    break;
                case PUTSTATIC:
                    pop(a);
                    break;
                case GETFIELD:
                    a.push(new GetField(pop(a), field, insn));
                    break;
                case PUTFIELD:
                    pop(a);
                    pop(a);
                    break;
                default:
                    throw new MatchError(insn.getOpcode());
            }
        } else if (insn instanceof MethodInsnNode) {
            MethodInsnNode method = (MethodInsnNode) insn;
            switch (insn.getOpcode()) {
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                case INVOKEINTERFACE:
                    a.push(new Invoke(insn.getOpcode(), a.popArgs(method.desc), pop(a), method, insn));
                    break;
                case INVOKESTATIC:
                    a.push(new Invoke(insn.getOpcode(), a.popArgs(method.desc), null, method, insn));
                    break;
                default:
                    throw new MatchError(insn.getOpcode());
            }
        } else if (insn instanceof TypeInsnNode) {
            TypeInsnNode typeInsn = (TypeInsnNode) insn;
            switch (insn.getOpcode()) {
                case NEW:
                    a.push(new New(getObjectType(typeInsn.desc), insn));
                    break;
                case NEWARRAY:
                    a.push(new NewArray(pop(a), getObjectType(typeInsn.desc), insn));
                    break;
                case ANEWARRAY:
                    a.push(new NewArray(pop(a), getObjectType("[" + typeInsn.desc), insn));
                    break;
                case CHECKCAST:
                    a.push(new Cast(pop(a), getObjectType(typeInsn.desc), insn));
                    break;
                case INSTANCEOF:
                    a.push(new UnaryOp(INSTANCEOF, pop(a), insn));
                    break;
                default:
                    throw new MatchError(insn.getOpcode());
            }
        } else if (insn instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode multi = (MultiANewArrayInsnNode) insn;
            StackEntry[] sizes = new StackEntry[multi.dims];
            for (int i = 0; i < sizes.length; i++) sizes[i] = pop(a);
            a.push(new NewMultiArray(sizes, getType(multi.desc), insn));
        } else if (insn instanceof LabelNode) {
            Option<TryCatchBlockNode> handler = a.codechicken$multipart$asm$StackAnalyser$$catchHandlers()
                    .get((LabelNode) insn);
            if (handler instanceof Some) {
                a.push(new CaughtException(Type.getType(((Some<TryCatchBlockNode>) handler).x().type), insn));
            } else if (!None$.MODULE$.equals(handler)) {
                throw new MatchError(handler);
            }
        }
    }
}
