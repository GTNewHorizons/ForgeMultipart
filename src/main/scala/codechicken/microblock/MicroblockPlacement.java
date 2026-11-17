package codechicken.microblock;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.ControlKeyModifer;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.Tuple2;

public class MicroblockPlacement {

    private final EntityPlayer player;
    private final MovingObjectPosition hit;
    private final int size;
    private final int material;
    private final boolean checkMaterial;
    private final PlacementProperties pp;
    private final World world;
    private final MicroblockClass mcrClass;
    private final BlockCoord pos;
    private final Vector3 vhit;
    private final Tuple2<TileMultipart, Object> gtile;
    private final TileMultipart htile;
    private final int slot;
    private final int oslot;
    private final double d;
    private final boolean useOppMod;
    private final boolean oppMod;
    private final boolean internal;
    private final boolean doExpand;
    private final int side;

    public MicroblockPlacement(EntityPlayer player, MovingObjectPosition hit, int size, int material,
            boolean checkMaterial, PlacementProperties pp) {
        this.player = player;
        this.hit = hit;
        this.size = size;
        this.material = material;
        this.checkMaterial = checkMaterial;
        this.pp = pp;
        world = player.worldObj;
        mcrClass = pp.microClass();
        pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ);
        vhit = new Vector3(hit.hitVec).add(-pos.x, -pos.y, -pos.z);
        gtile = TileMultipart.getOrConvertTile2(world, pos);
        htile = gtile._1();
        slot = pp.placementGrid().getHitSlot(vhit, hit.sideHit);
        oslot = pp.opposite(slot, hit.sideHit);
        d = getHitDepth(vhit, hit.sideHit);
        useOppMod = pp.sneakOpposite(slot, hit.sideHit);
        oppMod = ControlKeyModifer.isControlDown(player);
        internal = d < 1 && htile != null;
        doExpand = internal && !(Boolean) gtile._2()
                && !player.isSneaking()
                && !(oppMod && useOppMod)
                && pp.expand(slot, hit.sideHit);
        side = hit.sideHit;
    }

    public EntityPlayer player() {
        return player;
    }

    public MovingObjectPosition hit() {
        return hit;
    }

    public int size() {
        return size;
    }

    public int material() {
        return material;
    }

    public boolean checkMaterial() {
        return checkMaterial;
    }

    public PlacementProperties pp() {
        return pp;
    }

    public World world() {
        return world;
    }

    public MicroblockClass mcrClass() {
        return mcrClass;
    }

    public BlockCoord pos() {
        return pos;
    }

    public Vector3 vhit() {
        return vhit;
    }

    public Tuple2<TileMultipart, Object> gtile() {
        return gtile;
    }

    public TileMultipart htile() {
        return htile;
    }

    public int slot() {
        return slot;
    }

    public int oslot() {
        return oslot;
    }

    public double d() {
        return d;
    }

    public boolean useOppMod() {
        return useOppMod;
    }

    public boolean oppMod() {
        return oppMod;
    }

    public boolean internal() {
        return internal;
    }

    public boolean doExpand() {
        return doExpand;
    }

    public int side() {
        return side;
    }

    public ExecutablePlacement apply() {
        ExecutablePlacement customPlacement = pp.customPlacement(this);
        if (customPlacement != null) {
            return customPlacement;
        }
        if (slot < 0) {
            return null;
        }

        if (doExpand) {
            Tuple2<Object, Object> data = ExtendedMOP.getData(hit);
            TMultiPart hitPart = htile.partList().apply((Integer) data._1());
            if (Objects.equals(hitPart.getType(), mcrClass.getName())) {
                CommonMicroblock commonPart = (CommonMicroblock) hitPart;
                Microblock microPart = (Microblock) hitPart;
                if (microPart.material() == material && microPart.getSize() + size < 8) {
                    return expand(commonPart);
                }
            }
        }

        if (internal) {
            if (d < 0.5 || !useOppMod) {
                ExecutablePlacement placement = internalPlacement(htile, slot);
                if (placement != null) {
                    if (!useOppMod || !oppMod) {
                        return placement;
                    }
                    return internalPlacement(htile, oslot);
                }
            }
            if (useOppMod && !oppMod) {
                return internalPlacement(htile, oslot);
            }
            return externalPlacement(slot);
        }

        if (!useOppMod || !oppMod) {
            return externalPlacement(slot);
        }
        return externalPlacement(oslot);
    }

    public ExecutablePlacement expand(CommonMicroblock part) {
        Microblock microPart = (Microblock) part;
        return expand(microPart, create(microPart.getSize() + size, part.getSlot(), microPart.material()));
    }

    public ExecutablePlacement expand(Microblock part, Microblock newPart) {
        BlockCoord partPos = new BlockCoord(part.tile());
        if (TileMultipart.checkNoEntityCollision(world, partPos, newPart)
                && part.tile().canReplacePart(part, newPart)) {
            return new ExpandingPlacement(partPos, newPart, part);
        }
        return null;
    }

    public ExecutablePlacement internalPlacement(TileMultipart tile, int targetSlot) {
        return internalPlacement(tile, create(size, targetSlot, material));
    }

    public ExecutablePlacement internalPlacement(TileMultipart tile, Microblock newPart) {
        BlockCoord tilePos = new BlockCoord(tile);
        if (TileMultipart.checkNoEntityCollision(world, tilePos, newPart) && tile.canAddPart(newPart)) {
            return new AdditionPlacement(tilePos, newPart);
        }
        return null;
    }

    public ExecutablePlacement externalPlacement(int targetSlot) {
        return externalPlacement(create(size, targetSlot, material));
    }

    public ExecutablePlacement externalPlacement(Microblock newPart) {
        BlockCoord targetPos = pos.copy().offset(side);
        if (TileMultipart.canPlacePart(world, targetPos, newPart)) {
            return new AdditionPlacement(targetPos, newPart);
        }
        return null;
    }

    public double getHitDepth(Vector3 localHit, int hitSide) {
        return localHit.copy().scalarProject(Rotation.axes[hitSide]) + (hitSide % 2 ^ 1);
    }

    public Microblock create(int partSize, int partSlot, int partMaterial) {
        Microblock part = mcrClass.create(world.isRemote, partMaterial);
        part.setShape(partSize, partSlot);
        return part;
    }
}
