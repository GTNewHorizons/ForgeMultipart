package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.CommonMicroClass;
import codechicken.microblock.CommonMicroblockClient;
import codechicken.microblock.CornerMicroClass;
import codechicken.microblock.CornerMicroClass$;
import codechicken.microblock.CornerMicroblock;
import codechicken.microblock.CornerPlacement;
import codechicken.microblock.CornerPlacement$;
import codechicken.microblock.CornerPlacementGrid$;
import codechicken.microblock.EdgeMicroClass$;
import codechicken.microblock.FaceMicroClass;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.FaceMicroblock;
import codechicken.microblock.FacePlacement$;
import codechicken.microblock.FacePlacementGrid$;
import codechicken.microblock.GrassMicroMaterial;
import codechicken.microblock.HollowMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.microblock.MissingMicroMaterial;
import codechicken.microblock.MissingMicroMaterial$;
import codechicken.microblock.PostMicroClass$;
import codechicken.microblock.TopMicroMaterial;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import codechicken.multipart.TFacePart;
import scala.Tuple2;

class DefaultContentFunctionalTest {

    @Test
    void registersTheExactBuiltInMicroblockClasses() throws Exception {
        CommonMicroClass[] classes = CommonMicroClass.classes();
        assertSame(FaceMicroClass$.MODULE$, classes[0]);
        assertSame(HollowMicroClass$.MODULE$, classes[1]);
        assertSame(CornerMicroClass$.MODULE$, classes[2]);
        assertSame(EdgeMicroClass$.MODULE$, classes[3]);
        for (int id = 4; id < classes.length; id++) {
            assertNull(classes[id], "Unexpected microblock class ID " + id);
        }

        assertEquals(0, FaceMicroClass$.MODULE$.getClassId());
        assertEquals(1, HollowMicroClass$.MODULE$.getClassId());
        assertEquals(2, CornerMicroClass$.MODULE$.getClassId());
        assertEquals(3, EdgeMicroClass$.MODULE$.getClassId());

        Map<String, IPartFactory2> factories = partFactories();
        Set<String> microblockTypes = new TreeSet<>();
        for (String type : factories.keySet()) {
            if (type.startsWith("mcr_")) {
                microblockTypes.add(type);
            }
        }
        assertEquals(
                new TreeSet<>(Arrays.asList("mcr_cnr", "mcr_edge", "mcr_face", "mcr_hllw", "mcr_post")),
                microblockTypes);
        assertSame(CornerMicroClass$.MODULE$, factories.get("mcr_cnr"));
        assertSame(EdgeMicroClass$.MODULE$, factories.get("mcr_edge"));
        assertSame(FaceMicroClass$.MODULE$, factories.get("mcr_face"));
        assertSame(HollowMicroClass$.MODULE$, factories.get("mcr_hllw"));
        assertSame(PostMicroClass$.MODULE$, factories.get("mcr_post"));
    }

    @Test
    void keepsFaceFactoryPlacementAndAllFortyTwoBounds() {
        FaceMicroClass$ factory = FaceMicroClass$.MODULE$;
        assertSame(factory, FacePlacement$.MODULE$.microClass());
        assertSame(FacePlacementGrid$.MODULE$, FacePlacement$.MODULE$.placementGrid());
        assertSame(factory.aBounds(), FaceMicroClass.aBounds());
        assertEquals("mcr_face", factory.getName());
        assertEquals(3, factory.itemSlot());
        assertEquals(1f, factory.getResistanceFactor());

        Cuboid6[] bounds = factory.aBounds();
        assertEquals(256, bounds.length);
        int populated = 0;
        for (int side = 0; side < 6; side++) {
            for (int size = 1; size < 8; size++) {
                Cuboid6 expected = expectedFaceBounds(side, size / 8d);
                Cuboid6 actual = bounds[size << 4 | side];
                assertCuboid(expected, actual);
                populated++;
            }
        }
        for (Cuboid6 bound : bounds) {
            if (bound != null) {
                populated--;
            }
        }
        assertEquals(0, populated, "Only the 6 sides by 7 thicknesses may be populated");

        Microblock generated = factory.create(false, 0);
        assertTrue(generated instanceof FaceMicroblock);
        assertTrue(generated instanceof TFacePart);
        for (int side = 0; side < 6; side++) {
            generated.setShape(3, side);
            FaceMicroblock face = (FaceMicroblock) generated;
            assertSame(bounds[3 << 4 | side], face.getBounds());
            assertEquals(side, face.getSlot());
            assertEquals(generated.getIMaterial().isSolid(), face.solid(side));
        }
    }

