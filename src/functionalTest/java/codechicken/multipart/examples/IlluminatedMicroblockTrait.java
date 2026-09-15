package codechicken.multipart.examples;

import codechicken.lib.vec.Vector3;
import codechicken.microblock.Microblock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Register by name before loading this Java class; FMP transforms it into a runtime trait interface. */
public abstract class IlluminatedMicroblockTrait extends Microblock {

    public IlluminatedMicroblockTrait() {
        super(0);
    }

    @Override
    public boolean shouldRenderDynamic() {
        return true;
    }

    @Override
    public int getLightValue() {
        return IlluminatedMicroblockExample.lightValue(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderDynamic(Vector3 pos, float frame, int pass) {
        IlluminatedMicroblockExample.renderHalo(this, pass);
    }
}
