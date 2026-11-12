package codechicken.multipart.asm;

import java.util.BitSet;

public final class ASMImplicits$ {

    public static final ASMImplicits$ MODULE$ = new ASMImplicits$();

    private ASMImplicits$() {}

    public BitSet ExtBitSet(BitSet bitset) {
        return bitset;
    }

    public Class<?> ExtClass(Class<?> clazz) {
        return clazz;
    }

    public String nodeName(String name) {
        return name == null ? null : name.replace('.', '/');
    }
}