    @Test
    void keepsCornerFactoryPlacementAndAllFiftySixBounds() {
        CornerMicroClass$ factory = CornerMicroClass$.MODULE$;
        assertSame(factory, CornerPlacement$.MODULE$.microClass());
        assertSame(factory, CornerPlacement.microClass());
        assertSame(CornerPlacementGrid$.MODULE$, CornerPlacement$.MODULE$.placementGrid());
        assertSame(factory.aBounds(), CornerMicroClass.aBounds());
        assertEquals("mcr_cnr", factory.getName());
        assertEquals(CornerMicroblock.class, factory.baseTrait());
        assertEquals(CommonMicroblockClient.class, factory.clientTrait());
        assertEquals(7, factory.itemSlot());
        assertEquals(1f, factory.getResistanceFactor());

        Cuboid6[] bounds = factory.aBounds();
        assertEquals(256, bounds.length);
        int populated = 0;
        for (int corner = 0; corner < 8; corner++) {
            for (int size = 1; size < 8; size++) {
                Cuboid6 expected = expectedCornerBounds(corner, size / 8d);
                Cuboid6 actual = bounds[size << 4 | corner];
                assertCuboid(expected, actual);
                populated++;
            }
        }
        for (Cuboid6 bound : bounds) {
            if (bound != null) {
                populated--;
            }
        }
        assertEquals(0, populated, "Only the 8 corners by 7 sizes may be populated");

        Microblock generated = factory.create(false, 0);
        assertTrue(generated instanceof CornerMicroblock);
        for (int slot = 7; slot < 15; slot++) {
            generated.setShape(3, slot);
            CornerMicroblock corner = (CornerMicroblock) generated;
            assertSame(bounds[3 << 4 | slot - 7], corner.getBounds());
            assertEquals(slot - 7, generated.getShape());
            assertEquals(slot, corner.getSlot());
        }
    }

    @Test
    void registersTheExactOrderedBuiltInMaterialsAndRemaps() throws Exception {
        List<String> expectedNames = new ArrayList<>();
        Map<String, String> expectedRemaps = new HashMap<>();
        add(expectedNames, expectedRemaps, Blocks.stone, "minecraft:stone", 0);
        add(expectedNames, expectedRemaps, Blocks.dirt, "minecraft:dirt", 2);
        add(expectedNames, expectedRemaps, Blocks.cobblestone, "minecraft:cobblestone", 0);
        add(expectedNames, expectedRemaps, Blocks.planks, "minecraft:planks", 5);
        add(expectedNames, expectedRemaps, Blocks.log, "minecraft:log", 3);
        add(expectedNames, expectedRemaps, Blocks.log2, "minecraft:log2", 0, "tile.log2");
        add(expectedNames, expectedRemaps, Blocks.leaves, "minecraft:leaves", 3);
        add(expectedNames, expectedRemaps, Blocks.leaves2, "minecraft:leaves2", 0, "tile.leaves2");
        add(expectedNames, expectedRemaps, Blocks.sponge, "minecraft:sponge", 0);
        add(expectedNames, expectedRemaps, Blocks.glass, "minecraft:glass", 0);
        add(expectedNames, expectedRemaps, Blocks.lapis_block, "minecraft:lapis_block", 0);
        add(expectedNames, expectedRemaps, Blocks.sandstone, "minecraft:sandstone", 2);
        add(expectedNames, expectedRemaps, Blocks.wool, "minecraft:wool", 15);
        add(expectedNames, expectedRemaps, Blocks.gold_block, "minecraft:gold_block", 0);
        add(expectedNames, expectedRemaps, Blocks.iron_block, "minecraft:iron_block", 0);
        add(expectedNames, expectedRemaps, Blocks.brick_block, "minecraft:brick_block", 0);
        add(expectedNames, expectedRemaps, Blocks.bookshelf, "minecraft:bookshelf", 0);
        add(expectedNames, expectedRemaps, Blocks.mossy_cobblestone, "minecraft:mossy_cobblestone", 0);
        add(expectedNames, expectedRemaps, Blocks.obsidian, "minecraft:obsidian", 0);
        add(expectedNames, expectedRemaps, Blocks.diamond_block, "minecraft:diamond_block", 0);
        add(expectedNames, expectedRemaps, Blocks.ice, "minecraft:ice", 0);
        add(expectedNames, expectedRemaps, Blocks.snow, "minecraft:snow", 0);
        add(expectedNames, expectedRemaps, Blocks.clay, "minecraft:clay", 0);
        add(expectedNames, expectedRemaps, Blocks.netherrack, "minecraft:netherrack", 0);
        add(expectedNames, expectedRemaps, Blocks.soul_sand, "minecraft:soul_sand", 0);
        add(expectedNames, expectedRemaps, Blocks.glowstone, "minecraft:glowstone", 0);
        add(expectedNames, expectedRemaps, Blocks.stonebrick, "minecraft:stonebrick", 3);
        add(expectedNames, expectedRemaps, Blocks.nether_brick, "minecraft:nether_brick", 0);
        add(expectedNames, expectedRemaps, Blocks.end_stone, "minecraft:end_stone", 0);
        add(expectedNames, expectedRemaps, Blocks.emerald_block, "minecraft:emerald_block", 0);
        add(expectedNames, expectedRemaps, Blocks.redstone_block, "minecraft:redstone_block", 0);
        add(expectedNames, expectedRemaps, Blocks.quartz_block, "minecraft:quartz_block", 0);
        add(expectedNames, expectedRemaps, Blocks.stained_hardened_clay, "minecraft:stained_hardened_clay", 15);
        add(expectedNames, expectedRemaps, Blocks.hardened_clay, "minecraft:hardened_clay", 0);
        add(expectedNames, expectedRemaps, Blocks.coal_block, "minecraft:coal_block", 0);
        add(expectedNames, expectedRemaps, Blocks.packed_ice, "minecraft:packed_ice", 0);
        add(expectedNames, expectedRemaps, Blocks.stained_glass, "minecraft:stained_glass", 15);
        add(expectedNames, expectedRemaps, Blocks.grass, "minecraft:grass", 0);
        add(expectedNames, expectedRemaps, Blocks.mycelium, "minecraft:mycelium", 0);
        expectedNames.add(MissingMicroMaterial.key());
        Collections.sort(expectedNames);

        Tuple2<String, IMicroMaterial>[] idMap = MicroMaterialRegistry.getIdMap();
        List<String> actualNames = new ArrayList<>(idMap.length);
        for (Tuple2<String, IMicroMaterial> entry : idMap) {
            actualNames.add(entry._1());
            if ("minecraft:grass".equals(entry._1())) {
                assertEquals(GrassMicroMaterial.class, entry._2().getClass());
            } else if ("minecraft:mycelium".equals(entry._1())) {
                assertEquals(TopMicroMaterial.class, entry._2().getClass());
            } else if (MissingMicroMaterial.key().equals(entry._1())) {
                assertSame(MissingMicroMaterial$.MODULE$, entry._2());
            } else {
                assertEquals(BlockMicroMaterial.class, entry._2().getClass(), entry._1());
            }
        }

        assertEquals(103, expectedNames.size());
        assertEquals(expectedNames, actualNames);
        assertEquals(expectedRemaps, materialRemaps());
    }

