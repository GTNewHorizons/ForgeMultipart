package codechicken.multipart.scalatraits;

import codechicken.lib.vec.Rotation;
import codechicken.multipart.IRedstonePart;
import codechicken.multipart.IRedstoneTile;
import codechicken.multipart.PartMap;
import codechicken.multipart.RedstoneInteractions;
import codechicken.multipart.TEdgePart;
import codechicken.multipart.TFacePart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.Iterator;
import scala.collection.Seq;

/** Mixin implementation for multipart redstone queries. */
public class TRedstoneTile extends TileMultipart implements IRedstoneTile {

    @Override
    @SuppressWarnings("unchecked")
    public int strongPowerLevel(int side) {
        int max = 0;
        Seq<TMultiPart> current = TRedstoneTileAccess.partList(this);
        if (!(current instanceof scala.collection.immutable.List)) {
            Iterator<TMultiPart> iterator = current.iterator();
            while (iterator.hasNext()) {
                TMultiPart part = iterator.next();
                if (part instanceof IRedstonePart) {
                    max = Math.max(max, ((IRedstonePart) part).strongPowerLevel(side));
                }
            }
            return max;
        }

        scala.collection.immutable.List<TMultiPart> parts = (scala.collection.immutable.List<TMultiPart>) current;
        while (!parts.isEmpty()) {
            TMultiPart part = parts.head();
            parts = (scala.collection.immutable.List<TMultiPart>) parts.tail();
            if (part instanceof IRedstonePart) {
                max = Math.max(max, ((IRedstonePart) part).strongPowerLevel(side));
            }
        }
        return max;
    }

    @Override
    public int openConnections(int side) {
        int mask = 0x10;
        for (int rotation = 0; rotation < 4; rotation++) {
            int edge = PartMap.edgeBetween(side, Rotation.rotateSide(side & 6, rotation));
            if (redstoneConductionE(edge)) {
                mask |= 1 << rotation;
            }
        }
        return mask & redstoneConductionF(side);
    }

    public int redstoneConductionF(int slot) {
        TMultiPart part = TRedstoneTileAccess.partMap(this, slot);
        return part == null ? 0x1F : ((TFacePart) part).redstoneConductionMap();
    }

    public boolean redstoneConductionE(int slot) {
        TMultiPart part = TRedstoneTileAccess.partMap(this, slot);
        return part == null || ((TEdgePart) part).conductsRedstone();
    }

    @Override
    public int weakPowerLevel(int side) {
        return weakPowerLevel(side, TRedstoneTileAccess.otherConnectionMask(this, side, true));
    }

    @Override
    public boolean canConnectRedstone(int side) {
        int multipartSide = RedstoneInteractions.vanillaToSide(side);
        return (getConnectionMask(multipartSide) & TRedstoneTileAccess.otherConnectionMask(this, multipartSide, false))
                > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getConnectionMask(int side) {
        int openMask = openConnections(side);
        int result = 0;
        Seq<TMultiPart> current = TRedstoneTileAccess.partList(this);
        if (!(current instanceof scala.collection.immutable.List)) {
            Iterator<TMultiPart> iterator = current.iterator();
            while (iterator.hasNext()) {
                result |= RedstoneInteractions.connectionMask(iterator.next(), side) & openMask;
            }
            return result;
        }

        scala.collection.immutable.List<TMultiPart> parts = (scala.collection.immutable.List<TMultiPart>) current;
        while (!parts.isEmpty()) {
            TMultiPart part = parts.head();
            parts = (scala.collection.immutable.List<TMultiPart>) parts.tail();
            result |= RedstoneInteractions.connectionMask(part, side) & openMask;
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int weakPowerLevel(int side, int mask) {
        int connectedMask = openConnections(side) & mask;
        int max = 0;
        Seq<TMultiPart> current = TRedstoneTileAccess.partList(this);
        if (!(current instanceof scala.collection.immutable.List)) {
            Iterator<TMultiPart> iterator = current.iterator();
            while (iterator.hasNext()) {
                TMultiPart part = iterator.next();
                if ((RedstoneInteractions.connectionMask(part, side) & connectedMask) > 0) {
                    max = Math.max(max, ((IRedstonePart) part).weakPowerLevel(side));
                }
            }
            return max;
        }

        scala.collection.immutable.List<TMultiPart> parts = (scala.collection.immutable.List<TMultiPart>) current;
        while (!parts.isEmpty()) {
            TMultiPart part = parts.head();
            parts = (scala.collection.immutable.List<TMultiPart>) parts.tail();
            if ((RedstoneInteractions.connectionMask(part, side) & connectedMask) > 0) {
                max = Math.max(max, ((IRedstonePart) part).weakPowerLevel(side));
            }
        }
        return max;
    }
}

/** Keeps inherited Minecraft field reads outside the generated trait transformer. */
final class TRedstoneTileAccess {

    private TRedstoneTileAccess() {}

    static Seq<TMultiPart> partList(IRedstoneTile redstone) {
        return ((TileMultipart) redstone).partList();
    }

    static TMultiPart partMap(IRedstoneTile redstone, int slot) {
        return ((TileMultipart) redstone).partMap(slot);
    }

    static int otherConnectionMask(IRedstoneTile redstone, int side, boolean power) {
        TileMultipart tile = (TileMultipart) redstone;
        return RedstoneInteractions
                .otherConnectionMask(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, side, power);
    }
}
