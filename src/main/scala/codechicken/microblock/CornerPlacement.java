package codechicken.microblock;

public final class CornerPlacement {

    private CornerPlacement() {}

    public static ExecutablePlacement customPlacement(MicroblockPlacement placement) {
        return CornerPlacement$.MODULE$.customPlacement(placement);
    }

    public static CornerMicroClass$ microClass() {
        return CornerPlacement$.MODULE$.microClass();
    }

    public static CornerPlacementGrid$ placementGrid() {
        return CornerPlacement$.MODULE$.placementGrid();
    }

    public static int opposite(int slot, int side) {
        return CornerPlacement$.MODULE$.opposite(slot, side);
    }

    public static boolean expand(int slot, int side) {
        return CornerPlacement$.MODULE$.expand(slot, side);
    }

    public static boolean sneakOpposite(int slot, int side) {
        return CornerPlacement$.MODULE$.sneakOpposite(slot, side);
    }
}
