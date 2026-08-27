package codechicken.multipart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.google.common.collect.ArrayListMultimap;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.handler.MultipartProxy;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import scala.Tuple2;
import scala.collection.JavaConversions;

/** This class handles the registration and internal ID mapping of all multipart classes. */
public final class MultiPartRegistry {

    /**
     * Interface to be registered for constructing parts. Every instance of every multipart is constructed from an
     * implementor of this.
     *
     * @deprecated Use IPartFactory2
     */
    @Deprecated
    public interface IPartFactory {

        /**
         * Create a new instance of the part with the specified type name identifier.
         *
         * @param client If the part instance is for the client or the server
         */
        TMultiPart createPart(String name, boolean client);
    }

    /** Will replace IPartFactory in 1.8. */
    public interface IPartFactory2 {

        /**
         * Create a new server instance of the part with the specified type name identifier.
         *
         * @param nbt The tag compound that will be passed to part.load, can be used to change the class of part
         *            returned
         */
        TMultiPart createPart(String name, NBTTagCompound nbt);

        /**
         * Create a new client instance of the part with the specified type name identifier.
         *
         * @param packet The packet that will be passed to part.readDesc, can be used to change the class of part
         *               returned
         */
        TMultiPart createPart(String name, MCDataInput packet);
    }

    /** An interface for converting existing blocks/tile entities to multipart versions. */
    public interface IPartConverter {

        /**
         * Return true if this converter can handle the specific blockID (may or may not actually convert the block).
         */
        Iterable<Block> blockTypes();

        /** Return a multipart version of the block at pos in world. Return null if no conversion is possible. */
        TMultiPart convert(World world, BlockCoord pos);
    }

    private static final Map<String, IPartFactory2> typeMap = new HashMap<>();
    private static final Map<String, Integer> nameMap = new HashMap<>();
    private static Tuple2<String, IPartFactory2>[] idMap;
    private static final IDWriter idWriter = new IDWriter();
    private static final ArrayListMultimap<Block, IPartConverter> converters = ArrayListMultimap.create();
    private static final Map<String, ModContainer> containers = new HashMap<>();

    /** The state of the registry. 0 = no parts, 1 = registering, 2 = registered. */
    private static int state = 0;

    private MultiPartRegistry() {}

    static Map<String, IPartFactory2> typeMapBacking() {
        return typeMap;
    }

    /**
     * Register a part factory with an array of types it is capable of instantiating. Must be called before postInit.
     *
     * @deprecated Use IPartFactory2
     */
    @Deprecated
    public static void registerParts(IPartFactory partFactory, String... types) {
        registerParts(new IPartFactory2() {

            @Override
            public TMultiPart createPart(String name, MCDataInput packet) {
                return partFactory.createPart(name, true);
            }

            @Override
            public TMultiPart createPart(String name, NBTTagCompound nbt) {
                return partFactory.createPart(name, false);
            }
        }, types);
    }

    /**
     * Scala function version of registerParts.
     *
     * @deprecated Use IPartFactory2
     */
    @Deprecated
    public static void registerParts(scala.Function2<String, Object, TMultiPart> partFactory,
            scala.collection.Seq<String> types) {
        registerParts(new IPartFactory2() {

            @Override
            public TMultiPart createPart(String name, MCDataInput packet) {
                return partFactory.apply(name, Boolean.TRUE);
            }

            @Override
            public TMultiPart createPart(String name, NBTTagCompound nbt) {
                return partFactory.apply(name, Boolean.FALSE);
            }
        }, types);
    }

    /** Scala va-args version of registerParts. */
    public static void registerParts(IPartFactory2 partFactory, scala.collection.Seq<String> types) {
        registerParts(partFactory, JavaConversions.seqAsJavaList(types).toArray(new String[0]));
    }

