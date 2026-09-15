package codechicken.microblock;

import net.minecraft.nbt.NBTTagCompound;

import codechicken.lib.data.MCDataInput;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class MicroblockClass implements IPartFactory2 {

    private final int baseTraitId;

    @SideOnly(Side.CLIENT)
    private int clientTraitId;

    private volatile boolean bitmap$0;

    public MicroblockClass() {
        baseTraitId = MicroblockGenerator$.MODULE$.registerTrait(baseTrait());
    }

    public abstract String getName();

    public abstract Class<? extends Microblock> baseTrait();

    @SideOnly(Side.CLIENT)
    public abstract Class<? extends MicroblockClient> clientTrait();

    public abstract float getResistanceFactor();

    public int baseTraitId() {
        return baseTraitId;
    }

    public int clientTraitId() {
        return bitmap$0 ? clientTraitId : clientTraitId$lzycompute();
    }

    private int clientTraitId$lzycompute() {
        synchronized (this) {
            if (!bitmap$0) {
                clientTraitId = MicroblockGenerator$.MODULE$.registerTrait(clientTrait());
                bitmap$0 = true;
            }
            return clientTraitId;
        }
    }

    public void register() {
        MultiPartRegistry.registerParts(this, getName());
    }

    public Microblock create(boolean client, int material) {
        return MicroblockGenerator$.MODULE$.create(this, material, client);
    }

    @Override
    public Microblock createPart(String name, MCDataInput packet) {
        return create(true, MicroMaterialRegistry.readMaterialID(packet));
    }

    @Override
    public Microblock createPart(String name, NBTTagCompound nbt) {
        return create(false, MicroMaterialRegistry.materialID(nbt.getString("material")));
    }
}
