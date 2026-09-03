package codechicken.multipart.asm;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.util.Objects;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.FieldMixin;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import codechicken.multipart.asm.ScalaSignature.MethodSymbol;
import scala.Function1;
import scala.MatchError;
import scala.None$;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;
import scala.collection.TraversableLike;
import scala.collection.immutable.Set;
import scala.collection.mutable.Buffer;
import scala.collection.mutable.Buffer$;
import scala.collection.mutable.ListBuffer;
import scala.collection.mutable.Map;
import scala.collection.mutable.Map$;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractPartialFunction;
import scala.runtime.BoxedUnit;
import scala.runtime.BoxesRunTime;

/** Registration behind the Scala bridges for the retained ClassInfo$ nested types. */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class ScalaTraitRegistration {

    private ScalaTraitRegistration() {}

    static Buffer<MixinInfo> getAndRegisterParentTraits(ClassNode node, final Function1<ClassInfo, Boolean> isTrait,
            final Function1<ClassInfo, ClassNode> traitNode) {
        TraversableLike parents = (TraversableLike) ((TraversableLike) JavaConversions.asScalaBuffer(node.interfaces))
                .map(new AbstractFunction1<String, ClassInfo>() {

                    @Override
                    public ClassInfo apply(String name) {
                        return ASMMixinCompiler$.MODULE$.getClassInfo(name);
                    }
                }, Buffer$.MODULE$.canBuildFrom());
        return (Buffer<MixinInfo>) parents.collect(new AbstractPartialFunction<ClassInfo, MixinInfo>() {

            @Override
            public boolean isDefinedAt(ClassInfo info) {
                return BoxesRunTime.unboxToBoolean(isTrait.apply(info));
            }

            @Override
            public <A1 extends ClassInfo, B1> B1 applyOrElse(A1 info, Function1<A1, B1> fallback) {
                if (isDefinedAt(info)) return (B1) ASMMixinCompiler$.MODULE$.registerScalaTrait(traitNode.apply(info));
                return fallback.apply(info);
            }
        }, Buffer$.MODULE$.canBuildFrom());
    }

    static MixinInfo registerScalaTrait(final ClassNode node, Map<String, MixinInfo> mixins,
            Function1<ClassInfo, ScalaSignature> signature, Function1<ClassInfo, Object> classSymbol) {
        // Read and write the same map reference; getMixinInfo is that map's get, and one source keeps them in step.
        Option<MixinInfo> cached = mixins.get(node.name);
        if (cached instanceof Some) return cached.get();
        if (cached != (Object) None$.MODULE$) throw new MatchError(cached);

        ClassInfo info = ASMMixinCompiler$.MODULE$.getClassInfo(node);
        ScalaSignature sig = signature.apply(info);
        final Set<String> sideOnly = ASMMixinCompiler$.MODULE$.listSideOnly(sig);
        Buffer<MixinInfo> parents = ASMMixinCompiler$.MODULE$.getAndRegisterParentTraits(node);
        final Map<String, MethodSymbol> accessors = Map$.MODULE$.empty();
        final ListBuffer<FieldMixin> fields = new ListBuffer<>();
        final ListBuffer<MethodNode> methods = new ListBuffer<>();
        final ListBuffer<String> supers = new ListBuffer<>();
        final ScalaSignature.ClassSymbolRef symbol = (ScalaSignature.ClassSymbolRef) classSymbol.apply(info);
        sig.<MethodSymbol>collect(8).foreach(new AbstractFunction1<MethodSymbol, BoxedUnit>() {

            @Override
            public BoxedUnit apply(final MethodSymbol method) {
                if (method.isParam() || !BoxesRunTime.equals(method.owner(), symbol)) return BoxedUnit.UNIT;
                if (sideOnly.apply(method.full())) return BoxedUnit.UNIT;
                if (method.isAccessor()) {
                    accessors.put(method.name(), method);
                } else if (method.isMethod()) {
                    final String desc = method.jDesc();
                    if (method.name().startsWith("super$")) {
                        supers.$plus$eq(method.name().substring(6) + desc);
                    } else
                        if (!method.isPrivate() && !method.isDeferred() && !Objects.equals(method.name(), "$init$")) {
                            Option<MethodNode> found = JavaConversions.asScalaBuffer(node.methods)
                                    .find(new AbstractFunction1<MethodNode, Object>() {

                                        @Override
                                        public Object apply(MethodNode candidate) {
                                            return Objects.equals(candidate.name, method.name())
                                                    && Objects.equals(candidate.desc, desc);
                                        }
                                    });
                            if (found instanceof Some) {
                                methods.$plus$eq(found.get());
                            } else if (found == (Object) None$.MODULE$) {
                                throw new IllegalArgumentException(
                                        "Unable to add mixin trait " + node.name
                                                + ": "
                                                + method.name()
                                                + desc
                                                + " found in scala signature but not in class file. Most likely an obfuscation issue.");
                            } else {
                                throw new MatchError(found);
                            }
                        }
                } else {
                    fields.$plus$eq(
                            new FieldMixin(
                                    method.name().trim(),
                                    method.jDesc(),
                                    accessors.apply(method.name().trim()).isPrivate() ? ACC_PRIVATE : ACC_PUBLIC));
                }
                return BoxedUnit.UNIT;
            }
        });
        MixinInfo mixin = new MixinInfo(node.name, symbol.jParent(), parents, fields, methods, supers);
        mixins.put(node.name, mixin);
        return mixin;
    }
}
