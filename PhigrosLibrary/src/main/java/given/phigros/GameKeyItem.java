package given.phigros;

class GameKeyItem {
    final ByteReader reader;

    GameKeyItem(byte[] data) {
        reader = new ByteReader(data);
    }
    public String getId() {
        reader.position = 1;
        return reader.getString();
    }
    public boolean getReadCollection() {
        return getBoolKey(0);
    }
    public void setReadCollection(boolean b) {
        setBoolKey(0, b);
    }
    public boolean getSingleUnlock() {
        return getBoolKey(1);
    }
    public void setSingleUnlock(boolean b) {
        setBoolKey(1, b);
    }
    public byte getCollection() {
        return getKey(2);
    }
    public void setCollection(byte num) {
        setKey(2, (byte) num);
    }
    public boolean getIllustration() {
        return getBoolKey(3);
    }
    public void setIllustration(boolean b) {
        setBoolKey(3, b);
    }
    public boolean getAvater() {
        return getBoolKey(4);
    }
    public void setAvater(boolean b) {
        setBoolKey(4, b);
    }
    private boolean getBoolKey(int index) {
        switch (getKey(index)) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new RuntimeException("存档GameKey部分有误");
        }
    }
    private byte getKey(int index) {
        reader.position = 1;
        reader.skipString();
        reader.position++;
        byte key = reader.getByte();
        if (!Util.getBit(key,index))
            return 0;
        for (int i = 0; i < 5; i++) {
            if (!Util.getBit(key,i))
                continue;
            if (i == index)
                return reader.getByte();
            reader.position++;
        }
        throw new RuntimeException("存档GameKey部分有误");
    }
    private void setBoolKey(int index, boolean b) {
        setKey(index, b ? (byte) 1: (byte) 0);
    }
    private void setKey(int index, byte value) {
        reader.position = 1;
        reader.skipString();
        reader.position++;
        byte key = reader.getByte();
        if (Util.getBit(key, index)) {
            for (int i = 0; i < 5; i++) {
                if (!Util.getBit(key, i))
                    continue;
                if (i == index) {
                    reader.putByte(value);
                    return;
                }
                reader.position++;
            }
        } else {
            reader.data[reader.position - 2]++;
            key = Util.modifyBit(key, index, true);
            reader.data[reader.position - 2] = key;
            for (int i = 0; i < 5; i++) {
                if (!Util.getBit(key, i))
                    continue;
                if (i == index)
                    reader.insertBytes(new byte[] {value});
                reader.position++;
            }
        }
    }
}