import given.phigros.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class Handler extends SimpleChannelInboundHandler<HttpRequest> {
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
        String[] path = msg.uri().split("/", 3);
        if (path.length != 3) {
            notFound(ctx);
            return;
        }
        String result = "";
        StringBuilder builder;
        try {
            switch (path[1]) {
                case "saveUrl":
                    if (path[2].length() != 25)
                        result = "SessionToken的长度不为25";
                    else {
                        final PhigrosUser user = new PhigrosUser(path[2]);
                        final Summary summary = user.update();
                        result = summary.toString(user.saveUrl.toString());
                    }
                    break;
                case "playerId":
                    new PhigrosUser(path[2]).getPlayerId();
                    break;
                case "b19":
                    builder = new StringBuilder("[");
                    for (final SongLevel songLevel : new PhigrosUser(new URL(path[2])).getBestN(19)) {
                        builder.append(songLevel.toString());
                        builder.append(',');
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(']');
                    result =  builder.toString();
                    break;
                case "expects":
                    builder = new StringBuilder("[");
                    for (final SongExpect songExpect : new PhigrosUser(new URL(path[2])).getExpects()) {
                        builder.append(songExpect.toString());
                        builder.append(',');
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(']');
                    result = builder.toString();
                    break;
                case "song":
                    path = path[2].split("/", 2);
                    builder = new StringBuilder("[");
                    LevelRecord[] levelRecords = new PhigrosUser(new URL(path[1])).get(GameRecord.class).get(path[0]);
                    for (byte level = 0; level < 4; level++) {
                        final LevelRecord record = levelRecords[level];
                        if (record != null)
                            builder.append(String.format("{\"level\":%s,\"s\":%s,\"a\":%s,\"c\":%s},", level, record.s, record.a, record.c));
                        else
                            builder.append("{},");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(']');
                    result = builder.toString();
                    break;
                case "8":
                    new PhigrosUser(path[2]).modify(GameProgress.class, data -> {
                        data.chapter8UnlockBegin = false;
                        data.chapter8UnlockSecondPhase = false;
                        data.chapter8Passed = false;
                        data.chapter8SongUnlocked = 0;
                    });
                    result = "OK";
                    break;
                case "data":
                    short[] money = new PhigrosUser(new URL(path[2])).get(GameProgress.class).money;
                    result = String.format("%d,%d,%d,%d,%d", money[0], money[1], money[2], money[3], money[4]);
                    break;
            }
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