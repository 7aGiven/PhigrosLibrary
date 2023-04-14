本项目分为两个部分，分别为PhigrosRpc和PhigrosLibrary。

PhigrosLibrary是Java实现的Phigros云存档解析库。

PhigrosRpc是利用thrift对PhigrosLibrary的封装。

以下所有jre11或jdk11都指jre11以上或jdk11以上

**注：此项目为逆向成果，未非调用任何第三方接口。**

**注：严禁大规模查分对鸽游服务器进行DDOS。**

**注：个人项目，未学习法律，目的为了让大家可以开发自己的查分机器人。**

# 更新资源

1. [定数表](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/difficulty.csv)
2. [头像Id](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/avater.txt)
3. [收藏品Id](https://raw.githubusercontent.com/7aGiven/PhigrosLibrary/master/collection.txt)

# PhigrosRpc

基于thrift 0.16.0，PhigrosLibrary 0.4

### 快速使用

#### 服务端启动

已安装jre11的用户:

下载Release内的PhigrosRpc-nojre-0.4.zip

解压后windows运行start.cmd，linux运行start.sh，默认监听127.0.0.1:9090

未安装jre11的windows用户：

下载Release内的 PhigrosRpc-jre11-windows-0.4.zip

解压后运行start.cmd，默认监听127.0.0.1:9090

未安装jre11的linux用户：

自行安装

#### 客户端编写

使用phigros.thrift生成您使用语言的代码。`thrift --gen py phigros.thrift`

### phigros.thrift内的定义
```thrift
enum Level {
	EZ, HD, IN, AT
}

struct Summary {
	1: required string saveUrl; //存档Url
	2: required i16 challenge;  //课题分
	3: required double rks;     //总RKS
	4: required i8 version;     //客户端版本
	5: required string avater;  //头像
}

struct SongLevel {
	1: required string id;         //曲目id
	2: required Level level;
	3: required i32 score;         //分数
	4: required double acc;        //准确率
	5: required bool fc;           //是否Full Combo
	6: required double difficulty; //定数
	7: required double rks;        //单曲RKS
}

struct SongExpect {
	1: required string id;     //曲目id
	2: required Level level;
	3: required double acc;    //现在ACC
	4: required double expect; //目标ACC
}

//sessionToken为25位字符串，saveUrl为存档URL，需要通过getSaveUrl方法获取
service Phigros {
	Summary getSaveUrl(1:string sessionToken);         //获取saveUrl和其他
	list<SongLevel> best19(1:string saveUrl);          //最佳phi和最佳前19个
	list<SongLevel> bestn(1:string saveUrl, 2:i8 num); //最佳phi和最佳前N个
	list<SongExpect> songExpects(1:string saveUrl);    //所有可推分歌曲及其目标ACC
}
```

# PhigrosLibrary

基于Phigros 2.4.7
至 Phigros 2.5.1

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

PhigrosUser.readInfo为读取定数信息，本类库不保存定数信息。
定数表为一个csv文件，Release内difficulty.csv可查看其结构。

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
SongLevel的结构是这样的。
```java
class SongLevel implements Comparable<SongLevel>{
    public String id;        //曲目Id
    public int level;        // 0:EZ / 1:HD / 2:IN / 3:AT
    public int score;        // 分数
    public float acc; 
    public boolean fc;
    public float difficulty; // 定数
    public float rks;        // 单曲rks
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
    public int level;
    public float acc;
    public float expect; //目标ACC
    @Override
    public int compareTo(SongExpect songExpect) {
        return Float.compare(expect - acc, songExpect.expect - songExpect.acc);
    }
}
```

### PhigrosLibrary的高级应用

注意：如果只想查询B19和ACC，请使用快速使用的例子，PhigrosUser内的对这两个常用情景有优化。

Phigros云存档包含5部分内容

gameRecord, gameKey, gameProgress, user, settings和summary

其中gameRecord和gameKey为数组结构，其他四个是普通的结构。

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
Summary结构(未完全解析)
```java
public class Summary {
    public final short challenge; //课题分
    public final float rks;       //rks
    public final byte version;    //客户端版本号
    public final String avater;   //头像
}
```
GameSettings结构
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
GameUser结构
```java
public class GameUser {
    GameUser(byte[] data);
    public String getIntroduction(); //自我介绍
    public String getAvater();       //头像
    public String getIllustration(); //曲绘
}
```
GameProgress结构(未完全解析)
```java
class GameProgress {
    private final ByteReader reader;
    GameProgress(byte[] data);
    public short getChallenge(); //课题分
    public void setChallenge(short score);
    public int getGameData();    //
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
        user.modify(GameRecord.class, gameRecord -> {
            for (GameRecordItem item:gameRecord) {
                for (String id:item) {
                    if (id.equals(songId))
                        item.modifySong(level, score, acc, fc);
                }
            }
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
修改存档请使用
`<T extend GameExtend> PhigrosUser.modify(Class<T> clazz, ModifyStrategy<T> strategy)`
```java
@FunctionalInterface
interface ModifyStrategy<T extends GameExtend> {
    T apply(T data) throws IOException;
}
```

### Phigros QQ群
加入 282781492 闲聊