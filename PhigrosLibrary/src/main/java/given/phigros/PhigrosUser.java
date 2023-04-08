package given.phigros;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PhigrosUser {
    public String session;
    URI zipUrl;
    public long time;
    public final static TreeMap<String,SongInfo> info = new TreeMap<>();
    public PhigrosUser(String session) {
        if (!session.matches("[a-z0-9]{25}"))
            throw new RuntimeException("SessionToken格式错误。");
        this.session = session;
    }
    public PhigrosUser(URI zipUrl) {this.zipUrl = zipUrl;}
    public Summary update() throws IOException, InterruptedException {
        return new Summary(SaveManager.update(this));
    }
    public static void readInfo(BufferedReader reader) throws IOException {
        info.clear();
        String lineString;
        while ((lineString = reader.readLine()) != null) {
            String[] line = lineString.split(",");
            SongInfo songInfo = new SongInfo();
            songInfo.name = line[1];
            if (line.length != 5 && line.length != 6)
                throw new RuntimeException(String.format("曲目%s的定数数量错误。",songInfo.name));
            final var difficulty = new float[line.length - 2];
            for (int i = 0; i < line.length - 2; i++) {
                difficulty[i] = Float.parseFloat(line[i + 2]);
            }
            songInfo.levels = difficulty;
            info.put(line[0],songInfo);
        }
    }
    static SongInfo getInfo(String id) {
        final var songInfo = info.get(id);
        if (songInfo == null)
            throw new RuntimeException(String.format("缺少%s的信息。", id));
        return songInfo;
    }
    public static void validSession(String session) throws IOException, InterruptedException {
        SaveManager.save(session);
    }
    public SongLevel[] getB19() throws IOException, InterruptedException {
        return new B19(extractZip("gameRecord")).getB19();
    }
    public SongExpect[] getExpect() throws IOException, InterruptedException {
        return new B19(extractZip("gameRecord")).getExpects();
    }
    public GameRecord getGameRecord() throws IOException, InterruptedException {
        return new GameRecord(extractZip("gameRecord"));
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
    public void modify(String type, ModifyStrategy strategy) throws IOException, InterruptedException {
        SaveManager.modify(this, ModifyStrategyImpl.challengeScore, type, strategy);
    }
    public void downloadZip(Path path) throws IOException, InterruptedException {
        Files.write(path,getData());
    }
    public void uploadZip(Path path) throws IOException, InterruptedException {
        SaveManager saveManager = new SaveManager(this);
        saveManager.data = Files.readAllBytes(path);
        saveManager.uploadZip((short) 3);
    }
    private byte[] extractZip(String name) throws IOException, InterruptedException {
        byte[] buffer;
        byte[] data = getData();
        try (ByteArrayInputStream reader = new ByteArrayInputStream(data)) {
            try (ZipInputStream zipReader = new ZipInputStream(reader)) {
                while (true) {
                    ZipEntry entry = zipReader.getNextEntry();
                    System.out.println(entry);
                    if (entry.getName().equals(name)) {
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
    public byte[] getData() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = SaveManager.client.send(HttpRequest.newBuilder(zipUrl).build(),HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) throw new RuntimeException("存档文件不存在");
        return response.body();
    }
}
