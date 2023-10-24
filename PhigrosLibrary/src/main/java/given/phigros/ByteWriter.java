package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ByteWriter {
    final ByteArrayOutputStream outputStream;
    ByteWriter(ByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
    }
    void putByte(int num) {
        outputStream.write(num);
    }
    void putShort(short num) {
        outputStream.write((byte) num);
        outputStream.write(num >> 8);
    }
    void putInt(int num) {
        outputStream.write((byte) num);
        outputStream.write(num >> 8 & 0xFF);
        outputStream.write(num >> 16 & 0xFF);
        outputStream.write(0);
    }
    void putFloat(float f) {
        int i = Float.floatToIntBits(f);
        outputStream.write(i & 0xFF);
        outputStream.write(i >> 8 & 0xFF);
        outputStream.write(i >> 16 & 0xFF);
        outputStream.write(i >> 24 & 0xFF);
    }
    void putVarshort(short num) {
        if (num < 128)
            outputStream.write(num);
        else {
            outputStream.write(0b01111111 & num | 0b10000000);
            outputStream.write(num >> 7);
        }
    }
    void putString(String str) throws IOException {
        byte[] b = str.getBytes();
        putVarshort((short) b.length);
        outputStream.write(b);
    }
}
