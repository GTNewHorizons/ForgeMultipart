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
import codechicken.multipart.TNormalOcclusion;

class HollowMicroblockCharacterizationTest {

    private static final Set<String> PLACEMENT_FACADE_METHODS = signatures(
            "customPlacement(Lcodechicken/microblock/MicroblockPlacement;)Lcodechicken/microblock/ExecutablePlacement;",
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/HollowMicroClass$;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/HollowPlacement$HollowPlacementGrid$;",
            "sneakOpposite(II)Z");
    private static final Set<String> PLACEMENT_COMPANION_METHODS = signatures(
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/HollowMicroClass$;",
            "microClass()Lcodechicken/microblock/MicroblockClass;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/HollowPlacement$HollowPlacementGrid$;",
            "placementGrid()Lcodechicken/microblock/PlacementGrid;",
            "sneakOpposite(II)Z");
    private static final Set<String> CLASS_FACADE_METHODS = signatures(
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
            "occBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "occBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "pBoxes()[Lscala/collection/Seq;",
            "pBoxes_$eq([Lscala/collection/Seq;)V",
            "placementProperties()Lcodechicken/microblock/HollowPlacement$;",
            "register()V",
            "register(I)V");
    private static final Set<String> CLASS_COMPANION_METHODS = signatures(
            "baseTrait()Ljava/lang/Class;",
            "clientTrait()Ljava/lang/Class;",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F",
            "itemSlot()I",
            "occBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "occBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "pBoxes()[Lscala/collection/Seq;",
            "pBoxes_$eq([Lscala/collection/Seq;)V",
            "placementProperties()Lcodechicken/microblock/HollowPlacement$;",
            "placementProperties()Lcodechicken/microblock/PlacementProperties;");

    @Test
    void keepsFacadesNestedGridCompanionAndGeneratedTraitSurfaces() throws Exception {
        assertFacade(HollowPlacement.class, PLACEMENT_FACADE_METHODS);
        assertCompanion(HollowPlacement$.class, PlacementProperties.class, PLACEMENT_COMPANION_METHODS, 1);

        Class<?> grid = HollowPlacement.HollowPlacementGrid$.class;
        assertTrue(Modifier.isPublic(grid.getModifiers()));
        assertFalse(Modifier.isFinal(grid.getModifiers()));
        assertSame(FaceEdgeGrid.class, grid.getSuperclass());
        assertEquals(0, publicDeclaredMethods(grid).size());
        Field gridModule = grid.getField("MODULE$");
        assertSame(grid, gridModule.getType());
        assertTrue(Modifier.isStatic(gridModule.getModifiers()));
        assertTrue(Modifier.isFinal(gridModule.getModifiers()));
        assertEquals(1, grid.getDeclaredFields().length);
        Constructor<?> constructor = grid.getConstructor();
        assertTrue(Modifier.isPublic(constructor.getModifiers()));

        assertFacade(HollowMicroClass.class, CLASS_FACADE_METHODS);
        assertCompanion(HollowMicroClass$.class, CommonMicroClass.class, CLASS_COMPANION_METHODS, 3);
        assertMutablePrivateArray(HollowMicroClass$.class.getDeclaredField("pBoxes"), scala.collection.Seq[].class);
        assertMutablePrivateArray(HollowMicroClass$.class.getDeclaredField("occBounds"), Cuboid6[].class);

        assertTrait(
                HollowMicroblock.class,
                new Class<?>[] { CommonMicroblock.class, TFacePart.class, TNormalOcclusion.class },
                signatures(
                        "allowCompleteOcclusion()Z",
                        "codechicken$microblock$HollowMicroblock$$super$occlusionTest(Lcodechicken/multipart/TMultiPart;)Z",
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "getCollisionBoxes()Ljava/util/List;",
                        "getHollowSize()I",
                        "getOcclusionBoxes()Ljava/lang/Iterable;",
                        "getPartialOcclusionBoxes()Ljava/util/List;",
                        "getSubParts()Ljava/util/List;",
                        "microClass()Lcodechicken/microblock/HollowMicroClass$;",
                        "occlusionTest(Lcodechicken/multipart/TMultiPart;)Z",
                        "redstoneConductionMap()I",
                        "solid(I)Z"));
        assertHelper(
                "codechicken.microblock.HollowMicroblock$class",
                signatures(
                        "$init$(Lcodechicken/microblock/HollowMicroblock;)V",
                        "allowCompleteOcclusion(Lcodechicken/microblock/HollowMicroblock;)Z",
                        "getBounds(Lcodechicken/microblock/HollowMicroblock;)Lcodechicken/lib/vec/Cuboid6;",
                        "getCollisionBoxes(Lcodechicken/microblock/HollowMicroblock;)Ljava/util/List;",
                        "getHollowSize(Lcodechicken/microblock/HollowMicroblock;)I",
                        "getOcclusionBoxes(Lcodechicken/microblock/HollowMicroblock;)Ljava/lang/Iterable;",
                        "getPartialOcclusionBoxes(Lcodechicken/microblock/HollowMicroblock;)Ljava/util/List;",
                        "getSubParts(Lcodechicken/microblock/HollowMicroblock;)Ljava/util/List;",
                        "microClass(Lcodechicken/microblock/HollowMicroblock;)Lcodechicken/microblock/HollowMicroClass$;",
                        "occlusionTest(Lcodechicken/microblock/HollowMicroblock;Lcodechicken/multipart/TMultiPart;)Z",
                        "redstoneConductionMap(Lcodechicken/microblock/HollowMicroblock;)I",
                        "solid(Lcodechicken/microblock/HollowMicroblock;I)Z"));

        assertTrait(
                HollowMicroblockClient.class,
                new Class<?>[] { HollowMicroblock.class, CommonMicroblockClient.class },
                signatures(
                        "codechicken$microblock$HollowMicroblockClient$$super$recalcBounds()V",
                        "drawBreaking(Lnet/minecraft/client/renderer/RenderBlocks;)V",
                        "drawHighlight(Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/entity/player/EntityPlayer;F)Z",
                        "recalcBounds()V",
                        "render(Lcodechicken/lib/vec/Vector3;I)V",
                        "renderHollow(Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/vec/Cuboid6;IZLscala/Function5;)V"));
        assertHelper(
                "codechicken.microblock.HollowMicroblockClient$class",
                signatures(
                        "$init$(Lcodechicken/microblock/HollowMicroblockClient;)V",
                        "drawBreaking(Lcodechicken/microblock/HollowMicroblockClient;Lnet/minecraft/client/renderer/RenderBlocks;)V",
                        "drawHighlight(Lcodechicken/microblock/HollowMicroblockClient;Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/entity/player/EntityPlayer;F)Z",
                        "recalcBounds(Lcodechicken/microblock/HollowMicroblockClient;)V",
                        "render(Lcodechicken/microblock/HollowMicroblockClient;Lcodechicken/lib/vec/Vector3;I)V",
                        "renderHollow(Lcodechicken/microblock/HollowMicroblockClient;Lcodechicken/lib/vec/Vector3;ILcodechicken/lib/vec/Cuboid6;IZLscala/Function5;)V"));
    }

