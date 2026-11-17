package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

public final class MicroblockPlacement$ {

    public static final MicroblockPlacement$ MODULE$ = new MicroblockPlacement$();

    private MicroblockPlacement$() {}

    public ExecutablePlacement apply(EntityPlayer player, MovingObjectPosition hit, int size, int material,
            boolean checkMaterial, PlacementProperties properties) {
        return new MicroblockPlacement(player, hit, size, material, checkMaterial, properties).apply();
    }
}
