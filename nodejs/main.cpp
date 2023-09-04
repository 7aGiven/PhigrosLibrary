#include <node_api.h>
#include "../cpp/phigros.cpp"

static void js_read_nodes(napi_env& env, void* vbuf, Node nodes[], char size, napi_value& result) {
	napi_create_object(env, &result);
	napi_value array;
	napi_value value;
	char* buf = (char*) vbuf;
	bool b = false;
	char index = 0;
	for (char i = 0; i < size; i++) {
		if (nodes[i].type == "bool") {
			napi_get_boolean(env, getbit(*buf, index), &value);
			napi_set_named_property(env, result, nodes[i].name.data(), value);
			b = true;
			index++;
			continue;
		}
		if (b) {
			b = false;
			buf++;
			index = 0;
		}
		if (nodes[i].type == "char") {
			napi_create_int32(env, *buf, &value);
			napi_set_named_property(env, result, nodes[i].name.data(), value);
			buf++;
		} else if (nodes[i].type == "short") {
			napi_create_int32(env, *(short*) buf, &value);
			napi_set_named_property(env, result, nodes[i].name.data(), value);
			buf += 2;
		} else if (nodes[i].type == "float") {
			napi_create_double(env, *(float*) buf, &value);
			napi_set_named_property(env, result, nodes[i].name.data(), value);
			buf += 4;
		} else if (nodes[i].type == "string") {
			unsigned char len = read_cstring(buf);
			napi_create_string_utf8(env, buf - len, len, &value);
			napi_set_named_property(env, result, nodes[i].name.data(), value);
		} else if  (nodes[i].type == "short5") {
			napi_create_array_with_length(env, 5, &array);
			for (char i = 0; i < 5; i++) {
				napi_create_int32(env, read_varshort(buf), &value);
				napi_set_element(env, array, i, value);
			}
			napi_set_named_property(env, result, nodes[i].name.data(), array);
		}
	}
}

static short js_write_nodes(napi_env& env, napi_value& obj, Node nodes[], char size, char* buf) {
	char* start = buf;
	napi_value array;
	napi_value value;
	bool b = false;
	char index = 0;
	for (char i = 0; i < size; i++) {
		if (nodes[i].type == "bool") {
			if (index == 0)
				*buf = 0;
			bool bb;
			napi_get_named_property(env, obj, nodes[i].name.data(), &value);
			napi_get_value_bool(env, value, &bb);
			setbit(buf, index, bb);
			b = true;
			index++;
			continue;
		}
		if (b) {
			b = false;
			buf++;
			index = 0;
		}
		int32_t in;
		double d;
		size_t s;
		if (nodes[i].type == "char") {
			napi_get_named_property(env, obj, nodes[i].name.data(), &value);
			napi_get_value_int32(env, value, &in);
			*buf = in;
			buf++;
		} else if (nodes[i].type == "short") {
			napi_get_named_property(env, obj, nodes[i].name.data(), &value);
			napi_get_value_int32(env, value, &in);
			*(short*) buf = in;
			buf += 2;
		} else if (nodes[i].type == "string") {
			napi_get_named_property(env, obj, nodes[i].name.data(), &value);
			buf++;
			napi_get_value_string_utf8(env, value, buf, 163, &s);
			*(buf - 1) = s;
			if (s > 128) {
				memmove(buf + 1, buf, s);
				*buf = 1;
				buf++;
			}
			buf += s;
		} else if  (nodes[i].type == "short5") {
			napi_get_named_property(env, obj, nodes[i].name.data(), &array);
			for (char i = 0; i < 5; i++) {
				napi_get_element(env, array, i, &value);
				napi_get_value_int32(env, value, &in);
				write_varshort(buf, in);
				napi_set_element(env, array, i, value);
			}
		}
	}
	return buf - start;
}

