package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.render.CCRenderPipeline;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.ColourMultiplier;
import codechicken.lib.render.uv.MultiIconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.collection.JavaConversions;

class BlockMicroMaterialCharacterizationTest {

    private static final Set<String> THREAD_STATE_METHODS = signatures(
            "builder()Lcodechicken/lib/render/CCRenderPipeline$PipelineBuilder;",
            "builder_$eq(Lcodechicken/lib/render/CCRenderPipeline$PipelineBuilder;)V",
            "pass()I",
            "pass_$eq(I)V");
    private static final Set<String> RENDER_HELPER_METHODS = signatures(
            "blockAndMeta(Lnet/minecraft/block/Block;I)Lcodechicken/microblock/MaterialRenderHelper$;",
            "blockColour(I)Lcodechicken/microblock/MaterialRenderHelper$;",
            "builder()Lcodechicken/lib/render/CCRenderPipeline$PipelineBuilder;",
            "builder_$eq(Lcodechicken/lib/render/CCRenderPipeline$PipelineBuilder;)V",
            "lighting()Lcodechicken/microblock/MaterialRenderHelper$;",
            "pass()I",
            "pass_$eq(I)V",
            "render()V",
            "start(Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/render/uv/UVTransformation;)Lcodechicken/microblock/MaterialRenderHelper$;");
    private static final Set<String> MATERIAL_COMPANION_METHODS = signatures(
            "$lessinit$greater$default$2()I",
            "createAndRegister(Lnet/minecraft/block/Block;I)V",
            "createAndRegister(Lnet/minecraft/block/Block;ILjava/lang/String;)V",
            "createAndRegister(Lnet/minecraft/block/Block;ILjava/lang/String;Ljava/lang/String;)V",
            "createAndRegister(Lnet/minecraft/block/Block;Lscala/collection/Seq;)V",
            "createAndRegister(Lnet/minecraft/block/Block;Lscala/collection/Seq;Ljava/lang/String;)V",
            "createAndRegister(Lnet/minecraft/block/Block;Lscala/collection/Seq;Ljava/lang/String;Ljava/lang/String;)V",
            "createAndRegister$default$2()I",
            "materialKey(Lnet/minecraft/block/Block;)Ljava/lang/String;",
            "materialKey(Lnet/minecraft/block/Block;I)Ljava/lang/String;",
            "materialKey(Ljava/lang/String;I)Ljava/lang/String;",
            "oldKey(Lnet/minecraft/block/Block;)Ljava/lang/String;");
    private static final Set<String> MATERIAL_INSTANCE_METHODS = signatures(
            "block()Lnet/minecraft/block/Block;",
            "blockKey()Ljava/lang/String;",
            "canRenderInPass(I)Z",
            "codechicken$microblock$BlockMicroMaterial$$safeIcon$1(Lnet/minecraft/block/Block;I)Lnet/minecraft/util/IIcon;",
            "explosionResistance(Lnet/minecraft/entity/Entity;)F",
            "getBreakingIcon(I)Lnet/minecraft/util/IIcon;",
            "getColour(I)I",
            "getCutterStrength()I",
            "getItem()Lnet/minecraft/item/ItemStack;",
            "getLightValue()I",
            "getLocalizedName()Ljava/lang/String;",
            "getSound()Lnet/minecraft/block/Block$SoundType;",
            "getStrength(Lnet/minecraft/entity/player/EntityPlayer;)F",
            "icont()Lcodechicken/lib/render/uv/MultiIconTransformation;",
            "icont_$eq(Lcodechicken/lib/render/uv/MultiIconTransformation;)V",
            "isTransparent()Z",
            "loadIcons()V",
            "meta()I",
            "renderMicroFace(Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/vec/Cuboid6;)V",
            "toolClasses()Lscala/collection/Seq;");

