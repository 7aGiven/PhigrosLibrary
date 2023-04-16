package given.phigros;

public class SongLevel implements Comparable<SongLevel>{
    public String id;
    public int level;
    public int s;
    public float a;
    public boolean c;
    public float difficulty;
    public float rks;
    void set(String id, int level, boolean fc, float difficulty) {
        this.id = id;
        this.level = level;
        this.c = fc;
        this.difficulty = difficulty;
    }
    @Override
    public int compareTo(SongLevel songLevel) {
        return Double.compare(songLevel.rks, rks);
    }
}