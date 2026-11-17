package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;
import codechicken.microblock.PostMicroClass$;
import codechicken.microblock.PostMicroblock;
import codechicken.microblock.PostMicroblock$class;
import codechicken.microblock.PostMicroblockClient;
import codechicken.microblock.PostMicroblockClient$class;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.mutable.ArrayBuffer;

class PostMicroblockClientFunctionalTest {

    @Test
    void realCoverAndPostBoundsSplitAndResetAcrossAllAxes() {
        int material = MicroMaterialRegistry.materialID("minecraft:stone");
        int[] longAxes = { 1, 2, 0 };
        for (int axis = 0; axis < 3; axis++) {
            ClientProbe part = new ClientProbe(material);
            assertNull(part.renderBounds1());
            assertNull(part.renderBounds2());
            part.setShape(2, axis);
            Microblock crossing = MicroblockGenerator.create(PostMicroClass$.MODULE$, material, false);
            crossing.setShape(4, (axis + 1) % 3);
            Microblock low = MicroblockGenerator.create(FaceMicroClass$.MODULE$, material, false);
            Microblock high = MicroblockGenerator.create(FaceMicroClass$.MODULE$, material, false);
            low.setShape(1, axis * 2);
            high.setShape(1, axis * 2 + 1);
            SlotTile tile = new SlotTile();
            tile.slots[axis * 2] = low;
            tile.slots[axis * 2 + 1] = high;
            ArrayBuffer<TMultiPart> parts = new ArrayBuffer<>();
            parts.$plus$eq(part);
            parts.$plus$eq(low);
            parts.$plus$eq(high);
            parts.$plus$eq(crossing);
            tile.partList_$eq(parts);
            part.bind(tile);
            Cuboid6 physical = part.getBounds().copy();
            part.onAdded();
            assertEquals(0.125, coordinate(part.renderBounds1().min, longAxes[axis]), 1e-12);
            assertEquals(0.25, coordinate(part.renderBounds1().max, longAxes[axis]), 1e-12);
            assertEquals(0.75, coordinate(part.renderBounds2().min, longAxes[axis]), 1e-12);
            // The second segment is copied from physical bounds, after face clipping of only the first segment.
            assertEquals(1, coordinate(part.renderBounds2().max, longAxes[axis]), 1e-12);
            assertBounds(physical, part.getBounds());
            parts.remove(3);
            part.onPartChanged(crossing);
            assertNull(part.renderBounds2());
            assertEquals(0.125, coordinate(part.renderBounds1().min, longAxes[axis]), 1e-12);
            assertEquals(0.875, coordinate(part.renderBounds1().max, longAxes[axis]), 1e-12);
            tile.slots[axis * 2] = null;
            tile.slots[axis * 2 + 1] = null;
            part.onPartChanged(low);
            assertBounds(physical, part.renderBounds1());
            assertNotSame(part.getBounds(), part.renderBounds1());
        }
    }

    private static void assertBounds(Cuboid6 expected, Cuboid6 actual) {
        assertArrayEquals(
                new double[] { expected.min.x, expected.min.y, expected.min.z, expected.max.x, expected.max.y,
                        expected.max.z },
                new double[] { actual.min.x, actual.min.y, actual.min.z, actual.max.x, actual.max.y, actual.max.z },
                1e-12);
    }

    private static double coordinate(Vector3 vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    private static final class SlotTile extends TileMultipart {

        final TMultiPart[] slots = new TMultiPart[6];

        @Override
        public TMultiPart partMap(int slot) {
            return slots[slot];
        }
    }

    /**
     * Executes client geometry with real Forge factories without requesting the stripped client factory entry point.
     */
    private static final class ClientProbe extends Microblock implements PostMicroblockClient {

        private Cuboid6 first;
        private Cuboid6 second;

        ClientProbe(int material) {
            super(material);
            PostMicroblockClient$class.$init$(this);
        }

        @Override
        public Cuboid6 renderBounds1() {
            return first;
        }

        @Override
        public void renderBounds1_$eq(Cuboid6 value) {
            first = value;
        }

        @Override
        public Cuboid6 renderBounds2() {
            return second;
        }

        @Override
        public void renderBounds2_$eq(Cuboid6 value) {
            second = value;
        }

        @Override
        public PostMicroClass$ microClass() {
            return PostMicroClass$.MODULE$;
        }

        @Override
        public Cuboid6 getBounds() {
            return PostMicroblock$class.getBounds(this);
        }

        @Override
        public List<Cuboid6> getOcclusionBoxes() {
            return PostMicroblock$class.getOcclusionBoxes(this);
        }

        @Override
        public List<Cuboid6> getPartialOcclusionBoxes() {
            return getOcclusionBoxes();
        }

        @Override
        public int itemClassID() {
            return PostMicroblock$class.itemClassID(this);
        }

        @Override
        public float getResistanceFactor() {
            return PostMicroblock$class.getResistanceFactor(this);
        }

        @Override
        public IIcon getBrokenIcon(int side) {
            throw new AssertionError("GPU icon lookup");
        }

        @Override
        public void render(Vector3 pos, int pass) {
            PostMicroblockClient$class.render(this, pos, pass);
        }

        @Override
        public void recalcBounds() {
            PostMicroblockClient$class.recalcBounds(this);
        }

        @Override
        public void shrinkFace(int side) {
            PostMicroblockClient$class.shrinkFace(this, side);
        }

        @Override
        public void shrinkPost(PostMicroblock post) {
            PostMicroblockClient$class.shrinkPost(this, post);
        }

        @Override
        public boolean thisShrinks(PostMicroblock post) {
            return PostMicroblockClient$class.thisShrinks(this, post);
        }

        @Override
        public void onAdded() {
            PostMicroblockClient$class.onAdded(this);
        }

        @Override
        public void onPartChanged(TMultiPart part) {
            PostMicroblockClient$class.onPartChanged(this, part);
        }

        public void codechicken$microblock$PostMicroblockClient$$super$onAdded() {
            super.onAdded();
        }

        public void codechicken$microblock$PostMicroblockClient$$super$read(MCDataInput packet) {
            super.read(packet);
        }

        public boolean codechicken$microblock$PostMicroblock$$super$occlusionTest(TMultiPart part) {
            return super.occlusionTest(part);
        }
    }
}
