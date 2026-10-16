import base64
import io
import json
import struct
from types import GenericAlias
import zipfile
from Crypto.Cipher import AES
from Crypto.Util import Padding

class Buffer:
    def __init__(self, data):
        self.data = data
        self.pos = 0

class u8:
    def read(buf):
        buf.pos += 1
        return buf.data[buf.pos-1]
class u16:
    def read(buf):
        buf.pos += 2
        return buf.data[buf.pos-2] ^ buf.data[buf.pos-1] << 8
class u32:
    def read(buf):
        buf.pos += 4
        return buf.data[buf.pos-4] ^ buf.data[buf.pos-3] << 8 ^ buf.data[buf.pos-2] << 16 ^ buf.data[buf.pos-1] << 24
class varshort:
    def read(buf):
        b = buf.data[buf.pos]
        if b < 128:
            buf.pos += 1
            return b
        else:
            buf.pos += 2
            return b & 0x7F ^ buf.data[buf.pos-1] << 7
class GameRecord:
    def read(buf):
        obj = {}
        for i in range(varshort.read(buf)):
            len = varshort.read(buf)
            key = buf.data[buf.pos:buf.pos+len-2].decode()
            buf.pos += len

            next = buf.pos + buf.data[buf.pos] + 1
            buf.pos += 1

            exist = u8.read(buf)
            fc = u8.read(buf)
            array = []
            for level in range(4):
                if exist >> level & 1:
                    array.append([u32.read(buf),deserialize(float, buf),bool(fc >> level & 1)])
                else:
                    array.append(None)
            obj[key] = array

            buf.pos = next
        return obj

def deserialize(proto, buf):
    if proto == float:
        buf.pos += 4
        return struct.unpack_from("f", buf.data, buf.pos-4)[0]
    if proto == str:
        len = varshort.read(buf)
        buf.pos += len
        return buf.data[buf.pos-len:buf.pos].decode()
    if proto in {u8, u16, varshort}:
        return proto.read(buf)
    if isinstance(proto, GenericAlias):
        if proto.__origin__ == dict:
            obj = {}
            for i in range(varshort.read(buf)):
                key = deserialize(str, buf)
                next = buf.pos + buf.data[buf.pos] + 1
                buf.pos += 1
                obj[key] = deserialize(proto.__args__[0], buf)
                buf.pos = next
            return obj
        if proto.__origin__ == tuple:
            array = []
            for i in range(proto.__args__[0]):
                array.append(deserialize(proto.__args__[1], buf))
            return array
        if proto.__origin__ == list:
            exist = u8.read(buf)
            array = []
            for i in range(proto.__args__[0]):
                if exist >> i & 1:
                    array.append(deserialize(proto.__args__[1], buf))
                else:
                    array.append(None)
            return array
        raise "Unknown Type"

    obj = {}
    bit = 0
    for key, type in proto.__annotations__.items():
        if type == bool:
            obj[key] = bool(buf.data[buf.pos] >> bit & 1)
            bit += 1
            continue
        if bit:
            bit = 0
            buf.pos += 1
        obj[key] = deserialize(type, buf)
    return obj



class Summary:
	saveVersion: u8
	challengeModeRank: u16
	rankingScore: float
	gameVersion: varshort
	avatar: str
	progress: tuple[12, u16]

class GameKey1:
    keys: dict[list[5, u8]]
    lanotaReadKeys: u8

class GameKey2:
    camelliaReadKey: bool

class GameProgress1:
    isFirstRun: bool
    legacyChapterFinished: bool
    alreadyShowCollectionTip: bool
    alreadyShowAutoUnlockINTip: bool
    completed: str
    songUpdateInfo: u8
    challengeModeRank: u16
    money: tuple[5, varshort]
    unlockFlagOfSpasmodic: u8
    unlockFlagOfIgallta: u8
    unlockFlagOfRrharil: u8
    flagOfSongRecordKey: u8

class GameProgress2:
	randomVersionUnlocked: u8

class GameProgress3:
	chapter8UnlockBegin: bool
	chapter8UnlockSecondPhase: bool
	chapter8Passed: bool
	chapter8SongUnlocked: u8

class User:
    showPlayerId: bool
    selfIntro: str
    avatar: str
    background: str

class Settings:
    chordSupport: bool
    fcAPIndicator: bool
    enableHitSound: bool
    lowResolutionMode: bool
    deviceName: str
    bright: float
    musicVolume: float
    effectVolume: float
    hitSoundVolume: float
    soundOffset: float
    noteScale: float

