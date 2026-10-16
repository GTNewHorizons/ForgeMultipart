package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.CornerMicroClass$;
import codechicken.microblock.EdgeMicroClass$;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.HollowMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockGenerator;
import codechicken.microblock.PostMicroClass$;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.examples.IlluminatedMicroMaterial;
import codechicken.multipart.examples.IlluminatedMicroblockExample;
import codechicken.multipart.examples.IlluminatedMicroblockTrait;

class IlluminatedMicroblockFunctionalTest {

    @Test
    void registeredJavaMaterialComposesWithEveryShapeAndKeepsPersistenceAndClassCaching() throws Exception {
        assertTrue(IlluminatedMicroblockTrait.class.isInterface());
        assertThrows(
                NoSuchMethodException.class,
                () -> IlluminatedMicroblockTrait.class
                        .getDeclaredMethod("renderDynamic", Vector3.class, float.class, int.class));
        for (int meta = 16; meta < 32; meta++) {
            IlluminatedMicroMaterial material = (IlluminatedMicroMaterial) MicroMaterialRegistry
                    .getMaterial(BlockMicroMaterial.materialKey(ForgeMultipartFunctionalTestMod.illuminatedLamp, meta));
            assertSame(ForgeMultipartFunctionalTestMod.illuminatedLamp, material.block());
            assertEquals(meta, material.meta());
            BitSet server = new BitSet();
            server.set(10000);
            BitSet client = (BitSet) server.clone();
            material.addTraits(server, FaceMicroClass$.MODULE$, false);
            material.addTraits(client, FaceMicroClass$.MODULE$, true);
            assertEquals(server, client);
            assertEquals(2, server.cardinality());
        }
        for (MicroblockClass factory : families()) {
            Microblock first = create(factory, 19);
            Microblock second = create(factory, 31);
            assertTrue(first instanceof IlluminatedMicroblockTrait);
            assertSame(factory, first.microClass());
            assertSame(first.getClass(), second.getClass());
            assertNotSame(first, second);
            assertTrue(first.shouldRenderDynamic());
            assertNull(first.tile());
            first.setShape(3, slot(factory));
            NBTTagCompound tag = new NBTTagCompound();
            first.save(tag);
            assertEquals(
                    BlockMicroMaterial.materialKey(ForgeMultipartFunctionalTestMod.illuminatedLamp, 19),
                    tag.getString("material"));
            second.load(tag);
            assertEquals(first.shape(), second.shape());
            assertEquals(first.material(), second.material());
        }
        List<Integer> seenMetadata = new ArrayList<>();
        Block lamp = new Block(Material.glass) {

            @Override
            public int getHarvestLevel(int metadata) {
                seenMetadata.add(metadata);
                return metadata;
            }
        };
        for (int meta = 16; meta < 32; meta++) {
            assertEquals(meta - 16, new IlluminatedMicroMaterial(lamp, meta, 0).getCutterStrength());
        }
        assertEquals(16, seenMetadata.size());
    }

    @Test
    void lightAggregationUsesOnlyTraitMembersWithReferenceRoundingAndNoDetachedFilter() {
        Microblock first = create(FaceMicroClass$.MODULE$, 16);
        Microblock second = create(CornerMicroClass$.MODULE$, 17);
        Microblock plain = FaceMicroClass$.MODULE$.create(false, MicroMaterialRegistry.materialID("minecraft:stone"));
        TileMultipart tile = new TileMultipart();
        first.bind(tile);
        first.setShape(1, 0);
        second.setShape(2, 7);
        tile.setPartList(Arrays.asList(first, plain, second));
        try {
            IlluminatedMicroblockExample.dimLampParts = () -> true;
            assertEquals(5, first.getLightValue());
            assertNull(second.tile(), "Detached siblings are deliberately included by jPartList traversal");
            second.setShape(7, 7);
            assertEquals(15, first.getLightValue());
            tile.setPartList(Arrays.asList(plain));
            assertEquals(0, first.getLightValue());
            IlluminatedMicroblockExample.dimLampParts = () -> false;
            assertEquals(0, first.getLightValue());
            tile.setPartList(Arrays.asList(second));
            assertEquals(15, first.getLightValue());
            first.bind(null);
            assertThrows(NullPointerException.class, first::getLightValue);
        } finally {
            IlluminatedMicroblockExample.dimLampParts = () -> false;
        }
    }