    @Test
    void keepsTheFiveRetainedClassSurfacesAndStateShapes() throws Exception {
        assertClass(ThreadState.class, Object.class, false, THREAD_STATE_METHODS);
        assertField(ThreadState.class, "pass", int.class, false, false);
        assertField(ThreadState.class, "builder", CCRenderPipeline.PipelineBuilder.class, false, false);

        assertClass(MaterialRenderHelper.class, Object.class, true, RENDER_HELPER_METHODS);
        assertClass(MaterialRenderHelper$.class, Object.class, true, RENDER_HELPER_METHODS);
        assertEquals(0, MaterialRenderHelper.class.getDeclaredFields().length);
        assertField(MaterialRenderHelper$.class, "threadState", ThreadLocal.class, false, true);
        assertModule(MaterialRenderHelper$.class, MaterialRenderHelper$.MODULE$);

        assertTrue(Modifier.isPublic(BlockMicroMaterial.class.getModifiers()));
        assertFalse(Modifier.isFinal(BlockMicroMaterial.class.getModifiers()));
        assertSame(Object.class, BlockMicroMaterial.class.getSuperclass());
        assertEquals(Arrays.asList(IMicroMaterial.class), Arrays.asList(BlockMicroMaterial.class.getInterfaces()));
        Set<String> materialMethods = new TreeSet<>(MATERIAL_COMPANION_METHODS);
        materialMethods.addAll(MATERIAL_INSTANCE_METHODS);
        assertEquals(materialMethods, publicDeclaredMethods(BlockMicroMaterial.class));
        assertTrue(Modifier.isPublic(BlockMicroMaterial.class.getConstructor(Block.class, int.class).getModifiers()));
        assertEquals(1, BlockMicroMaterial.class.getDeclaredConstructors().length);
        assertField(BlockMicroMaterial.class, "block", Block.class, false, true);
        assertField(BlockMicroMaterial.class, "meta", int.class, false, true);
        assertField(BlockMicroMaterial.class, "blockKey", String.class, false, true);
        Field icons = assertField(BlockMicroMaterial.class, "icont", MultiIconTransformation.class, false, false);
        assertSideOnly(icons.getAnnotation(SideOnly.class));
        assertEquals(4, BlockMicroMaterial.class.getDeclaredFields().length);

        assertClass(BlockMicroMaterial$.class, Object.class, true, MATERIAL_COMPANION_METHODS);
        assertModule(BlockMicroMaterial$.class, BlockMicroMaterial$.MODULE$);
    }

    @Test
    void keepsTheExactClientOnlyBoundary() throws Exception {
        for (Method method : BlockMicroMaterial.class.getDeclaredMethods()) {
            SideOnly annotation = method.getAnnotation(SideOnly.class);
            if (method.getName().equals("loadIcons") || method.getName().equals("getBreakingIcon")) {
                assertSideOnly(annotation);
            } else {
                assertNull(annotation, method.toString());
            }
        }
        Method safeIcon = BlockMicroMaterial.class
                .getDeclaredMethod("codechicken$microblock$BlockMicroMaterial$$safeIcon$1", Block.class, int.class);
        assertTrue(Modifier.isFinal(safeIcon.getModifiers()));
    }

