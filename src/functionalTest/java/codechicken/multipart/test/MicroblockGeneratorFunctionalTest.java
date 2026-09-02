package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.FaceMicroblock;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;
import scala.Tuple2;

class MicroblockGeneratorFunctionalTest {

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