    /**
     * Register a part factory with an array of types it is capable of instantiating. Must be called before postInit.
     */
    public static void registerParts(IPartFactory2 partFactory, String... types) {
        if (loaded()) {
            throw new IllegalStateException("Parts must be registered in the init methods.");
        }
        state = 1;

        ModContainer container = Loader.instance().activeModContainer();
        if (container == null) {
            throw new IllegalStateException(
                    "Parts must be registered during the initialization phase of a mod container");
        }

        for (String s : types) {
            if (typeMap.containsKey(s)) {
                throw new IllegalStateException("Part with id " + s + " is already registered.");
            }

            typeMap.put(s, partFactory);
            containers.put(s, container);
        }
    }

    /** Register a part converter instance. */
    public static void registerConverter(IPartConverter c) {
        for (Block block : c.blockTypes()) {
            converters.put(block, c);
        }
    }

    @SuppressWarnings("unchecked")
    public static void beforeServerStart() {
        List<Map.Entry<String, IPartFactory2>> entries = new ArrayList<>(typeMap.entrySet());
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
    }

    public static void writeIDMap(PacketCustom packet) {
        packet.writeInt(idMap.length);
        for (Tuple2<String, IPartFactory2> entry : idMap) {
            packet.writeString(entry._1());
        }
    }

    /** Reads the server's id map, returning the names this client has no factory for. */
    @SuppressWarnings("unchecked")
    public static List<String> readIDMap(PacketCustom packet) {
        int k = packet.readInt();
        idWriter.setMax(k);
        idMap = new Tuple2[k];
        nameMap.clear();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            String s = packet.readString();
            IPartFactory2 v = typeMap.get(s);
            if (v == null) {
                missing.add(s);
            } else {
                idMap[i] = new Tuple2<>(s, v);
                nameMap.put(s, i);
            }
        }
        return missing;
    }

    /** Return true if any multiparts have been registered. */
    public static boolean required() {
        return state > 0;
    }

    /** Return true if no more parts can be registered. */
    public static boolean loaded() {
        return state == 2;
    }

    public static void postInit() {
        state = 2;
    }

    /** Writes the id of part to data. */
    public static void writePartID(MCDataOutput data, TMultiPart part) {
        Integer id = nameMap.get(part.getType());
        if (id == null) {
            throw new NoSuchElementException("None.get");
        }
        idWriter.write(data, id);
    }

    /** Uses instantiators to create a new part from the id read from data. */
    public static TMultiPart readPart(MCDataInput data) {
        Tuple2<String, IPartFactory2> e = idMap[idWriter.read(data)];
        return e._2().createPart(e._1(), data);
    }

    /** Uses instantiators to create a new part from a tag compound. */
    public static TMultiPart loadPart(String name, NBTTagCompound nbt) {
        IPartFactory2 factory = typeMap.get(name);
        if (factory == null) {
            MultipartProxy.logger().error("Missing mapping for part with ID: " + name);
            return null;
        }
        return factory.createPart(name, nbt);
    }

    /**
     * Uses instantiators to create a new part with specified identifier on side.
     *
     * @deprecated currently calls the nbt/packet version with a null parameter, use readPart or loadPart instead
     */
    @Deprecated
    public static TMultiPart createPart(String name, boolean client) {
        IPartFactory2 factory = typeMap.get(name);
        if (factory == null) {
            MultipartProxy.logger().error("Missing mapping for part with ID: " + name);
            return null;
        }
        return client ? factory.createPart(name, (MCDataInput) null) : factory.createPart(name, (NBTTagCompound) null);
    }

    /** Calls converters to create a multipart version of the block at pos. */
    public static TMultiPart convertBlock(World world, BlockCoord pos, Block block) {
        for (IPartConverter c : converters.get(block)) {
            TMultiPart ret = c.convert(world, pos);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static ModContainer getModContainer(String name) {
        ModContainer container = containers.get(name);
        if (container == null) {
            throw new NoSuchElementException("key not found: " + name);
        }
        return container;
    }
}
