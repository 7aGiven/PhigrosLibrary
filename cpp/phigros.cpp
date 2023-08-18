#include <fstream>
#include <iostream>
#include <openssl/ssl.h>
#include <regex>
#include <typeinfo>
#include <unordered_map>
#include <zip.h>
#ifdef __linux__
  #include <netdb.h>
  #include <unistd.h>
#elif defined _WIN32
  #include <ws2tcpip.h>
  #pragma comment(lib, "ws2_32")
#endif

void print_struct(void* vptr, int size) {
	unsigned char* ptr = (unsigned char*) vptr;
	for (short i = 0; i < size; i++)
		printf("%02X", ptr[i]);
	printf("\n");
}

const unsigned char key[] = {0xe8,0x96,0x9a,0xd2,0xa5,0x40,0x25,0x9b,0x97,0x91,0x90,0x8b,0x88,0xe6,0xbf,0x03,0x1e,0x6d,0x21,0x95,0x6e,0xfa,0xd6,0x8a,0x50,0xdd,0x55,0xd6,0x7a,0xb0,0x92,0x4b};
const unsigned char iv[] = {0x2a,0x4f,0xf0,0x8a,0xc8,0x0d,0x63,0x07,0x00,0x57,0xc5,0x95,0x18,0xc8,0x32,0x53};
const char global_req1[] = "GET /1.1/classes/_GameSave HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: ";
const char global_req2[] = "\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\nConnection: close\r\n\r\n";
const std::string base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
SSL_CTX *ctx = SSL_CTX_new(TLS_client_method());

