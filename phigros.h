#ifndef PHIGROS_H
#define PHIGROS_H
#include "src/cJSON.h"
//BIO为一种可变长内存
typedef struct bio_st BIO;
//释放BIO内存
int BIO_free(BIO *a);
//获取nickname，需要free(*nickname)
char get_nickname(char* sessionToken, char** nickname);
//下载存档bio_zip到BIO内存
BIO* download_save(char* url);
//反序列化存档
cJSON* parse_save(BIO* bio_zip);
//序列化存档至内存*ret，使用完成请free(*ret)
short gen_save(char** ret, cJSON* json);
//获取summary
cJSON* get_summary(char* sessionToken);
//获取summarys
cJSON* get_summarys(char* sessionToken);
//使用存档更新summary
void update_summary(cJSON* summary, cJSON* save);
//加载定数表，计算B19与目标ACC需要
void load_difficulty(char* path);
//上传存档
void upload_save(char* sessionToken, char* data, short data_len, cJSON* summary);
//使用gameRecord生成B19
cJSON* get_b19(cJSON* gameRecord);
#endif

