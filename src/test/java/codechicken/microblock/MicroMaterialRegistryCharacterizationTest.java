package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Proxy;

import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroHighlightRenderer;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

/**
 * The registry is global mutable state, so everything is registered once in {@link #registerAndFreezeIdMap()} and the
 * cases below only read it. Error paths are not covered: they call the microblock logger, which is null until
 * MicroblockProxy.preInit runs, so they cannot execute headless.
 */
class MicroMaterialRegistryCharacterizationTest {

    private static final TestMaterial STONE = new TestMaterial(false, 3);
    private static final TestMaterial GLASS = new TestMaterial(true, 1);

    @BeforeAll
    static void registerAndFreezeIdMap() {
        // Registration is global and re-registering hits the logger-backed error path, which cannot run headless.
        if (MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
        }
        MicroMaterialRegistry.registerMaterial(STONE, "test:stone");
        MicroMaterialRegistry.registerMaterial(GLASS, "test:glass");
        MicroMaterialRegistry.remapName("test:oldglass", "test:glass");
        // setupIDMap is package private in Scala, so it has no static forwarder and must go through the singleton.
        MicroMaterialRegistry$.MODULE$.setupIDMap();
    }

    @Test
    void idMapIsSortedByNameAndDrivesBothLookups() {
        String[] names = new String[MicroMaterialRegistry.getIdMap().length];
        for (int i = 0; i < names.length; i++) {
            names[i] = MicroMaterialRegistry.getIdMap()[i]._1();
        }

        String[] sorted = names.clone();
        java.util.Arrays.sort(sorted);
        assertArrayEquals(sorted, names);

        for (int i = 0; i < names.length; i++) {
            assertEquals(i, MicroMaterialRegistry.materialID(names[i]));
            assertEquals(names[i], MicroMaterialRegistry.materialName(i));
            assertSame(MicroMaterialRegistry.getIdMap()[i]._2(), MicroMaterialRegistry.getMaterial(i));
        }
    }

    @Test
    void getMaterialByNameResolvesRemapsAndReturnsNullWhenUnknown() {
        assertSame(GLASS, MicroMaterialRegistry.getMaterial("test:glass"));
        assertSame(GLASS, MicroMaterialRegistry.getMaterial("test:oldglass"));
        assertNull(MicroMaterialRegistry.getMaterial("test:nosuchmaterial"));
    }

    @Test
    void materialIdResolvesRemappedNames() {
        assertEquals(MicroMaterialRegistry.materialID("test:glass"), MicroMaterialRegistry.materialID("test:oldglass"));
    }

    @Test
    void missingIdPointsAtTheMissingMaterialRatherThanZero() {
        assertEquals(
                MicroMaterialRegistry.materialID(MissingMicroMaterial.key()),
                MicroMaterialRegistry.getMissingId());
        assertSame(
                MissingMicroMaterial$.MODULE$,
                MicroMaterialRegistry.getMaterial(MicroMaterialRegistry.getMissingId()));
    }

    @Test
    void materialIdsRoundTripThroughTheIdWriter() throws Exception {
        for (int id = 0; id < MicroMaterialRegistry.getIdMap().length; id++) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            MCDataOutput output = new MCDataOutputWrapper(new DataOutputStream(bytes));
            MicroMaterialRegistry.writeMaterialID(output, id);

            byte[] encoded = bytes.toByteArray();
            assertEquals(1, encoded.length, "A small id map must still use the byte carrier");
            assertEquals(id, MicroMaterialRegistry.readMaterialID(input(encoded)));
        }
    }

    @Test
    void highlightRendererShortCircuitsTheDefaultRenderer() {
        RecordingRenderer accepting = new RecordingRenderer(true);
        MicroMaterialRegistry.registerHighlightRenderer(accepting);

        // A renderer that claims the highlight stops the default client renderer being reached at all.
        assertTrue(MicroMaterialRegistry.renderHighlight(null, null, null, 2, 0));
        assertEquals(1, accepting.calls);
    }

    /**
     * MissingMicroMaterial overrides only isTransparent and loadIcons, so it inherits canRenderInPass and isSolid from
     * the trait. A Java class cannot inherit them today, so this is the only way to observe the supplied values.
     */
    @Test
    void materialInterfaceSuppliesRenderPassAndSolidity() {
        assertTrue(MissingMicroMaterial.canRenderInPass(0));
        assertFalse(MissingMicroMaterial.canRenderInPass(1));
        assertFalse(MissingMicroMaterial.canRenderInPass(-1));

        assertFalse(MissingMicroMaterial.isTransparent());
        assertTrue(MissingMicroMaterial.isSolid());
    }

    private static MCDataInput input(byte[] bytes) {
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
        return (MCDataInput) Proxy.newProxyInstance(
                MCDataInput.class.getClassLoader(),
                new Class<?>[] { MCDataInput.class },
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "readUByte":
                            return (short) data.readUnsignedByte();
                        case "readUShort":
                            return data.readUnsignedShort();
                        case "readInt":
                            return data.readInt();
                        default:
                            throw new AssertionError("Unexpected read method: " + method.getName());
                    }
                });
    }

    private static final class RecordingRenderer implements IMicroHighlightRenderer {

        private final boolean handled;
        private int calls;

        private RecordingRenderer(boolean handled) {
            this.handled = handled;
        }

        @Override
        public boolean renderHighlight(EntityPlayer player, MovingObjectPosition hit, CommonMicroClass mcrClass,
                int size, int material) {
            calls++;
            return handled;
        }
    }

    /** Mirrors the forwarders Scala emits, so this file compiles against the trait and against the Java interface. */
    private static final class TestMaterial implements IMicroMaterial {

        private final boolean transparent;
        private final int cutterStrength;

        private TestMaterial(boolean transparent, int cutterStrength) {
            this.transparent = transparent;
            this.cutterStrength = cutterStrength;
        }

        @Override
        public IIcon getBreakingIcon(int side) {
            return null;
        }

        @Override
        public void loadIcons() {}

        @Override
        public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {}

        @Override
        public boolean canRenderInPass(int pass) {
            return pass == 0;
        }

        @Override
        public boolean isTransparent() {
            return transparent;
        }

        @Override
        public int getLightValue() {
            return 0;
        }

        @Override
        public float getStrength(EntityPlayer player) {
            return 1f;
        }

        @Override
        public String getLocalizedName() {
            return "Test Material";
        }

        @Override
        public ItemStack getItem() {
            return null;
        }

        @Override
        public int getCutterStrength() {
            return cutterStrength;
        }

        @Override
        public SoundType getSound() {
            return null;
        }

        @Override
        public boolean isSolid() {
            return !isTransparent();
        }

        @Override
        public float explosionResistance(Entity entity) {
            return 1f;
        }
    }
}
