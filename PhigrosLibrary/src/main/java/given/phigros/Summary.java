package given.phigros;

import java.util.Base64;

public class Summary {
    public final short challenge;
    public final float rks;
    public final byte version;
    public final String avater;
    Summary(String summary) {
        final var reader = new ByteReader(Base64.getDecoder().decode(summary), 1);
        challenge = reader.getShort();
        rks = reader.getFloat();
        version = reader.getByte();
        avater = reader.getString();
    }
}
