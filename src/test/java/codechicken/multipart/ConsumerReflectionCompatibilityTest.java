package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import net.minecraft.block.Block;

import org.junit.jupiter.api.Test;

import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.ItemSaw;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.multipart.minecraft.ButtonPart;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Source-only compatibility contracts used by optional integrations in the target pack. */
class ConsumerReflectionCompatibilityTest {

    @Test
    void guideNhMixinCanTargetBlockMicroMaterialFields() throws NoSuchFieldException {
        Field block = field(BlockMicroMaterial.class, "block", Block.class);
        Field meta = field(BlockMicroMaterial.class, "meta", int.class);

        assertFalse(Modifier.isStatic(block.getModifiers()));
        assertFalse(Modifier.isStatic(meta.getModifiers()));
    }

    @Test
    void etFuturumCanReplaceButtonOrientationArrays() {
        Field metaSideMap = ReflectionHelper.findField(ButtonPart.class, "metaSideMap");
        Field sideMetaMap = ReflectionHelper.findField(ButtonPart.class, "sideMetaMap");

        assertMutableStaticIntArray(metaSideMap);
        assertMutableStaticIntArray(sideMetaMap);
    }

    @Test
    void iguanaCanFindTheSawHarvestLevel() throws NoSuchFieldException {
        Field harvestLevel = field(ItemSaw.class, "harvestLevel", int.class);

        assertFalse(Modifier.isStatic(harvestLevel.getModifiers()));
    }

    @Test
    void galacticraftNameOnlyLookupFindsOneCompatibleRegistrationMethod() throws NoSuchMethodException {
        Method[] methods = Arrays.stream(MicroMaterialRegistry.class.getMethods())
                .filter(method -> method.getName().equals("registerMaterial")).toArray(Method[]::new);

        assertEquals(1, methods.length, "Galacticraft invokes the first public method with this name");
        assertTrue(Modifier.isStatic(methods[0].getModifiers()));
        assertArrayEquals(new Class<?>[] { IMicroMaterial.class, String.class }, methods[0].getParameterTypes());
        assertNotNull(BlockMicroMaterial.class.getConstructor(Block.class, int.class));
    }

    private static Field field(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        assertEquals(type, field.getType());
        return field;
    }

    private static void assertMutableStaticIntArray(Field field) {
        assertEquals(int[].class, field.getType());
        assertTrue(Modifier.isStatic(field.getModifiers()));
        assertFalse(Modifier.isFinal(field.getModifiers()));
    }
}
