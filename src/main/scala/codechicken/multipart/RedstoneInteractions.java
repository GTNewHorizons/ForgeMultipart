package codechicken.multipart;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import scala.collection.immutable.Set;

/** Static forwarders for Java and existing bytecode; implementation remains on {@link RedstoneInteractions$}. */
public final class RedstoneInteractions {

    private RedstoneInteractions() {}

    public static int[] vanillaSideMap() {
        return RedstoneInteractions$.MODULE$.vanillaSideMap();
    }

    public static int[] sideVanillaMap() {
        return RedstoneInteractions$.MODULE$.sideVanillaMap();
    }

    public static Set<Block> fullVanillaBlocks() {
        return RedstoneInteractions$.MODULE$.fullVanillaBlocks();
    }

    public static int getPowerTo(TMultiPart part, int side) {
        return RedstoneInteractions$.MODULE$.getPowerTo(part, side);
    }

    public static int getPowerTo(World world, int x, int y, int z, int side, int mask) {
        return RedstoneInteractions$.MODULE$.getPowerTo(world, x, y, z, side, mask);
    }

    public static int getPower(World world, int x, int y, int z, int side, int mask) {
        return RedstoneInteractions$.MODULE$.getPower(world, x, y, z, side, mask);
    }

    public static int vanillaToSide(int vanillaSide) {
        return RedstoneInteractions$.MODULE$.vanillaToSide(vanillaSide);
    }

    public static int otherConnectionMask(IBlockAccess world, int x, int y, int z, int side, boolean power) {
        return RedstoneInteractions$.MODULE$.otherConnectionMask(world, x, y, z, side, power);
    }

    public static int connectionMask(TMultiPart part, int side) {
        return RedstoneInteractions$.MODULE$.connectionMask(part, side);
    }

    public static int getConnectionMask(IBlockAccess world, int x, int y, int z, int side, boolean power) {
        return RedstoneInteractions$.MODULE$.getConnectionMask(world, x, y, z, side, power);
    }

    public static int vanillaConnectionMask(Block block, IBlockAccess world, int x, int y, int z, int side,
            boolean power) {
        return RedstoneInteractions$.MODULE$.vanillaConnectionMask(block, world, x, y, z, side, power);
    }
}
