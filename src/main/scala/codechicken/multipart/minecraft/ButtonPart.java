package codechicken.multipart.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.IFaceRedstonePart;

public class ButtonPart extends McSidedMetaPart implements IFaceRedstonePart {

    public static BlockButton stoneButton = (BlockButton) Blocks.stone_button;
    public static BlockButton woodenButton = (BlockButton) Blocks.wooden_button;
    public static int[] metaSideMap = new int[] { -1, 4, 5, 2, 3, -1, -1, -1 };
    public static int[] sideMetaMap = new int[] { -1, -1, 3, 4, 1, 2 };
    public static Cuboid6[] cuboidRegions = setupCuboids();

    /**
     * Capture the boxes of all 16 button states, copying all button hitboxes directly We do this so any button mixins
     * will use the proper mixed hitbox size. So mods like Et Futurum Requiem which add floor/ceiling buttons need no
     * extra setup to have proper block bounds.
     */
    private static Cuboid6[] setupCuboids() {
        Cuboid6[] regions = new Cuboid6[16];
        for (int i = 0; i < regions.length; i++) {
            woodenButton.setBlockBoundsForItemRender(); // Reset bounds
            woodenButton.func_150043_b(i); // Set button bounds based on meta; this is how buttons change their hitbox
            regions[i] = new Cuboid6(
                    woodenButton.getBlockBoundsMinX(),
                    woodenButton.getBlockBoundsMinY(),
                    woodenButton.getBlockBoundsMinZ(),
                    woodenButton.getBlockBoundsMaxX(),
                    woodenButton.getBlockBoundsMaxY(),
                    woodenButton.getBlockBoundsMaxZ()); // Use the button hitbox to generate a Cuboid6 region. This
                                                        // isn't the actual instance, we'll copy from this later.
        }
        return regions;
    }

    public static BlockButton getButton(int meta) {
        return (meta & 0x10) > 0 ? woodenButton : stoneButton;
    }

    public ButtonPart() {}

    public ButtonPart(int meta) {
        super(meta);
    }

    @Override
    public int sideForMeta(int meta) {
        return metaSideMap[meta & 7];
    }

    @Override
    public Block getBlock() {
        return getButton(meta);
    }

    @Override
    public String getType() {
        return "mc_button";
    }

    public int delay() {
        return sensitive() ? 30 : 20;
    }

    public boolean sensitive() {
        return (meta & 0x10) > 0;
    }

    @Override
    public Cuboid6 getBounds() {
        // Somehow I got a crash here for meta 20? Let's wrap it to be safe
        return cuboidRegions[meta & 15].copy();// these objects get transformed I think, so we need to make a copy.
    }

    public static McBlockPart placement(World world, BlockCoord pos, int side, int type) {
        if (sideMetaMap[side ^ 1] == -1) return null;

        pos = pos.copy().offset(side ^ 1);
        if (!world.isSideSolid(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side))) return null;

        return new ButtonPart(sideMetaMap[side ^ 1] | type << 4);
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition part, ItemStack item) {
        if (pressed()) return false;

        if (!world().isRemote) toggle();

        return true;
    }

    @Override
    public void scheduledTick() {
        if (pressed()) updateState();
    }

    public boolean pressed() {
        return (meta & 8) > 0;
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (!pressed() && !world().isRemote && entity instanceof EntityArrow) updateState();
    }

    private void toggle() {
        boolean in = !pressed();
        meta ^= 8;
        world().playSoundEffect(x() + 0.5, y() + 0.5, z() + 0.5, "random.click", 0.3F, in ? 0.6F : 0.5F);
        if (in) scheduleTick(delay());

        sendDescUpdate();
        tile().notifyPartChange(this);
        tile().notifyNeighborChange(metaSideMap[meta & 7]);
        tile().markDirty();
    }

    private void updateState() {
        boolean arrows = sensitive() && !world()
                .getEntitiesWithinAABB(EntityArrow.class, getBounds().add(Vector3.fromTileEntity(tile())).toAABB())
                .isEmpty();
        boolean pressed = pressed();

        if (arrows != pressed) toggle();
        if (arrows && pressed) scheduleTick(delay());
    }

    @Override
    public void onRemoved() {
        if (pressed()) tile().notifyNeighborChange(metaSideMap[meta & 7]);
    }

    @Override
    public int weakPowerLevel(int side) {
        return pressed() ? 15 : 0;
    }

    @Override
    public int strongPowerLevel(int side) {
        return pressed() && side == metaSideMap[meta & 7] ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone(int side) {
        return true;
    }

    @Override
    public int getFace() {
        return metaSideMap[meta & 7];
    }
}
