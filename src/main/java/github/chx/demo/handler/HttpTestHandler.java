package github.chx.demo.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author intel小陈
 * @date 2023年07月24日 13:18
 */
public class HttpTestHandler extends SimpleChannelInboundHandler<HttpObject> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject httpObject) throws Exception {
        String hello = " hello";
        byte[] bytes = hello.getBytes();
        DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse
                (HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(bytes));
        defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/plain");
        defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,defaultFullHttpResponse.content().readableBytes());
        channelHandlerContext.writeAndFlush(defaultFullHttpResponse);
    }
}
