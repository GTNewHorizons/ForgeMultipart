package codechicken.microblock;

import net.minecraft.block.Block;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.uv.UVTransformation;
import codechicken.lib.render.uv.UVTranslation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;

public class TopMicroMaterial extends BlockMicroMaterial {

    public static int $lessinit$greater$default$2() {
        return TopMicroMaterial$.MODULE$.$lessinit$greater$default$2();
    }

    public TopMicroMaterial(Block block, int meta) {
        super(block, meta);
    }

    @Override
    public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {
        BlockFace face = (BlockFace) CCRenderState.instance().model;
        UVTransformation icons = icont();
        if (face.side > 1) {
            icons = (UVTransformation) new UVTranslation(0, bounds.max.y - 1).$plus$plus(icons);
        }

        MaterialRenderHelper$.MODULE$.start(pos, pass, icons).blockColour(getColour(pass)).lighting()
                .blockAndMeta(block(), meta()).render();
    }
}
