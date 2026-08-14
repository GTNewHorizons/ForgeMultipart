package codechicken.microblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.handler.MicroblockProxy;
import codechicken.multipart.IDWriter;
import codechicken.multipart.MultiPartRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.Tuple2;

public final class MicroMaterialRegistry {

    /** Interface for defining a micro material. */
    public interface IMicroMaterial {

        /** The icon to be used for breaking particles on side. */
        @SideOnly(Side.CLIENT)
        IIcon getBreakingIcon(int side);

        /** Callback to load icons from the underlying block/etc. */
        @SideOnly(Side.CLIENT)
        default void loadIcons() {}

        /**
         * This function must configure and use the CCRenderState pipeline to draw from the current vertex source.
         * CCRenderState.lightMatrix will be set.
         *
         * @param pass   The current render pass, -1 for inventory rendering
         * @param bounds The cuboid bounds of the face being rendered
         */
        @SideOnly(Side.CLIENT)
        void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds);

        /** Get the render pass for which this material renders in. */
        @SideOnly(Side.CLIENT)
        default boolean canRenderInPass(int pass) {
            return pass == 0;
        }

        /** Return true if this material is not opaque (glass, ice). */
        boolean isTransparent();

        /** Return the light level emitted by this material (glowstone). */
        int getLightValue();

        /** Return the strength of this material. */
        float getStrength(EntityPlayer player);

        /** Return the localised name of this material (normally the block name). */
        String getLocalizedName();

        /** Get the item that this material is cut from (full block to slabs). */
        ItemStack getItem();

        /** Get the strength of saw requried to cut this material. */
        int getCutterStrength();

        /** Get the breaking/walking sound. */
        SoundType getSound();

        /** Return true if this material is solid and opaque (can run wires on etc). */
        default boolean isSolid() {
            return !isTransparent();
        }

