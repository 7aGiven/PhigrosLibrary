import given.phigros.PhigrosUser;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {
    public static void main(String[] args) throws IOException, TTransportException {
        try (final var reader = Files.newBufferedReader(Path.of(args[1]))) {
            PhigrosUser.readInfo(reader);
        }
        TServerTransport serverTransport = new TServerSocket(Integer.parseInt(args[0]));
        TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(new Phigros.Processor<Phigros.Iface>(new PhigrosImpl())));
        server.serve();
    }
}
