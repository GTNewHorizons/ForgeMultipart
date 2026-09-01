package codechicken.microblock;

import net.minecraft.block.Block;

import scala.collection.Iterator;
import scala.collection.Seq;

public final class BlockMicroMaterial$ {

    public static final BlockMicroMaterial$ MODULE$ = new BlockMicroMaterial$();

    private BlockMicroMaterial$() {}

    public int $lessinit$greater$default$2() {
        return 0;
    }

    public int createAndRegister$default$2() {
        return 0;
    }

    public String oldKey(Block block) {
        return block.getUnlocalizedName();
    }

    public String materialKey(Block block) {
        return (String) Block.blockRegistry.getNameForObject(block);
    }

    public String materialKey(String name, int meta) {
        return name + (meta > 0 ? "_" + meta : "");
    }

    public String materialKey(Block block, int meta) {
        return materialKey(materialKey(block), meta);
    }

    public void createAndRegister(Block block, int meta, String name) {
        MicroMaterialRegistry.registerMaterial(new BlockMicroMaterial(block, meta), materialKey(name, meta));
    }

    public void createAndRegister(Block block, int meta, String name, String oldName) {
        MicroMaterialRegistry.remapName(materialKey(oldName, meta), materialKey(name, meta));
        createAndRegister(block, meta, name);
    }

    public void createAndRegister(Block block, int meta) {
        createAndRegister(block, meta, materialKey(block), oldKey(block));
    }

    public void createAndRegister(Block block, Seq<Object> meta) {
        createAndRegister(block, meta, materialKey(block), oldKey(block));
    }

    public void createAndRegister(Block block, Seq<Object> meta, String oldName) {
        createAndRegister(block, 0, materialKey(block), oldName);
    }

    public void createAndRegister(Block block, Seq<Object> meta, String name, String oldName) {
        Iterator<Object> iterator = meta.iterator();
        while (iterator.hasNext()) {
            createAndRegister(block, (Integer) iterator.next(), name, oldName);
        }
    }
}
