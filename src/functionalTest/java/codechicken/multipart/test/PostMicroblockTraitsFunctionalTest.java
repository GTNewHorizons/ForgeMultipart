package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.vec.Cuboid6;
import codechicken.microblock.EdgeMicroClass;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;
import codechicken.microblock.PostMicroClass;
import codechicken.microblock.PostMicroClass$;
import codechicken.microblock.PostMicroblock;

class PostMicroblockTraitsFunctionalTest {

    @Test
    void generatedPostsKeepAllNineBoundsTorchSupportAndEdgeItems() {
        Microblock part = createPost();
        PostMicroblock post = (PostMicroblock) part;
        assertSame(PostMicroClass$.MODULE$, post.microClass());
        assertSame(PostMicroClass$.MODULE$, part.microClass());
        assertEquals("mcr_post", part.getType());
        assertEquals(0.5F, post.getResistanceFactor());
        assertFalse(post.allowCompleteOcclusion());
        int[] longAxes = { 1, 2, 0 };
        for (int size : new int[] { 2, 4, 6 }) {
            for (int axis = 0; axis < 3; axis++) {
                part.setShape(size, axis);
                assertEquals(size, part.getSize());
                assertEquals(axis, part.getShape());
                assertEquals(axis == 0, part.canPlaceTorchOnTop());
                assertEquals(EdgeMicroClass.getClassId(), part.itemClassID());
                Cuboid6 bounds = part.getBounds();
                assertSame(PostMicroClass.aBounds()[size * 16 + axis], bounds);
                double[] min = { bounds.min.x, bounds.min.y, bounds.min.z };
                double[] max = { bounds.max.x, bounds.max.y, bounds.max.z };
                for (int coordinate = 0; coordinate < 3; coordinate++) {
                    assertEquals(coordinate == longAxes[axis] ? 0 : 0.5 - size / 16D, min[coordinate], 1e-12);
                    assertEquals(coordinate == longAxes[axis] ? 1 : 0.5 + size / 16D, max[coordinate], 1e-12);
                }
                assertSame(bounds, post.getOcclusionBoxes().get(0));
                assertSame(bounds, post.getPartialOcclusionBoxes().get(0));
                int volume = 0;
                for (ItemStack drop : part.getDrops()) {
                    assertEquals(EdgeMicroClass.getClassId(), drop.getItemDamage() >> 8);
                    volume += (drop.getItemDamage() & 255) * drop.stackSize;
                }
                assertEquals(size, volume);
                ItemStack pick = part.pickItem(null);
                assertEquals(EdgeMicroClass.getClassId(), pick.getItemDamage() >> 8);
                assertEquals(size == 4 ? 4 : 2, pick.getItemDamage() & 255);
            }
        }
    }

    @Test
    void boundsKeepSignedIndexFailuresAndLiveArrayReplacement() {
        Microblock part = createPost();
        for (int shape : new int[] { 0, 16, 35, 127 }) {
            part.shape_$eq((byte) shape);
            assertNull(part.getBounds());
        }
        for (int shape = -128; shape < 0; shape++) {
            part.shape_$eq((byte) shape);
            assertThrows(ArrayIndexOutOfBoundsException.class, part::getBounds);
        }
        Cuboid6[] original = PostMicroClass.aBounds();
        Cuboid6[] replacement = new Cuboid6[256];
        Cuboid6 sentinel = new Cuboid6(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
        replacement[65] = sentinel;
        part.setShape(4, 1);
        try {
            PostMicroClass.aBounds_$eq(replacement);
            assertSame(sentinel, part.getBounds());
            replacement[65] = null;
            assertNull(part.getBounds());
            PostMicroClass.aBounds_$eq(new Cuboid6[1]);
            assertThrows(ArrayIndexOutOfBoundsException.class, part::getBounds);
            PostMicroClass.aBounds_$eq(null);
            assertThrows(NullPointerException.class, part::getBounds);
        } finally {
            PostMicroClass.aBounds_$eq(original);
        }
        assertSame(original[65], part.getBounds());
    }

    @Test
    void generatedPostsKeepPackedAxisAndMaterialInNbtAndDescriptions() {
        Microblock part = createPost();
        Microblock loaded = createPost();
        ByteArrayOutputStream prefixBytes = new ByteArrayOutputStream();
        MicroMaterialRegistry.writeMaterialID(new MCDataOutputWrapper(new DataOutputStream(prefixBytes)), 0);
        byte[] prefix = prefixBytes.toByteArray();
        for (int size : new int[] { 2, 4, 6 }) {
            for (int axis = 0; axis < 3; axis++) {
                byte shape = (byte) (size * 16 + axis);
                part.setShape(size, axis);
                NBTTagCompound tag = new NBTTagCompound();
                part.save(tag);
                assertEquals(shape, tag.getByte("shape"));
                assertEquals(MicroMaterialRegistry.materialName(0), tag.getString("material"));
                loaded.load(tag);
                assertEquals(shape, loaded.shape());
                assertEquals(0, loaded.material());
                assertSame(part.getBounds(), loaded.getBounds());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                part.writeDesc(new MCDataOutputWrapper(new DataOutputStream(bytes)));
                byte[] expected = Arrays.copyOf(prefix, prefix.length + 1);
                expected[prefix.length] = shape;
                assertArrayEquals(expected, bytes.toByteArray());
                AtomicInteger reads = new AtomicInteger();
                MCDataInput input = (MCDataInput) Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] { MCDataInput.class },
                        (proxy, method, args) -> {
                            assertEquals("readByte", method.getName());
                            reads.incrementAndGet();
                            return shape;
                        });
                loaded.setShape(4, 0);
                loaded.readDesc(input);
                assertEquals(1, reads.get());
                assertEquals(shape, loaded.shape());
                assertSame(part.getBounds(), loaded.getBounds());
            }
        }
    }

    @Test
    void generatedOcclusionAllowsCrossingPostsAndOnlyAlignedOrNonOverlappingCovers() {
        Microblock part = createPost();
        Microblock other = createPost();
        Microblock face = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
        for (int size : new int[] { 2, 4, 6 }) {
            for (int axis = 0; axis < 3; axis++) {
                part.setShape(size, axis);
                for (int otherSize : new int[] { 2, 4, 6 }) {
                    for (int otherAxis = 0; otherAxis < 3; otherAxis++) {
                        other.setShape(otherSize, otherAxis);
                        assertEquals(axis != otherAxis, part.occlusionTest(other));
                    }
                }
                for (int coverSize = 1; coverSize <= 7; coverSize++) {
                    for (int side = 0; side < 6; side++) {
                        face.setShape(coverSize, side);
                        boolean expected = side / 2 == axis || coverSize / 8D <= 0.5 - size / 16D;
                        assertEquals(
                                expected,
                                part.occlusionTest(face),
                                "size=" + size + ", axis=" + axis + ", cover=" + coverSize + ", side=" + side);
                    }
                }
            }
        }
    }

    private static Microblock createPost() {
        return MicroblockGenerator.create(PostMicroClass$.MODULE$, 0, false);
    }
}
