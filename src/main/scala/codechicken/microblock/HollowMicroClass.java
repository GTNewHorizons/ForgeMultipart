package codechicken.microblock;

import net.minecraft.nbt.NBTTagCompound;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;
import scala.collection.Seq;

public final class HollowMicroClass {

    private HollowMicroClass() {}

    public static int baseTraitId() {
        return HollowMicroClass$.MODULE$.baseTraitId();
    }

    public static int clientTraitId() {
        return HollowMicroClass$.MODULE$.clientTraitId();
    }

    public static void register() {
        HollowMicroClass$.MODULE$.register();
    }

    public static Microblock create(boolean client, int material) {
        return HollowMicroClass$.MODULE$.create(client, material);
    }

    public static Microblock createPart(String name, MCDataInput packet) {
        return HollowMicroClass$.MODULE$.createPart(name, packet);
    }

    public static Microblock createPart(String name, NBTTagCompound nbt) {
        return HollowMicroClass$.MODULE$.createPart(name, nbt);
    }

    public static int getClassId() {
        return HollowMicroClass$.MODULE$.getClassId();
    }

    public static void register(int id) {
        HollowMicroClass$.MODULE$.register(id);
    }

    public static Seq<Cuboid6>[] pBoxes() {
        return HollowMicroClass$.MODULE$.pBoxes();
    }

    public static void pBoxes_$eq(Seq<Cuboid6>[] bounds) {
        HollowMicroClass$.MODULE$.pBoxes_$eq(bounds);
    }

    public static Cuboid6[] occBounds() {
        return HollowMicroClass$.MODULE$.occBounds();
    }

    public static void occBounds_$eq(Cuboid6[] bounds) {
        HollowMicroClass$.MODULE$.occBounds_$eq(bounds);
    }

    public static String getName() {
        return HollowMicroClass$.MODULE$.getName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Class<HollowMicroblock> baseTrait() {
        return (Class) HollowMicroClass$.MODULE$.baseTrait();
    }

    public static Class<HollowMicroblockClient> clientTrait() {
        return HollowMicroClass$.MODULE$.clientTrait();
    }

    public static int itemSlot() {
        return HollowMicroClass$.MODULE$.itemSlot();
    }

    public static HollowPlacement$ placementProperties() {
        return HollowMicroClass$.MODULE$.placementProperties();
    }

    public static float getResistanceFactor() {
        return HollowMicroClass$.MODULE$.getResistanceFactor();
    }
}
