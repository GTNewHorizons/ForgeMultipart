package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.asm.ScalaSignature.Bytes$;
import codechicken.multipart.asm.ScalaSignature.ClassSymbol;
import codechicken.multipart.asm.ScalaSignature.MethodSymbol;
import codechicken.multipart.asm.ScalaSignature.SigEntry;
import scala.collection.Iterator;
import scala.collection.immutable.IndexedSeq;

/** Drives the parser through the retained Scala shell, which is the only surface the compiler uses. */
class ScalaSignatureParserCharacterizationTest {

    private static final String FIXTURE = "codechicken.multipart.compat.ReferenceScalaEdgePart";

    @Test
    void readsTheTableHeaderAndEntryLayoutOfAFrozenScalaClass() {
        ScalaSignature sig = fixtureSignature();

        assertEquals(5, sig.major());
        assertEquals(0, sig.minor());
        assertEquals(42, sig.table().length);
        assertEquals("SigEntry(0,6,4 bytes)", sig.table()[0].toString());
        assertEquals(6, sig.table()[0].id());
        assertEquals(0, sig.table()[0].index());
        for (int i = 0; i < sig.table().length; i++) {
            assertEquals(i, sig.table()[i].index());
            assertTrue(sig.table()[i].bytes().pos() > sig.table()[i].start());
        }
    }

    @Test
    void evaluatesNamesSymbolsAndTheClassHierarchy() {
        ScalaSignature sig = fixtureSignature();

        assertEquals("ReferenceScalaEdgePart", sig.evalS(1));
        assertEquals("codechicken.multipart.compat", sig.evalS(2));
        assertEquals("<no symbol>", sig.evalS(8));
        assertEquals("NoSymbol", sig.eval(8).toString());
        assertSame(sig.NoSymbol(), sig.eval(8));

        ClassSymbol symbol = sig.findClass(FIXTURE).get();
        assertEquals("ClassSymbol(ReferenceScalaEdgePart,codechicken.multipart.compat,0,9)", symbol.toString());
        assertEquals(FIXTURE, symbol.full());
        assertEquals(0, symbol.flags());
        assertFalse(symbol.isTrait());
        assertFalse(symbol.isModule());
        assertFalse(symbol.isObject());
        assertEquals("codechicken/multipart/TMultiPart", symbol.jParent());
        assertEquals("List(codechicken/multipart/TEdgePart)", symbol.jInterfaces().toString());

        assertFalse(sig.findClass("codechicken.multipart.compat.Missing").isDefined());
        assertFalse(sig.findObject(FIXTURE).isDefined());
        assertEquals(1, sig.collect(6).size());
        assertEquals(0, sig.collect(7).size());
    }

    @Test
    void collectsMethodSymbolsInTableOrderWithJavaDescriptors() {
        ScalaSignature sig = fixtureSignature();

        IndexedSeq<MethodSymbol> methods = sig.collect(8);
        assertEquals(3, methods.size());
        List<String> actual = new ArrayList<>();
        Iterator<MethodSymbol> iterator = methods.iterator();
        while (iterator.hasNext()) {
            MethodSymbol method = iterator.next();
            assertTrue(method.isMethod());
            assertFalse(method.isPrivate());
            actual.add(method.full() + method.jDesc());
        }
        assertEquals(
                Arrays.asList(
                        FIXTURE + ".<init>()Lcodechicken/multipart/compat/ReferenceScalaEdgePart;",
                        FIXTURE + ".getType()Ljava/lang/String;",
                        FIXTURE + ".getSlotMask()I"),
                actual);
    }

    @Test
    void deletingAnEntryRewritesItsTagInPlace() {
        ScalaSignature sig = fixtureSignature();
        SigEntry entry = sig.table()[22];
        assertEquals(8, entry.id());

        entry.delete();

        assertEquals(3, entry.id());
        assertSame(sig.NoSymbol(), sig.eval(22));
        assertEquals(2, sig.collect(8).size());
        // The rewrite lands in the shared array the signature re-encodes from.
        assertEquals(3, sig.bytes().arr()[entry.start()]);
    }

    @Test
    void evaluatesLiteralsAndUnknownTagsFromASyntheticTable() {
        ScalaSignature sig = new ScalaSignature(
                Bytes$.MODULE$.apply(
                        table(
                                entry(25, 0),
                                entry(25, 1),
                                entry(26, 0x01, 0x02),
                                entry(27, 0x01, 0x00, 0x00),
                                entry(28, 0x00, 0x41),
                                entry(29, 0xff, 0xff, 0xff, 0xff),
                                entry(30, 0x01, 0x00),
                                entry(31, 0x3f, 0xc0, 0x00, 0x00),
                                entry(32, 0x40, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                                entry(34),
                                entry(11),
                                entry(12),
                                entry(99))));

        assertEquals(5, sig.major());
        assertEquals(0, sig.minor());
        assertEquals(13, sig.table().length);

        assertEquals("BooleanLiteral(false)", sig.eval(0).toString());
        assertEquals("BooleanLiteral(true)", sig.eval(1).toString());
        assertEquals("ByteLiteral(2)", sig.eval(2).toString());
        assertEquals("ShortLiteral(0)", sig.eval(3).toString());
        assertEquals("CharLiteral(A)", sig.eval(4).toString());
        assertEquals("IntLiteral(-1)", sig.eval(5).toString());
        assertEquals("LongLiteral(256)", sig.eval(6).toString());
        assertEquals("FloatLiteral(1.5)", sig.eval(7).toString());
        assertEquals("DoubleLiteral(2.5)", sig.eval(8).toString());
        assertEquals("NullLiteral", sig.eval(9).toString());

        // 12 is a bounded super type read as NoType, like the reference.
        assertSame(sig.NoType(), sig.eval(10));
        assertSame(sig.NoType(), sig.eval(11));
        assertEquals("<no type>", sig.NoType().name());

        // An unrecognised tag evaluates to its own table entry.
        assertSame(sig.table()[12], sig.eval(12));
        assertEquals("SigEntry(12,99,0 bytes)", sig.eval(12).toString());
    }

    @Test
    void reportsAnUnreadableTagRatherThanGuessing() {
        ScalaSignature sig = new ScalaSignature(Bytes$.MODULE$.apply(table(entry(99))));
        assertThrows(scala.MatchError.class, () -> sig.evalS(0));
    }

    private static ScalaSignature fixtureSignature() {
        ClassNode cnode = new ClassNode();
        new ClassReader(fixture()).accept(cnode, 0);
        return ScalaSigReader.read(ScalaSigReader.ann(cnode).get());
    }

    private static byte[] entry(int id, int... payload) {
        byte[] bytes = new byte[payload.length + 2];
        bytes[0] = (byte) id;
        bytes[1] = (byte) payload.length;
        for (int i = 0; i < payload.length; i++) {
            bytes[i + 2] = (byte) payload[i];
        }
        return bytes;
    }

    private static byte[] table(byte[]... entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(5);
        out.write(0);
        out.write(entries.length);
        for (byte[] entry : entries) {
            out.write(entry, 0, entry.length);
        }
        return out.toByteArray();
    }

    private static byte[] fixture() {
        InputStream input = Objects.requireNonNull(
                ScalaSignatureParserCharacterizationTest.class
                        .getResourceAsStream("/compat/ReferenceScalaEdgePart.class.b64"));
        try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
            return Base64.getMimeDecoder().decode(scanner.next());
        }
    }
}
