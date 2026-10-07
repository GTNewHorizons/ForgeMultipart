package codechicken.multipart;

/**
 * Interface for parts that fill a slot based configuration as defined in PartMap. If this is implemented, calling
 * partMap(slot) on the host tile will return this part if the corresponding bit in the slotMask is set.
 * <p>
 * Marker interface for TSlottedTile.
 * <p>
 * The Scala trait extended TMultiPart, but a trait extending a class is a bare interface in bytecode, so implementors
 * must extend TMultiPart themselves.
 */
public interface TSlottedPart {

    /** A bitmask of slots that this part fills. Slot x is 1&lt;&lt;x. */
    int getSlotMask();
}