static void js_read_record(napi_env env, void* vbuf, napi_value& result) {
	napi_value song;
	napi_value array;
	napi_value songlevel;
	napi_value value;
	char* buf = (char*) vbuf;
	unsigned char song_len = read_varshort(buf);
	napi_create_array_with_length(env, song_len, &result);
	for (unsigned char index = 0; index < song_len; index++) {
		char* end = buf + *buf + 1;
		end = end + *end + 1;
		char len = read_cstring(buf);
		napi_create_string_utf8(env, buf - len, len - 2, &value);
		buf++;
		len = *buf;
		buf++;
		char fc = *buf;
		buf++;
		napi_create_object(env, &song);
		napi_set_named_property(env, song, "id", value);
		napi_create_array_with_length(env, 4, &array);
		for (char level = 0; level < 4; level++) {
			napi_create_object(env, &songlevel);
			if (getbit(len, level)) {
				napi_create_int32(env, *(int*) buf, &value);
				napi_set_named_property(env, songlevel, "score", value);
				napi_create_double(env, *(float*) buf, &value);
				napi_set_named_property(env, songlevel, "acc", value);
				napi_get_boolean(env, getbit(fc, level), &value);
				napi_set_named_property(env, songlevel, "fc", value);
				napi_set_element(env, array, level, songlevel);
			}
		}
		napi_set_named_property(env, song, "levels", array);
		napi_set_element(env, result, index, song);
		buf = end;
	}
}

static void js_read_key(napi_env& env, void* vbuf, napi_value& wrap) {
	napi_value result;
	napi_value obj;
	napi_value value;
	char* buf = (char*) vbuf;
	short key_len = read_varshort(buf);
	napi_create_array_with_length(env, key_len, &result);
	for (short index = 0; index < key_len; index++) {
		char len = read_cstring(buf);
		char* end = buf + *buf + 1;
		napi_create_string_utf8(env, buf - len, len, &value);
		buf++;
		len = *buf++;
		napi_create_object(env, &obj);
		napi_set_named_property(env, obj, "id", value);
		for (char i = 0; i < sizeof(nodeKey) / sizeof(Node); i++) {
			if (!getbit(len, i)) continue;
			if (nodeKey[i].type == "char") {
				napi_create_int32(env, *buf, &value);
				napi_set_named_property(env, obj, nodeKey[i].name.data(), value);
			} else if (nodeKey[i].type == "bool") {
				napi_get_boolean(env, *buf, &value);
				napi_set_named_property(env, obj, nodeKey[i].name.data(), value);
			}
			buf++;
		}	
		napi_set_element(env, result, index, obj);
	}
	napi_create_object(env, &wrap);
	napi_set_named_property(env, wrap, "keys", result);
	napi_create_int32(env, *buf++, &value);
	napi_set_named_property(env, wrap, "lanotaReadKeys", value);
	napi_create_int32(env, *buf++, &value);
	napi_set_named_property(env, wrap, "camelliaReadKey", value);
}

static void js_write_map(napi_env env, napi_value map, char*& buf, void(*callback)(napi_env, napi_value, char*&)) {
	napi_value obj;
	napi_value value;
	int32_t int32;
	size_t str_len;
	napi_get_named_property(env, map, "length", &value);
	napi_get_value_int32(env, value, &int32);
	short item_len = int32;
	write_varshort(buf, item_len);
	for (short index = 0; index < item_len; index++) {
		napi_get_element(env, map, index, &obj);
		napi_get_named_property(env, obj, "id", &value);
		buf++;
		napi_get_value_string_utf8(env, value, buf, 100, &str_len);
		buf[-1] = str_len;
		buf += str_len;
		char* len_position = buf;
		buf++;
		callback(env, obj, buf);
		*len_position = buf - len_position - 1;
	}
}

static short js_write_key(napi_env env, napi_value wrap, char* buf) {
	char* start = buf;
	napi_value map;
	napi_get_named_property(env, wrap, "keys", &map);
	js_write_map(env, map, buf, [](napi_env env, napi_value obj, char*& buf) {
		napi_value value;
		int32_t int32;
		bool bvalue;
		char* len = buf++;
		*len = 0;
		for (char i = 0; i < sizeof nodeKey / sizeof(Node); i++) {
			napi_has_named_property(env, obj, nodeKey[i].name.data(), &bvalue);
			if (!bvalue) continue;
			napi_get_named_property(env, obj, nodeKey[i].name.data(), &value);
			if (nodeKey[i].type == "char") {
				napi_get_value_int32(env, value, &int32);
				*buf = int32;
			} else if (nodeKey[i].type == "bool") {
				napi_get_value_bool(env, value, &bvalue);
				*buf = bvalue;
			}
			setbit(len, i, true);
			buf++;
		}	
	});
	int32_t int32;
	bool bvalue;
	napi_get_named_property(env, wrap, "lanotaReadKeys", &map);
	napi_get_value_int32(env, map, &int32);
	*buf = int32;
	buf++;
	napi_get_named_property(env, wrap, "camelliaReadKey", &map);
	napi_get_value_bool(env, map, &bvalue);
	*buf = bvalue;
	buf++;
	return buf - start;
}

