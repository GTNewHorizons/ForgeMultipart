package codechicken.multipart.asm;

import static org.objectweb.asm.Opcodes.*;

import java.util.Objects;
import java.util.function.Function;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.FieldMixin;
import codechicken.multipart.asm.ASMMixinCompiler.MethodInfo;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.Function1;
import scala.Option;
import scala.Option$;
import scala.Tuple2;
import scala.collection.GenTraversableOnce;
import scala.collection.Iterable;
import scala.collection.Iterable$;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.Seq$;
import scala.collection.TraversableLike;
import scala.collection.mutable.ListBuffer;
import scala.collection.mutable.Map;
import scala.collection.mutable.Set;
import scala.collection.mutable.Set$;
import scala.runtime.AbstractFunction1;

/** Composite generation behind the retained Scala entry point and metadata models. */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class MixinClassGenerator {

    private MixinClassGenerator() {}

    static Class<?> mixinClasses(String name, String superClass, Seq<String> traits, Map<String, MixinInfo> mixins,
            Function1<ClassInfo, Iterable<ClassInfo>> parents) throws ClassNotFoundException {
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        if (traits.isEmpty())
            return compiler.cl().findClass(compiler.getClassInfo(superClass).name().replace('/', '.'));

        long startTime = System.currentTimeMillis();
        Seq<MixinInfo> baseTraits = map(traits, mixins::apply);
        Seq<MixinInfo> mixinInfos = flatMap(baseTraits, MixinInfo::linearise).distinct();
        ClassInfo baseInfo = compiler.getClassInfo(superClass);
        Seq<ClassInfo> traitInfos = map(mixinInfos, i -> compiler.getClassInfo(i.name()));
        ClassNode node = new ClassNode();
        node.visit(
                V1_6,
                ACC_PUBLIC,
                name,
                null,
                superClass,
                JavaConversions.seqAsJavaList(map(baseTraits, MixinInfo::name)).toArray(new String[0]));

        MethodInfo constructor = baseInfo.methods().find(fn(m -> Objects.equals(m.name(), "<init>"))).get();
        MethodNode init = (MethodNode) node.visitMethod(ACC_PUBLIC, "<init>", constructor.desc(), null, null);
        compiler.writeBridge(init, constructor.desc(), INVOKESPECIAL, superClass, "<init>", constructor.desc());
        init.instructions.remove(init.instructions.getLast());

        ListBuffer<MixinInfo> previous = new ListBuffer<>();
        for (MixinInfo trait : JavaConversions.seqAsJavaList(mixinInfos)) {
            init.visitVarInsn(ALOAD, 0);
            init.visitMethodInsn(INVOKESTATIC, trait.tname(), "$init$", "(L" + trait.name() + ";)V", false);
            for (FieldMixin field : JavaConversions.seqAsJavaList(trait.fields())) {
                FieldNode storage = (FieldNode) node
                        .visitField(ACC_PRIVATE, field.accessName(trait.name()), field.desc(), null, null);
                Type type = Type.getType(storage.desc);
                MethodVisitor method = node.visitMethod(ACC_PUBLIC, storage.name, "()" + field.desc(), null, null);
                method.visitVarInsn(ALOAD, 0);
                method.visitFieldInsn(GETFIELD, name, storage.name, storage.desc);
                method.visitInsn(type.getOpcode(IRETURN));
                method.visitMaxs(1, 1);
                method = node.visitMethod(ACC_PUBLIC, storage.name + "_$eq", "(" + field.desc() + ")V", null, null);
                method.visitVarInsn(ALOAD, 0);
                method.visitVarInsn(type.getOpcode(ILOAD), 1);
                method.visitFieldInsn(PUTFIELD, name, storage.name, storage.desc);
                method.visitInsn(RETURN);
                method.visitMaxs(StackAnalyser.width(type) + 1, StackAnalyser.width(type) + 1);
            }
            for (String signature : JavaConversions.seqAsJavaList(trait.supers())) {
                Tuple2<String, String> split = compiler.seperateDesc(signature);
                String methodName = split._1();
                String descriptor = split._2();
                MethodNode method = (MethodNode) node.visitMethod(
                        ACC_PUBLIC,
                        trait.name().replace('/', '$') + "$$super$" + methodName,
                        descriptor,
                        null,
                        null);
                Option<MixinInfo> prior = ((Seq<MixinInfo>) previous.reverse()).find(
                        fn(
                                t -> t.methods().exists(
                                        fn(
                                                m -> Objects.equals(m.name, methodName)
                                                        && Objects.equals(m.desc, descriptor)))));
                if (prior.isDefined()) compiler.writeStaticBridge(method, methodName, prior.get());
                else compiler.writeBridge(
                        method,
                        descriptor,
                        INVOKESPECIAL,
                        baseInfo.findPublicImpl(methodName, descriptor).get().owner().name(),
                        methodName,
                        descriptor);
            }
            previous.$plus$eq(trait);
        }

        Set<String> signatures = Set$.MODULE$.empty();
        for (MixinInfo trait : JavaConversions.seqAsJavaList(mixinInfos.reverse())) {
            for (MethodNode method : JavaConversions.seqAsJavaList(trait.methods())) {
                if (!signatures.apply(method.name + method.desc)) {
                    MethodNode bridge = (MethodNode) node.visitMethod(
                            ACC_PUBLIC,
                            method.name,
                            method.desc,
                            null,
                            (String[]) method.exceptions.toArray(new String[0]));
                    compiler.writeStaticBridge(bridge, method.name, trait);
                    signatures.$plus$eq(method.name + method.desc);
                }
            }
        }
        init.visitInsn(RETURN);

        Seq<ClassInfo> allParents = flatMap(prepend(baseInfo, traitInfos), parents::apply).distinct();
        Seq<MethodInfo> allMethods = flatMap(allParents, ClassInfo::methods);
        for (String signature : JavaConversions.seqAsJavaList(signatures.toSeq())) {
            Tuple2<String, String> split = compiler.seperateDesc(signature);
            String methodName = split._1();
            String descriptor = split._2();
            String parameters = descriptor.substring(0, descriptor.lastIndexOf(')') + 1);
            Seq<MethodInfo> matching = (Seq<MethodInfo>) allMethods
                    .filter(fn(m -> Objects.equals(m.name(), methodName) && m.desc().startsWith(parameters)));
            for (MethodInfo method : JavaConversions.seqAsJavaList(matching)) {
                if (!signatures.apply(method.name() + method.desc())) {
                    MethodNode bridge = (MethodNode) node.visitMethod(
                            ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE,
                            method.name(),
                            method.desc(),
                            null,
                            method.exceptions());
                    compiler.writeBridge(bridge, bridge.desc, INVOKEVIRTUAL, node.name, methodName, descriptor);
                    signatures.$plus$eq(method.name() + method.desc());
                }
            }
        }

        Class<?> result = compiler.define(name, ASMHelper.createBytes(node, 0));
        DebugPrinter$.MODULE$.logger().debug(
                "Generation [" + superClass
                        + " with "
                        + traits.mkString(", ")
                        + "] took "
                        + (System.currentTimeMillis() - startTime)
                        + "ms");
        return result;
    }

    static Iterable<ClassInfo> allParents(ClassInfo info) {
        Iterable<ClassInfo> parents = (Iterable<ClassInfo>) ((TraversableLike) Option$.MODULE$
                .option2Iterable(info.superClass())).$plus$plus(info.interfaces(), Iterable$.MODULE$.canBuildFrom());
        return prepend(info, flatMap(parents.toSeq(), MixinClassGenerator::allParents));
    }

    private static <A, B> Seq<B> map(Seq<A> values, Function<A, B> function) {
        return (Seq<B>) ((TraversableLike) values).map(fn(function), Seq$.MODULE$.canBuildFrom());
    }

    private static <A, B> Seq<B> flatMap(Seq<A> values, Function<A, GenTraversableOnce<B>> function) {
        return (Seq<B>) ((TraversableLike) values).flatMap(fn(function), Seq$.MODULE$.canBuildFrom());
    }

    private static <A> Seq<A> prepend(A value, Seq<A> values) {
        return (Seq<A>) ((scala.collection.SeqLike) values).$plus$colon(value, Seq$.MODULE$.canBuildFrom());
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
