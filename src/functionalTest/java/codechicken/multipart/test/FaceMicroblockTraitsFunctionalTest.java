package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.microblock.FaceMicroClass;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.FaceMicroblock;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;
import scala.Tuple2;

class FaceMicroblockTraitsFunctionalTest {

    @Test
    void generatedFaceSelectsFactoryBoundsForEverySupportedShape() {
        Microblock part = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
        FaceMicroblock face = (FaceMicroblock) part;
        assertSame(FaceMicroClass$.MODULE$, face.microClass());
        assertSame(FaceMicroClass$.MODULE$, part.microClass());
        for (int size = 1; size <= 7; size++) {
            for (int side = 0; side < 6; side++) {
                part.setShape(size, side);
                Cuboid6 bounds = face.getBounds();
                assertSame(FaceMicroClass.aBounds()[size << 4 | side], bounds);
                double thickness = size / 8D;
                int axis = side / 2 == 0 ? 1 : side / 2 == 1 ? 2 : 0;
                double[] min = { bounds.min.x, bounds.min.y, bounds.min.z };
                double[] max = { bounds.max.x, bounds.max.y, bounds.max.z };
                for (int coordinate = 0; coordinate < 3; coordinate++) {
                    assertEquals(coordinate == axis && (side & 1) != 0 ? 1 - thickness : 0, min[coordinate], 1e-12);
                    assertEquals(coordinate == axis && (side & 1) == 0 ? thickness : 1, max[coordinate], 1e-12);
                }
            }
        }
    }

    @Test
    void boundsLookupKeepsSignedIndexingNullEntriesAndLiveArrayReplacement() {
        Microblock part = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
        FaceMicroblock face = (FaceMicroblock) part;
        part.shape_$eq((byte) 0);
        assertNull(face.getBounds());
        part.shape_$eq((byte) 127);
        assertNull(face.getBounds());
        for (int shape = -128; shape < 0; shape++) {
            part.shape_$eq((byte) shape);
            assertThrows(ArrayIndexOutOfBoundsException.class, face::getBounds);
        }
        Cuboid6[] original = FaceMicroClass.aBounds();
        Cuboid6[] replacement = new Cuboid6[256];
        Cuboid6 sentinel = new Cuboid6(0.2, 0.3, 0.4, 0.5, 0.6, 0.7);
        replacement[35] = sentinel;
        part.shape_$eq((byte) 35);
        try {
            FaceMicroClass.aBounds_$eq(replacement);
            assertSame(sentinel, face.getBounds());
            replacement[35] = null;
            assertNull(face.getBounds());
            FaceMicroClass.aBounds_$eq(new Cuboid6[1]);
            assertThrows(ArrayIndexOutOfBoundsException.class, face::getBounds);
        } finally {
            FaceMicroClass.aBounds_$eq(original);
        }
        assertSame(original[35], face.getBounds());
    }

    @Test
    void solidityDelegatesToCurrentMaterialAndIgnoresTheSide() {
        Microblock part = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
        FaceMicroblock face = (FaceMicroblock) part;
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> original = materials[0];
        AtomicBoolean solid = new AtomicBoolean();
        AtomicInteger calls = new AtomicInteger();
        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class },
                (proxy, method, args) -> {
                    assertEquals("isSolid", method.getName());
                    calls.incrementAndGet();
                    return solid.get();
                });
        try {
            materials[0] = new Tuple2<>(original._1(), material);
            for (boolean value : new boolean[] { false, true }) {
                solid.set(value);
                for (int side : new int[] { -1, 0, 1, 2, 3, 4, 5, 6 }) assertEquals(value, face.solid(side));
            }
            assertEquals(16, calls.get());
            materials[0] = new Tuple2<>(original._1(), null);
            assertThrows(NullPointerException.class, () -> face.solid(0));
        } finally {
            materials[0] = original;
        }
    }
}
