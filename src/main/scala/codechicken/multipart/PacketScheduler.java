package codechicken.multipart;

import java.util.HashMap;
import java.util.Map;

import codechicken.lib.data.MCDataOutput;

/**
 * Static class for packing update data. When a specific property of a part changes and needs sending to the client, a
 * bit can be set in the mask. This bit can then be checked in the writeScheduled callback. This prevents sending
 * multiple packets if the same property updates more than once per tick.
 */
public final class PacketScheduler {

    private static final Map<TMultiPart, Long> map = new HashMap<>();

    private PacketScheduler() {}

    /** Add bits to the current update mask for part. (binary OR) */
    public static void schedulePacket(TMultiPart part, long mask) {
        if (part.world().isRemote) {
            throw new IllegalArgumentException("Cannot use PacketScheduler on a client world");
        }

        Long current = map.get(part);
        map.put(part, (current == null ? 0L : current) | mask);
    }

    /**
     * Writes every scheduled part and clears the schedule.
     * <p>
     * The reference declared this {@code private[multipart]}, which reaches the handler package. Java has no equivalent
     * scope, so it is public.
     */
    public static void sendScheduled() {
        for (Map.Entry<TMultiPart, Long> e : map.entrySet()) {
            TMultiPart part = e.getKey();
            long mask = e.getValue();
            if (part.tile() != null) {
                IScheduledPacketPart ipart = (IScheduledPacketPart) part;
                MCDataOutput w = part.getWriteStream();
                switch (ipart.maskWidth()) {
                    case 1:
                        w.writeByte((int) mask);
                        break;
                    case 2:
                        w.writeShort((int) mask);
                        break;
                    case 4:
                        w.writeInt((int) mask);
                        break;
                    case 8:
                        w.writeLong(mask);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid maskWidth: " + ipart.maskWidth());
                }

                ipart.writeScheduled(mask, w);
            }
        }
        map.clear();
    }
}
