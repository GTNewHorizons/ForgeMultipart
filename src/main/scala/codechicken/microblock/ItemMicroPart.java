package codechicken.microblock;

import java.util.List;

import net.minecraft.block.Block.SoundType;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import codechicken.lib.raytracer.RayTracer;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import scala.Tuple2;

public class ItemMicroPart extends Item {

    public ItemMicroPart() {
        setUnlocalizedName("microblock");
        setHasSubtypes(true);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        IMicroMaterial material = getMaterial(stack);
        CommonMicroClass mcrClass = CommonMicroClass.getMicroClass(stack.getItemDamage());
        int size = stack.getItemDamage() & 0xFF;
        if (material == null || mcrClass == null) {
            return "Unnamed";
        }
        return StatCollector
                .translateToLocalFormatted(mcrClass.getName() + "." + size + ".name", material.getLocalizedName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        List<ItemStack> stacks = (List<ItemStack>) list;
        CommonMicroClass[] classes = CommonMicroClass.classes();
        for (int classId = 0; classId < classes.length; classId++) {
            if (classes[classId] == null) {
                continue;
            }
            for (int size = 1; size <= 4; size <<= 1) {
                for (Tuple2<String, IMicroMaterial> material : MicroMaterialRegistry.getIdMap()) {
                    stacks.add(create(classId << 8 | size, material._1()));
                }
            }
        }
    }

    @Override
    public void registerIcons(IIconRegister register) {}

    @Override
    public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        int material = getMaterialID(item);
        CommonMicroClass mcrClass = CommonMicroClass.getMicroClass(item.getItemDamage());
        int size = item.getItemDamage() & 0xFF;
        if (material < 0 || mcrClass == null) {
            return false;
        }

        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        if (hit == null || hit.typeOfHit != MovingObjectType.BLOCK) {
            return false;
        }

        ExecutablePlacement placement = MicroblockPlacement$.MODULE$.apply(
                player,
                hit,
                size,
                material,
                !player.capabilities.isCreativeMode,
                mcrClass.placementProperties());
        if (placement == null) {
            return false;
        }

        if (!world.isRemote) {
            placement.place(world, player, item);
            if (!player.capabilities.isCreativeMode) {
                placement.consume(world, player, item);
            }

            SoundType sound = MicroMaterialRegistry.getMaterial(material).getSound();
            if (sound != null) {
                world.playSoundEffect(
                        placement.pos().x + 0.5,
                        placement.pos().y + 0.5,
                        placement.pos().z + 0.5,
                        sound.func_150496_b(),
                        (sound.getVolume() + 1.0F) / 2.0F,
                        sound.getPitch() * 0.8F);
            }
        }
        return true;
    }

    public static void checkTagCompound(ItemStack stack) {
        ItemMicroPart$.MODULE$.checkTagCompound(stack);
    }

    public static ItemStack create(int damage, int material) {
        return ItemMicroPart$.MODULE$.create(damage, material);
    }

    public static ItemStack create(int damage, String material) {
        return ItemMicroPart$.MODULE$.create(damage, material);
    }

    public static ItemStack create(int amount, int damage, String material) {
        return ItemMicroPart$.MODULE$.create(amount, damage, material);
    }

    public static IMicroMaterial getMaterial(ItemStack stack) {
        return ItemMicroPart$.MODULE$.getMaterial(stack);
    }

    public static int getMaterialID(ItemStack stack) {
        return ItemMicroPart$.MODULE$.getMaterialID(stack);
    }
}
