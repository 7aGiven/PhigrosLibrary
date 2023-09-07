package given.phigros;

import java.io.IOException;
import java.util.Map;

public class GameKey extends MapSaveModule<GameKeyValue> {
    final static String name = "gameKey";
    final static byte version = 1;
    public byte lanotaReadKeys;
    public boolean camelliaReadKey;
    GameKey(byte[] data) {
        loadFromBinary(data);
    }

    void getBytes(ByteWriter writer, Map.Entry<String, GameKeyValue> entry) throws IOException {
        final byte[] strBytes = entry.getKey().getBytes();
        writer.putByte(strBytes.length);
        writer.outputStream.write(strBytes);
        final GameKeyValue value = entry.getValue();
        byte length = 0;
        byte num = 1;
        for (byte index = 0; index < 5; index++) {
            if (value.get(index) != 0) {
                length = Util.modifyBit(length, index, true);
                num++;
            }
        }
        writer.putByte(num);
        writer.putByte(length);
        for (byte index = 0; index < 5; index++) {
            if (value.get(index) != 0)
                writer.putByte(value.get(index));
        }
    }

    void putBytes(ByteReader reader) {
        String key = reader.getString(0);
        reader.position++;
        byte len = reader.getByte();
        final GameKeyValue value = new GameKeyValue();
        for (byte index = 0; index < 5; index++) {
            if (Util.getBit(len, index))
                value.set(index, reader.getByte());
        }
        put(key, value);
    }
}