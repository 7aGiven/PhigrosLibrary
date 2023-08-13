#include <fstream>
#include <iostream>
#include <openssl/ssl.h>
#include <regex>
#include <typeinfo>
#include <unordered_map>
#include <zip.h>
#ifdef __linux__
  #include <netdb.h>
#elif defined _WIN32
  #include <ws2tcpip.h>
  #pragma comment(lib, "ws2_32")
#endif

void print_struct(void* pptr, int size) {
	unsigned char* ptr = (unsigned char*) pptr;
	for (int i = 0; i < size; i++) {
		printf("%02X ", ptr[i]);
	}
	printf("\n");
}

const unsigned char key[] = {0xe8,0x96,0x9a,0xd2,0xa5,0x40,0x25,0x9b,0x97,0x91,0x90,0x8b,0x88,0xe6,0xbf,0x03,0x1e,0x6d,0x21,0x95,0x6e,0xfa,0xd6,0x8a,0x50,0xdd,0x55,0xd6,0x7a,0xb0,0x92,0x4b};
const unsigned char iv[] = {0x2a,0x4f,0xf0,0x8a,0xc8,0x0d,0x63,0x07,0x00,0x57,0xc5,0x95,0x18,0xc8,0x32,0x53};
const char global_req[] = "GET /1.1/classes/_GameSave HTTP/1.1\r\nX-LC-Key: Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0\r\nX-LC-Session: %s\r\nUser-Agent: LeanCloud-CSharp-SDK/1.0.3\r\nAccept: application/json\r\nX-LC-Id: rAK3FfdieFob2Nn8Am\r\nHost: rak3ffdi.cloud.tds1.tapapis.cn\r\nConnection: close\r\n\r\n";
const std::string base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

