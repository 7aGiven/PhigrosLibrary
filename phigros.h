#ifndef PHIGROS_H
#define PHIGROS_H
//获取handle,申请内存,参数为sessionToken
void *get_handle(char *sessionToken);
//释放handle的内存,不会被垃圾回收,使用完handle请确保释放
void free_handle(void *handle)
//获取nickname
char *get_nickname(void *handle);
//获取summary
cJSON* get_summary(void *handle);
//获取存档
char *get_save(void *handle)
//加载定数表，计算B19与目标ACC需要
void load_difficulty(char *path);
//从存档读取B19,依赖load_difficulty
char *get_b19(void *handle);
#endif
