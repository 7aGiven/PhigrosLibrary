package given.phigros;

public class SongExpect implements Comparable<SongExpect> {
    public String id;
    public Level level;
    public float acc;
    public float expect;
    SongExpect(String id,Level level,float acc,float expect) {
        this.id = id;
        this.level = level;
        this.acc = acc;
        this.expect = expect;
    }
    @Override
    public int compareTo(SongExpect songExpect) {
        return Float.compare(expect - acc, songExpect.expect - songExpect.acc);
    }

    @Override
    public String toString() {
        return String.format("{\"songId\":\"%s\",\"level\":\"%s\",\"acc\":%f,\"expect\":%f}", id, level, acc, expect);
    }
}