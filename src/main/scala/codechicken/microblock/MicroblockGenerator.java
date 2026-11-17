package codechicken.microblock;

import java.util.BitSet;

import scala.collection.Seq;

public final class MicroblockGenerator {

    /** Optional material capability for adding registered traits before a microblock is constructed. */
    public interface IGeneratedMaterial {

        /**
         * Adds material-specific traits to the borrowed generator scratch set, which already contains the factory's
         * base trait and, on the client path, its client trait. Add registered trait IDs without removing required
         * traits. Do not retain this mutable set or recursively create a microblock from this callback: the next
         * creation on this thread clears and reuses the same set. Exceptions propagate without rolling back changes.
         *
         * @param microClass the exact factory supplied to create
         * @param client     the requested construction side, not a world lookup
         */
        void addTraits(BitSet traits, MicroblockClass microClass, boolean client);
    }

    private MicroblockGenerator() {}

    public static Class<Microblock> baseType() {
        return MicroblockGenerator$.MODULE$.baseType();
    }

    @SuppressWarnings("rawtypes")
    public static Object construct(BitSet traits, Seq arguments) {
        return MicroblockGenerator$.MODULE$.construct(traits, arguments);
    }

    public static int getId(String trait) {
        return MicroblockGenerator$.MODULE$.getId(trait);
    }

    /**
     * Registers an already loadable trait, including retained Scala trait interfaces. For a Java source trait that FMP
     * must rewrite into an interface, use {@link #registerTrait(String)} before its class is loaded instead.
     */
    public static int registerTrait(Class<?> trait) {
        return MicroblockGenerator$.MODULE$.registerTrait(trait);
    }

    /**
     * Registers a microblock trait by binary name (dots or slashes accepted), returning its generator-local ID.
     * Existing registration returns the same ID. Use this overload for Java source traits before any class literal,
     * instance or other use loads that class; FMP must first transform its class bytes into a runtime interface.
     * Register during mod initialization, before material registration and construction that require this trait.
     *
     * Java input is a top-level class extending Microblock (possibly abstract), with a no-argument constructor. Its
     * direct superclass constructor call is discarded during trait initialization; the generated microblock receives
     * the real material ID. Client-only overrides must carry SideOnly(CLIENT). The existing transformer has bytecode
     * restrictions: keep inherited state access and substantial logic in an ordinary helper, passing the trait as
     * Object and casting to Microblock there. Do not instantiate or invoke a transformed trait as a Java class. Use
     * stable Microblock methods or a separately declared capability interface for consumer dispatch.
     *
     * The material's {@link IGeneratedMaterial#addTraits(BitSet, MicroblockClass, boolean)} callback adds this ID to
     * each requested shape/side. Registration does not register a material or remove the retained Scala trait path.
     */
    public static int registerTrait(String trait) {
        return MicroblockGenerator$.MODULE$.registerTrait(trait);
    }

    public static BitSet getBitSet() {
        return MicroblockGenerator$.MODULE$.getBitSet();
    }

    public static BitSet freshBitSet() {
        return MicroblockGenerator$.MODULE$.freshBitSet();
    }

    /**
     * Constructs a fresh, unbound microblock using the factory's registered traits and an ID from the active material
     * map. Selects the base trait, then the client trait if requested, then invokes the material's
     * {@link IGeneratedMaterial#addTraits(BitSet, MicroblockClass, boolean)} callback if implemented. Generated classes
     * are cached, but part instances are new and their trait initializers run for each construction.
     *
     * <p>
     * Does not copy another part, load NBT, assign shape, bind to a tile or install anything in a world. Callers
     * restore shape or load NBT before preparing/loading the composite tile. Resolve the intended material before
     * construction; subsequently changing a part's material field does not regenerate its material-specific traits.
     *
     * <p>
     * Call on the initialization/game thread after factory/trait registration and material-map setup. On multiplayer
     * clients use the active synchronized material IDs. Client microblock construction requires the physical client;
     * the dedicated server strips client-only factory methods. Null factories, invalid/unresolved IDs and callback or
     * construction failures propagate through the existing generator. No fallback or rollback is added.
     *
     * @param microClass factory whose registered traits define the microblock family
     * @param material   numeric ID in the active material map, not a persistent saved identifier
     * @param client     true for client traits, false for server traits
     */
    public static Microblock create(MicroblockClass microClass, int material, boolean client) {
        return MicroblockGenerator$.MODULE$.create(microClass, material, client);
    }
}
