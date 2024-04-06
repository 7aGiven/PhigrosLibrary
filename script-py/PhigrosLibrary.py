import base64
from Crypto.Cipher import AES
from Crypto.Util import Padding
import io
import struct
import zipfile

levels = ["EZ", "HD", "IN", "AT"]
difficulty = {}

def getBool(num, index):
        return bool(num & 1 << index)

class ByteReader:
    def __init__(self, data:bytes):
        self.data = data
        self.position = 0
    
    def readVarShort(self):
        num = self.data[self.position]
        if  num < 128:
            self.position += 1
        else:
            self.position += 2
        return num
    
    def readString(self):
        length = self.data[self.position]
        self.position += length + 1
        return self.data[self.position-length:self.position].decode()
    
    def readScoreAcc(self):
        self.position += 8
        scoreAcc = struct.unpack("if", self.data[self.position-8:self.position])
        return {"score": scoreAcc[0], "acc": scoreAcc[1]}

    
    def readRecord(self, songId):
        end_position = self.position + self.data[self.position] + 1
        self.position += 1
        exists = self.data[self.position]
        self.position += 1
        fc = self.data[self.position]
        self.position += 1
        diff = difficulty[songId]
        records = []
        for level in range(len(diff)):
            if getBool(exists, level):
                scoreAcc = self.readScoreAcc()
                scoreAcc["level"] = levels[level]
                scoreAcc["fc"] = getBool(fc, level)
                scoreAcc["songId"] = songId
                scoreAcc["difficulty"] = diff[level]
                scoreAcc["rks"] = (scoreAcc["acc"]-55)/45
                scoreAcc["rks"] = scoreAcc["rks"] * scoreAcc["rks"] * scoreAcc["difficulty"]
                records.append(scoreAcc)
        self.position = end_position
        return records 

global_headers = {
    "X-LC-Id": "rAK3FfdieFob2Nn8Am",
    "X-LC-Key": "Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0",
    "User-Agent": "LeanCloud-CSharp-SDK/1.0.3",
    "Accept": "application/json"
}

async def readGameRecord(client, url):
    async with client.get(url) as response:
        result = await response.read()
    with zipfile.ZipFile(io.BytesIO(result)) as zip:
        with zip.open("gameRecord") as gameRecord_file:
            if gameRecord_file.read(1) != b"\x01":
                raise "版本号不正确，可能协议已更新。"
            return gameRecord_file.read()

key = base64.b64decode("6Jaa0qVAJZuXkZCLiOa/Ax5tIZVu+taKUN1V1nqwkks=")
iv = base64.b64decode("Kk/wisgNYwcAV8WVGMgyUw==")

def decrypt_gameRecord(gameRecord):
    gameRecord = AES.new(key, AES.MODE_CBC, iv).decrypt(gameRecord)
    return Padding.unpad(gameRecord, AES.block_size)

def parse_b19(gameRecord):
    records = []
    reader = ByteReader(gameRecord)
    for i in range(reader.readVarShort()):
        songId = reader.readString()[:-2]
        record = reader.readRecord(songId)
        records.extend(record)
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
        async with self.client.get("https://rak3ffdi.cloud.tds1.tapapis.cn/1.1/classes/_GameSave", headers=headers) as response:
            result = (await response.json())["results"][0]
        updateAt = result["updatedAt"]
        url = result["gameFile"]["url"]
        summary = base64.b64decode(result["summary"])
        summary = struct.unpack("=BHfBx%ds12H" % summary[8], summary)
        return {"updateAt": updateAt, "url": url, "saveVersion": summary[0], "challenge": summary[1], "rks": summary[2], "gameVersion": summary[3], "avatar": summary[4].decode(), "EZ": summary[5:8], "HD": summary[8:11], "IN": summary[11:14], "AT": summary[14:17]}
    
    async def get_b19(self, url):
        gameRecord = await readGameRecord(self.client, url)
        gameRecord = decrypt_gameRecord(gameRecord)
        return parse_b19(gameRecord)
