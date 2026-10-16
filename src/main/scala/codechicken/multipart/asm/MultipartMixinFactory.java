package codechicken.multipart.asm;

import java.util.BitSet;

import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.TileMultipart;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import scala.collection.Seq;

public final class MultipartMixinFactory {

    private MultipartMixinFactory() {}

    public static Class<TileMultipart> baseType() {
        return MultipartMixinFactory$.MODULE$.baseType();
    }

    @SuppressWarnings("rawtypes")
    public static Object construct(BitSet traitSet, Seq args) {
        return MultipartMixinFactory$.MODULE$.construct(traitSet, args);
    }

    public static int getId(String trait) {
        return MultipartMixinFactory$.MODULE$.getId(trait);
    }

    public static int registerTrait(Class<?> trait) {
        return MultipartMixinFactory$.MODULE$.registerTrait(trait);
    }

    public static int registerTrait(String trait) {
        return MultipartMixinFactory$.MODULE$.registerTrait(trait);
    }

    public static void onCompiled(Class<? extends TileMultipart> clazz, BitSet traitSet) {
        MultipartMixinFactory$.MODULE$.onCompiled(clazz, traitSet);
    }

    public static void autoCompleteJavaTrait(ClassNode cnode) {
        MultipartMixinFactory$.MODULE$.autoCompleteJavaTrait(cnode);
    }

    public static String generatePassThroughTrait(String s_interface) {
        return MultipartMixinFactory$.MODULE$.generatePassThroughTrait(s_interface);
    }

    public static ClassInfo codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1(ClassInfo info) {
        return MultipartMixinFactory$.MODULE$.codechicken$multipart$asm$ASMMixinFactory$$concreteParent$1(info);
    }

    public static boolean codechicken$multipart$asm$ASMMixinFactory$$checkParent$1(ClassInfo info, String parentName) {
        return MultipartMixinFactory$.MODULE$
                .codechicken$multipart$asm$ASMMixinFactory$$checkParent$1(info, parentName);
    }
}
