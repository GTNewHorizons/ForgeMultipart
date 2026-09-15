package codechicken.microblock;

public final class CornerPlacement$ extends PlacementProperties {

    public static final CornerPlacement$ MODULE$ = new CornerPlacement$();

    private CornerPlacement$() {}

    @Override
    public CornerMicroClass$ microClass() {
        return CornerMicroClass$.MODULE$;
    }

    @Override
    public CornerPlacementGrid$ placementGrid() {
        return CornerPlacementGrid$.MODULE$;
    }

    @Override
    public int opposite(int slot, int side) {
        return ((slot - 7) ^ (1 << (side >> 1))) + 7;
    }
}
