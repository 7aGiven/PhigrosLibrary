package given.phigros;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameKey extends LinkedHashMap<String, GameKeyValue> implements MapSaveModule {
    final static String name = "gameKey";
    public byte lanotaReadKeys;

    GameKey(byte[] data) {
        loadFromBinary(data);
    }

    static void getBytes(ByteArrayOutputStream outputStream, Map.Entry<String, GameKeyValue> entry) {
        final var strBytes = entry.getKey().getBytes();
        outputStream.write(strBytes.length);
        outputStream.writeBytes(strBytes);
        final var value = entry.getValue();
        byte length = 0;
        var num = 1;
        for (var index = 0; index < 5; index++) {
            if (value.get(index) != 0) {
                length = Util.modifyBit(length, index, true);
                num++;
            }
        }
        outputStream.write(num);
        outputStream.write(length);
        for (var index = 0; index < 5; index++) {
            if (value.get(index) != 0) {
                outputStream.write(value.get(index));
            }
        }
    }

    void putBytes(byte[] data, int position) {
        final var key = new String(data, position + 1, data[position]);
        position += data[position] + 2;
        final var length = data[position++];
        final var value = new GameKeyValue();
        for (var index = 0; index < 5; index++) {
            if (Util.getBit(length, index))
                value.set(index, data[position++]);
        }
        put(key, value);
    }
}
