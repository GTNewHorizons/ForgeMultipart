package codechicken.multipart.asm;

import java.util.BitSet;

/** Per-owner, per-thread scratch storage used by both generators. */
public interface ScratchBitSet {

    void codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
            ThreadLocal value);

    ThreadLocal<BitSet> codechicken$multipart$asm$ScratchBitSet$$bitSets();

    BitSet getBitSet();

    BitSet freshBitSet();
}
