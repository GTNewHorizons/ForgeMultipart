package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

class MissingMicroMaterialCharacterizationTest {

    private static final Set<String> PUBLIC_METHODS = new TreeSet<>(
            Arrays.asList(
                    "explosionResistance(Lnet/minecraft/entity/Entity;)F",
                    "getBreakingIcon(I)Lnet/minecraft/util/IIcon;",
                    "getCutterStrength()I",
                    "getItem()Lnet/minecraft/item/ItemStack;",
                    "getLightValue()I",
                    "getLocalizedName()Ljava/lang/String;",
                    "getSound()Lnet/minecraft/block/Block$SoundType;",
                    "getStrength(Lnet/minecraft/entity/player/EntityPlayer;)F",
                    "isTransparent()Z",
                    "key()Ljava/lang/String;",
                    "loadIcons()V",
                    "renderMicroFace(Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/vec/Cuboid6;)V"));

    private static final Set<String> CLIENT_METHODS = new TreeSet<>(
            Arrays.asList("getBreakingIcon", "loadIcons", "renderMicroFace"));

    @Test
    void keepsTheFacadeAndCompanionSurface() throws Exception {
        assertSingletonType(MissingMicroMaterial.class, true);
        assertSingletonType(MissingMicroMaterial$.class, false);

        assertArrayEquals(new Class<?>[0], MissingMicroMaterial.class.getInterfaces());
        assertArrayEquals(new Class<?>[] { IMicroMaterial.class }, MissingMicroMaterial$.class.getInterfaces());
        assertEquals(0, MissingMicroMaterial.class.getFields().length);

        Field module = MissingMicroMaterial$.class.getField("MODULE$");
        assertSame(MissingMicroMaterial$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MissingMicroMaterial$.MODULE$, module.get(null));
    }

    @Test
    void keepsClientOnlyMethodsOnBothSingletonTypes() {
        assertClientBoundaries(MissingMicroMaterial.class);
        assertClientBoundaries(MissingMicroMaterial$.class);
    }

    @Test
    void suppliesTheInertMissingMaterialValues() {
        IMicroMaterial missing = MissingMicroMaterial$.MODULE$;

        assertEquals("forgemicroblock:missing", MissingMicroMaterial.key());
        assertEquals(MissingMicroMaterial.key(), MissingMicroMaterial$.MODULE$.key());
        assertFalse(missing.isTransparent());
        assertTrue(missing.isSolid());
        assertTrue(missing.canRenderInPass(0));
        assertFalse(missing.canRenderInPass(1));
        assertEquals(0, missing.getLightValue());
        assertEquals(1f, missing.getStrength(null));
        assertEquals("Missing Material", missing.getLocalizedName());
        assertEquals(0, missing.getCutterStrength());
        assertSame(Block.soundTypeStone, missing.getSound());
        assertEquals(6f, missing.explosionResistance(null));

        ItemStack item = missing.getItem();
        assertNotNull(item);
        assertEquals(1, item.stackSize);
    }

    private static void assertSingletonType(Class<?> type, boolean staticMethods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertNull(type.getAnnotation(SideOnly.class));
        assertEquals(PUBLIC_METHODS, publicMethodSignatures(type));

        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertEquals(staticMethods, Modifier.isStatic(method.getModifiers()), method.toString());
            }
        }
    }

    private static void assertClientBoundaries(Class<?> type) {
        Set<String> annotated = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            SideOnly sideOnly = method.getAnnotation(SideOnly.class);
            if (sideOnly != null) {
                assertEquals(Side.CLIENT, sideOnly.value(), method.toString());
                annotated.add(method.getName());
            }
        }
        assertEquals(CLIENT_METHODS, annotated);
    }

    private static Set<String> publicMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
