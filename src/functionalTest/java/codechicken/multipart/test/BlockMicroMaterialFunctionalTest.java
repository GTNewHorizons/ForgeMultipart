package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.junit.jupiter.api.Test;

import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.microblock.MissingMicroMaterial$;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.examples.MaterialAccessExample;
import scala.Tuple2;

class BlockMicroMaterialFunctionalTest {

    @Test
    void typedQueryUsesFirstUsableBlockAndGuideNhMetadataFormatting() {
        assertNull(MaterialAccessExample.resolvePrimaryMicroblockId(null));
        assertNull(MaterialAccessExample.resolvePrimaryMicroblockId(new TileEntity()));
        TileMultipart tile = new TileMultipart();
        assertNull(MaterialAccessExample.resolvePrimaryMicroblockId(tile));
        Microblock first = FaceMicroClass$.MODULE$.create(false, 0);
        Microblock second = FaceMicroClass$.MODULE$.create(false, 1);
        tile.setPartList(Arrays.asList(new MultipartGeneratorFunctionalTest.PlainPart(), first, second));
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> originalFirst = materials[0];
        Tuple2<String, IMicroMaterial> originalSecond = materials[1];
        try {
            materials[1] = new Tuple2<>("test:second", new BlockMicroMaterial(Blocks.stone, 0));
            for (int meta : new int[] { Integer.MIN_VALUE, -1, 0, 15, 17, Integer.MAX_VALUE }) {
                materials[0] = new Tuple2<>("test:alias", new BlockMicroMaterial(Blocks.wool, meta));
                assertEquals(
                        meta > 0 ? "minecraft:wool:" + meta : "minecraft:wool",
                        MaterialAccessExample.resolvePrimaryMicroblockId(tile));
            }
            for (IMicroMaterial skipped : new IMicroMaterial[] { MissingMicroMaterial$.MODULE$, null,
                    new BlockMicroMaterial(null, 0), new BlockMicroMaterial(Blocks.air, 0),
                    new BlockMicroMaterial(new Block(Material.rock) {}, 0) }) {
                materials[0] = new Tuple2<>("test:skipped", skipped);
                assertEquals("minecraft:stone", MaterialAccessExample.resolvePrimaryMicroblockId(tile));
            }
            materials[1] = materials[0];
            assertNull(MaterialAccessExample.resolvePrimaryMicroblockId(tile));
        } finally {
            materials[0] = originalFirst;
            materials[1] = originalSecond;
        }
    }

    @Test
    void typedQueryUsesVirtualAccessorsAndPropagatesFailuresWithoutSelectingALaterPart() {
        Microblock first = FaceMicroClass$.MODULE$.create(false, 0);
        Microblock second = FaceMicroClass$.MODULE$.create(false, 1);
        TileMultipart tile = new TileMultipart();
        tile.setPartList(Arrays.asList(first, second));
        Tuple2<String, IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        Tuple2<String, IMicroMaterial> original = materials[0];
        List<String> calls = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("material failure");
        try {
            materials[0] = new Tuple2<>("test:override", new BlockMicroMaterial(null, 3) {

                @Override
                public Block block() {
                    calls.add("block");
                    return Blocks.wool;
                }

                @Override
                public int meta() {
                    calls.add("meta");
                    return 23;
                }
            });
            assertEquals("minecraft:wool:23", MaterialAccessExample.resolvePrimaryMicroblockId(tile));
            assertEquals(Arrays.asList("block", "meta"), calls);
            materials[0] = new Tuple2<>("test:failure", new BlockMicroMaterial(null, 0) {

                @Override
                public int meta() {
                    throw failure;
                }
            });
            assertSame(
                    failure,
                    assertThrows(RuntimeException.class, () -> MaterialAccessExample.resolvePrimaryMicroblockId(tile)));
            first.material_$eq(Integer.MAX_VALUE);
            assertThrows(IndexOutOfBoundsException.class, () -> MaterialAccessExample.resolvePrimaryMicroblockId(tile));
        } finally {
            materials[0] = original;
        }
    }

    @Test
    void registeredMaterialsExposeBlockAndMetadataWithoutClientInitialization() throws Exception {
        int id = MicroMaterialRegistry.materialID("minecraft:stone");
        BlockMicroMaterial material = (BlockMicroMaterial) MicroMaterialRegistry.getMaterial(id);
        assertSame(MicroMaterialRegistry.getMaterial("minecraft:stone"), material);
        assertSame(Blocks.stone, material.block());
        assertEquals(0, material.meta());
        assertSame(Blocks.wool, new BlockMicroMaterial(Blocks.wool, 23).block());
        assertEquals(23, new BlockMicroMaterial(Blocks.wool, 23).meta());
        assertEquals("minecraft:stone", net.minecraft.block.Block.blockRegistry.getNameForObject(material.block()));
    }

    @Test
    void dedicatedServerKeepsCommonMaterialBehaviorAndStripsClientMembers() throws Exception {
        BlockMicroMaterial material = (BlockMicroMaterial) MicroMaterialRegistry.getMaterial("minecraft:stone");
        assertSame(Blocks.stone, material.block());
        assertEquals(0, material.meta());
        assertEquals("minecraft:stone", material.blockKey());

        ItemStack item = material.getItem();
        assertSame(Item.getItemFromBlock(Blocks.stone), item.getItem());
        assertEquals(1, item.stackSize);
        assertEquals(0, item.getItemDamage());
        assertEquals(item.getDisplayName(), material.getLocalizedName());
        assertFalse(material.isTransparent());
        assertTrue(new BlockMicroMaterial(Blocks.glass, 0).isTransparent());
        assertEquals(Blocks.glowstone.getLightValue(), new BlockMicroMaterial(Blocks.glowstone, 0).getLightValue());
        assertEquals(Blocks.stone.getHarvestLevel(0), material.getCutterStrength());
        assertSame(Blocks.stone.stepSound, material.getSound());
        assertEquals(Blocks.stone.getExplosionResistance(null), material.explosionResistance(null));
        WorldServer world = MinecraftServer.getServer().worldServers[0];
        EntityPlayer player = FakePlayerFactory.getMinecraft(world);
        float hardness = Blocks.stone.getBlockHardness(null, 0, 0, 0);
        float expected = player.getBreakSpeed(Blocks.stone, false, 0, 0, -1, 0) / hardness;
        assertEquals(expected, material.getStrength(player));

        assertThrows(NoSuchFieldException.class, () -> BlockMicroMaterial.class.getDeclaredField("icont"));
        assertThrows(NoSuchMethodException.class, () -> BlockMicroMaterial.class.getDeclaredMethod("loadIcons"));
        assertThrows(
                NoSuchMethodException.class,
                () -> BlockMicroMaterial.class.getDeclaredMethod("getBreakingIcon", int.class));
        assertSame(Blocks.stone, BlockMicroMaterial.class.getDeclaredMethod("block").invoke(material));
    }
}
