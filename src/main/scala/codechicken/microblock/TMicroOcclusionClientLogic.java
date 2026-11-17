package codechicken.microblock;

/** Render-state updates behind the retained Scala state accessors and lifecycle super bridges. */
final class TMicroOcclusionClientLogic {

    private TMicroOcclusionClientLogic() {}

    static void recalcBounds(TMicroOcclusionClient part) {
        part.renderBounds_$eq(part.getBounds().copy());
        part.renderMask_$eq(MicroOcclusion$.MODULE$.recalcBounds(part, part.renderBounds()));
    }
}
