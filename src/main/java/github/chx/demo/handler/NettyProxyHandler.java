package github.chx.demo.handler;

import github.chx.demo.handler.DataTransHandler;
import github.chx.demo.http.HttpClient;
import github.chx.demo.http.HttpConnectionUtil;
import github.chx.demo.obj.ProxyPathFacotry;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;

import java.util.HashMap;

public class NettyProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private ProxyPathFacotry facotry;

    public NettyProxyHandler(ProxyPathFacotry facotry) {
        this.facotry = facotry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try{
            //从请求头中，获取目标地址
            //该请求头，有发送方和代理服务协商，或者使用常用请求头host
            ReferenceCountUtil.retain(request);

            // 添加处理器
            HashMap<String, ChannelHandler> map = new HashMap<>();
            map.put("httpClient",new HttpClientCodec());
            map.put("dataTransHandler",new DataTransHandler(ctx.channel()));
            //创建客户端连接目标机器
            HttpConnectionUtil.connectToRemote(ctx,"127.0.0.1",9203,1000,map).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        //代理服务器连接目标服务器成功
                        //发送消息到目标服务器
                        //关闭长连接
                        request.headers().set(HttpHeaderNames.CONNECTION, "close");

                        //转发请求到目标服务器
                        channelFuture.channel().writeAndFlush(request).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                if (channelFuture.isSuccess()) {
                                    //移除客户端的http编译码器
                                    channelFuture.channel().pipeline().remove("httpClient");
                                    //移除代理服务和请求端 通道之间的http编译码器和集合器
                                    ctx.channel().pipeline().remove("httpCodec");
                                    ctx.channel().pipeline().remove("aggregator");
                                    //移除后，让通道直接直接变成单纯的ByteBuf传输
                                }
                            }
                        });
                    } else {
                        ReferenceCountUtil.retain(request);
                        ctx.writeAndFlush(HttpClient.getResponse(HttpResponseStatus.BAD_REQUEST, "代理服务连接远程服务失败"))
                                .addListener(ChannelFutureListener.CLOSE);
                    }
                }
            });
        }catch (Exception E){
            System.out.println("出现异常");
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();
//        System.out.println("handler es");
        ctx.close();
    }
}
