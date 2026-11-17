package codechicken.multipart.asm;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.asm.ScalaSignature.Bytes$;
import scala.Option;
import scala.Some;

public final class ScalaSigReader$ {

    public static final ScalaSigReader$ MODULE$ = new ScalaSigReader$();

    private ScalaSigReader$() {}

    public byte[] decode(String s) {
        // The reference decodes with the platform charset, not the UTF-8 used by encode.
        byte[] bytes = s.getBytes();
        int length = ByteCodecs.decode(bytes);
        return Arrays.copyOf(bytes, Math.min(bytes.length, Math.max(0, length)));
    }

    public String encode(byte[] b) {
        byte[] bytes = ByteCodecs.encode8to7(b);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((bytes[i] + 1) & 0x7f);
        }
        return new String(bytes, 0, Math.max(0, bytes.length - 1), StandardCharsets.UTF_8);
    }

    public ScalaSignature read(AnnotationNode ann) {
        return new ScalaSignature(Bytes$.MODULE$.apply(decode((String) ann.values.get(1))));
    }

    public Object write(ScalaSignature sig, AnnotationNode ann) {
        return ann.values.set(1, encode(sig.bytes().arr()));
    }

    public Option<AnnotationNode> ann(ClassNode cnode) {
        List<AnnotationNode> annotations = cnode.visibleAnnotations;
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals("Lscala/reflect/ScalaSignature;")) return new Some<>(annotation);
            }
        }
        return Option.empty();
    }
}
