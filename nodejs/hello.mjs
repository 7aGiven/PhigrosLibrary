import { platform } from "node:os"
import { createRequire } from "node:module"
const require = createRequire(import.meta.url)
const p = platform()
let addon;
if (p == "linux") {
	addon = require("./build/Release/PhigrosLibrary_linux.node")
} else if (p == "win32") {
	addon = require("./build/Release/PhigrosLibrary_win.node")
}



//const sessionToken = "e7fwncl41x41bbwasp74r3ajm"//me
//const sessionToken = "m7n8t8jhu5sekp6kdyble4yba"//maoge
const sessionToken = "716cd1v975dwv6tcl01osti0k"

//获取玩家昵称
//const player = addon.get_player(sessionToken); console.log(player)

//获取summary
//const summary = addon.info(sessionToken); console.log(summary)

//获取存档全部内容
//const save = addon.get_save(summary.url);
//console.log(save)
//const keys = save["gameKey"]["keys"]
//for (let i = 0; i < keys.length; i++) console.log(keys[i])

//读取同目录的difficulty.csv
//addon.read_difficulty()

//从存档读取B19
//const b19 = addon.b19(summary.url); console.log(b19)

//修改data为1GB。序列化版，随着版本更新可能会导致新的键无法被解析。

addon.modify_gameProgress(sessionToken, (obj)=>{
	obj.money[0] = 0
	obj.money[0] = 0
	obj.money[0] = 1
	obj.money[0] = 0
	obj.money[0] = 0
})

//重置第八章剧情，请在确保安全的情况下测试。二进制版，总是可用。
//addon.re8(sessionToken)

//重置第八章剧情，请在确保安全的情况下测试。序列化版，随着版本更新可能会导致新的键无法被解析。
/*
addon.modify_gameProgress(sessionToken, (obj)=>{
	obj.chapter8UnlockBegin = false
	obj.chapter8UnlockSecondPhase = false
	obj.chapter8Passed = false
	obj.chapter8SongUnlocked = 0
})
*/
