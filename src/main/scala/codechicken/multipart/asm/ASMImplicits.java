package codechicken.multipart.asm;

import java.util.BitSet;
import java.util.Objects;

/** Name and BitSet helpers, retaining the Scala value-class entry points for compiled callers. */
public final class ASMImplicits {

    private ASMImplicits() {}

    public static BitSet ExtBitSet(BitSet bitset) {
        return ASMImplicits$.MODULE$.ExtBitSet(bitset);
    }

    public static Class ExtClass(Class clazz) {
        return ASMImplicits$.MODULE$.ExtClass(clazz);
    }

    public static String nodeName(String name) {
        return ASMImplicits$.MODULE$.nodeName(name);
    }

    public static final class ExtBitSet {

        private final BitSet bitset;

        public ExtBitSet(BitSet bitset) {
            this.bitset = bitset;
        }

        public BitSet bitset() {
            return bitset;
        }

        public BitSet set(BitSet other) {
            return ExtBitSet$.MODULE$.set$extension(bitset, other);
        }

        public BitSet copy() {
            return ExtBitSet$.MODULE$.copy$extension(bitset);
        }

        @Override
        public int hashCode() {
            return ExtBitSet$.MODULE$.hashCode$extension(bitset);
        }

        @Override
        public boolean equals(Object other) {
            return ExtBitSet$.MODULE$.equals$extension(bitset, other);
        }
    }

    public static class ExtBitSet$ {

        public static final ExtBitSet$ MODULE$ = new ExtBitSet$();

        public ExtBitSet$() {}

        public final BitSet set$extension(BitSet bitset, BitSet other) {
            // Preserve clear-before-read: self-replacement empties the set, and a null source fails after clearing.
            bitset.clear();
            bitset.or(other);
            return bitset;
        }

        public final BitSet copy$extension(BitSet bitset) {
            return set$extension(new BitSet(), bitset);
        }

        public final int hashCode$extension(BitSet bitset) {
            return bitset.hashCode();
        }

        public final boolean equals$extension(BitSet bitset, Object other) {
            if (!(other instanceof ExtBitSet)) return false;
            BitSet otherBits = ((ExtBitSet) other).bitset();
            // Scala still dispatches equals for identical non-null references; Objects.equals would skip it.
            return bitset == null ? otherBits == null : bitset.equals(otherBits);
        }
    }

    public static final class ExtClass {

        private final Class<?> clazz;

        public ExtClass(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Class<?> clazz() {
            return clazz;
        }

        public String nodeName() {
            return ExtClass$.MODULE$.nodeName$extension(clazz);
        }

        @Override
        public int hashCode() {
            return ExtClass$.MODULE$.hashCode$extension(clazz);
        }

        @Override
        public boolean equals(Object other) {
            return ExtClass$.MODULE$.equals$extension(clazz, other);
        }
    }

    public static class ExtClass$ {

        public static final ExtClass$ MODULE$ = new ExtClass$();

        public ExtClass$() {}

        public final String nodeName$extension(Class clazz) {
            return ASMImplicits$.MODULE$.nodeName(clazz.getName());
        }

        public final int hashCode$extension(Class clazz) {
            return clazz.hashCode();
        }

        public final boolean equals$extension(Class clazz, Object other) {
            return other instanceof ExtClass && Objects.equals(clazz, ((ExtClass) other).clazz());
        }
    }
}
