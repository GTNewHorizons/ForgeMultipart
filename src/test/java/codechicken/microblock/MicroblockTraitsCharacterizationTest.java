package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.multipart.TileMultipart;

/** Exercises frozen Scala forwarders compiled before the implementation extraction. */
class MicroblockTraitsCharacterizationTest {

    @Test
    void frozenConstructorAndSlotDispatchKeepStateAndJvmShiftSemantics() throws Exception {
        Fixture fixture = new Fixture();
        assertEquals(23, fixture.part.material());
        assertEquals(0, fixture.part.shape());
        assertNull(fixture.client.renderBounds());
        assertEquals(0, fixture.client.renderMask());
        for (int shape = -128; shape < 128; shape++) {
            fixture.part.shape_$eq((byte) shape);
            assertEquals(shape & 15, fixture.client.getSlot());
            assertEquals(1 << (shape & 15), fixture.client.getSlotMask());
        }
        fixture.set("overrideSlot", boolean.class, true);
        for (int slot : new int[] { -1, 0, 31, 32, 35 }) {
            fixture.set("selectedSlot", int.class, slot);
            assertEquals(1 << slot, fixture.client.getSlotMask());
        }
    }

    @Test
    void partialBoxesAreFreshImmutableListsContainingTheOriginalBounds() throws Exception {
        Fixture fixture = new Fixture();
        Cuboid6 bounds = fixture.bounds();
        List<Cuboid6> first = fixture.client.getPartialOcclusionBoxes();
        List<Cuboid6> second = fixture.client.getPartialOcclusionBoxes();
        assertNotSame(first, second);
        assertEquals(1, first.size());
        assertSame(bounds, first.get(0));
        assertSame(bounds, fixture.client.getRenderBounds());
        bounds.min.x = 0.125;
        assertEquals(0.125, first.get(0).min.x);
        assertThrows(UnsupportedOperationException.class, () -> first.set(0, Cuboid6.full));
        assertThrows(UnsupportedOperationException.class, () -> first.add(Cuboid6.full));
        assertThrows(UnsupportedOperationException.class, () -> first.remove(0));
        fixture.set("bounds", Cuboid6.class, null);
        assertNull(fixture.client.getPartialOcclusionBoxes().get(0));
        assertNull(fixture.client.getRenderBounds());
        assertSame(bounds, first.get(0));
    }

    @Test
    void itemClassIdUsesTheCurrentVirtualClass() throws Exception {
        Fixture fixture = new Fixture();
        assertThrows(NullPointerException.class, fixture.part::itemClassID);
        fixture.set("selectedClass", CommonMicroClass.class, TestClass.create(19));
        assertEquals(19, fixture.part.itemClassID());
        fixture.set("selectedClass", CommonMicroClass.class, TestClass.create(-3));
        assertEquals(-3, fixture.part.itemClassID());
    }

