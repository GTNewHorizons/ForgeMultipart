package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.asm.ASMMixinCompiler.FieldMixin;

class FieldMixinAccessNameTest {

    @Test
    void onlyThePrivateBitSelectsManglingRegardlessOfOtherFlags() {
        String name = "field";
        for (int bit = 0; bit < Integer.SIZE; bit++) {
            int flags = (1 << bit) & ~ACC_PRIVATE;
            assertSame(name, new FieldMixin(name, null, flags).accessName(null));
            assertEquals("pkg$Owner$$field", new FieldMixin(name, null, flags | ACC_PRIVATE).accessName("pkg/Owner"));
        }
        assertSame(name, new FieldMixin(name, null, ~ACC_PRIVATE).accessName(null));
        assertEquals("pkg$Owner$$field", new FieldMixin(name, null, -1).accessName("pkg/Owner"));
    }

    @Test
    void replacesOnlyOwnerSlashesAndKeepsTheFieldNameLiteral() {
        String[][] owners = { { "", "" }, { "Owner", "Owner" }, { "pkg/Owner", "pkg$Owner" },
                { "/pkg//Owner/", "$pkg$$Owner$" }, { "pkg.Owner$Inner", "pkg.Owner$Inner" },
                { "pkg\\Owner", "pkg\\Owner" }, { "caf\u00e9/Owner", "caf\u00e9$Owner" } };
        for (String name : new String[] { "field", "", "slot/part.$" }) {
            FieldMixin field = new FieldMixin(name, null, ACC_PRIVATE);
            for (String[] owner : owners) assertEquals(owner[1] + "$$" + name, field.accessName(owner[0]));
        }
    }

    @Test
    void nullNamesAreReturnedDirectlyOrAppendedAsNull() {
        assertNull(new FieldMixin(null, null, 0).accessName(null));
        assertNull(new FieldMixin(null, null, ACC_PUBLIC).accessName("pkg/Owner"));
        assertEquals("pkg$Owner$$null", new FieldMixin(null, null, ACC_PRIVATE).accessName("pkg/Owner"));
    }

    @Test
    void readsVirtualAccessThenVirtualNameOncePerCallWithoutReadingDescriptor() {
        List<String> calls = new ArrayList<>();
        int[] reads = { 0 };
        FieldMixin field = new FieldMixin("stored", "stored", 0) {

            @Override
            public int access() {
                calls.add("access");
                return ++reads[0] == 1 ? ACC_PRIVATE : ACC_PUBLIC;
            }

            @Override
            public String name() {
                calls.add("name");
                return "virtual" + reads[0];
            }

            @Override
            public String desc() {
                throw new AssertionError("Descriptor is not part of an accessor name");
            }
        };
        assertEquals("pkg$Owner$$virtual1", field.accessName("pkg/Owner"));
        assertEquals("virtual2", field.accessName(null));
        assertEquals(Arrays.asList("access", "name", "access", "name"), calls);
    }

    @Test
    void nullPrivateOwnerFailsAfterAccessButBeforeNameWhileOtherOwnersAreIgnored() {
        List<String> calls = new ArrayList<>();
        int[] flags = { ACC_PRIVATE };
        FieldMixin field = new FieldMixin("stored", null, 0) {

            @Override
            public int access() {
                calls.add("access");
                return flags[0];
            }

            @Override
            public String name() {
                calls.add("name");
                return "virtual";
            }
        };
        assertThrows(NullPointerException.class, () -> field.accessName(null));
        assertEquals(Arrays.asList("access"), calls);
        flags[0] = ACC_PUBLIC;
        assertEquals("virtual", field.accessName(null));
        assertEquals(Arrays.asList("access", "access", "name"), calls);
    }

    @Test
    void accessFailuresPrecedeOwnerAndNameEvaluation() {
        RuntimeException failure = new IllegalStateException("access");
        int[] reads = { 0 };
        FieldMixin field = new FieldMixin("stored", null, ACC_PRIVATE) {

            @Override
            public int access() {
                reads[0]++;
                throw failure;
            }

            @Override
            public String name() {
                throw new AssertionError("Name must not be read after access fails");
            }
        };
        assertSame(failure, assertThrows(RuntimeException.class, () -> field.accessName(null)));
        assertSame(failure, assertThrows(RuntimeException.class, () -> field.accessName("pkg/Owner")));
        assertEquals(2, reads[0]);
    }

    @Test
    void nameFailuresPropagateUnwrappedAfterOneAccessReadInBothBranches() {
        RuntimeException failure = new IllegalStateException("name");
        for (int flags : new int[] { ACC_PRIVATE, ACC_PUBLIC }) {
            List<String> calls = new ArrayList<>();
            FieldMixin field = new FieldMixin("stored", null, 0) {

                @Override
                public int access() {
                    calls.add("access");
                    return flags;
                }

                @Override
                public String name() {
                    calls.add("name");
                    throw failure;
                }
            };
            assertSame(failure, assertThrows(RuntimeException.class, () -> field.accessName("pkg/Owner")));
            assertEquals(Arrays.asList("access", "name"), calls);
        }
    }
}
