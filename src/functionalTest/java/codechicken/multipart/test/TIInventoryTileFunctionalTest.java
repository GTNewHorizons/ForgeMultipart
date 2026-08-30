package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.scalatraits.JInventoryTile;
import codechicken.multipart.scalatraits.TIInventoryTile;
import scala.Tuple2;
import scala.collection.immutable.Nil$;

/** Consumer-visible state and behavior of the generated inventory trait. */
class TIInventoryTileFunctionalTest {

    @Test
    void copyFromSharesACompatibleInventoryListAndRebuildsTheSlotMap() {
        TileMultipart source = newInventoryTile();
        TileMultipart target = newInventoryTile();
        InventoryPart part = new InventoryPart("source", 2);
        source.bindPart(part);

        TIInventoryTile sourceInventory = inventory(source);
        TIInventoryTile targetInventory = inventory(target);
        Tuple2<IInventory, Object>[] sourceMap = sourceInventory.slotMap();
        target.copyFrom(source);

        assertSame(sourceInventory.invList(), targetInventory.invList());
        assertNotSame(sourceMap, targetInventory.slotMap());
        assertEquals(2, targetInventory.getSizeInventory());

        LinkedList<IInventory> retainedList = targetInventory.invList();
        Tuple2<IInventory, Object>[] retainedMap = targetInventory.slotMap();
        target.copyFrom(new TileMultipart());
        assertSame(retainedList, targetInventory.invList());
        assertSame(retainedMap, targetInventory.slotMap());
    }

    @Test
    void bindRemoveAndClearTrackOnlyInventoriesInOrder() {
        TileMultipart tile = newInventoryTile();
        InventoryPart first = new InventoryPart("first", 1);
        InventoryPart second = new InventoryPart("second", 2);
        PlainPart plain = new PlainPart();
        tile.addPart_do(first);
        tile.addPart_do(plain);
        tile.addPart_do(second);

        TIInventoryTile inventory = inventory(tile);
        assertEquals(Arrays.asList(first, second), inventory.invList());
        assertEquals(3, inventory.getSizeInventory());

        tile.partRemoved(first, 0);
        assertEquals(Arrays.asList(second), inventory.invList());
        assertEquals(2, inventory.getSizeInventory());
        tile.partRemoved(plain, 0);
        assertEquals(Arrays.asList(second), inventory.invList());

        tile.clearParts();
        assertTrue(inventory.invList().isEmpty());
        assertEquals(0, inventory.slotMap().length);
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void flattenedSlotsRouteInventoryOperationsToLocalSlots() {
        TileMultipart tile = newInventoryTile();
        InventoryPart first = new InventoryPart("first", 1);
        InventoryPart second = new InventoryPart("second", 2);
        ItemStack apple = new ItemStack(Items.apple);
        ItemStack stick = new ItemStack(Items.stick);
        ItemStack decremented = new ItemStack(Items.coal, 4);
        ItemStack closing = new ItemStack(Items.bowl);
        first.stacks[0] = apple;
        second.stacks[1] = stick;
        second.decrementResult = decremented;
        second.closingResult = closing;
        second.valid = false;
        tile.bindPart(first);
        tile.bindPart(second);

        TIInventoryTile inventory = inventory(tile);
        assertEquals(3, inventory.getSizeInventory());
        assertSame(apple, inventory.getStackInSlot(0));
        assertSame(stick, inventory.getStackInSlot(2));
        assertSame(decremented, inventory.decrStackSize(1, 4));
        assertEquals(0, second.lastDecrementSlot);
        assertEquals(4, second.lastDecrementAmount);
        assertSame(closing, inventory.getStackInSlotOnClosing(2));
        assertEquals(1, second.lastClosingSlot);

        ItemStack replacement = new ItemStack(Items.diamond);
        inventory.setInventorySlotContents(1, replacement);
        assertSame(replacement, second.stacks[0]);
        assertFalse(inventory.isItemValidForSlot(2, replacement));
        assertEquals(1, second.lastValiditySlot);
        assertSame(replacement, second.lastValidityStack);
    }

    @Test
    void directTraitCallRebuildsTheMapAfterInventorySizeChanges() {
        TileMultipart tile = newInventoryTile();
        InventoryPart part = new InventoryPart("resizable", 1);
        tile.bindPart(part);
        TIInventoryTile api = (TIInventoryTile) tile;
        assertEquals(1, api.getSizeInventory());

        part.resize(3);
        api.rebuildSlotMap();

        assertEquals(3, api.getSizeInventory());
        assertSame(part, api.slotMap()[2]._1());
        assertEquals(2, ((Number) api.slotMap()[2]._2()).intValue());
    }

    @Test
    void tileInventoryMetadataAndLifecycleRemainFixed() {
        TileMultipart tile = newInventoryTile();
        InventoryPart part = new InventoryPart("metadata", 1);
        tile.bindPart(part);
        TIInventoryTile inventory = inventory(tile);

        assertEquals("Multipart Inventory", inventory.getInventoryName());
        assertFalse(inventory.hasCustomInventoryName());
        assertEquals(64, inventory.getInventoryStackLimit());
        assertTrue(inventory.isUseableByPlayer(null));
        inventory.openInventory();
        inventory.closeInventory();
        assertEquals(0, part.openCalls);
        assertEquals(0, part.closeCalls);
    }

    @Test
    void accessibleSlotsIncludeOnlySidedInventoriesWithGlobalOffsets() {
        TileMultipart tile = newInventoryTile();
        InventoryPart plainFirst = new InventoryPart("plain_first", 2);
        SidedInventoryPart sidedFirst = new SidedInventoryPart("sided_first", 1, new int[] { 0 });
        InventoryPart plainSecond = new InventoryPart("plain_second", 1);
        SidedInventoryPart sidedSecond = new SidedInventoryPart("sided_second", 3, new int[] { 2, 0 });
        tile.bindPart(plainFirst);
        tile.bindPart(sidedFirst);
        tile.bindPart(plainSecond);
        tile.bindPart(sidedSecond);

        assertArrayEquals(new int[] { 2, 6, 4 }, inventory(tile).getAccessibleSlotsFromSide(5));
        assertEquals(5, sidedFirst.lastAccessibleSide);
        assertEquals(5, sidedSecond.lastAccessibleSide);
    }

    @Test
    void sidedInsertAndExtractChecksReceiveLocalSlotsWhilePlainInventoriesAllowBoth() {
        TileMultipart tile = newInventoryTile();
        InventoryPart plain = new InventoryPart("plain", 2);
        SidedInventoryPart sided = new SidedInventoryPart("sided", 2, new int[0]);
        sided.insertResult = false;
        sided.extractResult = true;
        tile.bindPart(plain);
        tile.bindPart(sided);
        TIInventoryTile inventory = inventory(tile);
        ItemStack stack = new ItemStack(Items.apple);

        assertTrue(inventory.canInsertItem(1, stack, 3));
        assertTrue(inventory.canExtractItem(0, stack, 4));
        assertFalse(inventory.canInsertItem(3, stack, 5));
        assertTrue(inventory.canExtractItem(2, stack, 1));
        assertEquals(1, sided.lastInsertSlot);
        assertEquals(5, sided.lastInsertSide);
        assertEquals(0, sided.lastExtractSlot);
        assertEquals(1, sided.lastExtractSide);
    }

    private static TileMultipart newInventoryTile() {
        int traitId = MultipartMixinFactory.getId(JInventoryTile.class.getName().replace('.', '/'));
        BitSet traits = new BitSet();
        traits.set(traitId);
        return (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
    }

    private static TIInventoryTile inventory(TileMultipart tile) {
        return (TIInventoryTile) tile;
    }

    private static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "inventory_test:plain";
        }
    }

