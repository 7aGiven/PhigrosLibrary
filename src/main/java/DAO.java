import given.phigros.PhigrosUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DAO {
    public static final DAO INSTANCE;
    static {
        try {
            INSTANCE = new DAO();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public final HashMap<Long, PhigrosUser> users;
    private DAO() throws IOException {
        try (final var reader = Files.newBufferedReader(MyPlugin.INSTANCE.resolveDataPath(Path.of("info.csv")))) {
            PhigrosUser.readInfo(reader);
        }
        users = readUser();
    }
    private HashMap<Long, PhigrosUser> readUser() throws IOException {
        Path path = MyPlugin.INSTANCE.resolveDataFile("user.csv").toPath();
        HashMap<Long, PhigrosUser> users = new HashMap<>();
        if (!Files.exists(path)) return users;
        try (Stream<String> stream = Files.lines(path)) {
            stream.forEach(s -> {
                String[] line = s.split(",");
                users.put(Long.valueOf(line[0]), new PhigrosUser(line[1]));
            });
        }
        return users;
    }
    public void writeUser() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Long, PhigrosUser> entry:users.entrySet()) {
            builder.append(String.format("%d,%s\n",entry.getKey(),entry.getValue().session));
        }
        try {
            Files.writeString(MyPlugin.INSTANCE.resolveDataFile("user.csv").toPath(),builder.toString(),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}