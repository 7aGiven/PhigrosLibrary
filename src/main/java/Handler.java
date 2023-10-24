import given.phigros.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.io.IOException;
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
        String[] path = msg.uri().split("/", 3);
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
                    final PhigrosUser user = new PhigrosUser(path[2]);
                    final Summary summary = user.update();
                    yield summary.toString(user.saveUrl.toString());
                }
                case "playerId" -> new PhigrosUser(path[2]).getPlayerId();
                case "b19" -> {
                    StringBuilder builder = new StringBuilder("[");
                    for (final SongLevel songLevel : new PhigrosUser(URI.create(path[2])).getBestN(19)) {
                        builder.append(songLevel.toString());
                        builder.append(',');
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(']');
                    yield  builder.toString();
                }
                case "expects" -> {
                    StringBuilder builder = new StringBuilder("[");
                    for (final SongExpect songExpect : new PhigrosUser(URI.create(path[2])).getExpects()) {
                        builder.append(songExpect.toString());
                        builder.append(',');
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(']');
                    yield builder.toString();
                }
                case "song" -> {
                    path = path[2].split("/", 2);
                    StringBuilder builder = new StringBuilder("[");
                    LevelRecord[] levelRecords = new PhigrosUser(URI.create(path[1])).get(GameRecord.class).get(path[0]);
                    for (byte level = 0; level < 4; level++) {
                        final LevelRecord record = levelRecords[level];
                        if (record != null)
                            builder.append(String.format("{\"level\":%s,\"s\":%s,\"a\":%s,\"c\":%s},", level, record.s, record.a, record.c));
                        else
                            builder.append("{},");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(']');
                    yield  builder.toString();
                }
                case "8" -> {
                    new PhigrosUser(path[2]).modify(GameProgress.class, data -> {
                        data.chapter8UnlockBegin = false;
                        data.chapter8UnlockSecondPhase = false;
                        data.chapter8Passed = false;
                        data.chapter8SongUnlocked = 0;
                    });
                    yield "OK";
                }
                case "data" -> {
                    PhigrosUser money = new PhigrosUser(URI.create(path[2])).get(GameProgress.class).money;
                    yield  String.format("%d,%d,%d,%d,%d", money[0], money[1], money[2], money[3], money[4]);
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
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }

    private void response(ChannelHandlerContext ctx, String content) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content, StandardCharsets.UTF_8));
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=utf-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }

    private void response(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content, StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }
}