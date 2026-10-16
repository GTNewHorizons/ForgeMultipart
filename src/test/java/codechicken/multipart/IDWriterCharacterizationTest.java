package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.data.MCDataOutputWrapper;

class IDWriterCharacterizationTest {

    @ParameterizedTest(name = "max {0} encodes {1} as {2}")
    @CsvSource({ "0,           305419896, '78',          120", "255,         305419896, '78',          120",
            "256,         305419896, '56 78',       22136", "65535,       305419896, '56 78',       22136",
            "65536,       305419896, '12 34 56 78', 305419896", "2147483647,  305419896, '12 34 56 78', 305419896",
            "-2147483648, 305419896, '78',          120", "-1,          305419896, '78',          120", })
    void selectsCarrierFromMaximum(int maximum, int value, String encodedHex, int decodedValue) throws Exception {
        IDWriter writer = new IDWriter();
        writer.setMax(maximum);

        byte[] encoded = encode(writer, value);

        assertArrayEquals(hex(encodedHex), encoded);
        assertEquals(decodedValue, decode(writer, encoded));
    }

    private static byte[] encode(IDWriter writer, int value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MCDataOutput output = new MCDataOutputWrapper(new DataOutputStream(bytes));
        writer.write(output, value);
        return bytes.toByteArray();
    }

    private static int decode(IDWriter writer, byte[] bytes) throws Exception {
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
        MCDataInput input = (MCDataInput) Proxy.newProxyInstance(
                MCDataInput.class.getClassLoader(),
                new Class<?>[] { MCDataInput.class },
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "readUByte":
                            return (short) data.readUnsignedByte();
                        case "readUShort":
                            return data.readUnsignedShort();
                        case "readInt":
                            return data.readInt();
                        default:
                            throw new AssertionError("Unexpected read method: " + method.getName());
                    }
                });

        int value = writer.read(input);
        assertEquals(0, data.available(), "The selected reader must consume its whole carrier");
        return value;
    }

    @Test
    void rejectsUseBeforeSetMax() {
        IDWriter writer = new IDWriter();

        assertThrows(IllegalStateException.class, () -> writer.write(null, 0));
        assertThrows(IllegalStateException.class, () -> writer.read(null));
    }

    private static byte[] hex(String value) {
        String[] octets = value.split(" ");
        byte[] bytes = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            bytes[i] = (byte) Integer.parseInt(octets[i], 16);
        }
        return bytes;
    }
}
