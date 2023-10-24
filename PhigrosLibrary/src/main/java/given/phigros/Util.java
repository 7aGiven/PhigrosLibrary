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

//    public static String repair(String session, byte index) throws IOException, InterruptedException {
//        final var array = SaveManager.saveArray(session);
//        if (array.size() == 1)
//            throw new RuntimeException("存档无误");
//        final var builder = new StringBuilder();
//        for (var i = 0; i < array.size(); i++) {
//            if (i == index)
//                continue;
//            JSONObject response = SaveManager.delete(session, array.getJSONObject(i).getString("objectId"));
//            builder.append(response);
//            builder.append('\n');
//        }
//        return builder.toString();
//    }
}
