from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol


from phigrosLibrary import Phigros

transport = TTransport.TBufferedTransport(TSocket.TSocket())
protocol = TBinaryProtocol.TBinaryProtocol(transport)
client = Phigros.Client(protocol)
transport.open()
result = client.best19("https://rak3ffdi.tds1.tapfiles.cn/gamesaves/y3rcfBiAGNNW7S6wMRbmP2Rd4qNHBP7u/.save")
transport.close()
print(result)