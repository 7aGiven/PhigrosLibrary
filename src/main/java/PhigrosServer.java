import given.phigros.PhigrosUser;
import io.javalin.Javalin;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhigrosServer {
    public static void main(String[] args) throws Exception {
        try (final var reader = Files.newBufferedReader(Path.of("difficulty.csv"))) {
            PhigrosUser.readInfo(reader);
        }
        var app = Javalin.create();

        app.get("saveUrl/{sessionToken}", ctx -> {
            try {
                ctx.res().setCharacterEncoding("UTF-8");
                final var sessionToken = ctx.pathParam("sessionToken");
                if (sessionToken.length() != 25)
                    throw new RuntimeException("SessionToken的长度不为25.");
                final var user = new PhigrosUser(sessionToken);
                final var summary = user.update();
                ctx.result(summary.toString(user.saveUrl.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        app.get("b19/{saveUrl}", ctx -> {
            try {
                ctx.res().setCharacterEncoding("UTF-8");
                var saveUrl = ctx.pathParam("saveUrl").replace('|','/');
                StringBuilder builder = new StringBuilder("[");
                for (final var songLevel : new PhigrosUser(URI.create(saveUrl)).getBestN(19)) {
                    builder.append(songLevel.toString());
                    builder.append(',');
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append(']');
                ctx.result(builder.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

        });

        app.get("expects/{saveUrl}", ctx -> {
            try {
                ctx.res().setCharacterEncoding("UTF-8");
                var saveUrl = ctx.pathParam("saveUrl").replace('|','/');
                StringBuilder builder = new StringBuilder("[");
                for (final var songExpect : new PhigrosUser(URI.create(saveUrl)).getExpects()) {
                    builder.append(songExpect.toString());
                    builder.append(',');
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append(']');
                ctx.result(builder.toString());
            } catch (Exception e) {
            e.printStackTrace();
            }
        });

        app.start(Integer.parseInt(args[0]));
    }
}
