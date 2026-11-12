package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.FieldMixin;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import codechicken.multipart.asm.ScalaSigReader;
import codechicken.multipart.asm.ScalaSignature;
import codechicken.multipart.asm.ScalaSignature.Bytes$;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.IndexedSeq$;
import scala.collection.immutable.Map.Map1;
import scala.collection.mutable.Builder;
import scala.collection.mutable.Map;

class ScalaTraitRegistrationFunctionalTest {

    private static final ASMMixinCompiler$ COMPILER = ASMMixinCompiler$.MODULE$;
    private static final String PREFIX = "codechicken/multipart/test/registration/";
    private static final int TRAIT = 0x02000000;
    private static final int METHOD = 0x200;
    private static final int ACCESSOR = 0x08000000;

    @Test
    void registersRealScalaFieldsMethodsParentsAndSideAnnotations() throws Exception {
        String pkg = "codechicken/multipart/test/";
        try (Scope scope = new Scope()) {
            for (String name : Arrays.asList(
                    "RegistrationParentA",
                    "RegistrationParentB",
                    "RegistrationInterface",
                    "ScalaTraitRegistrationFixture")) {
                scope.remember(pkg + name);
                ClassNode node = raw(pkg + name);
                ScalaSignature sig = ScalaSigReader.read(ScalaSigReader.ann(node).get());
                scope.cache.put(node.name, scalaInfo(node, sig, sig.findClass(node.name.replace('/', '.')).get()));
            }
            ClassNode node = raw(pkg + "ScalaTraitRegistrationFixture");
            MixinInfo info = ASMMixinCompiler.registerScalaTrait(node);
            assertEquals(node.name, info.name());
            assertEquals("java/lang/Object", info.parent());
            assertEquals(Arrays.asList(pkg + "RegistrationParentA", pkg + "RegistrationParentB"), parentNames(info));
            assertFalse(COMPILER.getMixinInfo(pkg + "RegistrationInterface").isDefined());
            assertEquals(Arrays.asList("count:I:1", "hidden:I:2"), fields(info));
            List<String> expected = new ArrayList<>(
                    Arrays.asList(
                            "concrete()I",
                            "overloaded(I)I",
                            "overloaded(Ljava/lang/String;)I",
                            "protectedMethod()I",
                            "toString()Ljava/lang/String;",
                            FMLLaunchHandler.side().isServer() ? "serverOnly()I" : "clientOnly()I"));
            List<String> actual = methods(info);
            Collections.sort(expected);
            Collections.sort(actual);
            assertEquals(expected, actual);
            for (MethodNode method : values(info.methods())) assertTrue(node.methods.contains(method));
            assertEquals(Arrays.asList("toString()Ljava/lang/String;"), values(info.supers()));
            assertSame(info, COMPILER.getMixinInfo(node.name).get());
            node.methods.clear();
            node.interfaces = null;
            assertSame(info, COMPILER.registerScalaTrait(node));
        }
    }

