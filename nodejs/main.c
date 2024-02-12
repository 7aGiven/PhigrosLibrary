#include <node_api.h>
#include <stdlib.h>
#include <string.h>
#include "../phigros.h"

static napi_value MethodNickname(napi_env env, napi_callback_info info) {
	size_t len = 1;
	napi_value value;
	napi_get_cb_info(env, info, &len, &value, 0, 0);
	char buf[26];
	napi_get_value_string_utf8(env, value, buf, 26, &len);
	char* nickname;
	len = get_nickname(buf, &nickname);
	napi_create_string_utf8(env, nickname, len, &value);
	free(nickname);
	return value;
}

static napi_value MethodDifficulty(napi_env env, napi_callback_info info) {
	size_t len = 1;
	napi_value value;
	napi_get_cb_info(env, info, &len, &value, 0, 0);
	napi_get_value_string_utf8(env, value, 0, 0, &len);
	char* path = (char*) malloc(len + 1);
	napi_get_value_string_utf8(env, value, path, len + 1, &len);
	load_difficulty(path);
	free(path);
	return value;
}

static napi_value MethodSave(napi_env env, napi_callback_info info) {
	size_t len = 1;
	napi_value value;
	napi_get_cb_info(env, info, &len, &value, 0, 0);
	char sessionToken[26];
	napi_get_value_string_utf8(env, value, sessionToken, sizeof sessionToken, &len);
	cJSON* summarys = get_summarys(sessionToken);
	cJSON* summary = cJSON_GetArrayItem(summarys, 0);
	char* url = cJSON_GetObjectItemCaseSensitive(summary, "url")->valuestring;
	BIO* save_bio = download_save(url);
	cJSON_Delete(summarys);
	cJSON* save = parse_save(save_bio);
	BIO_free(save_bio);
	char* str = cJSON_PrintUnformatted(save);
	cJSON_Delete(save);
	napi_create_string_utf8(env, str, strlen(str), &value);
	free(str);
	return value;
}

static napi_value MethodB19(napi_env env, napi_callback_info info) {
	size_t len = 1;
	napi_value value;
	napi_get_cb_info(env, info, &len, &value, 0, 0);
	napi_get_value_string_utf8(env, value, 0, 0, &len);
	char* str = malloc(len + 1);
	napi_get_value_string_utf8(env, value, str, len + 1, &len);
	printf("%s\n", str);
	cJSON* gameRecord = cJSON_ParseWithLength(str, len);
	printf("parse\n");
	free(str);
	printf("b19\n");
	cJSON* b19 = get_b19(gameRecord);
	printf("b19 ok\n");
	cJSON_Delete(gameRecord);
	str = cJSON_PrintUnformatted(b19);
	cJSON_Delete(b19);
	napi_create_string_utf8(env, str, strlen(str), &value);
	free(str);
	return value;
}

static napi_value MethodRe8(napi_env env, napi_callback_info info) {
	size_t len = 1;
	napi_value value;
	napi_get_cb_info(env, info, &len, &value, 0, 0);
	char sessionToken[26];
	napi_get_value_string_utf8(env, value, sessionToken, 26, &len);
	cJSON* summarys = get_summarys(sessionToken);
	cJSON* summary = cJSON_GetArrayItem(summarys, 0);
	char* url = cJSON_GetObjectItemCaseSensitive(summary, "url")->valuestring;
	BIO* save_bio = download_save(url);
	cJSON* save = parse_save(save_bio);
	BIO_free(save_bio);
	cJSON* gameProgress = cJSON_GetObjectItemCaseSensitive(save, "gameProgress");
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8UnlockBegin"), 0);
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8UnlockSecondPhase"), 0);
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8Passed"), 0);
	cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8SongUnlocked")->valueint = 0;
	char* save_buf;
	len = gen_save(&save_buf, save);
	cJSON_Delete(save);
	upload_save(sessionToken, save_buf, len, summary);
	free(save_buf);
	cJSON_Delete(summarys);
	return value;
}

napi_value Init(napi_env env, napi_value exports) {
	napi_property_descriptor properties[] = {
		{"get_nickname", 0, MethodNickname, 0, 0, 0, napi_default, 0},
		{"get_save", 0, MethodSave, 0, 0, 0, napi_default, 0},
        {"load_difficulty", 0, MethodDifficulty, 0, 0, 0, napi_default, 0},
        {"b19", 0, MethodB19, 0, 0, 0, napi_default, 0},
		{"re8", 0, MethodRe8, 0, 0, 0, napi_default, 0},
	};
	napi_define_properties(env, exports, sizeof properties / sizeof(napi_property_descriptor), properties);
	return exports;
}

NAPI_MODULE(NODE_GYP_MODULE_NAME, Init)
