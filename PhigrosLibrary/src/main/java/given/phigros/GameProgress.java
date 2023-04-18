package given.phigros;

import java.io.IOException;

public class GameProgress implements GameExtend {
    final static String name = "gameProgress";
    public boolean isFirstRun;
    public boolean legacyChapterFinished;
    public boolean alreadyShowCollectionTip;
    public boolean alreadyShowAutoUnlockINTip;
    public String completed;
    public int songUpdateInfo;
    public short challengeModeRank;
    public int[] money = new int[5];
    public byte unlockFlagOfSpasmodic;
    public byte unlockFlagOfIgallta;
    public byte unlockFlagOfRrharil;
    public byte flagOfSongRecordKey;
    public byte randomVersionUnlocked;
    GameProgress(byte[] data) {
        ByteSerialize.read(this, data);
    }

    public byte[] getData() throws IOException {
        if (money.length != 5)
            throw new RuntimeException("money数组长度不为5。");
        return ByteSerialize.write(this);
    }
}
