package codechicken.multipart.asm;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.asm.InsnComparator;
import codechicken.lib.asm.InsnListSection;
import codechicken.multipart.asm.ASMMixinCompiler.FieldMixin;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.Function1;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.TraversableLike;
import scala.collection.immutable.Map;
import scala.collection.mutable.Buffer;
import scala.collection.mutable.ListBuffer;
import scala.runtime.AbstractFunction1;

/** Java mixin rewriting behind the retained compiler singleton and Scala metadata models. */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class JavaTraitRegistration {

    private JavaTraitRegistration() {}

    static void register(ClassNode input, scala.collection.mutable.Map<String, MixinInfo> mixins) {
        if ((input.access & ACC_INTERFACE) != 0) throw new IllegalArgumentException(
                "Cannot register java interface " + input.name
                        + " as a mixin trait. Try register passThroughInterface");
        if (!input.innerClasses.isEmpty()) throw new IllegalArgumentException(
                "Inner classes are not permitted for " + input.name + " as a java mixin trait. Use scala");
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        Option<MixinInfo> parentTrait = compiler.getMixinInfo(input.superName);
        Buffer<FieldNode> sourceFields = JavaConversions.asScalaBuffer(input.fields);
        Map<String, FieldMixin> fields = scala.collection.immutable.Map$.MODULE$.empty();
        for (FieldNode field : iterable(sourceFields))
            fields = fields.$plus(new Tuple2<>(field.name, new FieldMixin(field.name, field.desc, field.access)));
        ListBuffer<String> supers = new ListBuffer<>();
        ListBuffer<MethodNode> methods = new ListBuffer<>();
        java.util.Set<String> methodSignatures = new LinkedHashSet<>();
        for (MethodNode method : input.methods) methodSignatures.add(method.name + method.desc);

        ClassNode implementation = new ClassNode();
        implementation.visit(V1_6, ACC_ABSTRACT | ACC_PUBLIC, input.name + "$class", null, "java/lang/Object", null);
        implementation.sourceFile = input.sourceFile;
        ClassNode contract = new ClassNode();
        List<String> interfaces = new ArrayList<>(input.interfaces);
        if (parentTrait.isDefined()) interfaces.add(parentTrait.get().name());
        contract.visit(
                V1_6,
                ACC_INTERFACE | ACC_ABSTRACT | ACC_PUBLIC,
                input.name,
                null,
                "java/lang/Object",
                new LinkedHashSet<>(interfaces).toArray(new String[0]));

        for (FieldMixin field : iterable(fields.values())) {
            String name = field.accessName(input.name);
            contract.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, name, "()" + field.desc(), null, null);
            contract.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, name + "_$eq", "(" + field.desc() + ")V", null, null);
        }

        Context context = new Context(input, implementation, contract, fields, methodSignatures, supers, methods);
        Buffer<MethodNode> sourceMethods = JavaConversions.asScalaBuffer(input.methods);
        Buffer<MethodNode> selected = (Buffer<MethodNode>) ((TraversableLike) sourceMethods)
                .filterNot(fn(context::isGeneratedFieldAccessor));
        selected.foreach(fn(method -> {
            context.convertMethod(method);
            return scala.runtime.BoxedUnit.UNIT;
        }));

        compiler.define(implementation.name, ASMHelper.createBytes(implementation, 0));
        compiler.define(contract.name, ASMHelper.createBytes(contract, 0));
        mixins.put(
                contract.name,
                new MixinInfo(
                        contract.name,
                        parentTrait.isDefined() ? parentTrait.get().parent() : input.superName,
                        JavaConversions.asScalaBuffer(
                                parentTrait.isDefined() ? java.util.Collections.singletonList(parentTrait.get())
                                        : java.util.Collections.<MixinInfo>emptyList())
                                .toList(),
                        fields.values().toSeq(),
                        methods,
                        supers));
    }

    private static final class Context {

        final ClassNode input;
        final ClassNode implementation;
        final ClassNode contract;
        final Map<String, FieldMixin> fields;
        final java.util.Set<String> methodSignatures;
        final ListBuffer<String> supers;
        final ListBuffer<MethodNode> methods;

        Context(ClassNode input, ClassNode implementation, ClassNode contract, Map<String, FieldMixin> fields,
                java.util.Set<String> methodSignatures, ListBuffer<String> supers, ListBuffer<MethodNode> methods) {
            this.input = input;
            this.implementation = implementation;
            this.contract = contract;
            this.fields = fields;
            this.methodSignatures = methodSignatures;
            this.supers = supers;
            this.methods = methods;
        }

        String fieldName(String name) {
            return fields.apply(name).accessName(input.name);
        }

        MethodInsnNode superInstruction(MethodInsnNode instruction) {
            String bridge = input.name.replace('/', '$') + "$$super$" + instruction.name;
            if (!supers.contains(instruction.name + instruction.desc)) {
                contract.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, bridge, instruction.desc, null, null);
                supers.$plus$eq(instruction.name + instruction.desc);
            }
            return new MethodInsnNode(INVOKEINTERFACE, input.name, bridge, instruction.desc, true);
        }

        MethodNode staticClone(MethodNode source, String name, int access) {
            MethodNode result = (MethodNode) implementation.visitMethod(
                    access | ACC_STATIC,
                    name,
                    ASMMixinCompiler$.MODULE$.staticDesc(input.name, source.desc),
                    null,
                    (String[]) source.exceptions.toArray(new String[0]));
            ASMHelper.copy(source, result);
            return result;
        }

        void staticTransform(MethodNode method, MethodNode base) {
            StackAnalyser stack = new StackAnalyser(Type.getType(Type.getObjectType(input.name).getDescriptor()), base);
            InsnList instructions = method.instructions;
            AbstractInsnNode instruction = instructions.getFirst();
            while (instruction != null) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (instruction.getOpcode() == GETFIELD) instruction = replace(
                            instructions,
                            instruction,
                            new MethodInsnNode(
                                    INVOKEINTERFACE,
                                    input.name,
                                    fieldName(field.name),
                                    "()" + field.desc,
                                    true));
                    else if (instruction.getOpcode() == PUTFIELD) instruction = replace(
                            instructions,
                            instruction,
                            new MethodInsnNode(
                                    INVOKEINTERFACE,
                                    input.name,
                                    fieldName(field.name) + "_$eq",
                                    "(" + field.desc + ")V",
                                    true));
                } else if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (instruction.getOpcode() == INVOKESPECIAL) {
                        if (ASMMixinCompiler$.MODULE$.getSuper(call, stack).isDefined())
                            instruction = replace(instructions, instruction, superInstruction(call));
                    } else if (instruction.getOpcode() == INVOKEVIRTUAL && Objects.equals(call.owner, input.name)) {
                        if (methodSignatures.contains(call.name + call.desc)) instruction = replace(
                                instructions,
                                instruction,
                                new MethodInsnNode(INVOKEINTERFACE, call.owner, call.name, call.desc, true));
                        else {
                            Type methodType = Type.getMethodType(call.desc);
                            StackAnalyser.StackEntry receiver = stack.peek(
                                    StackAnalyser.width(
                                            JavaConversions.asScalaBuffer(
                                                    java.util.Arrays.asList(methodType.getArgumentTypes()))));
                            instructions.insert(
                                    receiver.insn(),
                                    new TypeInsnNode(CHECKCAST, Type.getObjectType(input.superName).getDescriptor()));
                            call.owner = input.superName;
                        }
                    }
                }
                stack.visitInsn(instruction);
                instruction = instruction.getNext();
            }
        }

        void convertMethod(MethodNode source) {
            if (Objects.equals(source.name, "<clinit>")) throw new IllegalArgumentException(
                    "Static initialisers are not permitted " + input.name + " as a mixin trait");
            if (Objects.equals(source.name, "<init>")) {
                if (!Objects.equals(source.desc, "()V")) throw new IllegalArgumentException(
                        "Constructor arguments are not permitted " + input.name + " as a mixin trait");
                MethodNode result = staticClone(source, "$init$", ACC_PUBLIC);
                if ((input.access & ACC_ABSTRACT) != 0) removeAbstractSuperConstructor(result, source);
                else removeSuperConstructor(result);
                staticTransform(result, source);
                return;
            }
            if ((source.access & ACC_ABSTRACT) != 0) {
                contract.visitMethod(
                        ACC_PUBLIC | ACC_ABSTRACT,
                        source.name,
                        source.desc,
                        null,
                        (String[]) source.exceptions.toArray(new String[0]));
                return;
            }
            if ((source.access & ACC_PRIVATE) == 0) {
                MethodNode exposed = (MethodNode) contract.visitMethod(
                        ACC_PUBLIC | ACC_ABSTRACT,
                        source.name,
                        source.desc,
                        null,
                        (String[]) source.exceptions.toArray(new String[0]));
                methods.$plus$eq(exposed);
            }
            int access = (source.access & ACC_PRIVATE) == 0 ? ACC_PUBLIC : ACC_PRIVATE;
            staticTransform(staticClone(source, source.name, access), source);
        }

        void removeSuperConstructor(MethodNode constructor) {
            InsnListSection expected = new InsnListSection();
            expected.add(new VarInsnNode(ALOAD, 0));
            expected.add(new MethodInsnNode(INVOKESPECIAL, input.superName, "<init>", "()V", false));
            InsnListSection actual = new InsnListSection(constructor.instructions);
            InsnListSection found = InsnComparator.matches(actual, expected, java.util.Collections.emptySet());
            if (found == null)
                throw new IllegalArgumentException("Invalid constructor insn sequence " + input.name + "\n" + actual);
            found.trim(java.util.Collections.emptySet()).remove();
        }

        void removeAbstractSuperConstructor(MethodNode constructor, MethodNode source) {
            StackAnalyser stack = new StackAnalyser(Type.getObjectType(input.name), source);
            AbstractInsnNode instruction = constructor.instructions.getFirst();
            while (instruction != null) {
                if (instruction.getOpcode() == INVOKESPECIAL && instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (Objects.equals(call.owner, input.superName) && Objects.equals(call.name, "<init>")) {
                        int argumentWidth = 0;
                        for (Type argument : Type.getArgumentTypes(call.desc)) argumentWidth += argument.getSize();
                        StackAnalyser.StackEntry receiver = stack.peek(argumentWidth);
                        if (receiver instanceof StackAnalyser.Load
                                && ((StackAnalyser.Load) receiver).e() instanceof StackAnalyser.This) {
                            removeRange(constructor.instructions, receiver.insn(), instruction);
                            return;
                        }
                    }
                }
                stack.visitInsn(instruction);
                instruction = instruction.getNext();
            }
            throw new IllegalArgumentException(
                    "Invalid constructor insn sequence " + input.name
                            + "\n"
                            + new InsnListSection(constructor.instructions));
        }

        boolean isGeneratedFieldAccessor(MethodNode method) {
            for (FieldMixin field : iterable(fields.values())) {
                String name = field.accessName(input.name);
                if (Objects.equals(method.name, name) && Objects.equals(method.desc, "()" + field.desc())
                        || Objects.equals(method.name, name + "_$eq")
                                && Objects.equals(method.desc, "(" + field.desc() + ")V"))
                    return true;
            }
            return false;
        }
    }

    private static AbstractInsnNode replace(InsnList instructions, AbstractInsnNode old, AbstractInsnNode replacement) {
        instructions.insert(old, replacement);
        instructions.remove(old);
        return replacement;
    }

    private static void removeRange(InsnList instructions, AbstractInsnNode first, AbstractInsnNode last) {
        AbstractInsnNode after = last.getNext();
        AbstractInsnNode instruction = first;
        while (instruction != after) {
            AbstractInsnNode next = instruction.getNext();
            instructions.remove(instruction);
            instruction = next;
        }
    }

    private static <A> java.lang.Iterable<A> iterable(scala.collection.Iterable<A> values) {
        return JavaConversions.asJavaIterable(values);
    }

    private static <A, B> Function1<A, B> fn(Function<A, B> function) {
        return new AbstractFunction1<A, B>() {

            @Override
            public B apply(A value) {
                return function.apply(value);
            }
        };
    }
}
