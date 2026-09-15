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

        for (Method method : IScheduledPacketPart.class.getDeclaredMethods()) {
            assertTrue(Modifier.isAbstract(method.getModifiers()), method + " must stay abstract");
        }
        assertEquals(
                new TreeSet<>(
                        Arrays.asList(
                                "writeScheduled(long,codechicken.lib.data.MCDataOutput)void",
                                "readScheduled(long,codechicken.lib.data.MCDataInput)void",
                                "maskWidth()int")),
                instanceSignatures(IScheduledPacketPart.class));
    }

    /**
     * The two callbacks the trait supplied empty bodies for are now interface defaults, which is what mixing the Scala
     * trait in already gave a Scala implementor. read stays abstract for the reason below.
     */
    @Test
    void thePartTraitExtendsTheCallbackInterfaceAndAddsRead() {
        assertTrue(TScheduledPacketPart.class.isInterface());
        assertEquals(
                new TreeSet<>(Arrays.asList(IScheduledPacketPart.class.getName())),
                names(TScheduledPacketPart.class.getInterfaces()));
        assertEquals(
                new TreeSet<>(
                        Arrays.asList(
                                "read(codechicken.lib.data.MCDataInput)void",
                                "writeScheduled(long,codechicken.lib.data.MCDataOutput)void",
                                "readScheduled(long,codechicken.lib.data.MCDataInput)void")),
                instanceSignatures(TScheduledPacketPart.class));
    }

    @Test
    void theOptionalCallbacksDoNothingUnlessOverridden() {
        SilentPart part = new SilentPart();

        part.writeScheduled(0xffL, null);
        part.readScheduled(0xffL, null);
    }

    /**
     * TMultiPart declares read, so a superclass method would beat any default the interface tried to supply. A part
     * that does not declare read resolves it to TMultiPart's, which reads a description rather than a mask.
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

    /** Declared instance methods only, so the static mask reader is not counted as part of the contract. */
    private static Set<String> instanceSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            assertTrue(Modifier.isPublic(method.getModifiers()), method + " must stay public");
            signatures.add(signature(method));
        }
        return signatures;
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
    }
}
