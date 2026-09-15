package codechicken.microblock;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import codechicken.microblock.MicroMaterialRegistry.IMicroHighlightRenderer;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import scala.Tuple2;

/**
 * Scala companion singleton. Retained because compiled Scala consumers read MODULE$ and call these instance methods.
 */
public final class MicroMaterialRegistry$ {

    public static final MicroMaterialRegistry$ MODULE$ = new MicroMaterialRegistry$();

    private MicroMaterialRegistry$() {}

    public void registerMaterial(IMicroMaterial material, String name) {
        MicroMaterialRegistry.registerMaterial(material, name);
    }

    public void replaceMaterial(IMicroMaterial material, String name) {
        MicroMaterialRegistry.replaceMaterial(material, name);
    }

    public void registerHighlightRenderer(IMicroHighlightRenderer handler) {
        MicroMaterialRegistry.registerHighlightRenderer(handler);
    }

    public void remapName(String oldName, String newName) {
        MicroMaterialRegistry.remapName(oldName, newName);
    }

    /** Internal FMP lifecycle bridge; see {@link MicroMaterialRegistry#setupIDMap()}. */
    public void setupIDMap() {
        MicroMaterialRegistry.setupIDMap();
    }

    public int getMissingId() {
        return MicroMaterialRegistry.getMissingId();
    }

    /** Internal FMP lifecycle bridge; see {@link MicroMaterialRegistry#calcMaxCuttingStrength()}. */
    public void calcMaxCuttingStrength() {
        MicroMaterialRegistry.calcMaxCuttingStrength();
    }

    /** Internal FMP client texture-lifecycle bridge; see {@link MicroMaterialRegistry#loadIcons()}. */
    public void loadIcons() {
        MicroMaterialRegistry.loadIcons();
    }

    public int getMaxCuttingStrength() {
        return MicroMaterialRegistry.getMaxCuttingStrength();
    }

    /** Internal FMP handshake bridge; see {@link MicroMaterialRegistry#writeIDMap(PacketCustom)}. */
    public void writeIDMap(PacketCustom packet) {
        MicroMaterialRegistry.writeIDMap(packet);
    }

    /** Internal FMP handshake bridge; see {@link MicroMaterialRegistry#readIDMap(PacketCustom)}. */
    public List<String> readIDMap(PacketCustom packet) {
        return MicroMaterialRegistry.readIDMap(packet);
    }

    public void writeMaterialID(MCDataOutput data, int id) {
        MicroMaterialRegistry.writeMaterialID(data, id);
    }

    public int readMaterialID(MCDataInput data) {
        return MicroMaterialRegistry.readMaterialID(data);
    }

    public String materialName(int id) {
        return MicroMaterialRegistry.materialName(id);
    }

    public int materialID(String name) {
        return MicroMaterialRegistry.materialID(name);
    }

    public IMicroMaterial getMaterial(String name) {
        return MicroMaterialRegistry.getMaterial(name);
    }

    public IMicroMaterial getMaterial(int id) {
        return MicroMaterialRegistry.getMaterial(id);
    }

    /**
     * Returns the same legacy backing array as {@link MicroMaterialRegistry#getIdMap()}.
     *
     * @deprecated For enumeration, use {@link MicroMaterialRegistry#materialCount()},
     *             {@link MicroMaterialRegistry#materialName(int)} and {@link MicroMaterialRegistry#getMaterial(int)}.
     */
    @Deprecated
    public Tuple2<String, IMicroMaterial>[] getIdMap() {
        return MicroMaterialRegistry.getIdMap();
    }

    public boolean renderHighlight(EntityPlayer player, MovingObjectPosition hit, CommonMicroClass mcrClass, int size,
            int material) {
        return MicroMaterialRegistry.renderHighlight(player, hit, mcrClass, size, material);
    }
}
