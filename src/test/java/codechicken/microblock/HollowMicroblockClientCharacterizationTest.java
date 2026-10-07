package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.CCRenderState.IVertexOperation;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import scala.Function5;
import scala.MatchError;
import scala.runtime.AbstractFunction5;
import scala.runtime.BoxedUnit;

/** Executes original frozen forwarders, replacing only GPU-facing services with a call recorder. */
public class HollowMicroblockClientCharacterizationTest {

    @Test
    void initializationOrsTheDefaultOpeningAndRecalculationPreservesSuperLowBits() throws Exception {
        Fixture f = new Fixture();
        assertEquals(0x123c01, f.client.renderMask());
        for (int size : new int[] { -1, 0, 8, 11, Integer.MAX_VALUE }) {
            f.set("hollowSize", int.class, size);
            f.set("changeMaskOnSize", boolean.class, true);
            f.events.clear();
            f.client.recalcBounds();
            assertEquals(0x65 | size << 8, f.client.renderMask());
            assertEquals(Arrays.asList("super", "size"), f.events);
        }
        f.set("failSuper", boolean.class, true);
        f.events.clear();
        assertEquals("super", assertThrows(IllegalStateException.class, f.client::recalcBounds).getMessage());
        assertEquals(Collections.singletonList("super"), f.events);
        assertEquals(0x12345665, f.client.renderMask());
    }

    @Test
    void renderChoosesPhysicalTransparentAndOpaqueCallsWithLiveSlotDispatch() throws Exception {
        Fixture f = new Fixture();
        Cuboid6 render = new Cuboid6(0, 0, 0, 0.5, 0.5, 0.5);
        f.client.renderBounds_$eq(render);
        f.client.renderMask_$eq(0x851);
        for (int pass : new int[] { -1, -2, 0, 1 }) {
            for (boolean transparent : new boolean[] { false, true }) {
                f.set("transparent", boolean.class, transparent);
                f.set("slot", int.class, 2);
                f.set("slotAdvance", int.class, 1);
                f.clear();
                f.client.render(f.pos, pass);
                assertEquals(pass == -1 || transparent ? 1 : 2, f.draws.size());
                f.assertDraw(
                        0,
                        pass,
                        pass == -1 ? f.bounds : render,
                        pass == -1 ? 0 : transparent ? 0x851 : 0x855,
                        false);
                if (pass == -1) assertEquals(Arrays.asList("bounds", "hollow"), f.events);
                else if (transparent) assertEquals(Arrays.asList("transparent", "hollow"), f.events);
                else {
                    f.assertDraw(1, pass, Cuboid6.full, ~(1 << 3), true);
                    assertEquals(Arrays.asList("transparent", "slot", "hollow", "slot", "hollow"), f.events);
                    assertNotSame(f.draws.get(0)[5], f.draws.get(1)[5]);
                }
            }
        }
    }

