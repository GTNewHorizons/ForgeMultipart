package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * The marker interfaces carry no implementation, so their characterization is their shape: every member abstract, no
 * superinterface, and the exact names downstream parts implement.
 */
class MarkerInterfaceCharacterizationTest {

    @Test
    void slottedPartIsABareInterfaceDespiteExtendingTMultiPartInSource() {
        assertBareInterface(TSlottedPart.class, "getSlotMask()int");
    }

    @Test
    void randomDisplayTickDeclaresOnlyTheTickCallback() {
        assertBareInterface(IRandomDisplayTick.class, "randomDisplayTick(java.util.Random)void");
    }

    @Test
    void neighborTileChangeDeclaresBothCallbacks() {
        assertBareInterface(
                INeighborTileChange.class,
                "weakTileChanges()boolean",
                "onNeighborTileChanged(int,boolean)void");
    }

    @Test
    void randomUpdateTickDeclaresOnWorldJoinAbstract() {
        assertBareInterface(TRandomUpdateTick.class, "randomUpdate()void", "onWorldJoin()void");
    }

    /**
     * The auto-registration lives in the {@code $class} helper, which only Scala implementors reach. A part that does
     * not declare {@code onWorldJoin} resolves it to {@link TMultiPart}, which does nothing -- notably it does not
     * reach {@link TickScheduler}, which would need a world.
     */
    @Test
    void randomUpdateTickImplementorWithoutOwnOnWorldJoinInheritsTheNoOp() throws Exception {
        SilentPart part = new SilentPart();

        part.onWorldJoin();

        assertEquals(TMultiPart.class, SilentPart.class.getMethod("onWorldJoin").getDeclaringClass());
    }

    @Test
    void markersAreImplementableAlongsideTMultiPart() {
        MarkedPart part = new MarkedPart();

        assertTrue(part instanceof TSlottedPart);
        assertTrue(part instanceof IRandomDisplayTick);
        assertTrue(part instanceof INeighborTileChange);
        assertEquals(1 << 6, part.getSlotMask());
        assertFalse(part.weakTileChanges());

        part.randomDisplayTick(new Random(0));
        part.onNeighborTileChanged(3, false);
        assertEquals(3, part.lastTileChangeSide);
    }

    private static void assertBareInterface(Class<?> type, String... expectedMembers) {
        assertTrue(type.isInterface(), type.getName() + " must stay an interface");
        assertEquals(
                0,
                type.getInterfaces().length,
                type.getName() + " must carry no superinterface, as the Scala trait emits none");

        Set<String> actual = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            assertTrue(Modifier.isAbstract(method.getModifiers()), method + " must stay abstract");
            assertTrue(Modifier.isPublic(method.getModifiers()), method + " must stay public");
            actual.add(signature(method));
        }
        assertEquals(new TreeSet<>(Arrays.asList(expectedMembers)), actual);
    }

    private static String signature(Method method) {
        StringBuilder out = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(parameters[i].getName());
        }
        return out.append(')').append(method.getReturnType().getName()).toString();
    }

    private static class SilentPart extends TMultiPart implements TRandomUpdateTick {

        @Override
        public String getType() {
            return "test:silent";
        }

        @Override
        public void randomUpdate() {}
    }

    private static final class MarkedPart extends TMultiPart
            implements TSlottedPart, IRandomDisplayTick, INeighborTileChange {

        int lastTileChangeSide = -1;

        @Override
        public String getType() {
            return "test:marked";
        }

        @Override
        public int getSlotMask() {
            return 1 << 6;
        }

        @Override
        public void randomDisplayTick(Random random) {}

        @Override
        public boolean weakTileChanges() {
            return false;
        }

        @Override
        public void onNeighborTileChanged(int side, boolean weak) {
            lastTileChangeSide = side;
        }
    }
}