        /** Get the explosion resistance of this part to an explosion caused by entity. */
        float explosionResistance(Entity entity);
    }

    /**
     * Interface for overriding the default micro placement highlight handler to show the effect of placement on a
     * certain block/part.
     */
    public interface IMicroHighlightRenderer {

        /** Return true if a custom highlight was rendered and the default should be skipped. */
        boolean renderHighlight(EntityPlayer player, MovingObjectPosition hit, CommonMicroClass mcrClass, int size,
                int material);
    }

    private static final Map<String, IMicroMaterial> typeMap = new HashMap<>();
    private static final Map<String, Integer> nameMap = new HashMap<>();
    private static Tuple2<String, IMicroMaterial>[] idMap;
    private static final IDWriter idWriter = new IDWriter();

    private static final List<IMicroHighlightRenderer> highlightRenderers = new ArrayList<>();
    private static int maxCuttingStrength;

    private static final Map<String, String> remap = new HashMap<>();
    private static int missingId;

    private MicroMaterialRegistry() {}

    /** Register a micro material with unique identifier name. */
    public static void registerMaterial(IMicroMaterial material, String name) {
        if (MultiPartRegistry.loaded()) {
            throw new IllegalStateException("You must register your materials in the init methods.");
        }

        if (typeMap.containsKey(name)) {
            MicroblockProxy.logger().error("Material with id " + name + " is already registered.");
            return;
        }

        typeMap.put(name, material);
    }

    /** Replace a micro material with unique identifier name. */
    public static void replaceMaterial(IMicroMaterial material, String name) {
        if (MultiPartRegistry.loaded()) {
            throw new IllegalStateException("You must register your materials in the init methods.");
        }

        if (typeMap.remove(name) == null) {
            MicroblockProxy.logger().error("Material with id " + name + " was not registered.");
        }

        MicroblockProxy.logger().debug("Replaced micro material: " + name);

        typeMap.put(name, material);
    }

    /** Registers a highlight renderer. */
    public static void registerHighlightRenderer(IMicroHighlightRenderer handler) {
        highlightRenderers.add(handler);
    }

    public static void remapName(String oldName, String newName) {
        remap.put(oldName, newName);
    }

    @SuppressWarnings("unchecked")
    public static void setupIDMap() {
        List<Map.Entry<String, IMicroMaterial>> entries = new ArrayList<>(typeMap.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        idMap = new Tuple2[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            idMap[i] = new Tuple2<>(entries.get(i).getKey(), entries.get(i).getValue());
        }

        idWriter.setMax(idMap.length);
        nameMap.clear();
        for (int i = 0; i < idMap.length; i++) {
            nameMap.put(idMap[i]._1(), i);
        }

        Integer missing = nameMap.get(MissingMicroMaterial.key());
        if (missing == null) {
            throw new IllegalStateException(
                    "MissingMicroMaterial is not registered; the placeholder must never fall back to id 0.");
        }
        missingId = missing;
    }

    public static int getMissingId() {
        return missingId;
    }

    public static void calcMaxCuttingStrength() {
        int max = Integer.MIN_VALUE;
        boolean found = false;
        for (Iterator<?> it = Item.itemRegistry.iterator(); it.hasNext();) {
            Object next = it.next();
            if (next instanceof Saw) {
                max = Math.max(max, ((Saw) next).getMaxCuttingStrength());
                found = true;
            }
        }
        if (!found) {
            // Matches the reference, where max on an empty collection throws.
            throw new UnsupportedOperationException("empty.max");
        }
        maxCuttingStrength = max;
    }

    public static void loadIcons() {
        if (idMap != null) {
            for (Tuple2<String, IMicroMaterial> entry : idMap) {
                entry._2().loadIcons();
            }
        }
    }

    public static int getMaxCuttingStrength() {
        return maxCuttingStrength;
    }

    public static void writeIDMap(PacketCustom packet) {
        packet.writeInt(idMap.length);
        for (Tuple2<String, IMicroMaterial> entry : idMap) {
            packet.writeString(entry._1());
        }
    }

    /** Reads the server's id map, returning the names this client has no material for. */
    @SuppressWarnings("unchecked")
    public static List<String> readIDMap(PacketCustom packet) {
        int k = packet.readInt();
        idWriter.setMax(k);
        idMap = new Tuple2[k];
        nameMap.clear();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            String s = packet.readString();
            IMicroMaterial v = typeMap.get(s);
            if (v == null) {
                missing.add(s);
            } else {
                idMap[i] = new Tuple2<>(s, v);
                nameMap.put(s, i);
            }
        }
        return missing;
    }

    public static void writeMaterialID(MCDataOutput data, int id) {
        idWriter.write(data, id);
    }

    public static int readMaterialID(MCDataInput data) {
        return idWriter.read(data);
    }

    public static String materialName(int id) {
        return idMap[id]._1();
    }

    public static int materialID(String name) {
        Integer id = nameMap.get(remapped(name));
        if (id == null) {
            MicroblockProxy.logger().error("Missing mapping for part with ID: " + name);
            return missingId;
        }
        return id;
    }

    public static IMicroMaterial getMaterial(String name) {
        return typeMap.get(remapped(name));
    }

    public static IMicroMaterial getMaterial(int id) {
        return idMap[id]._2();
    }

    public static Tuple2<String, IMicroMaterial>[] getIdMap() {
        return idMap;
    }

    public static boolean renderHighlight(EntityPlayer player, MovingObjectPosition hit, CommonMicroClass mcrClass,
            int size, int material) {
        for (IMicroHighlightRenderer renderer : highlightRenderers) {
            if (renderer.renderHighlight(player, hit, mcrClass, size, material)) {
                return true;
            }
        }

        MicroblockRender.renderHighlight(player, hit, mcrClass, size, material);
        return true;
    }

    private static String remapped(String name) {
        String replacement = remap.get(name);
        return replacement == null ? name : replacement;
    }
}
