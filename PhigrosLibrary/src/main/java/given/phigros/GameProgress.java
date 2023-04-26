package given.phigros;

public class GameProgress implements SaveModule {
    final static String name = "gameProgress";
    public boolean isFirstRun;
    public boolean legacyChapterFinished;
    public boolean alreadyShowCollectionTip;
    public boolean alreadyShowAutoUnlockINTip;
    public boolean chapter8UnlockBegin;
    public boolean chapter8UnlockSecondPhase;
    public boolean chapter8Passed;
    public String completed;
    public int songUpdateInfo;
    public short challengeModeRank;
    public int[] money = new int[5];
    public byte unlockFlagOfSpasmodic;
    public byte unlockFlagOfIgallta;
    public byte unlockFlagOfRrharil;
    public byte flagOfSongRecordKey;
    public byte randomVersionUnlocked;
    public byte chapter8SongUnlocked;
}