import given.phigros.PhigrosUser;
import given.phigros.SongLevel;

import java.io.IOException;
import java.nio.file.Paths;

public class MainTest {
    @org.junit.jupiter.api.Test
    void gameRecord() throws IOException {
        PhigrosUser.readInfo(Paths.get("/home/given/difficulty.csv"));
        PhigrosUser user = new PhigrosUser("74ami8z3xjtn52azw3sqj8omn");
        user.update();
        SongLevel[] songLevels = user.getBestN(19);
        for (SongLevel songLevel : songLevels)
            System.out.println(songLevel.toString());
    }
}
