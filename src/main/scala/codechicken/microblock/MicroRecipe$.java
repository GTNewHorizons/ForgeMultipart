package codechicken.microblock;

import java.util.Objects;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.RecipeSorter;

import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.handler.MicroblockProxy;
import scala.Tuple2;
import scala.Tuple3;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

/** Scala-compatible singleton carrying the microblock recipe implementation. */
public final class MicroRecipe$ implements IRecipe {

    public static final MicroRecipe$ MODULE$ = new MicroRecipe$();

    private final Map<Object, Object> splitMap;

    private MicroRecipe$() {
        RecipeSorter.register("fmp:micro", getClass(), RecipeSorter.Category.SHAPED, "after:forge:shapelessore");
        Map<Object, Object> splits = Map$.MODULE$.empty();
        splits = splits.updated(0, 3);
        splits = splits.updated(1, 3);
        splitMap = splits.updated(3, 2);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemMicroPart.create(1, 1, "tile.stone");
    }

    @Override
    public int getRecipeSize() {
        return 9;
    }

    @Override
    public boolean matches(InventoryCrafting crafting, World world) {
        return getCraftingResult(crafting) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting crafting) {
        ItemStack result = getHollowResult(crafting);
        if (result != null) {
            return result;
        }
        result = getGluingResult(crafting);
        if (result != null) {
            return result;
        }
        result = getThinningResult(crafting);
        if (result != null) {
            return result;
        }
        result = getSplittingResult(crafting);
        return result != null ? result : getHollowFillResult(crafting);
    }

    public ItemStack create(int amount, int microClass, int size, int material) {
        if (size == 8) {
            ItemStack item = MicroMaterialRegistry.getMaterial(material).getItem().copy();
            item.stackSize = amount;
            return item;
        }
        return ItemMicroPart.create(amount, microClass << 8 | size, MicroMaterialRegistry.materialName(material));
    }

    public int microMaterial(ItemStack item) {
        return Objects.equals(item.getItem(), MicroblockProxy.itemMicro()) ? ItemMicroPart.getMaterialID(item)
                : findMaterial(item);
    }

    public int microClass(ItemStack item) {
        return Objects.equals(item.getItem(), MicroblockProxy.itemMicro()) ? item.getItemDamage() >> 8 : 0;
    }

    public int microSize(ItemStack item) {
        return Objects.equals(item.getItem(), MicroblockProxy.itemMicro()) ? item.getItemDamage() & 0xFF : 8;
    }

    public ItemStack getHollowResult(InventoryCrafting crafting) {
        if (crafting.getStackInRowAndColumn(1, 1) != null) {
            return null;
        }

        ItemStack first = crafting.getStackInRowAndColumn(0, 0);
        if (first == null || !Objects.equals(first.getItem(), MicroblockProxy.itemMicro()) || microClass(first) != 0) {
            return null;
        }
        int size = microSize(first);
        int material = microMaterial(first);

        for (int slot = 1; slot <= 8; slot++) {
            if (slot == 4) {
                continue;
            }
            ItemStack item = crafting.getStackInSlot(slot);
            if (item == null || !Objects.equals(item.getItem(), MicroblockProxy.itemMicro())
                    || microMaterial(item) != material
                    || item.getItemDamage() != first.getItemDamage()) {
                return null;
            }
        }
        return create(8, 1, size, material);
    }

    public ItemStack getGluingResult(InventoryCrafting crafting) {
        int size = 0;
        int count = 0;
        int smallest = 0;
        int microClass = 0;
        int material = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = crafting.getStackInSlot(slot);
            if (item == null) {
                continue;
            }
            if (!Objects.equals(item.getItem(), MicroblockProxy.itemMicro())) {
                return null;
            }
            if (count == 0) {
                size = microSize(item);
                microClass = microClass(item);
                material = microMaterial(item);
                count = 1;
                smallest = size;
            } else if (microClass(item) != microClass || microMaterial(item) != material) {
                return null;
            } else if (microClass >= 2 && microSize(item) != smallest) {
                return null;
            } else {
                smallest = Math.min(smallest, microSize(item));
                count++;
                size += microSize(item);
            }
        }

