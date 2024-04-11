#include <string.h>
#include <openssl/buffer.h>
#include <openssl/evp.h>
#include <zip.h>
#include "cJSON.h"

const EVP_CIPHER* cipher;
const unsigned char key[] = {0xe8,0x96,0x9a,0xd2,0xa5,0x40,0x25,0x9b,0x97,0x91,0x90,0x8b,0x88,0xe6,0xbf,0x03,0x1e,0x6d,0x21,0x95,0x6e,0xfa,0xd6,0x8a,0x50,0xdd,0x55,0xd6,0x7a,0xb0,0x92,0x4b};
const unsigned char iv[] = {0x2a,0x4f,0xf0,0x8a,0xc8,0x0d,0x63,0x07,0x00,0x57,0xc5,0x95,0x18,0xc8,0x32,0x53};

enum Type {
	Bool,
	u8,
	u16,
	_float,
	string,
	varshort,
	u16array,
};

struct LeafNode
{
	char type;
	char* name;
};

struct Nodes {
	char node_len;
	struct LeafNode* nodes;
};

struct LeafNode Summary[] = {
	{u8, "saveVersion"},
	{u16, "challengeModeRank"},
	{_float, "rankingScore"},
	{u8, "gameVersion"},
	{string, "avatar"},
	{u16array, "progress"}
};

struct LeafNode GameKey1[] = {{u8, "lanotaReadKeys"}};

struct LeafNode GameKey2[] = {{Bool, "camelliaReadKey"}};

struct Nodes GameKey[] = {
	{sizeof GameKey1 / sizeof(struct LeafNode), GameKey1},
	{sizeof GameKey2 / sizeof(struct LeafNode), GameKey2}
};

struct LeafNode GameProgress1[] = {
	{Bool, "isFirstRun"},
	{Bool, "legacyChapterFinished"},
	{Bool, "alreadyShowCollectionTip", },
	{Bool, "alreadyShowAutoUnlockINTip"},
	{string, "completed"},
	{u8, "songUpdateInfo"},
	{u16, "challengeModeRank"},
	{varshort, "money"},
	{u8, "unlockFlagOfSpasmodic"},
	{u8, "unlockFlagOfIgallta"},
	{u8, "unlockFlagOfRrharil"},
	{u8, "flagOfSongRecordKey"},
};

struct LeafNode GameProgress2[] = {
	{u8, "randomVersionUnlocked"}
};

struct LeafNode GameProgress3[] = {
	{Bool, "chapter8UnlockBegin"},
	{Bool, "chapter8UnlockSecondPhase"},
	{Bool, "chapter8Passed"},
	{u8, "chapter8SongUnlocked"}
};

struct Nodes GameProgress[] = {
	{sizeof GameProgress1 / sizeof(struct LeafNode), GameProgress1},
	{sizeof GameProgress2 / sizeof(struct LeafNode), GameProgress2},
	{sizeof GameProgress3 / sizeof(struct LeafNode), GameProgress3},
};

struct LeafNode User[] = {
	{Bool, "showPlayerId"},
	{string, "selfIntro"},
	{string, "avatar"},
	{string, "background"}
};

struct LeafNode Settings[] = {
	{Bool, "chordSupport"},
	{Bool, "fcAPIndicator"},
	{Bool, "enableHitSound"},
	{Bool, "lowResolutionMode"},
	{string, "deviceName"},
	{_float, "bright"},
	{_float, "musicVolume"},
	{_float, "effectVolume"},
	{_float, "hitSoundVolume"},
	{_float, "soundOffset"},
	{_float, "noteScale"}
};

short read_varshort(signed char** ptr) {
	if ((*ptr)[0] >= 0) {
		++*ptr;
		return (*ptr)[-1];
	} else {
		*ptr += 2;
		return (*ptr)[-2] & 0x7F ^ (*ptr)[-1] << 7;
	}
}

void write_varshort(BIO* bio, short n) {
	if (n < 128) {
		BIO_write(bio, &n, 1);
	} else {
		n = n << 1 & 0xF00 | n & 0x7F | 0x80;
		BIO_write(bio, &n, 2);
	}
}

cJSON* read_string(char** ptr, char end) {
	short len = read_varshort(ptr);
	char tmp = (*ptr)[len - end];
	(*ptr)[len - end] = 0;
	cJSON* str = cJSON_CreateString(*ptr);
	(*ptr)[len - end] = tmp;
	*ptr += len;
	return str;
}

