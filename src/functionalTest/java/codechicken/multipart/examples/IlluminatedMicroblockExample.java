package codechicken.multipart.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.HollowMicroblock;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;
import codechicken.multipart.TMultiPart;

/** Compiling example for docs/api/MICROBLOCK_EXTENSIONS.md; the consumer supplies its config and halo renderer. */
public final class IlluminatedMicroblockExample {

    public static BooleanSupplier dimLampParts = () -> false;

    /**
     * Install on the physical client before rendering, e.g. a lambda forwarding to ProjectRed's RenderHalo.addLight.
     */
    public static HaloRenderer haloRenderer;

    public interface HaloRenderer {

        void addLight(int x, int y, int z, int colour, Cuboid6 bounds);
    }

    private IlluminatedMicroblockExample() {}

    /** Call once during mod initialization, after the lamp block is registered and before FMP builds material IDs. */
    public static void registerMaterials(Block lamp) {
        int traitId = MicroblockGenerator.registerTrait("codechicken.multipart.examples.IlluminatedMicroblockTrait");
        for (int meta = 16; meta < 32; meta++) {
            MicroMaterialRegistry.registerMaterial(
                    new IlluminatedMicroMaterial(lamp, meta, traitId),
                    BlockMicroMaterial.materialKey(lamp, meta));
        }
    }

    /** Object is deliberate: the transformed trait interface does not inherit Microblock in the JVM type system. */
    public static int lightValue(Object trait) {
        Microblock part = (Microblock) trait;
        if (dimLampParts.getAsBoolean()) {
            // Capture matching parts before size callbacks, as the reference's strict collection pipeline does.
            List<Microblock> selected = new ArrayList<>();
            for (TMultiPart sibling : part.tile().jPartList()) {
                if (sibling instanceof IlluminatedMicroblockTrait) selected.add((Microblock) sibling);
            }
            double total = 0;
            for (Microblock sibling : selected) total += sibling.getSize() / 8D;
            return Math.min(15, (int) (15 * total));
        }
        for (TMultiPart sibling : part.tile().jPartList()) {
            if (sibling instanceof IlluminatedMicroblockTrait) return 15;
        }
        return 0;
    }

    /** Halo submission is a client responsibility; the geometry itself is headlessly testable. */
    public static void renderHalo(Object trait, int pass) {
        if (pass != 0) return;
        Microblock part = (Microblock) trait;
        List<Cuboid6> boxes = haloBoxes(part);
        int colour = ((IlluminatedMicroMaterial) part.getIMaterial()).meta() - 16;
        for (Cuboid6 box : boxes) haloRenderer.addLight(part.x(), part.y(), part.z(), colour, box);
    }

    public static List<Cuboid6> haloBoxes(Microblock part) {
        double expansion = 0.025;
        if (part instanceof HollowMicroblock) {
            int size = ((HollowMicroblock) part).getHollowSize();
            double near = 0.5 - size / 32D;
            double far = 0.5 + size / 32D;
            double thickness = (part.shape() >> 4) / 8D;
            Transformation rotation = Rotation.sideRotations[part.shape() & 15].at(Vector3.center);
            List<Cuboid6> boxes = Arrays.asList(
                    new Cuboid6(
                            -expansion,
                            -expansion,
                            -expansion,
                            1 + expansion,
                            thickness + expansion,
                            near + expansion),
                    new Cuboid6(
                            -expansion,
                            -expansion,
                            far - expansion,
                            1 + expansion,
                            thickness + expansion,
                            1 + expansion),
                    new Cuboid6(
                            -expansion,
                            -expansion,
                            near + expansion,
                            near + expansion,
                            thickness + expansion,
                            far - expansion),
                    new Cuboid6(
                            far - expansion,
                            -expansion,
                            near + expansion,
                            1 + expansion,
                            thickness + expansion,
                            far - expansion));
            for (Cuboid6 box : boxes) box.apply(rotation);
            return boxes;
        }
        List<Cuboid6> source = new ArrayList<>();
        for (Cuboid6 box : part.getCollisionBoxes()) source.add(box);
        List<Cuboid6> expanded = new ArrayList<>();
        for (Cuboid6 box : source) expanded.add(box.copy().expand(expansion));
        return expanded;
    }
}