static short js_write_record(napi_env env, napi_value map, char* buf) {
	js_write_map(env, map, buf, [](napi_env env, napi_value obj, char*& buf) {
		napi_value undefined;
		napi_get_undefined(env, &undefined);
		napi_value array;
		napi_value level;
		napi_value value;
		double fvalue;
		bool bvalue;
		char* len = buf++;
		char* fc = buf++;
		*len = 0;
		*fc = 0;
		napi_get_named_property(env, obj, "levels", &array);
		for (char i = 0; i < 4; i++) {
			napi_get_element(env, array, i, &level);
			napi_strict_equals(env, level, undefined, &bvalue);
			if (bvalue) continue;
			napi_get_named_property(env, level, "score", &value);
			napi_get_value_int32(env, value, (int*) buf);
			buf += 4;
			napi_get_named_property(env, level, "acc", &value);
			napi_get_value_double(env, value, &fvalue);
			*(float*) buf = fvalue;
			buf += 4;
			napi_get_named_property(env, level, "fc", &value);
			napi_get_value_bool(env, value, &bvalue);
			setbit(fc, i, bvalue);
			setbit(len, i, true);
		}
	});
}

static napi_value MethodDifficulty(napi_env env, napi_callback_info info) {
	read_difficulty();
	napi_value value;
	return value;
}

static napi_value MethodPlayer(napi_env env, napi_callback_info info) {
	size_t len = 1;
	napi_value value;
	napi_get_cb_info(env, info, &len, &value, 0, 0);
	char buf[26];
	napi_get_value_string_utf8(env, value, buf, 26, &len);
	std::string id = get_player(buf);
	napi_create_string_utf8(env, id.data(), id.length(), &value);
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
	napi_create_string_utf8(env, summary.update.data(), summary.update.length(), &value);
	napi_set_named_property(env, result, "update", value);
	return result;
}

