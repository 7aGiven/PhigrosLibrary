#ifndef PHIGROS_H
#define PHIGROS_H
#include "src/cJSON.h"
typedef struct bio_st BIO;
char get_nickname(char* sessionToken, char** nickname);
void load_difficulty(char* path);
cJSON* parse_save(BIO* bio_zip);
short gen_save(char** ret, cJSON* json);
cJSON* get_summarys(char* sessionToken);
void update_summary(cJSON* summary, cJSON* save);
BIO* download_save(char* url);
void upload_save(char* sessionToken, char* data, short data_len, cJSON* summary);
cJSON* get_b19(cJSON* gameRecord);
#endif