    private static void add(List<String> names, Map<String, String> remaps, Block block, String registeredName,
            int maxMeta) {
        add(names, remaps, block, registeredName, maxMeta, block.getUnlocalizedName());
    }

    private static Cuboid6 expectedFaceBounds(int side, double thickness) {
        switch (side) {
            case 0:
                return new Cuboid6(0, 0, 0, 1, thickness, 1);
            case 1:
                return new Cuboid6(0, 1 - thickness, 0, 1, 1, 1);
            case 2:
                return new Cuboid6(0, 0, 0, 1, 1, thickness);
            case 3:
                return new Cuboid6(0, 0, 1 - thickness, 1, 1, 1);
            case 4:
                return new Cuboid6(0, 0, 0, thickness, 1, 1);
            case 5:
                return new Cuboid6(1 - thickness, 0, 0, 1, 1, 1);
            default:
                throw new AssertionError(side);
        }
    }

    private static Cuboid6 expectedCornerBounds(int corner, double size) {
        double minX = (corner & 4) == 0 ? 0 : 1 - size;
        double minY = (corner & 1) == 0 ? 0 : 1 - size;
        double minZ = (corner & 2) == 0 ? 0 : 1 - size;
        return new Cuboid6(minX, minY, minZ, minX + size, minY + size, minZ + size);
    }

    private static void assertCuboid(Cuboid6 expected, Cuboid6 actual) {
        assertEquals(expected.min.x, actual.min.x, 1e-12);
        assertEquals(expected.min.y, actual.min.y, 1e-12);
        assertEquals(expected.min.z, actual.min.z, 1e-12);
        assertEquals(expected.max.x, actual.max.x, 1e-12);
        assertEquals(expected.max.y, actual.max.y, 1e-12);
        assertEquals(expected.max.z, actual.max.z, 1e-12);
    }

    private static void add(List<String> names, Map<String, String> remaps, Block block, String registeredName,
            int maxMeta, String oldName) {
        for (int meta = 0; meta <= maxMeta; meta++) {
            String suffix = meta == 0 ? "" : "_" + meta;
            names.add(registeredName + suffix);
            remaps.put(oldName + suffix, registeredName + suffix);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, IPartFactory2> partFactories() throws Exception {
        Field field = MultiPartRegistry.class.getDeclaredField("typeMap");
        field.setAccessible(true);
        return (Map<String, IPartFactory2>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> materialRemaps() throws Exception {
        Field field = MicroMaterialRegistry.class.getDeclaredField("remap");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }
}
