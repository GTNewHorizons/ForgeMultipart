package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.scalatraits.TSlottedTile;
import scala.collection.immutable.Nil$;

/** Consumer-visible state and behavior of the generated slotted-tile trait. */
class TSlottedTileFunctionalTest {

    @Test
    void copyFromSharesACompatibleSlotArrayAndKeepsItForAPlainSource() throws Exception {
        TileMultipart source = newSlottedTile();
        TileMultipart target = newSlottedTile();
        TMultiPart[] sourceMap = new TMultiPart[27];
        sourceMap[4] = new PlainPart();
        setPartMap(source, sourceMap);

        target.copyFrom(source);
        assertSame(sourceMap, partMap(target));

        TMultiPart[] retained = new TMultiPart[27];
        setPartMap(target, retained);
        target.copyFrom(new TileMultipart());
        assertSame(retained, partMap(target));
    }

    @Test
    void externalArrayMutationCanBeRebuiltThroughBindPart() throws Exception {
        TileMultipart tile = newSlottedTile();
        SlotPart part = new SlotPart((1 << 1) | (1 << 5) | (1 << 26));

        tile.bindPart(part);
        TMultiPart[] slots = partMap(tile);
        assertSame(part, slots[1]);
        assertSame(part, slots[5]);
        assertSame(part, slots[26]);

        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == part) {
                slots[index] = null;
            }
        }
        tile.bindPart(part);
        assertSame(part, slots[1]);
        assertSame(part, slots[5]);
        assertSame(part, slots[26]);
    }

    @Test
    void partRemovedClearsOnlyMatchingSlotsForSlottedParts() throws Exception {
        TileMultipart tile = newSlottedTile();
        SlotPart removed = new SlotPart((1 << 1) | (1 << 5));
        SlotPart retained = new SlotPart(1 << 5);
        PlainPart plain = new PlainPart();
        tile.bindPart(removed);
        tile.bindPart(retained);
        partMap(tile)[9] = plain;

        tile.partRemoved(removed, 0);
        assertNull(tile.partMap(1));
        assertSame(retained, tile.partMap(5));
        assertSame(plain, tile.partMap(9));

        tile.partRemoved(plain, 0);
        assertSame(plain, tile.partMap(9));
    }

    @Test
    void partRemovedUsesScalaValueEqualityForTheStoredPart() throws Exception {
        TileMultipart tile = newSlottedTile();
        SlotPart stored = new SlotPart(1 << 4);
        tile.bindPart(stored);

        tile.partRemoved(new SlotPart(1 << 4), 0);

        assertNull(tile.partMap(4));
    }

    @Test
    void clearPartsClearsTheSuperclassListAndEverySlot() throws Exception {
        TileMultipart tile = newSlottedTile();
        SlotPart slotted = new SlotPart((1 << 0) | (1 << 26));
        tile.addPart_do(slotted);
        tile.addPart_do(new PlainPart());
        assertEquals(2, tile.jPartList().size());

        tile.clearParts();

        assertTrue(tile.jPartList().isEmpty());
        for (TMultiPart part : partMap(tile)) {
            assertNull(part);
        }
    }

    @Test
    void occupiedSlotsRejectBeforeOtherwiseDelegatingToTheBaseTile() {
        TileMultipart tile = newSlottedTile();
        tile.bindPart(new SlotPart((1 << 2) | (1 << 8)));

        assertFalse(tile.canAddPart(new SlotPart(1 << 8)));
        assertTrue(tile.canAddPart(new SlotPart(1 << 9)));
        assertTrue(tile.canAddPart(new PlainPart()));
    }

    private static TileMultipart newSlottedTile() {
        int traitId = MultipartMixinFactory.getId(TSlottedTile.class.getName().replace('.', '/'));
        BitSet traits = new BitSet();
        traits.set(traitId);
        return (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
    }

    private static TMultiPart[] partMap(TileMultipart tile) throws Exception {
        return (TMultiPart[]) tile.getClass().getMethod("v_partMap").invoke(tile);
    }

    private static void setPartMap(TileMultipart tile, TMultiPart[] map) throws Exception {
        tile.getClass().getMethod("v_partMap_$eq", TMultiPart[].class).invoke(tile, (Object) map);
    }

    private static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "slotted_test:plain";
        }
    }

    private static final class SlotPart extends PlainPart implements TSlottedPart {

        private final int slotMask;

        private SlotPart(int slotMask) {
            this.slotMask = slotMask;
        }

        @Override
        public int getSlotMask() {
            return slotMask;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SlotPart && slotMask == ((SlotPart) other).slotMask;
        }

        @Override
        public int hashCode() {
            return slotMask;
        }
    }
}
