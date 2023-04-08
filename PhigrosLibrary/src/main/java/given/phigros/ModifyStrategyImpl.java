package given.phigros;

import java.io.IOException;

class ModifyStrategyImpl {
    public static final short challengeScore = 3;
    public static void song(PhigrosUser user, String name, int level, int s, float a, boolean fc) throws IOException, InterruptedException {
        SaveManager.modify(user,challengeScore,"gameRecord", data -> {
            boolean exist = false;
            GameRecord gameRecord = new GameRecord(data);
            for (GameRecordItem item:gameRecord) {
                if (item.getId().equals(name)) {
                    exist = true;
                    item.modifySong(level,s,a,fc);
                    data = gameRecord.getData();
                    break;
                }
            }
            if (!exist) {
                throw new RuntimeException("您尚未游玩此歌曲");
            }
            return data;
        });
    }
    public static void avater(PhigrosUser user, String avater) throws IOException, InterruptedException {
        SaveManager.modify(user,challengeScore,"gameKey", data -> {
            GameKey gameKey = new GameKey(data);
            boolean exist = false;
            for (GameKeyItem item:gameKey) {
                if (item.getId().equals(avater)) {
                    exist = true;
                    if (item.getAvater())
                        throw new RuntimeException("您已经拥有该头像");
                    item.setAvater(true);
                    data = gameKey.getData();
                    break;
                }
            }
            if (!exist) {
                gameKey.addKey(avater, new byte[] {16, 1});
                data = gameKey.getData();
            }
            return data;
        });
    }
    public static void collection(PhigrosUser user, String collection) throws IOException, InterruptedException {
        SaveManager.modify(user,challengeScore,"gameKey", data -> {
            GameKey gameKey = new GameKey(data);
            boolean exist = false;
            for (GameKeyItem item:gameKey) {
                if (item.getId().equals(collection)) {
                    exist = true;
                    item.setCollection((byte) (item.getCollection() +  1));
                    data = gameKey.getData();
                    break;
                }
            }
            if (!exist) {
                gameKey.addKey(collection, new byte[] {4, 1});
                data = gameKey.getData();
            }
            return data;
        });
    }
    public static void challenge(PhigrosUser user, short score) throws IOException, InterruptedException {
        SaveManager.modify(user,challengeScore,"gameProgress", data -> {
            final var gameProgress = new GameProgress(data);
            gameProgress.setChallenge(score);
            return gameProgress.getData();
        });
    }
    public static void data(PhigrosUser user, short num) throws IOException, InterruptedException {
        SaveManager.modify(user,challengeScore,"gameProgress", data -> {
            final var gameProgress = new GameProgress(data);
            gameProgress.setGameData(num);
            return gameProgress.getData();
        });
    }
}