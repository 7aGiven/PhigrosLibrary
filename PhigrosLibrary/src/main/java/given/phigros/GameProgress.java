package given.phigros;

public class GameProgress implements SaveModule {
    final static String name = "gameProgress";
    final static byte version = 3;
    public boolean isFirstRun;
    public boolean legacyChapterFinished;
    public boolean alreadyShowCollectionTip;
    public boolean alreadyShowAutoUnlockINTip;
    public String completed;
    public byte songUpdateInfo;
    public short challengeModeRank;
    public short[] money = new short[5];
    public byte unlockFlagOfSpasmodic;
    public byte unlockFlagOfIgallta;
    public byte unlockFlagOfRrharil;
    public byte flagOfSongRecordKey;
    public byte randomVersionUnlocked;
    public boolean chapter8UnlockBegin;
    public boolean chapter8UnlockSecondPhase;
    public boolean chapter8Passed;
    public byte chapter8SongUnlocked;
}