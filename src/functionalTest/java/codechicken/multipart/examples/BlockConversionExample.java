package codechicken.multipart.examples;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import codechicken.multipart.TMultiPart;

/** Compiling registration/state example; add the real part's geometry, rendering and gameplay behavior. */
public final class BlockConversionExample implements IPartConverter, IPartFactory2 {

    public static final String PART_TYPE = "yourmod:converted";
    private final Block sourceBlock;

    public BlockConversionExample(Block sourceBlock) {
        this.sourceBlock = sourceBlock;
    }

    /** Call once from common preInit/init, after registering the source block, on both sides. */
    public static void register(Block sourceBlock) {
        BlockConversionExample example = new BlockConversionExample(sourceBlock);
        MultiPartRegistry.registerPartFactory(example, PART_TYPE);
        MultiPartRegistry.registerConverter(example);
    }

    @Override
    public Iterable<Block> blockTypes() {
        return Collections.singletonList(sourceBlock);
    }

    @Override
    public TMultiPart convert(World world, BlockCoord pos) {
        if (world.getBlock(pos.x, pos.y, pos.z) != sourceBlock) return null;
        ConvertedPart part = new ConvertedPart();
        part.metadata = world.getBlockMetadata(pos.x, pos.y, pos.z);
        return part;
    }

    @Override
    public TMultiPart createPart(String name, NBTTagCompound nbt) {
        return new ConvertedPart();
    }

    @Override
    public TMultiPart createPart(String name, MCDataInput packet) {
        return new ConvertedPart();
    }

    public static final class ConvertedPart extends TMultiPart {

        private int metadata;

        @Override
        public String getType() {
            return PART_TYPE;
        }

        @Override
        public void save(NBTTagCompound tag) {
            tag.setInteger("metadata", metadata);
        }

        @Override
        public void load(NBTTagCompound tag) {
            metadata = tag.getInteger("metadata");
        }

        @Override
        public void writeDesc(MCDataOutput packet) {
            packet.writeInt(metadata);
        }

        @Override
        public void readDesc(MCDataInput packet) {
            metadata = packet.readInt();
        }
    }
}