key = base64.b64decode("6Jaa0qVAJZuXkZCLiOa/Ax5tIZVu+taKUN1V1nqwkks=")
iv = base64.b64decode("Kk/wisgNYwcAV8WVGMgyUw==")

def decrypt(save):
    save = AES.new(key, AES.MODE_CBC, iv).decrypt(save)
    return Padding.unpad(save, AES.block_size)

class Save:
    def __init__(self, byte):
        self.zip = byte
    def __enter__(self):
        self.zip = zipfile.ZipFile(io.BytesIO(self.zip))
        return self
    def __exit__(self, exc_type, exc_value, traceback):
        self.zip.close()
    def keys(self):
        return {"gameRecord","gameKey","gameProgress","user","settings"}
    def buf(self, key):
        with self.zip.open(key) as f:
            version = f.read(1)[0]
            buf = f.read()
        return version, Buffer(decrypt(buf))
    def __getitem__(self, key):
        if key == "gameRecord":
            version, buf = self.buf(key)
            return GameRecord.read(buf)
        elif key == "gameKey":
            version, buf = self.buf(key)
            obj = deserialize(GameKey1, buf)
            if version >= 2:
                obj.update(deserialize(GameKey2, buf))
            return obj
        elif key == "gameProgress":
            version, buf = self.buf(key)
            obj = deserialize(GameProgress1, buf)
            if version >= 2:
                obj.update(deserialize(GameProgress2, buf))
                if version >= 3:
                    obj.update(deserialize(GameProgress3, buf))
            return obj
        elif key == "user":
            version, buf = self.buf(key)
            return deserialize(User, buf)
        elif key == "settings":
            version, buf = self.buf(key)
            return deserialize(Settings, buf)
        else:
            raise "zip not include file"



global_headers = {
    "X-LC-Id": "rAK3FfdieFob2Nn8Am",
    "X-LC-Key": "Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0",
    "User-Agent": "LeanCloud-CSharp-SDK/1.0.3",
    "Accept": "application/json"
}

difficulty = {}

def parse_b19(gameRecord):
    records = []
    for songId, record in gameRecord.items():
        for level in range(4):
            if record[level] != None:
                obj = {"songId":songId,"level":level,"difficulty":difficulty[songId][level],"score":record[level][0],"acc":record[level][1],"fc":record[level][2]}
                obj["rks"] = obj["difficulty"] * ((obj["acc"] - 55) / 45) ** 2
                records.append(obj)
    records.sort(key=lambda x:x["rks"], reverse=True)
    b19 = [max(filter(lambda x:x["score"] == 1000000, records), key=lambda x:x["difficulty"])]
    b19.extend(records[:19])
    return b19

class B19Class:
    def __init__(self, client):
        self.client = client
    
    def read_difficulty(self, path):
        difficulty.clear()
        with open(path,encoding="UTF-8") as f:
            lines = f.readlines()
        for line in lines:
            line = line[:-1].split("\t")
            diff = []
            for i in range(1, len(line)):
                diff.append(float(line[i]))
            difficulty[line[0]] = diff

    async def get_playerId(self, sessionToken):
        headers = global_headers.copy()
        headers["X-LC-Session"] = sessionToken
        async with self.client.get("https://rak3ffdi.cloud.tds1.tapapis.cn/1.1/users/me", headers=headers) as response:
            result = (await response.json())["nickname"]
        return result

    async def get_summary(self, sessionToken):
        headers = global_headers.copy()
        headers["X-LC-Session"] = sessionToken
        async with self.client.get("https://rak3ffdi.cloud.tds1.tapapis.cn/1.1/users/me", headers=headers) as response:
            objectId = (await response.json())["objectId"]
        params = {
            "where": json.dumps({"user":{"__type":"Pointer","className":"_User","objectId":objectId}}),
            "order": "-updatedAt",
            "limit": 1
        }
        async with self.client.get("https://rak3ffdi.cloud.tds1.tapapis.cn/1.1/classes/_GameSave", headers=headers, params=params) as response:
            result = (await response.json())["results"][0]
        if result["user"]["objectId"] != objectId:
            raise Exception("save user mismatch")
        summary = base64.b64decode(result["summary"])
        summary = deserialize(Summary, Buffer(summary))
        summary["updateAt"] = result["updatedAt"]
        summary["url"] = result["gameFile"]["url"]
        return summary
    
    async def get_b19(self, url):
        async with self.client.get(url) as response:
            result = await response.read()
        with Save(result) as save:
            gameRecord = save["gameRecord"]
        return parse_b19(gameRecord)