        if (count <= 1) {
            return null;
        }
        switch (microClass) {
            case 3:
                return count == 2 ? create(1, 0, smallest, material) : null;
            case 2:
                if (count == 2) {
                    return create(1, 3, smallest, material);
                }
                return count == 4 ? create(1, 0, smallest, material) : null;
            case 1:
            case 0:
                int base = 0;
                for (int candidate = 1; candidate <= 4; candidate <<= 1) {
                    if ((candidate & size) != 0) {
                        base = candidate;
                        break;
                    }
                }
                if (base == 0) {
                    return create(size / 8, 0, 8, material);
                }
                return base <= smallest ? null : create(size / base, microClass, base, material);
            default:
                return null;
        }
    }

    public Tuple3<Saw, Object, Object> getSaw(InventoryCrafting crafting) {
        int slot = findSawSlot(crafting);
        if (slot < 0) {
            return new Tuple3<>(null, 0, 0);
        }
        ItemStack item = crafting.getStackInRowAndColumn(slot % 3, slot / 3);
        return new Tuple3<>((Saw) item.getItem(), slot / 3, slot % 3);
    }

    public boolean canCut(Saw saw, ItemStack sawItem, int material) {
        int sawStrength = saw.getCuttingStrength(sawItem);
        int materialStrength = MicroMaterialRegistry.getMaterial(material).getCutterStrength();
        return sawStrength >= materialStrength || sawStrength == MicroMaterialRegistry.getMaxCuttingStrength();
    }

    public ItemStack getThinningResult(InventoryCrafting crafting) {
        int sawSlot = findSawSlot(crafting);
        if (sawSlot < 0) {
            return null;
        }
        int row = sawSlot / 3;
        int column = sawSlot % 3;
        ItemStack sawItem = crafting.getStackInRowAndColumn(column, row);
        Saw saw = (Saw) sawItem.getItem();
        ItemStack item = crafting.getStackInRowAndColumn(column, row + 1);
        if (item == null) {
            return null;
        }

        int size = microSize(item);
        int material = microMaterial(item);
        int microClass = microClass(item);
        if (size == 1 || material < 0 || !canCut(saw, sawItem, material)) {
            return null;
        }

        for (int checkedRow = 0; checkedRow < 3; checkedRow++) {
            for (int checkedColumn = 0; checkedColumn < 3; checkedColumn++) {
                if ((checkedColumn != column || checkedRow != row && checkedRow != row + 1)
                        && crafting.getStackInRowAndColumn(checkedColumn, checkedRow) != null) {
                    return null;
                }
            }
        }
        return create(2, microClass, size / 2, material);
    }

    public int findMaterial(ItemStack item) {
        for (Tuple2<String, IMicroMaterial> entry : MicroMaterialRegistry.getIdMap()) {
            ItemStack materialItem = entry._2().getItem();
            if (Objects.equals(item.getItem(), materialItem.getItem())
                    && item.getItemDamage() == materialItem.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(item, materialItem)) {
                return MicroMaterialRegistry.materialID(entry._1());
            }
        }
        return -1;
    }

    public Map<Object, Object> splitMap() {
        return splitMap;
    }

    public ItemStack getSplittingResult(InventoryCrafting crafting) {
        int sawSlot = findSawSlot(crafting);
        if (sawSlot < 0) {
            return null;
        }
        int row = sawSlot / 3;
        int column = sawSlot % 3;
        ItemStack sawItem = crafting.getStackInRowAndColumn(column, row);
        Saw saw = (Saw) sawItem.getItem();
        ItemStack item = crafting.getStackInRowAndColumn(column + 1, row);
        if (item == null || !Objects.equals(item.getItem(), MicroblockProxy.itemMicro())) {
            return null;
        }

        int microClass = microClass(item);
        int material = microMaterial(item);
        if (!canCut(saw, sawItem, material)) {
            return null;
        }

        if (!splitMap.contains(microClass)) {
            return null;
        }
        int splitClass = ((Number) splitMap.apply(microClass)).intValue();

        for (int checkedRow = 0; checkedRow < 3; checkedRow++) {
            for (int checkedColumn = 0; checkedColumn < 3; checkedColumn++) {
                if ((checkedRow != row || checkedColumn != column && checkedColumn != column + 1)
                        && crafting.getStackInRowAndColumn(checkedColumn, checkedRow) != null) {
                    return null;
                }
            }
        }
        return create(2, splitClass, microSize(item), material);
    }

    public ItemStack getHollowFillResult(InventoryCrafting crafting) {
        ItemStack cover = null;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = crafting.getStackInSlot(slot);
            if (item == null) {
                continue;
            }
            if (!Objects.equals(item.getItem(), MicroblockProxy.itemMicro()) || cover != null
                    || microClass(item) != 1) {
                return null;
            }
            cover = item;
        }
        return cover == null ? null : create(1, 0, microSize(cover), microMaterial(cover));
    }

    private int findSawSlot(InventoryCrafting crafting) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = crafting.getStackInRowAndColumn(slot % 3, slot / 3);
            if (item != null && item.getItem() instanceof Saw) {
                return slot;
            }
        }
        return -1;
    }
}
