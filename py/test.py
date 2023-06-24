import aiohttp
import asyncio
import sys
from PhigrosLibrary import B19Class

async def main(sessionToken):
    async with aiohttp.ClientSession() as client:
        b19Class = B19Class(client)
        b19Class.read_difficulty("difficulty.csv")
        playerId = await b19Class.get_playerId(sessionToken)
        print(playerId)
        summary = await b19Class.get_summary(sessionToken)
        print(summary)
        b19 = await b19Class.get_b19(summary["url"])
    for song in b19:
        print(song)

s = "h0kg9ba19ulrlz408o22g44pq"
if len(sys.argv) == 2:
    s = sys.argv[1]
asyncio.run(main(s))