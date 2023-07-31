package github.chx.demo.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.http.util.EntityUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author intel小陈
 * @date 2023年07月24日 16:25
 */
public class DataTransHandler extends SimpleChannelInboundHandler<Object> {
    private Channel channel;

    public DataTransHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!channel.isOpen()) {
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg instanceof  DefaultHttpContent) {
            DefaultHttpContent defaultHttpContent = (DefaultHttpContent) msg;

            // 创建 响应
            DefaultFullHttpResponse defaultFullPostHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(defaultHttpContent.content().toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8));

            // 设置响应头
            defaultFullPostHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json");
            defaultFullPostHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,defaultFullPostHttpResponse.content().readableBytes());
            channel.writeAndFlush(defaultFullPostHttpResponse);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //目标服务器和代理服务器断开连接
        //代理服务器和原始服务器也断开连接
        if (channel != null) {
            //发送一个空的buf,通过listener监听的方式，关闭channel，确保通道中的数据传输完毕
            channel.writeAndFlush(PooledByteBufAllocator.DEFAULT.buffer()).addListener(ChannelFutureListener.CLOSE);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();;
//        System.out.println("data es");
        ctx.close();
    }
}
