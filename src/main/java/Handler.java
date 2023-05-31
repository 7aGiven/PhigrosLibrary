import given.phigros.PhigrosUser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

class Handler extends SimpleChannelInboundHandler<HttpRequest> {
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        var path = msg.uri().split("/", 3);
        if (path.length != 3) {
            notFound(ctx);
            return;
        }
        String result;
        try {
            result = switch (path[1]) {
                case "saveUrl" -> {
                    if (path[2].length() != 25)
                        yield "SessionToken的长度不为25";
                    final var user = new PhigrosUser(path[2]);
                    final var summary = user.update();
                    yield summary.toString(user.saveUrl.toString());
                }
                case "playerId" -> new PhigrosUser(path[2]).getPlayerId();
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
                default -> "";
            };
        } catch (Exception e) {
            response(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        if (result.length() == 0)
            notFound(ctx);
        else if (result.charAt(0) != '{')
            response(ctx, HttpResponseStatus.BAD_REQUEST, result);
        else
            response(ctx, result);
    }

    private void notFound(ChannelHandlerContext ctx) {
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }

    private void response(ChannelHandlerContext ctx, String content) {
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content, StandardCharsets.UTF_8));
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=utf-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }

    private void response(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content, StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }
}