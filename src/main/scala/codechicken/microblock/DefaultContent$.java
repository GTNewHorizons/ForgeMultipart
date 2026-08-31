package codechicken.microblock;

import net.minecraft.init.Blocks;

import scala.collection.immutable.Range$;

public final class DefaultContent$ {

    public static final DefaultContent$ MODULE$ = new DefaultContent$();

    private DefaultContent$() {}

    public void load() {
        FaceMicroClass$.MODULE$.register(0);
        HollowMicroClass$.MODULE$.register(1);
        CornerMicroClass$.MODULE$.register(2);
        EdgeMicroClass$.MODULE$.register(3);
        PostMicroClass$.MODULE$.register();

        BlockMicroMaterial$ materials = BlockMicroMaterial$.MODULE$;
        materials.createAndRegister(Blocks.stone, 0);
        materials.createAndRegister(Blocks.dirt, Range$.MODULE$.inclusive(0, 2));
        materials.createAndRegister(Blocks.cobblestone, 0);
        materials.createAndRegister(Blocks.planks, Range$.MODULE$.inclusive(0, 5));
        materials.createAndRegister(Blocks.log, Range$.MODULE$.inclusive(0, 3));
        materials.createAndRegister(Blocks.log2, Range$.MODULE$.inclusive(0, 1), "tile.log2");
        materials.createAndRegister(Blocks.leaves, Range$.MODULE$.inclusive(0, 3));
        materials.createAndRegister(Blocks.leaves2, Range$.MODULE$.inclusive(0, 1), "tile.leaves2");
        materials.createAndRegister(Blocks.sponge, 0);
        materials.createAndRegister(Blocks.glass, 0);
        materials.createAndRegister(Blocks.lapis_block, 0);
        materials.createAndRegister(Blocks.sandstone, Range$.MODULE$.inclusive(0, 2));
        materials.createAndRegister(Blocks.wool, Range$.MODULE$.inclusive(0, 15));
        materials.createAndRegister(Blocks.gold_block, 0);
        materials.createAndRegister(Blocks.iron_block, 0);
        materials.createAndRegister(Blocks.brick_block, 0);
        materials.createAndRegister(Blocks.bookshelf, 0);
        materials.createAndRegister(Blocks.mossy_cobblestone, 0);
        materials.createAndRegister(Blocks.obsidian, 0);
        materials.createAndRegister(Blocks.diamond_block, 0);
        materials.createAndRegister(Blocks.ice, 0);
        materials.createAndRegister(Blocks.snow, 0);
        materials.createAndRegister(Blocks.clay, 0);
        materials.createAndRegister(Blocks.netherrack, 0);
        materials.createAndRegister(Blocks.soul_sand, 0);
        materials.createAndRegister(Blocks.glowstone, 0);
        materials.createAndRegister(Blocks.stonebrick, Range$.MODULE$.inclusive(0, 3));
        materials.createAndRegister(Blocks.nether_brick, 0);
        materials.createAndRegister(Blocks.end_stone, 0);
        materials.createAndRegister(Blocks.emerald_block, 0);
        materials.createAndRegister(Blocks.redstone_block, 0);
        materials.createAndRegister(Blocks.quartz_block, 0);
        materials.createAndRegister(Blocks.stained_hardened_clay, Range$.MODULE$.inclusive(0, 15));
        materials.createAndRegister(Blocks.hardened_clay, 0);
        materials.createAndRegister(Blocks.coal_block, 0);
        materials.createAndRegister(Blocks.packed_ice, 0);
        materials.createAndRegister(Blocks.stained_glass, Range$.MODULE$.inclusive(0, 15));

        MicroMaterialRegistry.remapName(materials.oldKey(Blocks.grass), materials.materialKey(Blocks.grass));
        MicroMaterialRegistry.registerMaterial(new GrassMicroMaterial(), materials.materialKey(Blocks.grass));
        MicroMaterialRegistry.remapName(materials.oldKey(Blocks.mycelium), materials.materialKey(Blocks.mycelium));
        MicroMaterialRegistry
                .registerMaterial(new TopMicroMaterial(Blocks.mycelium, 0), materials.materialKey(Blocks.mycelium));

        MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
    }
}
