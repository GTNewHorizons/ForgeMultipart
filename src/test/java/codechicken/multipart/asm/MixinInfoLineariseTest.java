package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.Function1;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.Seq$;
import scala.collection.SeqLike;
import scala.collection.mutable.ArrayBuffer;

class MixinInfoLineariseTest {

    @Test
    void preservesDepthFirstParentOrderDuplicatesAndIdentity() {
        MixinInfo base = info(seq());
        MixinInfo left = info(seq(base));
        MixinInfo right = info(seq(base));
        MixinInfo root = info(seq(left, right, left));
        assertEntries(base.linearise(), base);
        assertEntries(root.linearise(), base, left, base, right, base, left, root);
    }

    @Test
    void keepsCollectionBuildersAndRereadsMutableParentsOnEachQuery() {
        MixinInfo parent = info(seq());
        ArrayBuffer<MixinInfo> parents = new ArrayBuffer<>();
        MixinInfo root = info(parents);
        Seq<MixinInfo> first = root.linearise();
        assertSame(ArrayBuffer.class, first.getClass());
        assertEntries(first, root);
        parents.$plus$eq(parent);
        assertEntries(root.linearise(), parent, root);
        assertEntries(first, root);
        assertTrue(info(seq()).linearise() instanceof scala.collection.immutable.List);
    }

    @Test
    void readsVirtualParentsOnceAndUsesVirtualParentResultsWithoutFurtherTraversal() {
        List<String> calls = new ArrayList<>();
        MixinInfo marker = info(null);
        MixinInfo parent = new MixinInfo(null, null, null, null, null, null) {

            @Override
            public Seq<MixinInfo> linearise() {
                calls.add("parent");
                return seq(marker, null, marker);
            }
        };
        MixinInfo root = new MixinInfo(null, null, null, null, null, null) {

            @Override
            public Seq<MixinInfo> parentTraits() {
                calls.add("parents");
                return seq(parent);
            }
        };
        assertEntries(root.linearise(), marker, null, marker, root);
        assertEquals(Arrays.asList("parents", "parent"), calls);
    }

    @Test
    void emptyParentOverridesContributeNoEntries() {
        MixinInfo parent = new MixinInfo(null, null, null, null, null, null) {

            @Override
            public Seq<MixinInfo> linearise() {
                return seq();
            }
        };
        MixinInfo root = info(seq(parent));
        assertEntries(root.linearise(), root);
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchesFlatMapThenAppendWithSeqBuildersAndAllowsASeqLikeIntermediate() {
        List<String> calls = new ArrayList<>();
        MixinInfo marker = info(seq());
        Seq<MixinInfo> result = seq(marker);
        MixinInfo[] root = { null };
        SeqLike<?, ?> flattened = (SeqLike<?, ?>) Proxy.newProxyInstance(
                SeqLike.class.getClassLoader(),
                new Class<?>[] { SeqLike.class },
                (proxy, method, args) -> {
                    assertEquals("$colon$plus", method.getName());
                    calls.add("append");
                    assertSame(root[0], args[0]);
                    assertSame(Seq$.MODULE$.canBuildFrom(), args[1]);
                    return result;
                });
        MixinInfo parent = new MixinInfo(null, null, null, null, null, null) {

            @Override
            public Seq<MixinInfo> linearise() {
                calls.add("parent");
                return seq();
            }
        };
        root[0] = info(collection((proxy, method, args) -> {
            assertEquals("flatMap", method.getName());
            calls.add("flatMap");
            assertSame(Seq$.MODULE$.canBuildFrom(), args[1]);
            assertEntries(((Function1<MixinInfo, Seq<MixinInfo>>) args[0]).apply(parent));
            return flattened;
        }));
        assertSame(result, root[0].linearise());
        assertEquals(Arrays.asList("flatMap", "parent", "append"), calls);
    }

    @Test
    void stopsAtNullParentsAndNullOrThrowingParentResults() {
        assertThrows(NullPointerException.class, () -> info(null).linearise());
        for (boolean throwsFailure : new boolean[] { false, true }) {
            RuntimeException failure = new IllegalStateException("parent");
            List<String> calls = new ArrayList<>();
            MixinInfo bad = new MixinInfo(null, null, null, null, null, null) {

                @Override
                public Seq<MixinInfo> linearise() {
                    calls.add("bad");
                    if (throwsFailure) throw failure;
                    return null;
                }
            };
            MixinInfo later = new MixinInfo(null, null, null, null, null, null) {

                @Override
                public Seq<MixinInfo> linearise() {
                    throw new AssertionError("Later parents must not run");
                }
            };
            RuntimeException actual = assertThrows(RuntimeException.class, () -> info(seq(bad, later)).linearise());
            if (throwsFailure) assertSame(failure, actual);
            else assertInstanceOf(NullPointerException.class, actual);
            assertEquals(Arrays.asList("bad"), calls);
            assertThrows(NullPointerException.class, () -> info(seq(null, later)).linearise());
        }
    }

    @Test
    void preservesCollectionFailuresAndErasedIntermediateCasts() {
        RuntimeException failure = new IllegalStateException("flatMap");
        assertSame(
                failure,
                assertThrows(
                        RuntimeException.class,
                        () -> info(collection((proxy, method, args) -> { throw failure; })).linearise()));
        assertThrows(NullPointerException.class, () -> info(collection((proxy, method, args) -> null)).linearise());
        assertThrows(
                ClassCastException.class,
                () -> info(collection((proxy, method, args) -> new Object())).linearise());
        for (Object result : new Object[] { null, new Object() }) {
            SeqLike<?, ?> flattened = (SeqLike<?, ?>) Proxy.newProxyInstance(
                    SeqLike.class.getClassLoader(),
                    new Class<?>[] { SeqLike.class },
                    (proxy, method, args) -> result);
            MixinInfo root = info(collection((proxy, method, args) -> flattened));
            if (result == null) assertNull(root.linearise());
            else assertThrows(ClassCastException.class, root::linearise);
        }
    }

    private static MixinInfo info(Seq<MixinInfo> parents) {
        return new MixinInfo(null, null, parents, null, null, null);
    }

    private static Seq<MixinInfo> seq(MixinInfo... entries) {
        return JavaConversions.asScalaBuffer(Arrays.asList(entries)).toList();
    }

    @SuppressWarnings("unchecked")
    private static Seq<MixinInfo> collection(InvocationHandler handler) {
        return (Seq<MixinInfo>) Proxy
                .newProxyInstance(Seq.class.getClassLoader(), new Class<?>[] { Seq.class }, handler);
    }

    private static void assertEntries(Seq<MixinInfo> actual, MixinInfo... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) assertSame(expected[i], actual.apply(i));
    }
}
