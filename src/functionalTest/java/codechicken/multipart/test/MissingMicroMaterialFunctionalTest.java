package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MissingMicroMaterial;
import codechicken.microblock.MissingMicroMaterial$;

class MissingMicroMaterialFunctionalTest {

    private static final Set<String> CLIENT_METHODS = Arrays
            .stream(new String[] { "getBreakingIcon", "loadIcons", "renderMicroFace" }).collect(Collectors.toSet());

    @Test
    void dedicatedServerRegistersTheCompanionAfterStrippingClientState() {
        assertSame(MissingMicroMaterial$.MODULE$, MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()));
        assertSame(
                MissingMicroMaterial$.MODULE$,
                MicroMaterialRegistry.getMaterial(MicroMaterialRegistry.getMissingId()));

        ItemStack item = MissingMicroMaterial$.MODULE$.getItem();
        assertSame(Item.getItemFromBlock(Blocks.stone), item.getItem());
        assertEquals(1, item.stackSize);
        assertEquals(0, item.getItemDamage());

        assertFalse(publicMethodNames(MissingMicroMaterial.class).stream().anyMatch(CLIENT_METHODS::contains));
        assertFalse(publicMethodNames(MissingMicroMaterial$.class).stream().anyMatch(CLIENT_METHODS::contains));
        assertFalse(
                Arrays.stream(MissingMicroMaterial$.class.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers())).map(Field::getName)
                        .anyMatch("icon"::equals));
    }

    private static Set<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName).collect(Collectors.toSet());
    }
}
