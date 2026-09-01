package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.PartMap;
import codechicken.multipart.TEdgePart;
import codechicken.multipart.TNormalOcclusion;

class EdgeMicroblockCharacterizationTest {

    private static final Set<String> PLACEMENT_FACADE_METHODS = signatures(
            "customPlacement(Lcodechicken/microblock/MicroblockPlacement;)Lcodechicken/microblock/ExecutablePlacement;",
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/EdgeMicroClass$;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/EdgePlacementGrid$;",
            "sneakOpposite(II)Z");
    private static final Set<String> PLACEMENT_COMPANION_METHODS = signatures(
            "customPlacement(Lcodechicken/microblock/MicroblockPlacement;)Lcodechicken/microblock/ExecutablePlacement;",
            "microClass()Lcodechicken/microblock/EdgeMicroClass$;",
            "microClass()Lcodechicken/microblock/MicroblockClass;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/EdgePlacementGrid$;",
            "placementGrid()Lcodechicken/microblock/PlacementGrid;");
    private static final Set<String> EDGE_CLASS_FACADE_METHODS = signatures(
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
            "placementProperties()Lcodechicken/microblock/EdgePlacement$;",
            "register()V",
            "register(I)V");
    private static final Set<String> EDGE_CLASS_COMPANION_METHODS = signatures(
            "aBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "aBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "baseTrait()Ljava/lang/Class;",
            "clientTrait()Ljava/lang/Class;",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F",
            "itemSlot()I",
            "placementProperties()Lcodechicken/microblock/EdgePlacement$;",
            "placementProperties()Lcodechicken/microblock/PlacementProperties;");
    private static final Set<String> POST_CLASS_FACADE_METHODS = signatures(
            "aBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "aBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "baseTrait()Ljava/lang/Class;",
            "baseTraitId()I",
            "clientTrait()Ljava/lang/Class;",
            "clientTraitId()I",
            "create(ZI)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lcodechicken/lib/data/MCDataInput;)Lcodechicken/microblock/Microblock;",
            "createPart(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)Lcodechicken/microblock/Microblock;",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F",
            "register()V");
    private static final Set<String> POST_CLASS_COMPANION_METHODS = signatures(
            "aBounds()[Lcodechicken/lib/vec/Cuboid6;",
            "aBounds_$eq([Lcodechicken/lib/vec/Cuboid6;)V",
            "baseTrait()Ljava/lang/Class;",
            "clientTrait()Ljava/lang/Class;",
            "getName()Ljava/lang/String;",
            "getResistanceFactor()F");