void write_string(BIO* bio, char* str, char end) {
	short len = strlen(str);
	write_varshort(bio, len + end);
	BIO_write(bio, str, len);
}

void deserialization(cJSON* json, char** ptr, struct LeafNode* nodes, char len) {
	cJSON* item;
	char bit = 0;
	for (char i = 0; i < len; i++) {
		if (nodes[i].type == Bool) {
			item = cJSON_CreateBool((**ptr >> bit++) & 1);
			cJSON_AddItemToObject(json, nodes[i].name, item);
			continue;
		}
		if (bit) {
			bit = 0;
			++*ptr;
		}
		if (nodes[i].type == u8)
			item = cJSON_CreateNumber(*(*ptr)++);
		else if (nodes[i].type == u16) {
			item = cJSON_CreateNumber(*(short*)(*ptr));
			*ptr += 2;
		} else if (nodes[i].type == _float) {
			item = cJSON_CreateNumber(*(float*)(*ptr));
			*ptr += 4;
		} else if (nodes[i].type == string) {
			item = read_string(ptr, 0);
		} else if (nodes[i].type == u16array) {
			item = cJSON_CreateArray();
			for (int ii = 0; ii < 12; ii++) {
				cJSON_AddItemToArray(item, cJSON_CreateNumber(*(short*)(*ptr)));
				*ptr += 2;
			}
		} else if (nodes[i].type == varshort) {
			item = cJSON_CreateArray();
			for (int ii = 0; ii < 5; ii++) {
				short num = read_varshort(ptr);
				cJSON_AddItemToArray(item, cJSON_CreateNumber(num));
			}
		}
		cJSON_AddItemToObject(json,nodes[i].name, item);
	}
	if (bit)
		++*ptr;
}

void serialization(BIO* bio, cJSON* json, struct LeafNode* nodes, char len) {
	char bit = 0;
	char num = 0;
	for (char i = 0; i < len; i++) {
		cJSON* item = cJSON_GetObjectItemCaseSensitive(json, nodes[i].name);
		if (nodes[i].type == Bool) {
			if (cJSON_IsTrue(item))
				num |= 1 << bit;
			bit++;
			continue;
		}
		if (bit) {
			BIO_write(bio, &num, 1);
			bit = 0;
			num = 0;
		}
		if (nodes[i].type == u8 || nodes[i].type == u16)
			BIO_write(bio, &item->valueint, nodes[i].type);
		else if (nodes[i].type == _float) {
			float f = (float) item->valuedouble;
			BIO_write(bio, &f, 4);
		} else if (nodes[i].type == string)
			write_string(bio, item->valuestring, 0);
		else if (nodes[i].type == u16array)
			for (char ii = 0; ii < 12; ii++)
				BIO_write(bio, &cJSON_GetArrayItem(item, ii)->valueint, 2);
		else if (nodes[i].type == varshort)
			for (char ii = 0; ii < 5; ii++)
				write_varshort(bio, cJSON_GetArrayItem(item, ii)->valueint);
	}
	if (bit)
		BIO_write(bio, &num, 1);
}

void deserializationNodes(cJSON* json, char** ptr, struct Nodes* nodes) {
	char version = cJSON_GetObjectItemCaseSensitive(json, "version")->valueint;
	for (char i = 0; i < version; i++)
		deserialization(json, ptr, nodes[i].nodes, nodes[i].node_len);
}

void serializationNodes(BIO* bio, cJSON* json, struct Nodes* nodes) {
	char version = cJSON_GetObjectItemCaseSensitive(json, "version")->valueint;
	for (char i = 0; i < version; i++)
		serialization(bio, json, nodes[i].nodes, nodes[i].node_len);
}

cJSON* deserializationMap(char** ptr, char end) {
	cJSON* map = cJSON_CreateObject();
	short length = read_varshort(ptr);
	for (short i = 0; i < length; i++) {
		cJSON* key = read_string(ptr, end);
		char* next = *ptr + **ptr + 1;
		++*ptr;
		cJSON* array = cJSON_CreateArray();
		char len = *(*ptr)++;
		if (end) {
			char fc = *(*ptr)++;
			for (char level = 0; level < 4; level++) {
				if (len >> level & 1) {
					cJSON_AddItemToArray(array, cJSON_CreateNumber(*(int*)(*ptr)));
					*ptr += 4;
					cJSON_AddItemToArray(array, cJSON_CreateNumber(*(float*)(*ptr)));
					*ptr += 4;
					cJSON_AddItemToArray(array, cJSON_CreateNumber(fc >> level & 1));
				} else
					for (char ii = 0; ii < 3; ii++)
						cJSON_AddItemToArray(array, cJSON_CreateNumber(0));
			}
		} else
			for (char ii = 0; ii < 5; ii++)
				if (len >> ii & 1)
					cJSON_AddItemToArray(array, cJSON_CreateNumber(*(*ptr)++));
				else
					cJSON_AddItemToArray(array, cJSON_CreateNumber(0));
		cJSON_AddItemToObject(map, key->valuestring, array);
		cJSON_Delete(key);
		*ptr = next;
	}
	return map;
}

