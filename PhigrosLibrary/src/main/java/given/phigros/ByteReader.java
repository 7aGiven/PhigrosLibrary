package given.phigros;

import java.nio.ByteBuffer;

class ByteReader {
    byte[] data;
    int position;

    ByteReader(byte[] data) {
        this.data = data;
    }

    ByteReader(byte[] data, int position) {
        this.data = data;
        this.position = position;
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

    void putInt(int num) {
        data[position] = (byte) num;
        data[position + 1] = (byte) (num >>> 8 & 0xff);
        data[position + 2] = (byte) (num >>> 16 & 0xff);
        data[position + 3] = (byte) (num >>> 24 & 0xff);
        position += 4;
    }

    float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    void putFloat(float num) {
        putInt(Float.floatToIntBits(num));
    }

    short getVarShort() {
        if(Util.getBit(data[position],7)) {
            position += 2;
            return (short) (0b01111111 & data[position - 2] ^ data[position - 1] << 7);
        }
        else
            return data[position++];
    }

    void skipVarShort() {
        if(data[position] < 0)
            position += 2;
        else
            position++;
    }

    String getString() {
        byte length = getByte();
        position += length;
        return new String(data, position - length, length);
    }

    void skipString() {
        position += getByte();
    }

    void insertBytes(byte[] bytes) {
        final var result = new byte[data.length + bytes.length];
        System.arraycopy(data, 0, result, 0, position);
        System.arraycopy(bytes, 0, result, position, bytes.length);
        System.arraycopy(data, position, result, position + bytes.length, data.length - position);
        data = result;
    }

    void replaceBytes(int length, byte[] bytes) {
        if (bytes.length == length) {
            System.arraycopy(bytes, 0, data, position, length);
            return;
        }
        final var result = new byte[data.length + bytes.length - length];
        System.arraycopy(data, 0, result, 0, position);
        System.arraycopy(bytes, 0, result, position, bytes.length);
        System.arraycopy(data, position + length, result, position + bytes.length, data.length - position - length);
        data = result;
    }
}
