const addon = require("./build/Release/PhigrosLibrary_linux.node")

const sessionToken = ""

//获取玩家昵称
//const player = addon.get_player(sessionToken); console.log(player)

//获取summary
//const summary = addon.info(sessionToken); console.log(summary)

//获取存档全部内容
//const save = addon.get_save(summary.url); console.log(save)

//读取同目录的difficulty.csv
//addon.read_difficulty()

//从存档读取B19
//const b19 = addon.b19(summary.url); console.log(b19)

//重置第八章剧情，请在确保安全的情况下测试。
addon.re8(sessionToken)
