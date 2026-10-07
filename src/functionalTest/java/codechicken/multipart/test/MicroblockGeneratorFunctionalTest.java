package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import codechicken.microblock.CornerMicroClass$;
import codechicken.microblock.EdgeMicroClass$;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.FaceMicroblock;
import codechicken.microblock.HollowMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockGenerator;
import codechicken.microblock.MicroblockGenerator$;
import codechicken.microblock.PostMicroClass$;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.examples.MicroblockCreationExample;
import scala.Tuple2;

class MicroblockGeneratorFunctionalTest {

    @Test
    void externalMaterialTraitComposesWithEveryBuiltInShapeAndReusesClasses() throws Exception {
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> original = materials[0];
        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class, MicroblockGenerator.IGeneratedMaterial.class },
                (proxy, method, args) -> {
                    if ("addTraits".equals(method.getName())) {
                        ((BitSet) args[0]).set(ForgeMultipartFunctionalTestMod.externalScalaMicroblockTraitId);
                    }
                    return defaultValue(method.getReturnType());
                });
        materials[0] = new Tuple2<>(original._1(), material);
        try {
            Class<?> trait = Class.forName("codechicken.multipart.test.ExternalScalaMicroblockFixture");
            for (MicroblockClass factory : new MicroblockClass[] { FaceMicroClass$.MODULE$, HollowMicroClass$.MODULE$,
                    CornerMicroClass$.MODULE$, EdgeMicroClass$.MODULE$, PostMicroClass$.MODULE$ }) {
                Microblock first = MicroblockGenerator.create(factory, 0, false);
                Microblock second = MicroblockGenerator.create(factory, 0, false);
                assertSame(factory, first.microClass());
                assertSame(first.getClass(), second.getClass());
                assertNotSame(first, second);
                assertTrue(trait.isInstance(first));
                assertTrue(first.shouldRenderDynamic());
                assertEquals(41, first.getLightValue());
                assertEquals(0, first.material());
                assertNull(first.tile());
            }
        } finally {
            materials[0] = original;
            MicroblockGenerator.freshBitSet();
        }
    }

    @Test
    void javaExampleCapturesSourceShapeBeforeMaterialCallbacksRun() {
        Microblock source = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
        source.shape_$eq((byte) 0x25);
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> original = materials[0];
        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class, MicroblockGenerator.IGeneratedMaterial.class },
                (proxy, method, args) -> {
                    if ("addTraits".equals(method.getName())) source.shape_$eq((byte) 0x51);
                    return defaultValue(method.getReturnType());
                });
        materials[0] = new Tuple2<>(original._1(), material);
        try {
            Microblock created = MicroblockCreationExample.recreateForSide(source, false);
            assertEquals(0x25, created.shape());
            assertEquals(0x51, source.shape(), "Material callbacks can have side effects");
        } finally {
            materials[0] = original;
            MicroblockGenerator.freshBitSet();
        }
    }

    @Test
    void javaExamplePreservesEveryShapeByteWithoutCopyingBindingsOrChangingTheSource() {
        Microblock source = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
        TileMultipart sourceTile = new TileMultipart();
        source.bind(sourceTile);
        for (int shape = Byte.MIN_VALUE; shape <= Byte.MAX_VALUE; shape++) {
            source.shape_$eq((byte) shape);
            Microblock created = MicroblockCreationExample.recreateForSide(source, false);
            assertNotSame(source, created);
            assertSame(source.getClass(), created.getClass());
            assertSame(source.microClass(), created.microClass());
            assertEquals(source.material(), created.material());
            assertEquals((byte) shape, created.shape());
            assertNull(created.tile());
            assertSame(sourceTile, source.tile());
            assertEquals((byte) shape, source.shape());
        }
    }

    @Test
    void staticAndCompanionReflectionCreateFreshPartsBeforeCallerOwnedShapeLoading() throws Exception {
        MicroblockClass factory = FaceMicroClass$.MODULE$;
        int material = MicroMaterialRegistry.materialID("minecraft:stone");
        Microblock source = factory.create(false, material);
        source.shape_$eq((byte) 0x25);
        TileMultipart sourceTile = new TileMultipart();
        source.bind(sourceTile);
        for (Class<?> owner : new Class<?>[] { MicroblockGenerator.class, MicroblockGenerator$.class }) {
            Object receiver = owner == MicroblockGenerator.class ? null : MicroblockGenerator$.MODULE$;
            Microblock created = (Microblock) owner.getMethod("create", MicroblockClass.class, int.class, boolean.class)
                    .invoke(receiver, factory, material, false);
            assertNotSame(source, created);
            assertSame(source.getClass(), created.getClass());
            assertSame(factory, created.microClass());
            assertEquals(material, created.material());
            assertEquals(0, created.shape());
            assertNull(created.tile());
            created.shape_$eq(source.shape());
            assertEquals(0x25, created.shape());
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("material", "minecraft:stone");
            tag.setByte("shape", (byte) 0x31);
            created.load(tag);
            assertEquals(0x31, created.shape());
            assertEquals(material, created.material());
            assertNull(created.tile());
            assertSame(sourceTile, source.tile());
            assertEquals(0x25, source.shape());
        }
    }

    @Test
    void materialCallbackFailurePropagatesAndTheNextCreationClearsScratch() {
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> original = materials[0];
        IllegalStateException failure = new IllegalStateException("material trait failure");
        BitSet scratch = MicroblockGenerator.getBitSet();
        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class, MicroblockGenerator.IGeneratedMaterial.class },
                (proxy, method, args) -> {
                    if ("addTraits".equals(method.getName())) {
                        assertSame(scratch, args[0]);
                        assertSame(FaceMicroClass$.MODULE$, args[1]);
                        assertEquals(Boolean.FALSE, args[2]);
                        ((BitSet) args[0]).set(2048);
                        throw failure;
                    }
                    return defaultValue(method.getReturnType());
                });
        materials[0] = new Tuple2<>(original._1(), material);
        try {
            assertSame(
                    failure,
                    assertThrows(
                            IllegalStateException.class,
                            () -> MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false)));
            assertTrue(scratch.get(2048));
            assertSame(
                    failure,
                    assertThrows(
                            IllegalStateException.class,
                            () -> MicroblockGenerator$.MODULE$.create(FaceMicroClass$.MODULE$, 0, false)));
            materials[0] = original;
            Microblock created = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);
            assertFalse(scratch.get(2048));
            assertSame(FaceMicroClass$.MODULE$, created.microClass());
            assertNull(created.tile());
        } finally {
            materials[0] = original;
            MicroblockGenerator.freshBitSet();
        }
    }

    @Test
    void generatedMaterialAddsExternalScalaTraitBeforeConstruction() throws Exception {
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> original = materials[0];
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<BitSet> callbackBits = new AtomicReference<>();
        AtomicReference<BitSet> initialTraits = new AtomicReference<>();

        IMicroMaterial material = (IMicroMaterial) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IMicroMaterial.class, MicroblockGenerator.IGeneratedMaterial.class },
                (proxy, method, args) -> {
                    if ("addTraits".equals(method.getName())) {
                        calls.incrementAndGet();
                        BitSet traits = (BitSet) args[0];
                        callbackBits.set(traits);
                        initialTraits.set((BitSet) traits.clone());
                        assertSame(FaceMicroClass$.MODULE$, args[1]);
                        assertEquals(Boolean.FALSE, args[2]);
                        traits.set(ForgeMultipartFunctionalTestMod.externalScalaMicroblockTraitId);
                    }
                    return defaultValue(method.getReturnType());
                });

        BitSet scratch = MicroblockGenerator.freshBitSet();
        scratch.set(2048);
        materials[0] = new Tuple2<>(original._1(), material);
        try {
            Microblock generated = MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false);

            BitSet expectedInitial = new BitSet();
            expectedInitial.set(FaceMicroClass$.MODULE$.baseTraitId());
            assertEquals(1, calls.get());
            assertSame(scratch, callbackBits.get());
            assertEquals(expectedInitial, initialTraits.get());
            assertFalse(initialTraits.get().get(2048));
            assertTrue(generated instanceof FaceMicroblock);
            assertTrue(
                    Class.forName("codechicken.multipart.test.ExternalScalaMicroblockFixture").isInstance(generated));
            assertEquals(0, generated.material());
            assertTrue(generated.shouldRenderDynamic());
            assertEquals(41, generated.getLightValue());
        } finally {
            materials[0] = original;
            MicroblockGenerator.freshBitSet();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return (char) 0;
        return null;
    }
}
