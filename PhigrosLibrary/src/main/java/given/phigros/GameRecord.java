package given.phigros;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class GameRecord implements Iterable<GameRecordItem> {
    private final GameRecordItem[] array;

    GameRecord(byte[] data) {
        final ArrayList<GameRecordItem> list = new ArrayList<>();
        int position = Util.getBit(data[0],7) ? 2 : 1;
        int start;
        while (position != data.length){
            start = position;
            position += data[position] + 1;
            position += data[position] + 1;
            byte[] tmp = new byte[position - start];
            System.arraycopy(data, start, tmp, 0, position - start);
            list.add(new GameRecordItem(tmp));
        }
        array = list.toArray(GameRecordItem[]::new);
    }
    byte[] getData() throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            outputStream.writeBytes(Util.getVarShort(array.length));
            for (int position = 0; position != array.length; position++)
                outputStream.writeBytes(array[position].reader.data);
            return outputStream.toByteArray();
        }
    }
    @Override
    public Iterator<GameRecordItem> iterator() {
        return new ArrayIterator<>(array);
    }
}
