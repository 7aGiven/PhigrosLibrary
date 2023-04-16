package given.phigros;

public class GameKeyValue {
    public boolean readCollection;
    public boolean unlockSingle;
    public byte collection;
    public boolean illustration;
    public boolean avatar;

    byte get(int index) {
        switch (index) {
            case 0:
                return b2b(readCollection);
            case 1:
                return b2b(unlockSingle);
            case 2:
                return collection;
            case 3:
                return b2b(illustration);
            case 4:
                return b2b(avatar);
        }
        throw new RuntimeException("get参数超出范围。");
    }

    void set(int index, byte b) {
        switch (index) {
            case 0:
                readCollection = b2b(b);
                return;
            case 1:
                unlockSingle = b2b(b);
                return;
            case 2:
                collection = b;
                return;
            case 3:
                illustration = b2b(b);
                return;
            case 4:
                avatar = b2b(b);
                return;
        }
        throw new RuntimeException("set参数超出范围。");
    }

    private boolean b2b(byte b) {
        if (b == 0)
            return false;
        else if (b == 1)
            return true;
        else
            throw new RuntimeException("b2b参数超出范围。");
    }

    private byte b2b(boolean b) {
        return b ? (byte) 1 : (byte) 0;
    }
}
