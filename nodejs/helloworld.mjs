import ffi from "ffi-napi"

const path = "./libphigros" //libphigros.so的路径

//除了get_handle和load_difficulty,其他函数的参数均为handle
const phigros = ffi.Library(path,{
    "get_handle": ["pointer", ["string"]],  //获取handle,申请内存,参数为sessionToken
    "free_handle": ["void", ["pointer"]],   //释放handle的内存,不会被垃圾回收,使用完handle请确保释放
    "get_nickname": ["string", ["pointer"]],//获取玩家昵称
    "get_summary": ["string", ["pointer"]], //获取Summary
    "get_save": ["string", ["pointer"]],    //获取存档
    "load_difficulty": ["void", ["string"]],//读取difficulty.tsv,参数为文件路径
    "get_b19": ["string", ["pointer"]],     //从存档读取B19,依赖load_difficulty
})

const sessionToken = ""
const handle = phigros.get_handle(sessionToken)
console.log(handle)
console.log(phigros.get_nickname(handle))
console.log(phigros.get_summary(handle))
console.log(phigros.get_save(handle))
phigros.load_difficulty("../difficulty.tsv")
console.log(phigros.get_b19(handle))
phigros.free_handle(handle)