static napi_value MethodSave(napi_env env, napi_callback_info info) {
	size_t size = 1;
	napi_value value;
	napi_get_cb_info(env, info, &size, &value, 0, 0);
	unsigned char buf[12 * 1024];
	char* url = (char*) buf;
	napi_get_value_string_utf8(env, value, url, 75, &size);
	printf("%s\n", url);
	napi_value result;
	napi_create_object(env, &result);
	int outlen;
EVP_CIPHER_CTX* cipher_ctx = EVP_CIPHER_CTX_new();

	char res[save_size];
	zip_t* zip = download_save(url, res);
	zip_file_t* zip_file;
	short len;

	zip_file = zip_fopen(zip, "gameProgress", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	js_read_nodes(env, buf + 1, nodeGameProgress, sizeof nodeGameProgress / sizeof(Node), value);
	napi_set_named_property(env, result, "gameProgress", value);
	
	zip_file = zip_fopen(zip, "user", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	js_read_nodes(env, buf + 1, nodeUser, sizeof nodeUser / sizeof(Node), value);
	napi_set_named_property(env, result, "user", value);

	zip_file = zip_fopen(zip, "settings", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	js_read_nodes(env, buf + 1, nodeSettings, sizeof nodeSettings / sizeof(Node), value);
	napi_set_named_property(env, result, "settings", value);

	zip_file = zip_fopen(zip, "gameRecord", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	js_read_record(env, buf + 1, value);
	napi_set_named_property(env, result, "gameRecord", value);

	zip_file = zip_fopen(zip, "gameKey", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	js_read_key(env, buf + 1, value);
	napi_set_named_property(env, result, "gameKey", value);

	zip_discard(zip);

	EVP_CIPHER_CTX_free(cipher_ctx);

	return result;
}

static napi_value Methodre8(napi_env env, napi_callback_info info) {
	size_t argc = 1;
	napi_value value;
	napi_get_cb_info(env, info, &argc, &value, 0, 0);
	char buf[26];
	size_t result_len;
	napi_get_value_string_utf8(env, value, buf, 26, &result_len);
	re8(buf);
	return value;
}

static napi_value MethodGameProgress(napi_env env, napi_callback_info info) {
	size_t argc = 2;
	napi_value value[2];
	napi_get_cb_info(env, info, &argc, value, 0, 0);
	char sessionToken[26];
	size_t result_len;
	napi_get_value_string_utf8(env, value[0], sessionToken, 26, &result_len);
	modify_save(sessionToken, "gameProgress", [&env, value](char* buf, short len) -> short {
		napi_value global;
		napi_get_global(env, &global);
		napi_value gameProgress;
		js_read_nodes(env, buf, nodeGameProgress, sizeof nodeGameProgress / sizeof(Node), gameProgress);
		napi_call_function(env, global, value[1], 1, &gameProgress, 0);
		return js_write_nodes(env, gameProgress, nodeGameProgress, sizeof nodeGameProgress / sizeof(Node), buf);
	});
	return value[0];
}

static napi_value MethodGameKey(napi_env env, napi_callback_info info) {
	size_t argc = 2;
	napi_value value[2];
	napi_get_cb_info(env, info, &argc, value, 0, 0);
	char sessionToken[26];
	size_t result_len;
	napi_get_value_string_utf8(env, value[0], sessionToken, 26, &result_len);
	modify_save(sessionToken, "gameKey", [&env, value](char* buf, short len) -> short {
		napi_value global;
		napi_get_global(env, &global);
		napi_value gameProgress;
		js_read_key(env, buf, gameProgress);
		napi_call_function(env, global, value[1], 1, &gameProgress, 0);
		return js_write_key(env, gameProgress, buf);
	});
	return value[0];
}

static napi_value MethodGameRecord(napi_env env, napi_callback_info info) {
	size_t argc = 2;
	napi_value value[2];
	napi_get_cb_info(env, info, &argc, value, 0, 0);
	char sessionToken[26];
	size_t result_len;
	napi_get_value_string_utf8(env, value[0], sessionToken, 26, &result_len);
	modify_save(sessionToken, "gameRecord", [&env, value](char* buf, short len) -> short {
		napi_value global;
		napi_get_global(env, &global);
		napi_value gameProgress;
		js_read_record(env, buf, gameProgress);
		napi_call_function(env, global, value[1], 1, &gameProgress, 0);
		return js_write_record(env, gameProgress, buf);
	});
	return value[0];
}

static napi_value MethodB19(napi_env env, napi_callback_info info) {
	size_t argc = 1;
	napi_value argv;
	napi_get_cb_info(env, info, &argc, &argv, 0, 0);
	char url[75];
	size_t result_len;
	napi_get_value_string_utf8(env, argv, url, 75, &result_len);
	printf("%s\n", url);
	char res[save_size];
	zip_t* zip = download_save(url, res);
	SongLevel songs[20];
	parseGameRecord(zip, songs);
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
	init();
	napi_property_descriptor properties[] = {
		{"read_difficulty", 0, MethodDifficulty, 0, 0, 0, napi_default, 0},
		{"get_player", 0, MethodPlayer, 0, 0, 0, napi_default, 0},
		{"info", 0, MethodInfo, 0, 0, 0, napi_default, 0},
		{"get_save", 0, MethodSave, 0, 0, 0, napi_default, 0},
		{"modify_gameProgress", 0, MethodGameProgress, 0, 0, 0, napi_default, 0},
		{"modify_gameKey", 0, MethodGameKey, 0, 0, 0, napi_default, 0},
		{"modify_gameRecord", 0, MethodGameRecord, 0, 0, 0, napi_default, 0},
		{"re8", 0, Methodre8, 0, 0, 0, napi_default, 0},
		{"b19", 0, MethodB19, 0, 0, 0, napi_default, 0},
	};
	napi_define_properties(env, exports, sizeof properties / sizeof(napi_property_descriptor), properties);
	return exports;
}

NAPI_MODULE(NODE_GYP_MODULE_NAME, Init)
