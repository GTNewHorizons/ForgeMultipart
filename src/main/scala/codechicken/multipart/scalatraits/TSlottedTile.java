package codechicken.multipart.scalatraits;

import java.util.Arrays;
import java.util.Objects;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;

/**
 * Mixin implementation for slotted parts. Raw compiler input transformed into a runtime interface; consumer Java reads
 * slots through {@link TileMultipart#partMap(int)}. Do not compile field accesses against this class: the runtime
 * interface has accessor methods instead of fields. Existing binary accessor callers remain supported. Use
 * {@link TileMultipart#refreshPartSlots(TMultiPart)} after changing a stored part's mask; partMap is read-only.
 */
public class TSlottedTile extends TileMultipart {

    public TMultiPart[] v_partMap = new TMultiPart[27];

    @Override
    public void copyFrom(TileMultipart that) {
        super.copyFrom(that);
        if (that instanceof TSlottedTile) {
            v_partMap = ((TSlottedTile) that).v_partMap;
        }
    }

    @Override
    public TMultiPart partMap(int slot) {
        return v_partMap[slot];
    }

    @Override
    public void clearParts() {
        super.clearParts();
        Arrays.fill(v_partMap, null);
    }

    @Override
    public void partRemoved(TMultiPart part, int position) {
        super.partRemoved(part, position);
        if (part instanceof TSlottedPart) {
            for (int index = 0; index < 27; index++) {
                if (Objects.equals(partMap(index), part)) {
                    v_partMap[index] = null;
                }
            }
        }
    }

    @Override
    public boolean canAddPart(TMultiPart part) {
        if (part instanceof TSlottedPart) {
            int slotMask = ((TSlottedPart) part).getSlotMask();
            for (int index = 0; index < v_partMap.length; index++) {
                if ((slotMask & (1 << index)) != 0 && partMap(index) != null) {
                    return false;
                }
            }
        }
        return super.canAddPart(part);
    }

    @Override
    public void bindPart(TMultiPart part) {
        super.bindPart(part);
        if (part instanceof TSlottedPart) {
            int slotMask = ((TSlottedPart) part).getSlotMask();
            for (int index = 0; index < 27; index++) {
                if ((slotMask & (1 << index)) > 0) {
                    v_partMap[index] = part;
                }
            }
        }
    }

    @Override
    public void refreshPartSlots(TMultiPart part) {
        for (int index = 0; index < v_partMap.length; index++) {
            if (Objects.equals(v_partMap[index], part)) {
                v_partMap[index] = null;
            }
        }
        bindPart(part);
    }
}
