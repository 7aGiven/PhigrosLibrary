package given.phigros;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class SaveManager {
    private static final String baseUrl = "https://rak3ffdi.cloud.tds1.tapapis.cn/1.1";
    public static final HttpClient client = HttpClient.newHttpClient();
    private static final HttpRequest.Builder globalRequest = HttpRequest.newBuilder().header("X-LC-Id","rAK3FfdieFob2Nn8Am").header("X-LC-Key","Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0").header("User-Agent","LeanCloud-CSharp-SDK/1.0.3").header("Accept","application/json");
    private static final HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
    private static final String fileTokens = baseUrl + "/fileTokens";
    private static final String fileCallback = baseUrl + "/fileCallback";
    private static final String save = baseUrl + "/classes/_GameSave";
    private static final String userInfo = baseUrl + "/users/me";
    public final SaveModel saveModel;
    private final MessageDigest md5;
    private final PhigrosUser user;
    public byte[] data;

    SaveManager(PhigrosUser user) throws IOException, InterruptedException {
        this(user, SaveManager.saveCheck(user.session));
    }

    SaveManager(PhigrosUser user, JSONObject saveInfo) {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.user = user;
        SaveModel saveModel = new SaveModel();
        saveModel.summary = saveInfo.getString("summary");
        saveModel.objectId = saveInfo.getString("objectId");
        saveModel.userObjectId = saveInfo.getJSONObject("user").getString("objectId");
        saveInfo = saveInfo.getJSONObject("gameFile");
        saveModel.gameObjectId = saveInfo.getString("objectId");
        saveModel.updatedTime = saveInfo.getString("updatedAt");
        saveModel.checksum = saveInfo.getJSONObject("metaData").getString("_checksum");
        user.saveUrl = URI.create(saveInfo.getString("url"));
        this.saveModel = saveModel;
    }

    static String getPlayerId(String session) throws IOException, InterruptedException {
        final var request = globalRequest.copy().header("X-LC-Session",session).uri(URI.create(userInfo)).build();
        final var response = client.send(request, handler).body();
        Logger.getGlobal().info(response);
        return JSON.parseObject(response).getString("nickname");
    }

    static JSONArray saveArray(String session) throws IOException, InterruptedException {
        HttpRequest request = globalRequest.copy().header("X-LC-Session",session).uri(URI.create(save)).build();
        String response = client.send(request,handler).body();
        Logger.getGlobal().info(response);
        return JSON.parseObject(response).getJSONArray("results");
    }

    static JSONObject saveCheck(String session) throws IOException, InterruptedException {
        final var array = saveArray(session);
        final var size = array.size();
        if (size == 0)
            throw new RuntimeException("存档不存在");
        else if (array.size() > 1) {
            StringBuilder builder = new StringBuilder("存档有误，请修复存档\n");
            for (int i = 0; i < array.size();) {
                JSONObject object = array.getJSONObject(i);
                String str = String.format("存档%d：\nobjectId：%s\n创建时间：%s\n更新时间：%s\nURL：%s\n", ++i, object.getString("objectId"), object.getString("createdAt"), object.getString("updatedAt"), object.getJSONObject("gameFile").getString("url"));
                builder.append(str);
            }
            throw new RuntimeException(builder.toString());
        }
        return array.getJSONObject(0);
    }

    static JSONObject save(String session) throws IOException, InterruptedException {
        final var array = saveArray(session);
        final var size = array.size();
        if (size == 0)
            throw new RuntimeException("存档不存在");
        return array.getJSONObject(0);
    }
    static String delete(String session, String objectId) throws IOException, InterruptedException {
        HttpRequest.Builder builder = globalRequest.copy();
        builder.DELETE();
        builder.uri(URI.create(baseUrl + "/classes/_GameSave/" + objectId));
        builder.header("X-LC-Session",session);
        return client.send(builder.build(),handler).body();
    }

    <T extends SaveModule> void modify(Class<T> clazz, ModifyStrategy<T> callback) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(user.saveUrl).build();
        data = client.send(request,HttpResponse.BodyHandlers.ofByteArray()).body();
        if (!md5(data).equals(saveModel.checksum)) throw new RuntimeException("文件校验不一致");
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputStream.available())) {
                try (ZipOutputStream zipWriter = new ZipOutputStream(outputStream)) {
                    try (ZipInputStream zipReader = new ZipInputStream(inputStream)) {
                        String name = (String) clazz.getDeclaredField("name").get(null);
                        ZipEntry entry;
                        while ((entry = zipReader.getNextEntry()) != null) {
                            ZipEntry dEntry = new ZipEntry(entry);
                            dEntry.setCompressedSize(-1);
                            zipWriter.putNextEntry(dEntry);
                            if (entry.getName().equals(name)) {
                                byte version = clazz.getDeclaredField("version").getByte(null);
                                if (zipReader.read() != version)
                                    throw new RuntimeException("存档该部分已升级。");
                                Logger.getGlobal().info(String.valueOf(version));
                                data = decrypt(zipReader.readAllBytes());
                                T tmp = clazz.getDeclaredConstructor().newInstance();
                                tmp.loadFromBinary(data);
                                callback.apply(tmp);
                                data = encrypt(tmp.serialize());
                                zipWriter.write(version);
                            } else
                                data = zipReader.readAllBytes();
                            zipWriter.write(data);
                        }
                        zipReader.closeEntry();
                    } catch (NoSuchFieldException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                data = outputStream.toByteArray();
            }
        }
    }
    void uploadZip(short score) throws IOException, InterruptedException {
        String response;
        final HttpRequest.Builder template = globalRequest.copy().header("X-LC-Session",user.session);

        final var summary = Base64.getDecoder().decode(saveModel.summary);
        summary[1] = (byte) (score & 0xff);
        summary[2] = (byte) (score >>> 8 & 0xff);
        saveModel.summary = Base64.getEncoder().encodeToString(summary);
        Logger.getGlobal().info(new Summary(saveModel.summary).toString());



        HttpRequest.Builder builder = template.copy();
        builder.uri(URI.create(fileTokens));
        builder.POST(HttpRequest.BodyPublishers.ofString(String.format("{\"name\":\".save\",\"__type\":\"File\",\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"prefix\":\"gamesaves\",\"metaData\":{\"size\":%d,\"_checksum\":\"%s\",\"prefix\":\"gamesaves\"}}",saveModel.userObjectId,data.length, md5(data))));
        response = client.send(builder.build(),handler).body();
        String tokenKey = Base64.getEncoder().encodeToString(JSON.parseObject(response).getString("key").getBytes());
        String newGameObjectId = JSON.parseObject(response).getString("objectId");
        String authorization = "UpToken "+JSON.parseObject(response).getString("token");
        Logger.getGlobal().fine(response);



        builder = HttpRequest.newBuilder(URI.create(String.format("https://upload.qiniup.com/buckets/rAK3Ffdi/objects/%s/uploads", tokenKey)));
        builder.header("Authorization",authorization);
        builder.POST(HttpRequest.BodyPublishers.noBody());
        response = client.send(builder.build(),handler).body();
        final var uploadId = JSON.parseObject(response).getString("uploadId");
        Logger.getGlobal().fine(response);



        builder = HttpRequest.newBuilder(URI.create(String.format("https://upload.qiniup.com/buckets/rAK3Ffdi/objects/%s/uploads/%s/1",tokenKey,uploadId)));
        builder.header("Authorization",authorization);
        builder.header("Content-Type","application/octet-stream");
        builder.PUT(HttpRequest.BodyPublishers.ofByteArray(data));
        response = client.send(builder.build(),handler).body();
        final var etag = JSON.parseObject(response).getString("etag");
        Logger.getGlobal().fine(response);



        builder = HttpRequest.newBuilder(URI.create(String.format("https://upload.qiniup.com/buckets/rAK3Ffdi/objects/%s/uploads/%s",tokenKey,uploadId)));
        builder.header("Authorization",authorization);
        builder.header("Content-Type","application/json");
        builder.POST(HttpRequest.BodyPublishers.ofString(String.format("{\"parts\":[{\"partNumber\":1,\"etag\":\"%s\"}]}",etag)));
        response = client.send(builder.build(),handler).body();
        Logger.getGlobal().fine(response);



        builder = template.copy();
        builder.uri(URI.create(fileCallback));
        builder.header("Content-Type","application/json");
        builder.POST(HttpRequest.BodyPublishers.ofString(String.format("{\"result\":true,\"token\":\"%s\"}",tokenKey)));
        response = client.send(builder.build(), handler).body();
        Logger.getGlobal().fine(response);



        builder = template.copy();
        builder.uri(URI.create(String.format(baseUrl + "/classes/_GameSave/%s?",saveModel.objectId)));
        builder.header("Content-Type","application/json");
        builder.PUT(HttpRequest.BodyPublishers.ofString(String.format("{\"summary\":\"%s\",\"modifiedAt\":{\"__type\":\"Date\",\"iso\":\"%s\"},\"gameFile\":{\"__type\":\"Pointer\",\"className\":\"_File\",\"objectId\":\"%s\"},\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"user\":{\"__type\":\"Pointer\",\"className\":\"_User\",\"objectId\":\"%s\"}}",saveModel.summary, Instant.ofEpochMilli(System.currentTimeMillis()), newGameObjectId,saveModel.userObjectId,saveModel.userObjectId)));
        response = client.send(builder.build(),handler).body();
        Logger.getGlobal().fine(response);



        builder = template.copy();
        builder.uri(URI.create(String.format(baseUrl + "/files/%s",saveModel.gameObjectId)));
        builder.DELETE();
        response = client.send(builder.build(),handler).body();
        Logger.getGlobal().fine(response);
    }
    private static final SecretKeySpec key = new SecretKeySpec(new byte[] {-24,-106,-102,-46,-91,64,37,-101,-105,-111,-112,-117,-120,-26,-65,3,30,109,33,-107,110,-6,-42,-118,80,-35,85,-42,122,-80,-110,75}, "AES");
    private static final IvParameterSpec iv = new IvParameterSpec(new byte[] {42,79,-16,-118,-56,13,99,7,0,87,-59,-107,24,-56,50,83});
    static byte[] decrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    static byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private String md5(byte[] data) {
        md5.reset();
        data = md5.digest(data);
        StringBuilder builder = new StringBuilder();
        for (byte b:data) {
            builder.append(Character.forDigit(b>>4&15,16));
            builder.append(Character.forDigit(b&15,16));
        }
        return builder.toString();
    }
}