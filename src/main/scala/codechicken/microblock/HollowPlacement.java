package codechicken.microblock;

public final class HollowPlacement {

    private HollowPlacement() {}

    public static ExecutablePlacement customPlacement(MicroblockPlacement placement) {
        return HollowPlacement$.MODULE$.customPlacement(placement);
    }

    public static HollowMicroClass$ microClass() {
        return HollowPlacement$.MODULE$.microClass();
    }

    public static HollowPlacementGrid$ placementGrid() {
        return HollowPlacement$.MODULE$.placementGrid();
    }

    public static int opposite(int slot, int side) {
        return HollowPlacement$.MODULE$.opposite(slot, side);
    }

    public static boolean expand(int slot, int side) {
        return HollowPlacement$.MODULE$.expand(slot, side);
    }

    public static boolean sneakOpposite(int slot, int side) {
        return HollowPlacement$.MODULE$.sneakOpposite(slot, side);
    }

    public static class HollowPlacementGrid$ extends FaceEdgeGrid {

        public static final HollowPlacementGrid$ MODULE$ = new HollowPlacementGrid$();

        public HollowPlacementGrid$() {
            super(3 / 8D);
        }
    }
}
