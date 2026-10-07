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
import codechicken.microblock.CornerMicroClass;
import codechicken.microblock.CornerMicroClass$;
import codechicken.microblock.CornerMicroblock;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;

class CornerMicroblockTraitsFunctionalTest {

    @Test
    void generatedCornerKeepsClassIdentityAndAllFiftySixBounds() {
        Microblock part = MicroblockGenerator.create(CornerMicroClass$.MODULE$, 0, false);
        CornerMicroblock cornerPart = (CornerMicroblock) part;
        assertSame(CornerMicroClass$.MODULE$, cornerPart.microClass());
        assertSame(CornerMicroClass$.MODULE$, part.microClass());
        assertEquals("mcr_cnr", part.getType());
        int[] axisBits = { 4, 1, 2 };
        for (int size = 1; size <= 7; size++) {
            for (int corner = 0; corner < 8; corner++) {
                part.setShape(size, corner + 7);
                assertEquals(size, part.getSize());
                assertEquals(corner + 7, cornerPart.getSlot());
                assertEquals(1 << (corner + 7), cornerPart.getSlotMask());
                Cuboid6 bounds = cornerPart.getBounds();
                assertSame(CornerMicroClass.aBounds()[size * 16 + corner], bounds);
                double distance = size / 8D;
                double[] min = { bounds.min.x, bounds.min.y, bounds.min.z };
                double[] max = { bounds.max.x, bounds.max.y, bounds.max.z };
                for (int axis = 0; axis < 3; axis++) {
                    boolean positive = (corner & axisBits[axis]) != 0;
                    assertEquals(positive ? 1 - distance : 0, min[axis], 1e-12);
                    assertEquals(positive ? 1 : distance, max[axis], 1e-12);
                }
            }
        }
    }

    @Test
    void boundsUseSignedShapeIndexAndTheLiveReplaceableArray() {
        Microblock part = MicroblockGenerator.create(CornerMicroClass$.MODULE$, 0, false);
        CornerMicroblock corner = (CornerMicroblock) part;
        for (int shape : new int[] { 0, 8, 127 }) {
            part.shape_$eq((byte) shape);
            assertNull(corner.getBounds());
        }
        for (int shape = -128; shape < 0; shape++) {
            part.shape_$eq((byte) shape);
            assertThrows(ArrayIndexOutOfBoundsException.class, corner::getBounds);
        }
        Cuboid6[] original = CornerMicroClass.aBounds();
        Cuboid6[] replacement = new Cuboid6[256];
        Cuboid6 sentinel = new Cuboid6(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
        replacement[35] = sentinel;
        part.setShape(2, 10);
        try {
            CornerMicroClass.aBounds_$eq(replacement);
            assertSame(sentinel, corner.getBounds());
            replacement[35] = null;
            assertNull(corner.getBounds());
            CornerMicroClass.aBounds_$eq(new Cuboid6[1]);
            assertThrows(ArrayIndexOutOfBoundsException.class, corner::getBounds);
        } finally {
            CornerMicroClass.aBounds_$eq(original);
        }
        assertSame(original[35], corner.getBounds());
    }

    @Test
    void generatedCornersKeepThePackedShapeInNbtAndDescriptions() {
        Microblock part = MicroblockGenerator.create(CornerMicroClass$.MODULE$, 0, false);
        Microblock loaded = MicroblockGenerator.create(CornerMicroClass$.MODULE$, 0, false);
        ByteArrayOutputStream prefixBytes = new ByteArrayOutputStream();
        MicroMaterialRegistry.writeMaterialID(new MCDataOutputWrapper(new DataOutputStream(prefixBytes)), 0);
        byte[] prefix = prefixBytes.toByteArray();
        for (int size : new int[] { 1, 4, 7 }) {
            for (int corner = 0; corner < 8; corner++) {
                byte shape = (byte) (size * 16 + corner);
                part.setShape(size, corner + 7);
                NBTTagCompound tag = new NBTTagCompound();
                part.save(tag);
                assertEquals(shape, tag.getByte("shape"));
                assertEquals(MicroMaterialRegistry.materialName(0), tag.getString("material"));
                loaded.load(tag);
                assertEquals(shape, loaded.shape());
                assertEquals(0, loaded.material());
                assertEquals(corner + 7, ((CornerMicroblock) loaded).getSlot());
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
                loaded.setShape(2, 7);
                loaded.readDesc(input);
                assertEquals(1, reads.get());
                assertEquals(shape, loaded.shape());
                assertEquals(corner + 7, ((CornerMicroblock) loaded).getSlot());
            }
        }
    }
}
