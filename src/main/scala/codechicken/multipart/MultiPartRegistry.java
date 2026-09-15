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
     * @deprecated Implement {@link IPartFactory2} and use
     *             {@link MultiPartRegistry#registerPartFactory(IPartFactory2, String...)}.
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

    /**
     * Factory with separate server/NBT and client/packet construction paths. Register during mod initialization with
     * {@link MultiPartRegistry#registerPartFactory(IPartFactory2, String...)}. Registration does not call the factory.
     * Return a fresh, unbound part with a stable registered {@link TMultiPart#getType()} for each supported name.
     */
    public interface IPartFactory2 {

        /**
         * Create a new server instance of the part with the specified type name identifier.
         *
         * @param nbt The same tag compound the reconstruction caller subsequently passes to part.load; may be inspected
         *            to select the part class. The registry does not call load. Legacy createPart(name, false) passes
         *            null, so factories used through that deprecated path must handle it.
         */
        TMultiPart createPart(String name, NBTTagCompound nbt);

        /**
         * Create a new client instance of the part with the specified type name identifier.
         *
         * @param packet The same input the reconstruction caller subsequently passes to part.readDesc, positioned after
         *               the registry ID. A factory may consume a constructor discriminator; readDesc continues at the
         *               resulting cursor without a rewind. The registry does not call readDesc. Legacy createPart(name,
         *               true) passes null.
         */
        TMultiPart createPart(String name, MCDataInput packet);
    }

    /**
     * Converts an existing block/tile into a fresh, unbound part. Register once during common mod initialization on
     * both sides with {@link MultiPartRegistry#registerConverter(IPartConverter)}, separately from its persistent part
     * factory. Conversion may run repeatedly for inspection, placement checks and eventual placement; do not remove the
     * source block or transfer live resources here. See {@link TMultiPart#invalidateConvertedTile()} and
     * {@link TMultiPart#onConverted()} for the committed conversion lifecycle.
     */
    public interface IPartConverter {

        /**
         * Returns the block instances this converter may handle. Enumerated immediately during registration; the
         * iterable is not retained. Return non-null blocks without duplicates. Metadata/tile eligibility is checked by
         * convert, not by registration.
         */
        Iterable<Block> blockTypes();

        /**
         * Inspect the source at pos and return a fresh, unbound part, or null to let the next converter try. World and
         * position are borrowed, unchanged references; do not mutate the position. Use world.isRemote for side-specific
         * construction. Copy required state without consuming it: the candidate may be discarded and this callback may
         * run again. FMP does not invoke load/readDesc on this result or validate its registered type. Exceptions
         * propagate and stop dispatch; they do not mean "try the next converter".
         */
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
     * @deprecated Implement {@link IPartFactory2} and use {@link #registerPartFactory(IPartFactory2, String...)}.
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
     * @deprecated Implement {@link IPartFactory2} and use {@link #registerPartFactory(IPartFactory2, String...)}.
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

    /**
     * Scala va-args version of registerParts.
     *
     * @deprecated Use {@link #registerPartFactory(IPartFactory2, String...)} with Java strings/arrays. Retained for
     *             existing Scala callers; sequence conversion still precedes registry-state checks.
     */
    @Deprecated
    public static void registerParts(IPartFactory2 partFactory, scala.collection.Seq<String> types) {
        registerParts(partFactory, JavaConversions.seqAsJavaList(types).toArray(new String[0]));
    }

    /**
     * Register a part factory with an array of types it is capable of instantiating. Must be called before postInit.
     * Prefer {@link #registerPartFactory(IPartFactory2, String...)} in Java sources compiled without Scala: this legacy
     * overload family contains Scala parameter types that javac may require during overload resolution.
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

    /**
     * Registers a factory for stable part identifiers during a mod's preInit/init, before FMP's postInit closes the
     * registry. Requires FML's active mod container, which becomes the owner of each ID. Register on both sides using
     * the same IDs; construction later selects the NBT/server or packet/client factory method.
     *
     * <p>
     * Processes IDs in array order without calling the factory or retaining the array. A duplicate throws immediately;
     * earlier registrations from this call remain. Factory and ID validity are not eagerly checked, so callers must
     * supply a non-null factory and valid non-null, stable names. A null array fails after the existing state/container
     * checks. Empty arrays still perform those checks. This entry retains all legacy failure/state behavior.
     *
     * @param partFactory factory retained for later construction; it does not load, bind or place parts at registration
     * @param types       unique persistent type names; preserve published names when migrating existing parts
     */
    public static void registerPartFactory(IPartFactory2 partFactory, String... types) {
        registerParts(partFactory, types);
    }

    /**
     * Registers a converter for each block returned by blockTypes, in iteration order. Retains the converter object and
     * appends to each block's ordered list; duplicate blocks or calls create duplicate entries. Registration does not
     * call convert, register a part factory or copy the iterable. Iterator failures leave earlier entries present.
     *
     * <p>
     * Call once from common preInit/init after source blocks exist, on both sides. Unlike part-factory registration,
     * this legacy entry does not enforce an active mod container or a registry-state gate. It provides no unregister,
     * deduplication or synchronization; do not use world-load callbacks to repeatedly register converters.
     */
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

    /**
     * Returns the registered factory for an exact, case-sensitive part type name, or null if no factory is mapped.
     * Reads the current registration map, independently of the network ID map, without constructing a part, logging a
     * missing name or changing registration state. The returned object is the registered instance, not a copy.
     *
     * <p>
     * Look up after the owning mod has registered its parts, on the initialization/game thread. This method neither
     * waits for registration nor adds synchronization. It grants no right to mutate the registry or factory state. For
     * ordinary NBT/packet construction use {@link #loadPart(String, NBTTagCompound)} or {@link #readPart(MCDataInput)}.
     * Specialized callers may inspect the factory's supported type, for example a microblock class needed to create a
     * client preview from a material ID. Factory construction still requires the caller to load and bind the part.
     */
    public static IPartFactory2 getPartFactory(String name) {
        return typeMap.get(name);
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

    /**
     * Tries converters registered for the supplied block, in registration order, and returns the first non-null result
     * unchanged. The block argument selects the list; this method does not read or verify the world's block. Callbacks
     * receive the original world/pos references. Returns null when all decline or none are registered; failures
     * propagate. Does not load, bind, install or invoke conversion lifecycle hooks on the result. Ordinary callers
     * should use {@link TileMultipart#getOrConvertTileResult(World, BlockCoord)} for a bound placeholder, or the
     * placement APIs.
     */
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
