package codechicken.microblock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.JCuboidPart;
import codechicken.multipart.TCuboidPart;
import codechicken.multipart.TMultiPart;

public abstract class Microblock extends TMultiPart implements TCuboidPart {

    private int material;
    private byte shape;

    public static int $lessinit$greater$default$1() {
        return Microblock$.MODULE$.$lessinit$greater$default$1();
    }

    public Microblock(int material) {
        this.material = material;
    }

    public int material() {
        return material;
    }

    public void material_$eq(int value) {
        material = value;
    }

    public byte shape() {
        return shape;
    }

    public void shape_$eq(byte value) {
        shape = value;
    }

    @Override
    public Iterable<IndexedCuboid6> getSubParts() {
        return JCuboidPart.subParts(this);
    }

    @Override
    public Iterable<Cuboid6> getCollisionBoxes() {
        return JCuboidPart.collisionBoxes(this);
    }

    @Override
    public void drawBreaking(RenderBlocks renderBlocks) {
        JCuboidPart.renderBreaking(this, renderBlocks);
    }

    public abstract MicroblockClass microClass();

    @Override
    public String getType() {
        return microClass().getName();
    }

    @Override
    public float getStrength(MovingObjectPosition hit, EntityPlayer player) {
        MicroMaterialRegistry.IMicroMaterial microMaterial = getIMaterial();
        return microMaterial == null ? super.getStrength(hit, player) : microMaterial.getStrength(player);
    }

    @Override
    public boolean doesTick() {
        return false;
    }

    public int getSize() {
        return shape >> 4;
    }

    public int getShape() {
        return shape & 0xF;
    }

    public void setShape(int size, int slot) {
        shape = (byte) (size << 4 | slot);
    }

    public int getMaterial() {
        return material;
    }

    public MicroMaterialRegistry.IMicroMaterial getIMaterial() {
        return MicroMaterialRegistry.getMaterial(material);
    }

    public abstract int itemClassID();

    @Override
    public List<ItemStack> getDrops() {
        int remaining = getSize();
        List<ItemStack> items = new ArrayList<>(3);
        for (int size = 4; size > 0; size /= 2) {
            int amount = remaining / size;
            remaining -= amount * size;
            if (amount > 0) {
                items.add(
                        ItemMicroPart.create(
                                amount,
                                size | itemClassID() << 8,
                                MicroMaterialRegistry.materialName(material)));
            }
        }
        return items;
    }

    @Override
    public ItemStack pickItem(MovingObjectPosition hit) {
        int size = getSize();
        for (int itemSize = 4; itemSize > 0; itemSize /= 2) {
            if (size % itemSize == 0 && size / itemSize >= 1) {
                return ItemMicroPart
                        .create(itemSize | itemClassID() << 8, MicroMaterialRegistry.materialName(material));
            }
        }
        return null;
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        MicroMaterialRegistry.writeMaterialID(packet, material);
        packet.writeByte(shape);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        shape = packet.readByte();
    }

    public void sendShapeUpdate() {
        getWriteStream().writeByte(shape);
    }

    @Override
    public void read(MCDataInput packet) {
        super.read(packet);
        tile().notifyPartChange(this);
    }

    @Override
    public void save(NBTTagCompound tag) {
        tag.setByte("shape", shape);
        tag.setString("material", MicroMaterialRegistry.materialName(material));
    }

    @Override
    public void load(NBTTagCompound tag) {
        shape = tag.getByte("shape");
        material = MicroMaterialRegistry.materialID(tag.getString("material"));
    }

    public boolean isTransparent() {
        return getIMaterial().isTransparent();
    }

    @Override
    public int getLightValue() {
        return getIMaterial().getLightValue();
    }

    @Override
    public float explosionResistance(Entity entity) {
        return getIMaterial().explosionResistance(entity) * microClass().getResistanceFactor();
    }
}
