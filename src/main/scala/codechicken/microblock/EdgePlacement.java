package codechicken.microblock;

public final class EdgePlacement {

    private EdgePlacement() {}

    public static ExecutablePlacement customPlacement(MicroblockPlacement placement) {
        return EdgePlacement$.MODULE$.customPlacement(placement);
    }

    public static EdgeMicroClass$ microClass() {
        return EdgePlacement$.MODULE$.microClass();
    }

    public static EdgePlacementGrid$ placementGrid() {
        return EdgePlacement$.MODULE$.placementGrid();
    }

    public static int opposite(int slot, int side) {
        return EdgePlacement$.MODULE$.opposite(slot, side);
    }

    public static boolean expand(int slot, int side) {
        return EdgePlacement$.MODULE$.expand(slot, side);
    }

    public static boolean sneakOpposite(int slot, int side) {
        return EdgePlacement$.MODULE$.sneakOpposite(slot, side);
    }
}
