package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import codechicken.microblock.CommonMicroblock;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClient;
import codechicken.microblock.MicroblockClient$class;
import codechicken.microblock.MicroblockGenerator;

class MicroblockTraitsFunctionalTest {

    @Test
    void generatedCommonTraitUsesMaterialShapeBoundsAndClass() {
        int material = MicroMaterialRegistry.materialID("minecraft:stone");
        Microblock part = MicroblockGenerator.create(FaceMicroClass$.MODULE$, material, false);
        CommonMicroblock common = (CommonMicroblock) part;
        assertFalse(part instanceof MicroblockClient);
        assertEquals(material, part.material());
        assertEquals(0, part.shape());
        for (int side = 0; side < 6; side++) {
            part.setShape(3, side);
            assertEquals(side, common.getSlot());
            assertEquals(1 << side, common.getSlotMask());
            assertSame(FaceMicroClass$.MODULE$, common.microClass());
            assertEquals(FaceMicroClass$.MODULE$.getClassId(), part.itemClassID());
            assertEquals(1, common.getPartialOcclusionBoxes().size());
            assertSame(part.getBounds(), common.getPartialOcclusionBoxes().get(0));
        }
    }

    @Test
    void absentMaterialReachesTheClientOnlyStoneLookupOnTheServer() {
        // getBrokenIcon only needs the Microblock base cast; a proxy would not satisfy that contract.
        MissingMaterialClient part = new MissingMaterialClient();
        for (int side : new int[] { -1, 0, 5, 12 }) {
            // Forge strips Block.getIcon on a dedicated server; reaching it proves the fallback was selected.
            NoSuchMethodError error = assertThrows(
                    NoSuchMethodError.class,
                    () -> MicroblockClient$class.getBrokenIcon(part, side));
            assertTrue(error.getMessage().contains("net.minecraft.block.Block.getIcon(II)"));
        }
        assertEquals(4, part.materialReads);
    }

    private static final class MissingMaterialClient extends Microblock implements MicroblockClient {

        int materialReads;

        MissingMaterialClient() {
            super(-1);
        }

        @Override
        public codechicken.microblock.MicroMaterialRegistry.IMicroMaterial getIMaterial() {
            materialReads++;
            return null;
        }

        @Override
        public codechicken.microblock.MicroblockClass microClass() {
            return FaceMicroClass$.MODULE$;
        }

        @Override
        public int itemClassID() {
            return 0;
        }

        @Override
        public codechicken.lib.vec.Cuboid6 getBounds() {
            return codechicken.lib.vec.Cuboid6.full;
        }

        @Override
        public net.minecraft.util.IIcon getBrokenIcon(int side) {
            return MicroblockClient$class.getBrokenIcon(this, side);
        }

        @Override
        public void render(codechicken.lib.vec.Vector3 pos, int pass) {}
    }
}
