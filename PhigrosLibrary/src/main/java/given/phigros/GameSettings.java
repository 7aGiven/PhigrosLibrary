package given.phigros;

public class GameSettings {
    private final ByteReader reader;
    GameSettings(byte[] data) {
        reader = new ByteReader(data);
    }
    public String getDevice() {
        reader.position = 1;
        return reader.getString();
    }
    public float 背景亮度() {
        return getItem(0);
    }
    public float 音乐音量() {
        return getItem(1);
    }
    public float 界面音效音量() {
        return getItem(2);
    }
    public float 打击音效音量() {
        return getItem(3);
    }
    public float 铺面延迟() {
        return getItem(4);
    }
    public float 按键缩放() {
        return getItem(5);
    }
    private float getItem(int index) {
        reader.position = 1;
        reader.skipString();
        for (int i = 0; i < index; i++) {
            reader.position += 4;
        }
        return reader.getFloat();
    }
}
