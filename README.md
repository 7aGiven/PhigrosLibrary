# Phigros Library

PhigrosLibrary是C/C++实现的Phigros云存档解析库。
> [!IMPORTANT]
> **此项目为逆向成果，未非调用任何第三方接口。**  
> **个人项目，未触及法律，目的为了让大家可以开发自己的查分机器人。**

> [!CAUTION]
> **严禁大规模查分对鸽游服务器进行DDOS。**

## 目录
- [其他语言的实现](#其他语言实现查分)
- [使用本项目的优秀项目](#使用本项目的优秀项目)
- [PhigrosLibrary文档](./PhigrosLibrary.md)
- [资源更新方式](#更新资源) 
- ~~淘宝店~~
- [Phigros QQ群](#phigros-qq群)

## 更新资源
部分资源可直接在线下载，可能会有更新不及时的情况：
1. [头像id](https://github.com/7aGiven/PhigrosLibrary/blob/main/avatar.txt)
2. [收藏品id](https://github.com/7aGiven/PhigrosLibrary/blob/main/collection.tsv)
3. [定数表](https://github.com/7aGiven/Phigros_Resource/blob/info/difficulty.tsv)
4. [曲绘](https://github.com/7aGiven/Phigros_Resource/tree/illustration)
5. [模糊曲绘](https://github.com/7aGiven/Phigros_Resource/tree/illustrationBlur)
6. [低质量曲绘](https://github.com/7aGiven/Phigros_Resource/tree/illustrationLowRes)

### 手动更新
[Phigros_Resource](https://github.com/7aGiven/Phigros_Resource/)项目可从apk文件中提取上述文件  

运行以下代码将会提取头像id`avatar.txt`，收藏品id`collection.tsv`，定数表`difficulty.tsv` 至 `运行目录/info`：
  ```python
  pip install UnityPy==1.10.18
  python gameInformation.py Phigros.apk
  ```

运行以下代码将会提取不同清晰度的曲绘至 `运行目录/illustration` `运行目录/illustrationBlur` `运行目录/illustrationLowRes`：
  ```python
  pip install fsb5
  python resource.py Phigros.apk
  ```

将生成的文件替换项目内的同名文件即可

> [!TIP]
> 如使用pip下载安装速度过慢请先运行
>```python
>pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

>[!TIP]
>3个文件都在项目根目录下，可查看修改时间判断版本。


  
## 其他语言实现查分
均为C语言的包装器  
查看`nodejs`和`python`文件夹

## 使用本项目的优秀项目
[phi-plugin](https://github.com/catrong/phi-plugin)
云崽bot插件，可查分等非常多的有关Phigros的功能  
UI精美，使用nodejs重构本项目的查分，使用本项目nodejs C++ addon的re8

## Phigros QQ群
### Phigros玩家 QQ群
加入 [282781491](https://qm.qq.com/q/Qszphpu3WE) 闲聊
### PhigrosLibrary开发 QQ群
加入 [855374626](https://qm.qq.com/q/41K4NchxcA) 询问开发相关
