package given.phigros;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SaveManager {
    private static final String baseUrl = "https://rak3ffdi.cloud.tds1.tapapis.cn/1.1";
    private static final String fileTokens = baseUrl + "/fileTokens";
    private static final String fileCallback = baseUrl + "/fileCallback";
    private static final String save = baseUrl + "/classes/_GameSave";
    private static final String userInfo = baseUrl + "/users/me";
    public final SaveModel saveModel;
    private final MessageDigest md5;
    private final PhigrosUser user;
    public byte[] data;


    public SaveManager(PhigrosUser user) throws IOException {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.user = user;
        JSONObject saveInfo = save(user.session);
        SaveModel saveModel = new SaveModel();
        saveModel.summary = saveInfo.getString("summary");
        saveModel.objectId = saveInfo.getString("objectId");
        saveModel.userObjectId = saveInfo.getJSONObject("user").getString("objectId");
        saveInfo = saveInfo.getJSONObject("gameFile");
        saveModel.gameObjectId = saveInfo.getString("objectId");
        saveModel.checksum = saveInfo.getJSONObject("metaData").getString("_checksum");
        user.saveUrl = new URL(saveInfo.getString("url").replace("https", "http"));
        this.saveModel = saveModel;
    }

    static String getPlayerId(String session) throws IOException {
        JSONObject response = new HttpConnection(userInfo)
                .pigeon(session)
                .json();
        Logger.getGlobal().info(response.toString());
        return response.getString("nickname");
    }

    static JSONArray saveArray(String session) throws IOException {
        JSONObject response = new HttpConnection(save)
                .pigeon(session)
                .json();
        Logger.getGlobal().info(response.toString());
        return response.getJSONArray("results");
    }

    static JSONObject saveCheck(String session) throws IOException {
        final JSONArray array = saveArray(session);
        final int size = array.size();
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

    static JSONObject save(String session) throws IOException {
        final JSONArray array = saveArray(session);
        final int size = array.size();
        if (size == 0)
            throw new RuntimeException("存档不存在");
        return array.getJSONObject(0);
    }

    public void downloadSave() throws IOException {
        HttpConnection connection = new HttpConnection(user.saveUrl);
        if (connection.connect() == 404) throw new RuntimeException("存档文件不存在");
        data = connection.bytes();
    }

    <T extends SaveModule> void modify(Class<T> clazz, ModifyStrategy<T> callback) throws IOException, InterruptedException {
        HttpConnection connection = new HttpConnection(user.saveUrl);
        data = connection.bytes();
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
                                data = decrypt(Util.readAllBytes(zipReader));
                                T tmp = clazz.getDeclaredConstructor().newInstance();
                                tmp.loadFromBinary(data);
                                callback.apply(tmp);
                                data = encrypt(tmp.serialize());
                                zipWriter.write(version);
                            } else
                                data = Util.readAllBytes(zipReader);
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

    public void uploadZip() throws IOException {
        JSONObject response;

        final byte[] summary = Base64.getDecoder().decode(saveModel.summary);
        summary[7] = 64;
        saveModel.summary = Base64.getEncoder().encodeToString(summary);
        Logger.getGlobal().warning(new Summary(saveModel.summary).toString());



        response = new HttpConnection(fileTokens)
                .pigeon(user.session)
                .post(String.format("{\"name\":\".save\",\"__type\":\"File\",\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"prefix\":\"gamesaves\",\"metaData\":{\"size\":%d,\"_checksum\":\"%s\",\"prefix\":\"gamesaves\"}}",saveModel.userObjectId,data.length, md5(data)))
                .json();
        String tokenKey = Base64.getEncoder().encodeToString(response.getString("key").getBytes());
        String newGameObjectId = response.getString("objectId");
        String authorization = "UpToken " + response.getString("token");
        Logger.getGlobal().warning(response.toString());



        response = new HttpConnection(String.format("https://upload.qiniup.com/buckets/rAK3Ffdi/objects/%s/uploads", tokenKey))
                .header("Authorization", authorization)
                .post(null)
                .json();
        final String uploadId = response.getString("uploadId");
        Logger.getGlobal().warning(response.toString());



        response = new HttpConnection(String.format("https://upload.qiniup.com/buckets/rAK3Ffdi/objects/%s/uploads/%s/1",tokenKey,uploadId))
                .header("Authorization", authorization)
                .header("Content-Type","application/octet-stream")
                .put(data)
                .json();
        final String etag = response.getString("etag");
        Logger.getGlobal().warning(response.toString());



        response = new HttpConnection(String.format("https://upload.qiniup.com/buckets/rAK3Ffdi/objects/%s/uploads/%s",tokenKey,uploadId))
                .header("Authorization",authorization)
                .header("Content-Type","application/json")
                .post(String.format("{\"parts\":[{\"partNumber\":1,\"etag\":\"%s\"}]}",etag))
                .json();
        Logger.getGlobal().warning(response.toString());



        response = new HttpConnection(fileCallback)
                .pigeon(user.session)
                .header("Content-Type","application/json")
                .post(String.format("{\"result\":true,\"token\":\"%s\"}",tokenKey))
                .json();
        Logger.getGlobal().warning(response.toString());



        response = new HttpConnection(String.format(baseUrl + "/classes/_GameSave/%s?",saveModel.objectId))
                .pigeon(user.session)
                .header("Content-Type","application/json")
                .put(String.format("{\"summary\":\"%s\",\"modifiedAt\":{\"__type\":\"Date\",\"iso\":\"%s\"},\"gameFile\":{\"__type\":\"Pointer\",\"className\":\"_File\",\"objectId\":\"%s\"},\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"user\":{\"__type\":\"Pointer\",\"className\":\"_User\",\"objectId\":\"%s\"}}",saveModel.summary, Instant.ofEpochMilli(System.currentTimeMillis()), newGameObjectId,saveModel.userObjectId,saveModel.userObjectId))
                .json();
        Logger.getGlobal().warning(response.toString());



        response = new HttpConnection(String.format(baseUrl + "/files/%s",saveModel.gameObjectId))
                .pigeon(user.session)
                .delete()
                .json();
        Logger.getGlobal().warning(response.toString());
    }
    private static final SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode("6Jaa0qVAJZuXkZCLiOa/Ax5tIZVu+taKUN1V1nqwkks="), "AES");
    private static final IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode("Kk/wisgNYwcAV8WVGMgyUw=="));
    public static byte[] decrypt(byte[] data) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byteArrayOutputStream.write(data[0]);
            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(byteArrayOutputStream, cipher)) {
                cipherOutputStream.write(data, 1, data.length - 1);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] encrypt(byte[] data) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byteArrayOutputStream.write(data[0]);
            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(byteArrayOutputStream, cipher)) {
                cipherOutputStream.write(data, 1, data.length - 1);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
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