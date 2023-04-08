package given.phigros;

class GameProgress {
    private final ByteReader reader;
    GameProgress(byte[] data) {
        reader = new ByteReader(data);
    }
    public short getChallenge() {
        reader.position = 6;
        return reader.getShort();
    }
    public void setChallenge(short score) {
        if (score >= 600)
            throw new RuntimeException("score不允许超过599");
        reader.position = 6;
        reader.putShort(score);
    }
    public int getGameData() {
        reader.position = 8;
        var sum = 0;
        for (int i = 0; i < 5; i++) {
            sum += reader.getVarShort() * Util.pow(1024, i);
        }
        return sum;
    }
    public void setGameData(short MB) {
        if (MB >= 1024)
            throw new RuntimeException("MB不可超过1024");
        reader.position = 8;
        for (int i = 0; i < 5; i++)
            reader.skipVarShort();
        byte[] bytes;
        if (MB < 128)
            bytes = new byte[] {0, (byte) MB, 0, 0, 0};
        else
            bytes = new byte[] {0, (byte) (MB % 128 + 128), (byte) (MB / 128), 0, 0, 0};
        final var length = reader.position - 8;
        reader.position = 8;
        reader.replaceBytes(length, bytes);
    }
    public byte[] getData() {
        return reader.data;
    }
}
