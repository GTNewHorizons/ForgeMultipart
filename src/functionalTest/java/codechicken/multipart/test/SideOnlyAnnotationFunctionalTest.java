package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ScalaSigReader;
import codechicken.multipart.asm.ScalaSignature;
import codechicken.multipart.asm.ScalaSignature.AnnotationInfo;
import codechicken.multipart.asm.ScalaSignature.Bytes$;
import codechicken.multipart.asm.ScalaSignature.Literal;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import scala.collection.Iterator;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.IndexedSeq$;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set;
import scala.collection.mutable.Builder;

class SideOnlyAnnotationFunctionalTest {

    private static final String ANNOTATION = "cpw.mods.fml.relauncher.SideOnly";
    private static final String SIDE = "cpw.mods.fml.relauncher.Side.";

    @Test
    void readsSideAnnotationsFromACompiledScalaSignature() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass()
                .getResourceAsStream("/codechicken/multipart/test/SideOnlySignatureFixture.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        ScalaSignature sig = ScalaSigReader.read(ScalaSigReader.ann(node).get());
        String excluded = FMLLaunchHandler.side().isServer() ? "clientOnly" : "serverOnly";
        assertEquals(
                new HashSet<>(Arrays.asList("codechicken.multipart.test.SideOnlySignatureFixture." + excluded)),
                values(ASMMixinCompiler.listSideOnly(sig)));
    }

    @Test
    void filtersExactNamesAndTheCurrentSideThenDeduplicatesOwnerNames() {
        Fixture sig = new Fixture();
        String other = FMLLaunchHandler.side().isServer() ? "CLIENT" : "SERVER";
        sig.add(sig.annotation("duplicate", ANNOTATION, sig.enumValue(SIDE + other)));
        sig.add(sig.annotation("duplicate", ANNOTATION, sig.enumValue(SIDE + other)));
        sig.add(sig.annotation("current", ANNOTATION, sig.enumValue(SIDE + FMLLaunchHandler.side().name())));
        sig.add(sig.annotation("unknown", ANNOTATION, sig.enumValue(SIDE + "UNKNOWN")));
        sig.add(sig.annotation("null-enum-name", ANNOTATION, sig.enumValue(null)));
        sig.add(sig.annotation(null, ANNOTATION, sig.enumValue(SIDE + other)));
        sig.add(sig.annotation("not-exact", ANNOTATION + "Suffix", null));
        sig.add(sig.annotation("null-annotation-name", null, null));
        Set<String> result = ASMMixinCompiler$.MODULE$.listSideOnly(sig);
        assertEquals(new HashSet<>(Arrays.asList("duplicate", "unknown", "null-enum-name", null)), values(result));
        assertEquals(Arrays.asList("collect:40"), sig.events);
        sig.entries.clear();
        assertTrue(ASMMixinCompiler.listSideOnly(sig).isEmpty());
        assertEquals(4, result.size());
    }

