package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import codechicken.multipart.examples.RenderIdExample;

class TileMultipartRenderIdTest {

    @Test
    void javaAccessorsAndExampleShareAllLegacyReadersAndWriters() {
        int previous = TileMultipart.getRenderID();
        BlockMultipart block = new BlockMultipart();
        try {
            for (int value : new int[] { -1, 0, 37, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
                TileMultipart.setRenderID(value);
                assertEquals(value, TileMultipart.renderID());
                assertEquals(value, TileMultipart$.MODULE$.renderID());
                assertEquals(value, RenderIdExample.multipartRenderType());
                assertEquals(value, block.getRenderType());
                TileMultipart.renderID_$eq(~value);
                assertEquals(~value, TileMultipart.getRenderID());
                TileMultipart$.MODULE$.renderID_$eq(value);
                assertEquals(value, TileMultipart.getRenderID());
            }
        } finally {
            TileMultipart.setRenderID(previous);
        }
    }

    @Test
    void uninitializedRenderIdIsTheSharedMinusOneSentinel() {
        assertEquals(-1, TileMultipart.renderID());
        assertEquals(-1, TileMultipart$.MODULE$.renderID());
        assertEquals(-1, new BlockMultipart().getRenderType());
    }

    @Test
    void bothLegacySettersUpdateOneGlobalValueWithoutValidation() {
        int previous = TileMultipart.renderID();
        BlockMultipart first = new BlockMultipart();
        BlockMultipart second = new BlockMultipart();
        try {
            for (int value : new int[] { 0, 37, -1, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
                TileMultipart.renderID_$eq(value);
                assertEquals(value, TileMultipart$.MODULE$.renderID());
                assertEquals(value, first.getRenderType());
                TileMultipart$.MODULE$.renderID_$eq(~value);
                assertEquals(~value, TileMultipart.renderID());
                assertEquals(~value, first.getRenderType());
                assertEquals(~value, second.getRenderType());
            }
        } finally {
            TileMultipart.renderID_$eq(previous);
        }
    }
}
