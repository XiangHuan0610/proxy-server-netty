package github.chx.demo.http;

import github.chx.demo.handler.DataTransHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

import java.util.List;
import java.util.Map;

/**
 * @author intel小陈
 * @date 2023年07月25日 17:18
 */
public class HttpConnectionUtil {

    // 建立连接
    public static ChannelFuture connectToRemote(ChannelHandlerContext ctx, String targetHost, int targetPort, int timeout, Map<String,ChannelHandler> map) {
        return new Bootstrap().group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        for (String key : map.keySet()) {
                            ChannelHandler channelHandler = map.get(key);
                            pipeline.addLast(key,channelHandler);
                        }
                    }
                })
                .connect(targetHost, targetPort);
    }
}