    @Test
    void brokenIconsReadTheMaterialOnceAndForwardTheRequestedSide() throws Exception {
        Fixture fixture = new Fixture();
        IIcon icon = (IIcon) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IIcon.class },
                (proxy, method, args) -> null);
        fixture.material((method, args) -> {
            assertEquals("getBreakingIcon", method);
            fixture.events.add("icon:" + args[0]);
            return icon;
        });
        for (int side : new int[] { -1, 0, 5, 9 }) {
            fixture.events.clear();
            assertSame(icon, fixture.client.getBrokenIcon(side));
            assertEquals(Arrays.asList("material", "icon:" + side), fixture.events);
        }
    }

    @Test
    void staticRenderingChecksThePassBeforeVirtualRender() throws Exception {
        Fixture fixture = new Fixture();
        fixture.material((method, args) -> {
            assertEquals("canRenderInPass", method);
            fixture.events.add("pass:" + args[0]);
            return ((Integer) args[0]) == 2;
        });
        Vector3 position = new Vector3(4, 5, 6);
        assertTrue(fixture.part.renderStatic(position, 2));
        assertSame(position, fixture.get("renderedPosition"));
        assertEquals(Arrays.asList("material", "pass:2", "render:2"), fixture.events);
        fixture.events.clear();
        assertFalse(fixture.part.renderStatic(position, 3));
        assertEquals(Arrays.asList("material", "pass:3"), fixture.events);
        fixture.set("selectedMaterial", IMicroMaterial.class, null);
        fixture.events.clear();
        assertThrows(NullPointerException.class, () -> fixture.part.renderStatic(position, 2));
        assertEquals(Arrays.asList("material"), fixture.events);
    }

    @Test
    void commonRenderingSelectsPhysicalOrShrunkBoundsAndFaceMask() throws Exception {
        Fixture fixture = new Fixture();
        fixture.set("commonRender", boolean.class, true);
        List<Object[]> faces = new ArrayList<>();
        fixture.material((method, args) -> {
            assertEquals("renderMicroFace", method);
            faces.add(args.clone());
            return null;
        });
        Vector3 position = new Vector3(1, 2, 3);
        Cuboid6 shrunk = new Cuboid6(0.1, 0.2, 0.3, 0.7, 0.8, 0.9);
        fixture.client.renderBounds_$eq(shrunk);
        fixture.client.renderMask_$eq(0b101010);
        fixture.client.render(position, -4);
        assertFaces(faces, 6, position, -4, fixture.bounds());
        assertEquals(Arrays.asList("render:-4", "material", "bounds"), fixture.events);
        faces.clear();
        fixture.events.clear();
        fixture.client.render(position, 0);
        assertFaces(faces, 3, position, 0, shrunk);
        assertEquals(Arrays.asList("render:0", "material"), fixture.events);
        faces.clear();
        fixture.client.renderMask_$eq(63);
        fixture.client.render(position, 1);
        assertTrue(faces.isEmpty());
    }

    @Test
    void particleForwardersReachIconEffectsAndPreserveEvaluationOrder() throws Exception {
        Fixture fixture = new Fixture();
        fixture.material((method, args) -> {
            assertEquals("getBreakingIcon", method);
            fixture.events.add("icon:" + args[0]);
            return null;
        });
        assertThrows(NullPointerException.class, () -> fixture.part.addDestroyEffects(null));
        assertEquals(
                Arrays.asList(
                        "material",
                        "icon:0",
                        "material",
                        "icon:1",
                        "material",
                        "icon:2",
                        "material",
                        "icon:3",
                        "material",
                        "icon:4",
                        "material",
                        "icon:5",
                        "bounds"),
                fixture.events);
        fixture.part.bind(new TileMultipart());
        fixture.events.clear();
        MovingObjectPosition hit = new MovingObjectPosition(0, 0, 0, 3, Vec3.createVectorHelper(0, 0, 0));
        assertThrows(NullPointerException.class, () -> fixture.part.addHitEffects(hit, null));
        assertEquals(Arrays.asList("bounds", "material", "icon:3"), fixture.events);
    }

    private static void assertFaces(List<Object[]> faces, int count, Vector3 position, int pass, Cuboid6 bounds) {
        assertEquals(count, faces.size());
        for (Object[] face : faces) {
            assertSame(position, face[0]);
            assertEquals(pass, face[1]);
            assertSame(bounds, face[2]);
        }
    }

    private interface MaterialCall {

        Object apply(String method, Object[] arguments);
    }

    private static final class Fixture extends ClassLoader {

        final Microblock part;
        final CommonMicroblockClient client;
        final List<String> events;

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            InputStream input = Objects
                    .requireNonNull(getClass().getResourceAsStream("/compat/ReferenceScalaMicroblockTraits.class.b64"));
            byte[] bytes;
            try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                bytes = Base64.getMimeDecoder().decode(scanner.next());
            }
            part = (Microblock) defineClass(null, bytes, 0, bytes.length).getConstructor().newInstance();
            client = (CommonMicroblockClient) part;
            events = (List<String>) get("events");
        }

        Object get(String name) throws Exception {
            return part.getClass().getMethod(name).invoke(part);
        }

        Cuboid6 bounds() throws Exception {
            return (Cuboid6) get("bounds");
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }

        void material(MaterialCall call) throws Exception {
            set(
                    "selectedMaterial",
                    IMicroMaterial.class,
                    Proxy.newProxyInstance(
                            getParent(),
                            new Class<?>[] { IMicroMaterial.class },
                            (proxy, method, args) -> call.apply(method.getName(), args)));
        }
    }

    private static final class TestClass extends CommonMicroClass {

        private int id;

        static TestClass create(int id) throws Exception {
            // MicroblockClass's constructor registers a trait and requires Forge initialization.
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field field = unsafe.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            TestClass value = (TestClass) unsafe.getMethod("allocateInstance", Class.class)
                    .invoke(field.get(null), TestClass.class);
            value.id = id;
            return value;
        }

        @Override
        public int getClassId() {
            return id;
        }

        @Override
        public int itemSlot() {
            return 0;
        }

        @Override
        public PlacementProperties placementProperties() {
            return null;
        }

        @Override
        public String getName() {
            return "test:trait-class";
        }

        @Override
        public Class<? extends Microblock> baseTrait() {
            return Microblock.class;
        }

        @Override
        public Class<? extends MicroblockClient> clientTrait() {
            return MicroblockClient.class;
        }

        @Override
        public float getResistanceFactor() {
            return 1;
        }
    }
}