    @Test
    void preservesTheExistingPredefStringAliasDescriptorFailure() throws Exception {
        String name = "codechicken/multipart/test/RegistrationAliasFixture";
        try (Scope scope = new Scope()) {
            scope.remember(name);
            ClassNode node = raw(name);
            ScalaSignature sig = ScalaSigReader.read(ScalaSigReader.ann(node).get());
            scope.cache.put(name, scalaInfo(node, sig, sig.findClass(name.replace('/', '.')).get()));
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> COMPILER.registerScalaTrait(node));
            assertEquals(
                    "Unable to add mixin trait " + name
                            + ": alias(Lscala/Predef/String;)I found in scala signature but not in class file. Most likely an obfuscation issue.",
                    failure.getMessage());
            assertFalse(COMPILER.getMixinInfo(name).isDefined());
        }
    }

    @Test
    void cachedValuesIncludingNullBypassMetadataAndMalformedNodes() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode node = node("cached");
            scope.remember(node.name);
            MixinInfo cached = new MixinInfo("cached-value", null, null, null, null, null);
            scope.mixins.put(node.name, cached);
            node.interfaces = null;
            node.methods = null;
            assertSame(cached, COMPILER.registerScalaTrait(node));
            scope.mixins.put(node.name, null);
            assertNull(COMPILER.registerScalaTrait(node));
            assertThrows(NullPointerException.class, () -> COMPILER.registerScalaTrait(null));
        }
    }

    @Test
    void resolvesAllParentsBeforeRegisteringAndPreservesOrderAndDuplicates() throws Exception {
        try (Scope scope = new Scope()) {
            List<String> events = new ArrayList<>();
            Fixture first = scope.fixture("first", TRAIT, events);
            Fixture second = scope.fixture("second", TRAIT, events);
            Fixture plain = scope.fixture("plain", 0, events);
            Fixture contract = scope.fixture("contract", TRAIT | 0x800, events);
            String missing = PREFIX + "null-parent";
            scope.remember(missing);
            scope.cache.put(missing, null);
            ClassNode child = node("parent-list");
            List<String> names = Arrays.asList(
                    first.node.name,
                    plain.node.name,
                    contract.node.name,
                    missing,
                    "java/lang/Runnable",
                    second.node.name,
                    first.node.name);
            child.interfaces = new AbstractList<String>() {

                @Override
                public String get(int index) {
                    events.add("lookup:" + index);
                    return names.get(index);
                }

                @Override
                public int size() {
                    return names.size();
                }
            };
            Seq<MixinInfo> parents = ASMMixinCompiler.getAndRegisterParentTraits(child);
            assertEquals(Arrays.asList(first.node.name, second.node.name, first.node.name), names(parents));
            assertSame(parents.apply(0), parents.apply(2));
            assertEquals(
                    Arrays.asList(
                            "lookup:0",
                            "lookup:1",
                            "lookup:2",
                            "lookup:3",
                            "lookup:4",
                            "lookup:5",
                            "lookup:6",
                            "first:40",
                            "first:8",
                            "second:40",
                            "second:8"),
                    events);
            assertFalse(COMPILER.getMixinInfo(plain.node.name).isDefined());
            assertFalse(COMPILER.getMixinInfo(contract.node.name).isDefined());
        }
    }

    @Test
    void parentLookupFailureHappensBeforeAnyRegistration() throws Exception {
        try (Scope scope = new Scope()) {
            List<String> events = new ArrayList<>();
            Fixture first = scope.fixture("lookup-failure-first", TRAIT, events);
            ClassNode child = node("lookup-failure-child");
            IllegalStateException failure = new IllegalStateException("second interface read");
            child.interfaces = new AbstractList<String>() {

                @Override
                public String get(int index) {
                    if (index == 0) return first.node.name;
                    throw failure;
                }

                @Override
                public int size() {
                    return 2;
                }
            };
            assertSame(
                    failure,
                    assertThrows(IllegalStateException.class, () -> COMPILER.getAndRegisterParentTraits(child)));
            assertTrue(events.isEmpty());
            assertFalse(COMPILER.getMixinInfo(first.node.name).isDefined());
        }
    }

    @Test
    void failedRegistrationKeepsCompletedParentsButDoesNotCacheTheChild() throws Exception {
        try (Scope scope = new Scope()) {
            Fixture first = scope.fixture("partial-first");
            Fixture second = scope.fixture("partial-second");
            Fixture child = scope.fixture("partial-child");
            child.node.interfaces.addAll(Arrays.asList(first.node.name, second.node.name));
            second.entries.add(second.method("missing", METHOD, "()I"));
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> COMPILER.registerScalaTrait(child.node));
            assertEquals(
                    "Unable to add mixin trait " + second.node.name
                            + ": missing()I found in scala signature but not in class file. Most likely an obfuscation issue.",
                    failure.getMessage());
            MixinInfo completed = COMPILER.getMixinInfo(first.node.name).get();
            assertFalse(COMPILER.getMixinInfo(second.node.name).isDefined());
            assertFalse(COMPILER.getMixinInfo(child.node.name).isDefined());
            second.node.methods.add(method("missing", "()I"));
            MixinInfo result = COMPILER.registerScalaTrait(child.node);
            assertSame(completed, result.parentTraits().apply(0));
            assertSame(second.node.methods.get(0), result.parentTraits().apply(1).methods().apply(0));
        }
    }

    @Test
    void preservesSymbolFilteringEqualityAndFirstExactMethodIdentity() throws Exception {
        try (Scope scope = new Scope()) {
            Fixture sig = scope.fixture("symbols");
            sig.entries.add(sig.method("parameter", 0x2000, null));
            sig.entries.add(sig.new MethodSymbol("foreign", sig.new ExternalSymbol("elsewhere"), METHOD, 0));
            sig.entries.add(sig.method("private", METHOD | 4, "()V"));
            sig.entries.add(sig.method("deferred", METHOD | 0x100, "()V"));
            sig.entries.add(sig.method("$init$", METHOD, "()V"));
            sig.entries.add(sig.method("super$wide", METHOD | 4 | 0x100, "(JD)I"));
            ScalaSignature.ClassSymbol equalOwner = sig.new ClassSymbol("symbols", sig.symbol.owner(), TRAIT, 0);
            assertNotSame(sig.symbol, equalOwner);
            assertEquals(sig.symbol, equalOwner);
            sig.entries.add(sig.new MethodSymbol("chosen", equalOwner, METHOD, 0) {

                @Override
                public String jDesc() {
                    return "(I)I";
                }
            });
            sig.node.methods.add(method("chosen", "()I"));
            MethodNode chosen = method("chosen", "(I)I");
            chosen.access = ACC_PRIVATE | ACC_ABSTRACT;
            sig.node.methods.add(chosen);
            sig.node.methods.add(method("chosen", "(I)I"));
            MixinInfo info = COMPILER.registerScalaTrait(sig.node);
            assertEquals(1, info.methods().size());
            assertSame(chosen, info.methods().apply(0));
            assertEquals(Arrays.asList("wide(JD)I"), values(info.supers()));
            assertTrue(info.fields().isEmpty());
        }
    }

    @Test
    void sideFilteringPrecedesAccessorAndDescriptorReads() throws Exception {
        try (Scope scope = new Scope()) {
            Fixture sig = scope.fixture("side-filter");
            ScalaSignature.MethodSymbol excluded = sig.method("excluded", METHOD | ACCESSOR, null);
            sig.entries.add(excluded);
            sig.annotations.add(
                    sig.AnnotationInfo().apply(
                            excluded,
                            sig.new ThisType(sig.new ExternalSymbol("cpw.mods.fml.relauncher.SideOnly")),
                            new Map1<>("value", sig.new EnumLiteral(sig.new ExternalSymbol("unknown-side")))));
            MixinInfo info = COMPILER.registerScalaTrait(sig.node);
            assertTrue(info.fields().isEmpty());
            assertTrue(info.methods().isEmpty());
            assertTrue(info.supers().isEmpty());
        }
    }

    @Test
    void fieldsUseTheLatestPrecedingAccessorAndTrimmedNames() throws Exception {
        try (Scope scope = new Scope()) {
            Fixture sig = scope.fixture("fields");
            sig.entries.add(sig.method("hidden", ACCESSOR, null));
            sig.entries.add(sig.method("hidden", ACCESSOR | 4, null));
            sig.entries.add(sig.method(" hidden ", 0, "J"));
            sig.entries.add(sig.method("visible", ACCESSOR, null));
            sig.entries.add(sig.method("visible ", 4, "I"));
            assertEquals(Arrays.asList("hidden:J:2", "visible:I:1"), fields(COMPILER.registerScalaTrait(sig.node)));
        }
    }

    @Test
    void aFieldBeforeItsAccessorFailsWithoutPublishingMetadata() throws Exception {
        try (Scope scope = new Scope()) {
            Fixture sig = scope.fixture("missing-accessor");
            sig.entries.add(sig.method("value ", 0, "I"));
            sig.entries.add(sig.method("value", ACCESSOR, null));
            assertThrows(NoSuchElementException.class, () -> COMPILER.registerScalaTrait(sig.node));
            assertFalse(COMPILER.getMixinInfo(sig.node.name).isDefined());
        }
    }

    @Test
    void parentNameCallbackRunsAfterMemberCollectionAndBeforeThePublicationKeyRead() throws Exception {
        try (Scope scope = new Scope()) {
            Fixture sig = scope.fixture("publication");
            String original = sig.node.name;
            String changed = original + "Changed";
            scope.remember(changed);
            ScalaSignature.ClassSymbol symbol = sig.new ClassSymbol("publication", sig.symbol.owner(), TRAIT, 0) {

                @Override
                public String jParent() {
                    assertEquals(Arrays.asList("publication:40", "publication:8"), sig.events);
                    sig.node.name = changed;
                    return "chosen/Parent";
                }
            };
            scope.cache.put(original, scalaInfo(sig.node, sig, symbol));
            MixinInfo info = COMPILER.registerScalaTrait(sig.node);
            assertEquals(original, info.name());
            assertEquals("chosen/Parent", info.parent());
            assertFalse(COMPILER.getMixinInfo(original).isDefined());
            assertSame(info, COMPILER.getMixinInfo(changed).get());
        }
    }

    @Test
    void rejectsWrongOrNullMetadataWithoutCachingDefaults() throws Exception {
        try (Scope scope = new Scope()) {
            ClassNode wrong = node("wrong-metadata");
            scope.remember(wrong.name);
            scope.cache.put(wrong.name, COMPILER.getClassInfo("java/lang/Runnable"));
            assertThrows(ClassCastException.class, () -> COMPILER.registerScalaTrait(wrong));
            scope.cache.put(wrong.name, null);
            assertThrows(NullPointerException.class, () -> COMPILER.registerScalaTrait(wrong));
            assertFalse(COMPILER.getMixinInfo(wrong.name).isDefined());
        }
    }

    private static ClassNode raw(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = ScalaTraitRegistrationFunctionalTest.class
                .getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.name = PREFIX + name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode method(String name, String desc) {
        return new MethodNode(ACC_PUBLIC, name, desc, null, null);
    }

    private static ClassInfo scalaInfo(ClassNode node, ScalaSignature sig, ScalaSignature.ClassSymbolRef symbol)
            throws Exception {
        return (ClassInfo) Class.forName("codechicken.multipart.asm.ASMMixinCompiler$ClassInfo$ScalaClassInfo")
                .getConstructor(ClassNode.class, ScalaSignature.class, ScalaSignature.ClassSymbolRef.class)
                .newInstance(node, sig, symbol);
    }

    private static <T> List<T> values(Seq<T> sequence) {
        List<T> result = new ArrayList<>();
        Iterator<T> iterator = sequence.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }

    private static List<String> parentNames(MixinInfo info) {
        return names(info.parentTraits());
    }

    private static List<String> names(Seq<MixinInfo> sequence) {
        List<String> result = new ArrayList<>();
        for (MixinInfo info : values(sequence)) result.add(info.name());
        return result;
    }

    private static List<String> methods(MixinInfo info) {
        List<String> result = new ArrayList<>();
        for (MethodNode method : values(info.methods())) result.add(method.name + method.desc);
        return result;
    }

    private static List<String> fields(MixinInfo info) {
        List<String> result = new ArrayList<>();
        for (FieldMixin field : values(info.fields()))
            result.add(field.name() + ":" + field.desc() + ":" + field.access());
        return result;
    }

    private static class Fixture extends ScalaSignature {

        final ClassNode node;
        final ClassSymbol symbol;
        final List<MethodSymbol> entries = new ArrayList<>();
        final List<AnnotationInfo> annotations = new ArrayList<>();
        final List<String> events;
        final String label;

        Fixture(String label, int flags, List<String> events) {
            super(Bytes$.MODULE$.apply(new byte[] { 5, 0, 0 }));
            this.label = label;
            this.events = events;
            node = node(label);
            symbol = new ClassSymbol(label, new ExternalSymbol(PREFIX.replace('/', '.')), flags, 0) {

                @Override
                public String jParent() {
                    return "java/lang/Object";
                }
            };
        }

        MethodSymbol method(String name, int flags, String descriptor) {
            return new MethodSymbol(name, symbol, flags, 0) {

                @Override
                public String jDesc() {
                    assertNotNull(descriptor, "This descriptor must not be read: " + name);
                    return descriptor;
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> IndexedSeq<T> collect(int id) {
            events.add(label + ":" + id);
            Builder<T, IndexedSeq<T>> builder = IndexedSeq$.MODULE$.newBuilder();
            for (Object entry : id == 40 ? annotations : entries) builder.$plus$eq((T) entry);
            return builder.result();
        }
    }

    private static class Scope implements AutoCloseable {

        final Map<String, ClassInfo> cache = state("infoCache");
        final Map<String, MixinInfo> mixins = state("mixinMap");
        final java.util.Map<String, Option<ClassInfo>> oldCache = new LinkedHashMap<>();
        final java.util.Map<String, Option<MixinInfo>> oldMixins = new LinkedHashMap<>();

        Scope() throws Exception {}

        void remember(String name) {
            if (!oldCache.containsKey(name)) {
                oldCache.put(name, cache.get(name));
                oldMixins.put(name, mixins.get(name));
            }
        }

        Fixture fixture(String name) throws Exception {
            return fixture(name, TRAIT, new ArrayList<>());
        }

        Fixture fixture(String name, int flags, List<String> events) throws Exception {
            Fixture fixture = new Fixture(name, flags, events);
            remember(fixture.node.name);
            cache.put(fixture.node.name, scalaInfo(fixture.node, fixture, fixture.symbol));
            return fixture;
        }

        @Override
        public void close() {
            oldCache.forEach((key, old) -> restore(cache, key, old));
            oldMixins.forEach((key, old) -> restore(mixins, key, old));
        }

        private static <T> void restore(Map<String, T> map, String key, Option<T> old) {
            map.remove(key);
            if (old.isDefined()) map.put(key, old.get());
        }

        @SuppressWarnings("unchecked")
        private static <T> Map<String, T> state(String name) throws Exception {
            Field field = ASMMixinCompiler$.class.getDeclaredField(name);
            field.setAccessible(true);
            return (Map<String, T>) field.get(COMPILER);
        }
    }
}
