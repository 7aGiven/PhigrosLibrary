package given.phigros;

import java.io.IOException;
import java.util.Map;

public class GameRecord extends MapSaveModule<LevelRecord[]> {
    final static String name = "gameRecord";
    final static byte version = 1;

    @Override
    void getBytes(ByteWriter writer, Map.Entry<String, LevelRecord[]> entry) throws IOException {
        final byte[] strBytes = entry.getKey().getBytes();
        writer.putByte(strBytes.length + 2);
        writer.outputStream.write(strBytes);
        writer.putByte(46);
        writer.putByte(48);
        final LevelRecord[] levelRecords = entry.getValue();
        byte length = 0;
        byte fc = 0;
        byte num = 0;
        for (byte level = 0; level < 4; level++) {
            if (levelRecords[level] != null) {
                length = Util.modifyBit(length, level, true);
                if (levelRecords[level].c)
                    fc = Util.modifyBit(fc, level, true);
                num++;
            }
        }
        writer.putByte(2 + 8 * num);
        writer.putByte(length);
        writer.putByte(fc);
        for (byte level = 0; level < 4; level++) {
            if (levelRecords[level] != null) {
                writer.putInt(levelRecords[level].s);
                writer.putFloat(levelRecords[level].a);
            }
        }
    }

    @Override
    void putBytes(ByteReader reader) {
        String key = reader.getString(2);
        System.out.println(key);
        reader.position++;
        byte len = reader.getByte();
        byte fc = reader.getByte();
        LevelRecord[] levelRecords = new LevelRecord[4];
        for (byte level = 0; level < 4; level++) {
            if (Util.getBit(len, level)) {
                levelRecords[level] = new LevelRecord();
                levelRecords[level].c = Util.getBit(fc, level);
                levelRecords[level].s = reader.getInt();
                levelRecords[level].a = reader.getFloat();
            }
        }
        put(key, levelRecords);
    }
}