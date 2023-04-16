package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteSerialize {
    static void requiredRead(Object object, byte[] data) {
        final var fields = object.getClass().getFields();
        try {
            byte index = 0;
            for (final var field : fields) {
                if (field.getType() == boolean.class) {
                    field.setBoolean(object, Util.getBit(data[0], index));
                    index++;
                }
            }
            var position = 1;
            for (final var field : fields) {
                if (field.getType() == String.class) {
                    final byte length = data[position++];
                    field.set(object, new String(data, position, length));
                    position += length;
                }
            }
            for (final var field : fields) {
                if (field.getType() == float.class) {
                    field.setFloat(object, Float.intBitsToFloat(getInt(data, position)));
                    position += 4;
                }
            }
            for (final var field : fields) {
                if (field.getType() == int.class) {
                    field.setInt(object, getVarInt(data, position));
                    position += data[position] >= 0 ? 1 : 2;
                }
            }
            for (final var field : fields) {
                if (field.getType() == short.class) {
                    field.setShort(object, getShort(data, position));
                    position += 2;
                }
            }
            for (final var field : fields) {
                if (field.getType() == int[].class) {
                    var array = (int[]) field.get(object);
                    for (var i = 0; i < array.length; i++) {
                        array[i] = getVarInt(data, position);
                        position += data[position] >= 0 ? 1 : 2;
                    }
                }
            }
            for (final var field : fields) {
                if (field.getType() == byte.class)
                    field.setShort(object, data[position++]);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
    static byte[] requiredWrite(Object object) throws IOException {
        final var fields = object.getClass().getFields();
        try (var outputStream = new ByteArrayOutputStream()) {
            byte b = 0;
            byte index = 0;
            for (final var field : fields) {
                if (field.getType() == boolean.class) {
                    b = Util.modifyBit(b, index, field.getBoolean(object));
                    index++;
                }
            }
            outputStream.write(b);
            for (final var field : fields) {
                if (field.getType() == String.class) {
                    final var bytes = ((String) field.get(object)).getBytes();
                    outputStream.write(bytes.length);
                    outputStream.writeBytes(bytes);
                }
            }
            for (final var field : fields) {
                if (field.getType() == float.class)
                    outputStream.writeBytes(int2bytes(Float.floatToIntBits(field.getFloat(object))));
            }
            for (final var field : fields) {
                if (field.getType() == int.class)
                    outputStream.writeBytes(varInt2bytes(field.getInt(object)));
            }
            for (final var field : fields) {
                if (field.getType() == short.class)
                    outputStream.writeBytes(short2bytes(field.getShort(object)));
            }
            for (final var field : fields) {
                if (field.getType() == int[].class) {
                    var array = (int[]) field.get(object);
                    for (int i : array)
                        outputStream.writeBytes(varInt2bytes(i));
                }
            }
            for (final var field : fields) {
                if (field.getType() == byte.class)
                    outputStream.write(field.getByte(object));
            }
            return outputStream.toByteArray();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getInt(byte[] data, int position) {
        return data[position + 3] << 24 ^ Byte.toUnsignedInt(data[position + 2]) << 16 ^ Byte.toUnsignedInt(data[position + 1]) << 8 ^ Byte.toUnsignedInt(data[position]);
    }

    private static byte[] int2bytes(int num) {
        final var bytes = new byte[4];
        bytes[0] = (byte) num;
        bytes[1] = (byte) (num >>> 8 & 0xff);
        bytes[2] = (byte) (num >>> 16 & 0xff);
        bytes[3] = (byte) (num >>> 24 & 0xff);
        return bytes;
    }

    private static short getShort(byte[] data, int position) {
        return (short) (Byte.toUnsignedInt(data[position + 1]) << 8 ^ Byte.toUnsignedInt(data[position]));
    }

    private static byte[] short2bytes(short num) {
        final var bytes = new byte[2];
        bytes[0] = (byte) num;
        bytes[1] = (byte) (num >>> 8);
        return bytes;
    }

    private static int getVarInt(byte[] data, int position) {
        if (data[position] >= 0)
            return data[position];
        else
            return data[position + 1] << 7 ^ data[position] & 0x7f;
    }

    private static byte[] varInt2bytes(int num) {
        if (num < 128)
            return new byte[] {(byte) num};
        else
            return new byte[] {(byte) (num & 0xff | 0b10000000), (byte) (num >>> 7)};
    }
}