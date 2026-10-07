package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.vec.Vector3;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import codechicken.multipart.asm.MultipartMixinFactory;
import scala.collection.JavaConversions;
import scala.collection.immutable.Nil$;

/** Executes the shipped renderer method against real generated client tiles, replacing only its GL/pass services. */
class MultipartRendererFunctionalTest {

    @BeforeEach
    void resetRenderState() {
        RenderServices.events.clear();
        RenderServices.INSTANCE.useNormals = false;
        RenderServices.dynamicCalls = 0;
        RenderServices.position = null;
    }

    @Test
    void nonemptyStaticTileReturnsWithoutTouchingRenderState() throws Exception {
        TileMultipart tile = newClientTile();
        RecordingPart part = new RecordingPart();
        setParts(tile, part);
        setField(tile, "hasDynamicParts", false);

        render(tile);

        assertTrue(RenderServices.events.isEmpty());
        assertEquals(0, RenderServices.dynamicCalls);
    }

    @Test
    void dynamicTileInitializesStateAndDispatchesWithTheSameCoordinatesFrameAndPass() throws Exception {
        TileMultipart tile = newClientTile();
        RecordingPart part = new RecordingPart();
        setParts(tile, part);
        setField(tile, "hasDynamicParts", true);
        setField(tile, "codechicken$multipart$TileMultipartClient$$dynamicCache", new TMultiPart[] { part });

        render(tile);

        assertEquals(Arrays.asList("instance", "reset", "lightmap", "pass", "tile"), RenderServices.events);
        assertEquals(1, RenderServices.dynamicCalls);
        assertEquals(1.25, RenderServices.position.x);
        assertEquals(-2.5, RenderServices.position.y);
        assertEquals(3.75, RenderServices.position.z);
        assertEquals(0.625F, RenderServices.frame);
        assertEquals(1, RenderServices.pass);
        assertTrue(RenderServices.INSTANCE.useNormals);
    }

    @Test
    void emptyTileShortCircuitsEvenWithADynamicFlagAndCache() throws Exception {
        TileMultipart tile = newClientTile();
        RecordingPart part = new RecordingPart();
        setField(tile, "hasDynamicParts", true);
        setField(tile, "codechicken$multipart$TileMultipartClient$$dynamicCache", new TMultiPart[] { part });

        render(tile);

        assertTrue(RenderServices.events.isEmpty());
        assertEquals(0, RenderServices.dynamicCalls);
    }

    @Test
    void rejectsTilesWithoutTheClientTraitBeforeTouchingRenderState() throws Exception {
        assertThrows(ClassCastException.class, () -> render(new TileMultipart()));
        assertTrue(RenderServices.events.isEmpty());
    }

