import given.phigros.PhigrosUser;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

import java.nio.file.Files;
import java.nio.file.Path;

public class PhigrosServer {
    public static void main(String[] args) throws Exception {
        try (final var reader = Files.newBufferedReader(Path.of("difficulty.csv"))) {
            PhigrosUser.readInfo(reader);
        }

        var server = new ServerBootstrap();
        server.channel(NioServerSocketChannel.class);
        server.group(new NioEventLoopGroup(1), new NioEventLoopGroup());
        server.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                var pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new Handler());
            }
        });
        server.bind("localhost", Integer.parseInt(args[0])).sync();
    }
}
