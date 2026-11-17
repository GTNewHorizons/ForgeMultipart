package codechicken.microblock;

import net.minecraft.nbt.NBTTagCompound;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;

public class FaceMicroClass {

    public static int baseTraitId() {
        return FaceMicroClass$.MODULE$.baseTraitId();
    }

    public static int clientTraitId() {
        return FaceMicroClass$.MODULE$.clientTraitId();
    }

    public static void register() {
        FaceMicroClass$.MODULE$.register();
    }

    public static Microblock create(boolean client, int material) {
        return FaceMicroClass$.MODULE$.create(client, material);
    }

    public static Microblock createPart(String name, MCDataInput packet) {
        return FaceMicroClass$.MODULE$.createPart(name, packet);
    }

    public static Microblock createPart(String name, NBTTagCompound nbt) {
        return FaceMicroClass$.MODULE$.createPart(name, nbt);
    }

    public static int getClassId() {
        return FaceMicroClass$.MODULE$.getClassId();
    }

    public static void register(int id) {
        FaceMicroClass$.MODULE$.register(id);
    }

    public static Cuboid6[] aBounds() {
        return FaceMicroClass$.MODULE$.aBounds();
    }

    public static void aBounds_$eq(Cuboid6[] bounds) {
        FaceMicroClass$.MODULE$.aBounds_$eq(bounds);
    }

    public static String getName() {
        return FaceMicroClass$.MODULE$.getName();
    }

    public static int itemSlot() {
        return FaceMicroClass$.MODULE$.itemSlot();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Class<FaceMicroblock> baseTrait() {
        return (Class) FaceMicroClass$.MODULE$.baseTrait();
    }

    public static Class<FaceMicroblockClient> clientTrait() {
        return FaceMicroClass$.MODULE$.clientTrait();
    }

    public static FacePlacement$ placementProperties() {
        return FaceMicroClass$.MODULE$.placementProperties();
    }

    public static float getResistanceFactor() {
        return FaceMicroClass$.MODULE$.getResistanceFactor();
    }
}
