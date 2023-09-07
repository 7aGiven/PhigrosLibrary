package given.phigros;

public class GameSettings implements SaveModule {
    final static String name = "settings";
    final static byte version = 1;
    @Order(0)
    public boolean chordSupport;
    @Order(1)
    public boolean fcAPIndicator;
    @Order(2)
    public boolean enableHitSound;
    @Order(3)
    public boolean lowResolutionMode;
    @Order(4)
    public String deviceName;
    @Order(5)
    public float bright;
    @Order(6)
    public float musicVolume;
    @Order(7)
    public float effectVolume;
    @Order(8)
    public float hitSoundVolume;
    @Order(9)
    public float soundOffset;
    @Order(10)
    public float noteScale;
}