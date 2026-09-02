package codechicken.multipart.asm;

import java.util.Objects;

import codechicken.multipart.asm.ScalaSignature.Bytes;
import scala.MatchError;
import scala.None$;
import scala.Option;
import scala.Some;
import scala.collection.Iterator;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.IndexedSeq$;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;
import scala.collection.mutable.Builder;

/** Parser implementation behind the retained Scala case-class and path-dependent API. */
final class ScalaSignatureParser {

    private ScalaSignatureParser() {}

    // Keep path-dependent model types out of signatures read by Scala 2.11's Java parser.
    static Object readTable(ScalaSignature sig, Bytes bytes) {
        ByteCodeReader reader = bytes.reader();
        reader.pos_$eq(2);
        // Scala Array.tabulate produces an empty array for a negative (overflowed) count.
        ScalaSignature.SigEntry[] table = new ScalaSignature.SigEntry[Math.max(0, reader.readNat())];
        for (int i = 0; i < table.length; i++) {
            int start = reader.pos();
            reader.readByte();
            int length = reader.readNat();
            table[i] = reader
                    .advance(length, sig.new SigEntry(i, start, new Bytes(sig.bytes().arr(), reader.pos(), length)));
        }
        return table;
    }

    static String evalS(ScalaSignature sig, int index) {
        ScalaSignature.SigEntry entry = sig.table()[index];
        Bytes bytes = entry.bytes();
        ByteCodeReader reader = bytes.reader();
        byte id = entry.id();
        switch (id) {
            case 1:
            case 2:
                return reader.readString(bytes.len());
            case 3:
                return sig.NoSymbol().full();
            case 9:
            case 10:
                String name = sig.evalS(reader.readNat());
                if (bytes.pos() + bytes.len() > reader.pos()) {
                    name = sig.evalS(reader.readNat()) + "." + name;
                }
                return name;
            default:
                throw new MatchError(Byte.valueOf(id));
        }
    }

    static List<?> evalList(ScalaSignature sig, ByteCodeReader reader) {
        Builder<Object, List<Object>> builder = List$.MODULE$.newBuilder();
        while (reader.more()) {
            builder.$plus$eq(sig.evalT(reader.readNat()));
        }
        return builder.result();
    }

    static Object eval(ScalaSignature sig, int index, Object entry, ByteCodeReader reader, byte id) {
        // The Scala optimizer constructs case classes directly, without initializing their companions.
        switch (id) {
            case 1:
            case 2:
                return sig.evalS(index);
            case 3:
                return sig.NoSymbol();
            case 6:
                return sig.new ClassSymbol(sig.evalS(reader.readNat()), sig.evalT(reader.readNat()), reader.readNat(),
                        reader.readNat());
            case 7:
                return sig.new ObjectSymbol(sig.evalS(reader.readNat()), sig.evalT(reader.readNat()), reader.readNat(),
                        reader.readNat());
            case 8:
                return sig.new MethodSymbol(sig.evalS(reader.readNat()), sig.evalT(reader.readNat()), reader.readNat(),
                        reader.readNat());
            case 9:
            case 10:
                return sig.new ExternalSymbol(sig.evalS(index));
            case 11:
            case 12: // NoPrefixType is treated as NoType in the reference.
                return sig.NoType();
            case 13:
                return sig.new ThisType(sig.evalT(reader.readNat()));
            case 14:
                return sig.new SingleType(sig.evalT(reader.readNat()), sig.evalT(reader.readNat()));
            case 21:
            case 48: // Retain the reference's parameterless interpretation of a bounded super type.
                return sig.new ParameterlessType(sig.evalT(reader.readNat()));
            case 25:
                return sig.new BooleanLiteral(reader.readLong() != 0);
            case 26:
                return sig.new ByteLiteral((byte) reader.readLong());
            case 27:
                return sig.new ShortLiteral((short) reader.readLong());
            case 28:
                return sig.new CharLiteral((char) reader.readLong());
            case 29:
                return sig.new IntLiteral((int) reader.readLong());
            case 30:
                return sig.new LongLiteral(reader.readLong());
            case 31:
                return sig.new FloatLiteral(Float.intBitsToFloat((int) reader.readLong()));
            case 32:
                return sig.new DoubleLiteral(Double.longBitsToDouble(reader.readLong()));
            case 33:
                return sig.new StringLiteral(sig.evalS(reader.readNat()));
            case 34:
                return sig.NullLiteral();
            case 35:
                return sig.new TypeLiteral(sig.evalT(reader.readNat()));
            case 36:
                return sig.new EnumLiteral(sig.evalT(reader.readNat()));
            default:
                return entry;
        }
    }

    static <T> IndexedSeq<T> collect(ScalaSignature sig, int id) {
        int length = sig.table().length;
        Builder<T, IndexedSeq<T>> builder = IndexedSeq$.MODULE$.newBuilder();
        for (int i = 0; i < length; i++) {
            if (sig.table()[i].id() == id) builder.$plus$eq(sig.evalT(i));
        }
        return builder.result();
    }

    static Option<?> findObject(ScalaSignature sig, String name) {
        Iterator<ScalaSignature.ObjectSymbol> symbols = sig.<ScalaSignature.ObjectSymbol>collect(7).iterator();
        while (symbols.hasNext()) {
            ScalaSignature.ObjectSymbol symbol = symbols.next();
            if (Objects.equals(symbol.full(), name)) return new Some<>(symbol);
        }
        return None$.MODULE$;
    }

    static Option<?> findClass(ScalaSignature sig, String name) {
        Iterator<ScalaSignature.ClassSymbol> symbols = sig.<ScalaSignature.ClassSymbol>collect(6).iterator();
        while (symbols.hasNext()) {
            ScalaSignature.ClassSymbol symbol = symbols.next();
            if (!symbol.isModule() && Objects.equals(symbol.full(), name)) return new Some<>(symbol);
        }
        return None$.MODULE$;
    }
}
