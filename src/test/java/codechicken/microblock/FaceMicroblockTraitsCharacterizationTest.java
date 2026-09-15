package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

/** Runs a frozen Scala face-client forwarder against the original or extracted helper. */
class FaceMicroblockTraitsCharacterizationTest {

    @Test
    void negativePassUsesPhysicalBoundsAndIgnoresTransparencyAndMasks() throws Exception {
        Fixture f = new Fixture();
        assertEquals(37, f.part.material());
        assertNull(f.client.renderBounds());
        assertEquals(0, f.client.renderMask());
        f.client.renderMask_$eq(63);
        for (int pass : new int[] { -1, -7 }) {
            f.clear();
            f.client.render(f.position, pass);
            assertDraw(f, 0, 6, f.bounds, pass, 0);
            assertEquals(6, f.faces.size());
            assertEquals(Arrays.asList("material", "bounds"), f.events);
        }
    }

    @Test
    void transparentPassRendersExactlyTheUnmaskedShrunkFaces() throws Exception {
        Fixture f = new Fixture();
        f.set("transparent", boolean.class, true);
        f.client.renderBounds_$eq(f.shrunk);
        for (int mask = 0; mask < 64; mask++) {
            f.clear();
            f.client.renderMask_$eq(mask);
            f.client.render(f.position, 2);
            int count = 6 - Integer.bitCount(mask);
            assertDraw(f, 0, count, f.shrunk, 2, mask);
            assertEquals(count, f.faces.size());
            assertEquals(Arrays.asList("transparent", "material"), f.events);
        }
    }

    @Test
    void opaquePassRendersShrunkSidesThenTheOuterFaceForEveryMaskAndSlot() throws Exception {
        Fixture f = new Fixture();
        f.client.renderBounds_$eq(f.shrunk);
        for (int slot = 0; slot < 6; slot++) {
            f.set("selectedSlot", int.class, slot);
            for (int mask = 0; mask < 64; mask++) {
                f.clear();
                f.client.renderMask_$eq(mask);
                f.client.render(f.position, 0);
                int innerMask = mask | 1 << slot;
                int count = 6 - Integer.bitCount(innerMask);
                assertDraw(f, 0, count, f.shrunk, 0, innerMask);
                assertDraw(f, count, 1, Cuboid6.full, 0, ~(1 << slot));
                assertEquals(count + 1, f.faces.size());
                assertEquals(Arrays.asList("transparent", "material", "slot:" + slot, "slot:" + slot), f.events);
            }
        }
    }

    @Test
    void opaquePassCachesMaterialButEvaluatesTheSlotAgainAfterTheFirstDraw() throws Exception {
        Fixture f = new Fixture();
        f.client.renderBounds_$eq(f.shrunk);
        f.set("selectedSlot", int.class, 2);
        f.set("slotAdvance", int.class, 1);
        IMicroMaterial material = f.material;
        f.afterFace = () -> {
            try {
                f.set("selectedMaterial", IMicroMaterial.class, null);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        };
        f.client.render(f.position, 1);
        assertDraw(f, 0, 5, f.shrunk, 1, 1 << 2);
        assertDraw(f, 5, 1, Cuboid6.full, 1, ~(1 << 3));
        assertEquals(6, f.faces.size());
        for (Face face : f.faces) assertSame(material, face.material);
        assertEquals(Arrays.asList("transparent", "material", "slot:2", "slot:3"), f.events);
    }

    @Test
    void missingMaterialFailsOnlyWhenAnUnmaskedFaceIsEmitted() throws Exception {
        Fixture f = new Fixture();
        f.set("selectedMaterial", IMicroMaterial.class, null);
        f.set("transparent", boolean.class, true);
        f.client.renderBounds_$eq(f.shrunk);
        f.client.renderMask_$eq(63);
        assertDoesNotThrow(() -> f.client.render(f.position, 0));
        assertTrue(f.faces.isEmpty());
        f.client.renderMask_$eq(0);
        assertThrows(NullPointerException.class, () -> f.client.render(f.position, 0));
        f.set("transparent", boolean.class, false);
        f.client.renderMask_$eq(63);
        f.clear();
        assertThrows(NullPointerException.class, () -> f.client.render(f.position, 0));
        assertEquals(Arrays.asList("transparent", "material", "slot:0", "slot:0"), f.events);
    }

    private static void assertDraw(Fixture f, int offset, int count, Cuboid6 bounds, int pass, int mask) {
        int index = offset;
        for (int side = 0; side < 6; side++) {
            if ((mask & 1 << side) != 0) continue;
            Face face = f.faces.get(index++);
            assertEquals(side, face.side);
            assertSame(bounds, face.bounds);
            assertSame(f.position, face.position);
            assertEquals(pass, face.pass);
        }
        assertEquals(offset + count, index);
    }

    private static final class Face {

        final Object material;
        final Vector3 position;
        final Cuboid6 bounds;
        final int pass;
        final int side;

        Face(Object material, Object[] args) {
            this.material = material;
            position = (Vector3) args[0];
            pass = (Integer) args[1];
            bounds = (Cuboid6) args[2];
            side = MicroblockRender.face().get().side;
        }
    }

    private static final class Fixture extends ClassLoader {

        final Microblock part;
        final FaceMicroblockClient client;
        final List<String> events;
        final List<Face> faces = new ArrayList<>();
        final Vector3 position = new Vector3(2, 3, 4);
        final Cuboid6 shrunk = new Cuboid6(0.1, 0.05, 0.2, 0.8, 0.2, 0.9);
        final Cuboid6 bounds;
        final IMicroMaterial material;
        Runnable afterFace;

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            InputStream input = Objects.requireNonNull(
                    getClass().getResourceAsStream("/compat/ReferenceScalaFaceMicroblockClient.class.b64"));
            byte[] bytes;
            try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                bytes = Base64.getMimeDecoder().decode(scanner.next());
            }
            part = (Microblock) defineClass(null, bytes, 0, bytes.length).getConstructor().newInstance();
            client = (FaceMicroblockClient) part;
            events = (List<String>) part.getClass().getMethod("events").invoke(part);
            bounds = (Cuboid6) part.getClass().getMethod("bounds").invoke(part);
            material = (IMicroMaterial) Proxy
                    .newProxyInstance(getParent(), new Class<?>[] { IMicroMaterial.class }, (proxy, method, args) -> {
                        assertEquals("renderMicroFace", method.getName());
                        faces.add(new Face(proxy, args));
                        if (afterFace != null) afterFace.run();
                        return null;
                    });
            set("selectedMaterial", IMicroMaterial.class, material);
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }

        void clear() {
            events.clear();
            faces.clear();
        }
    }
}
