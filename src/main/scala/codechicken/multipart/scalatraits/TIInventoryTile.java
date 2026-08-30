package codechicken.multipart.scalatraits;

import java.util.LinkedList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.Tuple2;

/** Consumer-facing inventory contract implemented by the generated multipart tile. */
public interface TIInventoryTile extends ISidedInventory {

    void codechicken$multipart$scalatraits$TIInventoryTile$$super$copyFrom(TileMultipart that);

    void codechicken$multipart$scalatraits$TIInventoryTile$$super$bindPart(TMultiPart part);

    void codechicken$multipart$scalatraits$TIInventoryTile$$super$partRemoved(TMultiPart part, int position);

    void codechicken$multipart$scalatraits$TIInventoryTile$$super$clearParts();

    LinkedList<IInventory> invList();

    void invList_$eq(LinkedList<IInventory> inventories);

    Tuple2<IInventory, Object>[] slotMap();

    void slotMap_$eq(Tuple2<IInventory, Object>[] slots);

    void copyFrom(TileMultipart that);

    void bindPart(TMultiPart part);

    void partRemoved(TMultiPart part, int position);

    void clearParts();

    void rebuildSlotMap();

    @Override
    int getSizeInventory();

    @Override
    ItemStack getStackInSlot(int slot);

    @Override
    ItemStack decrStackSize(int slot, int amount);

    @Override
    ItemStack getStackInSlotOnClosing(int slot);

    @Override
    void setInventorySlotContents(int slot, ItemStack stack);

    @Override
    String getInventoryName();

    @Override
    boolean hasCustomInventoryName();

    @Override
    int getInventoryStackLimit();

    @Override
    boolean isUseableByPlayer(EntityPlayer player);

    @Override
    void openInventory();

    @Override
    void closeInventory();

    @Override
    boolean isItemValidForSlot(int slot, ItemStack stack);

    @Override
    int[] getAccessibleSlotsFromSide(int side);

    @Override
    boolean canInsertItem(int slot, ItemStack stack, int side);

    @Override
    boolean canExtractItem(int slot, ItemStack stack, int side);
}
