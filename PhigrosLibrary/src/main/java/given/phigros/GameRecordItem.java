package given.phigros;

import java.util.Iterator;

public class GameRecordItem implements Iterable<SongLevel> {
    final ByteReader reader;
    GameRecordItem(byte[] data) {
        reader = new ByteReader(data);
    }
    public String getId() {
        reader.position = 1;
        return reader.getString();
    }
    public void modifySong(int level,int score,float acc,boolean fc) {
        reader.position = 1;
        reader.skipString();
        byte length = reader.getByte();
        if (!Util.getBit(length, level))
            throw new RuntimeException("未游玩此曲目的此难度。");
        reader.data[reader.position] = Util.modifyBit(reader.data[reader.position], level, fc);
        reader.position++;
        for (int i = 0; i < 4; i++) {
            if (Util.getBit(length, i)) {
                if (i == level) {
                    reader.putInt(score);
                    reader.putFloat(acc);
                    break;
                }
                reader.position += 8;
            }
        }

    }
    @Override
    public Iterator<SongLevel> iterator() {
        return new GameRecordItemIterator();
    }
    private class GameRecordItemIterator implements Iterator<SongLevel> {
        private final String id;
        private final float[] difficulty;
        private final byte length;
        private final byte fc;
        int level = -1;
        GameRecordItemIterator() {
            id = GameRecordItem.this.getId();
            length = reader.getByte();
            fc = reader.getByte();
            difficulty = PhigrosUser.getInfo(id).levels;
        }
        @Override
        public boolean hasNext() {
            while (level != 3) {
                if (Util.getBit(length, ++level)) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public SongLevel next() {
            SongLevel songLevel = new SongLevel();
            songLevel.id = id;
            songLevel.level = level;
            songLevel.difficulty = difficulty[level];
            songLevel.fc = Util.getBit(fc, level);
            songLevel.score = reader.getInt();
            songLevel.acc = reader.getFloat();
            if (songLevel.acc < 70f)
                return songLevel;
            else if (songLevel.score == 1000000)
                songLevel.rks = songLevel.difficulty;
            else
                songLevel.rks = (float) Math.pow((songLevel.acc - 55) / 45, 2) * songLevel.difficulty;
            return songLevel;
        }
    }
}