void base64decode(const char* ptr, char* result) {
	int** presult = (int**) &result;
	char tmp;
	for (int i = 0; i < 80 / 4; i++) {
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

void setbit(char* b, char v) {
	*b = v;
}

int read_varshort(char*& pos) {
    if (pos[0] > 0) {
        pos++;
	return *(pos - 1);
    } else {
        pos = pos + 2;
	return *(pos -2) & 0b01111111  ^ *(pos - 1) << 8;
    }
}

std::string read_string(char*& pos, char offset) {
    int len = *pos;
    pos += len + 1;
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
	short offset;
};

void read_nodes(char* buf, Node nodes[], char size, char* object) {
	for (char i = 0; i < size; i++) {
		char index = 0;
		if (nodes[i].type == "bool") {
			*(object + nodes[i].offset) = getbit(*buf, index);
			index++;
			continue;
		}
		index = 0;
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
};

Node nodeSummary[] = {
	{"char", offsetof(Summary, saveVersion)},
	{"short", offsetof(Summary, challengeModeRank)},
	{"float", offsetof(Summary, rks)},
	{"char", offsetof(Summary, gameVersion)},
	{"string", offsetof(Summary, avatar)},
	{"short12", offsetof(Summary, progress)}
};

struct GameKey {
	std::string key;
	char readCollection;
	bool unlockSingle;
	char collection;
	bool illustration;
	bool avatar;
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
	{"bool", offsetof(GameProgress, isFirstRun)},
	{"bool", offsetof(GameProgress, legacyChapterFinished)},
	{"bool", offsetof(GameProgress, alreadyShowCollectionTip)},
	{"bool", offsetof(GameProgress, alreadyShowAutoUnlockINTip)},
	{"string", offsetof(GameProgress, completed)},
	{"char", offsetof(GameProgress, songUpdateInfo)},
	{"short", offsetof(GameProgress, challengeModeRank)},
	{"short5", offsetof(GameProgress, money)},
	{"char", offsetof(GameProgress, unlockFlagOfSpasmodic)},
	{"char", offsetof(GameProgress, unlockFlagOfIgallta)},
	{"char", offsetof(GameProgress, unlockFlagOfRrharil)},
	{"char", offsetof(GameProgress, flagOfSongRecordKey)},
	{"bool", offsetof(GameProgress, chapter8UnlockBegin)},
	{"bool", offsetof(GameProgress, chapter8UnlockSecondPhase)},
	{"bool", offsetof(GameProgress, chapter8Passed)},
	{"char", offsetof(GameProgress, chapter8SongUnlocked)}
};

struct User {
	bool showPlayerId;
	std::string selfIntro;
	std::string avatar;
	std::string background;
};

Node nodeUser[] = {
	{"bool", offsetof(User, showPlayerId)},
	{"string", offsetof(User, selfIntro)},
	{"string", offsetof(User, avatar)},
	{"string", offsetof(User, background)}
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
	{"bool", offsetof(Settings, chordSupport)},
	{"bool", offsetof(Settings, fcAPIndicator)},
	{"bool", offsetof(Settings, enableHitSound)},
	{"bool", offsetof(Settings, lowResolutionMode)},
	{"string", offsetof(Settings, deviceName)},
	{"float", offsetof(Settings, bright)},
	{"float", offsetof(Settings, musicVolume)},
	{"float", offsetof(Settings, effectVolume)},
	{"float", offsetof(Settings, hitSoundVolume)},
	{"float", offsetof(Settings, soundOffset)},
	{"float", offsetof(Settings, noteScale)}
};

struct Save {
	GameProgress gameProgress;
	User user;
	Settings settings;
};

Save readSave(zip_t* zip) {
	zip_file_t* zip_file;
	char buf[8 * 1024];
	int len;
	Save save;
	zip_file = zip_fopen(zip, "gameProgress", 0);
	len = zip_fread(zip_file, buf, 8 * 1024);
	read_nodes(buf, nodeGameProgress, sizeof(nodeGameProgress) / sizeof(Node), (char*) &save.gameProgress);
	zip_file = zip_fopen(zip, "user", 0);
	len = zip_fread(zip_file, buf, 8 * 1024);
	read_nodes(buf, nodeUser, sizeof(nodeUser) / sizeof(Node), (char*) &save.user);
	zip_file = zip_fopen(zip, "settings", 0);
	len = zip_fread(zip_file, buf, 8 * 1024);
	read_nodes(buf, nodeSettings, sizeof(nodeSettings) / sizeof(Node), (char*) &save.settings);
	return save;
}

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
    addr->sa_data[0] = port / 256;
    addr->sa_data[1] = port % 256;
    printf("%d.%d.%d.%d\n", (unsigned char)addr->sa_data[2], (unsigned char)addr->sa_data[3], (unsigned char)addr->sa_data[4], (unsigned char)addr->sa_data[5]);
    return addr;
}

void info(char* sessionToken, Summary& summary) {
    std::regex reurl("//([^\"]+)");
    std::regex resummary("ry\":\"([^\"]+)");
    SSL_CTX *ctx = SSL_CTX_new(TLS_client_method());
    printf("ctx = %p\n", ctx);
    SSL *ssl = SSL_new(ctx);
    printf("ssl = %p\n", ssl);
    int sock = socket(PF_INET, SOCK_STREAM, 0);
    printf("sock = %d\n", sock);
    int err = SSL_set_fd(ssl, sock);
    printf("SSL_set_fd err = %d\n", err);
    char domain[] = "rak3ffdi.cloud.tds1.tapapis.cn";
    struct sockaddr* addr = dns(domain, 443);
    char res[2048];
    err = connect(sock, addr, sizeof(*addr));
    printf("connect err = %d\n", err);
    err = SSL_connect(ssl);
    printf("SSL_connect err = %d\n", err);
    char req[sizeof global_req + 23];
    sprintf(req, global_req, sessionToken);
    int num = SSL_write(ssl, req, sizeof req - 1);
    printf("write %d\n", num);
    num = SSL_read(ssl, &res, 2048);
    printf("%d\n", num);
    res[num] = 0;
    printf("%s\n", res);
    std::cmatch results;
    std::regex_search(res, results, reurl);
    summary.url = results.str(1);
    std::regex_search(res, results, resummary);
    summary.avatar = results.str(1);
    char buf[summary.avatar.length() / 4 * 3];
    base64decode(summary.avatar.data(), buf);
    read_nodes(buf, nodeSummary, sizeof(nodeSummary) / sizeof(Node), (char*) &summary);
}

zip_t* download_save(char* domain) {
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
    char res[12 * 1024];
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
        length = recv(sock, res + end, 12 * 1024 - end, 0);
	end += length;
        printf("recv end = %d\n", end);
    } while(length);
    char *start = strstr(res, "\r\n\r\n") + 4;
    for (int i = 0; i < 512; i++)
        printf("%c", start[i]);
    printf("\n");
    zip_source_t *source = zip_source_buffer_create(start, res + end - start, 0, 0);
    zip_t* zip = zip_open_from_source(source, 0, 0);
    return zip;
}

void save_object() {
	
}

SongLevel* parseGameRecord(zip_t* zip, std::unordered_map<std::string, float*>& difficulty) {
    zip_file_t* zip_file = zip_fopen(zip, "gameRecord", 0);
    unsigned char gameRecord[8 * 1024];
    zip_fread(zip_file, gameRecord, 1);
    char version = gameRecord[0];
    int length = zip_fread(zip_file, gameRecord, 8 * 1024);
    printf("zip_read length = %d\n", length);
    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    EVP_DecryptInit(ctx, EVP_aes_256_cbc(), key, iv);
    unsigned char result[8 * 1024];
    int outlen;
    int err = EVP_DecryptUpdate(ctx, result, &outlen, gameRecord, length);
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
