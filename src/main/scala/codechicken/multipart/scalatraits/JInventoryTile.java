package codechicken.multipart.scalatraits;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.Tuple2;

/** Registered Java trait input that distributes inventory access among multipart inventories. */
public class JInventoryTile extends TileMultipart implements TIInventoryTile {

    private LinkedList<IInventory> invList = new LinkedList<>();

    @SuppressWarnings("unchecked")
    private Tuple2<IInventory, Object>[] slotMap = (Tuple2<IInventory, Object>[]) new Tuple2[0];

    @Override
    public LinkedList<IInventory> invList() {
        return invList;
    }

    @Override
    public void invList_$eq(LinkedList<IInventory> inventories) {
        invList = inventories;
    }

    @Override
    public Tuple2<IInventory, Object>[] slotMap() {
        return slotMap;
    }

    @Override
    public void slotMap_$eq(Tuple2<IInventory, Object>[] slots) {
        slotMap = slots;
    }

    @Override
    public void codechicken$multipart$scalatraits$TIInventoryTile$$super$copyFrom(TileMultipart that) {
        super.copyFrom(that);
    }

    @Override
    public void codechicken$multipart$scalatraits$TIInventoryTile$$super$bindPart(TMultiPart part) {
        super.bindPart(part);
    }

    @Override
    public void codechicken$multipart$scalatraits$TIInventoryTile$$super$partRemoved(TMultiPart part, int position) {
        super.partRemoved(part, position);
    }

    @Override
    public void codechicken$multipart$scalatraits$TIInventoryTile$$super$clearParts() {
        super.clearParts();
    }

    @Override
    public void copyFrom(TileMultipart that) {
        codechicken$multipart$scalatraits$TIInventoryTile$$super$copyFrom(that);
        if (that instanceof TIInventoryTile) {
            invList = ((TIInventoryTile) that).invList();
            rebuildSlotMap();
        }
    }

    @Override
    public void bindPart(TMultiPart part) {
        codechicken$multipart$scalatraits$TIInventoryTile$$super$bindPart(part);
        if (part instanceof IInventory) {
            invList.add((IInventory) part);
            rebuildSlotMap();
        }
    }

    @Override
    public void partRemoved(TMultiPart part, int position) {
        codechicken$multipart$scalatraits$TIInventoryTile$$super$partRemoved(part, position);
        if (part instanceof IInventory) {
            invList.remove(part);
            rebuildSlotMap();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void clearParts() {
        codechicken$multipart$scalatraits$TIInventoryTile$$super$clearParts();
        invList.clear();
        slotMap = (Tuple2<IInventory, Object>[]) new Tuple2[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rebuildSlotMap() {
        int totalSlots = 0;
        for (IInventory inventory : invList) {
            totalSlots += inventory.getSizeInventory();
        }

        Tuple2<IInventory, Object>[] rebuilt = (Tuple2<IInventory, Object>[]) new Tuple2[totalSlots];
        int index = 0;
        for (IInventory inventory : invList) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                rebuilt[index++] = new Tuple2<IInventory, Object>(inventory, Integer.valueOf(slot));
            }
        }
        slotMap = rebuilt;
    }

    @Override
    public int getSizeInventory() {
        return slotMap.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        return entry._1().getStackInSlot(((Number) entry._2()).intValue());
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        return entry._1().decrStackSize(((Number) entry._2()).intValue(), amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        return entry._1().getStackInSlotOnClosing(((Number) entry._2()).intValue());
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        entry._1().setInventorySlotContents(((Number) entry._2()).intValue(), stack);
    }

    @Override
    public String getInventoryName() {
        return "Multipart Inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        return entry._1().isItemValidForSlot(((Number) entry._2()).intValue(), stack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        List<Integer> accessibleSlots = new ArrayList<>();
        int base = 0;
        for (IInventory inventory : invList) {
            if (inventory instanceof ISidedInventory) {
                int[] localSlots = ((ISidedInventory) inventory).getAccessibleSlotsFromSide(side);
                for (int localSlot : localSlots) {
                    accessibleSlots.add(Integer.valueOf(localSlot + base));
                }
            }
            base += inventory.getSizeInventory();
        }

        return JInventoryTileAccess.toIntArray(accessibleSlots);
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        IInventory inventory = entry._1();
        return !(inventory instanceof ISidedInventory)
                || ((ISidedInventory) inventory).canInsertItem(((Number) entry._2()).intValue(), stack, side);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        Tuple2<IInventory, Object> entry = slotMap[slot];
        IInventory inventory = entry._1();
        return !(inventory instanceof ISidedInventory)
                || ((ISidedInventory) inventory).canExtractItem(((Number) entry._2()).intValue(), stack, side);
    }
}

/** Keeps primitive-array allocation outside the generated trait transformer. */
final class JInventoryTileAccess {

    private JInventoryTileAccess() {}

    static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = values.get(index).intValue();
        }
        return result;
    }
}
