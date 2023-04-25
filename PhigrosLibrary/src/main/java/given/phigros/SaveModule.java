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
        var length = SaveModule.getVarInt(data, 0);
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
            outputStream.writeBytes(SaveModule.varInt2bytes(size()));
            for (final var entry : entrySet())
                getBytes(outputStream, entry);
            if (this instanceof GameKey)
                outputStream.write(((GameKey) this).lanotaReadKeys);
            return outputStream.toByteArray();
        }
    }
}

interface SaveModule {
    default void loadFromBinary(byte[] data) {
        final var fields = getClass().getFields();
        try {
            byte index = 0;
            for (final var field : fields) {
                if (field.getType() == boolean.class)
                    field.setBoolean(this, Util.getBit(data[0], index++));
            }
            var position = 1;
            for (final var field : fields) {
                if (field.getType() == String.class) {
                    final byte length = data[position++];
                    field.set(this, new String(data, position, length));
                    position += length;
                }
            }
            for (final var field : fields) {
                if (field.getType() == float.class) {
                    field.setFloat(this, Float.intBitsToFloat(getInt(data, position)));
                    position += 4;
                }
            }
            for (final var field : fields) {
                if (field.getType() == int.class) {
                    field.setInt(this, getVarInt(data, position));
                    position += data[position] >= 0 ? 1 : 2;
                }
            }
            for (final var field : fields) {
                if (field.getType() == short.class) {
                    field.setShort(this, getShort(data, position));
                    position += 2;
                }
            }
            for (final var field : fields) {
                if (field.getType() == int[].class) {
                    var array = (int[]) field.get(this);
                    for (var i = 0; i < array.length; i++) {
                        array[i] = getVarInt(data, position);
                        position += data[position] >= 0 ? 1 : 2;
                    }
                }
            }
            for (final var field : fields) {
                if (field.getType() == byte.class)
                    field.setByte(this, data[position++]);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    default byte[] serialize() throws IOException {
        final var fields = getClass().getFields();
        try (var outputStream = new ByteArrayOutputStream()) {
            byte b = 0;
            byte index = 0;
            for (final var field : fields) {
                if (field.getType() == boolean.class)
                    b = Util.modifyBit(b, index++, field.getBoolean(this));
            }
            outputStream.write(b);
            for (final var field : fields) {
                if (field.getType() == String.class) {
                    final var bytes = ((String) field.get(this)).getBytes();
                    outputStream.write(bytes.length);
                    outputStream.writeBytes(bytes);
                }
            }
            for (final var field : fields) {
                if (field.getType() == float.class)
                    outputStream.writeBytes(int2bytes(Float.floatToIntBits(field.getFloat(this))));
            }
            for (final var field : fields) {
                if (field.getType() == int.class)
                    outputStream.writeBytes(varInt2bytes(field.getInt(this)));
            }
            for (final var field : fields) {
                if (field.getType() == short.class)
                    outputStream.writeBytes(short2bytes(field.getShort(this)));
            }
            for (final var field : fields) {
                if (field.getType() == int[].class) {
                    var array = (int[]) field.get(this);
                    for (int i : array)
                        outputStream.writeBytes(varInt2bytes(i));
                }
            }
            for (final var field : fields) {
                if (field.getType() == byte.class)
                    outputStream.write(field.getByte(this));
            }
            return outputStream.toByteArray();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static int getInt(byte[] data, int position) {
        return data[position + 3] << 24 ^ Byte.toUnsignedInt(data[position + 2]) << 16 ^ Byte.toUnsignedInt(data[position + 1]) << 8 ^ Byte.toUnsignedInt(data[position]);
    }

    static byte[] int2bytes(int num) {
        final var bytes = new byte[4];
        bytes[0] = (byte) num;
        bytes[1] = (byte) (num >>> 8 & 0xff);
        bytes[2] = (byte) (num >>> 16 & 0xff);
        bytes[3] = (byte) (num >>> 24 & 0xff);
        return bytes;
    }

    static short getShort(byte[] data, int position) {
        return (short) (Byte.toUnsignedInt(data[position + 1]) << 8 ^ Byte.toUnsignedInt(data[position]));
    }

    static byte[] short2bytes(short num) {
        final var bytes = new byte[2];
        bytes[0] = (byte) num;
        bytes[1] = (byte) (num >>> 8);
        return bytes;
    }

    static int getVarInt(byte[] data, int position) {
        if (data[position] >= 0)
            return data[position];
        else
            return data[position + 1] << 7 ^ data[position] & 0x7f;
    }

    static byte[] varInt2bytes(int num) {
        if (num < 128)
            return new byte[] {(byte) num};
        else
            return new byte[] {(byte) (num & 0xff | 0b10000000), (byte) (num >>> 7)};
    }
}