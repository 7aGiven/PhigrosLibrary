package given.phigros;

public class GameUser implements SaveModule {
    final static String name = "user";
    final static byte version = 1;
    @Order(0)
    public boolean showPlayerId;
    @Order(1)
    public String selfIntro;
    @Order(2)
    public String avatar;
    @Order(3)
    public String background;
}