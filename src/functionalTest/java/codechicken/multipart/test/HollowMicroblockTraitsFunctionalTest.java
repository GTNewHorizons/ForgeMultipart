package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.microblock.FaceMicroClass;
import codechicken.microblock.HollowMicroClass$;
import codechicken.microblock.HollowMicroblock;
import codechicken.microblock.ISidedHollowConnect;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.MatchError;
import scala.collection.JavaConversions;
import scala.collection.Seq;

class HollowMicroblockTraitsFunctionalTest {

    @Test
    void generatedHollowsKeepAllThicknessesOrientationsAndLiveConnectorOpenings() {
        Microblock part = create();
        HollowMicroblock hollow = (HollowMicroblock) part;
        Connector connector = new Connector();
        part.bind(new TileMultipart() {

            @Override
            public TMultiPart partMap(int slot) {
                assertEquals(6, slot);
                return connector;
            }
        });
        assertSame(HollowMicroClass$.MODULE$, part.microClass());
        assertEquals("mcr_hllw", part.getType());
        for (int thickness = 1; thickness <= 7; thickness++) {
            for (int side = 0; side < 6; side++) {
                part.setShape(thickness, side);
                for (int opening : new int[] { 0, 1, 8, 11 }) {
                    connector.size = opening;
                    assertEquals(opening, hollow.getHollowSize());
                    assertEquals(side, connector.lastSide);
                    double t = thickness / 8D;
                    double gap = opening / 16D;
                    assertEquals(t * (1 - gap * gap), volume(hollow.getCollisionBoxes()), 1e-12);
                    assertEquals(t * (0.75 * 0.75 - gap * gap), volume(hollow.getOcclusionBoxes()), 1e-12);
                    assertEquals(t * (1 - 0.75 * 0.75), volume(hollow.getPartialOcclusionBoxes()), 1e-12);
                    int axis = new int[] { 1, 2, 0 }[side / 2];
                    List<Cuboid6> collision = hollow.getCollisionBoxes();
                    List<IndexedCuboid6> subparts = hollow.getSubParts();
                    assertEquals(4, collision.size());
                    assertEquals(4, subparts.size());
                    for (int i = 0; i < 4; i++) {
                        Cuboid6 box = collision.get(i);
                        double[] min = { box.min.x, box.min.y, box.min.z };
                        double[] max = { box.max.x, box.max.y, box.max.z };
                        assertEquals((side & 1) == 0 ? 0 : 1 - t, min[axis], 1e-12);
                        assertEquals((side & 1) == 0 ? t : 1, max[axis], 1e-12);
                        assertBox(box, subparts.get(i));
                        assertEquals(0, subparts.get(i).data);
                        if (opening > 0) {
                            boolean missesCenter = false;
                            for (int j = 0; j < 3; j++) if (j != axis) missesCenter |= max[j] < 0.5 || min[j] > 0.5;
                            assertTrue(missesCenter, "central connector must fit through the hollow cover");
                        }
                    }
                    assertSame(FaceMicroClass.aBounds()[thickness * 16 + side], part.getBounds());
                }
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void partialBoxesRemainLiveJavaViewsAndBoundsKeepSignedIndexFailures() {
        Microblock part = create();
        HollowMicroblock hollow = (HollowMicroblock) part;
        HollowMicroClass$ factory = HollowMicroClass$.MODULE$;
        Cuboid6[] bounds = FaceMicroClass.aBounds();
        Seq<Cuboid6>[] partial = factory.pBoxes();
        Cuboid6 sentinel = new Cuboid6(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
        part.setShape(3, 0);
        try {
            Cuboid6[] replacementBounds = new Cuboid6[256];
            replacementBounds[48] = sentinel;
            FaceMicroClass.aBounds_$eq(replacementBounds);
            assertSame(sentinel, part.getBounds());
            Seq<Cuboid6>[] replacement = (Seq<Cuboid6>[]) new Seq<?>[256];
            List<Cuboid6> backing = new ArrayList<>(Arrays.asList(sentinel));
            replacement[48] = JavaConversions.asScalaBuffer(backing);
            factory.pBoxes_$eq(replacement);
            List<Cuboid6> view = hollow.getPartialOcclusionBoxes();
            assertSame(sentinel, view.get(0));
            backing.add(Cuboid6.full);
            assertEquals(2, view.size());
            view.remove(0);
            assertSame(Cuboid6.full, backing.get(0));
            replacement[48] = null;
            List<Cuboid6> nullView = hollow.getPartialOcclusionBoxes();
            assertNotNull(nullView);
            assertThrows(NullPointerException.class, nullView::size);
            part.shape_$eq((byte) 0);
            assertNull(part.getBounds());
            for (int shape = -128; shape < 0; shape++) {
                part.shape_$eq((byte) shape);
                assertThrows(ArrayIndexOutOfBoundsException.class, part::getBounds);
                assertThrows(ArrayIndexOutOfBoundsException.class, hollow::getPartialOcclusionBoxes);
            }
            part.setShape(3, 0);
            factory.pBoxes_$eq((Seq<Cuboid6>[]) new Seq<?>[1]);
            assertThrows(ArrayIndexOutOfBoundsException.class, hollow::getPartialOcclusionBoxes);
            factory.pBoxes_$eq(null);
            FaceMicroClass.aBounds_$eq(null);
            assertThrows(NullPointerException.class, hollow::getPartialOcclusionBoxes);
            assertThrows(NullPointerException.class, part::getBounds);
        } finally {
            factory.pBoxes_$eq(partial);
            FaceMicroClass.aBounds_$eq(bounds);
        }
    }

    @Test
    void normalBoxesUseLiveBoundsAndKeepNullIndexAndInvalidSlotFailureOrder() {
        Microblock part = create();
        HollowMicroblock hollow = (HollowMicroblock) part;
        HollowMicroClass$ factory = HollowMicroClass$.MODULE$;
        Cuboid6[] original = factory.occBounds();
        Cuboid6[] replacement = new Cuboid6[256];
        replacement[48] = new Cuboid6(0.1, 0.2, 0.3, 0.9, 0.8, 0.7);
        try {
            factory.occBounds_$eq(replacement);
            part.setShape(3, 0);
            List<Cuboid6> boxes = (List<Cuboid6>) hollow.getOcclusionBoxes();
            assertEquals(4, boxes.size());
            assertBox(new Cuboid6(0.75, 0.2, 0.25, 0.9, 0.8, 0.75), boxes.get(0));
            assertThrows(UnsupportedOperationException.class, () -> boxes.set(0, Cuboid6.full));
            replacement[48].max.x = 0.8;
            assertEquals(0.8, hollow.getOcclusionBoxes().iterator().next().max.x);
            assertEquals(0.9, boxes.get(0).max.x);
            for (int slot = 6; slot < 16; slot++) {
                part.setShape(3, slot);
                assertThrows(NullPointerException.class, hollow::getOcclusionBoxes);
                replacement[48 + slot] = Cuboid6.full;
                assertEquals(
                        Integer.toString(slot) + " (of class java.lang.Integer)",
                        assertThrows(MatchError.class, hollow::getOcclusionBoxes).getMessage());
            }
            part.shape_$eq((byte) -1);
            assertThrows(ArrayIndexOutOfBoundsException.class, hollow::getOcclusionBoxes);
            factory.occBounds_$eq(null);
            assertThrows(NullPointerException.class, hollow::getOcclusionBoxes);
        } finally {
            factory.occBounds_$eq(original);
        }
    }

    @Test
    void allFortyTwoShapesKeepMaterialAndPackedNbtAndDescriptionBytes() {
        Microblock part = create();
        Microblock loaded = create();
        ByteArrayOutputStream prefixBytes = new ByteArrayOutputStream();
        MicroMaterialRegistry.writeMaterialID(new MCDataOutputWrapper(new DataOutputStream(prefixBytes)), 0);
        byte[] prefix = prefixBytes.toByteArray();
        for (int thickness = 1; thickness <= 7; thickness++) {
            for (int side = 0; side < 6; side++) {
                byte shape = (byte) (thickness * 16 + side);
                part.setShape(thickness, side);
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
                loaded.setShape(1, 0);
                loaded.readDesc(input);
                assertEquals(1, reads.get());
                assertEquals(shape, loaded.shape());
                assertSame(part.getBounds(), loaded.getBounds());
            }
        }
    }

    private static Microblock create() {
        return MicroblockGenerator.create(HollowMicroClass$.MODULE$, 0, false);
    }

    private static double volume(Iterable<Cuboid6> boxes) {
        double result = 0;
        for (Cuboid6 box : boxes) result += (box.max.x - box.min.x) * (box.max.y - box.min.y) * (box.max.z - box.min.z);
        return result;
    }

    private static void assertBox(Cuboid6 expected, Cuboid6 actual) {
        assertArrayEquals(
                new double[] { expected.min.x, expected.min.y, expected.min.z, expected.max.x, expected.max.y,
                        expected.max.z },
                new double[] { actual.min.x, actual.min.y, actual.min.z, actual.max.x, actual.max.y, actual.max.z },
                1e-12);
    }

    private static final class Connector extends TMultiPart implements ISidedHollowConnect {

        int size;
        int lastSide;

        @Override
        public String getType() {
            return "test:hollow_connector";
        }

        @Override
        public int getHollowSize(int side) {
            lastSide = side;
            return size;
        }
    }
}
