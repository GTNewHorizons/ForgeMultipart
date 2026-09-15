package codechicken.multipart.asm;

import java.lang.reflect.Constructor;
import java.util.BitSet;
import java.util.Objects;

import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.Option;
import scala.collection.Seq;
import scala.collection.Seq$;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Builder;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;
import scala.reflect.ClassTag$;
import scala.runtime.BoxesRunTime;

public class ASMMixinFactory<T> {

    private final Class<T> baseType;
    private final Seq<Class<?>> paramTypes;
    private final Map<String, Object> traitMap;
    private final ArrayBuffer<String> traits;
    private final Map<BitSet, Constructor<? extends T>> classMap;
    private int ugenid;

    public ASMMixinFactory(Class<T> baseType, Seq<Class<?>> paramTypes) {
        this.baseType = baseType;
        this.paramTypes = paramTypes;
        traitMap = new HashMap<>();
        traits = new ArrayBuffer<>();
        classMap = new HashMap<>();
    }

    public Class<T> baseType() {
        return baseType;
    }

    private String nextName() {
        String name = baseType().getSimpleName() + "_cmp$$" + ugenid;
        ugenid++;
        return name;
    }

    @SuppressWarnings("unchecked")
    private Constructor<? extends T> compile(BitSet traitSet) throws NoSuchMethodException {
        Builder<String, Seq<String>> seq = Seq$.MODULE$.newBuilder();
        for (int i = traitSet.nextSetBit(0); i >= 0; i = traitSet.nextSetBit(i + 1)) {
            seq.$plus$eq(traits.apply(i));
        }

        Class<? extends T> clazz = (Class<? extends T>) ASMMixinCompiler$.MODULE$
                .mixinClasses(nextName(), ASMImplicits.nodeName(baseType().getName()), seq.result());
        onCompiled(clazz, traitSet);
        return clazz.getDeclaredConstructor((Class<?>[]) paramTypes.toArray(ClassTag$.MODULE$.apply(Class.class)));
    }

    // Scala's protected hooks are public in the reference bytecode.
    public void onCompiled(Class<? extends T> clazz, BitSet traitSet) {}

    public void autoCompleteJavaTrait(ClassNode cnode) {}

    public synchronized T construct(BitSet traitSet, Seq<Object> args) {
        try {
            Option<Constructor<? extends T>> cached = classMap.get(traitSet);
            Constructor<? extends T> constructor;
            if (cached.isDefined()) {
                constructor = cached.get();
            } else {
                constructor = compile(traitSet);
                classMap.put(ASMImplicits.ExtBitSet$.MODULE$.copy$extension(traitSet), constructor);
            }
            return constructor.newInstance((Object[]) args.toArray(ClassTag$.MODULE$.Object()));
        } catch (ReflectiveOperationException e) {
            return ASMMixinFactory.<T, RuntimeException>throwUnchecked(e);
        }
    }

    public int getId(String s_trait) {
        return BoxesRunTime.unboxToInt(traitMap.apply(s_trait));
    }

    public int registerTrait(Class<?> traitClass) {
        return registerTrait(ASMImplicits.nodeName(traitClass.getName()));
    }

    public int registerTrait(String s_trait) {
        ClassNode cnode = ASMMixinCompiler$.MODULE$.classNode(s_trait);
        if (cnode == null) {
            return ASMMixinFactory.<Integer, RuntimeException>throwUnchecked(new ClassNotFoundException(s_trait));
        }

        Option<Object> registered = traitMap.get(cnode.name);
        if (registered.isDefined()) {
            return BoxesRunTime.unboxToInt(registered.get());
        }

        ClassInfo info = ASMMixinCompiler$.MODULE$.getClassInfo(cnode);
        String parentName = codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1(info).name();
        if (!codechicken$multipart$asm$ASMMixinFactory$$checkParent$1(
                ASMMixinCompiler$.MODULE$.getClassInfo(baseType()),
                parentName)) {
            throw new IllegalArgumentException(
                    ASMImplicits.nodeName(baseType().getName()) + " does not extend parent "
                            + parentName
                            + " of mixin trait "
                            + s_trait);
        }

        if (info.isTrait()) {
            ASMMixinCompiler$.MODULE$.registerScalaTrait(cnode);
        } else {
            autoCompleteJavaTrait(cnode);
            ASMMixinCompiler$.MODULE$.registerJavaTrait(cnode);
        }

        int id = traits.size();
        traits.$plus$eq(cnode.name);
        traitMap.put(cnode.name, id);
        return id;
    }

    // Retain the public helpers emitted for Scala's local recursive functions.
    public final ClassInfo codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1(ClassInfo info) {
        Option<MixinInfo> mixin = ASMMixinCompiler$.MODULE$.getMixinInfo(info.name());
        if (mixin.isDefined()) {
            return ASMMixinCompiler$.MODULE$.getClassInfo(mixin.get().parent());
        }
        ClassInfo parent = info.superClass().get();
        if (parent.isTrait() || ASMMixinCompiler$.MODULE$.getMixinInfo(parent.name()).isDefined()) {
            return codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1(parent);
        }
        return parent;
    }

    public final boolean codechicken$multipart$asm$ASMMixinFactory$$checkParent$1(ClassInfo info, String parentName) {
        if (Objects.equals(info.name(), parentName)) return true;
        Option<ClassInfo> parent = info.superClass();
        return parent.isDefined() && codechicken$multipart$asm$ASMMixinFactory$$checkParent$1(parent.get(), parentName);
    }

    @SuppressWarnings("unchecked")
    private static <R, E extends Throwable> R throwUnchecked(Throwable throwable) throws E {
        throw (E) throwable;
    }
}
