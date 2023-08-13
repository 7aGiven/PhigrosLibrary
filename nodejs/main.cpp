#include <node_api.h>
#include "../cpp/phigros.cpp"

static napi_value MethodDifficulty(napi_env env, napi_callback_info info) {
	read_difficulty();
	napi_value value;
	return value;
}

static napi_value MethodInfo(napi_env env, napi_callback_info callback_info) {
	size_t argc = 1;
	napi_value argv;
	napi_get_cb_info(env, callback_info, &argc, &argv, 0, 0);
	char buf[26];
	size_t result_len;
	napi_get_value_string_utf8(env, argv, buf, 26, &result_len);
	Summary summary;
	info(buf, summary);
	napi_value result;
	napi_create_object(env, &result);
	napi_value value;
	napi_create_int32(env, summary.saveVersion, &value);
	napi_set_named_property(env, result, "saveVersion", value);
	napi_create_int32(env, summary.challengeModeRank, &value);
	napi_set_named_property(env, result, "challengeModeRank", value);
	napi_create_double(env, summary.rks, &value);
	napi_set_named_property(env, result, "rks", value);
	napi_create_int32(env, summary.gameVersion, &value);
	napi_set_named_property(env, result, "gameVersion", value);
	napi_create_string_utf8(env, summary.avatar.data(), summary.avatar.length(), &value);
	napi_set_named_property(env, result, "avatar", value);
	napi_value array;
	napi_create_array_with_length(env, 12, &array);
	for (char i = 0; i < 12; i++) {
		napi_create_int32(env, summary.progress[i], &value);
		napi_set_element(env, array, i, value);
	}
	napi_set_named_property(env, result, "progress", array);
	napi_create_string_utf8(env, summary.url.data(), summary.url.length(), &value);
	napi_set_named_property(env, result, "url", value);
	return result;
}

static napi_value MethodB19(napi_env env, napi_callback_info info) {
	size_t argc = 1;
	napi_value argv;
	napi_get_cb_info(env, info, &argc, &argv, 0, 0);
	char url[75];
	size_t result_len;
	napi_get_value_string_utf8(env, argv, url, 75, &result_len);
	printf("%s\n", url);
	zip_t* zip = download_save(url);
	SongLevel* songs = parseGameRecord(zip, difficulty);
	napi_value array;
	napi_create_array_with_length(env, 20, &array);
	napi_value element;
	napi_value value;
	for (char i = 0; i < 20; i++) {
		napi_create_object(env, &element);
		napi_create_string_utf8(env, songs[i].id.data(), songs[i].id.length(), &value);
		napi_set_named_property(env, element, "id", value);
		napi_create_int32(env, songs[i].level, &value);
		napi_set_named_property(env, element, "level", value);
		napi_create_double(env, songs[i].difficulty, &value);
		napi_set_named_property(env, element, "difficulty", value);
		napi_create_double(env, songs[i].rks, &value);
		napi_set_named_property(env, element, "rks", value);
		napi_create_int32(env, songs[i].score, &value);
		napi_set_named_property(env, element, "score", value);
		napi_create_double(env, songs[i].acc, &value);
		napi_set_named_property(env, element, "acc", value);
		napi_get_boolean(env, songs[i].fc, &value);
		napi_set_named_property(env, element, "fc", value);
		napi_set_element(env, array, i, element);
	}
	return array;
}

static napi_value Init(napi_env env, napi_value exports) {
	napi_property_descriptor desc = {"read_difficulty", 0, MethodDifficulty, 0, 0, 0, napi_default, 0};
	napi_define_properties(env, exports, 1, &desc);
	desc = {"info", 0, MethodInfo, 0, 0, 0, napi_default, 0};
	napi_define_properties(env, exports, 1, &desc);
	desc = {"b19", 0, MethodB19, 0, 0, 0, napi_default, 0};
	napi_define_properties(env, exports, 1, &desc);
	return exports;
}

NAPI_MODULE(NODE_GYP_MODULE_NAME, Init)
