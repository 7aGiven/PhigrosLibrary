const addon = require("./build/Release/PhigrosLibrary.node")

addon.read_difficulty()

const summary = addon.info("h0kg9ba19ulrlz408o22g44pq")
console.log(summary)

const b19 = addon.b19(summary.url)
console.log(b19)
