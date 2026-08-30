package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import org.junit.jupiter.api.Test;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.scalatraits.TFluidHandlerTile;
import scala.collection.immutable.Nil$;

/** Consumer-visible state and behavior of the generated fluid-handler trait. */
class TFluidHandlerTileFunctionalTest {

    @Test
    void copyFromSharesACompatibleTankListAndKeepsItForAPlainSource() throws Exception {
        TileMultipart source = newFluidTile();
        TileMultipart target = newFluidTile();
        RecordingFluidPart part = part("source", FluidRegistry.WATER, 0, 0);
        source.bindPart(part);

        target.copyFrom(source);
        assertSame(tankList(source), tankList(target));
        assertEquals(Arrays.asList(part), tankList(target));

        LinkedList<IFluidHandler> retained = new LinkedList<>();
        setTankList(target, retained);
        target.copyFrom(new TileMultipart());
        assertSame(retained, tankList(target));
    }

    @Test
    void bindRemoveAndClearTrackOnlyFluidPartsInOrder() throws Exception {
        TileMultipart tile = newFluidTile();
        RecordingFluidPart first = part("first", FluidRegistry.WATER, 0, 0);
        RecordingFluidPart second = part("second", FluidRegistry.LAVA, 0, 0);
        PlainPart plain = new PlainPart();
        tile.addPart_do(first);
        tile.addPart_do(plain);
        tile.addPart_do(second);

        assertEquals(Arrays.asList(first, second), tankList(tile));
        tile.partRemoved(first, 0);
        assertEquals(Arrays.asList(second), tankList(tile));
        tile.partRemoved(plain, 0);
        assertEquals(Arrays.asList(second), tankList(tile));

        tile.clearParts();
        assertTrue(tankList(tile).isEmpty());
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void tankInfoIsFlattenedInPartOrder() {
        TileMultipart tile = newFluidTile();
        RecordingFluidPart first = part("first", FluidRegistry.WATER, 0, 0);
        RecordingFluidPart second = part("second", FluidRegistry.LAVA, 0, 0);
        FluidTankInfo water = info(FluidRegistry.WATER, 100, 1000);
        FluidTankInfo lava = info(FluidRegistry.LAVA, 200, 2000);
        FluidTankInfo empty = new FluidTankInfo(null, 3000);
        first.tankInfo = new FluidTankInfo[] { water, empty };
        second.tankInfo = new FluidTankInfo[] { lava };
        tile.bindPart(first);
        tile.bindPart(second);

        assertArrayEquals(new FluidTankInfo[] { water, empty, lava }, fluid(tile).getTankInfo(ForgeDirection.UP));
        assertEquals(2, first.tankInfoCalls);
        assertEquals(2, second.tankInfoCalls);
        assertEquals(Arrays.asList(ForgeDirection.UP, ForgeDirection.UP), first.tankInfoDirections);
    }

    @Test
    void fillWalksEveryHandlerWithADecreasingCopy() {
        TileMultipart tile = newFluidTile();
        RecordingFluidPart first = part("first", FluidRegistry.WATER, 300, 0);
        RecordingFluidPart second = part("second", FluidRegistry.WATER, 500, 0);
        RecordingFluidPart third = part("third", FluidRegistry.WATER, 1000, 0);
        RecordingFluidPart afterFull = part("after", FluidRegistry.WATER, 100, 0);
        tile.bindPart(first);
        tile.bindPart(second);
        tile.bindPart(third);
        tile.bindPart(afterFull);
        FluidStack offered = new FluidStack(FluidRegistry.WATER, 1000);

        assertEquals(1000, fluid(tile).fill(ForgeDirection.DOWN, offered, false));
        assertEquals(1000, offered.amount);
        assertEquals(Arrays.asList(1000), first.fillAmounts);
        assertEquals(Arrays.asList(700), second.fillAmounts);
        assertEquals(Arrays.asList(200), third.fillAmounts);
        assertEquals(Arrays.asList(0), afterFull.fillAmounts);
        assertEquals(Arrays.asList(false), first.fillActions);
        assertNotSame(offered, first.fillStacks.get(0));
        assertTrue(offered.isFluidEqual(first.fillStacks.get(0)));
    }

    @Test
    void fillAndDrainCapabilityChecksStopAtTheFirstMatch() {
        TileMultipart tile = newFluidTile();
        RecordingFluidPart first = part("first", FluidRegistry.WATER, 0, 0);
        RecordingFluidPart match = part("match", FluidRegistry.WATER, 0, 0);
        RecordingFluidPart skipped = part("skipped", FluidRegistry.WATER, 0, 0);
        match.canFill = true;
        match.canDrain = true;
        skipped.canFill = true;
        skipped.canDrain = true;
        tile.bindPart(first);
        tile.bindPart(match);
        tile.bindPart(skipped);

        assertTrue(fluid(tile).canFill(ForgeDirection.EAST, FluidRegistry.WATER));
        assertTrue(fluid(tile).canDrain(ForgeDirection.WEST, FluidRegistry.WATER));
        assertEquals(1, first.canFillCalls);
        assertEquals(1, match.canFillCalls);
        assertEquals(0, skipped.canFillCalls);
        assertEquals(1, first.canDrainCalls);
        assertEquals(1, match.canDrainCalls);
        assertEquals(0, skipped.canDrainCalls);
    }

    @Test
    void amountDrainSimulatesThenCommitsOnlyMatchingFluid() {
        TileMultipart tile = newFluidTile();
        RecordingFluidPart first = part("first", FluidRegistry.WATER, 0, 300);
        RecordingFluidPart mismatch = part("mismatch", FluidRegistry.LAVA, 0, 400);
        RecordingFluidPart last = part("last", FluidRegistry.WATER, 0, 500);
        tile.bindPart(first);
        tile.bindPart(mismatch);
        tile.bindPart(last);

        FluidStack drained = fluid(tile).drain(ForgeDirection.NORTH, 700, true);

        assertEquals(FluidRegistry.WATER, drained.getFluid());
        assertEquals(700, drained.amount);
        assertEquals(Arrays.asList(700, 700), first.amountDrainAmounts);
        assertEquals(Arrays.asList(false, true), first.amountDrainActions);
        assertEquals(Arrays.asList(400), mismatch.amountDrainAmounts);
        assertEquals(Arrays.asList(false), mismatch.amountDrainActions);
        assertEquals(Arrays.asList(400, 400), last.amountDrainAmounts);
        assertEquals(Arrays.asList(false, true), last.amountDrainActions);
    }

    @Test
    void stackDrainUsesDecreasingCopiesWithoutCommittingSimulation() {
        TileMultipart tile = newFluidTile();
        RecordingFluidPart first = part("first", FluidRegistry.WATER, 0, 200);
        RecordingFluidPart mismatch = part("mismatch", FluidRegistry.LAVA, 0, 300);
        RecordingFluidPart last = part("last", FluidRegistry.WATER, 0, 400);
        tile.bindPart(first);
        tile.bindPart(mismatch);
        tile.bindPart(last);
        FluidStack requested = new FluidStack(FluidRegistry.WATER, 500);

        FluidStack drained = fluid(tile).drain(ForgeDirection.SOUTH, requested, false);

        assertEquals(FluidRegistry.WATER, drained.getFluid());
        assertEquals(500, drained.amount);
        assertEquals(500, requested.amount);
        assertEquals(Arrays.asList(500), first.stackDrainAmounts);
        assertEquals(Arrays.asList(300), mismatch.stackDrainAmounts);
        assertEquals(Arrays.asList(300), last.stackDrainAmounts);
        assertEquals(Arrays.asList(false), first.stackDrainActions);
        assertEquals(Arrays.asList(false), mismatch.stackDrainActions);
        assertEquals(Arrays.asList(false), last.stackDrainActions);
        assertNotSame(requested, first.stackDrainStacks.get(0));
        assertTrue(requested.isFluidEqual(first.stackDrainStacks.get(0)));
    }

    private static TileMultipart newFluidTile() {
        int traitId = MultipartMixinFactory.getId(TFluidHandlerTile.class.getName().replace('.', '/'));
        BitSet traits = new BitSet();
        traits.set(traitId);
        return (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
    }

    private static IFluidHandler fluid(TileMultipart tile) {
        return (IFluidHandler) tile;
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<IFluidHandler> tankList(TileMultipart tile) throws Exception {
        return (LinkedList<IFluidHandler>) tile.getClass().getMethod("tankList").invoke(tile);
    }

    private static void setTankList(TileMultipart tile, LinkedList<IFluidHandler> list) throws Exception {
        tile.getClass().getMethod("tankList_$eq", LinkedList.class).invoke(tile, list);
    }

    private static RecordingFluidPart part(String name, Fluid fluid, int fillLimit, int drainLimit) {
        return new RecordingFluidPart(name, fluid, fillLimit, drainLimit);
    }

    private static FluidTankInfo info(Fluid fluid, int amount, int capacity) {
        return new FluidTankInfo(new FluidStack(fluid, amount), capacity);
    }

    private static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "fluid_test:plain";
        }
    }

    private static final class RecordingFluidPart extends PlainPart implements IFluidHandler {

        private final String name;
        private final Fluid drainedFluid;
        private final int fillLimit;
        private final int drainLimit;
        private final List<Integer> fillAmounts = new ArrayList<>();
        private final List<Boolean> fillActions = new ArrayList<>();
        private final List<FluidStack> fillStacks = new ArrayList<>();
        private final List<Integer> amountDrainAmounts = new ArrayList<>();
        private final List<Boolean> amountDrainActions = new ArrayList<>();
        private final List<Integer> stackDrainAmounts = new ArrayList<>();
        private final List<Boolean> stackDrainActions = new ArrayList<>();
        private final List<FluidStack> stackDrainStacks = new ArrayList<>();
        private final List<ForgeDirection> tankInfoDirections = new ArrayList<>();
        private FluidTankInfo[] tankInfo = new FluidTankInfo[0];
        private boolean canFill;
        private boolean canDrain;
        private int tankInfoCalls;
        private int canFillCalls;
        private int canDrainCalls;

        private RecordingFluidPart(String name, Fluid drainedFluid, int fillLimit, int drainLimit) {
            this.name = name;
            this.drainedFluid = drainedFluid;
            this.fillLimit = fillLimit;
            this.drainLimit = drainLimit;
        }

        @Override
        public String getType() {
            return "fluid_test:" + name;
        }

        @Override
        public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
            fillAmounts.add(resource.amount);
            fillActions.add(doFill);
            fillStacks.add(resource);
            return Math.min(fillLimit, Math.max(0, resource.amount));
        }

        @Override
        public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
            stackDrainAmounts.add(resource.amount);
            stackDrainActions.add(doDrain);
            stackDrainStacks.add(resource);
            return drained(Math.min(drainLimit, resource.amount));
        }

        @Override
        public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
            amountDrainAmounts.add(maxDrain);
            amountDrainActions.add(doDrain);
            return drained(Math.min(drainLimit, maxDrain));
        }

        @Override
        public boolean canFill(ForgeDirection from, Fluid fluid) {
            canFillCalls++;
            return canFill;
        }

        @Override
        public boolean canDrain(ForgeDirection from, Fluid fluid) {
            canDrainCalls++;
            return canDrain;
        }

        @Override
        public FluidTankInfo[] getTankInfo(ForgeDirection from) {
            tankInfoCalls++;
            tankInfoDirections.add(from);
            return tankInfo;
        }

        private FluidStack drained(int amount) {
            return amount > 0 ? new FluidStack(drainedFluid, amount) : null;
        }
    }
}
