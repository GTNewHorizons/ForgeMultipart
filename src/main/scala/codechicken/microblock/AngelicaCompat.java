package codechicken.microblock;

import net.coderbot.iris.Iris;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;

import scala.Unit$;
import scala.runtime.BoxedUnit;

public class AngelicaCompat {

    public Object setShaderMaterialOverride(Block block, int meta) {
        try {
            if (Tessellator.instance instanceof CapturingTessellator) {
                Iris.setShaderMaterialOverride(block, meta);
            }
            return BoxedUnit.UNIT;
        } catch (ClassCastException ignored) {
            return Unit$.MODULE$;
        }
    }

    public Object resetShaderMaterialOverride() {
        try {
            if (Tessellator.instance instanceof CapturingTessellator) {
                Iris.resetShaderMaterialOverride();
            }
            return BoxedUnit.UNIT;
        } catch (ClassCastException ignored) {
            return Unit$.MODULE$;
        }
    }
}
