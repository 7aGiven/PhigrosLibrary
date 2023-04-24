package given.phigros;

public class GameSettings implements SaveModule {
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
        loadFromBinary(data);
    }
}