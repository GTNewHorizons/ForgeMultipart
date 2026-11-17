package codechicken.multipart;

/** Binary bridge for Scala implementations compiled against the original trait. */
@Deprecated
public abstract class TNormalOcclusion$class {

    private TNormalOcclusion$class() {}

    public static boolean occlusionTest(TNormalOcclusion part, TMultiPart npart) {
        return NormalOcclusionTest.apply(part, npart)
                && part.codechicken$multipart$TNormalOcclusion$$super$occlusionTest(npart);
    }

    public static void $init$(TNormalOcclusion part) {}
}
