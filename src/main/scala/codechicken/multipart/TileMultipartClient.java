package codechicken.multipart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import scala.collection.Iterator;
import scala.collection.Seq;

/** Client-side multipart render-cache mixin. */
public class TileMultipartClient extends TileMultipart {

    private transient AxisAlignedBB cachedRenderBounds;
    private transient TMultiPart[] staticCache;
    private transient TMultiPart[] dynamicCache;

    public transient boolean hasDynamicParts;

    public boolean hasDynamicParts() {
        return hasDynamicParts;
    }

    public void hasDynamicParts_$eq(boolean value) {
        hasDynamicParts = value;
    }

    public void updateRenderCache() {
        Seq<TMultiPart> parts = TileMultipartClientAccess.partList(this);
        if (parts != null) {
            List<TMultiPart> dynamicParts = new ArrayList<>();
            List<TMultiPart> staticParts = new ArrayList<>();
            Iterator<TMultiPart> iterator = parts.iterator();
            while (iterator.hasNext()) {
                TMultiPart part = iterator.next();
                if (part.doesTick() || part.shouldRenderDynamic()) {
                    dynamicParts.add(part);
                } else {
                    staticParts.add(part);
                }
            }

            TMultiPart[] statics = staticParts.toArray(new TMultiPart[staticParts.size()]);
            TMultiPart[] dynamics = dynamicParts.toArray(new TMultiPart[dynamicParts.size()]);
            Cuboid6 bounds = null;
            for (TMultiPart part : statics) {
                Cuboid6 partBounds = part.getRenderBounds();
                if (bounds == null) {
                    bounds = partBounds.copy();
                } else {
                    bounds.enclose(partBounds);
                }
            }
            for (TMultiPart part : dynamics) {
                Cuboid6 partBounds = part.getRenderBounds();
                if (bounds == null) {
                    bounds = partBounds.copy();
                } else {
                    bounds.enclose(partBounds);
                }
            }
            if (bounds == null) {
                bounds = Cuboid6.full;
            }

            bounds.add(TileMultipartClientAccess.position(this));
            cachedRenderBounds = bounds.toAABB();
            staticCache = statics;
            dynamicCache = dynamics;
            hasDynamicParts = dynamics.length > 0;
        } else {
            staticCache = new TMultiPart[0];
            dynamicCache = new TMultiPart[0];
            hasDynamicParts = false;
            cachedRenderBounds = TileMultipartClientAccess.fullBlockBounds(this);
        }
    }

    @Override
    public boolean renderStatic(IBlockAccess world, Vector3 vector, RenderBlocks renderer) {
        if (staticCache == null) {
            updateRenderCache();
        }

        boolean rendered = false;
        TMultiPart[] statics = staticCache;
        if (statics != null) {
            for (TMultiPart part : statics) {
                rendered |= TileMultipartClientAccess.renderPart(part, world, vector, renderer);
            }
        }

        TMultiPart[] dynamics = dynamicCache;
        if (dynamics != null) {
            for (TMultiPart part : dynamics) {
                rendered |= TileMultipartClientAccess.renderPart(part, world, vector, renderer);
            }
        }
        return rendered;
    }

    @Override
    public void renderDynamic(Vector3 position, float frame, int pass) {
        if (!hasDynamicParts) {
            return;
        }

        TMultiPart[] dynamics = dynamicCache;
        if (dynamics != null) {
            for (TMultiPart part : dynamics) {
                if (part != null) {
                    part.renderDynamic(position, frame, pass);
                }
            }
        }
    }

    @Override
    public void randomDisplayTick(Random random) {}

    @Override
    public boolean shouldRenderInPass(int pass) {
        if (staticCache == null) {
            updateRenderCache();
        }
        return hasDynamicParts;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (cachedRenderBounds == null) {
            updateRenderCache();
        }
        return cachedRenderBounds;
    }

    @Override
    public void markRender() {
        super.markRender();
        updateRenderCache();
    }
}

/** Keeps inherited tile access and client rendering calls outside the generated trait transformer. */
final class TileMultipartClientAccess {

    private TileMultipartClientAccess() {}

    static Seq<TMultiPart> partList(Object tile) {
        return ((TileMultipart) tile).partList();
    }

    static Vector3 position(Object tile) {
        return Vector3.fromTileEntity((TileMultipart) tile);
    }

    static AxisAlignedBB fullBlockBounds(Object tile) {
        TileMultipart multipart = (TileMultipart) tile;
        return AxisAlignedBB.getBoundingBox(
                multipart.xCoord,
                multipart.yCoord,
                multipart.zCoord,
                multipart.xCoord + 1,
                multipart.yCoord + 1,
                multipart.zCoord + 1);
    }

    static boolean renderPart(TMultiPart part, IBlockAccess world, Vector3 vector, RenderBlocks renderer) {
        if (part == null) {
            return false;
        }
        if (part instanceof ISBRHPart) {
            return ((ISBRHPart) part).renderWorldBlock(world, (int) vector.x, (int) vector.y, (int) vector.z, renderer);
        }
        return part.renderStatic(vector, ForgeHooksClient.getWorldRenderPass());
    }
}
