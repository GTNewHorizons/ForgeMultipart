package codechicken.multipart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.vec.Cuboid6;
import scala.Tuple2;
import scala.collection.Iterator;

/** Block class for all multiparts, should be internal use only. */
public class BlockMultipart extends Block {

    public BlockMultipart() {
        super(new Material(MapColor.stoneColor));
    }

    public static TileMultipart getTile(IBlockAccess world, int x, int y, int z) {
        net.minecraft.tileentity.TileEntity t = world.getTileEntity(x, y, z);
        if (t instanceof TileMultipart && !((TileMultipart) t).partList().isEmpty()) {
            return (TileMultipart) t;
        }
        return null;
    }

    public static TileMultipartClient getClientTile(IBlockAccess world, int x, int y, int z) {
        net.minecraft.tileentity.TileEntity t = world.getTileEntity(x, y, z);
        if (t instanceof TileMultipartClient && !((TileMultipart) t).partList().isEmpty()) {
            return (TileMultipartClient) t;
        }
        return null;
    }

    /** Splits a hit into the index of the part it struck and a MOP rebased onto that part's own data. */
    public static Tuple2<Object, ExtendedMOP> reduceMOP(MovingObjectPosition hit) {
        ExtendedMOP ehit = (ExtendedMOP) hit;
        Tuple2<Object, Object> data = ExtendedMOP.getData(hit);
        return new Tuple2<>(data._1(), new ExtendedMOP(ehit, data._2(), ehit.dist));
    }

    public static boolean drawHighlight(World world, EntityPlayer player, MovingObjectPosition hit, float frame) {
        TileMultipart tile = getTile(world, hit.blockX, hit.blockY, hit.blockZ);
        if (tile == null) {
            return false;
        }

        Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
        int index = (Integer) reduced._1();
        if (tile.partList().apply(index).drawHighlight(reduced._2(), player, frame)) {
            return true;
        }

        tile.partList().apply(index).collisionRayTrace(RayTracer.getStartVec(player), RayTracer.getEndVec(player));
        return false;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile != null && tile.isSolid(side);
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return isBlockSolid(world, x, y, z, side.ordinal());
    }

    @Override
    public boolean canPlaceTorchOnTop(World world, int x, int y, int z) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile != null && tile.canPlaceTorchOnTop();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.onNeighborBlockChange();
        }
    }

    @Override
    public ExtendedMOP collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null ? null : tile.collisionRayTrace(start, end);
    }

    public Iterable<ExtendedMOP> rayTraceAll(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null ? java.util.Collections.<ExtendedMOP>emptyList() : tile.rayTraceAll(start, end);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        TileMultipart tile = getTile(world, x, y, z);

        if (hit == null || tile == null) {
            dropAndDestroy(world, x, y, z);
            return true;
        }

        Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
        int index = (Integer) reduced._1();
        if (world.isRemote) {
            tile.partList().apply(index).addDestroyEffects(reduced._2(), Minecraft.getMinecraft().effectRenderer);
            return true;
        }

        tile.harvestPart(index, reduced._2(), player);
        return world.getTileEntity(x, y, z) == null;
    }

    public void dropAndDestroy(World world, int x, int y, int z) {
        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null && !world.isRemote) {
            tile.dropItems(getDrops(world, x, y, z, 0, 0));
        }

        world.setBlockToAir(x, y, z);
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return 0;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ArrayList<ItemStack> ai = new ArrayList<>();
        if (world.isRemote) {
            return ai;
        }

        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null) {
            Iterator<TMultiPart> iterator = tile.partList().iterator();
            while (iterator.hasNext()) {
                TMultiPart part = iterator.next();
                for (ItemStack item : part.getDrops()) {
                    ai.add(item);
                }
            }
        }

        return ai;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB ebb, List list$,
            Entity entity) {
        List<AxisAlignedBB> list = list$;
        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null) {
            Iterator<TMultiPart> iterator = tile.partList().iterator();
            while (iterator.hasNext()) {
                TMultiPart part = iterator.next();
                for (Cuboid6 c : part.getCollisionBoxes()) {
                    AxisAlignedBB aabb = c.toAABB().offset(x, y, z);
                    if (aabb.intersectsWith(ebb)) {
                        list.add(aabb);
                    }
                }
            }
        }
    }

    @Override
    public boolean addHitEffects(World world, MovingObjectPosition hit, EffectRenderer effectRenderer) {
        TileMultipartClient tile = getClientTile(world, hit.blockX, hit.blockY, hit.blockZ);
        if (tile != null) {
            // The mixin transformer turns TileMultipartClient into an interface at runtime, so retain the cast in
            // bytecode even though the Java source model extends TileMultipart.
            TileMultipart parts = TileMultipart.class.cast(tile);
            Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
            int index = (Integer) reduced._1();
            if (index < parts.partList().size()) {
                parts.partList().apply(index).addHitEffects(reduced._2(), effectRenderer);
            }
        }

        return true;
    }

    @Override
    public boolean addDestroyEffects(World world, int x, int y, int z, int s, EffectRenderer effectRenderer) {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return TileMultipart.renderID();
    }

    @Override
    public boolean isAir(IBlockAccess world, int x, int y, int z) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null || tile.partList().isEmpty();
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return isAir(world, x, y, z);
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return true;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition hit, World world, int x, int y, int z) {
        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null) {
            if (!(hit instanceof ExtendedMOP)) {
                return null;
            }
            Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
            return tile.partList().apply((Integer) reduced._1()).pickItem(reduced._2());
        }
        return null;
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        TileMultipart tile = getTile(world, x, y, z);
        if (hit != null && tile != null) {
            Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
            return tile.partList().apply((Integer) reduced._1()).getStrength(reduced._2(), player) / 30f;
        }

        return 1 / 100f;
    }

    /** Kludge to set PROTECTED blockIcon to a blank icon. */
    @Override
    public void registerBlockIcons(IIconRegister register) {
        net.minecraft.util.IIcon icon = TextureUtils.getBlankIcon(16, register);
        setBlockTextureName(icon.getIconName());
        super.registerBlockIcons(register);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null ? 0 : tile.getLightValue();
    }

    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        TileMultipartClient tile = getClientTile(world, x, y, z);
        if (tile != null) {
            TileMultipart.class.cast(tile).randomDisplayTick(random);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
            float hitY, float hitZ) {
        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        if (hit == null) {
            return false;
        }

        TileMultipart tile = getTile(world, x, y, z);
        if (tile == null) {
            return false;
        }

        Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
        return tile.partList().apply((Integer) reduced._1()).activate(player, reduced._2(), player.getHeldItem());
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        if (hit == null) {
            return;
        }

        TileMultipart tile = getTile(world, x, y, z);
        if (tile == null) {
            return;
        }

        Tuple2<Object, ExtendedMOP> reduced = reduceMOP(hit);
        tile.partList().apply((Integer) reduced._1()).click(player, reduced._2(), player.getHeldItem());
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null ? 0 : tile.strongPowerLevel(side ^ 1);
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null ? 0 : tile.weakPowerLevel(side ^ 1);
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile != null && tile.canConnectRedstone(side);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.onEntityCollision(entity);
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
        TileMultipart tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.onNeighborTileChange(tileX, tileY, tileZ);
        }
    }

    @Override
    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX,
            double explosionY, double explosionZ) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile == null ? 0 : tile.getExplosionResistance(entity);
    }

    @Override
    public boolean getWeakChanges(IBlockAccess world, int x, int y, int z) {
        TileMultipart tile = getTile(world, x, y, z);
        return tile != null && tile.getWeakChanges();
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }
}
