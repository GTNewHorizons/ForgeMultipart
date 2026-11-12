package codechicken.microblock;

public final class CommonMicroClass$ {

    public static final CommonMicroClass$ MODULE$ = new CommonMicroClass$();

    private final CommonMicroClass[] classes = new CommonMicroClass[256];

    private CommonMicroClass$() {}

    public CommonMicroClass[] classes() {
        return classes;
    }

    public CommonMicroClass getMicroClass(int modelId) {
        return classes[modelId >> 8];
    }

    public void registerMicroClass(CommonMicroClass microClass, int id) {
        if (classes[id] != null) {
            throw new IllegalArgumentException(
                    "Microblock class id " + id
                            + " is already taken by "
                            + classes[id].getName()
                            + " when adding "
                            + microClass.getName());
        }
        classes[id] = microClass;
    }
}
