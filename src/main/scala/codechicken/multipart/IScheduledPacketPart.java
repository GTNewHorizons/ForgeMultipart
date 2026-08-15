package codechicken.multipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;

/** Callback interface for PacketScheduler. */
public interface IScheduledPacketPart {

    /** Write scheduled data to the packet, mask is the cumulative mask from calls to schedulePacket. */
    void writeScheduled(long mask, MCDataOutput packet);

    /**
     * Returns the width (in bytes) of the data type required to hold all valid mask bits. Valid values are 1, 2, 4 and
     * 8.
     */
    int maskWidth();

    /**
     * Read data matching mask. Establishes a method for subclasses to override. This should be called from read.
     */
    void readScheduled(long mask, MCDataInput packet);
}