void serializationMap(BIO* bio, cJSON* map, char end) {
	int length = cJSON_GetArraySize(map);
	write_varshort(bio, length);
	cJSON* array;
	cJSON* item;
	cJSON_ArrayForEach(array, map) {
		char len = 0;
		char fc = 0;
		write_string(bio, array->string, end);
		if (end) {
			BIO_write(bio, ".0", 2);
			length = 2;
			for (char level = 0; level < 4; level++) {
				if (cJSON_GetArrayItem(array, 3 * level)->valueint) {
					length += 8;
					len |= 1 << level;
					if (cJSON_GetArrayItem(array, 3 * level + 2)->valueint)
						fc |= 1 << level;
				}
			}
			BIO_write(bio, &length, 1);
			BIO_write(bio, &len, 1);
			BIO_write(bio, &fc, 1);
			for (char level = 0; level < 4; level++) {
				length = cJSON_GetArrayItem(array, 3 * level)->valueint;
				if (length) {
					float acc = cJSON_GetArrayItem(array, 3 * level + 1)->valuedouble;
					BIO_write(bio, &length, 4);
					BIO_write(bio, &acc, 4);
				}
			}
		} else {
			for (char i = 0; i < 5; i++) {
				if (cJSON_GetArrayItem(array, i)->valueint) {
					fc |= 1 << i;
					len++;
				}
			}
			len++;
			BIO_write(bio, &len, 1);
			BIO_write(bio, &fc, 1);
			cJSON_ArrayForEach(item, array) {
				if (item->valueint)
					BIO_write(bio, &item->valueint, 1);
			}
		}
	}
}

char* b64encode(void* mem, char* data, char len) {
	if (len == -1)
		len = strlen(data);
	char b64_len = len / 3 * 4;
	if (len % 3)
		b64_len += 4;
	if (mem) {
		EVP_EncodeBlock(mem, data, len);
		return 0;
	} else {
		mem = malloc(b64_len + 1);
		EVP_EncodeBlock(mem, data, len);
		return mem;
	}
}

char b64decode(char* mem, char** pmem, char* str) {
	char len = strlen(str);
	char mem_len = len / 4 * 3;
	if (str[len - 1] == '=') {
		mem_len--;
		if (str[len - 2] == '=')
			mem_len--;
	}
	if (!mem) {
		mem = malloc(mem_len);
		*pmem = mem;
	}
	EVP_DecodeBlock(mem, str, len);
	return mem_len;
}

char* decrypt(EVP_CIPHER_CTX* ctx, BIO* bio, zip_t* zip, char* name) {
	if (cipher == 0)
		cipher = EVP_aes_256_cbc();
	EVP_CIPHER_CTX_reset(ctx);
	EVP_DecryptInit(ctx, cipher, key, iv);
	BIO_reset(bio);
	int len;
	char ciphertext[1024];
	char plaintext[sizeof ciphertext + 16];
	zip_file_t* zip_file = zip_fopen(zip, name, 0);
	zip_fread(zip_file, plaintext, 1);
	BIO_write(bio, plaintext, 1);
	while (1) {
		len = zip_fread(zip_file, ciphertext, sizeof ciphertext);
		if (len == 0) break;
		EVP_DecryptUpdate(ctx, plaintext, &len, ciphertext, len);
		BIO_write(bio, plaintext, len);
	}
	zip_fclose(zip_file);
	EVP_DecryptFinal_ex(ctx, plaintext, &len);
	BIO_write(bio, plaintext, len);
	BIO_get_mem_data(bio, &name);
	return name;
}

