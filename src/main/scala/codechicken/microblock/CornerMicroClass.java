package codechicken.microblock;

import net.minecraft.nbt.NBTTagCompound;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;

public final class CornerMicroClass {

    private CornerMicroClass() {}

    public static int baseTraitId() {
        return CornerMicroClass$.MODULE$.baseTraitId();
    }

    public static int clientTraitId() {
        return CornerMicroClass$.MODULE$.clientTraitId();
    }

    public static void register() {
        CornerMicroClass$.MODULE$.register();
    }

    public static Microblock create(boolean client, int material) {
        return CornerMicroClass$.MODULE$.create(client, material);
    }

    public static Microblock createPart(String name, MCDataInput packet) {
        return CornerMicroClass$.MODULE$.createPart(name, packet);
    }

    public static Microblock createPart(String name, NBTTagCompound nbt) {
        return CornerMicroClass$.MODULE$.createPart(name, nbt);
    }

    public static int getClassId() {
        return CornerMicroClass$.MODULE$.getClassId();
    }

    public static void register(int id) {
        CornerMicroClass$.MODULE$.register(id);
    }

    public static Cuboid6[] aBounds() {
        return CornerMicroClass$.MODULE$.aBounds();
    }

    public static void aBounds_$eq(Cuboid6[] bounds) {
        CornerMicroClass$.MODULE$.aBounds_$eq(bounds);
    }

    public static String getName() {
        return CornerMicroClass$.MODULE$.getName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Class<CornerMicroblock> baseTrait() {
        return (Class) CornerMicroClass$.MODULE$.baseTrait();
    }

    public static Class<CommonMicroblockClient> clientTrait() {
        return CornerMicroClass$.MODULE$.clientTrait();
    }

    public static int itemSlot() {
        return CornerMicroClass$.MODULE$.itemSlot();
    }

    public static CornerPlacement$ placementProperties() {
        return CornerMicroClass$.MODULE$.placementProperties();
    }

    public static float getResistanceFactor() {
        return CornerMicroClass$.MODULE$.getResistanceFactor();
    }
}
