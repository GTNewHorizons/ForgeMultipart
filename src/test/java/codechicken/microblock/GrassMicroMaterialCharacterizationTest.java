package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.render.CCRenderPipeline;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.ColourMultiplier;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.render.uv.MultiIconTransformation;
import codechicken.lib.render.uv.UVTransformation;
import codechicken.lib.render.uv.UVTransformationList;
import codechicken.lib.render.uv.UVTranslation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.relauncher.SideOnly;

class GrassMicroMaterialCharacterizationTest {

    private static final Set<String> GRASS_METHODS = new TreeSet<>(
            Arrays.asList(
                    "loadIcons()V",
                    "renderMicroFace(Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/vec/Cuboid6;)V",
                    "sideIconT()Lcodechicken/lib/render/uv/IconTransformation;",
                    "sideIconT_$eq(Lcodechicken/lib/render/uv/IconTransformation;)V"));
    private static final Set<String> TOP_METHODS = new TreeSet<>(
            Arrays.asList(
                    "$lessinit$greater$default$2()I",
                    "renderMicroFace(Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/vec/Cuboid6;)V"));

    @Test
    void keepsTheTwoClassSurfacesAndTopDefaultCompanion() throws Exception {
        assertEquals(BlockMicroMaterial.class, GrassMicroMaterial.class.getSuperclass());
        assertEquals(BlockMicroMaterial.class, TopMicroMaterial.class.getSuperclass());
        assertFalse(Modifier.isFinal(GrassMicroMaterial.class.getModifiers()));
        assertFalse(Modifier.isFinal(TopMicroMaterial.class.getModifiers()));
        assertNull(GrassMicroMaterial.class.getAnnotation(SideOnly.class));
        assertNull(TopMicroMaterial.class.getAnnotation(SideOnly.class));
        assertEquals(GRASS_METHODS, publicDeclaredMethodSignatures(GrassMicroMaterial.class));
        assertEquals(TOP_METHODS, publicDeclaredMethodSignatures(TopMicroMaterial.class));

        Constructor<GrassMicroMaterial> grassConstructor = GrassMicroMaterial.class.getConstructor();
        Constructor<TopMicroMaterial> topConstructor = TopMicroMaterial.class.getConstructor(Block.class, int.class);
        assertTrue(Modifier.isPublic(grassConstructor.getModifiers()));
        assertTrue(Modifier.isPublic(topConstructor.getModifiers()));
        assertEquals(1, GrassMicroMaterial.class.getDeclaredConstructors().length);
        assertEquals(1, TopMicroMaterial.class.getDeclaredConstructors().length);

        Field sideIcon = GrassMicroMaterial.class.getDeclaredField("sideIconT");
        assertEquals(IconTransformation.class, sideIcon.getType());
        assertTrue(Modifier.isPrivate(sideIcon.getModifiers()));
        assertEquals(1, GrassMicroMaterial.class.getDeclaredFields().length);
        assertEquals(0, TopMicroMaterial.class.getDeclaredFields().length);

        assertTrue(Modifier.isPublic(TopMicroMaterial$.class.getModifiers()));
        assertTrue(Modifier.isFinal(TopMicroMaterial$.class.getModifiers()));
        assertEquals(1, publicDeclaredMethodSignatures(TopMicroMaterial$.class).size());
        assertTrue(publicDeclaredMethodSignatures(TopMicroMaterial$.class).contains("$lessinit$greater$default$2()I"));
        Field module = TopMicroMaterial$.class.getField("MODULE$");
        assertSame(TopMicroMaterial$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(TopMicroMaterial$.MODULE$, module.get(null));
        assertEquals(0, TopMicroMaterial.$lessinit$greater$default$2());
        assertEquals(0, TopMicroMaterial$.MODULE$.$lessinit$greater$default$2());
    }

    @Test
    void keepsConstructionAndGrassOverlayState() {
        GrassMicroMaterial grass = new GrassMicroMaterial();
        assertSame(Blocks.grass, grass.block());
        assertEquals(0, grass.meta());
        assertNull(grass.sideIconT());

        IconTransformation overlay = new IconTransformation(null);
        grass.sideIconT_$eq(overlay);
        assertSame(overlay, grass.sideIconT());

        TopMicroMaterial top = new TopMicroMaterial(Blocks.mycelium, 7);
        assertSame(Blocks.mycelium, top.block());
        assertEquals(7, top.meta());
    }

    @Test
    void keepsRenderingMethodsAndOverlayAccessorsOnTheCommonSide() {
        for (Class<?> type : Arrays.asList(GrassMicroMaterial.class, TopMicroMaterial.class)) {
            for (Method method : type.getDeclaredMethods()) {
                assertNull(method.getAnnotation(SideOnly.class), method.toString());
            }
        }
        assertNull(GrassMicroMaterial.class.getDeclaredFields()[0].getAnnotation(SideOnly.class));
    }

    @Test
    void grassRoutesBottomTopAndSideFacesThroughTheSamePipelineInputs() throws Exception {
        GrassMicroMaterial grass = new GrassMicroMaterial() {

            @Override
            public int getColour(int pass) {
                return 0xFFFFFFFF;
            }
        };
        MultiIconTransformation base = new MultiIconTransformation((net.minecraft.util.IIcon) null);
        IconTransformation overlay = new IconTransformation(null);
        grass.icont_$eq(base);
        grass.sideIconT_$eq(overlay);
        Cuboid6 bounds = new Cuboid6(0, 0, 0, 1, 0.625, 1);

        List<CCRenderState.IVertexOperation> bottom = renderOperations(grass, 0, bounds);
        assertSame(base, uvOperation(bottom));
        assertFalse(hasColourMultiplier(bottom));

        List<CCRenderState.IVertexOperation> top = renderOperations(grass, 1, bounds);
        assertSame(base, uvOperation(top));
        assertTrue(hasColourMultiplier(top));

        List<CCRenderState.IVertexOperation> side = renderOperations(grass, 2, bounds);
        assertTransformationList(uvOperation(side), -0.375, overlay);
        assertTrue(hasColourMultiplier(side));
    }

    @Test
    void topMaterialUsesTheBaseUvOnHorizontalFacesAndHeightAdjustedUvOnSides() throws Exception {
        TopMicroMaterial top = new TopMicroMaterial(Blocks.mycelium, 0) {

            @Override
            public int getColour(int pass) {
                return 0xFFFFFFFF;
            }
        };
        MultiIconTransformation icons = new MultiIconTransformation((net.minecraft.util.IIcon) null);
        top.icont_$eq(icons);
        Cuboid6 bounds = new Cuboid6(0, 0, 0, 1, 0.375, 1);

        List<CCRenderState.IVertexOperation> horizontal = renderOperations(top, 1, bounds);
        assertSame(icons, uvOperation(horizontal));
        assertTrue(hasColourMultiplier(horizontal));

        List<CCRenderState.IVertexOperation> side = renderOperations(top, 4, bounds);
        assertTransformationList(uvOperation(side), -0.625, icons);
        assertTrue(hasColourMultiplier(side));
    }

    private static List<CCRenderState.IVertexOperation> renderOperations(BlockMicroMaterial material, int side,
            Cuboid6 bounds) throws Exception {
        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        BlockFace face = new BlockFace();
        face.side = side;
        state.model = face;
        state.firstVertexIndex = 0;
        state.lastVertexIndex = 0;

        material.renderMicroFace(new Vector3(), -1, bounds);

        Field operations = CCRenderPipeline.class.getDeclaredField("ops");
        operations.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<CCRenderState.IVertexOperation> result = new ArrayList<>(
                (List<CCRenderState.IVertexOperation>) operations.get(state.pipeline));
        return result;
    }

    private static UVTransformation uvOperation(List<CCRenderState.IVertexOperation> operations) {
        List<UVTransformation> transformations = new ArrayList<>();
        for (CCRenderState.IVertexOperation operation : operations) {
            if (operation instanceof UVTransformation) {
                transformations.add((UVTransformation) operation);
            }
        }
        assertEquals(1, transformations.size());
        return transformations.get(0);
    }

    private static boolean hasColourMultiplier(List<CCRenderState.IVertexOperation> operations) {
        for (CCRenderState.IVertexOperation operation : operations) {
            if (operation instanceof ColourMultiplier) {
                return true;
            }
        }
        return false;
    }

    private static void assertTransformationList(UVTransformation transformation, double expectedDv, Object tail)
            throws Exception {
        assertTrue(transformation instanceof UVTransformationList);
        Field transformations = UVTransformationList.class.getDeclaredField("transformations");
        transformations.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<UVTransformation> parts = (List<UVTransformation>) transformations.get(transformation);
        assertEquals(2, parts.size());
        assertTrue(parts.get(0) instanceof UVTranslation);
        UVTranslation translation = (UVTranslation) parts.get(0);
        assertEquals(0, translation.du);
        assertEquals(expectedDv, translation.dv);
        assertSame(tail, parts.get(1));
    }

    private static Set<String> publicDeclaredMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