short encrypt(EVP_CIPHER_CTX* ctx, BUF_MEM* mem) {
	if (cipher == 0)
		cipher = EVP_aes_256_cbc();
	EVP_CIPHER_CTX_reset(ctx);
	short inl = mem->length;
	short encrypt_len = inl % 16;
	if (encrypt_len)
		encrypt_len = inl + 16 - encrypt_len;
	else
		encrypt_len = inl;
	encrypt_len++;
	if (encrypt_len > mem->max) {
		mem->data = OPENSSL_realloc(mem->data, encrypt_len);
		mem->max = encrypt_len;
	}
	EVP_EncryptInit(ctx, cipher, key, iv);
	inl--;
	int outl;
	EVP_EncryptUpdate(ctx, mem->data + 1, &outl, mem->data + 1, inl);
	inl = outl;
	EVP_EncryptFinal_ex(ctx, mem->data + 1 + inl, &outl);
	return outl + inl + 1;
}

cJSON* read_version(char** ptr) {
	cJSON* json = cJSON_CreateObject();
	cJSON_AddItemToObject(json, "version", cJSON_CreateNumber(*(*ptr)++));
	return json;
}

cJSON* parse_save(BIO* bio_zip) {
	cJSON* json = cJSON_CreateObject();

	char* ptr;
	char* end;
	short len = BIO_get_mem_data(bio_zip, &ptr);
	zip_source_t* source = zip_source_buffer_create(ptr, len, 0, 0);
	zip_t* zip = zip_open_from_source(source, ZIP_RDONLY, 0);
	BIO* bio = BIO_new(BIO_s_mem());
	EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
	
	ptr = decrypt(ctx, bio, zip, "gameRecord") + 1;
	cJSON* gameRecord = deserializationMap(&ptr, 2);
	cJSON_AddItemToObject(json, "gameRecord", gameRecord);

	decrypt(ctx, bio, zip, "gameKey");
	len = BIO_get_mem_data(bio, &ptr);
	end = ptr + len;
	cJSON* gameKey = read_version(&ptr);
	cJSON_AddItemToObject(gameKey, "map", deserializationMap(&ptr, 0));
	deserializationNodes(gameKey, &ptr, GameKey);
	if (end > ptr) {
		void* b64 = b64encode(0, ptr, end - ptr);
		cJSON_AddStringToObject(gameKey, "overflow", b64);
		free(b64);
	}
	cJSON_AddItemToObject(json, "gameKey", gameKey);

	decrypt(ctx, bio, zip, "gameProgress");
	len = BIO_get_mem_data(bio, &ptr);
	end = ptr + len;
	cJSON* gameProgress = read_version(&ptr);
	deserializationNodes(gameProgress, &ptr, GameProgress);
	if (end > ptr) {
		void* b64 = b64encode(0, ptr, end - ptr);
		cJSON_AddStringToObject(gameProgress, "overflow", b64);
		free(b64);
	}
	cJSON_AddItemToObject(json, "gameProgress", gameProgress);

	ptr = decrypt(ctx, bio, zip, "user") + 1;
	cJSON* user = cJSON_CreateObject();
	deserialization(user, &ptr, User, sizeof User / sizeof(struct LeafNode));
	cJSON_AddItemToObject(json, "user", user);

	ptr = decrypt(ctx, bio, zip, "settings") + 1;
	cJSON* settings = cJSON_CreateObject();
	deserialization(settings, &ptr, Settings, sizeof Settings / sizeof(struct LeafNode));
	cJSON_AddItemToObject(json, "settings", settings);

	EVP_CIPHER_CTX_free(ctx);
	BIO_free(bio);
	zip_discard(zip);
	return json;
}

