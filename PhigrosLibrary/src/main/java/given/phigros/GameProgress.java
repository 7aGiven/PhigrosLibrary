package given.phigros;

public class GameProgress implements SaveModule {
    final static String name = "gameProgress";
    final static byte version = 3;
    @Order(0)
    public boolean isFirstRun;
    @Order(1)
    public boolean legacyChapterFinished;
    @Order(2)
    public boolean alreadyShowCollectionTip;
    @Order(3)
    public boolean alreadyShowAutoUnlockINTip;
    @Order(4)
    public String completed;
    @Order(5)
    public byte songUpdateInfo;
    @Order(6)
    public short challengeModeRank;
    @Order(7)
    public short[] money = new short[5];
    @Order(8)
    public byte unlockFlagOfSpasmodic;
    @Order(9)
    public byte unlockFlagOfIgallta;
    @Order(10)
    public byte unlockFlagOfRrharil;
    @Order(11)
    public byte flagOfSongRecordKey;
    @Order(12)
    public byte randomVersionUnlocked;
    @Order(13)
    public boolean chapter8UnlockBegin;
    @Order(14)
    public boolean chapter8UnlockSecondPhase;
    @Order(15)
    public boolean chapter8Passed;
    @Order(16)
    public byte chapter8SongUnlocked;
}