void base64decode(const char* ptr, char size, char* result) {
	int** presult = (int**) &result;
	char tmp;
	for (int i = 0; i < size / 4; i++) {
		**presult = base64.find(*ptr) << 18 ^ base64.find(*(ptr+1)) << 12 ^ base64.find(*(ptr+2)) << 6 ^ base64.find(*(ptr+3));
		tmp = *result;
		*result = *(result+2);
		*(result+2) = tmp;
		ptr += 4;
		result += 3;
	}
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

unsigned char read_varshort(char*& pos) {
    if (pos[0] > 0) {
        pos++;
	return *(pos - 1);
    } else {
        pos += 2;
	return *(pos -2);
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

static void read_map(void* vbuf, void* vobject) {
	char* buf = (char*) vbuf;
	char* object = (char*) vobject;
	unsigned char length = read_varshort(buf);
	for (; length > 0; length--) {
		read_string(buf, 2);
		
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

bool comp(SongLevel& o1, SongLevel& o2) {
    return o1.rks > o2.rks;
}

std::unordered_map<std::string, float*> difficulty;
void read_difficulty() {
    std::ifstream f("difficulty.csv");
    std::string line;
    while (std::getline(f, line)) {
	int indexs[4];
	int index = 0;
	for (int i = 0; i < 4; i++) {
	    index = line.find(',', index + 1);
	    indexs[i] = index;
	}
	std::string id = line.substr(0, indexs[0]);
	int len = 3;
	if (indexs[3] == -1)
	    len--;
	float* floats = new float[4];
	floats[3] = 0;
	for (int i = 0; i < len; i++)
	    floats[i] = std::stof(line.substr(indexs[i] + 1, indexs[i + 1] - indexs[i] - 1), 0);
        floats[len] = std::stof(line.substr(indexs[len] + 1, line.size() - indexs[len] - 1));
	difficulty[id] = floats;
    }
    f.close();
}

struct sockaddr* dns(char* domain, short port) {
    struct addrinfo hint = {0, AF_INET, SOCK_STREAM};
    struct addrinfo* addrs;
    getaddrinfo(domain, 0, &hint, &addrs);
    freeaddrinfo(addrs);
    struct sockaddr* addr = addrs->ai_addr;
    char* pport = (char*) &port;
    addr->sa_data[0] = pport[1];
    addr->sa_data[1] = pport[0];
    printf("%d.%d.%d.%d\n", (unsigned char)addr->sa_data[2], (unsigned char)addr->sa_data[3], (unsigned char)addr->sa_data[4], (unsigned char)addr->sa_data[5]);
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
}

void append(char*& dest, const char* src, unsigned char size) {
	memcpy(dest, src, size);
	dest += size;
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
    char buf[1536];
    err = connect(sock, &info_addr, sizeof(info_addr));
    printf("connect err = %d\n", err);
    err = SSL_connect(ssl);
    printf("SSL_connect err = %d\n", err);
    char* ptr = buf;
    append(ptr, global_req1, sizeof global_req1 - 1);
    append(ptr, sessionToken, 25);
    append(ptr, global_req2, sizeof global_req2 - 1);
    int num = SSL_write(ssl, buf, sizeof global_req1 + sizeof global_req2 + 23);
    printf("SSL_write %d\n", num);
    num = SSL_read(ssl, buf, sizeof buf);
#ifdef __linux__
	close(sock);
#elif defined _WIN32
	closesocket(sock);
#endif
    printf("SSL_read %d\n", num);
    buf[num] = 0;
    printf("%s\n", buf);
    std::cmatch results;
    std::regex_search(buf, results, reurl);
    summary.url = results.str(1);
    std::regex_search(buf, results, reupdate);
    summary.update = results.str(1);
    std::regex_search(buf, results, resummary);
    summary.avatar = results.str(1);
    base64decode(summary.avatar.data(), summary.avatar.length(), buf);
    read_nodes(buf, nodeSummary, sizeof(nodeSummary) / sizeof(Node), (char*) &summary);
}

zip_t* download_save(char* domain, char* res) {
    char r[] = "GET /%s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n";
    int index;
    for (index = 0;; index++) {
        if (domain[index] == '/')
            break;
    }
    domain[index++] = '\0';
    printf("domain = %s\n", domain);
    char *path = domain + index;
    printf("path = %s\n", path);
    sockaddr* addr = dns(domain, 80);
    int sock = socket(PF_INET, SOCK_STREAM, 0);
    printf("sock = %d\n", sock);
    int err = connect(sock, addr, sizeof(*addr));
    printf("connect err = %d\n", err);
    char req[128];
    int length = sprintf(req, r, path, domain);
    length = send(sock, req, length, 0);
    printf("send length = %d\n", length);
    int end = 0;
    do {
        length = recv(sock, res + end, 14 * 1024 - end, 0);
	end += length;
        printf("recv end = %d\n", end);
    } while(length);
    char *start = strstr(res, "\r\n\r\n") + 4;
    for (int i = 0; i < 2720; i++)
        printf("%c", res[i]);
    printf("\n");
    zip_source_t *source = zip_source_buffer_create(start, res + end - start, 0, 0);
    zip_t* zip = zip_open_from_source(source, 0, 0);
    return zip;
}

void get_save(char* url, Save& save) {
EVP_CIPHER_CTX* cipher_ctx = EVP_CIPHER_CTX_new();
	unsigned char result[12 * 1024];
	int outlen;
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, EVP_aes_256_cbc(), key, iv);
	char res[14 * 1024];
	zip_t* zip = download_save(url, res);
	zip_file_t* zip_file;
	unsigned char buf[12 * 1024];
	int len;
	
	zip_file = zip_fopen(zip, "gameProgress", 0);
	zip_fread(zip_file, buf, 1);
	len = zip_fread(zip_file, buf, 12 * 1024);
	EVP_DecryptUpdate(cipher_ctx, result, &outlen, buf, len);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, EVP_aes_256_cbc(), key, iv);
	read_nodes(result, nodeGameProgress, sizeof(nodeGameProgress) / sizeof(Node), &save.gameProgress);
	
	zip_file = zip_fopen(zip, "user", 0);
	zip_fread(zip_file, buf, 1);
	len = zip_fread(zip_file, buf, 12 * 1024);
	EVP_DecryptUpdate(cipher_ctx, result, &outlen, buf, len);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, EVP_aes_256_cbc(), key, iv);
	read_nodes(result, nodeUser, sizeof(nodeUser) / sizeof(Node), &save.user);

	zip_file = zip_fopen(zip, "settings", 0);
	zip_fread(zip_file, buf, 1);
	len = zip_fread(zip_file, buf, 12 * 1024);
	EVP_DecryptUpdate(cipher_ctx, result, &outlen, buf, len);
	EVP_CIPHER_CTX_reset(cipher_ctx);
	EVP_DecryptInit(cipher_ctx, EVP_aes_256_cbc(), key, iv);
	read_nodes(result, nodeSettings, sizeof(nodeSettings) / sizeof(Node), (char*) &save.settings);
}


std::regex reid("d\":\"([^\"]+)");
void upload_save(char* sessionToken, short size) {
	char buf[14 * 1024];
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
    char* ptr = buf;
    append(ptr, global_req1, sizeof global_req1 - 1);
    append(ptr, sessionToken, 25);
    append(ptr, global_req2, sizeof global_req2 - 1);
    int num = SSL_write(ssl, buf, sizeof global_req1 + sizeof global_req2 + 23);
    printf("SSL_write %d\n", num);
    num = SSL_read(ssl, buf, sizeof buf);
    printf("SSL_read %d\n", num);
    buf[num] = 0; printf("%s\n", buf);

    ptr = buf;
    std::cmatch match;
    std::cregex_iterator iter(buf, buf + num, reid);
    std::string fileId = (*iter).str(1);
    iter++;
    std::string id = (*iter).str(1);
    iter++;
    std::string userId = (*iter).str(1);
    std::cout << "fileId = " << fileId << ", id = " << id << ", userId = " << userId << '\n';

    std::string size_str = std::to_string(size);
	char pigeon_host[] = "rak3ffdi.cloud.tds1.tapapis.cn";
	char header1[] = "X-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: ";
	char header2[] = "\r\nUser-Agent: LeanCloud-CSharp-SDK/1.0.3\r\nAccept: application/json\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\n";
	char header3[] = "Content-Type: application/json\r\n";
	char header4[] = "\r\nConnection: close\r\n\r\n";
	char fileTokens_body1[] = "{\"name\":\".save\",\"__type\":\"File\",\"ACL\":{\"";
	char fileTokens_body2[] = "\":{\"read\":true,\"write\":true}},\"prefix\":\"gamesaves\",\"metaData\":{\"size\":";
	char fileTokens_body3[] = ",\"_checksum\":\"";
	char fileTokens_body4[] = "\",\"prefix\":\"gamesaves\"}}";
	char md5[] = "12345678901234567890123456789012";
	char body_len = sizeof fileTokens_body1 + sizeof fileTokens_body2 + sizeof fileTokens_body3 + sizeof fileTokens_body4 + userId.length() + size_str.length() + 32 - 4;
	std::string body_len_str = std::to_string(body_len);
	ptr = buf;
	char* body = buf + 1024;
	append(ptr, "POST", 4);
	append(ptr, " /1.1/", 6);
	append(ptr, "fileTokens", 10);
	append(ptr, " HTTP/1.1\r\n", 11);
	append(ptr, header1, sizeof(header1) - 1);
	append(ptr, sessionToken, 25);
	append(ptr, header2, sizeof(header2) - 1);
	append(ptr, header3, sizeof(header3) - 1);
	append(ptr, "Content-Length: ", 16);
	append(ptr, body_len_str.data(), body_len_str.length());
	append(ptr, "\r\nHost: ", 8);
	append(ptr, pigeon_host, sizeof pigeon_host - 1);
	append(ptr, header4, sizeof header4 - 1);
	append(ptr, fileTokens_body1, sizeof fileTokens_body1 - 1);
	append(ptr, userId.data(), userId.length());
	append(ptr, fileTokens_body2, sizeof fileTokens_body2 - 1);
	append(ptr, size_str.data(), size_str.length());
	append(ptr, fileTokens_body3, sizeof fileTokens_body3 - 1);
	append(ptr, md5, sizeof md5 - 1);
	append(ptr, fileTokens_body4, sizeof fileTokens_body4 - 1);
	*ptr = 0;
	printf("%s\n", buf);

	char upload11[] = "";
	ptr = buf;

}

SongLevel* parseGameRecord(zip_t* zip, std::unordered_map<std::string, float*>& difficulty) {
EVP_CIPHER_CTX* cipher_ctx = EVP_CIPHER_CTX_new();
    zip_file_t* zip_file = zip_fopen(zip, "gameRecord", 0);
    unsigned char gameRecord[8 * 1024];
    zip_fread(zip_file, gameRecord, 1);
    char version = gameRecord[0];
    int length = zip_fread(zip_file, gameRecord, 8 * 1024);
    printf("zip_read length = %d\n", length);
    EVP_DecryptInit(cipher_ctx, EVP_aes_256_cbc(), key, iv);
    unsigned char result[8 * 1024];
    int outlen;
    int err = EVP_DecryptUpdate(cipher_ctx, result, &outlen, gameRecord, length);
    printf("decrypt err = %d\n", err);
    printf("decrypt length = %d\n", outlen);
    for (int i = 0; i < 512; i++)
	printf("%c", result[i]);
    printf("\n");
    char* pos = (char*) result;
    int song_length = read_varshort(pos);
    printf("length = %d\n", length);
    std::vector<SongLevel> songlevels;
    for (int i = 0; i < song_length; i++) {
	char* end = pos + *pos + 1;
	end = end + *end + 1;
	std::string id = read_string(pos, 2);
	pos++;
	char len = *pos;
	pos++;
	char fc = *pos;
	pos++;
	for (int level = 0; level < 4; level++) {
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
		songlevel.difficulty = difficulty[id][level];
		songlevel.fc = getbit(fc, level);
		songlevel.rks = (songlevel.acc - 55) / 45;
		songlevel.rks = songlevel.rks * songlevel.rks * songlevel.difficulty;
		songlevels.push_back(songlevel);
	    }
	}
	pos = end;
    }
    SongLevel* song_result = new SongLevel[20];
    song_result[0] = {};
    std::partial_sort_copy(songlevels.begin(), songlevels.end(), song_result + 1, song_result + 20, comp);
    for (int i = 0; i < songlevels.size(); i++) {
	if (songlevels[i].score == 1000000 && songlevels[i].difficulty > song_result[0].difficulty)
	    song_result[0] = songlevels[i];
    }
    return song_result;
}
