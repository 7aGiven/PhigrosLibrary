package given.phigros;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameRecord extends LinkedHashMap<String, LevelRecord[]> implements MapSaveModule {
    final static String name = "gameRecord";

    GameRecord(byte[] data) {
        loadFromBinary(data);
    }

    static void getBytes(ByteArrayOutputStream outputStream, Map.Entry<String, LevelRecord[]> entry) {
        final var strBytes = entry.getKey().getBytes();
        outputStream.write(strBytes.length + 2);
        outputStream.writeBytes(strBytes);
        outputStream.write(46);
        outputStream.write(48);
        final var levelRecords = entry.getValue();
        byte length = 0;
        byte fc = 0;
        var num = 0;
        for (var level = 0; level < 4; level++) {
            if (levelRecords[level] != null) {
                length = Util.modifyBit(length, level, true);
                if (levelRecords[level].c)
                    fc = Util.modifyBit(fc, level, true);
                num++;
            }
        }
        outputStream.write(2 + 8 * num);
        outputStream.write(length);
        outputStream.write(fc);
        for (var level = 0; level < 4; level++) {
            if (levelRecords[level] != null) {
                outputStream.writeBytes(ISaveModule.int2bytes(levelRecords[level].s));
                outputStream.writeBytes(ISaveModule.int2bytes(Float.floatToIntBits(levelRecords[level].a)));
            }
        }
    }

    void putBytes(byte[] data, int position) {
        final var key = new String(data, position + 1, data[position] - 2);
        final var reader = new ByteReader(data, position + data[position] + 2);
        final var length = reader.getByte();
        final var fc = reader.getByte();
        final var levelRecords = new LevelRecord[4];
        for (var level = 0; level < 4; level++) {
            if (Util.getBit(length, level)) {
                levelRecords[level] = new LevelRecord();
                levelRecords[level].c = Util.getBit(fc, level);
                levelRecords[level].s = reader.getInt();
                levelRecords[level].a = reader.getFloat();
            }
        }
        put(key, levelRecords);
    }
}