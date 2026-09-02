package codechicken.microblock;

import java.util.BitSet;

import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.multipart.asm.ASMMixinFactory;
import codechicken.multipart.asm.ScratchBitSet;
import codechicken.multipart.asm.ScratchBitSet$class;
import scala.Predef$;

public final class MicroblockGenerator$ extends ASMMixinFactory<Microblock> implements ScratchBitSet {

    public static final MicroblockGenerator$ MODULE$ = new MicroblockGenerator$();

    private ThreadLocal<BitSet> codechicken$multipart$asm$ScratchBitSet$$bitSets;

    private MicroblockGenerator$() {
        super(Microblock.class, Predef$.MODULE$.wrapRefArray(new Class<?>[] { Integer.TYPE }));
        ScratchBitSet$class.$init$(this);
    }

    @Override
    public ThreadLocal<BitSet> codechicken$multipart$asm$ScratchBitSet$$bitSets() {
        return codechicken$multipart$asm$ScratchBitSet$$bitSets;
    }

    @Override
    public void codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
            ThreadLocal value) {
        codechicken$multipart$asm$ScratchBitSet$$bitSets = value;
    }

    @Override
    public BitSet getBitSet() {
        return ScratchBitSet$class.getBitSet(this);
    }

    @Override
    public BitSet freshBitSet() {
        return ScratchBitSet$class.freshBitSet(this);
    }

    public Microblock create(MicroblockClass microClass, int materialId, boolean client) {
        BitSet traits = freshBitSet();
        traits.set(microClass.baseTraitId());
        if (client) {
            traits.set(microClass.clientTraitId());
        }

        IMicroMaterial material = MicroMaterialRegistry.getMaterial(materialId);
        if (material instanceof MicroblockGenerator.IGeneratedMaterial) {
            ((MicroblockGenerator.IGeneratedMaterial) material).addTraits(traits, microClass, client);
        }

        return construct(traits, Predef$.MODULE$.wrapRefArray(new Object[] { Integer.valueOf(materialId) }));
    }
}
