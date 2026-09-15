package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import codechicken.multipart.examples.CustomTileTraitCapability;
import codechicken.multipart.examples.CustomTileTraitExample;
import codechicken.multipart.examples.CustomTileTraitPart;

class CustomTileTraitFunctionalTest {

    @Test
    void registeredJavaTraitIsSelectedOnBothSidesAndExposesItsStableCapability() {
        for (boolean client : new boolean[] { false, true }) {
            ContributingPart first = new ContributingPart(4);
            ContributingPart second = new ContributingPart(7);
            List<TMultiPart> parts = Arrays.asList(first, new PlainPart(), second);

            TileMultipart tile = MultipartGenerator.generateCompositeTile(null, parts, client);

            assertEquals(client, tile instanceof TileMultipartClient);
            assertTrue(tile instanceof CustomTileTraitCapability);
            assertEquals(0, CustomTileTraitExample.totalContribution(tile), "Generation does not load parts");
            tile.loadPartList(parts);
            assertEquals(11, CustomTileTraitExample.totalContribution(tile));
            assertEquals(1, first.calls);
            assertEquals(1, second.calls);
            assertSame(tile, first.tile());
            assertSame(tile, second.tile());
            assertSame(tile, MultipartGenerator.generateCompositeTile(tile, parts, client));
            assertSame(
                    tile.getClass(),
                    MultipartGenerator.generateCompositeTile(null, Arrays.asList(new ContributingPart(1)), client)
                            .getClass());
        }

        assertFalse(new TileMultipart() instanceof CustomTileTraitCapability);
        assertEquals(0, CustomTileTraitExample.totalContribution(new TileMultipart()));
        assertEquals(0, CustomTileTraitExample.totalContribution(null));
    }

    private static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "custom_tile_trait:plain";
        }
    }

    private static final class ContributingPart extends PlainPart implements CustomTileTraitPart {

        private final int contribution;
        private int calls;

        private ContributingPart(int contribution) {
            this.contribution = contribution;
        }

        @Override
        public int tileContribution() {
            calls++;
            return contribution;
        }
    }
}
