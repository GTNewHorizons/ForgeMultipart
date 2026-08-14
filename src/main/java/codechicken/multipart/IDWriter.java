package codechicken.multipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import scala.Function1;
import scala.Function2;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;
import scala.runtime.BoxedUnit;

/** Class for reading and writing IDs, widening the carrier data type as necessary. */
public class IDWriter {

    private Function2<MCDataOutput, Object, BoxedUnit> write;
    private Function1<MCDataInput, Object> read;

    @Deprecated
    public Function2<MCDataOutput, Object, BoxedUnit> write() {
        return write;
    }

    @Deprecated
    public void write_$eq(Function2<MCDataOutput, Object, BoxedUnit> write) {
        this.write = write;
    }

    @Deprecated
    public Function1<MCDataInput, Object> read() {
        return read;
    }

    @Deprecated
    public void read_$eq(Function1<MCDataInput, Object> read) {
        this.read = read;
    }

    public void write(MCDataOutput data, int id) {
        write.apply(data, id);
    }

    public int read(MCDataInput data) {
        return (Integer) read.apply(data);
    }

    public void setMax(int value) {
        // The reference Scala implementation widens negative values as signed values.
        long maximum = value;
        if (maximum > 0xffffL) {
            write_$eq(new AbstractFunction2<MCDataOutput, Object, BoxedUnit>() {

                @Override
                public BoxedUnit apply(MCDataOutput data, Object id) {
                    data.writeInt((Integer) id);
                    return BoxedUnit.UNIT;
                }
            });
            read_$eq(new AbstractFunction1<MCDataInput, Object>() {

                @Override
                public Object apply(MCDataInput data) {
                    return data.readInt();
                }
            });
        } else if (maximum > 0xffL) {
            write_$eq(new AbstractFunction2<MCDataOutput, Object, BoxedUnit>() {

                @Override
                public BoxedUnit apply(MCDataOutput data, Object id) {
                    data.writeShort((Integer) id);
                    return BoxedUnit.UNIT;
                }
            });
            read_$eq(new AbstractFunction1<MCDataInput, Object>() {

                @Override
                public Object apply(MCDataInput data) {
                    return data.readUShort();
                }
            });
        } else {
            write_$eq(new AbstractFunction2<MCDataOutput, Object, BoxedUnit>() {

                @Override
                public BoxedUnit apply(MCDataOutput data, Object id) {
                    data.writeByte((Integer) id);
                    return BoxedUnit.UNIT;
                }
            });
            read_$eq(new AbstractFunction1<MCDataInput, Object>() {

                @Override
                public Object apply(MCDataInput data) {
                    return (int) data.readUByte();
                }
            });
        }
    }
}
