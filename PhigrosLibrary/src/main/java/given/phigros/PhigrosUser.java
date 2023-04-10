package given.phigros;

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
import java.util.TreeMap;
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

    public static void validSession(String session) throws IOException, InterruptedException {
        SaveManager.save(session);
    }

    public Summary update() throws IOException, InterruptedException {
        return new Summary(SaveManager.update(this));
    }

    public SongLevel[] getB19() throws IOException, InterruptedException {
        return new B19(extractZip(GameRecord.class)).getB19(19);
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
    public <T extends GameExtend> T get(Class<T> clazz) throws IOException, InterruptedException {
        try {
            return clazz.getDeclaredConstructor(byte[].class).newInstance(extractZip(clazz));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    public void modifyData(short num) throws Exception {
        ModifyStrategyImpl.data(this,num);
    }
    public void modifyAvater(String avater) throws Exception {
        ModifyStrategyImpl.avater(this,avater);
    }
    public void modifyCollection(String collection) throws Exception {
        ModifyStrategyImpl.collection(this,collection);
    }
    public void modifyChallenge(short challenge) throws Exception {
        ModifyStrategyImpl.challenge(this,challenge);
    }
    public void modifySong(String songId,int level,int s,float a,boolean fc) throws Exception {
        ModifyStrategyImpl.song(this,songId,level,s,a,fc);
    }
    public <T extends GameExtend> void modify(Class<T> clazz, ModifyStrategy<T> strategy) throws IOException, InterruptedException {
        SaveManager.modify(this, ModifyStrategyImpl.challengeScore, clazz, strategy);
    }
    public void downloadSave(Path path) throws IOException, InterruptedException {
        Files.write(path,getData(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
    public void uploadSave(Path path) throws IOException, InterruptedException {
        SaveManager saveManager = new SaveManager(this);
        saveManager.data = Files.readAllBytes(path);
        saveManager.uploadZip((short) 3);
    }
    private <T extends GameExtend> byte[] extractZip(Class<T> clazz) throws IOException, InterruptedException {
        byte[] buffer;
        byte[] data = getData();
        try (ByteArrayInputStream reader = new ByteArrayInputStream(data)) {
            try (ZipInputStream zipReader = new ZipInputStream(reader)) {
                while (true) {
                    ZipEntry entry = zipReader.getNextEntry();
                    System.out.println(entry);
                    String tmp;
                    try {
                        tmp = (String) clazz.getDeclaredField("name").get(null);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    if (entry.getName().equals(tmp)) {
                        break;
                    }
                }
                zipReader.skip(1);
                buffer = zipReader.readAllBytes();
                zipReader.closeEntry();
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
