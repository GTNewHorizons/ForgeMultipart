package codechicken.multipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;

/**
 * Mixin interface for parts whose updates are batched through {@link PacketScheduler}. Implementors must also extend
 * {@link TMultiPart}; the Scala trait declared that, but a trait extending a class is a bare interface in bytecode.
 */
public interface TScheduledPacketPart extends IScheduledPacketPart {

    /**
     * This cannot be a default method: {@link TMultiPart} declares read, and a superclass method always beats an
     * interface default, so the default would never run and the part would read a description instead of a mask. Every
     * implementor must declare it and delegate to {@link #readMask(TScheduledPacketPart, MCDataInput)}.
     *
     * <pre>
     * 
     * &#64;Override
     * public final void read(MCDataInput packet) {
     *     TScheduledPacketPart.readMask(this, packet);
     * }
     * </pre>
     */
    void read(MCDataInput packet);

    @Override
    default void writeScheduled(long mask, MCDataOutput packet) {}

    @Override
    default void readScheduled(long mask, MCDataInput packet) {}

    /** Reads a mask of the part's own width and hands it to readScheduled. */
    static void readMask(TScheduledPacketPart part, MCDataInput packet) {
        long mask;
        switch (part.maskWidth()) {
            case 1:
                mask = packet.readUByte();
                break;
            case 2:
                mask = packet.readUShort();
                break;
            case 4:
                mask = packet.readInt();
                break;
            case 8:
                mask = packet.readLong();
                break;
            default:
                throw new IllegalArgumentException("Invalid maskWidth: " + part.maskWidth());
        }
        part.readScheduled(mask, packet);
    }
}
