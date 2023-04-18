package given.phigros;

import java.io.IOException;

public class GameSettings implements GameExtend {
    final static String name = "settings";
    public boolean chordSupport;
    public boolean fcAPIndicator;
    public boolean enableHitSound;
    public boolean lowResolutionMode;
    public String deviceName;
    public float bright;
    public float musicVolume;
    public float effectVolume;
    public float hitSoundVolume;
    public float soundOffset;
    public float noteScale;
    GameSettings(byte[] data) {
        ByteSerialize.read(this, data);
    }
    
    @Override
    public byte[] getData() throws IOException {
        return ByteSerialize.write(this);
    }
}