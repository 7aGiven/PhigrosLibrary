package given.phigros;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Logger;

public class HttpConnection {
    HttpURLConnection connection;
    HttpConnection(String url) throws IOException {
        connection = (HttpURLConnection) new URL(url).openConnection();
    }
    HttpConnection(URL url) throws IOException {
        connection = (HttpURLConnection) url.openConnection();
    }
    HttpConnection header(String key, String value) {
        connection.setRequestProperty(key, value);
        return this;
    }
    HttpConnection pigeon(String sessionToken) {
        connection.setRequestProperty("X-LC-Id", "rAK3FfdieFob2Nn8Am");
        connection.setRequestProperty("X-LC-Key", "Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0");
        connection.setRequestProperty("User-Agent", "LeanCloud-CSharp-SDK/1.0.3");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-LC-Session", sessionToken);
        return this;
    }
    HttpConnection post(String body) throws IOException {
        connection.setRequestMethod("POST");
        if (body == null)
            return this;
        connection.setDoOutput(true);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes());
        }
        return this;
    }
    HttpConnection put(byte[] body) throws IOException {
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }
        return this;
    }
    HttpConnection put(String body) throws IOException {
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes());
        }
        return this;
    }
    HttpConnection delete() throws ProtocolException {
        connection.setRequestMethod("DELETE");
        return this;
    }

    int connect() throws IOException {
        connection.connect();
        return connection.getResponseCode();
    }
    byte[] bytes() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = new byte[15 * 1024];
                while (true) {
                    int len = inputStream.read(bytes);
                    if (len == -1)
                        break;
                    Logger.getGlobal().warning(String.valueOf(len));
                    outputStream.write(bytes, 0, len);
                }
            }
            return outputStream.toByteArray();
        }
    }
    public JSONObject json() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            connection.connect();
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = new byte[15 * 1024];
                while (true) {
                    int len = inputStream.read(bytes);
                    if (len == -1)
                        break;
                    Logger.getGlobal().warning(String.valueOf(len));
                    outputStream.write(bytes, 0, len);
                }
            }
            return JSON.parseObject(outputStream.toByteArray());
        }
    }
    void disconnect() {
        connection.disconnect();
    }
}