    private static TileMultipart newClientTile() throws Exception {
        assertTrue(
                TileMultipartClient.class.isInterface(),
                "Exercise the transformed runtime type, not the Java input");
        BitSet traits = new BitSet();
        traits.set(MultipartMixinFactory.getId(TileMultipartClient.class.getName().replace('.', '/')));
        TileMultipart generated = (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
        // Keep the real generated interface/flag getter. Drawing is recorded because Forge strips the actual
        // TMultiPart.renderDynamic method on this dedicated server.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String parent = Type.getInternalName(generated.getClass());
        writer.visit(V1_8, ACC_PUBLIC, "codechicken/multipart/test/RendererRecordingTile", null, parent, null);
        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        String descriptor = "(Lcodechicken/lib/vec/Vector3;FI)V";
        MethodVisitor draw = writer.visitMethod(ACC_PUBLIC, "renderDynamic", descriptor, null, null);
        draw.visitCode();
        draw.visitVarInsn(ALOAD, 1);
        draw.visitVarInsn(FLOAD, 2);
        draw.visitVarInsn(ILOAD, 3);
        draw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RenderServices.class), "draw", descriptor, false);
        draw.visitInsn(RETURN);
        draw.visitMaxs(0, 0);
        draw.visitEnd();
        writer.visitEnd();
        return (TileMultipart) new BodyLoader().define(writer.toByteArray()).getConstructor().newInstance();
    }

    private static void setParts(TileMultipart tile, TMultiPart... parts) {
        tile.partList_$eq(JavaConversions.asScalaBuffer(Arrays.asList(parts)).toList());
    }

    private static void setField(TileMultipart tile, String name, Object value) throws Exception {
        Field field = tile.getClass().getSuperclass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(tile, value);
    }

    private static void render(TileEntity tile) throws Exception {
        Object renderer = RendererBody.TYPE.getConstructor().newInstance();
        try {
            RendererBody.METHOD.invoke(renderer, tile, 1.25, -2.5, 3.75, 0.625F);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof Exception) throw (Exception) cause;
            throw new AssertionError(cause);
        }
    }

    private static final class RendererBody {

        static final Class<?> TYPE;
        static final Method METHOD;

        static {
            try {
                // Loading the client-only TESR class on a server is forbidden. Copy its actual method body without
                // changing any tile casts, field accesses or invocation opcodes; only rendering services are replaced.
                ClassNode source = new ClassNode();
                try (InputStream input = MultipartRendererFunctionalTest.class
                        .getResourceAsStream("/codechicken/multipart/MultipartRenderer$.class")) {
                    assertNotNull(input);
                    new ClassReader(input).accept(source, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                String name = "codechicken/multipart/test/HeadlessMultipartRenderer";
                writer.visit(V1_8, ACC_PUBLIC, name, null, "java/lang/Object", null);
                MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                init.visitCode();
                init.visitVarInsn(ALOAD, 0);
                init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                init.visitInsn(RETURN);
                init.visitMaxs(0, 0);
                init.visitEnd();
                boolean found = false;
                for (MethodNode method : source.methods) {
                    if (!method.name.equals("renderTileEntityAt")) continue;
                    assertFalse(found);
                    found = true;
                    MethodVisitor target = writer.visitMethod(ACC_PUBLIC, method.name, method.desc, null, null);
                    method.accept(new RemappingMethodAdapter(ACC_PUBLIC, method.desc, target, new Remapper() {

                        @Override
                        public String map(String internalName) {
                            if (internalName.equals("codechicken/lib/render/CCRenderState")
                                    || internalName.equals("net/minecraftforge/client/MinecraftForgeClient"))
                                return Type.getInternalName(RenderServices.class);
                            return internalName;
                        }
                    }));
                }
                assertTrue(found);
                writer.visitEnd();
                TYPE = new BodyLoader().define(writer.toByteArray());
                METHOD = TYPE.getMethod(
                        "renderTileEntityAt",
                        TileEntity.class,
                        double.class,
                        double.class,
                        double.class,
                        float.class);
            } catch (Exception failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }
    }

    private static final class BodyLoader extends ClassLoader {

        BodyLoader() {
            super(MultipartRendererFunctionalTest.class.getClassLoader());
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    public static final class RenderServices {

        static final RenderServices INSTANCE = new RenderServices();
        static final List<String> events = new ArrayList<>();
        public boolean useNormals;
        static int dynamicCalls;
        static Vector3 position;
        static float frame;
        static int pass;

        public static void draw(Vector3 position, float frame, int pass) {
            assertTrue(INSTANCE.useNormals);
            events.add("tile");
            dynamicCalls++;
            RenderServices.position = position;
            RenderServices.frame = frame;
            RenderServices.pass = pass;
        }

        public static RenderServices instance() {
            events.add("instance");
            return INSTANCE;
        }

        public void resetInstance() {
            events.add("reset");
            useNormals = false;
        }

        public void pullLightmapInstance() {
            events.add("lightmap");
        }

        public static int getRenderPass() {
            events.add("pass");
            return 1;
        }
    }

    private static final class RecordingPart extends TMultiPart {

        @Override
        public String getType() {
            return "renderer-regression";
        }

    }
}
