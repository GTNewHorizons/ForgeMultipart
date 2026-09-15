package codechicken.microblock;

import net.minecraft.nbt.NBTTagCompound;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;

public final class PostMicroClass {

    private PostMicroClass() {}

    public static int baseTraitId() {
        return PostMicroClass$.MODULE$.baseTraitId();
    }

    public static int clientTraitId() {
        return PostMicroClass$.MODULE$.clientTraitId();
    }

    public static void register() {
        PostMicroClass$.MODULE$.register();
    }

    public static Microblock create(boolean client, int material) {
        return PostMicroClass$.MODULE$.create(client, material);
    }

    public static Microblock createPart(String name, MCDataInput packet) {
        return PostMicroClass$.MODULE$.createPart(name, packet);
    }

    public static Microblock createPart(String name, NBTTagCompound nbt) {
        return PostMicroClass$.MODULE$.createPart(name, nbt);
    }

    public static Cuboid6[] aBounds() {
        return PostMicroClass$.MODULE$.aBounds();
    }

    public static void aBounds_$eq(Cuboid6[] bounds) {
        PostMicroClass$.MODULE$.aBounds_$eq(bounds);
    }

    public static String getName() {
        return PostMicroClass$.MODULE$.getName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Class<PostMicroblock> baseTrait() {
        return (Class) PostMicroClass$.MODULE$.baseTrait();
    }

    public static Class<PostMicroblockClient> clientTrait() {
        return PostMicroClass$.MODULE$.clientTrait();
    }

    public static float getResistanceFactor() {
        return PostMicroClass$.MODULE$.getResistanceFactor();
    }
}
