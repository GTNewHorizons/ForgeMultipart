package codechicken.multipart;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.MultiPartRegistry.IPartFactory;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import cpw.mods.fml.common.ModContainer;
import scala.collection.JavaConversions;

/**
 * Scala companion singleton. Retained because compiled Scala consumers read MODULE$ and call these instance methods.
 */
public final class MultiPartRegistry$ {

    public static final MultiPartRegistry$ MODULE$ = new MultiPartRegistry$();

    /** Retained with its original name and Scala type because Schematica reflects it directly. */
    @SuppressWarnings("unused")
    private final scala.collection.mutable.Map<String, IPartFactory2> codechicken$multipart$MultiPartRegistry$$typeMap = JavaConversions
            .mapAsScalaMap(MultiPartRegistry.typeMapBacking());

    private MultiPartRegistry$() {}

    @Deprecated
    public void registerParts(IPartFactory partFactory, String... types) {
        MultiPartRegistry.registerParts(partFactory, types);
    }

    @Deprecated
    public void registerParts(scala.Function2<String, Object, TMultiPart> partFactory,
            scala.collection.Seq<String> types) {
        MultiPartRegistry.registerParts(partFactory, types);
    }

    public void registerParts(IPartFactory2 partFactory, String... types) {
        MultiPartRegistry.registerParts(partFactory, types);
    }

    public void registerParts(IPartFactory2 partFactory, scala.collection.Seq<String> types) {
        MultiPartRegistry.registerParts(partFactory, types);
    }

    public void registerConverter(IPartConverter c) {
        MultiPartRegistry.registerConverter(c);
    }

    public void beforeServerStart() {
        MultiPartRegistry.beforeServerStart();
    }

    public void writeIDMap(PacketCustom packet) {
        MultiPartRegistry.writeIDMap(packet);
    }

    public List<String> readIDMap(PacketCustom packet) {
        return MultiPartRegistry.readIDMap(packet);
    }

    public boolean required() {
        return MultiPartRegistry.required();
    }

    public boolean loaded() {
        return MultiPartRegistry.loaded();
    }

    public void postInit() {
        MultiPartRegistry.postInit();
    }

    public void writePartID(MCDataOutput data, TMultiPart part) {
        MultiPartRegistry.writePartID(data, part);
    }

    public TMultiPart readPart(MCDataInput data) {
        return MultiPartRegistry.readPart(data);
    }

    public TMultiPart loadPart(String name, NBTTagCompound nbt) {
        return MultiPartRegistry.loadPart(name, nbt);
    }

    @Deprecated
    public TMultiPart createPart(String name, boolean client) {
        return MultiPartRegistry.createPart(name, client);
    }

    public TMultiPart convertBlock(World world, BlockCoord pos, Block block) {
        return MultiPartRegistry.convertBlock(world, pos, block);
    }

    public ModContainer getModContainer(String name) {
        return MultiPartRegistry.getModContainer(name);
    }
}
