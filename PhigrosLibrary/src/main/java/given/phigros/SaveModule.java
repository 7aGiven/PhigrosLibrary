package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class MapSaveModule<T> extends LinkedHashMap<String, T> implements SaveModule {
    abstract void getBytes(ByteArrayOutputStream outputStream, Map.Entry<String, T> entry);

    abstract void putBytes(byte[] data, int position);

    @Override
    public void loadFromBinary(byte[] data) {
        clear();
        var length = SaveModule.getVarShort(data, 0);
        var position = data[0] < 0 ? 2 : 1;
        byte keyLength;
        for (; length > 0; length--) {
            putBytes(data, position);
            keyLength = data[position];
            position += keyLength + data[position + keyLength + 1] + 2;
        }
        if (this instanceof GameKey)
            ((GameKey) this).lanotaReadKeys = data[position];
    }

    @Override
    public byte[] serialize() throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            outputStream.writeBytes(SaveModule.varShort2bytes((short) size()));
            for (final var entry : entrySet())
                getBytes(outputStream, entry);
            if (this instanceof GameKey)
                outputStream.write(((GameKey) this).lanotaReadKeys);
            return outputStream.toByteArray();
        }
    }
}

public interface SaveModule {
    default void loadFromBinary(byte[] data) {
        try {
            byte index = 0;
            var position = 0;
            for (final var field : getClass().getFields()) {
                if (field.getType() == boolean.class) {
                    field.setBoolean(this, Util.getBit(data[position], index++));
                    continue;
                }
                if (index != 0) {
                    index = 0;
                    position++;
                }
                if (field.getType() == String.class) {
                    final byte length = data[position++];
                    field.set(this, new String(data, position, length));
                    position += length;
                } else if (field.getType() == float.class) {
                    field.setFloat(this, Float.intBitsToFloat(getInt(data, position)));
                    position += 4;
                } else if (field.getType() == short.class) {
                    field.setShort(this, getShort(data, position));
                    position += 2;
                } else if (field.getType() == short[].class) {
                    final var array = (short[]) field.get(this);
                    for (var i = 0; i < array.length; i++) {
                        array[i] = getVarShort(data, position);
                        position += data[position] >= 0 ? 1 : 2;
                    }
                } else if (field.getType() == byte.class)
                    field.setByte(this, data[position++]);
                else throw new RuntimeException("出现新类型。");
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    default byte[] serialize() throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            byte b = 0;
            byte index = 0;
            for (final var field : getClass().getFields()) {
                if (field.getType() == boolean.class) {
                    b = Util.modifyBit(b, index++, field.getBoolean(this));
                    continue;
                }
                if (b != 0 && index != 0) {
                    outputStream.write(b);
                    b = index = 0;
                }
                if (field.getType() == String.class) {
                    final var bytes = ((String) field.get(this)).getBytes();
                    outputStream.write(bytes.length);
                    outputStream.writeBytes(bytes);
                } else if (field.getType() == float.class)
                    outputStream.writeBytes(int2bytes(Float.floatToIntBits(field.getFloat(this))));
                else if (field.getType() == short.class)
                    outputStream.writeBytes(short2bytes(field.getShort(this)));
                else if (field.getType() == short[].class)
                    for (final var h : (short[]) field.get(this))
                        outputStream.writeBytes(varShort2bytes(h));
                else if (field.getType() == byte.class)
                    outputStream.write(field.getByte(this));
                else
                    throw new RuntimeException("出现新类型。");
            }
            return outputStream.toByteArray();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static int getInt(byte[] data, int position) {
        return data[position + 3] << 24 ^ (data[position + 2] & 0xff) << 16 ^ (data[position + 1] & 0xff) << 8 ^ (data[position] & 0xff);
    }

    static byte[] int2bytes(int num) {
        final var bytes = new byte[4];
        bytes[0] = (byte) num;
        bytes[1] = (byte) (num >>> 8 & 0xff);
        bytes[2] = (byte) (num >>> 16 & 0xff);
        bytes[3] = (byte) (num >>> 24);
        return bytes;
    }

    static short getShort(byte[] data, int position) {
        return (short) ((data[position + 1] & 0xff) << 8 ^ (data[position] & 0xff));
    }

    static byte[] short2bytes(short num) {
        final var bytes = new byte[2];
        bytes[0] = (byte) num;
        bytes[1] = (byte) (num >>> 8);
        return bytes;
    }

    static short getVarShort(byte[] data, int position) {
        if (data[position] >= 0)
            return data[position];
        else
            return (short) (data[position + 1] << 7 ^ data[position] & 0x7f);
    }

    static byte[] varShort2bytes(short num) {
        if (num < 128)
            return new byte[] {(byte) num};
        else
            return new byte[] {(byte) (num & 0x7f | 0b10000000), (byte) (num >>> 7)};
    }
}