package given.phigros;

import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PhigrosUser {
    public String session;
    public URI saveUrl;
    final static TreeMap<String, float[]> info = new TreeMap<>();
    public PhigrosUser(String session) {
        if (!session.matches("[a-z0-9]{25}"))
            throw new RuntimeException("SessionToken格式错误。");
        this.session = session;
    }
    public PhigrosUser(URI saveUrl) {this.saveUrl = saveUrl;}
    public static void readInfo(BufferedReader reader) throws IOException {
        info.clear();
        String lineString;
        while ((lineString = reader.readLine()) != null) {
            String[] line = lineString.split(",");
            if (line.length != 4 && line.length != 5)
                throw new RuntimeException(String.format("曲目%s的定数数量错误。", line[0]));
            final var difficulty = new float[line.length - 1];
            for (int i = 0; i < line.length - 1; i++) {
                difficulty[i] = Float.parseFloat(line[i + 1]);
            }
            info.put(line[0], difficulty);
        }
    }
    static float[] getInfo(String id) {
        final var songInfo = info.get(id);
        if (songInfo == null)
            throw new RuntimeException(String.format("缺少%s的信息。", id));
        return songInfo;
    }

    public JSONObject getSaveInfo() throws IOException, InterruptedException {
        return SaveManager.saveCheck(session);
    }

    public String getPlayerId() throws IOException, InterruptedException {
        return SaveManager.getPlayerId(session);
    }

    public Summary update() throws IOException, InterruptedException {
        JSONObject json = SaveManager.save(session);
        saveUrl = URI.create(json.getJSONObject("gameFile").getString("url"));
        Logger.getGlobal().info(saveUrl.toString());
        Summary summary = new Summary(json.getString("summary"));
        summary.updatedAt = json.getInstant("updatedAt");
        Logger.getGlobal().info(summary.toString());
        return summary;
    }

    public SongLevel[] getBestN(int num) throws IOException, InterruptedException {
        return new B19(extractZip(GameRecord.class)).getB19(num);
    }
    public SongExpect[] getExpect(String id) throws IOException, InterruptedException {
        return new B19(extractZip(GameRecord.class)).getExpect(id);
    }
    public SongExpect[] getExpects() throws IOException, InterruptedException {
        return new B19(extractZip(GameRecord.class)).getExpects();
    }
    public <T extends SaveModule> T get(Class<T> clazz) throws IOException, InterruptedException {
        try {
            T saveModule = clazz.getDeclaredConstructor().newInstance();
            saveModule.loadFromBinary(extractZip(clazz));
            return saveModule;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    public <T extends SaveModule> void modify(Class<T> clazz, ModifyStrategy<T> strategy) throws IOException, InterruptedException {
        SaveManager saveManagement = new SaveManager(this);
        saveManagement.modify(clazz, strategy);
        saveManagement.uploadZip((short) 3);
    }

    public <T extends SaveModule> void modify(Class<T> clazz, ModifyStrategy<T> strategy, JSONObject saveInfo) throws IOException, InterruptedException {
        SaveManager saveManagement = new SaveManager(this, saveInfo);
        saveManagement.modify(clazz, strategy);
        saveManagement.uploadZip((short) 3);
    }

    public void downloadSave(Path path) throws IOException, InterruptedException {
        Files.write(path,getData(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
    public void uploadSave(Path path) throws IOException, InterruptedException {
        SaveManager saveManager = new SaveManager(this);
        saveManager.data = Files.readAllBytes(path);
        saveManager.uploadZip((short) 3);
    }
    private <T extends SaveModule> byte[] extractZip(Class<T> clazz) throws IOException, InterruptedException {
        byte[] buffer;
        try (ByteArrayInputStream reader = new ByteArrayInputStream(getData())) {
            try (ZipInputStream zipReader = new ZipInputStream(reader)) {
                String name;
                byte version;
                try {
                    name = (String) clazz.getDeclaredField("name").get(null);
                    version = (byte) clazz.getDeclaredField("version").get(null);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                while (true) {
                    ZipEntry entry = zipReader.getNextEntry();
                    if (entry.getName().equals(name))
                        break;
                }
                if (zipReader.read() == version) {
                    buffer = zipReader.readAllBytes();
                    zipReader.closeEntry();
                } else
                    throw new RuntimeException("版本号已更新，请更新PhigrosLibrary。");
            }
        }
        return SaveManager.decrypt(buffer);
    }
    private byte[] getData() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = SaveManager.client.send(HttpRequest.newBuilder(saveUrl).build(),HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) throw new RuntimeException("存档文件不存在");
        return response.body();
    }
}
