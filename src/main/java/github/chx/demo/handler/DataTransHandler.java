package github.chx.demo.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutorGroup;

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

        channel.writeAndFlush(msg);
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

//        System.out.println("data es");
        ctx.close();
    }
}
