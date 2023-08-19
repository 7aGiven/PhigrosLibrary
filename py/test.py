import aiohttp
import asyncio
import sys
from PhigrosLibrary import B19Class

async def main(sessionToken):
    async with aiohttp.ClientSession() as client:
        b19Class = B19Class(client)
        b19Class.read_difficulty("../difficulty.csv")
        playerId = await b19Class.get_playerId(sessionToken)
        print(playerId)
        summary = await b19Class.get_summary(sessionToken)
        print(summary)
        b19 = await b19Class.get_b19(summary["url"])
    rks = 0
    for song in b19:
        rks += song["rks"]
        print(song)
    print(rks/20)

s = "m7n8t8jhu5sekp6kdyble4yba"
if len(sys.argv) == 2:
    s = sys.argv[1]
asyncio.run(main(s))
