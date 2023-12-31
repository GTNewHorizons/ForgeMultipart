package codechicken.multipart.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.TFacePart;
import codechicken.multipart.TileMultipart;

public abstract class McSidedMetaPart extends McMetaPart implements TFacePart {

    public McSidedMetaPart() {}

    public McSidedMetaPart(int meta) {
        super(meta);
    }

    public abstract int sideForMeta(int meta);

    @Override
    public void onNeighborChanged() {
        if (!world().isRemote) dropIfCantStay();
    }

    public boolean canStay() {
        int side = sideForMeta(meta);
        if (side < 0 || side > 5) return false;

        BlockCoord pos = new BlockCoord(tile()).offset(side);
        return world().isSideSolid(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side ^ 1));
    }

    public boolean dropIfCantStay() {
        if (!canStay()) {
            drop();
            return true;
        }
        return false;
    }

    public void drop() {
        TileMultipart.dropItem(new ItemStack(getBlock()), world(), Vector3.fromTileEntityCenter(tile()));
        tile().remPart(this);
    }

    @Override
    public int getSlotMask() {
        return 1 << sideForMeta(meta);
    }

    @Override
    public boolean solid(int side) {
        return false;
    }

    @Override
    public int redstoneConductionMap() {
        return 0x1F;
    }
}
