package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;

/**
 * schedulePacket refuses a client world and everything else here needs a bound tile, so the behavior lives in the Forge
 * server suite. What is worth pinning headless is the shape of the two interfaces, and in particular that read cannot
 * become a default method.
 */
class PacketSchedulerCharacterizationTest {

    @Test
    void theCallbackInterfaceDeclaresThreeAbstractMembersAndExtendsNothing() {
        assertTrue(IScheduledPacketPart.class.isInterface());
        assertEquals(0, IScheduledPacketPart.class.getInterfaces().length);
        assertAllAbstract(
                IScheduledPacketPart.class,
                "writeScheduled(long,codechicken.lib.data.MCDataOutput)void",
                "readScheduled(long,codechicken.lib.data.MCDataInput)void",
                "maskWidth()int");
    }

    @Test
    void thePartTraitExtendsTheCallbackInterfaceAndAddsRead() {
        assertTrue(TScheduledPacketPart.class.isInterface());
        assertArrayEqualsAsSet(TScheduledPacketPart.class.getInterfaces(), IScheduledPacketPart.class);
        assertAllAbstract(
                TScheduledPacketPart.class,
                "read(codechicken.lib.data.MCDataInput)void",
                "writeScheduled(long,codechicken.lib.data.MCDataOutput)void",
                "readScheduled(long,codechicken.lib.data.MCDataInput)void");
    }

    /**
     * TMultiPart declares read, so a superclass method would beat any default the interface tried to supply. The trait
     * kept its implementation in the $class helper, which only Scala implementors reach; a part that does not declare
     * read resolves it to TMultiPart's, which reads a description rather than a mask.
     */
    @Test
    void readMustStayAbstractBecauseTMultiPartDeclaresIt() throws Exception {
        Method onTheInterface = TScheduledPacketPart.class.getDeclaredMethod("read", MCDataInput.class);
        assertTrue(Modifier.isAbstract(onTheInterface.getModifiers()));

        assertSame(
                TMultiPart.class,
                SilentPart.class.getMethod("read", MCDataInput.class).getDeclaringClass(),
                "An implementor that does not declare read falls back to TMultiPart, not to the interface");
    }

    private static void assertAllAbstract(Class<?> type, String... expectedMembers) {
        Set<String> actual = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            assertTrue(Modifier.isAbstract(method.getModifiers()), method + " must stay abstract");
            assertTrue(Modifier.isPublic(method.getModifiers()), method + " must stay public");
            actual.add(signature(method));
        }
        assertEquals(new TreeSet<>(Arrays.asList(expectedMembers)), actual);
    }

    private static void assertArrayEqualsAsSet(Class<?>[] actual, Class<?>... expected) {
        assertEquals(new TreeSet<>(names(expected)), new TreeSet<>(names(actual)));
    }

    private static Set<String> names(Class<?>[] types) {
        Set<String> names = new TreeSet<>();
        for (Class<?> type : types) {
            names.add(type.getName());
        }
        return names;
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

    private static final class SilentPart extends TMultiPart implements TScheduledPacketPart {

        @Override
        public String getType() {
            return "test:silent";
        }

        @Override
        public int maskWidth() {
            return 1;
        }

        @Override
        public void writeScheduled(long mask, MCDataOutput packet) {}

        @Override
        public void readScheduled(long mask, MCDataInput packet) {}
    }
}
