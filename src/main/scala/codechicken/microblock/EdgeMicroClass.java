package codechicken.microblock;

import net.minecraft.nbt.NBTTagCompound;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;

public final class EdgeMicroClass {

    private EdgeMicroClass() {}

    public static int baseTraitId() {
        return EdgeMicroClass$.MODULE$.baseTraitId();
    }

    public static int clientTraitId() {
        return EdgeMicroClass$.MODULE$.clientTraitId();
    }

    public static void register() {
        EdgeMicroClass$.MODULE$.register();
    }

    public static Microblock create(boolean client, int material) {
        return EdgeMicroClass$.MODULE$.create(client, material);
    }

    public static Microblock createPart(String name, MCDataInput packet) {
        return EdgeMicroClass$.MODULE$.createPart(name, packet);
    }

    public static Microblock createPart(String name, NBTTagCompound nbt) {
        return EdgeMicroClass$.MODULE$.createPart(name, nbt);
    }

    public static int getClassId() {
        return EdgeMicroClass$.MODULE$.getClassId();
    }

    public static void register(int id) {
        EdgeMicroClass$.MODULE$.register(id);
    }

    public static Cuboid6[] aBounds() {
        return EdgeMicroClass$.MODULE$.aBounds();
    }

    public static void aBounds_$eq(Cuboid6[] bounds) {
        EdgeMicroClass$.MODULE$.aBounds_$eq(bounds);
    }

    public static int itemSlot() {
        return EdgeMicroClass$.MODULE$.itemSlot();
    }

    public static String getName() {
        return EdgeMicroClass$.MODULE$.getName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Class<EdgeMicroblock> baseTrait() {
        return (Class) EdgeMicroClass$.MODULE$.baseTrait();
    }

    public static Class<CommonMicroblockClient> clientTrait() {
        return EdgeMicroClass$.MODULE$.clientTrait();
    }

    public static EdgePlacement$ placementProperties() {
        return EdgeMicroClass$.MODULE$.placementProperties();
    }

    public static float getResistanceFactor() {
        return EdgeMicroClass$.MODULE$.getResistanceFactor();
    }
}
