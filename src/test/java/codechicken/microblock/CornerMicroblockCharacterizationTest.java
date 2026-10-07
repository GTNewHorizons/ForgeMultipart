package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.Cuboid6;

class CornerMicroblockCharacterizationTest {

    private static final Set<String> PLACEMENT_FACADE_METHODS = signatures(
            "customPlacement(Lcodechicken/microblock/MicroblockPlacement;)Lcodechicken/microblock/ExecutablePlacement;",
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/CornerMicroClass$;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/CornerPlacementGrid$;",
            "sneakOpposite(II)Z");
    private static final Set<String> PLACEMENT_COMPANION_METHODS = signatures(
            "microClass()Lcodechicken/microblock/CornerMicroClass$;",
            "microClass()Lcodechicken/microblock/MicroblockClass;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/CornerPlacementGrid$;",
            "placementGrid()Lcodechicken/microblock/PlacementGrid;");
    private static final Set<String> CLASS_FACADE_METHODS = signatures(
            "aBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "aBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "baseTrait()Ljava/lang/Class;",
            "baseTraitId()I",
            "clientTrait()Ljava/lang/Class;",
            "clientTraitId()I",
            "create(ZI)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lcodechicken/lib/data/MCDataInput;)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)Lcodechicken/microblock/Microblock;",
            "getClassId()I",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F",
            "itemSlot()I",
            "placementProperties()Lcodechicken/microblock/CornerPlacement$;",
            "register()V",
            "register(I)V");
    private static final Set<String> CLASS_COMPANION_METHODS = signatures(
            "aBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "aBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "baseTrait()Ljava/lang/Class;",
            "clientTrait()Ljava/lang/Class;",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F",
            "itemSlot()I",
            "placementProperties()Lcodechicken/microblock/CornerPlacement$;",
            "placementProperties()Lcodechicken/microblock/PlacementProperties;");

    @Test
    void keepsFacadesCompanionsAndGeneratedTraitSurface() throws Exception {
        assertFacade(CornerPlacement.class, PLACEMENT_FACADE_METHODS);
        assertCompanion(CornerPlacement$.class, PlacementProperties.class, PLACEMENT_COMPANION_METHODS);
        assertEquals(1, CornerPlacement$.class.getDeclaredFields().length);

        assertFacade(CornerMicroClass.class, CLASS_FACADE_METHODS);
        assertCompanion(CornerMicroClass$.class, CommonMicroClass.class, CLASS_COMPANION_METHODS);
        Field bounds = CornerMicroClass$.class.getDeclaredField("aBounds");
        assertSame(Cuboid6[].class, bounds.getType());
        assertTrue(Modifier.isPrivate(bounds.getModifiers()));
        assertFalse(Modifier.isFinal(bounds.getModifiers()));
        assertEquals(2, CornerMicroClass$.class.getDeclaredFields().length);

        assertTrait(
                CornerMicroblock.class,
                new Class<?>[] { CommonMicroblock.class },
                signatures(
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "getSlot()I",
                        "microClass()Lcodechicken/microblock/CornerMicroClass$;",
                        "setShape(II)V"));
        assertHelper(
                "codechicken.microblock.CornerMicroblock$class",
                signatures(
                        "$init$(Lcodechicken/microblock/CornerMicroblock;)V",
                        "getBounds(Lcodechicken/microblock/CornerMicroblock;)Lcodechicken/lib/vec/Cuboid6;",
                        "getSlot(Lcodechicken/microblock/CornerMicroblock;)I",
                        "microClass(Lcodechicken/microblock/CornerMicroblock;)Lcodechicken/microblock/CornerMicroClass$;",
                        "setShape(Lcodechicken/microblock/CornerMicroblock;II)V"));
    }

    @Test
    void keepsCornerPlacementRulesAndGridIdentity() {
        CornerPlacement$ placement = CornerPlacement$.MODULE$;
        assertSame(CornerPlacementGrid$.MODULE$, placement.placementGrid());
        assertSame(CornerPlacementGrid$.MODULE$, CornerPlacement.placementGrid());
        assertNull(placement.customPlacement(null));
        assertNull(CornerPlacement.customPlacement(null));

        for (int slot = 7; slot < 15; slot++) {
            for (int side = 0; side < 6; side++) {
                int opposite = ((slot - 7) ^ (1 << (side >> 1))) + 7;
                assertEquals(opposite, placement.opposite(slot, side));
                assertEquals(opposite, CornerPlacement.opposite(slot, side));
                assertTrue(placement.sneakOpposite(slot, side));
                assertTrue(CornerPlacement.sneakOpposite(slot, side));
                assertTrue(placement.expand(slot, side));
                assertTrue(CornerPlacement.expand(slot, side));
            }
        }
    }

    private static void assertFacade(Class<?> type, Set<String> methods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(Object.class, type.getSuperclass());
        assertEquals(0, type.getDeclaredFields().length);
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertCompanion(Class<?> type, Class<?> superclass, Set<String> methods) throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
        Field module = type.getField("MODULE$");
        assertSame(type, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
    }

    private static void assertTrait(Class<?> type, Class<?>[] interfaces, Set<String> methods) {
        assertTrue(type.isInterface());
        assertArrayEquals(interfaces, type.getInterfaces());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertHelper(String name, Set<String> methods) throws Exception {
        Class<?> type = Class.forName(name, false, CornerMicroblockCharacterizationTest.class.getClassLoader());
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isAbstract(type.getModifiers()));
        assertSame(Object.class, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
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
}
