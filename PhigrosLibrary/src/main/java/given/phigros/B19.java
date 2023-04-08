package given.phigros;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

class B19 implements Iterable<String> {
    private final byte[] data;
    private final ByteReader reader;
    private byte length;
    private byte fc;

    B19(byte[] data) {
        this.data = data;
        reader = new ByteReader(data);
    }

    SongLevel[] getB19() {
        var minIndex = 1;
        final var b19 = new SongLevel[20];
        Arrays.fill(b19,new SongLevel());
        for (String id:this) {
            final var levels = PhigrosUser.getInfo(id).levels;
            int level = levels.length - 1;
            for (; level >= 0; level--) {
                if (levels[level] <= b19[minIndex].rks && levels[level] <= b19[0].rks)
                    break;
            }
            if (++level == levels.length)
                continue;
            length = reader.getByte();
            fc = reader.getByte();
            go(level);
            for (; level < levels.length; level++) {
                if (levelNotExist(level))
                    continue;
                final var songLevel = new SongLevel();
                songLevel.score = reader.getInt();
                songLevel.acc = reader.getFloat();
                if (songLevel.acc < 70f)
                    continue;
                if (songLevel.score == 1000000) {
                    songLevel.rks = levels[level];
                    if (levels[level] > b19[0].rks) {
                        songLevel.set(id, level, getFC(level), levels[level]);
                        b19[0] = songLevel;
                    }
                } else {
                    songLevel.rks = (songLevel.acc - 55f) / 45f;
                    songLevel.rks *= songLevel.rks * levels[level];
                }
                if (songLevel.rks < b19[minIndex].rks)
                    continue;
                songLevel.set(id, level, getFC(level), levels[level]);
                b19[minIndex] = songLevel;
                minIndex = min(b19);
            }
        }
        Arrays.sort(b19);
        return b19;
    }

    SongExpect[] getExpect(String id) {
        for (String songId:this) {
            if (!songId.equals(id))
                continue;
            final float minRks = getMinRks();
            final var info = PhigrosUser.getInfo(id);
            length = reader.getByte();
            reader.position++;
            final var list = new ArrayList<SongExpect>();
            for (var level = 0; level < info.levels.length; level++) {
                if (levelNotExist(level))
                    continue;
                if (info.levels[level] <= minRks)
                    continue;
                final int score = reader.getInt();
                if (score == 1000000)
                    continue;
                final float acc = reader.getFloat();
                final var expect = (float) Math.sqrt(minRks / info.levels[level]) * 45f + 55f;
                if (expect > acc)
                    list.add(new SongExpect(id, info.name, level, acc, expect));
            }
            return list.toArray(SongExpect[]::new);
        }
        throw new RuntimeException("不存在该id的曲目。");
    }
    SongExpect[] getExpects() {
        final var minRks = getMinRks();
        final var list = new ArrayList<SongExpect>();
        for (String id:this) {
            final var info = PhigrosUser.getInfo(id);
            final var levels = info.levels;
            int level = levels.length - 1;
            for (; level >= 0; level--) {
                if (levels[level] <= minRks)
                    break;
            }
            if (++level == levels.length)
                continue;
            length = reader.getByte();
            reader.position++;
            go(level);
            for (; level < levels.length; level++) {
                if (levelNotExist(level))
                    continue;
                final int score = reader.getInt();
                if (score == 1000000)
                    continue;
                final float acc = reader.getFloat();
                final var expect = (float) Math.sqrt(minRks / levels[level]) * 45f + 55f;
                if (expect > acc)
                    list.add(new SongExpect(id, info.name, level, acc, expect));
            }
        }
        final var array = list.toArray(SongExpect[]::new);
        Arrays.sort(array);
        return array;
    }

    private float getMinRks() {
        var minIndex = 0;
        final var b19 = new float[19];
        for (String id:this) {
            final var levels = PhigrosUser.getInfo(id).levels;
            int level = levels.length - 1;
            for (; level >= 0; level--) {
                if (levels[level] <= b19[minIndex])
                    break;
            }
            if (++level == levels.length)
                continue;
            length = reader.getByte();
            reader.position++;
            go(level);
            for (; level < levels.length; level++) {
                if (levelNotExist(level))
                    continue;
                final var score = reader.getInt();
                final var acc = reader.getFloat();
                if (acc < 70f)
                    continue;
                float rks;
                if (score == 1000000)
                    rks = levels[level];
                else {
                    rks = (acc - 55f) / 45f;
                    rks *= rks * levels[level];
                }
                if (rks <= b19[minIndex])
                    continue;
                b19[minIndex] = rks;
                minIndex = min(b19);
            }
        }
        return b19[minIndex];
    }

    private int min(SongLevel[] array) {
        var index = -1;
        var min = 17f;
        for (int i = 1; i < 20; i++) {
            if (array[i].id == null)
                return i;
            if (array[i].rks < min) {
                index = i;
                min = array[i].rks;
            }
        }
        return index;
    }

    private int min(float[] array) {
        var index = -1;
        var min = 17f;
        for (var i = 0; i < 19; i++) {
            if (array[i] == 0f)
                return i;
            if (array[i] < min) {
                index = i;
                min = array[i];
            }
        }
        return index;
    }

    private void go(int index) {
        for (int i = 0; i < index; i++) {
            if (Util.getBit(length, i))
                reader.position += 8;
        }
    }

    private boolean levelNotExist(int level) {
        return !Util.getBit(length, level);
    }


    boolean getFC(int index) {
        return Util.getBit(fc, index);
    }

    @Override
    public Iterator<String> iterator() {
        return new B19Iterator();
    }

    private class B19Iterator implements Iterator<String> {
        private int position;
        B19Iterator() {
            position = Util.getBit(data[0], 7) ? 2 : 1;
        }

        @Override
        public boolean hasNext() {
            return position != data.length;
        }

        @Override
        public String next() {
            var length = data[position++];
            final var id = new String(data, position, length - 2);
            position += length;
            length = data[position++];
            reader.position = position;
            position += length;
            return id;
        }
    }
}