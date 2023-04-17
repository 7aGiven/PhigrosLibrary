package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

public class GameKey extends LinkedHashMap<String, GameKeyValue> implements GameExtend {
    final static String name = "gameKey";
    public byte lanotaReadKeys;

    GameKey(byte[] data) {
        final var reader = new ByteReader(data);
        var length = reader.getVarInt();
        int mark;
        byte strLength;
        for (; length > 0; length--){
            final var key = reader.getString();
            mark = reader.position++;
            strLength = reader.getByte();
            GameKeyValue value = new GameKeyValue();
            for (var i = 0; i < 5; i++) {
                if (Util.getBit(strLength, i)) {
                    value.set(i, reader.getByte());
                }
            }
            put(key, value);
            reader.position = mark;
            strLength = reader.getByte();
            reader.position += strLength;
        }
        lanotaReadKeys = data[data.length - 1];
    }

    public byte[] getData() throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            outputStream.writeBytes(Util.getVarShort(size()));
            for (final var entry : entrySet()) {
                var bytes = entry.getKey().getBytes();
                outputStream.write(bytes.length);
                outputStream.writeBytes(bytes);
                final var value = entry.getValue();
                var num = 0;
                for (var i = 0; i < 5; i++) {
                    if (value.get(i) != 0)
                        num++;
                }
                bytes = new byte[++num];
                var position = 1;
                for (num = 0; num < 5; num++) {
                    if (value.get(num) != 0) {
                        bytes[0] = Util.modifyBit(bytes[0], num, true);
                        bytes[position] = value.get(num);
                        position++;
                    }
                }
                outputStream.write(bytes.length);
                outputStream.writeBytes(bytes);
            }
            outputStream.write(lanotaReadKeys);
            return outputStream.toByteArray();
        }
    }
}
