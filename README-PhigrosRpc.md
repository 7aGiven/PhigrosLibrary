# PhigrosRpc

基于netty 4.1.93，PhigrosLibrary 3.0

### 功能

查询Best 19数据和可推分的目标ACC数据

### 快速使用

#### 服务端启动

已安装jre17的用户:

下载Release内的PhigrosRpc-nojre.zip

解压后windows运行start.cmd，linux运行start.sh，默认监听127.0.0.1:9090

未安装jre17的windows用户：

下载Release内的 PhigrosRpc-jre17-windows.zip

解压后运行start.cmd，默认监听127.0.0.1:9090

未安装jre17的linux用户：

自行安装

#### 客户端编写

[客户端示例](https://github.com/7aGiven/PhigrosLibrary/tree/master/clientExample)

使用http api

@GET

/saveUrl/{sessionToken}

返回saveUrl和summary
```json
{"saveUrl":"https://rak3ffdi.tds1.tapfiles.cn/gamesaves/0123456789abcde0123456789abcde/.save","存档版本":3,"课题分":536,"RKS":15.534,"游戏版本":78,"头像":"Glaciation","EZ":[100,100,100],"HD":[100,100,100],"IN":[100,100,100],"AT":[100,100,100]}
```
@GET

/playerId/{sessionToken}

返回playerId

`嗨嗨嗨`

@GET

/b19/{saveUrl}

返回：共20个元素，其中第一个元素为Best Phi
```json
[
  {"songId":"996.李化禹","level":"IN","acc":99.8,"fc":false,"定数":14.4,"单曲rks":13.6},
  ...
  {}
]
```
@GET

/expects/{saveUrl}

返回：所有已打过的可能达到B19最后一名单曲rks的歌曲

注：expect指目标ACC，即打到该ACC，单曲rks可达到B19最后一名
```json
[
  {"songId":"996.李化禹","level":"IN","acc":96.8,"expect":99.5},
  ...
  {}
]
```
@GET

/song/{songId}/{saveUrl}

返回单曲分数acc fc

```json
[
  {
    "s": 1000000,
    "a": 98.57,
    "c": true
  },
  ...
  {}
]
```
@GET

/8/{sessionToken}

重置第八章剧情

`OK`

@GET

/data/{saveUrl}

返回data值（KB,MB,GB,TB,PB）

```
4,5,6,7,8
```