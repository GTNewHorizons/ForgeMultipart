package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

/** Face behavior behind the retained Scala inheritance metadata and helper bridges. */
final class FaceMicroblockTraitLogic {

    private FaceMicroblockTraitLogic() {}

    static void render(FaceMicroblockClient part, Vector3 pos, int pass) {
        if (pass < 0) {
            MicroblockRender.renderCuboid(pos, ((Microblock) part).getIMaterial(), pass, part.getBounds(), 0);
        } else if (((Microblock) part).isTransparent()) {
            MicroblockRender.renderCuboid(
                    pos,
                    ((Microblock) part).getIMaterial(),
                    pass,
                    part.renderBounds(),
                    part.renderMask());
        } else {
            IMicroMaterial material = ((Microblock) part).getIMaterial();
            MicroblockRender
                    .renderCuboid(pos, material, pass, part.renderBounds(), part.renderMask() | 1 << part.getSlot());
            MicroblockRender.renderCuboid(pos, material, pass, Cuboid6.full, ~(1 << part.getSlot()));
        }
    }

    static Cuboid6 getBounds(FaceMicroblock part) {
        return FaceMicroClass.aBounds()[((Microblock) part).shape()];
    }

    static boolean solid(FaceMicroblock part, int side) {
        return ((Microblock) part).getIMaterial().isSolid();
    }
}
