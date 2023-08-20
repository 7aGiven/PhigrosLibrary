const addon = require("./build/Release/PhigrosLibrary.node")

//获取玩家昵称
const player = addon.get_player("m7n8t8jhu5sekp6kdyble4yba")
console.log(player)

//读取同目录的difficulty.csv
addon.read_difficulty()

//获取summary
const summary = addon.info("m7n8t8jhu5sekp6kdyble4yba")
console.log(summary)

//获取存档全部内容
const save = addon.get_save(summary.url)
console.log(save)

//从存档读取B19
const b19 = addon.b19(summary.url)
console.log(b19)

//重置第八章剧情，请在确保安全的情况下测试。
//addon.re8("e7fwncl41x41bbwasp74r3ajm")
