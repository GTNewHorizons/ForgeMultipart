package codechicken.multipart;

import codechicken.lib.vec.Cuboid6;

/** Tests whether every required part retains an exclusively owned voxel. */
public class PartialOcclusionTest {

    private final int size;
    private final int res = 8;
    private final byte[] bits;
    private final boolean[] partial;

    public PartialOcclusionTest(int size) {
        this.size = size;
        bits = new byte[res * res * res];
        partial = new boolean[size];
    }

    public int res() {
        return res;
    }

    public byte[] bits() {
        return bits;
    }

    public boolean[] partial() {
        return partial;
    }

    public void fill(int index, JPartialOcclusion part) {
        fill(index, part.getPartialOcclusionBoxes(), part.allowCompleteOcclusion());
    }

    public void fill(int index, Iterable<Cuboid6> boxes, boolean complete) {
        partial[index] = !complete;
        for (Cuboid6 box : boxes) {
            fill(index + 1, box);
        }
    }

    public void fill(int value, Cuboid6 box) {
        int minX = (int) (box.min.x * res + 0.5);
        int minY = (int) (box.min.y * res + 0.5);
        int minZ = (int) (box.min.z * res + 0.5);
        int maxX = (int) (box.max.x * res + 0.5);
        int maxY = (int) (box.max.y * res + 0.5);
        int maxZ = (int) (box.max.z * res + 0.5);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    int bitIndex = (x * res + y) * res + z;
                    if (bits[bitIndex] == 0) {
                        bits[bitIndex] = (byte) value;
                    } else {
                        bits[bitIndex] = -1;
                    }
                }
            }
        }
    }

    public boolean apply() {
        boolean[] visible = new boolean[size];
        for (byte owner : bits) {
            if (owner > 0) {
                visible[owner - 1] = true;
            }
        }

        for (int index = 0; index < partial.length; index++) {
            if (partial[index] && !visible[index]) {
                return false;
            }
        }
        return true;
    }
}