    @Test
    void keepsPlacementRulesAndNestedGridIdentity() {
        HollowPlacement$ placement = HollowPlacement$.MODULE$;
        assertSame(HollowPlacement.HollowPlacementGrid$.MODULE$, placement.placementGrid());
        assertSame(HollowPlacement.HollowPlacementGrid$.MODULE$, HollowPlacement.placementGrid());
        assertNull(placement.customPlacement(null));
        assertNull(HollowPlacement.customPlacement(null));

        for (int slot = -3; slot < 27; slot++) {
            for (int side = 0; side < 6; side++) {
                assertEquals(slot ^ 1, placement.opposite(slot, side));
                assertEquals(slot ^ 1, HollowPlacement.opposite(slot, side));
                boolean opposite = slot == (side ^ 1);
                assertEquals(opposite, placement.sneakOpposite(slot, side));
                assertEquals(opposite, HollowPlacement.sneakOpposite(slot, side));
                assertEquals(opposite, placement.expand(slot, side));
                assertEquals(opposite, HollowPlacement.expand(slot, side));
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

    private static void assertCompanion(Class<?> type, Class<?> superclass, Set<String> methods, int fieldCount)
            throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
        Field module = type.getField("MODULE$");
        assertSame(type, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertEquals(fieldCount, type.getDeclaredFields().length);
    }

    private static void assertMutablePrivateArray(Field field, Class<?> type) {
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertFalse(Modifier.isFinal(field.getModifiers()));
    }

    private static void assertTrait(Class<?> type, Class<?>[] interfaces, Set<String> methods) {
        assertTrue(type.isInterface());
        assertArrayEquals(interfaces, type.getInterfaces());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertHelper(String name, Set<String> methods) throws Exception {
        Class<?> type = Class.forName(name, false, HollowMicroblockCharacterizationTest.class.getClassLoader());
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
