package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.init.Blocks;

import org.junit.jupiter.api.Test;

import codechicken.microblock.GrassMicroMaterial;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.TopMicroMaterial;
import codechicken.microblock.TopMicroMaterial$;

class GrassMicroMaterialFunctionalTest {

    @Test
    void dedicatedServerKeepsTheCommonRenderingSurfaceAndRegisteredMaterials() throws Exception {
        GrassMicroMaterial grass = (GrassMicroMaterial) MicroMaterialRegistry.getMaterial("minecraft:grass");
        TopMicroMaterial mycelium = (TopMicroMaterial) MicroMaterialRegistry.getMaterial("minecraft:mycelium");
        assertSame(Blocks.grass, grass.block());
        assertEquals(0, grass.meta());
        assertEquals("minecraft:grass", grass.blockKey());
        assertSame(Blocks.mycelium, mycelium.block());
        assertEquals(0, mycelium.meta());
        assertEquals("minecraft:mycelium", mycelium.blockKey());

        assertEquals(
                new TreeSet<>(Arrays.asList("loadIcons", "renderMicroFace", "sideIconT", "sideIconT_$eq")),
                publicDeclaredMethodNames(GrassMicroMaterial.class));
        assertEquals(
                new TreeSet<>(Arrays.asList("$lessinit$greater$default$2", "renderMicroFace")),
                publicDeclaredMethodNames(TopMicroMaterial.class));
        assertEquals(
                new TreeSet<>(Arrays.asList("$lessinit$greater$default$2")),
                publicDeclaredMethodNames(TopMicroMaterial$.class));

        Field overlay = GrassMicroMaterial.class.getDeclaredField("sideIconT");
        assertFalse(Modifier.isStatic(overlay.getModifiers()));
        assertSame(TopMicroMaterial$.MODULE$, TopMicroMaterial$.class.getField("MODULE$").get(null));
        assertEquals(0, TopMicroMaterial.$lessinit$greater$default$2());
        assertTrue(TopMicroMaterial.class.isInstance(mycelium));
    }

    private static Set<String> publicDeclaredMethodNames(Class<?> type) {
        Set<String> names = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                names.add(method.getName());
            }
        }
        return names;
    }
}
