package codechicken.microblock;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.handler.MicroblockProxy$;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MissingMicroMaterial$ implements IMicroMaterial {

    public static final MissingMicroMaterial$ MODULE$ = new MissingMicroMaterial$();

    private final String key = "forgemicroblock:missing";

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    private MissingMicroMaterial$() {}

    public String key() {
        return key;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void loadIcons() {
        icon = MicroblockProxy$.MODULE$.renderBlocks().getIconSafe(null);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBreakingIcon(int side) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {
        MaterialRenderHelper$.MODULE$.start(pos, pass, new IconTransformation(icon)).lighting().render();
    }

    @Override
    public boolean isTransparent() {
        return false;
    }

    @Override
    public int getLightValue() {
        return 0;
    }

    @Override
    public float getStrength(EntityPlayer player) {
        return 1f;
    }

    @Override
    public String getLocalizedName() {
        return "Missing Material";
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Blocks.stone);
    }

    @Override
    public int getCutterStrength() {
        return 0;
    }

    @Override
    public SoundType getSound() {
        return Block.soundTypeStone;
    }

    @Override
    public float explosionResistance(Entity entity) {
        return 6f;
    }
}
