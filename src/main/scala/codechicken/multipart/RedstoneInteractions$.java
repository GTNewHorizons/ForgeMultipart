package codechicken.multipart;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import codechicken.lib.vec.Rotation;
import scala.collection.JavaConversions;
import scala.collection.immutable.Set;

/** Scala-compatible singleton carrying the redstone interaction implementation. */
public final class RedstoneInteractions$ {

    public static final RedstoneInteractions$ MODULE$ = new RedstoneInteractions$();

    private final int[] vanillaSideMap = { -2, -1, 0, 2, 3, 1 };
    private final int[] sideVanillaMap = { 1, 2, 5, 3, 4 };
    private final Set<Block> fullVanillaBlocks = JavaConversions.asScalaSet(
            new HashSet<>(
                    Arrays.asList(
                            Blocks.redstone_torch,
                            Blocks.unlit_redstone_torch,
                            Blocks.lever,
                            Blocks.stone_button,
                            Blocks.wooden_button,
                            Blocks.redstone_block)))
            .toSet();

    private RedstoneInteractions$() {}

    public int[] vanillaSideMap() {
        return vanillaSideMap;
    }

    public int[] sideVanillaMap() {
        return sideVanillaMap;
    }

    public Set<Block> fullVanillaBlocks() {
        return fullVanillaBlocks;
    }

    public int getPowerTo(TMultiPart part, int side) {
        TileMultipart tile = part.tile();
        return getPowerTo(
                tile.getWorldObj(),
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                side,
                ((IRedstoneTile) tile).openConnections(side) & connectionMask(part, side));
    }

    public int getPowerTo(World world, int x, int y, int z, int side, int mask) {
        return getPower(
                world,
                x + net.minecraft.util.Facing.offsetsXForSide[side],
                y + net.minecraft.util.Facing.offsetsYForSide[side],
                z + net.minecraft.util.Facing.offsetsZForSide[side],
                side ^ 1,
                mask);
    }

    public int getPower(World world, int x, int y, int z, int side, int mask) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IRedstoneConnector) {
            return ((IRedstoneConnector) tile).weakPowerLevel(side, mask);
        }

        Block block = world.getBlock(x, y, z);
        if (block instanceof IRedstoneConnectorBlock) {
            return ((IRedstoneConnectorBlock) block).weakPowerLevel(world, x, y, z, side, mask);
        }

        if ((vanillaConnectionMask(block, world, x, y, z, side, true) & mask) > 0) {
            int power = world.getIndirectPowerLevelTo(x, y, z, side ^ 1);
            if (power < 15 && Objects.equals(block, Blocks.redstone_wire)) {
                power = Math.max(power, world.getBlockMetadata(x, y, z));
            }
            return power;
        }
        return 0;
    }

    public int vanillaToSide(int vanillaSide) {
        return sideVanillaMap[vanillaSide + 1];
    }

    public int otherConnectionMask(IBlockAccess world, int x, int y, int z, int side, boolean power) {
        return getConnectionMask(
                world,
                x + net.minecraft.util.Facing.offsetsXForSide[side],
                y + net.minecraft.util.Facing.offsetsYForSide[side],
                z + net.minecraft.util.Facing.offsetsZForSide[side],
                side ^ 1,
                power);
    }

    public int connectionMask(TMultiPart part, int side) {
        if (!(part instanceof IRedstonePart) || !((IRedstonePart) part).canConnectRedstone(side)) {
            return 0;
        }
        if (part instanceof IFaceRedstonePart) {
            int face = ((IFaceRedstonePart) part).getFace();
            return (side & 6) == (face & 6) ? 0x10 : 1 << Rotation.rotationTo(side & 6, face);
        }
        if (part instanceof IMaskedRedstonePart) {
            return ((IMaskedRedstonePart) part).getConnectionMask(side);
        }
        return 0x1F;
    }

    public int getConnectionMask(IBlockAccess world, int x, int y, int z, int side, boolean power) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IRedstoneConnector) {
            return ((IRedstoneConnector) tile).getConnectionMask(side);
        }

        Block block = world.getBlock(x, y, z);
        if (block instanceof IRedstoneConnectorBlock) {
            return ((IRedstoneConnectorBlock) block).getConnectionMask(world, x, y, z, side);
        }
        return vanillaConnectionMask(block, world, x, y, z, side, power);
    }

    public int vanillaConnectionMask(Block block, IBlockAccess world, int x, int y, int z, int side, boolean power) {
        if (fullVanillaBlocks.contains(block)) {
            return 0x1F;
        }

        if (Objects.equals(block, Blocks.redstone_wire) || Objects.equals(block, Blocks.powered_comparator)
                || Objects.equals(block, Blocks.unpowered_comparator)) {
            return side == 0 ? 0 : power ? 0x1F : 4;
        }

        int vanillaSide = vanillaSideMap[side];
        if (Objects.equals(block, Blocks.powered_repeater) || Objects.equals(block, Blocks.unpowered_repeater)) {
            int metadata = world.getBlockMetadata(x, y, z) & 3;
            return vanillaSide == metadata || vanillaSide == Direction.rotateOpposite[metadata] ? power ? 0x1F : 4 : 0;
        }

        return power || block.canConnectRedstone(world, x, y, z, vanillaSide) ? 0x1F : 0;
    }
}
