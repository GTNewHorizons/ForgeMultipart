package codechicken.microblock;

public final class FacePlacement {

    private FacePlacement() {}

    public static ExecutablePlacement customPlacement(MicroblockPlacement placement) {
        return FacePlacement$.MODULE$.customPlacement(placement);
    }

    public static FaceMicroClass$ microClass() {
        return FacePlacement$.MODULE$.microClass();
    }

    public static FacePlacementGrid$ placementGrid() {
        return FacePlacement$.MODULE$.placementGrid();
    }

    public static int opposite(int slot, int side) {
        return FacePlacement$.MODULE$.opposite(slot, side);
    }

    public static boolean expand(int slot, int side) {
        return FacePlacement$.MODULE$.expand(slot, side);
    }

    public static boolean sneakOpposite(int slot, int side) {
        return FacePlacement$.MODULE$.sneakOpposite(slot, side);
    }
}
