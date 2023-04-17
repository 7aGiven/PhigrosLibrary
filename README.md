本项目分为两个部分，分别为PhigrosRpc和PhigrosLibrary。

PhigrosLibrary是Java实现的Phigros云存档解析库。

PhigrosRpc是利用thrift对PhigrosLibrary的封装。

以下所有jre11或jdk11都指jre11以上或jdk11以上

**注：此项目为逆向成果，未非调用任何第三方接口。**

**注：严禁大规模查分对鸽游服务器进行DDOS。**

**注：个人项目，未学习法律，目的为了让大家可以开发自己的查分机器人。**

# 更新资源

1. [定数表](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/difficulty.csv)
2. [头像id](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/avater.txt)
3. [收藏品id](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/collection.txt)

# PhigrosRpc

基于thrift 0.16.0，PhigrosLibrary 0.5

### 功能

查询Best N数据和可推分的目标ACC数据

### 快速使用

#### 服务端启动

已安装jre11的用户:

下载Release内的PhigrosRpc-nojre-0.5.zip

解压后windows运行start.cmd，linux运行start.sh，默认监听127.0.0.1:9090

未安装jre11的windows用户：

下载Release内的 PhigrosRpc-jre11-windows-0.5.zip

解压后运行start.cmd，默认监听127.0.0.1:9090

未安装jre11的linux用户：

自行安装

#### 客户端编写

使用phigros.thrift生成您使用语言的代码。`thrift --gen py phigros.thrift`

