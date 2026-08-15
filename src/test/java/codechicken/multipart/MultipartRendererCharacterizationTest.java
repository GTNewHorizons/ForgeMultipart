package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.junit.jupiter.api.Test;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Everything this class does needs a client: a GL context, a tessellator and a live render pass. Nothing here can run
 * it. What can be pinned is the contract two other parties depend on and the ABI diff cannot see.
 * <p>
 * guidenh reflects on both {@code MultipartRenderer} and {@code MultipartRenderer$} by name, calling renderWorldBlock
 * as a static first and falling back to the companion's MODULE$. Forge needs the singleton to be both a
 * TileEntitySpecialRenderer and an ISimpleBlockRenderingHandler to accept it at registration.
 * <p>
 * Every lookup uses {@code initialize = false}. Initialising the companion runs its constructor, which claims a render
 * id from Forge's RenderingRegistry, and that has no business happening in a test.
 */
class MultipartRendererCharacterizationTest {

    private static final String RENDERER = "codechicken.multipart.MultipartRenderer";
    private static final String COMPANION = "codechicken.multipart.MultipartRenderer$";

    @Test
    void theStaticGuidenhCallsFirstKeepsItsExactSignature() throws Exception {
        Method renderWorldBlock = load(RENDERER).getDeclaredMethod(
                "renderWorldBlock",
                IBlockAccess.class,
                int.class,
                int.class,
                int.class,
                Block.class,
                int.class,
                RenderBlocks.class);

        assertSame(boolean.class, renderWorldBlock.getReturnType());
        assertTrue(Modifier.isPublic(renderWorldBlock.getModifiers()));
        assertTrue(Modifier.isStatic(renderWorldBlock.getModifiers()));
    }

    @Test
    void theRemainingRenderingCallbacksStayAvailableAsStatics() throws Exception {
        Class<?> renderer = load(RENDERER);

        assertStatic(renderer.getDeclaredMethod("getRenderId"), int.class);
        assertStatic(
                renderer.getDeclaredMethod(
                        "renderTileEntityAt",
                        TileEntity.class,
                        double.class,
                        double.class,
                        double.class,
                        float.class),
                void.class);
        assertStatic(
                renderer.getDeclaredMethod(
                        "renderInventoryBlock",
                        Block.class,
                        int.class,
                        int.class,
                        RenderBlocks.class),
                void.class);
        assertStatic(renderer.getDeclaredMethod("shouldRender3DInInventory", int.class), boolean.class);
    }

    /** guidenh's fallback path, and the only handle on the instance Forge registers. */
    @Test
    void theCompanionKeepsItsSingletonField() throws Exception {
        Class<?> companion = load(COMPANION);
        Field module = companion.getField("MODULE$");

        assertSame(companion, module.getType());
        assertTrue(Modifier.isPublic(module.getModifiers()));
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
    }

    /** Forge rejects the registration outright if either of these is missing. */
    @Test
    void theSingletonIsBothRendererKindsForgeRegisters() throws Exception {
        Class<?> companion = load(COMPANION);

        assertTrue(
                TileEntitySpecialRenderer.class.isAssignableFrom(companion),
                "bindTileEntitySpecialRenderer needs a TileEntitySpecialRenderer");
        assertTrue(
                ISimpleBlockRenderingHandler.class.isAssignableFrom(companion),
                "registerBlockHandler needs an ISimpleBlockRenderingHandler");
    }

    /** Both classes are stripped on a dedicated server, so both must carry the annotation, not just the companion. */
    @Test
    void bothClassesStayClientOnly() throws Exception {
        for (String name : new String[] { RENDERER, COMPANION }) {
            SideOnly sideOnly = load(name).getAnnotation(SideOnly.class);

            assertNotNull(sideOnly, name + " must stay @SideOnly");
            assertEquals(Side.CLIENT, sideOnly.value(), name);
        }
    }

    private static void assertStatic(Method method, Class<?> returnType) {
        assertSame(returnType, method.getReturnType(), method.getName());
        assertTrue(Modifier.isPublic(method.getModifiers()), method.getName());
        assertTrue(Modifier.isStatic(method.getModifiers()), method.getName());
    }

    private static Class<?> load(String name) throws Exception {
        return Class.forName(name, false, MultipartRendererCharacterizationTest.class.getClassLoader());
    }
}