short gen_save(char** ret, cJSON* json) {
	char* ptr;
	short len;
	char index = 1;
	cJSON* item;

	zip_source_t* source = zip_source_buffer_create(0, 0, 1, 0);
	zip_source_keep(source);
	zip_t* zip = zip_open_from_source(source, 0, 0);
	
	item = cJSON_GetObjectItemCaseSensitive(json, "gameRecord");
	BIO* bio_gameRecord = BIO_new(BIO_s_mem());
	BIO_write(bio_gameRecord, &index, 1);
	serializationMap(bio_gameRecord, item, 2);

	item = cJSON_GetObjectItemCaseSensitive(json, "gameKey");
	BIO* bio_gameKey = BIO_new(BIO_s_mem());
	BIO_write(bio_gameKey, &cJSON_GetObjectItemCaseSensitive(item, "version")->valueint, 1);
	serializationMap(bio_gameKey, cJSON_GetObjectItemCaseSensitive(item, "map"), 0);
	serializationNodes(bio_gameKey, item, GameKey);
	if (cJSON_HasObjectItem(item, "overflow")) {
		*ret = cJSON_GetObjectItemCaseSensitive(item, "overflow")->valuestring;
		len = b64decode(0, &ptr, *ret);
		BIO_write(bio_gameKey, ptr, len);
		free(ptr);
	}

	item = cJSON_GetObjectItemCaseSensitive(json, "gameProgress");
	BIO* bio_gameProgress = BIO_new(BIO_s_mem());
	BIO_write(bio_gameProgress, &cJSON_GetObjectItemCaseSensitive(item, "version")->valueint, 1);
	serializationNodes(bio_gameProgress, item, GameProgress);
	if (cJSON_HasObjectItem(item, "overflow")) {
		*ret = cJSON_GetObjectItemCaseSensitive(item, "overflow")->valuestring;
		len = b64decode(0, &ptr, *ret);
		BIO_write(bio_gameProgress, ptr, len);
		free(ptr);
	}

	item = cJSON_GetObjectItemCaseSensitive(json, "user");
	BIO* bio_user = BIO_new(BIO_s_mem());
	BIO_write(bio_user, &index, 1);
	serialization(bio_user, item, User, sizeof User / sizeof(struct LeafNode));

	item = cJSON_GetObjectItemCaseSensitive(json, "settings");
	BIO* bio_settings = BIO_new(BIO_s_mem());
	BIO_write(bio_settings, &index, 1);
	serialization(bio_settings, item, Settings, sizeof Settings / sizeof(struct LeafNode));



	zip_source_t* par;
	BUF_MEM* mem;
	EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();

	BIO_get_mem_ptr(bio_gameKey, &mem);
	len = encrypt(ctx, mem);
	par = zip_source_buffer(zip, mem->data, len, 0);
	index = zip_file_add(zip, "gameKey", par, 0);
	zip_set_file_compression(zip, index, ZIP_CM_STORE, 0);

	BIO_get_mem_ptr(bio_gameProgress, &mem);
	len = encrypt(ctx, mem);
	par = zip_source_buffer(zip, mem->data, len, 0);
	index = zip_file_add(zip, "gameProgress", par, 0);
	zip_set_file_compression(zip, index, ZIP_CM_STORE, 0);

	BIO_get_mem_ptr(bio_gameRecord, &mem);
	len = encrypt(ctx, mem);
	par = zip_source_buffer(zip, mem->data, len, 0);
	index = zip_file_add(zip, "gameRecord", par, 0);
	zip_set_file_compression(zip, index, ZIP_CM_STORE, 0);

	BIO_get_mem_ptr(bio_settings, &mem);
	len = encrypt(ctx, mem);
	par = zip_source_buffer(zip, mem->data, len, 0);
	index = zip_file_add(zip, "settings", par, 0);
	zip_set_file_compression(zip, index, ZIP_CM_STORE, 0);

	BIO_get_mem_ptr(bio_user, &mem);
	len = encrypt(ctx, mem);
	par = zip_source_buffer(zip, mem->data, len, 0);
	index = zip_file_add(zip, "user", par, 0);
	zip_set_file_compression(zip, index, ZIP_CM_STORE, 0);

	EVP_CIPHER_CTX_free(ctx);
	zip_close(zip);
	BIO_free(bio_gameRecord);
	BIO_free(bio_gameKey);
	BIO_free(bio_gameProgress);
	BIO_free(bio_user);
	BIO_free(bio_settings);



	zip_stat_t stat;
	zip_source_stat(source, &stat);
	zip_source_open(source);
	*ret = malloc(stat.size);
	zip_source_read(source, *ret, stat.size);
	zip_source_close(source);
	return stat.size;
}

cJSON* parse_summary(char* ptr, char* base64) {
	b64decode(ptr, 0, base64);
	cJSON* summary = cJSON_CreateObject();
	deserialization(summary, &ptr, Summary, sizeof Summary / sizeof(struct LeafNode));
	return summary;
}

void gen_summary(char* mem, cJSON* summary) {
	char* ptr;
	BIO* bio = BIO_new(BIO_s_mem());
	serialization(bio, summary, Summary, sizeof Summary / sizeof(struct LeafNode));
	char len = BIO_get_mem_data(bio, &ptr);
	b64encode(mem, ptr, len);
	BIO_free(bio);
}
