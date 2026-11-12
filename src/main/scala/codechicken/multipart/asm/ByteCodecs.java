package codechicken.multipart.asm;

public final class ByteCodecs {

    private ByteCodecs() {}

    public static byte[] avoidZero(byte[] src) {
        return ByteCodecs$.MODULE$.avoidZero(src);
    }

    public static int regenerateZero(byte[] src) {
        return ByteCodecs$.MODULE$.regenerateZero(src);
    }

    public static byte[] encode8to7(byte[] src) {
        return ByteCodecs$.MODULE$.encode8to7(src);
    }

    public static int decode7to8(byte[] src, int srclen) {
        return ByteCodecs$.MODULE$.decode7to8(src, srclen);
    }

    public static byte[] encode(byte[] xs) {
        return ByteCodecs$.MODULE$.encode(xs);
    }

    public static int decode(byte[] xs) {
        return ByteCodecs$.MODULE$.decode(xs);
    }
}
