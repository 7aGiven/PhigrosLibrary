package given.phigros;

import java.time.Instant;
import java.util.Base64;

public final class Summary {
    public Instant updatedAt;
    public byte saveVersion;
    public short challengeModeRank;
    public float rankingScore;
    public byte gameVersion;
    public String avatar;
    public short[] cleared = new short[4];
    public short[] fullCombo = new short[4];
    public short[] phi = new short[4];
    Summary(String summary) {
        final var reader = new ByteReader(Base64.getDecoder().decode(summary));
        saveVersion = reader.getByte();
        challengeModeRank = reader.getShort();
        rankingScore = reader.getFloat();
        gameVersion = reader.getByte();
        avatar = reader.getString();
        for (var level = 0; level < 4; level++) {
            cleared[level] = reader.getShort();
            fullCombo[level] = reader.getShort();
            phi[level] = reader.getShort();
        }
    }

    String serialize() {
        final var bytes = avatar.getBytes();
        final var reader = new ByteReader(new byte[33 + bytes.length]);
        reader.putByte(saveVersion);
        reader.putShort(challengeModeRank);
        reader.putFloat(rankingScore);
        reader.putByte(gameVersion);
        reader.putString(avatar);
        for (var level = 0; level < 4; level++) {
            reader.putShort(cleared[level]);
            reader.putShort(fullCombo[level]);
            reader.putShort(phi[level]);
        }
        return Base64.getEncoder().encodeToString(reader.data);
    }

    @Override
    public String toString() {
        return String.format("{'存档版本':%d,'课题分':%d,'RKS':%.4f,'游戏版本':%d,'头像':'%s','EZ':[%d,%d,%d],'HD':[%d,%d,%d],'IN':[%d,%d,%d],'AT':[%d,%d,%d]}",
                saveVersion, challengeModeRank, rankingScore, gameVersion, avatar,
                cleared[0], fullCombo[0], phi[0],
                cleared[1], fullCombo[1], phi[1],
                cleared[2], fullCombo[2], phi[2],
                cleared[3], fullCombo[3], phi[3]);
    }
}
