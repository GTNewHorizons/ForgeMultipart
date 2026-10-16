package codechicken.microblock;

import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MissingMicroMaterial {

    private MissingMicroMaterial() {}

    public static String key() {
        return MissingMicroMaterial$.MODULE$.key();
    }

    @SideOnly(Side.CLIENT)
    public static void loadIcons() {
        MissingMicroMaterial$.MODULE$.loadIcons();
    }

    @SideOnly(Side.CLIENT)
    public static IIcon getBreakingIcon(int side) {
        return MissingMicroMaterial$.MODULE$.getBreakingIcon(side);
    }

    @SideOnly(Side.CLIENT)
    public static void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {
        MissingMicroMaterial$.MODULE$.renderMicroFace(pos, pass, bounds);
    }

    public static boolean isTransparent() {
        return MissingMicroMaterial$.MODULE$.isTransparent();
    }

    public static int getLightValue() {
        return MissingMicroMaterial$.MODULE$.getLightValue();
    }

    public static float getStrength(EntityPlayer player) {
        return MissingMicroMaterial$.MODULE$.getStrength(player);
    }

    public static String getLocalizedName() {
        return MissingMicroMaterial$.MODULE$.getLocalizedName();
    }

    public static ItemStack getItem() {
        return MissingMicroMaterial$.MODULE$.getItem();
    }

    public static int getCutterStrength() {
        return MissingMicroMaterial$.MODULE$.getCutterStrength();
    }

    public static SoundType getSound() {
        return MissingMicroMaterial$.MODULE$.getSound();
    }

    public static float explosionResistance(Entity entity) {
        return MissingMicroMaterial$.MODULE$.explosionResistance(entity);
    }
}
