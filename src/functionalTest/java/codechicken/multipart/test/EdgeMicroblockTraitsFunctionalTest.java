package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.vec.Cuboid6;
import codechicken.microblock.EdgeMicroClass;
import codechicken.microblock.EdgeMicroClass$;
import codechicken.microblock.EdgeMicroblock;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;

class EdgeMicroblockTraitsFunctionalTest {

    @Test
    void generatedEdgeKeepsClassIdentityAndAllEightyFourBounds() {
        Microblock part = MicroblockGenerator.create(EdgeMicroClass$.MODULE$, 0, false);
        EdgeMicroblock edgePart = (EdgeMicroblock) part;
        assertSame(EdgeMicroClass$.MODULE$, edgePart.microClass());
        assertSame(EdgeMicroClass$.MODULE$, part.microClass());
        assertEquals("mcr_edge", part.getType());
        int[][] axisBits = { { 2, 0, 1 }, { 1, 2, 0 }, { 0, 1, 2 } };
        for (int size = 1; size <= 7; size++) {
            for (int edge = 0; edge < 12; edge++) {
                part.setShape(size, edge + 15);
                assertEquals(size, part.getSize());
                assertEquals(edge + 15, edgePart.getSlot());
                assertEquals(1 << (edge + 15), edgePart.getSlotMask());
                assertFalse(edgePart.conductsRedstone());
                Cuboid6 bounds = edgePart.getBounds();
                assertSame(EdgeMicroClass.aBounds()[size * 16 + edge], bounds);
                double distance = size / 8D;
                double[] min = { bounds.min.x, bounds.min.y, bounds.min.z };
                double[] max = { bounds.max.x, bounds.max.y, bounds.max.z };
                for (int axis = 0; axis < 3; axis++) {
                    int bit = axisBits[edge / 4][axis];
                    boolean positive = (edge & bit) != 0;
                    assertEquals(positive ? 1 - distance : 0, min[axis], 1e-12);
                    assertEquals(bit == 0 || positive ? 1 : distance, max[axis], 1e-12);
                }
            }
        }
    }

    @Test
    void boundsUseSignedShapeIndexAndTheLiveReplaceableArray() {
        Microblock part = MicroblockGenerator.create(EdgeMicroClass$.MODULE$, 0, false);
        EdgeMicroblock edge = (EdgeMicroblock) part;
        for (int shape : new int[] { 0, 12, 28, 127 }) {
            part.shape_$eq((byte) shape);
            assertNull(edge.getBounds());
        }
        for (int shape = -128; shape < 0; shape++) {
            part.shape_$eq((byte) shape);
            assertThrows(ArrayIndexOutOfBoundsException.class, edge::getBounds);
        }
        Cuboid6[] original = EdgeMicroClass.aBounds();
        Cuboid6[] replacement = new Cuboid6[256];
        Cuboid6 sentinel = new Cuboid6(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
        replacement[35] = sentinel;
        part.setShape(2, 18);
        try {
            EdgeMicroClass.aBounds_$eq(replacement);
            assertSame(sentinel, edge.getBounds());
            replacement[35] = null;
            assertNull(edge.getBounds());
            EdgeMicroClass.aBounds_$eq(new Cuboid6[1]);
            assertThrows(ArrayIndexOutOfBoundsException.class, edge::getBounds);
            EdgeMicroClass.aBounds_$eq(null);
            assertThrows(NullPointerException.class, edge::getBounds);
        } finally {
            EdgeMicroClass.aBounds_$eq(original);
        }
        assertSame(original[35], edge.getBounds());
    }

    @Test
    void generatedEdgesKeepThePackedShapeInNbtAndDescriptions() {
        Microblock part = MicroblockGenerator.create(EdgeMicroClass$.MODULE$, 0, false);
        Microblock loaded = MicroblockGenerator.create(EdgeMicroClass$.MODULE$, 0, false);
        ByteArrayOutputStream prefixBytes = new ByteArrayOutputStream();
        MicroMaterialRegistry.writeMaterialID(new MCDataOutputWrapper(new DataOutputStream(prefixBytes)), 0);
        byte[] prefix = prefixBytes.toByteArray();
        for (int size = 1; size <= 7; size++) {
            for (int edge = 0; edge < 12; edge++) {
                byte shape = (byte) (size * 16 + edge);
                part.setShape(size, edge + 15);
                NBTTagCompound tag = new NBTTagCompound();
                part.save(tag);
                assertEquals(shape, tag.getByte("shape"));
                assertEquals(MicroMaterialRegistry.materialName(0), tag.getString("material"));
                loaded.load(tag);
                assertEquals(shape, loaded.shape());
                assertEquals(0, loaded.material());
                assertEquals(edge + 15, ((EdgeMicroblock) loaded).getSlot());
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
                loaded.setShape(2, 15);
                loaded.readDesc(input);
                assertEquals(1, reads.get());
                assertEquals(shape, loaded.shape());
                assertEquals(edge + 15, ((EdgeMicroblock) loaded).getSlot());
            }
        }
    }
}