    @Test
    void renderCallbacksInvokeMaterialDrawingAndFirstFailurePreventsTheSecondCall() throws Exception {
        Fixture f = new Fixture();
        List<Integer> sides = new ArrayList<>();
        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class },
                (proxy, method, args) -> {
                    assertEquals("renderMicroFace", method.getName());
                    assertSame(f.pos, args[0]);
                    assertEquals(7, args[1]);
                    assertSame(f.bounds, args[2]);
                    sides.add(MicroblockRender.face().get().side);
                    return null;
                });
        for (int pass : new int[] { -1, 0 }) {
            for (boolean transparent : new boolean[] { false, true }) {
                f.set("transparent", boolean.class, transparent);
                f.clear();
                f.client.render(f.pos, pass);
                for (Object[] draw : f.draws) {
                    sides.clear();
                    callback(draw).apply(f.pos, material, 7, f.bounds, 0x15);
                    assertEquals(Arrays.asList(1, 3, 5), sides);
                }
            }
        }
        f.set("transparent", boolean.class, false);
        f.set("afterHollow", Runnable.class, (Runnable) () -> { throw new IllegalStateException("draw"); });
        f.clear();
        assertEquals("draw", assertThrows(IllegalStateException.class, () -> f.client.render(f.pos, 0)).getMessage());
        assertEquals(1, f.draws.size());
    }

    @Test
    void rimCallbacksKeepAllSixOrderedBoundsAndFaceMasksOnEveryAxis() throws Exception {
        Fixture f = new Fixture();
        f.set("runHollow", boolean.class, true);
        f.client.renderMask_$eq(8 << 8);
        // Literal reference geometry at opening size 8, independent of callback implementation.
        double[][][] geometry = {
                { { .25, .2, .75, .75, .8, .7 }, { .25, .2, .3, .75, .8, .25 }, { .75, .2, .25, .9, .8, .75 },
                        { .1, .2, .25, .25, .8, .75 }, { .1, .2, .75, .9, .8, .7 }, { .1, .2, .3, .9, .8, .25 } },
                { { .75, .25, .3, .9, .75, .7 }, { .1, .25, .3, .25, .75, .7 }, { .25, .75, .3, .75, .8, .7 },
                        { .25, .2, .3, .75, .25, .7 }, { .75, .2, .3, .9, .8, .7 }, { .1, .2, .3, .25, .8, .7 } },
                { { .1, .75, .25, .9, .8, .75 }, { .1, .2, .25, .9, .25, .75 }, { .1, .25, .75, .9, .75, .7 },
                        { .1, .25, .3, .9, .75, .25 }, { .1, .75, .3, .9, .8, .7 }, { .1, .2, .3, .9, .25, .7 } } };
        int[][] masks0 = { { 59, 55, 12, 12, 4, 8 }, { 47, 31, 48, 48, 16, 32 }, { 62, 61, 3, 3, 1, 2 } };
        int[][] masks63 = { { 59, 55, 47, 31, 63, 63 }, { 47, 31, 62, 61, 63, 63 }, { 62, 61, 59, 55, 63, 63 } };
        int[] faceMask = { 60, 51, 15 };
        for (int side = 0; side < 6; side++) {
            f.set("slot", int.class, side);
            for (boolean face : new boolean[] { false, true }) {
                for (int mask : new int[] { 0, 63, -1 }) {
                    List<Cuboid6> boxes = new ArrayList<>();
                    List<Integer> masks = new ArrayList<>();
                    f.clear();
                    f.client.renderHollow(f.pos, 7, f.bounds, mask, face, collect((pos, mat, pass, box, bits) -> {
                        assertSame(f.pos, pos);
                        assertNull(mat);
                        assertEquals(7, pass);
                        boxes.add(box);
                        masks.add(bits);
                    }));
                    assertEquals(Arrays.asList("hollow", "material", "slot"), f.events);
                    assertEquals(6, boxes.size());
                    for (int i = 0; i < 6; i++) {
                        assertArrayEquals(geometry[side / 2][i], coords(boxes.get(i)), 0);
                        assertEquals(
                                (mask == 0 ? masks0 : masks63)[side / 2][i] | (face ? faceMask[side / 2] : 0),
                                masks.get(i));
                        for (int j = 0; j < i; j++) assertNotSame(boxes.get(i), boxes.get(j));
                    }
                }
            }
        }
    }

    @Test
    void rimRenderingSnapshotsMaterialMaskAndCoordinatesBeforeCallbacksAndPreservesFailures() throws Exception {
        Fixture f = new Fixture();
        f.set("runHollow", boolean.class, true);
        List<Cuboid6> boxes = new ArrayList<>();
        for (int size : new int[] { -8, 0, 11, 32 }) {
            f.client.renderMask_$eq(size << 8 | 0x55);
            boxes.clear();
            f.client.renderHollow(f.pos, 0, Cuboid6.full, 0, false, collect((pos, mat, pass, box, mask) -> {
                boxes.add(box);
                f.client.renderMask_$eq(0);
                f.setUnchecked("slot", int.class, 5);
            }));
            assertEquals(0.5 - size / 32D, boxes.get(0).min.x);
            assertEquals(0.5 + size / 32D, boxes.get(0).max.x);
            f.set("slot", int.class, 0);
        }
        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class },
                (proxy, method, args) -> { throw new AssertionError("material invoked"); });
        f.set("selectedMaterial", IMicroMaterial.class, material);
        f.client.renderMask_$eq(8 << 8);
        Cuboid6 mutable = f.bounds.copy();
        boxes.clear();
        f.client.renderHollow(f.pos, 0, mutable, 0, false, collect((pos, mat, pass, box, mask) -> {
            assertSame(material, mat);
            boxes.add(box);
            mutable.max.x = 99;
            f.setUnchecked("selectedMaterial", IMicroMaterial.class, null);
        }));
        assertEquals(.9, boxes.get(5).max.x);
        f.clear();
        assertThrows(NullPointerException.class, () -> f.client.renderHollow(null, 0, null, 0, false, null));
        assertEquals(Arrays.asList("hollow", "material"), f.events);
        for (int side : new int[] { -1, 6, 32 }) {
            f.set("slot", int.class, side);
            assertThrows(MatchError.class, () -> f.client.renderHollow(null, 0, f.bounds, 0, false, null));
        }
        f.set("slot", int.class, 0);
        int[] calls = { 0 };
        assertThrows(
                IllegalStateException.class,
                () -> f.client.renderHollow(null, 0, f.bounds, 0, false, collect((pos, mat, pass, box, mask) -> {
                    calls[0]++;
                    throw new IllegalStateException("callback");
                })));
        assertEquals(1, calls[0]);
    }

    @Test
    void highlightEmitsOrderedStateChangesTransformAndExpandedOutlines() throws Exception {
        Fixture f = new Fixture();
        f.set("rawShape", byte.class, (byte) 32);
        f.set("shapeAdvance", int.class, 1);
        assertTrue(f.part.drawHighlight(null, null, 0.625F));
        assertEquals(
                Arrays.asList(
                        "size",
                        "shape",
                        "enable:3042",
                        "blend:770:771",
                        "disable:3553",
                        "color:0.0:0.0:0.0:0.4",
                        "line:2.0",
                        "depth:false",
                        "push",
                        "world:0.625",
                        "x",
                        "y",
                        "z",
                        "translate:2.0:-3.0:4.0",
                        "shape",
                        "rotation",
                        "outline",
                        "outline",
                        "pop",
                        "depth:true",
                        "enable:3553",
                        "disable:3042"),
                f.events);
        assertArrayEquals(
                new double[] { -.001, -.001, -.001, 1.001, .251, 1.001 },
                coords(RenderRecorder.outlines.get(0)),
                1e-12);
        assertArrayEquals(
                new double[] { .251, .001, .251, .749, .249, .749 },
                coords(RenderRecorder.outlines.get(1)),
                1e-12);
        Vector3 expected = new Vector3(.2, .3, .4).apply(Rotation.sideRotations[1].at(Vector3.center));
        Vector3 actual = new Vector3(.2, .3, .4).apply(RenderRecorder.rotation);
        assertEquals(expected, actual);
    }

    @Test
    void highlightFailuresStopAtTheOriginalPointWithoutInventingCleanup() throws Exception {
        Fixture f = new Fixture();
        f.set("rawShape", byte.class, (byte) 38);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.part.drawHighlight(null, null, 0));
        assertEquals("shape", f.events.get(f.events.size() - 1));
        assertFalse(f.events.contains("pop"));
        assertTrue(RenderRecorder.outlines.isEmpty());
        f.clear();
        f.set("rawShape", byte.class, (byte) 32);
        RenderRecorder.failOutline = true;
        assertThrows(IllegalStateException.class, () -> f.part.drawHighlight(null, null, 0));
        assertEquals("outline", f.events.get(f.events.size() - 1));
        assertFalse(f.events.contains("pop"));
    }

    @Test
    void breakingBuildsTranslationAndIconPipelineAndDispatchesItsCuboidCallback() throws Exception {
        Fixture f = new Fixture();
        RenderBlocks render = new RenderBlocks();
        IIcon icon = (IIcon) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IIcon.class },
                (proxy, method, args) -> { throw new AssertionError("icon sampled"); });
        render.overrideBlockTexture = icon;
        f.part.drawBreaking(render);
        assertEquals(Arrays.asList("state", "reset", "x", "y", "z", "pipeline", "bounds", "hollow"), f.events);
        assertEquals(2, RenderRecorder.pipeline.length);
        Vector3 translated = new Vector3().apply((Translation) RenderRecorder.pipeline[0]);
        assertEquals(new Vector3(2, -3, 4), translated);
        assertSame(icon, ((IconTransformation) RenderRecorder.pipeline[1]).icon);
        Object[] draw = f.draws.get(0);
        assertNull(draw[0]);
        assertEquals(0, draw[1]);
        assertSame(f.bounds, draw[2]);
        assertEquals(0, draw[3]);
        assertEquals(false, draw[4]);
        callback(draw).apply(null, null, 0, f.bounds, 37);
        assertSame(f.bounds, RenderRecorder.breakBox);
        assertEquals(37, RenderRecorder.breakMask);
        f.clear();
        assertThrows(NullPointerException.class, () -> f.part.drawBreaking(null));
        assertEquals(Arrays.asList("state", "reset", "x", "y", "z"), f.events);
    }

    private interface Draw {

        void accept(Vector3 pos, IMicroMaterial material, int pass, Cuboid6 box, int mask);
    }

    private static Function5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit> collect(Draw draw) {
        return new AbstractFunction5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit>() {

            @Override
            public BoxedUnit apply(Vector3 pos, IMicroMaterial material, Object pass, Cuboid6 box, Object mask) {
                draw.accept(pos, material, (Integer) pass, box, (Integer) mask);
                return BoxedUnit.UNIT;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Function5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit> callback(Object[] draw) {
        return (Function5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit>) draw[5];
    }

    private static double[] coords(Cuboid6 c) {
        return new double[] { c.min.x, c.min.y, c.min.z, c.max.x, c.max.y, c.max.z };
    }

    private static final class Fixture extends ClassLoader {

        final List<String> events = new ArrayList<>();
        final List<Object[]> draws;
        final Microblock part;
        final HollowMicroblockClient client;
        final Cuboid6 bounds;
        final Vector3 pos = new Vector3(2, 3, 4);

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            for (String name : new String[] { "ReferenceHollowClientBase", "ReferenceScalaHollowMicroblockClient" }) {
                byte[] bytes;
                try (Scanner scanner = new Scanner(
                        getResourceAsStream("compat/" + name + ".class.b64"),
                        StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
            }
            part = (Microblock) loadClass("codechicken.multipart.compat.ReferenceScalaHollowMicroblockClient")
                    .getConstructor().newInstance();
            client = (HollowMicroblockClient) part;
            bounds = (Cuboid6) part.getClass().getMethod("bounds").invoke(part);
            draws = (List<Object[]>) part.getClass().getMethod("draws").invoke(part);
            set("events", List.class, events);
            RenderRecorder.events = events;
            RenderRecorder.outlines.clear();
            RenderRecorder.failOutline = false;
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }

        void setUnchecked(String name, Class<?> type, Object value) {
            try {
                set(name, type, value);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }

        void clear() {
            events.clear();
            draws.clear();
            RenderRecorder.outlines.clear();
        }

        void assertDraw(int index, int pass, Cuboid6 c, int mask, boolean face) {
            Object[] draw = draws.get(index);
            assertSame(pos, draw[0]);
            assertEquals(pass, draw[1]);
            assertSame(c, draw[2]);
            assertEquals(mask, draw[3]);
            assertEquals(face, draw[4]);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("codechicken.microblock.HollowMicroblockClient$")
                    && !name.startsWith("codechicken.microblock.HollowMicroblockClientLogic"))
                return super.loadClass(name, resolve);
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (input == null) throw new ClassNotFoundException(name);
                    ClassReader reader = new ClassReader(input);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassVisitor(ASM5, writer) {

                        @Override
                        public MethodVisitor visitMethod(int access, String method, String desc, String sig,
                                String[] exceptions) {
                            return new MethodVisitor(ASM5, super.visitMethod(access, method, desc, sig, exceptions)) {

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String method, String descriptor,
                                        boolean itf) {
                                    String target = Type.getInternalName(RenderRecorder.class);
                                    if (owner.equals("org/lwjgl/opengl/GL11")
                                            || owner.equals("codechicken/lib/render/RenderUtils")
                                            || owner.equals("codechicken/lib/render/BlockRenderer")) {
                                        super.visitMethodInsn(opcode, target, method, descriptor, false);
                                        return;
                                    }
                                    if (owner.equals("codechicken/lib/vec/Transformation")
                                            && method.equals("glApply")) {
                                        super.visitMethodInsn(
                                                INVOKESTATIC,
                                                target,
                                                "glApply",
                                                "(Lcodechicken/lib/vec/Transformation;)V",
                                                false);
                                        return;
                                    }
                                    if (owner.equals("codechicken/lib/render/CCRenderState")) {
                                        if (opcode == INVOKEVIRTUAL)
                                            descriptor = "(Lcodechicken/lib/render/CCRenderState;"
                                                    + descriptor.substring(1);
                                        super.visitMethodInsn(INVOKESTATIC, target, method, descriptor, false);
                                        return;
                                    }
                                    super.visitMethodInsn(opcode, owner, method, descriptor, itf);
                                }
                            };
                        }
                    }, 0);
                    byte[] bytes = writer.toByteArray();
                    loaded = defineClass(name, bytes, 0, bytes.length);
                } catch (java.io.IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    public static final class RenderRecorder {

        static List<String> events;
        static final List<Cuboid6> outlines = new ArrayList<>();
        static Transformation rotation;
        static boolean failOutline;
        static IVertexOperation[] pipeline;
        static Cuboid6 breakBox;
        static int breakMask;

        public static void glEnable(int value) {
            events.add("enable:" + value);
        }

        public static void glDisable(int value) {
            events.add("disable:" + value);
        }

        public static void glBlendFunc(int a, int b) {
            events.add("blend:" + a + ":" + b);
        }

        public static void glColor4f(float a, float b, float c, float d) {
            events.add("color:" + a + ":" + b + ":" + c + ":" + d);
        }

        public static void glLineWidth(float value) {
            events.add("line:" + value);
        }

        public static void glDepthMask(boolean value) {
            events.add("depth:" + value);
        }

        public static void glPushMatrix() {
            events.add("push");
        }

        public static void glPopMatrix() {
            events.add("pop");
        }

        public static void glTranslated(double x, double y, double z) {
            events.add("translate:" + x + ":" + y + ":" + z);
        }

        public static void translateToWorldCoords(Entity player, float frame) {
            assertNull(player);
            events.add("world:" + frame);
        }

        public static void glApply(Transformation value) {
            events.add("rotation");
            rotation = value;
        }

        public static void drawCuboidOutline(Cuboid6 box) {
            events.add("outline");
            outlines.add(box);
            if (failOutline) throw new IllegalStateException("outline");
        }

        public static CCRenderState instance() {
            events.add("state");
            return CCRenderState.instance();
        }

        public static void resetInstance(CCRenderState state) {
            events.add("reset");
        }

        public static void setPipelineInstance(CCRenderState state, IVertexOperation[] operations) {
            events.add("pipeline");
            pipeline = operations;
        }

        public static void renderCuboid(Cuboid6 box, int mask) {
            events.add("break");
            breakBox = box;
            breakMask = mask;
        }
    }
}
