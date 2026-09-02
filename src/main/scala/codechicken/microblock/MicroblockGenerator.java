package codechicken.microblock;

import java.util.BitSet;

import scala.collection.Seq;

public final class MicroblockGenerator {

    public interface IGeneratedMaterial {

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

    public static int registerTrait(Class<?> trait) {
        return MicroblockGenerator$.MODULE$.registerTrait(trait);
    }

    public static int registerTrait(String trait) {
        return MicroblockGenerator$.MODULE$.registerTrait(trait);
    }

    public static BitSet getBitSet() {
        return MicroblockGenerator$.MODULE$.getBitSet();
    }

    public static BitSet freshBitSet() {
        return MicroblockGenerator$.MODULE$.freshBitSet();
    }

    public static Microblock create(MicroblockClass microClass, int material, boolean client) {
        return MicroblockGenerator$.MODULE$.create(microClass, material, client);
    }
}
