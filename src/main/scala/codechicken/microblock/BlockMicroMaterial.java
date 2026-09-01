package codechicken.microblock;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.uv.MultiIconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.handler.MicroblockProxy;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/** Standard micro material class suitable for most blocks. */
public class BlockMicroMaterial implements IMicroMaterial {

    private final Block block;
    private final int meta;
    private final String blockKey;

    @SideOnly(Side.CLIENT)
    private MultiIconTransformation icont;

    public BlockMicroMaterial(Block block, int meta) {
        this.block = block;
        this.meta = meta;
        blockKey = (String) Block.blockRegistry.getNameForObject(block);
    }

    public static int $lessinit$greater$default$2() {
        return BlockMicroMaterial$.MODULE$.$lessinit$greater$default$2();
    }

    public static int createAndRegister$default$2() {
        return BlockMicroMaterial$.MODULE$.createAndRegister$default$2();
    }

    public static String oldKey(Block block) {
        return BlockMicroMaterial$.MODULE$.oldKey(block);
    }

    public static String materialKey(Block block) {
        return BlockMicroMaterial$.MODULE$.materialKey(block);
    }

    public static String materialKey(String name, int meta) {
        return BlockMicroMaterial$.MODULE$.materialKey(name, meta);
    }

    public static String materialKey(Block block, int meta) {
        return BlockMicroMaterial$.MODULE$.materialKey(block, meta);
    }

    public static void createAndRegister(Block block, int meta, String name) {
        BlockMicroMaterial$.MODULE$.createAndRegister(block, meta, name);
    }

    public static void createAndRegister(Block block, int meta, String name, String oldName) {
        BlockMicroMaterial$.MODULE$.createAndRegister(block, meta, name, oldName);
    }

    public static void createAndRegister(Block block, int meta) {
        BlockMicroMaterial$.MODULE$.createAndRegister(block, meta);
    }

    public static void createAndRegister(Block block, Seq<Object> meta) {
        BlockMicroMaterial$.MODULE$.createAndRegister(block, meta);
    }

    public static void createAndRegister(Block block, Seq<Object> meta, String oldName) {
        BlockMicroMaterial$.MODULE$.createAndRegister(block, meta, oldName);
    }

    public static void createAndRegister(Block block, Seq<Object> meta, String name, String oldName) {
        BlockMicroMaterial$.MODULE$.createAndRegister(block, meta, name, oldName);
    }

    public Block block() {
        return block;
    }

    public int meta() {
        return meta;
    }

    public String blockKey() {
        return blockKey;
    }

    public MultiIconTransformation icont() {
        return icont;
    }

    public void icont_$eq(MultiIconTransformation icont) {
        this.icont = icont;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void loadIcons() {
        Block currentBlock = Block.getBlockFromName(blockKey);
        IIcon[] icons = new IIcon[6];
        for (int side = 0; side < icons.length; side++) {
            icons[side] = codechicken$microblock$BlockMicroMaterial$$safeIcon$1(currentBlock, side);
        }
        icont_$eq(new MultiIconTransformation(icons));
    }

    public final IIcon codechicken$microblock$BlockMicroMaterial$$safeIcon$1(Block block, int side) {
        try {
            return MicroblockProxy.renderBlocks().getIconSafe(block.getIcon(side, meta));
        } catch (Exception ignored) {
            return MicroblockProxy.renderBlocks().getIconSafe(null);
        }
    }

    @Override
    public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {
        MaterialRenderHelper$.MODULE$.start(pos, pass, icont).blockColour(getColour(pass)).lighting()
                .blockAndMeta(block, meta).render();
    }

    public int getColour(int pass) {
        if (pass == -1) {
            return (block.getBlockColor() << 8) | 0xFF;
        }
        CCRenderState state = CCRenderState.instance();
        return (block.colorMultiplier(
                state.lightMatrix.access,
                state.lightMatrix.pos.x,
                state.lightMatrix.pos.y,
                state.lightMatrix.pos.z) << 8) | 0xFF;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return block.canRenderInPass(pass);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBreakingIcon(int side) {
        return block.getIcon(side, meta);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(block, 1, meta);
    }

    @Override
    public String getLocalizedName() {
        return getItem().getDisplayName();
    }

    @Override
    public float getStrength(EntityPlayer player) {
        float hardness = 30F;
        try {
            hardness = block.getBlockHardness(null, 0, 0, 0);
        } catch (Exception ignored) {}
        return player.getBreakSpeed(block, false, meta % 16, 0, -1, 0) / hardness;
    }

    @Override
    public boolean isTransparent() {
        return !block.isOpaqueCube();
    }

    @Override
    public int getLightValue() {
        return block.getLightValue();
    }

    public Seq<String> toolClasses() {
        return JavaConversions.asScalaBuffer(Arrays.asList("axe", "pickaxe", "shovel")).toList();
    }

    @Override
    public int getCutterStrength() {
        return block.getHarvestLevel(meta % 16);
    }

    @Override
    public Block.SoundType getSound() {
        return block.stepSound;
    }

    @Override
    public float explosionResistance(Entity entity) {
        return block.getExplosionResistance(entity);
    }
}
