/*
 * Scala API (c) 2007-2013, LAMP/EPFL http://scala-lang.org/
 */
package codechicken.multipart.asm;

public final class ByteCodecs$ {

    public static final ByteCodecs$ MODULE$ = new ByteCodecs$();

    private ByteCodecs$() {}

    public byte[] avoidZero(byte[] src) {
        int i = 0;
        int srclen = src.length;
        int count = 0;
        while (i < srclen) {
            if (src[i] == 0x7f) count += 1;
            i += 1;
        }
        byte[] dst = new byte[srclen + count];
        i = 0;
        int j = 0;
        while (i < srclen) {
            byte in = src[i];
            if (in == 0x7f) {
                dst[j] = (byte) 0xc0;
                dst[j + 1] = (byte) 0x80;
                j += 2;
            } else {
                dst[j] = (byte) (in + 1);
                j += 1;
            }
            i += 1;
        }
        return dst;
    }

    public int regenerateZero(byte[] src) {
        int i = 0;
        int srclen = src.length;
        int j = 0;
        while (i < srclen) {
            int in = src[i] & 0xff;
            // A trailing 0xc0 deliberately retains the reference's unchecked failure and prior writes.
            if (in == 0xc0 && (src[i + 1] & 0xff) == 0x80) {
                src[j] = 0x7f;
                i += 2;
            } else if (in == 0) {
                src[j] = 0x7f;
                i += 1;
            } else {
                src[j] = (byte) (in - 1);
                i += 1;
            }
            j += 1;
        }
        return j;
    }

    public byte[] encode8to7(byte[] src) {
        int srclen = src.length;
        int dstlen = (srclen * 8 + 6) / 7;
        byte[] dst = new byte[dstlen];
        int i = 0;
        int j = 0;
        while (i + 6 < srclen) {
            int in = src[i] & 0xff;
            dst[j] = (byte) (in & 0x7f);
            int out = in >>> 7;
            in = src[i + 1] & 0xff;
            dst[j + 1] = (byte) (out | (in << 1) & 0x7f);
            out = in >>> 6;
            in = src[i + 2] & 0xff;
            dst[j + 2] = (byte) (out | (in << 2) & 0x7f);
            out = in >>> 5;
            in = src[i + 3] & 0xff;
            dst[j + 3] = (byte) (out | (in << 3) & 0x7f);
            out = in >>> 4;
            in = src[i + 4] & 0xff;
            dst[j + 4] = (byte) (out | (in << 4) & 0x7f);
            out = in >>> 3;
            in = src[i + 5] & 0xff;
            dst[j + 5] = (byte) (out | (in << 5) & 0x7f);
            out = in >>> 2;
            in = src[i + 6] & 0xff;
            dst[j + 6] = (byte) (out | (in << 6) & 0x7f);
            out = in >>> 1;
            dst[j + 7] = (byte) out;
            i += 7;
            j += 8;
        }
        if (i < srclen) {
            int in = src[i] & 0xff;
            dst[j] = (byte) (in & 0x7f);
            j += 1;
            int out = in >>> 7;
            if (i + 1 < srclen) {
                in = src[i + 1] & 0xff;
                dst[j] = (byte) (out | (in << 1) & 0x7f);
                j += 1;
                out = in >>> 6;
                if (i + 2 < srclen) {
                    in = src[i + 2] & 0xff;
                    dst[j] = (byte) (out | (in << 2) & 0x7f);
                    j += 1;
                    out = in >>> 5;
                    if (i + 3 < srclen) {
                        in = src[i + 3] & 0xff;
                        dst[j] = (byte) (out | (in << 3) & 0x7f);
                        j += 1;
                        out = in >>> 4;
                        if (i + 4 < srclen) {
                            in = src[i + 4] & 0xff;
                            dst[j] = (byte) (out | (in << 4) & 0x7f);
                            j += 1;
                            out = in >>> 3;
                            if (i + 5 < srclen) {
                                in = src[i + 5] & 0xff;
                                dst[j] = (byte) (out | (in << 5) & 0x7f);
                                j += 1;
                                out = in >>> 2;
                            }
                        }
                    }
                }
            }
            if (j < dstlen) dst[j] = (byte) out;
        }
        return dst;
    }

    public int decode7to8(byte[] src, int srclen) {
        int i = 0;
        int j = 0;
        int dstlen = (srclen * 7 + 7) / 8;
        while (i + 7 < srclen) {
            // Keep signed byte promotion, including the unsigned shifts of malformed high-bit input.
            int out = src[i];
            byte in = src[i + 1];
            src[j] = (byte) (out | (in & 0x01) << 7);
            out = in >>> 1;
            in = src[i + 2];
            src[j + 1] = (byte) (out | (in & 0x03) << 6);
            out = in >>> 2;
            in = src[i + 3];
            src[j + 2] = (byte) (out | (in & 0x07) << 5);
            out = in >>> 3;
            in = src[i + 4];
            src[j + 3] = (byte) (out | (in & 0x0f) << 4);
            out = in >>> 4;
            in = src[i + 5];
            src[j + 4] = (byte) (out | (in & 0x1f) << 3);
            out = in >>> 5;
            in = src[i + 6];
            src[j + 5] = (byte) (out | (in & 0x3f) << 2);
            out = in >>> 6;
            in = src[i + 7];
            src[j + 6] = (byte) (out | in << 1);
            i += 8;
            j += 7;
        }
        if (i < srclen) {
            int out = src[i];
            if (i + 1 < srclen) {
                byte in = src[i + 1];
                src[j] = (byte) (out | (in & 0x01) << 7);
                j += 1;
                out = in >>> 1;
                if (i + 2 < srclen) {
                    in = src[i + 2];
                    src[j] = (byte) (out | (in & 0x03) << 6);
                    j += 1;
                    out = in >>> 2;
                    if (i + 3 < srclen) {
                        in = src[i + 3];
                        src[j] = (byte) (out | (in & 0x07) << 5);
                        j += 1;
                        out = in >>> 3;
                        if (i + 4 < srclen) {
                            in = src[i + 4];
                            src[j] = (byte) (out | (in & 0x0f) << 4);
                            j += 1;
                            out = in >>> 4;
                            if (i + 5 < srclen) {
                                in = src[i + 5];
                                src[j] = (byte) (out | (in & 0x1f) << 3);
                                j += 1;
                                out = in >>> 5;
                                if (i + 6 < srclen) {
                                    in = src[i + 6];
                                    src[j] = (byte) (out | (in & 0x3f) << 2);
                                    j += 1;
                                    out = in >>> 6;
                                }
                            }
                        }
                    }
                }
            }
            if (j < dstlen) src[j] = (byte) out;
        }
        return dstlen;
    }

    public byte[] encode(byte[] xs) {
        return avoidZero(encode8to7(xs));
    }

    /**
     * Destructively decodes xs and returns its decoded length, possibly including an extra zero padding byte. For
     * example, encoding {1, 2, 3} gives {2, 5, 13, 1}, which decodes to {1, 2, 3, 0} with length 4.
     */
    public int decode(byte[] xs) {
        int len = regenerateZero(xs);
        return decode7to8(xs, len);
    }
}
