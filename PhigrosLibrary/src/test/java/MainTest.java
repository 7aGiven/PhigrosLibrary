import given.phigros.PhigrosUser;
import given.phigros.SongLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

public class MainTest {
    final String sessionToken = "74ami8z3xjtn52azw3sqj8omn";
    @Test
    void playerId() throws IOException {
        PhigrosUser user = new PhigrosUser(sessionToken);
        String playerId = user.getPlayerId();
        System.out.println(playerId);
    }
    @Test
    void gameRecord() throws IOException {
        PhigrosUser.readInfo(Paths.get("/home/given/difficulty.csv"));
        PhigrosUser user = new PhigrosUser(sessionToken);
        user.update();
        SongLevel[] songLevels = user.getBestN(19);
        for (SongLevel songLevel : songLevels)
            System.out.println(songLevel.toString());
    }
}
