package codechicken.microblock;

public final class FacePlacement$ extends PlacementProperties {

    public static final FacePlacement$ MODULE$ = new FacePlacement$();

    private FacePlacement$() {}

    @Override
    public FaceMicroClass$ microClass() {
        return FaceMicroClass$.MODULE$;
    }

    @Override
    public FacePlacementGrid$ placementGrid() {
        return FacePlacementGrid$.MODULE$;
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
