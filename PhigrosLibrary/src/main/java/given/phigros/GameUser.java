package given.phigros;

import java.io.IOException;

public class GameUser extends GameExtend {
    private final static String name = "user";
    private final ByteReader reader;
    GameUser(byte[] data) {
        reader = new ByteReader(data);
    }
    public String getIntroduction() {
        return getItem(0);
    }
    public String getAvater() {
        return getItem(1);
    }
    public String getIllustration() {
        return getItem(2);
    }
    private String getItem(int index) {
        reader.position = 1;
        for (var i = 0; i < index; i++) {
            reader.skipString();
        }
        return reader.getString();
    }

    @Override
    byte[] getData() throws IOException {
        return reader.data;
    }
}
