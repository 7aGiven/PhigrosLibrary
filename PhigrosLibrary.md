# PhigrosLibrary

对Phigros云存档的序列化和反序列化

封装并优化了常用函数(B19,BestN,目标ACC)

# 构建C/C++
所有系统都依赖cmake

windows依赖Visual Studio Build Tools安装C++载荷

依赖libssl.lib libcrypto.lib zlib.lib以及zlib的头文件

运行build.cmd生成phigros.dll

linux依赖g++与libssl-dev

运行build.sh生成libphigros.so

# C/C++使用方法
### 直接引用源码
直接把源码加进您的项目内
### 调用编译完成的so与dll
目前仅支持x64的windows与linux以及aarch64的linux的Release，如为其他系统与架构请自行编译

下载Release内的libphigros.so或phigros.dll

请看[phigros.h](https://github.com/7aGiven/PhigrosLibrary/blob/main/phigros.h)的注释
