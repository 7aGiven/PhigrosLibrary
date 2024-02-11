#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>
#include <iostream>
#include <map>
#include <string>
#include "cJSON.h"

extern "C" {
struct record {
    cJSON* json;
    char level;
    float rks;
};
std::map<std::string, std::array<float, 4>> difficulties;
void load_difficulty(char* path) {
	difficulties.clear();
    char str[102];
    float d[4];
    char tab;
    FILE* file = fopen(path, "r");
    while (fscanf(file, "%s%f%f%f%c", str, d, d+1, d+2, &tab) != -1) {
        if (tab == 9)
            fscanf(file, "%f", d+3);
        else
            d[3] = 0;
        difficulties[str] = std::array<float, 4>{d[0], d[1], d[2], d[3]};
    }
    fclose(file);
}

record* low(record* records, unsigned char len) {
    char n = 0;
    for (char i = 0; i < len; i++)
        if (records[i].rks < records[n].rks)
            n = i;
    return records + n;
}

bool compare(record a, record b) {
    return a.rks > b.rks;
}

void cpp_bestn_internal(struct record* phi, unsigned char len, cJSON* gameRecord) {
    record* records = phi + 1;
    cJSON* song;
    record* low_record = records;
    cJSON_ArrayForEach(song, gameRecord) {
        std::array<float, 4> difficulty = difficulties.at(song->string);
        for (char level = 0; level < 4; level++) {
            if (level == 3 && difficulty.at(3) == 0)
                break;
            char compare_phi = difficulty.at(level) > phi->rks && cJSON_GetArrayItem(song, 3 * level)->valueint == 1000000;
            if (compare_phi || difficulty.at(level) > low_record->rks) {
                float rks = (cJSON_GetArrayItem(song, 3 * level + 1)->valuedouble - 55) / 45;
                rks *= rks * difficulty.at(level);
                if (compare_phi) {
                    phi->json = song;
                    phi->level = level;
                    phi->rks = rks;
                }
                if (rks > low_record->rks) {
                    low_record->json = song;
                    low_record->level = level;
                    low_record->rks = rks;
                    low_record = low(records, len);
                }
            }
        }
    }
    std::sort(records, records + len, compare);
}

cJSON* get_b19(cJSON* gameRecord) {
    record records[20] = {};
    cpp_bestn_internal(records, 19, gameRecord);
    float rks = 0;
    cJSON* phi;
    cJSON* array = cJSON_CreateArray();
    for (char i = 0; i < 20; i++) {
        cJSON* song = cJSON_CreateObject();
        cJSON_AddStringToObject(song, "id", records[i].json->string);
        cJSON_AddNumberToObject(song, "level", records[i].level);
        cJSON_AddNumberToObject(song, "difficulty", difficulties.at(records[i].json->string).at(records[i].level));
        cJSON_AddNumberToObject(song, "rks", records[i].rks);
        cJSON_AddNumberToObject(song, "score", cJSON_GetArrayItem(records[i].json, 3 * records[i].level)->valueint);
        cJSON_AddNumberToObject(song, "acc", cJSON_GetArrayItem(records[i].json, 3 * records[i].level + 1)->valuedouble);
        cJSON_AddNumberToObject(song, "fc", cJSON_GetArrayItem(records[i].json, 3 * records[i].level + 2)->valueint);
        if (i == 0)
            phi = song;
        else
            cJSON_AddItemToArray(array, song);
        rks += records[i].rks;
    }
    cJSON* json = cJSON_CreateObject();
    cJSON_AddNumberToObject(json, "rks", rks / 20);
    cJSON_AddItemToObject(json, "phi", phi);
    cJSON_AddItemToObject(json, "best", array);
    return json;

}

cJSON* cpp_expect(cJSON* gameRecord) {
    record records[20] = {};
    cpp_bestn_internal(records, 19, gameRecord);
    cJSON* array = cJSON_CreateArray();
    float low = records[20].rks;
    cJSON* song;
    cJSON_ArrayForEach(song, gameRecord) {
        std::array<float, 4> difficulty = difficulties.at(song->string);
        for (char level = 0; level < 4; level++) {
            if (difficulty.at(level) < low)
                continue;
            float acc = cJSON_GetArrayItem(song, 3 * level + 1)->valuedouble;
            float rks = (acc - 55) / 45;
            rks *= rks * difficulty.at(level);
            if (rks > low)
                continue;
            float expect = sqrt(low / difficulty.at(level)) * 45 + 55;
            cJSON* obj = cJSON_CreateObject();
            cJSON_AddStringToObject(obj, "id", song->string);
            cJSON_AddNumberToObject(obj, "level", level);
            cJSON_AddNumberToObject(obj, "difficulty", difficulty.at(level));
            cJSON_AddNumberToObject(obj, "rks", rks);
            cJSON_AddNumberToObject(obj, "acc", acc);
            cJSON_AddNumberToObject(obj, "expect", expect);
            cJSON_AddItemToArray(array, obj);
        }
    }
    return array;
}
}