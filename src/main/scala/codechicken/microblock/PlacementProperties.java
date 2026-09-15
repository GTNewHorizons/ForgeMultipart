package codechicken.microblock;

public abstract class PlacementProperties {

    public abstract int opposite(int slot, int side);

    public boolean sneakOpposite(int slot, int side) {
        return true;
    }

    public boolean expand(int slot, int side) {
        return true;
    }

    public abstract MicroblockClass microClass();

    public abstract PlacementGrid placementGrid();

    public ExecutablePlacement customPlacement(MicroblockPlacement placement) {
        return null;
    }
}
