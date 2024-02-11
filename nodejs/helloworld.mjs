import { platform } from "node:os"
import { createRequire } from "node:module"
const require = createRequire(import.meta.url)

let addon
const p = platform()
if (p == "linux") {
	addon = require("./build/Release/phigros_linux.node")
} else if (p == "win32") {
	addon = require("./build/Release/phigros_win.node")
}

const sessionToken = ""

//获取玩家昵称
// const player = addon.get_nickname(sessionToken); console.log(player)


//读取difficulty.tsv
// addon.load_difficulty("../difficulty.tsv"); console.log("difficulty")

//从存档读取B19
// const b19 = addon.b19(sessionToken); console.log(b19)

//重置第八章剧情，请在确保安全的情况下测试。二进制版，总是可用。
addon.re8(sessionToken)