    @Test
    void keepsMaterialKeysDefaultsAndCommonBlockBehavior() {
        Block block = new Block(Material.rock) {};
        BlockMicroMaterial stone = new BlockMicroMaterial(block, 17);
        assertSame(block, stone.block());
        assertEquals(17, stone.meta());
        assertNull(stone.blockKey());
        assertEquals(block.getUnlocalizedName(), BlockMicroMaterial.oldKey(block));
        assertNull(BlockMicroMaterial.materialKey(block));
        assertEquals("example", BlockMicroMaterial.materialKey("example", 0));
        assertEquals("example", BlockMicroMaterial.materialKey("example", -1));
        assertEquals("example_2", BlockMicroMaterial.materialKey("example", 2));
        assertEquals(0, BlockMicroMaterial.$lessinit$greater$default$2());
        assertEquals(0, BlockMicroMaterial.createAndRegister$default$2());
        assertEquals(0, BlockMicroMaterial$.MODULE$.$lessinit$greater$default$2());
        assertEquals(0, BlockMicroMaterial$.MODULE$.createAndRegister$default$2());

        assertEquals(!block.isOpaqueCube(), stone.isTransparent());
        assertEquals(block.getLightValue(), stone.getLightValue());
        assertEquals(block.getHarvestLevel(1), stone.getCutterStrength());
        assertSame(block.stepSound, stone.getSound());
        assertEquals(block.getExplosionResistance(null), stone.explosionResistance(null));
        assertEquals(block.canRenderInPass(1), stone.canRenderInPass(1));
        assertEquals((block.getBlockColor() << 8) | 0xFF, stone.getColour(-1));
        assertEquals(Arrays.asList("axe", "pickaxe", "shovel"), JavaConversions.seqAsJavaList(stone.toolClasses()));

        assertNull(stone.icont());
        MultiIconTransformation icons = new MultiIconTransformation((net.minecraft.util.IIcon) null);
        stone.icont_$eq(icons);
        assertSame(icons, stone.icont());
    }

    @Test
    void keepsRenderHelperStateThreadLocalAndFacadeBacked() throws Exception {
        ThreadState state = new ThreadState();
        assertEquals(0, state.pass());
        assertNull(state.builder());
        state.pass_$eq(4);
        assertEquals(4, state.pass());

        MaterialRenderHelper.pass_$eq(9);
        assertEquals(9, MaterialRenderHelper.pass());
        assertEquals(9, MaterialRenderHelper$.MODULE$.pass());
        AtomicInteger otherThreadPass = new AtomicInteger(-1);
        Thread thread = new Thread(() -> otherThreadPass.set(MaterialRenderHelper.pass()));
        thread.start();
        thread.join();
        assertEquals(0, otherThreadPass.get());

        MultiIconTransformation icons = new MultiIconTransformation((net.minecraft.util.IIcon) null);
        assertSame(MaterialRenderHelper$.MODULE$, MaterialRenderHelper.start(new Vector3(), -1, icons));
        assertNotNull(MaterialRenderHelper.builder());
        MaterialRenderHelper.pass_$eq(0);
        MaterialRenderHelper.builder_$eq(null);
    }

    @Test
    void baseMaterialBuildsTheInventoryRenderPipeline() throws Exception {
        BlockMicroMaterial material = new BlockMicroMaterial(Blocks.stone, 0) {

            @Override
            public int getColour(int pass) {
                return 0x89ABCDEF;
            }
        };
        MultiIconTransformation icons = new MultiIconTransformation((net.minecraft.util.IIcon) null);
        material.icont_$eq(icons);
        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        BlockFace face = new BlockFace();
        face.side = 1;
        state.model = face;
        state.firstVertexIndex = 0;
        state.lastVertexIndex = 0;

        material.renderMicroFace(new Vector3(), -1, Cuboid6.full);

        Field operationsField = CCRenderPipeline.class.getDeclaredField("ops");
        operationsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<CCRenderState.IVertexOperation> operations = new ArrayList<>(
                (List<CCRenderState.IVertexOperation>) operationsField.get(state.pipeline));
        assertTrue(operations.contains(icons));
        assertTrue(operations.stream().anyMatch(ColourMultiplier.class::isInstance));
    }

    private static void assertClass(Class<?> type, Class<?> superclass, boolean isFinal, Set<String> methods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertEquals(isFinal, Modifier.isFinal(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static Field assertField(Class<?> owner, String name, Class<?> type, boolean isStatic, boolean isFinal)
            throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertEquals(isStatic, Modifier.isStatic(field.getModifiers()));
        assertEquals(isFinal, Modifier.isFinal(field.getModifiers()));
        return field;
    }

    private static void assertModule(Class<?> type, Object expected) throws Exception {
        Field module = type.getField("MODULE$");
        assertSame(type, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(expected, module.get(null));
    }

    private static void assertSideOnly(SideOnly annotation) {
        assertNotNull(annotation);
        assertSame(Side.CLIENT, annotation.value());
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
