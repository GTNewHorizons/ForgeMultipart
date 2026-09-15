package codechicken.multipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;

/** Class for reading and writing IDs, widening the carrier data type as necessary. */
public class IDWriter {

    private int width;

    public void setMax(int value) {
        // The reference Scala implementation widens negative values as signed values.
        long maximum = value;
        if (maximum > 0xffffL) {
            width = 4;
        } else if (maximum > 0xffL) {
            width = 2;
        } else {
            width = 1;
        }
    }

    public void write(MCDataOutput data, int id) {
        switch (width) {
            case 1:
                data.writeByte(id);
                break;
            case 2:
                data.writeShort(id);
                break;
            case 4:
                data.writeInt(id);
                break;
            default:
                throw new IllegalStateException("setMax was not called before writing an ID");
        }
    }

    public int read(MCDataInput data) {
        switch (width) {
            case 1:
                return data.readUByte();
            case 2:
                return data.readUShort();
            case 4:
                return data.readInt();
            default:
                throw new IllegalStateException("setMax was not called before reading an ID");
        }
    }
}
