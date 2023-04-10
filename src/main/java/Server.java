import given.phigros.PhigrosUser;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {
    public static void main(String[] args) throws IOException, TTransportException {
        try (final var reader = Files.newBufferedReader(Path.of(args[1]))) {
            PhigrosUser.readInfo(reader);
        }
        final var arg = new TNonblockingServer.Args(new TNonblockingServerSocket(Integer.parseInt(args[0])));
        arg.processor(new Phigros.Processor<Phigros.Iface>(new PhigrosImpl()));
        new TNonblockingServer(arg).serve();
    }
}
