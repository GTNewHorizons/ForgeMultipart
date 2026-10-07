package codechicken.multipart.asm;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import scala.runtime.BoxesRunTime;

public class ByteCodeReader {

    private final Bytes bc;
    private int pos;

    public ByteCodeReader(Bytes bc) {
        this.bc = bc;
        pos = bc.pos();
    }

    public Bytes bc() {
        return bc;
    }

    public int pos() {
        return pos;
    }

    public void pos_$eq(int pos) {
        this.pos = pos;
    }

    public boolean more() {
        return pos() < bc().pos() + bc().len();
    }

    public String readString(int len) {
        byte[] bytes = bc().arr();
        int start = pos();
        start = Math.min(bytes.length, Math.max(0, start));
        int length = Math.min(bytes.length - start, Math.max(0, len));
        return advance(len, new String(bytes, start, length));
    }

    public byte readByte() {
        // Retain Scala's null-to-zero unboxing if an override of advance returns null.
        return BoxesRunTime.unboxToByte(advance(1, BoxesRunTime.boxToByte(bc().arr()[pos()])));
    }

    public int readNat() {
        int r = 0;
        int b = 0;
        do {
            b = readByte();
            r = r << 7 | b & 0x7f;
        } while ((b & 0x80) != 0);
        return r;
    }

    public long readLong() {
        long l = 0L;
        while (more()) {
            l <<= 8;
            l |= readByte() & 0xff;
        }
        return l;
    }

    public <A> A advance(int len, A r) {
        if (pos() + len > bc().pos() + bc().len()) {
            throw new IllegalArgumentException("Ran off the end of bytecode");
        }
        pos_$eq(pos() + len);
        return r;
    }
}
