package codechicken.multipart.asm;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import scala.Option;

public final class ScalaSigReader {

    private ScalaSigReader() {}

    public static byte[] decode(String s) {
        return ScalaSigReader$.MODULE$.decode(s);
    }

    public static String encode(byte[] b) {
        return ScalaSigReader$.MODULE$.encode(b);
    }

    public static ScalaSignature read(AnnotationNode ann) {
        return ScalaSigReader$.MODULE$.read(ann);
    }

    public static Object write(ScalaSignature sig, AnnotationNode ann) {
        return ScalaSigReader$.MODULE$.write(sig, ann);
    }

    public static Option<AnnotationNode> ann(ClassNode cnode) {
        return ScalaSigReader$.MODULE$.ann(cnode);
    }
}