    @Test
    void keepsAllFacadesCompanionsAndGeneratedTraitSurfaces() throws Exception {
        assertFacade(EdgePlacement.class, PLACEMENT_FACADE_METHODS);
        assertCompanion(EdgePlacement$.class, PlacementProperties.class, PLACEMENT_COMPANION_METHODS, false);

        assertFacade(EdgeMicroClass.class, EDGE_CLASS_FACADE_METHODS);
        assertCompanion(EdgeMicroClass$.class, CommonMicroClass.class, EDGE_CLASS_COMPANION_METHODS, true);

        assertTrait(
                EdgeMicroblock.class,
                new Class<?>[] { CommonMicroblock.class, TEdgePart.class },
                signatures(
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "getSlot()I",
                        "microClass()Lcodechicken/microblock/EdgeMicroClass$;",
                        "setShape(II)V"));
        assertHelper(
                "codechicken.microblock.EdgeMicroblock$class",
                signatures(
                        "$init$(Lcodechicken/microblock/EdgeMicroblock;)V",
                        "getBounds(Lcodechicken/microblock/EdgeMicroblock;)Lcodechicken/lib/vec/Cuboid6;",
                        "getSlot(Lcodechicken/microblock/EdgeMicroblock;)I",
                        "microClass(Lcodechicken/microblock/EdgeMicroblock;)Lcodechicken/microblock/EdgeMicroClass$;",
                        "setShape(Lcodechicken/microblock/EdgeMicroblock;II)V"));

        assertFacade(PostMicroClass.class, POST_CLASS_FACADE_METHODS);
        assertCompanion(PostMicroClass$.class, MicroblockClass.class, POST_CLASS_COMPANION_METHODS, true);

        assertTrait(
                PostMicroblock.class,
                new Class<?>[] { JPartialOcclusion.class, TNormalOcclusion.class },
                signatures(
                        "canPlaceTorchOnTop()Z",
                        "codechicken$microblock$PostMicroblock$$super$occlusionTest(Lcodechicken/multipart/TMultiPart;)Z",
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "getOcclusionBoxes()Ljava/util/List;",
                        "getPartialOcclusionBoxes()Ljava/util/List;",
                        "getResistanceFactor()F",
                        "itemClassID()I",
                        "microClass()Lcodechicken/microblock/PostMicroClass$;",
                        "occlusionTest(Lcodechicken/multipart/TMultiPart;)Z"));
        assertHelper(
                "codechicken.microblock.PostMicroblock$class",
                signatures(
                        "$init$(Lcodechicken/microblock/PostMicroblock;)V",
                        "canPlaceTorchOnTop(Lcodechicken/microblock/PostMicroblock;)Z",
                        "getBounds(Lcodechicken/microblock/PostMicroblock;)Lcodechicken/lib/vec/Cuboid6;",
                        "getOcclusionBoxes(Lcodechicken/microblock/PostMicroblock;)Ljava/util/List;",
                        "getPartialOcclusionBoxes(Lcodechicken/microblock/PostMicroblock;)Ljava/util/List;",
                        "getResistanceFactor(Lcodechicken/microblock/PostMicroblock;)F",
                        "itemClassID(Lcodechicken/microblock/PostMicroblock;)I",
                        "microClass(Lcodechicken/microblock/PostMicroblock;)Lcodechicken/microblock/PostMicroClass$;",
                        "occlusionTest(Lcodechicken/microblock/PostMicroblock;Lcodechicken/multipart/TMultiPart;)Z"));

        assertTrait(
                PostMicroblockClient.class,
                new Class<?>[] { PostMicroblock.class, MicroblockClient.class },
                signatures(
                        "codechicken$microblock$PostMicroblockClient$$super$onAdded()V",
                        "codechicken$microblock$PostMicroblockClient$$super$read(Lcodechicken/lib/data/MCDataInput;)V",
                        "onAdded()V",
                        "onPartChanged(Lcodechicken/multipart/TMultiPart;)V",
                        "read(Lcodechicken/lib/data/MCDataInput;)V",
                        "recalcBounds()V",
                        "render(Lcodechicken/lib/vec/Vector3;I)V",
                        "renderBounds1()Lcodechicken/lib/vec/Cuboid6;",
                        "renderBounds1_$eq(Lcodechicken/lib/vec/Cuboid6;)V",
                        "renderBounds2()Lcodechicken/lib/vec/Cuboid6;",
                        "renderBounds2_$eq(Lcodechicken/lib/vec/Cuboid6;)V",
                        "shrinkFace(I)V",
                        "shrinkPost(Lcodechicken/microblock/PostMicroblock;)V",
                        "thisShrinks(Lcodechicken/microblock/PostMicroblock;)Z"));
        assertHelper(
                "codechicken.microblock.PostMicroblockClient$class",
                signatures(
                        "$init$(Lcodechicken/microblock/PostMicroblockClient;)V",
                        "onAdded(Lcodechicken/microblock/PostMicroblockClient;)V",
                        "onPartChanged(Lcodechicken/microblock/PostMicroblockClient;Lcodechicken/multipart/TMultiPart;)V",
                        "read(Lcodechicken/microblock/PostMicroblockClient;Lcodechicken/lib/data/MCDataInput;)V",
                        "recalcBounds(Lcodechicken/microblock/PostMicroblockClient;)V",
                        "render(Lcodechicken/microblock/PostMicroblockClient;Lcodechicken/lib/vec/Vector3;I)V",
                        "shrinkFace(Lcodechicken/microblock/PostMicroblockClient;I)V",
                        "shrinkPost(Lcodechicken/microblock/PostMicroblockClient;Lcodechicken/microblock/PostMicroblock;)V",
                        "thisShrinks(Lcodechicken/microblock/PostMicroblockClient;Lcodechicken/microblock/PostMicroblock;)Z"));
    }

    @Test
    void keepsEdgePlacementRulesAndGridIdentity() {
        EdgePlacement$ placement = EdgePlacement$.MODULE$;
        assertSame(EdgePlacementGrid$.MODULE$, placement.placementGrid());
        assertSame(EdgePlacementGrid$.MODULE$, EdgePlacement.placementGrid());

        for (int slot = -3; slot < 0; slot++) {
            for (int side = 0; side < 6; side++) {
                assertEquals(slot, placement.opposite(slot, side));
                assertEquals(slot, EdgePlacement.opposite(slot, side));
            }
        }
        for (int slot = 15; slot < 27; slot++) {
            for (int side = 0; side < 6; side++) {
                int edge = slot - 15;
                int opposite = 15 + PartMap.packEdgeBits(edge, PartMap.unpackEdgeBits(edge) ^ (1 << (side >> 1)));
                assertEquals(opposite, placement.opposite(slot, side));
                assertEquals(opposite, EdgePlacement.opposite(slot, side));
                assertTrue(placement.sneakOpposite(slot, side));
                assertTrue(EdgePlacement.sneakOpposite(slot, side));
                assertTrue(placement.expand(slot, side));
                assertTrue(EdgePlacement.expand(slot, side));
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

    private static void assertCompanion(Class<?> type, Class<?> superclass, Set<String> methods, boolean hasBounds)
            throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
        Field module = type.getField("MODULE$");
        assertSame(type, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        if (hasBounds) {
            Field bounds = type.getDeclaredField("aBounds");
            assertSame(Cuboid6[].class, bounds.getType());
            assertTrue(Modifier.isPrivate(bounds.getModifiers()));
            assertFalse(Modifier.isFinal(bounds.getModifiers()));
        }
        assertEquals(hasBounds ? 2 : 1, type.getDeclaredFields().length);
    }

    private static void assertTrait(Class<?> type, Class<?>[] interfaces, Set<String> methods) {
        assertTrue(type.isInterface());
        assertArrayEquals(interfaces, type.getInterfaces());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertHelper(String name, Set<String> methods) throws Exception {
        Class<?> type = Class.forName(name, false, EdgeMicroblockCharacterizationTest.class.getClassLoader());
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
