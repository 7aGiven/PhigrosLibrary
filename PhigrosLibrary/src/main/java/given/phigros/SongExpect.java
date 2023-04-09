package given.phigros;

public class SongExpect implements Comparable<SongExpect> {
    public String id;
    public int level;
    public float acc;
    public float expect;
    SongExpect(String id,int level,float acc,float expect) {
        this.id = id;
        this.level = level;
        this.acc = acc;
        this.expect = expect;
    }
    @Override
    public int compareTo(SongExpect songExpect) {
        return Float.compare(expect - acc, songExpect.expect - songExpect.acc);
    }
}