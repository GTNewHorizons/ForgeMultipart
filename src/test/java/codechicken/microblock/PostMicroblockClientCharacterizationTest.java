package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;

class PostMicroblockClientCharacterizationTest {

    @Test
    void onlyMinusOneRendersPhysicalBoundsAndAllOtherPassesUseBothSegments() throws Exception {
        Fixture f = new Fixture();
        assertEquals(41, f.part.material());
        assertNull(f.client.renderBounds1());
        assertNull(f.client.renderBounds2());
        Cuboid6 first = new Cuboid6(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
        Cuboid6 second = new Cuboid6(0.4, 0.5, 0.6, 0.7, 0.8, 0.9);
        f.client.renderBounds1_$eq(first);
        f.client.renderBounds2_$eq(second);
        for (int pass : new int[] { -1, -2, 0, 1 }) {
            f.clear();
            f.client.render(f.position, pass);
            if (pass == -1) {
                f.assertDraw(0, f.bounds, pass);
                assertEquals(6, f.faces.size());
                assertEquals(Arrays.asList("material", "bounds:self"), f.events);
            } else {
                f.assertDraw(0, first, pass);
                f.assertDraw(6, second, pass);
                assertEquals(12, f.faces.size());
                assertEquals(Collections.singletonList("material"), f.events);
            }
        }
        f.client.renderBounds2_$eq(null);
        f.clear();
        f.client.render(f.position, 0);
        assertEquals(6, f.faces.size());
    }

    @Test
    void renderCachesMaterialButReadsTheSecondSegmentAfterTheFirstDraw() throws Exception {
        Fixture f = new Fixture();
        Cuboid6 second = new Cuboid6(0.2, 0.6, 0.2, 0.8, 1, 0.8);
        f.client.renderBounds1_$eq(f.bounds);
        f.afterFace = () -> {
            f.setUnchecked(f.part, "selectedMaterial", IMicroMaterial.class, null);
            f.client.renderBounds2_$eq(second);
        };
        f.client.render(f.position, 0);
        f.assertDraw(0, f.bounds, 0);
        f.assertDraw(6, second, 0);
        assertEquals(12, f.faces.size());
        assertEquals(Collections.singletonList("material"), f.events);
        f.afterFace = null;
        f.clear();
        assertThrows(NullPointerException.class, () -> f.client.render(f.position, -1));
        assertEquals(Arrays.asList("material", "bounds:self"), f.events);
    }

    @Test
    void shrinkPriorityUsesSizeThenTransparencyThenAxisWithRepeatedVirtualReads() throws Exception {
        Fixture f = new Fixture();
        Microblock other = f.newPart("other");
        for (int ownSize : new int[] { -1, 2, 4, 6 }) {
            for (int otherSize : new int[] { -1, 2, 4, 6 }) {
                for (boolean ownTransparent : new boolean[] { false, true }) {
                    for (boolean otherTransparent : new boolean[] { false, true }) {
                        for (int axis = 0; axis < 3; axis++) {
                            f.set(f.part, "selectedSize", int.class, ownSize);
                            f.set(other, "selectedSize", int.class, otherSize);
                            f.set(f.part, "transparent", boolean.class, ownTransparent);
                            f.set(other, "transparent", boolean.class, otherTransparent);
                            f.set(f.part, "selectedShape", int.class, axis);
                            f.set(other, "selectedShape", int.class, 1);
                            f.clear();
                            boolean expected = ownSize != otherSize ? ownSize < otherSize
                                    : ownTransparent != otherTransparent ? ownTransparent : axis > 1;
                            assertEquals(expected, f.client.thisShrinks((PostMicroblock) other));
                            List<String> calls = new ArrayList<>(Arrays.asList("size:self", "size:other"));
                            if (ownSize != otherSize) calls.addAll(Arrays.asList("size:self", "size:other"));
                            else {
                                calls.addAll(Arrays.asList("transparent:self", "transparent:other"));
                                calls.addAll(
                                        ownTransparent != otherTransparent
                                                ? Collections.singletonList("transparent:self")
                                                : Arrays.asList("shape:self", "shape:other"));
                            }
                            assertEquals(calls, f.events);
                        }
                    }
                }
            }
        }
        f.set(f.part, "selectedSize", int.class, 2);
        f.set(f.part, "sizeAdvance", int.class, 5);
        f.set(other, "selectedSize", int.class, 3);
        assertFalse(f.client.thisShrinks((PostMicroblock) other));
        f.set(f.part, "sizeAdvance", int.class, 0);
        f.set(f.part, "selectedSize", int.class, 3);
        f.set(f.part, "transparent", boolean.class, false);
        f.set(f.part, "flipTransparency", boolean.class, true);
        f.set(other, "transparent", boolean.class, true);
        assertTrue(f.client.thisShrinks((PostMicroblock) other));
    }

    @Test
    void lifecycleKeepsSuperOrderingPacketIdentityAndShortCircuitOnFailure() throws Exception {
        Fixture f = new Fixture();
        f.set(f.part, "runRecalc", boolean.class, false);
        f.part.onAdded();
        assertEquals(Arrays.asList("superAdded", "recalc"), f.events);
        f.clear();
        MCDataInput packet = (MCDataInput) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { MCDataInput.class },
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        f.part.read(packet);
        assertSame(packet, f.part.getClass().getMethod("seenPacket").invoke(f.part));
        assertEquals(Arrays.asList("superRead", "recalc"), f.events);
        f.clear();
        f.part.onPartChanged(null);
        assertEquals(Collections.singletonList("recalc"), f.events);
        f.set(f.part, "failSuper", boolean.class, true);
        f.clear();
        assertThrows(IllegalStateException.class, f.part::onAdded);
        assertEquals(Collections.singletonList("superAdded"), f.events);
        f.clear();
        assertThrows(IllegalStateException.class, () -> f.part.read(packet));
        assertEquals(Collections.singletonList("superRead"), f.events);
    }

    @Test
    void equalityUsesTheOtherReceiverEvenForTheSameObjectAndCanSuppressShrinking() throws Exception {
        Fixture f = new Fixture();
        Microblock other = f.newPart("other");
        f.set(other, "forceEquality", boolean.class, true);
        f.set(other, "equalityResult", boolean.class, true);
        f.client.shrinkPost((PostMicroblock) other);
        assertEquals(Arrays.asList("post", "equals:other"), f.events);
        f.clear();
        f.client.shrinkPost(f.client);
        assertEquals(Arrays.asList("post", "equals:self"), f.events);
        f.set(f.part, "forceEquality", boolean.class, true);
        f.set(f.part, "equalityResult", boolean.class, false);
        f.clear();
        f.client.shrinkPost(f.client);
        assertEquals(
                Arrays.asList(
                        "post",
                        "equals:self",
                        "size:self",
                        "size:self",
                        "transparent:self",
                        "transparent:self",
                        "shape:self",
                        "shape:self"),
                f.events);
    }

    @Test
    void splitBoundsCopyOnceAndReadOtherBoundsAndOwnAxisTwice() throws Exception {
        Fixture f = new Fixture();
        Microblock other = f.newPart("other");
        Cuboid6 crossing = new Cuboid6(0, 0.25, 0, 1, 0.75, 1);
        f.set(other, "bounds", Cuboid6.class, crossing);
        f.set(other, "selectedSize", int.class, 4);
        f.client.renderBounds1_$eq(f.bounds.copy());
        f.client.shrinkPost((PostMicroblock) other);
        assertEquals(0.25, f.client.renderBounds1().max.y);
        assertEquals(0.75, f.client.renderBounds2().min.y);
        assertNotSame(f.bounds, f.client.renderBounds2());
        assertEquals(0, f.bounds.min.y);
        assertEquals(1, f.bounds.max.y);
        assertEquals(2, Collections.frequency(f.events, "bounds:other"));
        assertEquals(2, Collections.frequency(f.events, "shape:self"));
        Cuboid6 second = f.client.renderBounds2();
        f.clear();
        f.client.shrinkPost((PostMicroblock) other);
        assertSame(second, f.client.renderBounds2());
        assertFalse(f.events.contains("bounds:self"));
        f.client.renderBounds1_$eq(Cuboid6.full.copy());
        f.client.renderBounds2_$eq(Cuboid6.full.copy());
        f.set(f.part, "shapeAdvance", int.class, 1);
        f.client.shrinkPost((PostMicroblock) other);
        assertEquals(0.25, f.client.renderBounds1().max.y);
        assertEquals(1, f.client.renderBounds2().min.z);
        assertEquals(0, f.client.renderBounds2().min.y);
    }

    @Test
    void recalculationResetsThenDispatchesBothFacesAndTheCollectionsForeachInOrder() throws Exception {
        Fixture f = new Fixture();
        Microblock equal = f.newPart("equal");
        Microblock other = f.newPart("other");
        f.set(equal, "forceEquality", boolean.class, true);
        f.set(equal, "equalityResult", boolean.class, true);
        f.set(f.part, "runShrinks", boolean.class, false);
        ArrayBuffer<TMultiPart> parts = new ArrayBuffer<>();
        for (TMultiPart p : Arrays.asList(null, new BarePart("bare"), f.part, equal, other)) parts.$plus$eq(p);
        @SuppressWarnings("unchecked")
        Seq<TMultiPart> sequence = (Seq<TMultiPart>) Proxy
                .newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Seq.class }, (proxy, method, args) -> {
                    f.events.add(method.getName());
                    return method.invoke(parts, args);
                });
        TileMultipart tile = new TileMultipart();
        tile.partList_$eq(sequence);
        f.part.bind(tile);
        f.client.renderBounds2_$eq(Cuboid6.full);
        f.clear();
        f.client.recalcBounds();
        assertEquals(
                Arrays.asList(
                        "recalc",
                        "bounds:self",
                        "shape:self",
                        "face:0",
                        "shape:self",
                        "face:1",
                        "tile:self",
                        "foreach",
                        "equals:self",
                        "equals:equal",
                        "equals:other",
                        "post"),
                f.events);
        assertNotSame(f.bounds, f.client.renderBounds1());
        assertBounds(f.bounds, f.client.renderBounds1());
        assertNull(f.client.renderBounds2());
    }

    @Test
    void faceFiltersAndFailuresPreserveStateAndCallOrdering() throws Exception {
        Fixture f = new Fixture();
        SlotTile tile = new SlotTile();
        f.part.bind(tile);
        Cuboid6 render = f.bounds.copy();
        f.client.renderBounds1_$eq(render);
        f.client.shrinkFace(0);
        tile.part = new BarePart("other");
        f.client.shrinkFace(0);
        assertSame(render, f.client.renderBounds1());
        assertBounds(f.bounds, render);
        tile.part = new BarePart(null);
        assertThrows(NullPointerException.class, () -> f.client.shrinkFace(0));
        tile.part = new BarePart("mcr_face");
        assertThrows(ClassCastException.class, () -> f.client.shrinkFace(0));
        f.client.renderBounds2_$eq(render);
        f.set(f.part, "bounds", Cuboid6.class, null);
        assertThrows(NullPointerException.class, f.client::recalcBounds);
        assertSame(render, f.client.renderBounds2());
    }

    private static void assertBounds(Cuboid6 expected, Cuboid6 actual) {
        assertArrayEquals(
                new double[] { expected.min.x, expected.min.y, expected.min.z, expected.max.x, expected.max.y,
                        expected.max.z },
                new double[] { actual.min.x, actual.min.y, actual.min.z, actual.max.x, actual.max.y, actual.max.z },
                1e-12);
    }

    private static final class BarePart extends TMultiPart {

        final String type;

        BarePart(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }
    }

    private static final class SlotTile extends TileMultipart {

        TMultiPart part;

        @Override
        public TMultiPart partMap(int slot) {
            return part;
        }
    }

    private static final class Face {

        final Cuboid6 bounds;
        final Vector3 position;
        final int pass;
        final int side;

        Face(Object[] args) {
            position = (Vector3) args[0];
            pass = (Integer) args[1];
            bounds = (Cuboid6) args[2];
            side = MicroblockRender.face().get().side;
        }
    }

    private static final class Fixture extends ClassLoader {

        final List<String> events = new ArrayList<>();
        final List<Face> faces = new ArrayList<>();
        final Microblock part;
        final PostMicroblockClient client;
        final Cuboid6 bounds;
        final Vector3 position = new Vector3(2, 3, 4);
        Runnable afterFace;

        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            for (String name : new String[] { "ReferencePostClientBase", "ReferenceScalaPostMicroblockClient" }) {
                InputStream input = Objects
                        .requireNonNull(getClass().getResourceAsStream("/compat/" + name + ".class.b64"));
                byte[] bytes;
                try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
            }
            part = newPart("self");
            client = (PostMicroblockClient) part;
            bounds = (Cuboid6) part.getClass().getMethod("bounds").invoke(part);
            IMicroMaterial material = (IMicroMaterial) Proxy
                    .newProxyInstance(getParent(), new Class<?>[] { IMicroMaterial.class }, (proxy, method, args) -> {
                        assertEquals("renderMicroFace", method.getName());
                        faces.add(new Face(args));
                        if (afterFace != null) afterFace.run();
                        return null;
                    });
            set(part, "selectedMaterial", IMicroMaterial.class, material);
        }

        Microblock newPart(String label) throws Exception {
            Microblock result = (Microblock) loadClass(
                    "codechicken.multipart.compat.ReferenceScalaPostMicroblockClient").getConstructor().newInstance();
            set(result, "events", List.class, events);
            set(result, "label", String.class, label);
            return result;
        }

        void set(Object target, String name, Class<?> type, Object value) throws Exception {
            target.getClass().getMethod(name + "_$eq", type).invoke(target, value);
        }

        void setUnchecked(Object target, String name, Class<?> type, Object value) {
            try {
                set(target, name, type, value);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        void clear() {
            events.clear();
            faces.clear();
        }

        void assertDraw(int offset, Cuboid6 bounds, int pass) {
            for (int side = 0; side < 6; side++) {
                Face face = faces.get(offset + side);
                assertSame(bounds, face.bounds);
                assertSame(position, face.position);
                assertEquals(pass, face.pass);
                assertEquals(side, face.side);
            }
        }
    }
}
