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

    SongLevel[] getB19(int num) {
        byte minIndex = 1;
        final SongLevel[] b19 = new SongLevel[num + 1];
        Arrays.fill(b19,new SongLevel());
        for (String id:this) {
            final float[] levels = PhigrosUser.getInfo(id);
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
                final SongLevel songLevel = new SongLevel();
                songLevel.s = reader.getInt();
                songLevel.a = reader.getFloat();
                if (songLevel.a < 70f)
                    continue;
                if (songLevel.s == 1000000) {
                    songLevel.rks = levels[level];
                    if (levels[level] > b19[0].rks) {
                        songLevel.set(id, Level.values()[level], getFC(level), levels[level]);
                        b19[0] = songLevel;
                    }
                } else {
                    songLevel.rks = (songLevel.a - 55f) / 45f;
                    songLevel.rks *= songLevel.rks * levels[level];
                }
                if (songLevel.rks < b19[minIndex].rks)
                    continue;
                songLevel.set(id, Level.values()[level], getFC(level), levels[level]);
                b19[minIndex] = songLevel;
                minIndex = min(b19);
            }
        }
        for (minIndex = 1; minIndex < 20; minIndex++) {
            if (b19[minIndex].id == null)
                break;
        }
        Arrays.sort(b19, 1, minIndex);
        return b19;
    }

    SongExpect[] getExpect(String id) {
        for (String songId:this) {
            if (!songId.equals(id))
                continue;
            final float minRks = getMinRks();
            final float[] levels = PhigrosUser.getInfo(id);
            length = reader.getByte();
            reader.position++;
            final ArrayList<SongExpect> list = new ArrayList();
            for (byte level = 0; level < levels.length; level++) {
                if (levelNotExist(level))
                    continue;
                if (levels[level] <= minRks)
                    continue;
                final int score = reader.getInt();
                if (score == 1000000)
                    continue;
                final float acc = reader.getFloat();
                final float expect = (float) Math.sqrt(minRks / levels[level]) * 45f + 55f;
                if (expect > acc)
                    list.add(new SongExpect(id, Level.values()[level], acc, expect));
            }
            return list.toArray(new SongExpect[list.size()]);
        }
        throw new RuntimeException("不存在该id的曲目。");
    }
    SongExpect[] getExpects() {
        final float minRks = getMinRks();
        final ArrayList<SongExpect> list = new ArrayList();
        for (String id:this) {
            final float[] levels = PhigrosUser.getInfo(id);
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
                if (score == 1000000) {
                    reader.position += 4;
                    continue;
                }
                final float acc = reader.getFloat();
                final float expect = (float) Math.sqrt(minRks / levels[level]) * 45f + 55f;
                if (expect > acc)
                    list.add(new SongExpect(id, Level.values()[level], acc, expect));
            }
        }
        final SongExpect[] array = list.toArray(new SongExpect[list.size()]);
        Arrays.sort(array);
        return array;
    }

    private float getMinRks() {
        byte minIndex = 0;
        final float[] b19 = new float[19];
        for (String id:this) {
            final float[] levels = PhigrosUser.getInfo(id);
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
                final int score = reader.getInt();
                final float acc = reader.getFloat();
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

    private byte min(SongLevel[] array) {
        byte index = -1;
        float min = 17f;
        for (byte i = 1; i < array.length; i++) {
            if (array[i].id == null)
                return i;
            if (array[i].rks < min) {
                index = i;
                min = array[i].rks;
            }
        }
        return index;
    }

    private byte min(float[] array) {
        byte index = -1;
        float min = 17f;
        for (byte i = 0; i < 19; i++) {
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
        private short position;
        B19Iterator() {
            position = (short) (data[0] < 0 ? 2 : 1);
        }

        @Override
        public boolean hasNext() {
            return position != data.length;
        }

        @Override
        public String next() {
            byte length = data[position++];
            final String id = new String(data, position, length - 2);
            position += length;
            length = data[position++];
            reader.position = position;
            position += length;
            return id;
        }
    }
}