import given.phigros.GameProgress;
import given.phigros.PhigrosUser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SaveManagerTest {
    @Test
    void modifyData() throws IOException, InterruptedException {
        var user = new PhigrosUser("4gpfqfijiv2ddidsnwl4jbf36");
        user.update();
        user.modify(GameProgress.class, data -> {
            var money = data.money;
            money[0] = 0;
            money[1] = 0;
            money[2] = 1;
            money[3] = 0;
            money[4] = 0;
        });
    }
}
