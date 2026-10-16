package codechicken.multipart.scalatraits;

import java.util.LinkedList;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Mixin implementation that distributes fluid handling among multipart tanks. */
public class TFluidHandlerTile extends TileMultipart implements IFluidHandler {

    public LinkedList<IFluidHandler> tankList = new LinkedList<>();

    @Override
    public void copyFrom(TileMultipart that) {
        super.copyFrom(that);
        if (that instanceof TFluidHandlerTile) {
            tankList = ((TFluidHandlerTile) that).tankList;
        }
    }

    @Override
    public void bindPart(TMultiPart part) {
        super.bindPart(part);
        if (part instanceof IFluidHandler) {
            tankList.add((IFluidHandler) part);
        }
    }

    @Override
    public void partRemoved(TMultiPart part, int position) {
        super.partRemoved(part, position);
        if (part instanceof IFluidHandler) {
            tankList.remove(part);
        }
    }

    @Override
    public void clearParts() {
        super.clearParts();
        tankList.clear();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection direction) {
        int tankCount = 0;
        for (IFluidHandler handler : tankList) {
            tankCount += handler.getTankInfo(direction).length;
        }

        FluidTankInfo[] tanks = new FluidTankInfo[tankCount];
        int index = 0;
        for (IFluidHandler handler : tankList) {
            for (FluidTankInfo tank : handler.getTankInfo(direction)) {
                tanks[index++] = tank;
            }
        }
        return tanks;
    }

    @Override
    public int fill(ForgeDirection direction, FluidStack liquid, boolean doFill) {
        int filled = 0;
        int initial = TFluidHandlerTileAccess.amount(liquid);
        for (IFluidHandler handler : tankList) {
            FluidStack remaining = TFluidHandlerTileAccess.copy(liquid, initial - filled);
            filled += handler.fill(direction, remaining, doFill);
        }
        return filled;
    }

    @Override
    public boolean canFill(ForgeDirection direction, Fluid liquid) {
        for (IFluidHandler handler : tankList) {
            if (handler.canFill(direction, liquid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection direction, Fluid liquid) {
        for (IFluidHandler handler : tankList) {
            if (handler.canDrain(direction, liquid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public FluidStack drain(ForgeDirection direction, int amount, boolean doDrain) {
        FluidStack drained = null;
        int drainedAmount = 0;
        for (IFluidHandler handler : tankList) {
            int remaining = amount - drainedAmount;
            FluidStack result = handler.drain(direction, remaining, false);
            if (result != null && TFluidHandlerTileAccess.amount(result) > 0
                    && (drained == null || drained.isFluidEqual(result))) {
                if (doDrain) {
                    handler.drain(direction, remaining, true);
                }
                if (drained == null) {
                    drained = result;
                }
                drainedAmount += TFluidHandlerTileAccess.amount(result);
            }
        }
        if (drained != null) {
            TFluidHandlerTileAccess.setAmount(drained, drainedAmount);
        }
        return drained;
    }

    @Override
    public FluidStack drain(ForgeDirection direction, FluidStack liquid, boolean doDrain) {
        int amount = TFluidHandlerTileAccess.amount(liquid);
        FluidStack drained = null;
        int drainedAmount = 0;
        for (IFluidHandler handler : tankList) {
            FluidStack remaining = TFluidHandlerTileAccess.copy(liquid, amount - drainedAmount);
            FluidStack result = handler.drain(direction, remaining, false);
            if (result != null && TFluidHandlerTileAccess.amount(result) > 0
                    && (drained == null || drained.isFluidEqual(result))) {
                if (doDrain) {
                    handler.drain(direction, remaining, true);
                }
                if (drained == null) {
                    drained = result;
                }
                drainedAmount += TFluidHandlerTileAccess.amount(result);
            }
        }
        if (drained != null) {
            TFluidHandlerTileAccess.setAmount(drained, drainedAmount);
        }
        return drained;
    }
}

/** Keeps fields owned by method arguments outside the generated trait transformer. */
final class TFluidHandlerTileAccess {

    private TFluidHandlerTileAccess() {}

    static int amount(FluidStack stack) {
        return stack.amount;
    }

    static void setAmount(FluidStack stack, int amount) {
        stack.amount = amount;
    }

    static FluidStack copy(FluidStack stack, int amount) {
        FluidStack copy = stack.copy();
        copy.amount = amount;
        return copy;
    }
}