    private static class InventoryPart extends PlainPart implements IInventory {

        private final String name;
        private ItemStack[] stacks;
        private ItemStack decrementResult;
        private ItemStack closingResult;
        private boolean valid = true;
        private int lastDecrementSlot = -1;
        private int lastDecrementAmount = -1;
        private int lastClosingSlot = -1;
        private int lastValiditySlot = -1;
        private ItemStack lastValidityStack;
        private int openCalls;
        private int closeCalls;

        private InventoryPart(String name, int size) {
            this.name = name;
            stacks = new ItemStack[size];
        }

        @Override
        public String getType() {
            return "inventory_test:" + name;
        }

        @Override
        public int getSizeInventory() {
            return stacks.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stacks[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            lastDecrementSlot = slot;
            lastDecrementAmount = amount;
            return decrementResult;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            lastClosingSlot = slot;
            return closingResult;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            stacks[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return name;
        }

        @Override
        public boolean hasCustomInventoryName() {
            return true;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {}

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return false;
        }

        @Override
        public void openInventory() {
            openCalls++;
        }

        @Override
        public void closeInventory() {
            closeCalls++;
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            lastValiditySlot = slot;
            lastValidityStack = stack;
            return valid;
        }

        private void resize(int size) {
            stacks = Arrays.copyOf(stacks, size);
        }
    }

    private static final class SidedInventoryPart extends InventoryPart implements ISidedInventory {

        private final int[] accessibleSlots;
        private boolean insertResult;
        private boolean extractResult;
        private int lastAccessibleSide = -1;
        private int lastInsertSlot = -1;
        private int lastInsertSide = -1;
        private int lastExtractSlot = -1;
        private int lastExtractSide = -1;

        private SidedInventoryPart(String name, int size, int[] accessibleSlots) {
            super(name, size);
            this.accessibleSlots = accessibleSlots;
        }

        @Override
        public int[] getAccessibleSlotsFromSide(int side) {
            lastAccessibleSide = side;
            return accessibleSlots;
        }

        @Override
        public boolean canInsertItem(int slot, ItemStack stack, int side) {
            lastInsertSlot = slot;
            lastInsertSide = side;
            return insertResult;
        }

        @Override
        public boolean canExtractItem(int slot, ItemStack stack, int side) {
            lastExtractSlot = slot;
            lastExtractSide = side;
            return extractResult;
        }
    }
}
