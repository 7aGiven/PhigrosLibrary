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
//        saveModel.checksum = saveInfo.getJSONObject("metaData").getString("_checksum");
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

//    static String fileId(String session, String id) throws IOException, InterruptedException {
//        HttpRequest request = globalRequest.copy().header("X-LC-Session",session).uri(URI.create(baseUrl + "/files/" + id)).build();
//        String response = client.send(request,handler).body();
//        Logger.getGlobal().info(response);
//        return JSON.parseObject(response).getString("url");
//    }
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
    /**
     * GET /1.1/classes/_GameSave HTTP/1.1
     * X-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0
     * X-LC-Session: {sessionToken}
     * User-Agent: LeanCloud-CSharp-SDK/1.0.3
     * Accept: application/json
     * X-LC-Id: rAK3FfdieFob2Nn8Am
     * Host: rak3ffdi.cloud.tds1.tapapis.cn
     * Connection: close
     *
     * HTTP/1.1 200 OK
     * Server: uewaf/4.0.1
     * Date: Sun, 23 Jul 2023 02:32:11 GMT
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 867
     * Connection: close
     * Vary: Accept-Encoding
     * x-envoy-upstream-service-time: 7
     * x-service-name: kimber-storage
     * Strict-Transport-Security: max-age=31536000
     *
     * {"results":[{"createdAt":"2023-02-01T05:36:37.455Z","gameFile":{"__type":"File","bucket":"rAK3Ffdi","createdAt":"2023-06-27T04:43:41.266Z","key":"gamesaves/RSJDH66WAWQ3rqszDizv8KvlHQTkzI4d/.save","metaData":{"_checksum":"3679d02d7dadfce7f5c1f6da372161c1","prefix":"gamesaves","size":9650},"mime_type":"application/octet-stream","name":".save","objectId":"649a68fdcd1a0bc9192e68a3","provider":"qiniu","updatedAt":"2023-06-27T04:43:41.266Z","url":"https://rak3ffdi.tds1.tapfiles.cn/gamesaves/RSJDH66WAWQ3rqszDizv8KvlHQTkzI4d/.save"},"modifiedAt":{"__type":"Date","iso":"2023-06-27T04:43:41.619Z"},"name":"save","objectId":"63d9fa6530b4987fb6526e7d","summary":"BAMA7np8QVIaRmVhc3Tov5zkuJzkuYvlrrQtU3BlY2lhbDIQAAoAAQA5AAsAAQB5ADMACgAOAAMAAAA=","updatedAt":"2023-06-27T04:43:41.669Z","user":{"__type":"Pointer","className":"_User","objectId":"63d46389beed1f239ac2fe2a"}}]}
     *
     * POST /1.1/fileTokens HTTP/1.1
     * X-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0
     * X-LC-Session: {sessionToken}
     * User-Agent: LeanCloud-CSharp-SDK/1.0.3
     * Accept: application/json
     * X-LC-Id: rAK3FfdieFob2Nn8Am
     * Content-Type: application/json
     * Content-Length: 207
     * Host: rak3ffdi.cloud.tds1.tapapis.cn
     * Connection: close
     *
     * {"name":".save","__type":"File","ACL":{"63d46389beed1f239ac2fe2a":{"read":true,"write":true}},"prefix":"gamesaves","metaData":{"size":877,"_checksum":"61afd574ba4d653fba5e468410283403","prefix":"gamesaves"}}
     *
     * HTTP/1.1 201 Created
     * Server: uewaf/4.0.1
     * Date: Sun, 23 Jul 2023 02:32:50 GMT
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 677
     * Connection: close
     * x-envoy-upstream-service-time: 3
     * x-service-name: kimber-storage
     * Strict-Transport-Security: max-age=31536000
     *
     * {"bucket":"rAK3Ffdi","createdAt":"2023-07-23T02:32:50.254Z","key":"gamesaves/jDbHjViJBtg6UtbY74Gk7wQyIv2ee2Kh/.save","metaData":{"_checksum":"61afd574ba4d653fba5e468410283403","prefix":"gamesaves","size":877},"mime_type":"application/octet-stream","name":".save","objectId":"64bc91523d92abed0553a36d","provider":"qiniu","token":"bOJAZVDET_Z11xes0ufp39ao_Tie7mrGqecKRkUf:7Aln2dH3KaVw2Ogfs3wvAeBY1d4=:eyJzY29wZSI6InJBSzNGZmRpOmdhbWVzYXZlcy9qRGJIalZpSkJ0ZzZVdGJZNzRHazd3UXlJdjJlZTJLaC8uc2F2ZSIsImRlYWRsaW5lIjoxNjkwMDgzMTcwLCJpbnNlcnRPbmx5IjoxfQ==","upload_url":"https://upload.qiniup.com","url":"https://rak3ffdi.tds1.tapfiles.cn/gamesaves/jDbHjViJBtg6UtbY74Gk7wQyIv2ee2Kh/.save"}
     *
     *POST /buckets/rAK3Ffdi/objects/Z2FtZXNhdmVzL2pEYkhqVmlKQnRnNlV0Ylk3NEdrN3dReUl2MmVlMktoLy5zYXZl/uploads HTTP/1.1
     * Authorization: UpToken bOJAZVDET_Z11xes0ufp39ao_Tie7mrGqecKRkUf:7Aln2dH3KaVw2Ogfs3wvAeBY1d4=:eyJzY29wZSI6InJBSzNGZmRpOmdhbWVzYXZlcy9qRGJIalZpSkJ0ZzZVdGJZNzRHazd3UXlJdjJlZTJLaC8uc2F2ZSIsImRlYWRsaW5lIjoxNjkwMDgzMTcwLCJpbnNlcnRPbmx5IjoxfQ==
     * Content-Length: 0
     * Connection: close
     * Host: upload.qiniup.com
     *
     * HTTP/2 200 OK
     * Server: openresty/1.17.8.2
     * Date: Sun, 23 Jul 2023 02:32:51 GMT
     * Content-Type: application/json
     * Content-Length: 71
     * Cache-Control: no-store, no-cache, must-revalidate
     * Pragma: no-cache
     * Vary: Origin
     * X-Content-Type-Options: nosniff
     * X-Reqid: XxQAAADJf3BqXnQX
     * X-Svr: UP
     * X-Alt-Svc: h3=":443"; ip="223.112.103.31"; ma=3600
     * X-Log: X-Log
     *
     * {"uploadId":"64bc9153c4bcff71ac3c0e24region02z0","expireAt":1690684371}
     *
     * PUT /buckets/rAK3Ffdi/objects/Z2FtZXNhdmVzL2pEYkhqVmlKQnRnNlV0Ylk3NEdrN3dReUl2MmVlMktoLy5zYXZl/uploads/64bc9153c4bcff71ac3c0e24region02z0/1 HTTP/2
     * Host: upload.qiniup.com
     * Authorization: UpToken bOJAZVDET_Z11xes0ufp39ao_Tie7mrGqecKRkUf:7Aln2dH3KaVw2Ogfs3wvAeBY1d4=:eyJzY29wZSI6InJBSzNGZmRpOmdhbWVzYXZlcy9qRGJIalZpSkJ0ZzZVdGJZNzRHazd3UXlJdjJlZTJLaC8uc2F2ZSIsImRlYWRsaW5lIjoxNjkwMDgzMTcwLCJpbnNlcnRPbmx5IjoxfQ==
     * Content-Type: application/octet-stream
     * Content-Md5: Ya/VdLpNZT+6XkaEECg0Aw==
     * Content-Length: 877
     *
     * 存档的二进制数据
     *
     * HTTP/2 200 OK
     * Server: openresty/1.17.8.2
     * Date: Sun, 23 Jul 2023 02:32:52 GMT
     * Content-Type: application/json
     * Content-Length: 80
     * Cache-Control: no-store, no-cache, must-revalidate
     * Pragma: no-cache
     * Vary: Origin
     * X-Content-Type-Options: nosniff
     * X-Reqid: RDcAAAD-0pdqXnQX
     * X-Svr: UP
     * X-Alt-Svc: h3=":443"; ip="223.112.103.31"; ma=3600
     * X-Log: X-Log
     *
     * {"etag":"FiOFO5Pgy8U2123P985IlBMkmziA","md5":"61afd574ba4d653fba5e468410283403"}
     *
     * POST /buckets/rAK3Ffdi/objects/Z2FtZXNhdmVzL2pEYkhqVmlKQnRnNlV0Ylk3NEdrN3dReUl2MmVlMktoLy5zYXZl/uploads/64bc9153c4bcff71ac3c0e24region02z0 HTTP/2
     * Host: upload.qiniup.com
     * Authorization: UpToken bOJAZVDET_Z11xes0ufp39ao_Tie7mrGqecKRkUf:7Aln2dH3KaVw2Ogfs3wvAeBY1d4=:eyJzY29wZSI6InJBSzNGZmRpOmdhbWVzYXZlcy9qRGJIalZpSkJ0ZzZVdGJZNzRHazd3UXlJdjJlZTJLaC8uc2F2ZSIsImRlYWRsaW5lIjoxNjkwMDgzMTcwLCJpbnNlcnRPbmx5IjoxfQ==
     * Content-Type: application/json
     * Content-Length: 66
     *
     * {"parts":[{"partNumber":1,"etag":"FiOFO5Pgy8U2123P985IlBMkmziA"}]}
     *
     * HTTP/2 200 OK
     * Server: openresty/1.17.8.2
     * Date: Sun, 23 Jul 2023 02:32:52 GMT
     * Content-Type: application/json
     * Content-Length: 96
     * Cache-Control: no-store, no-cache, must-revalidate
     * Pragma: no-cache
     * Vary: Origin
     * X-Content-Type-Options: nosniff
     * X-Reqid: 9rUAAABrG7xqXnQX
     * X-Svr: UP
     * X-Alt-Svc: h3=":443"; ip="223.112.103.31"; ma=3600
     * X-Log: X-Log
     *
     * {"hash":"FiOFO5Pgy8U2123P985IlBMkmziA","key":"gamesaves/jDbHjViJBtg6UtbY74Gk7wQyIv2ee2Kh/.save"}
     *
     * POST /1.1/fileCallback HTTP/1.1
     * X-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0
     * X-LC-Session: {sessionToken}
     * User-Agent: LeanCloud-CSharp-SDK/1.0.3
     * Accept: application/json
     * X-LC-Id: rAK3FfdieFob2Nn8Am
     * Content-Type: application/json
     * Content-Length: 240
     * Host: rak3ffdi.cloud.tds1.tapapis.cn
     * Connection: close
     *
     * {"result":true,"token":"bOJAZVDET_Z11xes0ufp39ao_Tie7mrGqecKRkUf:7Aln2dH3KaVw2Ogfs3wvAeBY1d4=:eyJzY29wZSI6InJBSzNGZmRpOmdhbWVzYXZlcy9qRGJIalZpSkJ0ZzZVdGJZNzRHazd3UXlJdjJlZTJLaC8uc2F2ZSIsImRlYWRsaW5lIjoxNjkwMDgzMTcwLCJpbnNlcnRPbmx5IjoxfQ=="}
     *
     * HTTP/1.1 200 OK
     * Server: uewaf/4.0.1
     * Date: Sun, 23 Jul 2023 02:32:54 GMT
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 2
     * Connection: close
     * x-envoy-upstream-service-time: 4
     * x-service-name: kimber-storage
     * Strict-Transport-Security: max-age=31536000
     *
     * {}
     *
     * PUT /1.1/classes/_GameSave/63d9fa6530b4987fb6526e7d? HTTP/1.1
     * X-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0
     * X-LC-Session: {sessionToken}
     * User-Agent: LeanCloud-CSharp-SDK/1.0.3
     * Accept: application/json
     * X-LC-Id: rAK3FfdieFob2Nn8Am
     * Content-Type: application/json
     * Content-Length: 360
     * Host: rak3ffdi.cloud.tds1.tapapis.cn
     * Connection: close
     *
     * {"summary":"BAAAAAAAAFQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","modifiedAt":{"__type":"Date","iso":"2023-07-23T02:32:11.586Z"},"gameFile":{"__type":"Pointer","className":"_File","objectId":"64bc91523d92abed0553a36d"},"ACL":{"63d46389beed1f239ac2fe2a":{"read":true,"write":true}},"user":{"__type":"Pointer","className":"_User","objectId":"63d46389beed1f239ac2fe2a"}}
     *
     * HTTP/1.1 200 OK
     * Server: uewaf/4.0.1
     * Date: Sun, 23 Jul 2023 02:32:54 GMT
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 78
     * Connection: close
     * Vary: Accept-Encoding
     * x-envoy-upstream-service-time: 13
     * x-service-name: kimber-storage
     * Strict-Transport-Security: max-age=31536000
     *
     * {"objectId":"63d9fa6530b4987fb6526e7d","updatedAt":"2023-07-23T02:32:54.206Z"}
     *
     * DELETE /1.1/files/649a68fdcd1a0bc9192e68a3 HTTP/1.1
     * X-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0
     * X-LC-Session: {sessionToken}
     * User-Agent: LeanCloud-CSharp-SDK/1.0.3
     * Accept: application/json
     * X-LC-Id: rAK3FfdieFob2Nn8Am
     * Content-Length: 0
     * Host: rak3ffdi.cloud.tds1.tapapis.cn
     * Connection: close
     *
     * HTTP/1.1 200 OK
     * Server: uewaf/4.0.1
     * Date: Sun, 23 Jul 2023 02:33:01 GMT
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 2
     * Connection: close
     * x-envoy-upstream-service-time: 40
     * x-service-name: kimber-storage
     * Strict-Transport-Security: max-age=31536000
     *
     * {}
     * */
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
    private static final SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode("6Jaa0qVAJZuXkZCLiOa/Ax5tIZVu+taKUN1V1nqwkks="), "AES");
    private static final IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode("Kk/wisgNYwcAV8WVGMgyUw=="));
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