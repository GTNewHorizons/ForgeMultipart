package codechicken.multipart.examples;

import codechicken.multipart.TileMultipart;

/** Compiling example for docs/api/RENDER_ID.md; does not initialize the client renderer. */
public final class RenderIdExample {

    private RenderIdExample() {}

    public static int multipartRenderType() {
        return TileMultipart.getRenderID();
    }
}
