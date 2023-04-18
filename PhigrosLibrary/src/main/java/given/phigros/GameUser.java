package given.phigros;

import java.io.IOException;

public class GameUser implements GameExtend {
    final static String name = "user";
    public boolean showPlayerId;
    public String selfIntro;
    public String avatar;
    public String background;

    GameUser(byte[] data) {
        ByteSerialize.read(this, data);
    }

    @Override
    public byte[] getData() throws IOException {
        return ByteSerialize.write(this);
    }
}
