package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.TFacePart;

class FaceMicroblockCharacterizationTest {

    private static final Set<String> PLACEMENT_FACADE_METHODS = signatures(
            "customPlacement(Lcodechicken/microblock/MicroblockPlacement;)Lcodechicken/microblock/ExecutablePlacement;",
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/FaceMicroClass$;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/FacePlacementGrid$;",
            "sneakOpposite(II)Z");
    private static final Set<String> PLACEMENT_COMPANION_METHODS = signatures(
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/FaceMicroClass$;",
            "microClass()Lcodechicken/microblock/MicroblockClass;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/FacePlacementGrid$;",
            "placementGrid()Lcodechicken/microblock/PlacementGrid;",
            "sneakOpposite(II)Z");
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
            "placementProperties()Lcodechicken/microblock/FacePlacement$;",
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
            "placementProperties()Lcodechicken/microblock/FacePlacement$;",
            "placementProperties()Lcodechicken/microblock/PlacementProperties;");

    @Test
    void keepsFacadesCompanionsAndGeneratedTraitSurfaces() throws Exception {
        assertFacade(FacePlacement.class, true, PLACEMENT_FACADE_METHODS);
        assertCompanion(FacePlacement$.class, PlacementProperties.class, PLACEMENT_COMPANION_METHODS);
        assertEquals(1, FacePlacement$.class.getDeclaredFields().length);

        assertFacade(FaceMicroClass.class, false, CLASS_FACADE_METHODS);
        Constructor<?> constructor = FaceMicroClass.class.getDeclaredConstructor();
        assertTrue(Modifier.isPublic(constructor.getModifiers()));
        assertEquals(1, FaceMicroClass.class.getDeclaredConstructors().length);
        assertCompanion(FaceMicroClass$.class, CommonMicroClass.class, CLASS_COMPANION_METHODS);
        Field bounds = FaceMicroClass$.class.getDeclaredField("aBounds");
        assertSame(Cuboid6[].class, bounds.getType());
        assertTrue(Modifier.isPrivate(bounds.getModifiers()));
        assertFalse(Modifier.isFinal(bounds.getModifiers()));
        assertEquals(2, FaceMicroClass$.class.getDeclaredFields().length);

        assertTrait(
                FaceMicroblock.class,
                new Class<?>[] { CommonMicroblock.class, TFacePart.class },
                signatures(
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "microClass()Lcodechicken/microblock/FaceMicroClass$;",
                        "solid(I)Z"));
        assertHelper(
                "codechicken.microblock.FaceMicroblock$class",
                signatures(
                        "$init$(Lcodechicken/microblock/FaceMicroblock;)V",
                        "getBounds(Lcodechicken/microblock/FaceMicroblock;)Lcodechicken/lib/vec/Cuboid6;",
                        "microClass(Lcodechicken/microblock/FaceMicroblock;)Lcodechicken/microblock/FaceMicroClass$;",
                        "solid(Lcodechicken/microblock/FaceMicroblock;I)Z"));

        assertTrait(
                FaceMicroblockClient.class,
                new Class<?>[] { CommonMicroblockClient.class },
                signatures("render(Lcodechicken/lib/vec/Vector3;I)V"));
        assertHelper(
                "codechicken.microblock.FaceMicroblockClient$class",
                signatures(
                        "$init$(Lcodechicken/microblock/FaceMicroblockClient;)V",
                        "render(Lcodechicken/microblock/FaceMicroblockClient;Lcodechicken/lib/vec/Vector3;I)V"));
    }

    @Test
    void keepsFacePlacementRulesAndGridIdentity() {
        FacePlacement$ placement = FacePlacement$.MODULE$;
        assertSame(FacePlacementGrid$.MODULE$, placement.placementGrid());
        assertSame(FacePlacementGrid$.MODULE$, FacePlacement.placementGrid());
        assertNull(placement.customPlacement(null));
        assertNull(FacePlacement.customPlacement(null));

        for (int slot = 0; slot < 6; slot++) {
            for (int side = 0; side < 6; side++) {
                boolean sneaking = slot == (side ^ 1);
                assertEquals(slot ^ 1, placement.opposite(slot, side));
                assertEquals(slot ^ 1, FacePlacement.opposite(slot, side));
                assertEquals(sneaking, placement.sneakOpposite(slot, side));
                assertEquals(sneaking, FacePlacement.sneakOpposite(slot, side));
                assertEquals(sneaking, placement.expand(slot, side));
                assertEquals(sneaking, FacePlacement.expand(slot, side));
            }
        }
    }

    private static void assertFacade(Class<?> type, boolean isFinal, Set<String> methods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertEquals(isFinal, Modifier.isFinal(type.getModifiers()));
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
        Class<?> type = Class.forName(name, false, FaceMicroblockCharacterizationTest.class.getClassLoader());
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
