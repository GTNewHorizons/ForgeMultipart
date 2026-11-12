package codechicken.multipart;

import codechicken.lib.data.MCDataOutput;
import scala.Option;
import scala.Tuple2;
import scala.collection.mutable.HashMap;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/**
 * Static class for packing update data. When a specific property of a part changes and needs sending to the client, a
 * bit can be set in the mask. This bit can then be checked in the writeScheduled callback. This prevents sending
 * multiple packets if the same property updates more than once per tick.
 */
public final class PacketScheduler {

    private static final HashMap<TMultiPart, Long> map = new HashMap<>();

    private PacketScheduler() {}

    /** Add bits to the current update mask for part. (binary OR) */
    public static void schedulePacket(TMultiPart part, long mask) {
        if (part.world().isRemote) {
            throw new IllegalArgumentException("Cannot use PacketScheduler on a client world");
        }

        Option<Long> current = map.get(part);
        map.put(part, (current.isEmpty() ? 0L : current.get()) | mask);
    }

    /**
     * Writes every scheduled part and clears the schedule.
     * <p>
     * The reference declared this {@code private[multipart]}, which reaches the handler package. Java has no equivalent
     * scope, so it is public.
     */
    public static void sendScheduled() {
        // Callbacks may modify the schedule. Keep Scala's original traversal and visibility of pending masks.
        map.foreach(new AbstractFunction1<Tuple2<TMultiPart, Long>, BoxedUnit>() {

            @Override
            public BoxedUnit apply(Tuple2<TMultiPart, Long> entry) {
                TMultiPart part = entry._1();
                long mask = entry._2();
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
                return BoxedUnit.UNIT;
            }
        });
        map.clear();
    }
}
