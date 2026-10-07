package codechicken.microblock;

public final class HollowPlacement$ extends PlacementProperties {

    public static final HollowPlacement$ MODULE$ = new HollowPlacement$();

    private HollowPlacement$() {}

    @Override
    public HollowMicroClass$ microClass() {
        return HollowMicroClass$.MODULE$;
    }

    @Override
    public HollowPlacement.HollowPlacementGrid$ placementGrid() {
        return HollowPlacement.HollowPlacementGrid$.MODULE$;
    }

    @Override
    public int opposite(int slot, int side) {
        return slot ^ 1;
    }

    @Override
    public boolean expand(int slot, int side) {
        return sneakOpposite(slot, side);
    }

    @Override
    public boolean sneakOpposite(int slot, int side) {
        return slot == (side ^ 1);
    }
}
