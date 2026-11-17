package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.function.Function;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockClient;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry$;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import codechicken.multipart.examples.PartFactoryLookupExample;
import cpw.mods.fml.relauncher.ReflectionHelper;
import scala.Option;

class PartFactoryLookupFunctionalTest {

    @Test
    void javaFactoriesSupportMicroblockConstructionAndLoading() throws Exception {
        checkMicroblocks(MultiPartRegistry::getPartFactory);
        assertSame(
                MultiPartRegistry.getPartFactory("mcr_face"),
                MultiPartRegistry.class.getMethod("getPartFactory", String.class).invoke(null, "mcr_face"));
        assertNull(
                MultiPartRegistry.class.getMethod("getPartFactory", String.class).invoke(null, "test:no_such_factory"));
    }

    @Test
    void javaExampleRejectsUnsupportedTypesAndCreatesFreshUnboundMicroblocks() {
        int material = MicroMaterialRegistry.materialID("minecraft:stone");
        assertNull(PartFactoryLookupExample.createMicroblock("test:no_such_factory", false, material));
        assertNull(PartFactoryLookupExample.createMicroblock("mc_torch", false, material));
        Microblock part = PartFactoryLookupExample.createMicroblock("mcr_face", false, material);
        assertSame(MultiPartRegistry.getPartFactory("mcr_face"), part.microClass());
        assertEquals(material, part.material());
        assertFalse(part instanceof MicroblockClient);
        assertNull(part.tile());
        assertNotSame(part, PartFactoryLookupExample.createMicroblock("mcr_face", false, material));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reflectedFactoriesSupportMicroblockConstructionAndLoading() throws Exception {
        Field field = ReflectionHelper
                .findField(MultiPartRegistry$.class, "codechicken$multipart$MultiPartRegistry$$typeMap");
        scala.collection.mutable.Map<String, IPartFactory2> map = (scala.collection.mutable.Map<String, IPartFactory2>) field
                .get(MultiPartRegistry$.MODULE$);
        checkMicroblocks(name -> {
            Option<IPartFactory2> factory = map.get(name);
            return factory.isEmpty() ? null : factory.get();
        });
    }

    private static void checkMicroblocks(Function<String, IPartFactory2> lookup) throws Exception {
        assertNull(lookup.apply("test:no_such_factory"));
        assertFalse(lookup.apply("mc_torch") instanceof MicroblockClass);
        int material = MicroMaterialRegistry.materialID("minecraft:stone");
        for (String name : new String[] { "mcr_face", "mcr_hllw", "mcr_cnr", "mcr_edge", "mcr_post" }) {
            IPartFactory2 factory = lookup.apply(name);
            assertTrue(factory instanceof MicroblockClass);
            assertSame(factory, lookup.apply(new String(name)));
            Microblock part = (Microblock) MicroblockClass.class.getMethod("create", boolean.class, int.class)
                    .invoke(factory, false, material);
            assertSame(factory, part.microClass());
            assertEquals(name, part.getType());
            assertEquals(material, part.material());
            assertFalse(part instanceof MicroblockClient);
            assertNull(part.tile());
            assertNotSame(part, ((MicroblockClass) factory).create(false, material));
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", name);
            tag.setString("material", "minecraft:stone");
            tag.setByte("shape", (byte) 0x21);
            part.load(tag);
            assertEquals(0x21, part.shape());
            assertEquals(material, part.material());
            assertNull(part.tile(), "Factory construction and NBT load do not bind a part");
        }
    }
}
