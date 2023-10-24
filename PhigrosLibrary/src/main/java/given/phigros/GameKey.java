package given.phigros;

public class GameKey extends MapSaveModule<GameKeyValue> {
    final static String name = "gameKey";
    final static byte version = 2;
    @Order(0)
    public byte lanotaReadKeys;
    @Order(1)
    public boolean camelliaReadKey;

    void output(ByteWriter writer, GameKeyValue value) {
        byte length = 0;
        byte num = 1;
        for (byte index = 0; index < 5; index++) {
            if (value.get(index) != 0) {
                length = Util.modifyBit(length, index, true);
                num++;
            }
        }
        writer.putByte(num);
        writer.putByte(length);
        for (byte index = 0; index < 5; index++) {
            if (value.get(index) != 0)
                writer.putByte(value.get(index));
        }
    }

    GameKeyValue input(ByteReader reader) {
        byte len = reader.getByte();
        final GameKeyValue value = new GameKeyValue();
        for (byte index = 0; index < 5; index++) {
            if (Util.getBit(len, index))
                value.set(index, reader.getByte());
        }
        return value;
    }
}
