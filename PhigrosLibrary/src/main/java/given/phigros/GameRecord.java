package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

public class GameRecord extends LinkedHashMap<String, LevelRecord[]> implements GameExtend {
    final static String name = "gameRecord";

    GameRecord(byte[] data) {
        final var reader = new ByteReader(data);
        var length = reader.getVarInt();
        int mark;
        byte strLength;
        for (; length > 0; length--){
            strLength = reader.getByte();
            final var key = new String(data, reader.position, strLength - 2);
            reader.position += strLength;
            mark = reader.position++;
            strLength = reader.getByte();
            byte fc = reader.getByte();
            final var levelRecords = new LevelRecord[4];
            for (var level = 0; level < 4; level++) {
                if (Util.getBit(strLength, level)) {
                    levelRecords[level] = new LevelRecord();
                    levelRecords[level].c = Util.getBit(fc, level);
                    levelRecords[level].s = reader.getInt();
                    levelRecords[level].a = reader.getFloat();
                }
            }
            this.put(key, levelRecords);
            reader.position = mark;
            strLength = reader.getByte();
            reader.position += strLength;
        }
    }

    public byte[] getData() throws IOException {
        final var zeroBytes = ".0".getBytes();
        try (var outputStream = new ByteArrayOutputStream()) {
            outputStream.writeBytes(Util.getVarShort(size()));
            for (final var entry : entrySet()) {
                System.out.println(entry.getKey());
                final var bytes = entry.getKey().getBytes();
                outputStream.write(bytes.length + 2);
                outputStream.write(bytes);
                outputStream.writeBytes(zeroBytes);
                final var levelRecords = entry.getValue();
                var level = 0;
                for (final var levelRecord : levelRecords) {
                    if (levelRecord != null)
                        level++;
                }
                final var reader = new ByteReader(new byte[8 * level + 2], 2);
                for (level = 0; level < 4; level++) {
                    if (levelRecords[level] != null) {
                        reader.data[0] = Util.modifyBit(reader.data[0], level , true);
                        reader.data[1] = Util.modifyBit(reader.data[1], level, levelRecords[level].c);
                        reader.putInt(levelRecords[level].s);
                        reader.putFloat(levelRecords[level].a);
                    }
                }
                outputStream.write(reader.data.length);
                outputStream.writeBytes(reader.data);
            }
            return outputStream.toByteArray();
        }
    }

}
