#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>
#include <iostream>
#include <map>
#include <openssl/ssl.h>
#include "cJSON.h"

extern "C" {
// #define DEBUG
#ifdef DEBUG
	#define LOG(...) printf(__VA_ARGS__)
	#define LOGSTR(str, len) str[len]=0;printf("%s\n", str)
#else
	#define LOG(format, ...) ;
	#define LOGSTR(str, len) ;
#endif
#ifdef _MSC_VER
	#define EXPORT __declspec(dllexport)
#elif __GNUC__
	#define EXPORT __attribute__((visibility("default")))
#endif

char* b64encode(void* mem, char* data, char len);
cJSON* parse_summary(char* ptr, char* base64);
void gen_summary(char* mem, cJSON* summary);
cJSON* parse_save(BIO* bio_zip);
short gen_save(char** ret, cJSON* json);

char errorbuffer[128];
const EVP_MD* md;

BIO* read_body(BIO* bio) {
	char buf[1024], *ptr;
	BIO* mem = BIO_new(BIO_s_mem());
	short len = BIO_read(bio, buf, sizeof buf - 1);
	buf[len] = 0;
	short code = atoi(buf + 9);
	if (code == 404) {
		BIO_free(mem);
		BIO_free_all(bio);
		throw code;
	}
	ptr = strstr(buf, "\r\n\r\n");
	BIO_write(mem, ptr + 4, buf - ptr + len - 4);
	while (1) {
		len = BIO_read(bio, buf, sizeof buf);
		if (!len) break;
		BIO_write(mem, buf, len);
	}
	BIO_free_all(bio);
	return mem;
}

cJSON* read_json_body(BIO* bio, char* mem, short len) {
	char *ptr;
	cJSON *json;
	if (mem) {
		len = BIO_read(bio, mem, len);
		BIO_free_all(bio);
		LOGSTR(head, len);
		ptr = strstr(mem, "\r\n\r\n") + 4;
		json = cJSON_ParseWithLength(ptr, mem - ptr + len);
	} else {
		BIO* resp = read_body(bio);
		len = BIO_get_mem_data(resp, &ptr);
		json = cJSON_ParseWithLength(ptr, len);
		BIO_free(resp);
	}
	return json;
}

void md5(char* str, void* ptr, unsigned int len) {
	if (md == 0)
		md = EVP_md5();
	char hexdig[] = "0123456789abcdef";
	EVP_MD_CTX* ctx = EVP_MD_CTX_new();
	EVP_DigestInit(ctx, md);
	EVP_DigestUpdate(ctx, ptr, len);
	EVP_DigestFinal_ex(ctx, (unsigned char*)str + 17, &len);
	EVP_MD_CTX_free(ctx);
	for (char i = 0; i < 16; i++) {
		unsigned char b = str[17 + i];
		str[2 * i] = hexdig[b >> 4];
		str[2 * i + 1] = hexdig[b & 0xF];
	}
	str[32] = 0;
}

char info_req[] = "GET /1.1/%s HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\nConnection: close\r\n\r\n";
cJSON* internal_get_summary(char* sessionToken) {
	SSL* ssl;
	char req[512];
	sprintf(req, info_req, "classes/_GameSave?limit=1", sessionToken);
	SSL_CTX* ctx = SSL_CTX_new(TLS_client_method());
	BIO* https = BIO_new_ssl_connect(ctx);
	BIO_get_ssl(https, &ssl);
	BIO_set_conn_hostname(https, "rak3ffdi.cloud.tds1.tapapis.cn:https");
	SSL_set_tlsext_host_name(ssl, "rak3ffdi.cloud.tds1.tapapis.cn");
	BIO_puts(https, req);
	BIO_do_connect(https);
	cJSON* resp = read_json_body(https, 0, 0);
	SSL_CTX_free(ctx);

	cJSON* result = cJSON_GetObjectItemCaseSensitive(resp, "results");
	result = cJSON_GetArrayItem(result, 0);
	if (!result) {
		cJSON_Delete(resp);
		throw "ERROR:no result";
	}
	
	cJSON* info = parse_summary(req, cJSON_GetObjectItemCaseSensitive(result, "summary")->valuestring);

	cJSON* tmp;
	tmp = cJSON_DetachItemFromObjectCaseSensitive(result, "objectId");
	cJSON_AddItemToObject(info, "objectId", tmp);
	tmp = cJSON_GetObjectItemCaseSensitive(result, "user");
	tmp = cJSON_DetachItemFromObjectCaseSensitive(tmp, "objectId");
	cJSON_AddItemToObject(info, "userId", tmp);
	tmp = cJSON_GetObjectItemCaseSensitive(result, "gameFile");
	cJSON_AddItemToObject(info, "fileId", cJSON_DetachItemFromObjectCaseSensitive(tmp, "objectId"));
	tmp = cJSON_GetObjectItemCaseSensitive(tmp, "url");
	cJSON_AddStringToObject(info, "url", tmp->valuestring + 8);
	tmp = cJSON_DetachItemFromObjectCaseSensitive(result, "updatedAt");
	cJSON_AddItemToObject(info, "updatedAt", tmp);

	cJSON_Delete(resp);
	return info;
}

cJSON* get_summarys(char* sessionToken) {
	SSL* ssl;
	char req[512];
	sprintf(req, info_req, "classes/_GameSave", sessionToken);
	SSL_CTX* ctx = SSL_CTX_new(TLS_client_method());
	BIO* https = BIO_new_ssl_connect(ctx);
	BIO_get_ssl(https, &ssl);
	BIO_set_conn_hostname(https, "rak3ffdi.cloud.tds1.tapapis.cn:https");
	SSL_set_tlsext_host_name(ssl, "rak3ffdi.cloud.tds1.tapapis.cn");
	BIO_puts(https, req);
	BIO_do_connect(https);
	cJSON* resp = read_json_body(https, 0, 0);
	SSL_CTX_free(ctx);

	cJSON* results = cJSON_GetObjectItemCaseSensitive(resp, "results");
	cJSON* array = cJSON_CreateArray();
	cJSON* element;
	cJSON_ArrayForEach(element, results) {
		cJSON* info = parse_summary(req, cJSON_GetObjectItemCaseSensitive(element, "summary")->valuestring);

		cJSON* tmp;
		tmp = cJSON_DetachItemFromObjectCaseSensitive(element, "objectId");
		cJSON_AddItemToObject(info, "objectId", tmp);
		tmp = cJSON_GetObjectItemCaseSensitive(element, "user");
		tmp = cJSON_DetachItemFromObjectCaseSensitive(tmp, "objectId");
		cJSON_AddItemToObject(info, "userId", tmp);
		tmp = cJSON_GetObjectItemCaseSensitive(element, "gameFile");
		cJSON_AddItemToObject(info, "fileId", cJSON_DetachItemFromObjectCaseSensitive(tmp, "objectId"));
		tmp = cJSON_GetObjectItemCaseSensitive(tmp, "url");
		cJSON_AddStringToObject(info, "url", tmp->valuestring + 8);
		tmp = cJSON_DetachItemFromObjectCaseSensitive(element, "updatedAt");
		cJSON_AddItemToObject(info, "updatedAt", tmp);

		cJSON_AddItemToArray(array, info);
	}
	cJSON_Delete(resp);
	return array;
}

char save_req[] = "GET /%s HTTP/1.0\r\nHost: %s\r\n\r\n";
BIO* download_save(char* url) {
	char req[128];
	char* ptr = strchr(url, '/');
	*ptr = 0;
	strcpy(req, url);
	strcat(req, ":http");
	BIO* http = BIO_new_connect(req);
	sprintf(req, save_req, ptr + 1, url);
	*ptr = '/';
	BIO_puts(http, req);
	BIO_do_connect(http);
	return read_body(http);
}

char prefix[] = "%s /1.1/%s";
char header[] = " HTTP/1.%c\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nUser-Agent: LeanCloud-CSharp-SDK/1.0.3\r\nAccept: application/json\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nContent-Length: %hd\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\nConnection: %s\r\n\r\n";
char qiniu[] = "%s /buckets/rAK3Ffdi/objects/%s/uploads";
char qiniu_header[] = " HTTP/1.1\r\nAuthorization: UpToken %s\r\nContent-Length: %hd\r\nHost: upload.qiniup.com\r\nConnection: %s\r\n";
void upload_save(char* sessionToken, char* data, short data_len, cJSON* summary) {
	SSL* ssl;
	char head[2048];
	char* body = head + 1024;
	short len, body_len;
	SSL_CTX* ctx = SSL_CTX_new(TLS_client_method());

	md5(head, data, data_len);
	body_len = sprintf(body, "{\"name\":\".save\",\"__type\":\"File\",\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"prefix\":\"gamesaves\",\"metaData\":{\"size\":%hd,\"_checksum\":\"%s\",\"prefix\":\"gamesaves\"}}", cJSON_GetObjectItemCaseSensitive(summary, "userId")->valuestring, data_len, head);
	len = sprintf(head, prefix, "POST", "fileTokens");
	len += sprintf(head + len, header, '0', sessionToken, body_len, "close");
	LOG("%s", head);
	LOG("%s\n", body);

	BIO* https = BIO_new_ssl_connect(ctx);
	BIO_get_ssl(https, &ssl);
	BIO_set_conn_hostname(https, "rak3ffdi.cloud.tds1.tapapis.cn:https");
	SSL_set_tlsext_host_name(ssl, "rak3ffdi.cloud.tds1.tapapis.cn");
	BIO_write(https, head, len);
	BIO_write(https, body, body_len);
	BIO_do_connect(https);
	cJSON* resp = read_json_body(https, head, sizeof head);
	cJSON* createdAt = cJSON_DetachItemFromObjectCaseSensitive(resp, "createdAt");
	char* key = cJSON_GetObjectItemCaseSensitive(resp, "key")->valuestring;
	key = b64encode(0, key, -1);
	cJSON* fileId = cJSON_DetachItemFromObjectCaseSensitive(resp, "objectId");
	cJSON* token = cJSON_DetachItemFromObjectCaseSensitive(resp, "token");
	cJSON_Delete(resp);

	len = sprintf(head, qiniu, "POST", key);
	len += sprintf(head + len, qiniu_header, token->valuestring, 0, "keep-alive");
	LOG("%s\n", head);

	BIO* http = BIO_new_connect("upload.qiniup.com:http");
	BIO_write(http, head, len);
	BIO_write(http, "\r\n", 2);
	BIO_do_connect(http);
	resp = read_json_body(https, head, sizeof head);
	cJSON* uploadId = cJSON_DetachItemFromObjectCaseSensitive(resp, "uploadId");
	cJSON_Delete(resp);

	len = sprintf(head, qiniu, "PUT", key);
	len += sprintf(head + len, "/%s/1", uploadId->valuestring);
	len += sprintf(head + len, qiniu_header, token->valuestring, data_len, "keep-alive");
	LOG("%s", head);
	BIO_write(http, head, len);
	BIO_write(http, "\r\n", 2);
	BIO_write(http, data, data_len);
	resp = read_json_body(https, head, sizeof head);
	cJSON* etag = cJSON_GetObjectItemCaseSensitive(resp, "etag");

	body_len = sprintf(body, "{\"parts\":[{\"partNumber\":1,\"etag\":\"%s\"}]}", etag->valuestring);
	cJSON_Delete(resp);
	len = sprintf(head, qiniu, "POST", key);
	len += sprintf(head + len, "/%s", uploadId->valuestring);
	len += sprintf(head + len, qiniu_header, token->valuestring, body_len, "close");
	LOG("%s", head);
	LOG("%s\n", body);
	BIO_write(http, head, len);
	BIO_write(http, "\r\n", 2);
	BIO_write(http, body, body_len);
	len = BIO_read(http, head, sizeof head);
	LOGSTR(head, len);

	BIO_free(http);

	body_len = sprintf(body, "{\"result\":true,\"token\":\"%s\"}", token->valuestring);
	len = sprintf(head, prefix, "POST", "fileCallback");
	len += sprintf(head + len, header, '1', sessionToken, body_len, "keep-alive");
	LOG("%s", head);
	LOG("%s\n", body);
	https = BIO_new_ssl_connect(ctx);
	BIO_get_ssl(https, &ssl);
	BIO_set_conn_hostname(https, "rak3ffdi.cloud.tds1.tapapis.cn:https");
	SSL_set_tlsext_host_name(ssl, "rak3ffdi.cloud.tds1.tapapis.cn");
	BIO_write(https, head, len);
	BIO_write(https, body, body_len);
	BIO_do_connect(https);
	len = BIO_read(https, head, sizeof head);
	LOGSTR(head, len);

	cJSON_GetObjectItemCaseSensitive(summary, "gameVersion")->valueint = 81;
	gen_summary(head, summary);
	
	resp = cJSON_GetObjectItemCaseSensitive(summary, "userId");
	body_len = sprintf(body, "{\"summary\":\"%s\",\"modifiedAt\":{\"__type\":\"Date\",\"iso\":\"%s\"},\"gameFile\":{\"__type\":\"Pointer\",\"className\":\"_File\",\"objectId\":\"%s\"},\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"user\":{\"__type\":\"Pointer\",\"className\":\"_User\",\"objectId\":\"%s\"}}", head, createdAt->valuestring, fileId->valuestring, resp->valuestring, resp->valuestring);
	len = sprintf(head, prefix, "PUT", "classes/_GameSave/");
	len += sprintf(head + len, "%s?", cJSON_GetObjectItemCaseSensitive(summary, "objectId")->valuestring);
	len += sprintf(head + len, header, '1', sessionToken, body_len, "keep-alive");
	LOG("%s", head);
	LOG("%s\n", body);
	BIO_write(https, head, len);
	BIO_write(https, body, body_len);
	len = BIO_read(https, head, sizeof head);
	LOGSTR(head, len);

	len = sprintf(head, prefix, "DELETE", "files/");
	len += sprintf(head + len, "%s", cJSON_GetObjectItemCaseSensitive(summary, "fileId")->valuestring);
	len += sprintf(head + len, header, '1', sessionToken, 0, "close");
	LOG("%s", head);
	BIO_write(https, head, len);
	len = BIO_read(https, head, sizeof head);
	LOGSTR(head, len);

	BIO_free_all(https);
	SSL_CTX_free(ctx);
	free(key);
	cJSON_Delete(uploadId);
	cJSON_Delete(token);
	cJSON_Delete(createdAt);
	cJSON_Delete(fileId);
}



struct record {
	cJSON* json;
	char level;
	float rks;
};
std::map<std::string, std::array<float, 4>> difficulties;

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

void cpp_bestn_internal(struct record* phi, unsigned char len, unsigned char progress[12], cJSON* gameRecord) {
	record* records = phi + 1;
	cJSON* song;
	record* low_record = records;
	cJSON_ArrayForEach(song, gameRecord) {
		std::map<std::string, std::array<float, 4>>::const_iterator iter = difficulties.find(song->string);
		if (iter == difficulties.end()) {
			sprintf(errorbuffer, "ERROR:std::out_of_range %s", song->string);
			throw errorbuffer;
		}
		std::array<float, 4> difficulty = iter->second;
		for (char level = 0; level < 4; level++) {
			if (level == 3 && difficulty.at(3) == 0)
				break;
			double acc = cJSON_GetArrayItem(song, 3 * level + 1)->valuedouble;
			if (progress && acc) {
				progress[3 * level]++;
				if (cJSON_GetArrayItem(song, 3 * level)->valueint == 1000000) {
					progress[3 * level + 1]++;
					progress[3 * level + 2]++;
				} else if (cJSON_GetArrayItem(song, 3 * level + 2)->valueint)
					progress[3 * level + 1]++;
			}
			if (acc < 55)
				continue;
			char compare_phi = difficulty.at(level) > phi->rks && cJSON_GetArrayItem(song, 3 * level)->valueint == 1000000;
			if (compare_phi || difficulty.at(level) > low_record->rks) {
				float rks = (acc - 55) / 45;
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

cJSON* internal_get_b19(cJSON* gameRecord) {
	record records[20] = {};
	cpp_bestn_internal(records, 19, 0, gameRecord);
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
	cpp_bestn_internal(records, 19, 0, gameRecord);
	cJSON* array = cJSON_CreateArray();
	float low = records[19].rks;
	cJSON* song;
	cJSON_ArrayForEach(song, gameRecord) {
		std::array<float, 4> difficulty = difficulties.at(song->string);
		for (char level = 0; level < 4; level++) {
			if (difficulty.at(level) < low)
				continue;
			float acc = cJSON_GetArrayItem(song, 3 * level + 1)->valuedouble;
			float rks = 0;
			if (acc > 55) {
				rks = (acc - 55) / 45;
				rks *= rks * difficulty.at(level);
			}
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

float get_rks_progress(cJSON* gameRecord, unsigned char progress[12]) {
	record records[20] = {};
	cpp_bestn_internal(records, 19, progress, gameRecord);
	float rks = 0;
	for (char i = 0; i < 20; i++)
		rks += records[i].rks;
	return rks;
}

void update_summary(cJSON* summary, cJSON* save) {
	cJSON *gameProgress = cJSON_GetObjectItemCaseSensitive(save, "gameProgress");
	cJSON_GetObjectItemCaseSensitive(summary, "challengeModeRank")->valueint = cJSON_GetObjectItemCaseSensitive(save, "challengeModeRank")->valueint;
	cJSON *user = cJSON_GetObjectItemCaseSensitive(save, "user");
	cJSON_SetValuestring(cJSON_GetObjectItemCaseSensitive(summary, "avatar"), cJSON_GetObjectItemCaseSensitive(user, "avatar")->valuestring);
	cJSON *gameRecord = cJSON_GetObjectItemCaseSensitive(save, "gameRecord");
	unsigned char progress[12] = {};
	cJSON_GetObjectItemCaseSensitive(summary, "rankingScore")->valuedouble = get_rks_progress(gameRecord, progress);
	cJSON* array = cJSON_GetObjectItemCaseSensitive(summary, "progress");
	for (char i = 0; i < 12; i++)
		cJSON_GetArrayItem(array, i)->valueint = progress[i];
}



struct Handle {
	char sessionToken[26];
	cJSON* summary;
	cJSON* save;
	char* buf;
};

EXPORT struct Handle *get_handle(char *sessionToken) {
	struct Handle *handle = (struct Handle*) calloc(1, sizeof(struct Handle));
	strcpy(handle->sessionToken, sessionToken);
	return handle;
}

EXPORT void free_handle(struct Handle* handle) {
	if (handle->summary) {
		cJSON_Delete(handle->summary);
		if (handle->save)
			cJSON_Delete(handle->save);
	}
	if (handle->buf)
		free(handle->buf);
	free(handle);
}


EXPORT char *get_nickname(struct Handle *handle) {
	SSL* ssl;
	char req[512];
	short len = sprintf(req, info_req, "users/me", handle->sessionToken);
	SSL_CTX* ctx = SSL_CTX_new(TLS_client_method());
	BIO* https = BIO_new_ssl_connect(ctx);
	BIO_get_ssl(https, &ssl);
	BIO_set_conn_hostname(https, "rak3ffdi.cloud.tds1.tapapis.cn:https");
	SSL_set_tlsext_host_name(ssl, "rak3ffdi.cloud.tds1.tapapis.cn");
	BIO_write(https, req, len);
	BIO_do_connect(https);
	cJSON* resp = read_json_body(https, 0, 0);
	SSL_CTX_free(ctx);
	if (cJSON_HasObjectItem(resp, "error")) {
		char* ptr = cJSON_GetObjectItemCaseSensitive(resp, "error")->valuestring;
		sprintf(errorbuffer, "ERROR:%s", ptr);
		cJSON_Delete(resp);
		return errorbuffer;
	}
	char *nickname = cJSON_GetObjectItemCaseSensitive(resp, "nickname")->valuestring;
	len = strlen(nickname);
	if (handle->buf)
		free(handle->buf);
	handle->buf = (char*) malloc(len + 1);
	strcpy(handle->buf, nickname);
	cJSON_Delete(resp);
	return handle->buf;
}

EXPORT const char *get_summary(struct Handle *handle) {
	if (!handle->summary) {
		try {
			handle->summary = internal_get_summary(handle->sessionToken);
		} catch (const char* e) {
			return e;
		}
	}
	if (handle->buf)
		free(handle->buf);
	handle->buf = cJSON_PrintUnformatted(handle->summary);
	return handle->buf;
}

EXPORT const char *get_save(struct Handle *handle) {
	if (!handle->summary) {
		try {
			handle->summary = internal_get_summary(handle->sessionToken);
		} catch (const char* e) {
			return e;
		}
	}
	if (!handle->save) {
		char* str = cJSON_GetObjectItemCaseSensitive(handle->summary, "url")->valuestring;
		BIO* save_bio = download_save(str);
		handle->save = parse_save(save_bio);
		BIO_free(save_bio);
	}
	if (handle->buf)
		free(handle->buf);
	handle->buf = cJSON_PrintUnformatted(handle->save);
	return handle->buf;
}

EXPORT void load_difficulty(char *path) {
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

EXPORT const char *get_b19(struct Handle *handle) {
	char *str;
	if (!handle->summary) {
		try {
			handle->summary = internal_get_summary(handle->sessionToken);
		} catch (const char* e) {
			return e;
		}
	}
	if (!handle->save) {
		str = cJSON_GetObjectItemCaseSensitive(handle->summary, "url")->valuestring;
		BIO* save_bio = download_save(str);
		handle->save = parse_save(save_bio);
		BIO_free(save_bio);
	}
	cJSON* gameRecord = cJSON_GetObjectItemCaseSensitive(handle->save, "gameRecord");
	cJSON* b19;
	try {
		b19 = internal_get_b19(gameRecord);
	} catch(char* e) {
		return e;
	}
	if (handle->buf)
		free(handle->buf);
	handle->buf = cJSON_PrintUnformatted(b19);
	cJSON_Delete(b19);
	return handle->buf;
}
/*
void re8(struct Handle *handle) {
	handle->summary = internal_get_summary(handle->sessionToken);
	char* str = cJSON_GetObjectItemCaseSensitive(handle->summary, "url")->valuestring;
	BIO* save_bio = download_save(str);
	handle->save = parse_save(save_bio);
	BIO_free(save_bio);

	cJSON* gameProgress = cJSON_GetObjectItemCaseSensitive(handle->save, "gameProgress");
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8UnlockBegin"), 0);
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8UnlockSecondPhase"), 0);
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8Passed"), 0);
	cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8SongUnlocked")->valueint = 0;
	char* save_buf;
	short len = gen_save(&save_buf, handle->save);
	update_summary(handle->summary, handle->save);
	upload_save(handle->sessionToken, save_buf, len, handle->summary);
	free(save_buf);
}
*/
}
