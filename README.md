# PhigrosLibrary

基于Phigros 2.4.7
至 Phigros 2.5.1

### Java开发者使用PhigrosLibrary

下载项目源码

复制PhigrosLibrary目录到您的项目根目录

在您的项目根目录的settings.gradle添加一行

`include 'PhigrosLibrary'`

在需要引用PhigrosLibrary的项目的build.gradle里修改 dependencies

```groovy
dependencies {
    implementation project(':PhigrosLibrary')
}
```

### 其他语言开发者使用PhigrosLibrary
如果有人提issue，可能会做。

初步考虑是通过grpc来通信。

### 功能

获取B19数组

获取所有已打过的可推分曲的目标ACC

修改存档已打过歌分数

修改存档课题模式等级

修改存档data(1024MB以内)

添加存档头像

添加存档收藏品

### PhigrosLibrary 快速使用

以下代码获取了Phigros账户的B19信息和推分信息。

PhigrosUser.readInfo为读取定数信息，本类库不保存定数信息和曲绘信息。
定数表为一个csv文件，项目根目录可查看其结构。

PhigrosUser对象执行update方法可以更新存档URL，否则会输出旧的B19图
```java
class Main {
    public static void main(String[] args) {
        PhigrosUser.readInfo(bufferReader);
        var user = new PhigrosUser(sessionToken);
        user.update();
        SongLevel[] songLevels = user.getB19();
        SongExpect[] accAll = user.getExpects(); //获取所有可推分歌曲的acc和目标acc
        SongExpect acc = user.getExpect("青芽.茶鸣拾贰律");
    }
}
```
SongLevel的结构是这样的。
```java
class SongLevel implements Comparable<SongLevel>{
    public String id;        //曲目Id
    public int level;        // 0:EZ / 1:HD / 2:IN / 3:AT
    public int score;        // 分数
    public float acc; 
    public boolean fc;
    public float difficulty; // 定数
    public float rks;        // 计算出的rks
    @Override
    public int compareTo(SongLevel songLevel) {
        return Double.compare(songLevel.rks, rks);
    }
}
```
SongExpect的结构是这样的。
```java
class SongExpect implements Comparable<SongExpect> {
    public String id;
    public String name;
    public int level;
    public float acc;
    public float expect;
    @Override
    public int compareTo(SongExpect songExpect) {
        return Float.compare(expect - acc, songExpect.expect - songExpect.acc);
    }
}
```

### PhigrosLibrary的高级应用

注意：如果只想查询B19和ACC，请使用快速使用的例子，PhigrosUser内的对这两个常用情景有优化。

Phigros云存档包含5部分内容

gameRecord, gameKey, gameProgress, user, settings

其中gameRecord和gameKey为数组结构，其他三个是普通的结构。
```java
class Main {
    public static void main(String[] args) {
        PhigrosUser.readInfo(bufferReader);
        var user = new PhigrosUser(sessionToken);
        user.update();
        user.getGameRecord();
        user.getGameKey();
        user.getGameProgress();
        user.getGameUser();
        user.getGameSettings()
    }
}
```
```java
public class GameSettings {
    GameSettings(byte[] data);
    public String getDevice();
    public float 背景亮度();
    public float 音乐音量();
    public float 界面音效音量();
    public float 打击音效音量();
    public float 铺面延迟();
    public float 按键缩放();
}
```
```java
public class GameUser {
    GameUser(byte[] data);
    public String getIntroduction();
    public String getAvater();
    public String getIllustration();
}
```
```java
class GameProgress {
    private final ByteReader reader;
    GameProgress(byte[] data);
    public short getChallenge();
    public void setChallenge(short score);
    public int getGameData();
    public void setGameData(short MB);
    public byte[] getData();
}
```
对于GameRecord的使用(修改分数)

该方法已经被user.modifySong(String songId, int level, int score, float acc, boolean fc)实现
```java
class Main {
    public static void main(String[] args) {
        var user = new PhigrosUser(sessionToken);
        user.update();
        String songId = "青芽.茶鸣拾贰律";
        user.modify("gameRecord", data -> {
            var gameRecord = new GameRecord(data);
            for (GameRecordItem item:gameRecord) {
                for (String id:item) {
                    if (id.equals(songId))
                        item.modifySong(level, score, acc, fc);
                }
            }
            byte[] data = gameRecord.getData();
        });
    }
}
```

对于GameKey的使用和GameRecord是一样的，for循环。

GameKey有5个属性为：读收藏品，单曲解锁，收藏品计数(一个收藏品里包含很多项)，曲绘，头像。
```java
package given.phigros;

class GameKeyItem {
    GameKeyItem(byte[] data);
    public String getId();
    public boolean getReadCollection();
    public void setReadCollection(boolean b);
    public boolean getSingleUnlock();
    public void setSingleUnlock(boolean b);
    public byte getCollection();
    public void setCollection(byte num);
    public boolean getIllustration();
    public void setIllustration(boolean b);
    public boolean getAvater();
    public void setAvater(boolean b);
}
```

### Phigros QQ群
加入 282781492 闲聊