    @Test
    void filtersEveryAnnotationBeforeReadingAnySelectedOwnerName() {
        Fixture sig = new Fixture();
        String[] firstOwner = { "before" };
        ScalaSignature.ExternalSymbol owner = sig.new ExternalSymbol("unused") {

            @Override
            public String full() {
                sig.events.add("owner:first");
                return firstOwner[0];
            }
        };
        ScalaSignature.ThisType firstType = sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)) {

            @Override
            public String name() {
                sig.events.add("type:first");
                return super.name();
            }
        };
        ScalaSignature.ThisType secondType = sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)) {

            @Override
            public String name() {
                sig.events.add("type:second");
                firstOwner[0] = "after";
                return super.name();
            }
        };
        ScalaSignature.ExternalSymbol secondOwner = sig.new ExternalSymbol("second") {

            @Override
            public String full() {
                sig.events.add("owner:second");
                return super.full();
            }
        };
        Map<String, Literal> values = new Map.Map1<>("value", sig.enumValue("other"));
        sig.add(sig.AnnotationInfo().apply(owner, firstType, values));
        sig.add(sig.AnnotationInfo().apply(secondOwner, secondType, values));
        assertEquals(new HashSet<>(Arrays.asList("after", "second")), values(ASMMixinCompiler.listSideOnly(sig)));
        assertEquals(
                Arrays.asList("collect:40", "type:first", "type:second", "owner:first", "owner:second"),
                sig.events);
    }

    @Test
    void aLaterPredicateFailurePreventsAllOwnerReadsAndPropagatesTheFailure() {
        Fixture sig = new Fixture();
        ScalaSignature.ExternalSymbol owner = sig.new ExternalSymbol("first") {

            @Override
            public String full() {
                fail("Owner mapping must wait until filtering is complete");
                return null;
            }
        };
        sig.add(
                sig.AnnotationInfo().apply(
                        owner,
                        sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)),
                        new Map.Map1<>("value", sig.enumValue("other"))));
        IllegalStateException failure = new IllegalStateException("annotation accessor failed");
        ScalaSignature.ThisType failing = sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)) {

            @Override
            public String name() {
                throw failure;
            }
        };
        sig.add(sig.AnnotationInfo().apply(null, failing, null));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ASMMixinCompiler.listSideOnly(sig)));
    }

    @Test
    void preservesMissingWrongAndNullValueFailuresButShortCircuitsUnrelatedAnnotations() {
        Fixture sig = new Fixture();
        sig.add(sig.AnnotationInfo().apply(null, sig.new ThisType(sig.new ExternalSymbol("unrelated")), null));
        sig.add(
                sig.AnnotationInfo().apply(
                        null,
                        sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)),
                        new Map.Map1<>("value", sig.enumValue(SIDE + FMLLaunchHandler.side().name()))));
        assertTrue(ASMMixinCompiler.listSideOnly(sig).isEmpty());
        sig.entries.clear();
        sig.add(
                sig.AnnotationInfo().apply(
                        sig.new ExternalSymbol("missing"),
                        sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)),
                        Map$.MODULE$.empty()));
        assertThrows(NoSuchElementException.class, () -> ASMMixinCompiler.listSideOnly(sig));
        sig.entries.clear();
        sig.add(sig.annotation("wrong", ANNOTATION, sig.new StringLiteral("CLIENT")));
        assertThrows(ClassCastException.class, () -> ASMMixinCompiler.listSideOnly(sig));
        sig.entries.clear();
        sig.add(sig.annotation("null-literal", ANNOTATION, null));
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.listSideOnly(sig));
        sig.entries.clear();
        sig.add(sig.annotation("null-symbol", ANNOTATION, sig.new EnumLiteral(null)));
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.listSideOnly(sig));
        sig.entries.clear();
        sig.add(
                sig.AnnotationInfo().apply(
                        null,
                        sig.new ThisType(sig.new ExternalSymbol(ANNOTATION)),
                        new Map.Map1<>("value", sig.enumValue("other"))));
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.listSideOnly(sig));
    }

    @Test
    void handlesEmptyCollectionsAndRejectsNullInputsWithoutInventingDefaults() {
        Fixture sig = new Fixture();
        assertTrue(ASMMixinCompiler.listSideOnly(sig).isEmpty());
        assertEquals(Arrays.asList("collect:40"), sig.events);
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.listSideOnly(null));
        sig.add(null);
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.listSideOnly(sig));
        sig.entries.clear();
        sig.add(sig.AnnotationInfo().apply(null, null, null));
        assertThrows(NullPointerException.class, () -> ASMMixinCompiler.listSideOnly(sig));
    }

    private static java.util.Set<String> values(Set<String> values) {
        java.util.Set<String> result = new HashSet<>();
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }

    private static class Fixture extends ScalaSignature {

        final List<AnnotationInfo> entries = new ArrayList<>();
        final List<String> events = new ArrayList<>();

        Fixture() {
            super(Bytes$.MODULE$.apply(new byte[] { 5, 0, 0 }));
        }

        void add(AnnotationInfo annotation) {
            entries.add(annotation);
        }

        EnumLiteral enumValue(String full) {
            return new EnumLiteral(new ExternalSymbol(full));
        }

        AnnotationInfo annotation(String owner, String type, Literal value) {
            return AnnotationInfo().apply(
                    new ExternalSymbol(owner),
                    new ThisType(new ExternalSymbol(type)),
                    new Map.Map1<>("value", value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> IndexedSeq<T> collect(int id) {
            events.add("collect:" + id);
            Builder<T, IndexedSeq<T>> builder = IndexedSeq$.MODULE$.newBuilder();
            for (AnnotationInfo annotation : entries) builder.$plus$eq((T) annotation);
            return builder.result();
        }
    }
}
