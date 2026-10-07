package codechicken.microblock;

public abstract class CommonMicroClass extends MicroblockClass {

    private int classId;

    public static CommonMicroClass[] classes() {
        return CommonMicroClass$.MODULE$.classes();
    }

    public static CommonMicroClass getMicroClass(int modelId) {
        return CommonMicroClass$.MODULE$.getMicroClass(modelId);
    }

    public static void registerMicroClass(CommonMicroClass microClass, int id) {
        CommonMicroClass$.MODULE$.registerMicroClass(microClass, id);
    }

    public int getClassId() {
        return classId;
    }

    public abstract int itemSlot();

    public abstract PlacementProperties placementProperties();

    public void register(int id) {
        register();
        classId = id;
        CommonMicroClass$.MODULE$.registerMicroClass(this, id);
    }
}
