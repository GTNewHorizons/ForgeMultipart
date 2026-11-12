package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

class MicroblockRenderCharacterizationTest {

    private static final Set<String> METHODS = signatures(
            "face()Ljava/lang/ThreadLocal;",
            "renderCuboid(Lcodechicken/lib/vec/Vector3;Lcodechicken/microblock/MicroMaterialRegistry$IMicroMaterial;ILcodechicken/lib/vec/Cuboid6;I)V",
            "renderHighlight(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;Lcodechicken/microblock/CommonMicroClass;II)V",
            "renderItem(Lcodechicken/microblock/Microblock;II)V");

    @Test
    void keepsFacadeAndCompanionSurfaces() throws Exception {
        assertTrue(Modifier.isPublic(MicroblockRender.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroblockRender.class.getModifiers()));
        assertEquals(METHODS, publicDeclaredMethods(MicroblockRender.class));
        assertEquals(0, MicroblockRender.class.getDeclaredFields().length);
        for (Method method : MicroblockRender.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
        }

        assertTrue(Modifier.isPublic(MicroblockRender$.class.getModifiers()));
        assertTrue(Modifier.isFinal(MicroblockRender$.class.getModifiers()));
        assertEquals(METHODS, publicDeclaredMethods(MicroblockRender$.class));

        Field module = MicroblockRender$.class.getField("MODULE$");
        assertSame(MicroblockRender$.class, module.getType());
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(MicroblockRender$.MODULE$, module.get(null));

        Field face = MicroblockRender$.class.getDeclaredField("face");
        assertSame(ThreadLocal.class, face.getType());
        assertEquals(Modifier.PRIVATE | Modifier.FINAL, face.getModifiers());
        assertEquals(2, MicroblockRender$.class.getDeclaredFields().length);
    }

    @Test
    void keepsBlockFacesThreadLocal() throws InterruptedException {
        ThreadLocal<BlockFace> faces = MicroblockRender.face();
        assertSame(faces, MicroblockRender$.MODULE$.face());

        BlockFace local = faces.get();
        assertSame(local, faces.get());

        AtomicReference<BlockFace> other = new AtomicReference<>();
        Thread thread = new Thread(() -> other.set(faces.get()), "microblock-render-characterization");
        thread.start();
        thread.join();
        assertNotSame(local, other.get());
    }

    @Test
    void rendersOnlyUnmaskedCuboidFaces() {
        Vector3 pos = new Vector3(2, 3, 4);
        Cuboid6 bounds = new Cuboid6(0, 0, 0, 1, 1, 1);
        RecordingMaterial material = new RecordingMaterial();

        MicroblockRender.renderCuboid(pos, material, 7, bounds, 0);
        assertCalls(material.calls, 6, pos, 7, bounds);

        material.calls.clear();
        MicroblockRender$.MODULE$.renderCuboid(pos, material, -1, bounds, 0b101010);
        assertCalls(material.calls, 3, pos, -1, bounds);

        material.calls.clear();
        MicroblockRender.renderCuboid(pos, material, 0, bounds, 0b111111);
        assertEquals(0, material.calls.size());
    }

    @Test
    void keepsTransformedClientCallsAndNoPlacementExit() throws IOException {
        ClassNode type = new ClassNode();
        new ClassReader(MicroblockRender$.class.getName()).accept(type, 0);

        MethodNode item = method(type, "renderItem", "(Lcodechicken/microblock/Microblock;II)V");
        assertCall(item, Opcodes.INVOKEVIRTUAL, "codechicken/microblock/Microblock", "setShape", "(II)V");
        assertCall(
                item,
                Opcodes.INVOKEINTERFACE,
                "codechicken/microblock/MicroblockClient",
                "getBounds",
                "()Lcodechicken/lib/vec/Cuboid6;");
        assertCall(
                item,
                Opcodes.INVOKEINTERFACE,
                "codechicken/microblock/MicroblockClient",
                "render",
                "(Lcodechicken/lib/vec/Vector3;I)V");

        MethodNode highlight = method(
                type,
                "renderHighlight",
                "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;Lcodechicken/microblock/CommonMicroClass;II)V");
        int placement = callIndex(highlight, "codechicken/microblock/MicroblockPlacement$", "apply");
        int earlyReturn = opcodeIndexAfter(highlight, Opcodes.RETURN, placement);
        int firstGlCall = ownerCallIndex(highlight, "org/lwjgl/opengl/GL11", placement);
        assertTrue(placement >= 0);
        assertTrue(earlyReturn > placement);
        assertTrue(firstGlCall > earlyReturn);
        assertCall(
                highlight,
                Opcodes.INVOKEINTERFACE,
                "codechicken/microblock/MicroblockClient",
                "render",
                "(Lcodechicken/lib/vec/Vector3;I)V");
    }

    private static void assertCalls(List<RenderCall> calls, int count, Vector3 pos, int pass, Cuboid6 bounds) {
        assertEquals(count, calls.size());
        for (RenderCall call : calls) {
            assertSame(pos, call.pos);
            assertEquals(pass, call.pass);
            assertSame(bounds, call.bounds);
        }
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> methods = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                methods.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return methods;
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }

    private static MethodNode method(ClassNode type, String name, String descriptor) {
        for (MethodNode method : type.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + name + descriptor);
    }

    private static void assertCall(MethodNode method, int opcode, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == opcode && owner.equals(call.owner)
                        && name.equals(call.name)
                        && descriptor.equals(call.desc)) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing call " + owner + '.' + name + descriptor);
    }

    private static int callIndex(MethodNode method, String owner, String name) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        for (int i = 0; i < instructions.length; i++) {
            if (instructions[i] instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instructions[i];
                if (owner.equals(call.owner) && name.equals(call.name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int ownerCallIndex(MethodNode method, String owner, int start) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        for (int i = start + 1; i < instructions.length; i++) {
            if (instructions[i] instanceof MethodInsnNode && owner.equals(((MethodInsnNode) instructions[i]).owner)) {
                return i;
            }
        }
        return -1;
    }

    private static int opcodeIndexAfter(MethodNode method, int opcode, int start) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        for (int i = start + 1; i < instructions.length; i++) {
            if (instructions[i].getOpcode() == opcode) {
                return i;
            }
        }
        return -1;
    }

    private static final class RecordingMaterial implements IMicroMaterial {

        private final List<RenderCall> calls = new ArrayList<>();

        @Override
        public IIcon getBreakingIcon(int side) {
            return null;
        }

        @Override
        public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {
            calls.add(new RenderCall(pos, pass, bounds));
        }

        @Override
        public boolean isTransparent() {
            return false;
        }

        @Override
        public int getLightValue() {
            return 0;
        }

        @Override
        public float getStrength(EntityPlayer player) {
            return 0;
        }

        @Override
        public String getLocalizedName() {
            return "recording";
        }

        @Override
        public ItemStack getItem() {
            return null;
        }

        @Override
        public int getCutterStrength() {
            return 0;
        }

        @Override
        public SoundType getSound() {
            return null;
        }

        @Override
        public float explosionResistance(Entity entity) {
            return 0;
        }
    }

    private static final class RenderCall {

        private final Vector3 pos;
        private final int pass;
        private final Cuboid6 bounds;

        private RenderCall(Vector3 pos, int pass, Cuboid6 bounds) {
            this.pos = pos;
            this.pass = pass;
            this.bounds = bounds;
        }
    }
}
