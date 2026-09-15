package codechicken.multipart.examples;

import net.minecraftforge.common.util.ForgeDirection;

import codechicken.multipart.minecraft.ButtonPart;

/** Forge-tested compiling example for docs/api/BUTTON_ORIENTATIONS.md. */
public final class ButtonOrientationExample {

    private ButtonOrientationExample() {}

    /** Enables the floor and ceiling metadata supplied by Et Futurum Requiem's button extension. */
    public static void registerVerticalOrientations() {
        ButtonPart.setOrientation(0, ForgeDirection.UP);
        ButtonPart.setOrientation(5, ForgeDirection.DOWN);
    }
}
