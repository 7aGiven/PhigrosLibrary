package given.phigros;

class ByteReader {
    byte[] data;
    int position;

    ByteReader(byte[] data) {
        this.data = data;
    }

    byte getByte() {
        return data[position++];
    }

    void putByte(byte num) {
        data[position++] = num;
    }

    short getShort() {
        position += 2;
        return (short) (data[position - 1] << 8 ^ Byte.toUnsignedInt(data[position - 2]));
    }

    void putShort(short num) {
        data[position++] = (byte) num;
        data[position++] = (byte) (num >>> 8);
    }

    int getInt() {
        position += 4;
        return data[position - 1] << 24 ^ Byte.toUnsignedInt(data[position - 2]) << 16 ^ Byte.toUnsignedInt(data[position - 3]) << 8 ^ Byte.toUnsignedInt(data[position - 4]);
    }

    float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    void putFloat(float f) {
        int num = Float.floatToIntBits(f);
        data[position] = (byte) num;
        data[position + 1] = (byte) (num >>> 8 & 0xff);
        data[position + 2] = (byte) (num >>> 16 & 0xff);
        data[position + 3] = (byte) (num >>> 24 & 0xff);
        position += 4;
    }

    short getVarshort() {
        if(data[position] < 0) {
            position += 2;
            return (short) (0b01111111 & data[position - 2] ^ data[position - 1] << 7);
        }
        else
            return data[position++];
    }

    String getString(int offset) {
        short len = getVarshort();
        position += len;
        return new String(data, position - len, len - offset);
    }

    void putString(String s) {
        final byte[] b = s.getBytes();
        data[position++] = (byte) b.length;
        System.arraycopy(b, 0, data, position, b.length);
    }
}
