package given.phigros;

import java.util.Base64;

public class Summary {
    public final byte saveVersion;
    public final short challengeModeRank;
    public final float rankingScore;
    public final byte gameVersion;
    public final String avatar;
    Summary(String summary) {
        final var reader = new ByteReader(Base64.getDecoder().decode(summary));
        saveVersion = reader.getByte();
        challengeModeRank = reader.getShort();
        rankingScore = reader.getFloat();
        gameVersion = reader.getByte();
        avatar = reader.getString();
    }
}