    @Test
    void haloGeometryCopiesCollisionBoundsAndKeepsHollowOpeningOnEverySide() {
        for (MicroblockClass factory : new MicroblockClass[] { FaceMicroClass$.MODULE$, CornerMicroClass$.MODULE$,
                EdgeMicroClass$.MODULE$, PostMicroClass$.MODULE$ }) {
            Microblock part = create(factory, 16);
            part.setShape(2, slot(factory));
            List<Cuboid6> original = new ArrayList<>();
            for (Cuboid6 box : part.getCollisionBoxes()) original.add(box.copy());
            List<Cuboid6> halo = IlluminatedMicroblockExample.haloBoxes(part);
            assertEquals(original.size(), halo.size());
            int i = 0;
            for (Cuboid6 box : part.getCollisionBoxes()) {
                assertBox(original.get(i), box);
                assertBox(original.get(i).copy().expand(0.025), halo.get(i));
                i++;
            }
        }
        Microblock hollow = create(HollowMicroClass$.MODULE$, 18);
        for (int side = 0; side < 6; side++) {
            hollow.setShape(2, side);
            List<Cuboid6> halo = IlluminatedMicroblockExample.haloBoxes(hollow);
            assertEquals(4, halo.size());
            // Default opening width is 8/16. These golden local bounds include the reference's trimmed middle strips.
            Cuboid6[] local = { new Cuboid6(-0.025, -0.025, -0.025, 1.025, 0.275, 0.275),
                    new Cuboid6(-0.025, -0.025, 0.725, 1.025, 0.275, 1.025),
                    new Cuboid6(-0.025, -0.025, 0.275, 0.275, 0.275, 0.725),
                    new Cuboid6(0.725, -0.025, 0.275, 1.025, 0.275, 0.725) };
            for (int i = 0; i < 4; i++) {
                assertBox(local[i].apply(Rotation.sideRotations[side].at(Vector3.center)), halo.get(i));
            }
        }
    }

    @Test
    void retainedClientMethodCompilesAndSubmitsHaloWithoutAClientOrGpu() throws Exception {
        // Exercise the actual compiled client body with a uniquely named compiler probe. This does not simulate a
        // physical
        // client.
        try {
            ClassNode input = ASMHelper.createClassNode(
                    Launch.classLoader.getClassBytes("codechicken.multipart.examples.IlluminatedMicroblockTrait"),
                    0);
            input.name = "codechicken/multipart/test/IlluminatedClientProbe";
            MethodNode render = input.methods.stream().map(m -> (MethodNode) m)
                    .filter(m -> m.name.equals("renderDynamic")).findFirst().get();
            assertTrue(render.visibleAnnotations.stream().anyMatch(a -> a.desc.endsWith("/SideOnly;")));
            render.visibleAnnotations.clear(); // Keep just this body for the headless compiler probe.
            MixinClassesFunctionalTest.COMPILER.registerJavaTrait(input);
            Class<?> composite = MixinClassesFunctionalTest.COMPILER.mixinClasses(
                    "codechicken/multipart/test/IlluminatedClientComposite",
                    "codechicken/microblock/Microblock",
                    MixinClassesFunctionalTest.seq("codechicken/microblock/FaceMicroblock", input.name));
            Microblock part = (Microblock) composite.getConstructor(int.class).newInstance(
                    MicroMaterialRegistry.materialID(
                            BlockMicroMaterial.materialKey(ForgeMultipartFunctionalTestMod.illuminatedLamp, 21)));
            part.setShape(2, 0);
            TileMultipart tile = new TileMultipart();
            tile.xCoord = 4;
            tile.yCoord = 5;
            tile.zCoord = 6;
            part.bind(tile);
            List<Cuboid6> submitted = new ArrayList<>();
            IlluminatedMicroblockExample.haloRenderer = (x, y, z, colour, bounds) -> {
                assertEquals(Arrays.asList(4, 5, 6, 5), Arrays.asList(x, y, z, colour));
                submitted.add(bounds);
            };
            java.lang.reflect.Method method = composite
                    .getDeclaredMethod("renderDynamic", Vector3.class, float.class, int.class);
            method.invoke(part, new Vector3(99, 99, 99), 0.5F, 1);
            assertTrue(submitted.isEmpty());
            method.invoke(part, new Vector3(99, 99, 99), 0.5F, 0);
            assertEquals(1, submitted.size());
            assertBox(IlluminatedMicroblockExample.haloBoxes(part).get(0), submitted.get(0));
        } finally {
            IlluminatedMicroblockExample.haloRenderer = null;
        }
    }

    private static int slot(MicroblockClass factory) {
        if (factory == CornerMicroClass$.MODULE$) return 7;
        if (factory == EdgeMicroClass$.MODULE$) return 15;
        return 0;
    }

    private static MicroblockClass[] families() {
        return new MicroblockClass[] { FaceMicroClass$.MODULE$, HollowMicroClass$.MODULE$, CornerMicroClass$.MODULE$,
                EdgeMicroClass$.MODULE$, PostMicroClass$.MODULE$ };
    }

    private static Microblock create(MicroblockClass factory, int meta) {
        return MicroblockGenerator.create(
                factory,
                MicroMaterialRegistry.materialID(
                        BlockMicroMaterial.materialKey(ForgeMultipartFunctionalTestMod.illuminatedLamp, meta)),
                false);
    }

    private static void assertBox(Cuboid6 expected, Cuboid6 actual) {
        assertEquals(expected.min.x, actual.min.x, 1e-9);
        assertEquals(expected.min.y, actual.min.y, 1e-9);
        assertEquals(expected.min.z, actual.min.z, 1e-9);
        assertEquals(expected.max.x, actual.max.x, 1e-9);
        assertEquals(expected.max.y, actual.max.y, 1e-9);
        assertEquals(expected.max.z, actual.max.z, 1e-9);
    }
}
