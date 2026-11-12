package codechicken.multipart.examples;

import java.util.BitSet;

import net.minecraft.block.Block;

import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockGenerator.IGeneratedMaterial;

/** Example material whose upper metadata bit means illuminated and low bits select the lamp colour. */
public final class IlluminatedMicroMaterial extends BlockMicroMaterial implements IGeneratedMaterial {

    private final int traitId;

    public IlluminatedMicroMaterial(Block lamp, int meta, int traitId) {
        super(lamp, meta);
        this.traitId = traitId;
    }

    @Override
    public void addTraits(BitSet traits, MicroblockClass microClass, boolean client) {
        traits.set(traitId);
    }

    @Override
    public int getCutterStrength() {
        return block().getHarvestLevel(meta() % 16);
    }
}
