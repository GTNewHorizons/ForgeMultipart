package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.microblock.handler.MicroblockProxy;

/** Loads a Scala consumer compiled against the reference ItemMicroPart$.MODULE$ ABI used by ProjRed. */
class ItemMicroPartBinaryCompatibilityTest {

    private static final String MATERIAL = "test:item_micro_part_binary";

    @BeforeAll
    static void registerMaterial() {
        if (MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
        }
        if (MicroMaterialRegistry.getMaterial(MATERIAL) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MATERIAL);
        }
        MicroMaterialRegistry$.MODULE$.setupIDMap();
    }

    @Test
    void referenceScalaConsumerStillLinksAgainstAllFourCompanionMethods() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(ItemMicroPart.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();
        ItemMicroPart previous = MicroblockProxy.itemMicro();
        ItemMicroPart item = new ItemMicroPart();
        MicroblockProxy.itemMicro_$eq(item);
        try {
            int id = MicroMaterialRegistry.materialID(MATERIAL);
            ItemStack numbered = (ItemStack) fixtureClass.getMethod("createById", int.class, int.class)
                    .invoke(fixture, 0x401, id);
            ItemStack named = (ItemStack) fixtureClass.getMethod("createByName", int.class, String.class)
                    .invoke(fixture, 0x202, MATERIAL);

            assertSame(item, numbered.getItem());
            assertEquals(MATERIAL, numbered.getTagCompound().getString("mat"));
            assertEquals(MATERIAL, named.getTagCompound().getString("mat"));
            assertSame(
                    MicroMaterialRegistry.getMaterial(MATERIAL),
                    fixtureClass.getMethod("material", ItemStack.class).invoke(fixture, named));
            assertEquals(id, fixtureClass.getMethod("materialId", ItemStack.class).invoke(fixture, named));
        } finally {
            MicroblockProxy.itemMicro_$eq(previous);
        }
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                ItemMicroPartBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaItemMicroPartConsumer.class.b64"));
        try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
            return Base64.getMimeDecoder().decode(scanner.next());
        }
    }

    private static final class FixtureClassLoader extends ClassLoader {

        private FixtureClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(byte[] bytecode) {
            return defineClass(null, bytecode, 0, bytecode.length);
        }
    }
}
