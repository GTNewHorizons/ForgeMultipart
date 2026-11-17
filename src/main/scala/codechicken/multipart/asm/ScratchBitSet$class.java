package codechicken.multipart.asm;

import java.util.BitSet;

/** Shared implementation retaining the original Scala trait-helper entry points. */
public abstract class ScratchBitSet$class {

    private ScratchBitSet$class() {}

    public static BitSet getBitSet(ScratchBitSet owner) {
        BitSet bitset = owner.codechicken$multipart$asm$ScratchBitSet$$bitSets().get();
        if (bitset == null) {
            bitset = new BitSet();
            owner.codechicken$multipart$asm$ScratchBitSet$$bitSets().set(bitset);
        }
        return bitset;
    }

    public static BitSet freshBitSet(ScratchBitSet owner) {
        BitSet bitset = owner.getBitSet();
        bitset.clear();
        return bitset;
    }

    public static void $init$(ScratchBitSet owner) {
        owner.codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
                new ThreadLocal<BitSet>());
    }
}
