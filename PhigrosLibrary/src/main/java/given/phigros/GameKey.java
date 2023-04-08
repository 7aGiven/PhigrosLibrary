package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class GameKey implements Iterable<GameKeyItem> {
    private final byte version;
    private final GameKeyItem[] array;
    private final ArrayList<GameKeyItem> list = new ArrayList<>();

    GameKey(byte[] data) {
        version = data[data.length - 1];
        final ArrayList<GameKeyItem> list = new ArrayList<>();
        int position = Util.getBit(data[0],7) ? 2 : 1;
        int start;
        while (position != data.length - 1){
            start = position;
            position = data[position] + 1;
            position = data[position] + 1;
            byte[] tmp = new byte[position - start];
            System.arraycopy(data, start, tmp, 0, position - start);
            list.add(new GameKeyItem(tmp));
        }
        array = list.toArray(GameKeyItem[]::new);
    }
    public void addKey(String key, byte[] data) {
        var byteString = key.getBytes();
        var result = new byte[byteString.length + data.length + 2];
        result[0] = (byte) byteString.length;
        System.arraycopy(byteString,0,result,1,byteString.length);
        result[byteString.length + 1] = (byte) data.length;
        System.arraycopy(data,0, result,byteString.length + 2, data.length);
        list.add(new GameKeyItem(result));
    }
    byte[] getData() throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            outputStream.writeBytes(Util.getVarShort(array.length + list.size()));
            for (int position = 0; position != array.length; position++)
                outputStream.writeBytes(array[position].reader.data);
            for (int position = 0; position != list.size(); position++)
                outputStream.writeBytes(list.get(position).reader.data);
            outputStream.writeBytes(Util.getVarShort(version));
            return outputStream.toByteArray();
        }
    }
    @Override
    public Iterator<GameKeyItem> iterator() {
        return new ArrayIterator<>(array);
    }
}
