package github.chx.demo.handler;

import github.chx.demo.handler.DataTransHandler;
import github.chx.demo.http.HttpClient;
import github.chx.demo.http.HttpConnectionUtil;
import github.chx.demo.http.HttpPasringPath;
import github.chx.demo.obj.ProxyPathFacotry;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ChannelHandler.Sharable
public class NettyProxyHandler extends SimpleChannelInboundHandler<Object> {

    private ProxyPathFacotry facotry;

    private static final String STATIC_FOLDER = "C:\\Users\\Administrator\\Desktop\\milsun-vite-demo-master\\dist";

    private ReentrantLock lock = new ReentrantLock();

    public NettyProxyHandler(ProxyPathFacotry facotry) {
        this.facotry = facotry;
    }



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try{
            FullHttpRequest request = (FullHttpRequest) msg;
            ReferenceCountUtil.retain(request);
            // 获取uri
            String uri = request.getUri();
            String proxyLocation = "";

            for (String key : facotry.getMap().keySet()) {
                if (uri.contains(key) && !key.equals("/")) {
                    proxyLocation = facotry.getMap().get(key);
                }
            }

            if (!uri.contains("/api")) {
                staicFileReponse(ctx, request);
                return;
            }

            String url = HttpPasringPath.parsimeUrl(uri);
            String host = HttpPasringPath.parsimeHost(proxyLocation);
            Integer port = HttpPasringPath.parsimePort(proxyLocation);
            System.out.println(proxyLocation);
//            request.setUri(url);

            // 添加处理器
            HashMap<String, ChannelHandler> map = new HashMap<>();
            map.put("httpClient", new HttpClientCodec());
            map.put("dataTransHandler", new DataTransHandler(ctx.channel()));

            //创建客户端连接目标机器
            HttpConnectionUtil.connectToRemote(ctx, host, port, 3000, map).addListener(new ChannelFutureListener() {
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
//                                    channelFuture.channel().pipeline().remove("httpClient");
//                                    //移除代理服务和请求端 通道之间的http编译码器和集合器
//                                    ctx.channel().pipeline().remove("httpCodec");
//                                    ctx.channel().pipeline().remove("aggregator");
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
            E.printStackTrace();;
            System.out.println("出现异常");
        }
    }



    private void staicFileReponse(ChannelHandlerContext ctx, FullHttpRequest request)  {

        try {
            String uri = request.uri();
            if ("/".equals(uri)) {
                uri = "/index.html"; // 将根路径的请求重定向到index.html
            }

            File file = new File(STATIC_FOLDER + uri);
            if (file.exists() && file.isFile()) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                long fileLength = raf.length();

                HttpResponseStatus status = HttpResponseStatus.OK;
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
                response.headers().set(HttpHeaders.Names.CONTENT_TYPE, getContentType(uri)); // 设置正确的Content-Type
                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, fileLength);

                // 读取文件内容并设置为响应的内容
                byte[] content = new byte[(int) fileLength];
                raf.readFully(content);
                response.content().writeBytes(content);

                ctx.writeAndFlush(response);
            }
        }catch (Exception e){
            e.printStackTrace();;
        }
    }

        private static String getContentType(String uri) {
            if (uri.endsWith(".html")) {
                return "text/html; charset=UTF-8";
            } else if (uri.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            } else if (uri.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            } else if (uri.endsWith(".png")) {
                return "image/png";
            } else if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (uri.endsWith(".gif")) {
                return "image/gif";
            }
            return "text/plain; charset=UTF-8";
        }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();
//        System.out.println("handler es");
        ctx.close();
    }
}
