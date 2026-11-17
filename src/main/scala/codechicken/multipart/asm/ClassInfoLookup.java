package codechicken.multipart.asm;

import java.lang.reflect.Method;
import java.util.Objects;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo$;
import codechicken.multipart.asm.ASMMixinCompiler.MethodInfo;
import codechicken.multipart.asm.StackAnalyser.Load;
import codechicken.multipart.asm.StackAnalyser.StackEntry;
import codechicken.multipart.asm.StackAnalyser.This;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import scala.Array$;
import scala.Function1;
import scala.Function3;
import scala.Option;
import scala.Predef;
import scala.Predef.DummyImplicit$;
import scala.collection.GenTraversableOnce;
import scala.collection.Iterable;
import scala.collection.Iterable$;
import scala.collection.IterableView;
import scala.collection.IterableView$;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.TraversableLike;
import scala.collection.TraversableOnce;
import scala.collection.immutable.IndexedSeq$;
import scala.collection.immutable.List$;
import scala.collection.immutable.Set;
import scala.collection.mutable.Buffer;
import scala.collection.mutable.Buffer$;
import scala.collection.mutable.Map;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;

/**
 * Metadata lookup behind the retained Scala nested models, construction callbacks and view bridge. Raw traversal
 * interfaces disambiguate Scala's covariant generic methods while preserving virtual collection dispatch.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class ClassInfoLookup {

    private ClassInfoLookup() {}

    static IterableView<MethodInfo, Iterable<?>> parentMethods(IterableView<ClassInfo, ?> parents) {
        return (IterableView<MethodInfo, Iterable<?>>) ((TraversableLike) parents)
                .flatMap(new AbstractFunction1<ClassInfo, GenTraversableOnce<MethodInfo>>() {

                    @Override
                    public GenTraversableOnce<MethodInfo> apply(ClassInfo parent) {
                        return parent.allMethods();
                    }
                }, IterableView$.MODULE$.canBuildFrom());
    }

    static Iterable<MethodInfo> allMethods(ClassInfo info) {
        return (Iterable<MethodInfo>) ((TraversableLike) info.methods())
                .$plus$plus(info.parentMethods(), Iterable$.MODULE$.canBuildFrom());
    }

    static Option<MethodInfo> findPublicImpl(ClassInfo info, final String name, final String desc) {
        return info.allMethods().find(new AbstractFunction1<MethodInfo, Object>() {

            @Override
            public Object apply(MethodInfo method) {
                return Objects.equals(method.name(), name) && Objects.equals(method.desc(), desc)
                        && !method.isAbstract()
                        && !method.isPrivate();
            }
        });
    }

    static ClassInfo getClassInfo(Map<String, ClassInfo> cache, final String name) {
        return cache.getOrElseUpdate(name, new AbstractFunction0<ClassInfo>() {

            @Override
            public ClassInfo apply() {
                return ClassInfo$.MODULE$.obtainInfo(name);
            }
        });
    }

    static Iterable<ClassInfo> reflectionInterfaces(Class<?> type) {
        return mapArray(type.getInterfaces(), new AbstractFunction1<Class<?>, ClassInfo>() {

            @Override
            public ClassInfo apply(Class<?> parent) {
                return ASMMixinCompiler$.MODULE$.getClassInfo(parent);
            }
        });
    }

    static Iterable<MethodInfo> reflectionMethods(Class<?> type, Function1<Method, MethodInfo> factory) {
        return mapArray(type.getMethods(), factory);
    }

    private static <A, B> Iterable<B> mapArray(A[] values, Function1<A, B> function) {
        return (Iterable<B>) ((TraversableLike) Predef.refArrayOps(values))
                .map(function, Array$.MODULE$.fallbackCanBuildFrom(DummyImplicit$.MODULE$.dummyImplicit()));
    }

    static Seq<ClassInfo> nodeInterfaces(ClassNode node) {
        return (Seq<ClassInfo>) ((TraversableLike) JavaConversions.asScalaBuffer(node.interfaces))
                .map(resolveName(), Buffer$.MODULE$.canBuildFrom());
    }

    static Iterable<MethodInfo> nodeMethods(Buffer<MethodNode> methods, Function1<MethodNode, MethodInfo> factory) {
        return (Iterable<MethodInfo>) ((TraversableLike) methods).map(factory, Buffer$.MODULE$.canBuildFrom());
    }

    static Seq<ClassInfo> scalaInterfaces(Object symbol) {
        return (Seq<ClassInfo>) ((TraversableLike) ((ScalaSignature.ClassSymbolRef) symbol).jInterfaces())
                .map(resolveName(), List$.MODULE$.canBuildFrom());
    }

    private static Function1<String, ClassInfo> resolveName() {
        return new AbstractFunction1<String, ClassInfo>() {

            @Override
            public ClassInfo apply(String name) {
                return ASMMixinCompiler$.MODULE$.getClassInfo(name);
            }
        };
    }

    static String[] exceptionNames(Method method) {
        Class<?>[] exceptions = method.getExceptionTypes();
        String[] names = new String[exceptions.length];
        for (int i = 0; i < names.length; i++) names[i] = ASMImplicits.nodeName(exceptions[i].getName());
        return names;
    }

    static String[] exceptionNames(java.util.List<String> exceptions) {
        return exceptions.toArray(new String[0]);
    }

    static ClassInfo obtainInfo(String name, Function3<ClassNode, ScalaSignature, Object, ClassInfo> scalaFactory,
            Function1<ClassNode, ClassInfo> nodeFactory, Function1<Class<?>, ClassInfo> reflectionFactory) {
        if (name == null) return null;
        ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        if (name.endsWith("$")) {
            ClassNode base = compiler.classNode(name.substring(0, name.length() - 1));
            if (base != null) {
                Option<ClassInfo> info = scalaInfo(base, true, scalaFactory);
                if (info.isDefined()) return info.get();
            }
        }
        ClassNode node = compiler.classNode(name);
        if (node == null) {
            Class<?> type;
            try {
                type = compiler.cl().findClass(name.replace('/', '.'));
            } catch (ClassNotFoundException exception) {
                return ClassInfoLookup.<ClassInfo, RuntimeException>throwUnchecked(exception);
            }
            return type == null ? null : reflectionFactory.apply(type);
        }
        Option<ClassInfo> info = scalaInfo(node, false, scalaFactory);
        return info.isDefined() ? info.get() : nodeFactory.apply(node);
    }

    private static Option<ClassInfo> scalaInfo(ClassNode node, boolean object,
            Function3<ClassNode, ScalaSignature, Object, ClassInfo> factory) {
        Option<AnnotationNode> annotation = ScalaSigReader$.MODULE$.ann(node);
        if (!annotation.isDefined()) return Option.empty();
        ScalaSignature signature = ScalaSigReader$.MODULE$.read(annotation.get());
        String name = node.name.replace('/', '.');
        Option<? extends ScalaSignature.ClassSymbolRef> symbol = object ? signature.findObject(name)
                : signature.findClass(name);
        if (!symbol.isDefined()) return Option.empty();
        return new scala.Some<>(factory.apply(node, signature, symbol.get()));
    }

    @SuppressWarnings("unchecked")
    private static <R, E extends Throwable> R throwUnchecked(Throwable throwable) throws E {
        throw (E) throwable;
    }

    static Option<MethodInfo> getSuper(final MethodInsnNode call, StackAnalyser stack) {
        if (Objects.equals(call.owner, stack.owner().getInternalName())) return Option.empty();

        final String methodName = stack.m().name.replaceAll(".+\\Q$$super$\\E", "");
        if (!Objects.equals(call.name, methodName)) return Option.empty();

        // Preserve the compiler's argument-count indexing, including its behavior for wide stack values.
        StackEntry receiver = stack.peek(Type.getType(call.desc).getArgumentTypes().length);
        if (!(receiver instanceof Load) || !(((Load) receiver).e() instanceof This)) return Option.empty();

        return ASMMixinCompiler$.MODULE$.getClassInfo(stack.owner().getInternalName()).superClass()
                .flatMap(new AbstractFunction1<ClassInfo, Option<MethodInfo>>() {

                    @Override
                    public Option<MethodInfo> apply(ClassInfo parent) {
                        return parent.findPublicImpl(methodName, call.desc);
                    }
                });
    }

    static Set<String> listSideOnly(ScalaSignature sig) {
        final String side = "cpw.mods.fml.relauncher.Side." + FMLLaunchHandler.side().name();
        TraversableLike filtered = (TraversableLike) sig.<ScalaSignature.AnnotationInfo>collect(40)
                .filter(new AbstractFunction1<ScalaSignature.AnnotationInfo, Object>() {

                    @Override
                    public Object apply(ScalaSignature.AnnotationInfo annotation) {
                        return Objects.equals(annotation.annType().name(), "cpw.mods.fml.relauncher.SideOnly")
                                && !Objects.equals(
                                        annotation.<ScalaSignature.EnumLiteral>getValue("value").value().full(),
                                        side);
                    }
                });
        TraversableOnce owners = (TraversableOnce) filtered
                .map(new AbstractFunction1<ScalaSignature.AnnotationInfo, String>() {

                    @Override
                    public String apply(ScalaSignature.AnnotationInfo annotation) {
                        return annotation.owner().full();
                    }
                }, IndexedSeq$.MODULE$.canBuildFrom());
        return owners.toSet();
    }
}
