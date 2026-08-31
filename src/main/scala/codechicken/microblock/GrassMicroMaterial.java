package codechicken.microblock;

import net.minecraft.block.BlockGrass;
import net.minecraft.init.Blocks;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.render.uv.UVTransformation;
import codechicken.lib.render.uv.UVTranslation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;

public class GrassMicroMaterial extends BlockMicroMaterial {

    private IconTransformation sideIconT;

    public GrassMicroMaterial() {
        super(Blocks.grass, 0);
    }

    public IconTransformation sideIconT() {
        return sideIconT;
    }

    public void sideIconT_$eq(IconTransformation sideIconT) {
        this.sideIconT = sideIconT;
    }

    @Override
    public void loadIcons() {
        super.loadIcons();
        sideIconT_$eq(new IconTransformation(BlockGrass.getIconSideOverlay()));
    }

    @Override
    public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {
        BlockFace face = (BlockFace) CCRenderState.instance().model;
        if (pass != -1) {
            face.computeLightCoords();
        }

        if (face.side == 1) {
            MaterialRenderHelper$.MODULE$.start(pos, pass, icont()).blockColour(getColour(pass)).lighting()
                    .blockAndMeta(block(), meta()).render();
        } else {
            MaterialRenderHelper$.MODULE$.start(pos, pass, icont()).lighting().render();
        }

        if (face.side > 1) {
            UVTransformation overlay = (UVTransformation) new UVTranslation(0, bounds.max.y - 1)
                    .$plus$plus(sideIconT());
            MaterialRenderHelper$.MODULE$.start(pos, pass, overlay).blockColour(getColour(pass)).lighting()
                    .blockAndMeta(block(), meta()).render();
        }
    }
}
