import given.phigros.PhigrosUser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.nio.charset.StandardCharsets;

class Handler extends SimpleChannelInboundHandler<HttpRequest> {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        var path = msg.uri().split("/", 3);
        String result = switch (path[1]) {
            case "saveUrl" -> {
                if (path[2].length() != 25)
                    yield "SessionToken的长度不为25";
                final var user = new PhigrosUser(path[2]);
                final var summary = user.update();
                yield summary.toString(user.saveUrl.toString());
            }
            case "b19" -> {
                StringBuilder builder = new StringBuilder("[");
                for (final var songLevel : new PhigrosUser(URI.create(path[2])).getBestN(19)) {
                    builder.append(songLevel.toString());
                    builder.append(',');
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append(']');
                yield  builder.toString();
            }
            case "expects" -> {
                StringBuilder builder = new StringBuilder("[");
                for (final var songExpect : new PhigrosUser(URI.create(path[2])).getExpects()) {
                    builder.append(songExpect.toString());
                    builder.append(',');
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append(']');
                yield builder.toString();
            }
            default -> "URL未匹配";
        };
        var content = Unpooled.copiedBuffer(result, StandardCharsets.UTF_8);
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK, content);
        response.headers().add("Content-Type", "application/json;charset=utf-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}