package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class Util {
    static boolean getBit(byte data, int index) {
        return (data & 1 << index) != 0;
    }
    static byte modifyBit(byte data, int index, boolean b) {
        byte result = (byte)(1 << index);
        if (b) {
            data |= result;
        } else {
            data &= (~result);
        }
        return data;
    }
    static byte[] readAllBytes(ZipInputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[15 * 1024];
            while (true) {
                int len = inputStream.read(bytes);
                if (len == -1)
                    break;
                outputStream.write(bytes, 0, len);
            }
            return outputStream.toByteArray();
        }
    }
}