示例：[使用python编写的示例](https://github.com/7aGiven/PhigrosLibrary/tree/master/clientExample)

### phigrosLibrary.thrift内的定义
```thrift
enum Level {
	EZ, HD, IN, AT
}

struct Summary {
	1: required string saveUrl;//存档Url
	2: required i8 saveVersion;//存档版本
	3: required i16 challenge; //课题分
	4: required double rks;    //总RKS
	5: required i8 gameVersion;//客户端版本
	6: required string avatar; //头像
}

struct SongLevel {
	1: required string id;   //曲目id
	2: required Level level;
	3: required i32 s;       //分数
	4: required double a;    //准确率
	5: required bool c;      //是否Full Combo
	6: required double difficulty;//定数
	7: required double rks;  //单曲RKS
}

struct SongExpect {
	1: required string id;    //曲目id
	2: required Level level;
	3: required double acc;   //现在ACC
	4: required double expect;//目标ACC
}

//sessionToken为25位字符串，saveUrl为存档URL，需要通过getSaveUrl方法获取
service Phigros {
	Summary getSaveUrl(1:string sessionToken);        //获取saveUrl和其他
	list<SongLevel> best19(1:string saveUrl);         //最佳phi和最佳前19个
	list<SongLevel> bestn(1:string saveUrl, 2:i8 num);//最佳phi和最佳前N个
	list<SongExpect> songExpects(1:string saveUrl);   //所有可推分歌曲及其目标ACC
}
```

# PhigrosLibrary

基于Phigros 2.4.7
至 Phigros 2.5.1

### 功能

对Phigros云存档的序列化和反序列化(gameProgress最后一个字节未知)

封装并优化了常用函数(B19,BestN,目标ACC)

### Java开发者使用PhigrosLibrary

方法1必须以JDK 11开发

方法1：

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

方法2：

下载Release内jar文件

放入您的项目根目录的libs文件夹下

在需要引用PhigrosLibrary的项目的build.gradle里修改 dependencies
```groovy
dependencies {
    implementation files('libs/PhigrosLibrary-0.4.jar')
}
```

### PhigrosLibrary 快速使用

以下代码获取了Phigros账户的B19信息和推分信息。

PhigrosUser.readInfo为读取定数信息，本类库不保存定数信息。

[定数表](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/difficulty.csv)

PhigrosUser对象执行update方法可以更新存档URL，否则会输出旧的B19图
```java
class Main {
    public static void main(String[] args) {
        PhigrosUser.readInfo(bufferReader);
        var user = new PhigrosUser(sessionToken);
        Summary summary = user.update();
        SongLevel[] songLevels = user.getB19();
        SongExpect[] accAll = user.getExpects(); //获取所有可推分歌曲的acc和目标acc
        SongExpect acc = user.getExpect("青芽.茶鸣拾贰律");
    }
}
```
Summary结构(未完全解析)
```java
public final class Summary {
    public final byte saveVersion;       //存档版本
    public final short challengeModeRank;//课题分
    public final float rankingScore;     //rks
    public final byte gameVersion;       //客户端版本号
    public final String avatar;          //头像
}
```
Level结构
```java
public enum Level {
    EZ,HD,IN,AT
}
```
SongLevel的结构是这样的。
```java
public class SongLevel implements Comparable<SongLevel>{
    public String id;  //曲目Id
    public Level level;
    public int s;      // 分数
    public float a;    //ACC
    public boolean c;  //FC
    public float difficulty;// 定数
    public float rks;  // 单曲rks
    @Override
    public int compareTo(SongLevel songLevel) {
        return Double.compare(songLevel.rks, rks);
    }
}
```
SongExpect的结构是这样的。
```java
public class SongExpect implements Comparable<SongExpect> {
    public String id;   //曲目Id
    public Level level;
    public float acc;
    public float expect;//目标ACC
    @Override
    public int compareTo(SongExpect songExpect) {
        return Float.compare(expect - acc, songExpect.expect - songExpect.acc);
    }
}
```

### PhigrosUser的public方法
```java
public class PhigrosUser {
    public String session;
    public URI saveUrl;
    public PhigrosUser(String session);//使用SessionToken初始化
    public PhigrosUser(URI saveUrl);//使用saveUrl初始化
    public static void readInfo(BufferedReader reader);//读取定数表
    public Summary update();//使用SessionToken更新saveUrl，并返回Summary
    public SongLevel[] getB19();//获取Best Phi和Best 19
    public SongLevel[] getBestN(int num);//获取Best Phi和Best N
    public SongExpect[] getExpect(String id);//获取曲目的所有等级的ACC和目标ACC
    public SongExpect[] getExpects();//获取所有可推分歌曲的ACC和目标ACC
    public <T extends GameExtend> T get(Class<T> clazz);//获取类为clazz的存档(详情高级应用)
    public <T extends GameExtend> void modify(Class<T> clazz, ModifyStrategy<T> strategy);//修改存档
    public void downloadSave(Path path);//备份存档到path
    public void uploadSave(Path path);  //从path恢复存档
}
```

### PhigrosLibrary的高级应用

注意：如果只想查询B19和ACC，请使用快速使用的例子，PhigrosUser内的对这两个常用情景有优化。

Phigros云存档包含5部分内容和Summary

gameRecord, gameKey, gameProgress, user, settings和Summary

其中gameRecord和gameKey为字典结构，其他四个是普通的结构。

获取这5个对象的方法：
```java
class Main {
    public static void main(String[] args) {
        PhigrosUser.readInfo(bufferReader);
        var user = new PhigrosUser(sessionToken);
        Summary summary = user.update();
        user.get(GameRecord.class);
        user.get(GameKey.class);
        user.get(GameProgress.class);
        user.get(GameUser.class);
        user.get(GameSettings.class);
    }
}
```
Summary结构
```java
public final class Summary {
    public byte saveVersion;       //存档版本
    public short challengeModeRank;//课题分
    public float rankingScore;     //rks
    public byte gameVersion;       //客户端版本号
    public String avatar;          //头像
    public short[] cleared = new short[4];  //完成曲目数量
    public short[] fullCombo = new short[4];//FC曲目数量
    public short[] phi = new short[4];      //AP曲目数量
}
```
GameSettings结构
```java
public class GameSettings {
    public boolean chordSupport;     //多押辅助
    public boolean fcAPIndicator;    //开启FC/AP指示器
    public boolean enableHitSound;   //开启打击音效
    public boolean lowResolutionMode;//低分辨率模式
    public String deviceName;        //设备名
    public float bright;             //背景亮度
    public float musicVolume;        //音乐音量
    public float effectVolume;       //界面音效音量
    public float hitSoundVolume;     //打击音效音量
    public float soundOffset;        //铺面延迟
    public float noteScale;          //按键缩放
}
```
GameUser结构
```java
public class GameUser {
    public boolean showPlayerId;//右上角展示用户id
    public String selfIntro;    //自我介绍
    public String avatar;       //头像
    public String background;   //曲绘
}
```
GameProgress结构(最后一个字节未知，一直是0,不知道干嘛的)
```java
class GameProgress {
    public boolean isFirstRun;                //首次运行
    public boolean legacyChapterFinished;     //过去的章节已完成
    public boolean alreadyShowCollectionTip;  //已展示收藏品Tip
    public boolean alreadyShowAutoUnlockINTip;//已展示自动解锁IN Tip
    public String completed;          //剧情完成(显示全部歌曲和课题模式入口)
    public int songUpdateInfo;        //？？？
    public short challengeModeRank;   //课题分
    public int[] money = new int[5];  //data货币
    public byte unlockFlagOfSpasmodic;//痉挛解锁
    public byte unlockFlagOfIgallta;  //Igallta解锁
    public byte unlockFlagOfRrharil;  //Rrhar'il解锁
    public byte flagOfSongRecordKey;  //AT解锁(倒霉蛋,船,Shadow,心之所向,inferior)
}
```
对于GameRecord的结构(Map)
```java
public class GameRecord extends LinkedHashMap<String, LevelRecord[]> implements GameExtend {}
public class LevelRecord {
    public boolean c;//Full Combo
    public int s;    //分数
    public float a;  //ACC
}
```
对于GameKey的结构(Map)
```java
public class GameKey extends LinkedHashMap<String, GameKeyValue> implements GameExtend {
    public byte lanotaReadKeys;   //是否读取Lanota收藏品(解锁倒霉蛋和船的AT)
}
public class GameKeyValue {
    public boolean readCollection;//读收藏品
    public boolean unlockSingle;  //解锁单曲
    public byte collection;       //收藏品
    public boolean illustration;  //曲绘
    public boolean avatar;        //头像
}
```
修改存档请使用
`<T extend GameExtend> PhigrosUser.modify(Class<T> clazz, ModifyStrategy<T> strategy)`
```java
@FunctionalInterface
interface ModifyStrategy<T extends GameExtend> {
    T apply(T data) throws IOException;
}
```
修改存档示例
```java
class Main {
    public static void main(String[] args) {
        var user = new PhigrosUser(sessionToken);
        user.update();
        String songId = "青芽.茶鸣拾贰律";
        int level = Level.IN.ordinal();
        int s = 1000000;
        float a = 100f;
        boolean c = true;
        user.modify(GameRecord.class, gameRecord -> {
            for (String id:gameRecord.keySet()) {
                if (id.equals(songId)) {
                    LevelRecord[] value = gameRecord.get(songId);
                    value[level].s = s;
                    value[level].a = a;
                    value[level].c = c;
                }
                for (String id:item) {
                    if (id.equals(songId))
                        item.modifySong(level, score, acc, fc);
                }
            }
        });
    }
}
```

### Phigros QQ群
加入 282781492 闲聊