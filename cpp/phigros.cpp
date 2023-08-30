#include <fstream>
#include <iostream>
#include <openssl/ssl.h>
#include <regex>
#include <unordered_map>
#include <zip.h>
#ifdef __linux__
  #include <netdb.h>
  #include <unistd.h>
void close_socket(int sock) {
	close(sock);
}
#elif defined _WIN32
  #include <array>
  #include <ws2tcpip.h>
  #pragma comment(lib, "ws2_32")
void close_socket(int sock) {
	closesocket(sock);
}
#endif

void print_struct(void* vptr, int size) {
	unsigned char* ptr = (unsigned char*) vptr;
	for (short i = 0; i < size; i++)
		printf("%02X ", ptr[i]);
	printf("\n");
}

const short save_size = 15 * 1024;
const EVP_MD* md = EVP_md5();
const EVP_CIPHER* cipher = EVP_aes_256_cbc();
const unsigned char key[] = {0xe8,0x96,0x9a,0xd2,0xa5,0x40,0x25,0x9b,0x97,0x91,0x90,0x8b,0x88,0xe6,0xbf,0x03,0x1e,0x6d,0x21,0x95,0x6e,0xfa,0xd6,0x8a,0x50,0xdd,0x55,0xd6,0x7a,0xb0,0x92,0x4b};
const unsigned char iv[] = {0x2a,0x4f,0xf0,0x8a,0xc8,0x0d,0x63,0x07,0x00,0x57,0xc5,0x95,0x18,0xc8,0x32,0x53};
const char global_req[] = "GET /1.1/%s HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\nConnection: close\r\n\r\n";
const char base64[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
SSL_CTX *ctx = SSL_CTX_new(TLS_client_method());

char base64decode(char* ptr, char len, char* result) {
	char end;
	if (ptr[len - 2] == '=')
		end = 2;
	else if (ptr[len - 1] == '=')
		end = 3;
	else
		end = 0;
	int*& iresult = (int*&) result;
	char tmp;
	for (short i = 0; i < len / 4 - (bool) end; i++) {
		*iresult = strchr(base64, *ptr) - base64 << 18 ^ strchr(base64, *(ptr+1)) - base64 << 12 ^ strchr(base64, *(ptr+2)) - base64 << 6 ^ strchr(base64, *(ptr+3)) - base64;
		tmp = *result;
		*result = *(result + 2);
		*(result + 2) = tmp;
		ptr += 4;
		result += 3;
	}
	if (end) {
		*iresult = 0;
		for (char i = 0; i < end; i++)
			*iresult ^= strchr(base64, *(ptr + i)) - base64 << 18 - 6 * i;
		*result = *(result + 2);
		end = 4 - end;
	}
	printf("len = %d, end = %d\n", len, end);
	return len / 4 * 3 - end;
}

std::string base64encode(char* cptr, char len) {
	unsigned char* ptr = (unsigned char*) cptr;
	char end = len % 3;
	len /= 3;
	int num;
	std::string result(4 * (len + (bool) end), '\0');
	for (char i = 0; i < len; i++) {
		num = ptr[3 * i] << 16 ^ ptr[3 * i + 1] << 8 ^ ptr[3 * i + 2];
		result.at(4 * i) = base64[num >> 18];
		result.at(4 * i + 1) = base64[num >> 12 & 0b00111111];
		result.at(4 * i + 2) = base64[num >> 6 & 0b00111111];
		result.at(4 * i + 3) = base64[num & 0b00111111];
	}
	if (end) {
		num = 0;
		for (char i = 0; i < end; i++)
			num ^= ptr[3 * len + i] << 8 - 8 * i;
		end += 1;
		for (char i = 0; i < end; i++)
			result.at(4 * len + i) = base64[num >> 10 - 6 * i & 0b00111111];
		for (; end < 4; end++)
			result.at(4 * len + end) = '=';
	}
	return result;
}

char getbit(char b, int i) {
    return b & 1 << i;
}

void setbit(char* b, char index, bool v) {
	if (v) {
		*b |= 1 << index;
	} else {
		*b &= (~(1 << index));
	}
}

short read_varshort(char*& pos) {
	if (pos[0] >= 0) {
		pos++;
		return *(pos - 1);
	} else {
		pos += 2;
		return *(pos -2) & 0b01111111 ^ *(pos - 1) << 7;
	}
}

void write_varshort(char*& pos, unsigned char v) {
	*pos = v;
	pos++;
	if (v > 127) {
		*pos = 1;
		pos++;
	}
}

std::string read_string(char*& pos, char offset) {
	int len = read_varshort(pos);
	pos += len;
	return std::string(pos -len, len - offset);
}

unsigned char read_cstring(char*& pos) {
	unsigned char len = read_varshort(pos);
	pos += len;
	return len;
}

struct Level {
    int score;
    float acc;
    bool fc;
};

struct Song {
    std::string id;
    Level levels[4];
};

struct SongLevel {
    std::string id;
    char level;
    int score;
    float acc;
    bool fc;
    float difficulty;
    float rks;
};

bool comp(SongLevel& o1, SongLevel& o2) {
    return o1.rks > o2.rks;
}

struct Node {
	std::string type;
	std::string name;
	short offset;
};

void read_nodes(void* vbuf, Node nodes[], char size, void* vobject) {
	char* buf = (char*) vbuf;
	char* object = (char*) vobject;
	bool b = false;
	for (char i = 0; i < size; i++) {
		char index = 0;
		if (nodes[i].type == "bool") {
			*(object + nodes[i].offset) = getbit(*buf, index);
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
			*(object + nodes[i].offset) = *buf;
			buf++;
		} else if (nodes[i].type == "short") {
			*(short*) (object + nodes[i].offset) = *(short*) buf;
			buf += 2;
		} else if (nodes[i].type == "float") {
			*(float*) (object + nodes[i].offset) = *(float*) buf;
			buf += 4;
		} else if (nodes[i].type == "string") {
			*(std::string*) (object + nodes[i].offset) = read_string(buf, 0);
		} else if  (nodes[i].type == "short5") {
			short* sobject = (short*) (object + nodes[i].offset);
			for (char i = 0; i < 5; i++)
				sobject[i] = read_varshort(buf);
		} else if  (nodes[i].type == "short12") {
			short* sobject = (short*) (object + nodes[i].offset);
			short* sbuf = (short*) buf;
			for (char i = 0; i < 12; i++)
				sobject[i] = sbuf[i];
			buf += 24;
		}
	}
}

struct Summary {
	char saveVersion;
	short challengeModeRank;
	float rks;
	char gameVersion;
	std::string avatar;
	short progress[12];
	std::string url;
	std::string update;
};

Node nodeSummary[] = {
	{"char", "saveVersion", offsetof(Summary, saveVersion)},
	{"short", "challengeModeRank", offsetof(Summary, challengeModeRank)},
	{"float", "rks", offsetof(Summary, rks)},
	{"char", "gameVersion", offsetof(Summary, gameVersion)},
	{"string", "avatar", offsetof(Summary, avatar)},
	{"short12", "progress", offsetof(Summary, progress)}
};

struct GameKey {
	std::string key;
	char readCollection;
	bool unlockSingle;
	char collection;
	bool illustration;
	bool avatar;
};

Node nodeKey[] = {
	{"char", "readCollection", offsetof(GameKey, readCollection)},
	{"bool", "unlockSingle", offsetof(GameKey, unlockSingle)},
	{"char", "collection", offsetof(GameKey, collection)},
	{"bool", "illustration", offsetof(GameKey, illustration)},
	{"bool", "avatar", offsetof(GameKey, avatar)}
};

struct GameProgress {
	bool isFirstRun;
	bool legacyChapterFinished;
	bool alreadyShowCollectionTip;
	bool alreadyShowAutoUnlockINTip;
	std::string completed;
	char songUpdateInfo;
	short challengeModeRank;
	short money[5];
	char unlockFlagOfSpasmodic;
	char unlockFlagOfIgallta;
	char unlockFlagOfRrharil;
	char flagOfSongRecordKey;
	bool chapter8UnlockBegin;
	bool chapter8UnlockSecondPhase;
	bool chapter8Passed;
	char chapter8SongUnlocked;
};

Node nodeGameProgress[] {
	{"bool", "isFirstRun", offsetof(GameProgress, isFirstRun)},
	{"bool", "legacyChapterFinished", offsetof(GameProgress, legacyChapterFinished)},
	{"bool", "alreadyShowCollectionTip", offsetof(GameProgress, alreadyShowCollectionTip)},
	{"bool", "alreadyShowAutoUnlockINTip", offsetof(GameProgress, alreadyShowAutoUnlockINTip)},
	{"string", "completed", offsetof(GameProgress, completed)},
	{"char", "songUpdateInfo", offsetof(GameProgress, songUpdateInfo)},
	{"short", "challengeModeRank", offsetof(GameProgress, challengeModeRank)},
	{"short5", "money", offsetof(GameProgress, money)},
	{"char", "unlockFlagOfSpasmodic", offsetof(GameProgress, unlockFlagOfSpasmodic)},
	{"char", "unlockFlagOfIgallta", offsetof(GameProgress, unlockFlagOfIgallta)},
	{"char", "unlockFlagOfRrharil", offsetof(GameProgress, unlockFlagOfRrharil)},
	{"char", "flagOfSongRecordKey", offsetof(GameProgress, flagOfSongRecordKey)},
	{"bool", "chapter8UnlockBegin", offsetof(GameProgress, chapter8UnlockBegin)},
	{"bool", "chapter8UnlockSecondPhase", offsetof(GameProgress, chapter8UnlockSecondPhase)},
	{"bool", "chapter8Passed", offsetof(GameProgress, chapter8Passed)},
	{"char", "chapter8SongUnlocked", offsetof(GameProgress, chapter8SongUnlocked)}
};

struct User {
	bool showPlayerId;
	std::string selfIntro;
	std::string avatar;
	std::string background;
};

Node nodeUser[] = {
	{"bool", "showPlayerId", offsetof(User, showPlayerId)},
	{"string", "selfIntro", offsetof(User, selfIntro)},
	{"string", "avatar", offsetof(User, avatar)},
	{"string", "background", offsetof(User, background)}
};

struct Settings {
	bool chordSupport;
	bool fcAPIndicator;
	bool enableHitSound;
	bool lowResolutionMode;
	std::string deviceName;
	float bright;
	float musicVolume;
	float effectVolume;
	float hitSoundVolume;
	float soundOffset;
	float noteScale;
};

Node nodeSettings[] = {
	{"bool", "chordSupport", offsetof(Settings, chordSupport)},
	{"bool", "fcAPIndicator", offsetof(Settings, fcAPIndicator)},
	{"bool", "enableHitSound", offsetof(Settings, enableHitSound)},
	{"bool", "lowResolutionMode", offsetof(Settings, lowResolutionMode)},
	{"string", "deviceName", offsetof(Settings, deviceName)},
	{"float", "bright", offsetof(Settings, bright)},
	{"float", "musicVolume", offsetof(Settings, musicVolume)},
	{"float", "effectVolume", offsetof(Settings, effectVolume)},
	{"float", "hitSoundVolume", offsetof(Settings, hitSoundVolume)},
	{"float", "soundOffset", offsetof(Settings, soundOffset)},
	{"float", "noteScale", offsetof(Settings, noteScale)}
};

struct Save {
	GameProgress gameProgress;
	User user;
	Settings settings;
};

std::unordered_map<std::string, std::array<float, 4>> difficulty;
void read_difficulty() {
	difficulty.clear();
    std::ifstream f("difficulty.csv");
    std::string line;
    while (std::getline(f, line)) {
	char indexs[5];
	char index = 0;
	for (char i = 0; i < 5; i++) {
	    index = line.find(',', index + 1);
	    indexs[i] = index;
	}
	index = 4;
	if (indexs[3] == -1)
	    index--;
	indexs[index] = line.length();
	std::array<float, 4> floats;
	for (char i = 0; i < index; i++)
	    floats[i] = std::stof(line.substr(indexs[i] + 1, indexs[i + 1] - indexs[i] - 1), 0);
	difficulty[line.substr(0, indexs[0])] = floats;
    }
    f.close();
}

sockaddr save_addr;
struct sockaddr* dns(char* domain, short port) {
    struct addrinfo hint = {0, AF_INET, SOCK_STREAM};
    struct addrinfo* addrs;
    getaddrinfo(domain, 0, &hint, &addrs);
    freeaddrinfo(addrs);
    struct sockaddr* addr = addrs->ai_addr;
    char* pport = (char*) &port;
    addr->sa_data[0] = pport[1];
    addr->sa_data[1] = pport[0];
    printf("%s: %d.%d.%d.%d\n", domain, (unsigned char) addr->sa_data[2], (unsigned char) addr->sa_data[3], (unsigned char) addr->sa_data[4], (unsigned char) addr->sa_data[5]);
    return addr;
}

sockaddr info_addr;
sockaddr upload_addr;
void init() {
	sockaddr* addr;
	addr = dns("rak3ffdi.cloud.tds1.tapapis.cn", 443);
	info_addr = *addr;
	addr = dns("upload.qiniup.com", 80);
	upload_addr = *addr;
	addr = dns("rak3ffdi.tds1.tapfiles.cn", 80);
	save_addr = *addr;
}

void append(char*& dest, const char* src, short size) {
	memcpy(dest, src, size);
	dest += size;
}

std::regex replayer("e\":\"([^\"]+)");
std::string get_player(char* sessionToken) {
	SSL *ssl = SSL_new(ctx);
	printf("ssl = %p\n", ssl);
    int sock = socket(PF_INET, SOCK_STREAM, 0);
    printf("sock = %d\n", sock);
    int err = SSL_set_fd(ssl, sock);
    printf("SSL_set_fd err = %d\n", err);
    char buf[2 * 1024];
    err = connect(sock, &info_addr, sizeof(info_addr));
    printf("connect err = %d\n", err);
    err = SSL_connect(ssl);
    printf("SSL_connect err = %d\n", err);
    char path[] = "users/me";
    sprintf(buf, global_req, path, sessionToken);
    int num = SSL_write(ssl, buf, sizeof global_req + sizeof path + 19);
    printf("SSL_write %d\n", num);
    num = SSL_read(ssl, buf, sizeof buf);
    printf("SSL_read %d\n", num);
    SSL_free(ssl);
    close_socket(sock);
	buf[num] = 0; printf("%s\n", buf);
    std::cmatch match;
    std::regex_search(buf, match, replayer);
    return match.str(1);
}

std::regex reurl("//([^\"]+)");
std::regex resummary("ry\":\"([^\"]+)");
std::regex reupdate("datedAt\":\"([^\"]+)");
void info(char* sessionToken, Summary& summary) {
	SSL *ssl = SSL_new(ctx);
	printf("ssl = %p\n", ssl);
	int sock = socket(PF_INET, SOCK_STREAM, 0);
	printf("sock = %d\n", sock);
    int err = SSL_set_fd(ssl, sock);
    printf("SSL_set_fd err = %d\n", err);
    char buf[1024 + 512];
    err = connect(sock, &info_addr, sizeof(info_addr));
    printf("connect err = %d\n", err);
    err = SSL_connect(ssl);
    printf("SSL_connect err = %d\n", err);
    char path[] = "classes/_GameSave";
    sprintf(buf, global_req, path, sessionToken);
    int num = SSL_write(ssl, buf, sizeof global_req + sizeof path + 19);
    printf("SSL_write %d\n", num);
    num = SSL_read(ssl, buf, sizeof buf);
    SSL_free(ssl);
    close_socket(sock);
    printf("SSL_read %d\n", num);
    buf[num] = 0;
	char* ptr = strstr(buf, "\r\n\r\n") + 4; printf("%s\n", ptr);
    std::cmatch match;
    std::regex_search(buf, match, reurl);
    summary.url = match.str(1);
    std::regex_search(buf, match, reupdate);
    summary.update = match.str(1);
    std::regex_search(buf, match, resummary);
    printf("%d\n", match.length(1));
    num = base64decode(buf + match.position(1), match.length(1), buf);
    printf("%d\n", num);
    print_struct(buf, num);
    read_nodes(buf, nodeSummary, sizeof nodeSummary / sizeof(Node), &summary);
}

zip_t* download_save(char* domain, char* buf, zip_source_t** source_argv = 0) {
    char r[] = "GET /%s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n";
    char* path = strchr(domain, '/');
    *path = '\0';
    printf("domain = %s\n", domain);
    path++;
    printf("path = %s\n", path);
    sockaddr* addr;
	if (!strcmp(domain, "rak3ffdi.tds1.tapfiles.cn")) addr = &save_addr;
	else addr = dns(domain, 80);
    int sock = socket(PF_INET, SOCK_STREAM, 0);
    printf("sock = %d\n", sock);
    int err = connect(sock, addr, sizeof(*addr));
    printf("connect err = %d\n", err);
    short length = sprintf(buf, r, path, domain);
    length = send(sock, buf, length, 0);
    printf("send length = %d\n", length);
    short end = 0;
	do {
		length = recv(sock, buf + end, save_size - end, 0);
		end += length;
		printf("recv end = %d\n", end);
	} while (length);
	printf("%s\n", buf);
	path = strstr(buf, "\r\n\r\n") + 4;
	zip_source_t* source = zip_source_buffer_create(path, buf - path + end, 0, 0);
    if (source_argv) {
	    printf("keep\n");
	zip_source_keep(source);
	*source_argv = source;
    }
    zip_t* zip = zip_open_from_source(source, 0, 0);
    return zip;
}
/*
std::string MethodSave(char* url) {
	unsigned char buf[12 * 1024];
	printf("%s\n", url);
	int outlen;
	EVP_CIPHER_CTX* cipher_ctx = EVP_CIPHER_CTX_new();

	char res[save_size];
	zip_t* zip = download_save(url, res);
	zip_file_t* zip_file;
	int len;

	zip_file = zip_fopen(zip, "gameProgress", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf, &outlen, buf + 1, len - 1);
	js_read_nodes(buf, nodeGameProgress, sizeof(nodeGameProgress) / sizeof(Node), value, env);
	napi_set_named_property(env, result, "gameProgress", value);
	
	zip_file = zip_fopen(zip, "user", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf, &outlen, buf + 1, len - 1);
	js_read_nodes(buf, nodeUser, sizeof(nodeUser) / sizeof(Node), value, env);
	napi_set_named_property(env, result, "user", value);

	zip_file = zip_fopen(zip, "settings", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf, &outlen, buf + 1, len - 1);
	js_read_nodes(buf, nodeSettings, sizeof(nodeSettings) / sizeof(Node), value, env);
	napi_set_named_property(env, result, "settings", value);

	zip_file = zip_fopen(zip, "gameRecord", 0);
	len = zip_fread(zip_file, buf, sizeof buf);
	zip_fclose(zip_file);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	EVP_DecryptUpdate(cipher_ctx, buf, &outlen, buf + 1, len - 1);
}
*/
void re8(zip_t* zip, unsigned char* buf) {
	int outlen;
	EVP_CIPHER_CTX* cipher_ctx = EVP_CIPHER_CTX_new();
	zip_int64_t index = zip_name_locate(zip, "gameProgress", 0);
	zip_file_t* zip_file = zip_fopen_index(zip, index, 0);
	int len = zip_fread(zip_file, buf, 33);
	zip_fclose(zip_file);
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
	print_struct(buf, len);
	EVP_DecryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	print_struct(buf, len);
	char* ptr = (char*) buf + 9;
	for (char i = 0; i < 5; i++)
		read_varshort(ptr);
	ptr[5] = 0;
	ptr[6] = 0;
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_EncryptInit(cipher_ctx, cipher, key, iv);
	print_struct(buf, len);
	EVP_EncryptUpdate(cipher_ctx, buf + 1, &outlen, buf + 1, len - 1);
	print_struct(buf, len);
	zip_source_t* source = zip_source_buffer(zip, buf, len, 0);
	printf("replace start\n");
	zip_file_replace(zip, index, source, 0);
	printf("replace end\n");
	EVP_CIPHER_CTX_free(cipher_ctx);
}

std::regex reid("d\":\"([^\"]+)");
std::regex recreate("At\":\"([^\"]+)");
std::regex rekey("y\":\"([^\"]+)");
std::regex retoken("n\":\"([^\"]+)");
std::regex reetag("g\":\"([^\"]+)");
void upload_save(char* sessionToken) {
	char save[save_size];
	char buf[save_size];
	std::cmatch match;



	SSL *ssl = SSL_new(ctx);
	printf("ssl = %p\n", ssl);
	int sock = socket(PF_INET, SOCK_STREAM, 0);
	printf("sock = %d\n", sock);
	int err = SSL_set_fd(ssl, sock);
	printf("SSL_set_fd err = %d\n", err);
	err = connect(sock, &info_addr, sizeof(info_addr));
	printf("connect err = %d\n", err);
	err = SSL_connect(ssl);
	printf("SSL_connect err = %d\n", err);



	char gameSave[] = "GET /1.1/classes/_GameSave HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\n\r\n";
	int num = sprintf(buf, gameSave, sessionToken);
	num = SSL_write(ssl, buf, num);
	printf("SSL_write %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = SSL_read(ssl, buf, sizeof buf);
	printf("SSL_read %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	char* ptr = strstr(buf, "\r\n\r\n") + 4;
	std::cregex_iterator iter(ptr, buf + num, reid);
	std::string oldfileId = (*iter).str(1);
	iter++;
	std::string id = (*iter).str(1);
	iter++;
	std::string userId = (*iter).str(1);
	std::cout << "fileId = " << oldfileId << ", id = " << id << ", userId = " << userId << '\n';
	std::regex_search(ptr, match, resummary);
	num = base64decode(ptr + match.position(1), match.length(1), buf);
	buf[7] = 0;
	std::string summary = base64encode(buf, num);
	std::cout << "summary = " << summary << "\n\n";



	std::regex_search(ptr, match, reurl);
	zip_source_t* source;
	zip_t* zip = download_save((char*) match.str(1).data(), buf, &source);
	re8(zip, (unsigned char*) save + sizeof save - 33);
	zip_close(zip);
	err = zip_source_open(source);
	printf("zip source open err = %d\n", err);
	short size = zip_source_read(source, save, save_size);
	zip_source_close(source);
	printf("zip source read len = %d\n", size);



	unsigned char* ubuf = (unsigned char*) buf;
	EVP_MD_CTX* md_ctx = EVP_MD_CTX_new();
	EVP_DigestInit(md_ctx, md);
	EVP_DigestUpdate(md_ctx, save, size);
	EVP_DigestFinal(md_ctx, ubuf + 1024, (unsigned int*) &num);
	EVP_MD_CTX_free(md_ctx);
	printf("md5 length = %d\n", num);
	std::string md5_base64 = base64encode(buf + 1024, 16);
	std::cout << "md5_base64 = " << md5_base64 << '\n';
	sprintf(buf, "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x", ubuf[1024], ubuf[1024 + 1], ubuf[1024 + 2], ubuf[1024 + 3], ubuf[1024 + 4], ubuf[1024 + 5], ubuf[1024 + 6], ubuf[1024 + 7], ubuf[1024 + 8], ubuf[1024 + 9], ubuf[1024 + 10], ubuf[1024 + 11], ubuf[1024 + 12], ubuf[1024 + 13], ubuf[1024 + 14], ubuf[1024 + 15]);



	char fileTokens[] = "POST /1.1/fileTokens HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nContent-Length: %d\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\n\r\n";
	char fileTokens_body[] = "{\"name\":\".save\",\"__type\":\"File\",\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"prefix\":\"gamesaves\",\"metaData\":{\"size\":%d,\"_checksum\":\"%s\",\"prefix\":\"gamesaves\"}}";
	num = sprintf(buf + 1024, fileTokens_body, userId.data(), size, buf);
	ptr = buf + sprintf(buf, fileTokens, sessionToken, num);
	append(ptr, buf + 1024, num);
	num = SSL_write(ssl, buf, ptr - buf);
	printf("SSL_write %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = SSL_read(ssl, buf, sizeof buf);
	printf("SSL_read %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	ptr = strstr(buf, "\r\n\r\n");
	std::regex_search(ptr, match, recreate);
	std::string create = match.str(1);
	std::regex_search(ptr, match, reid);
	std::string newfileId = match.str(1);
	std::regex_search(ptr, match, rekey);
	std::cout << "position = " << match.position(1) << " length = " << match.length(1) << '\n';
	std::string key = base64encode(ptr + match.position(1), match.length(1));
	std::regex_search(ptr, match, retoken);
	std::string token = match.str(1);
	std::cout << "createAt = " << create << ", base64 key = " << key << ", fileId = " << newfileId << ", token = " << token << "\n\n";

	

	int upload_sock = socket(PF_INET, SOCK_STREAM, 0);
	printf("upload_sock = %d\n", upload_sock);
	err = connect(upload_sock, &upload_addr, sizeof(upload_addr));
	printf("connect err = %d\n", err);



	char upload1[] = "POST /buckets/rAK3Ffdi/objects/%s/uploads HTTP/1.1\r\nAuthorization: UpToken %s\r\nHost: upload.qiniup.com\r\n\r\n";
	num = sprintf(buf, upload1, key.data(), token.data());
	num = send(upload_sock, buf, num, 0);
	printf("send %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = recv(upload_sock, buf, sizeof buf, 0);
	printf("recv %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	std::regex_search(buf, match, reid);
	std::string uploadId = match.str(1);
	std::cout << "uploadId = " << uploadId << "\n\n";



	char upload2[] = "PUT /buckets/rAK3Ffdi/objects/%s/uploads/%s/1 HTTP/1.1\r\nHost: upload.qiniup.com\r\nAuthorization: UpToken %s\r\nContent-Md5: %s\r\nContent-Length: %d\r\n\r\n";
	ptr = buf + sprintf(buf, upload2, key.data(), uploadId.data(), token.data(), md5_base64.data(), size);
	append(ptr, save, size);
	num = send(upload_sock, buf, ptr - buf, 0);
	printf("send %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = recv(upload_sock, buf, sizeof buf, 0);
	printf("recv %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	std::regex_search(buf, match, reetag);
	std::string etag = match.str(1);
	std::cout << "etag = " << etag << "\n\n";



	char upload3[] = "POST /buckets/rAK3Ffdi/objects/%s/uploads/%s HTTP/1.1\r\nHost: upload.qiniup.com\r\nAuthorization: UpToken %s\r\nContent-Length: %d\r\nConnection: close\r\n\r\n";
	char upload3_body[] = "{\"parts\":[{\"partNumber\":1,\"etag\":\"%s\"}]}";
	num = sprintf(buf + 1024, upload3_body, etag.data());
	ptr = buf + sprintf(buf, upload3, key.data(), uploadId.data(), token.data(), num);
	append(ptr, buf + 1024, num);
	num = send(upload_sock, buf, ptr - buf, 0);
	printf("send %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = recv(upload_sock, buf, sizeof buf, 0);
	printf("recv %d\n", num);
	buf[num] = 0; printf("%s\n\n", buf);


	close_socket(upload_sock);




	char fileCallback[] = "POST /1.1/fileCallback HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nContent-Length: %d\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\n\r\n";
	char fileCallback_body[] = "{\"result\":true,\"token\":\"%s\"}";
	num = sprintf(buf + 1024, fileCallback_body, token.data());
	ptr = buf + sprintf(buf, fileCallback, sessionToken, num);
	append(ptr, buf + 1024, num);
	num = SSL_write(ssl, buf, ptr - buf);
	printf("SSL_write %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = SSL_read(ssl, buf, sizeof buf);
	printf("SSL_read %d\n", num);
	buf[num] = 0; printf("%s\n\n", buf);



	char putsave[] = "PUT /1.1/classes/_GameSave/%s? HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nContent-Length: %d\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\n\r\n";
	char putsave_body[] = "{\"summary\":\"%s\",\"modifiedAt\":{\"__type\":\"Date\",\"iso\":\"%s\"},\"gameFile\":{\"__type\":\"Pointer\",\"className\":\"_File\",\"objectId\":\"%s\"},\"ACL\":{\"%s\":{\"read\":true,\"write\":true}},\"user\":{\"__type\":\"Pointer\",\"className\":\"_User\",\"objectId\":\"%s\"}}";
	num = sprintf(buf + 1024, putsave_body, summary.data(), create.data(), newfileId.data(), userId.data(), userId.data());
	ptr = buf + sprintf(buf, putsave, id.data(), sessionToken, num);
	append(ptr, buf + 1024, num);
	num = SSL_write(ssl, buf, ptr - buf);
	printf("SSL_write %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = SSL_read(ssl, buf, sizeof buf);
	printf("SSL_read %d\n", num);
	buf[num] = 0; printf("%s\n\n", buf);



	char deletefile[] = "DELETE /1.1/files/%s HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\nConnection: close\r\n\r\n";
	num = sprintf(buf, deletefile, oldfileId.data(), sessionToken);
	num = SSL_write(ssl, buf, num);
	printf("SSL_write %d\n", num);
	buf[num] = 0; printf("%s\n", buf);
	num = SSL_read(ssl, buf, sizeof buf);
	printf("SSL_read %d\n", num);
	buf[num] = 0; printf("%s\n\n", buf);



	SSL_free(ssl);
	close_socket(sock);
}

void parseGameRecord(zip_t* zip, SongLevel* song_result) {
    unsigned char buf[10 * 1024];
    zip_file_t* zip_file = zip_fopen(zip, "gameRecord", 0);
    int len = zip_fread(zip_file, buf, sizeof buf);
    zip_fclose(zip_file);
    zip_discard(zip);
    printf("zip_read length = %d\n", len);
	EVP_CIPHER_CTX* cipher_ctx = EVP_CIPHER_CTX_new();
	EVP_DecryptInit(cipher_ctx, cipher, key, iv);
    int err = EVP_DecryptUpdate(cipher_ctx, buf + 1, &len, buf + 1, len - 1);
	EVP_CIPHER_CTX_free(cipher_ctx);
    printf("decrypt err = %d\n", err);
    printf("decrypt length = %d\n", len);
    song_result[0].difficulty = 0;
    char* pos = (char*) buf + 1;
    unsigned char song_len = read_varshort(pos);
    printf("song length = %d\n", song_len);
    std::vector<SongLevel> songlevels(song_len);
    for (int i = 0; i < song_len; i++) {
	std::string id = read_string(pos, 2);
	char* end = pos + *pos + 1;
	pos++;
	char len = *pos++;
	char fc = *pos++;
	std::array<float, 4> song_difficulty = difficulty.at(id);
	for (char level = 0; level < 4; level++) {
	    if (getbit(len, level)) {
		SongLevel songlevel;
		songlevel.score = *(int*) pos;
		pos += 4;
		songlevel.acc = *(float*) pos;
		pos += 4;
		if (songlevel.acc < 55)
		    continue;
		songlevel.id = id;
		songlevel.level = level;
		songlevel.difficulty = song_difficulty[level];
		songlevel.fc = getbit(fc, level);
		songlevel.rks = (songlevel.acc - 55) / 45;
		songlevel.rks *= songlevel.rks * songlevel.difficulty;
		songlevels.push_back(songlevel);
		if (songlevel.score == 1000000 && songlevel.difficulty > song_result[0].difficulty)
			song_result[0] = songlevel;
	    }
	}
	pos = end;
    }
    std::partial_sort_copy(songlevels.begin(), songlevels.end(), song_result + 1, song_result + 20, comp);
}
