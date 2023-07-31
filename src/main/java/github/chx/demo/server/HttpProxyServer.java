package github.chx.demo.server;

import github.chx.demo.handler.NettyProxyHandler;
import github.chx.demo.obj.ProxyPathFacotry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.*;

public class HttpProxyServer {

    private static ProxyPathFacotry facotry;

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/conf/proxy-config.txt"))) {
            String line;
            facotry = new ProxyPathFacotry();
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                facotry.put(split[0],split[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(facotry.toString());
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(1);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
//                .option(ChannelOption.TCP_NODELAY, false)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //Http编解码器
                        pipeline.addLast("httpCodec",new HttpServerCodec());
                        pipeline.addLast("aggregator",new HttpObjectAggregator(100*1024*1024));
                        pipeline.addLast("chunk",new ChunkedWriteHandler());
                        pipeline.addLast(new HttpContentDecompressor());
//                        pipeline.addLast("static",new StaticFileHandler());
                        //Http代理服务
                        pipeline.addLast("handler",new NettyProxyHandler(facotry));
                    }
                });
        ChannelFuture bindFuture = serverBootstrap.bind(80).sync();

        try {
            bindFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    @ChannelHandler.Sharable
    private static class StaticFileHandler extends ChannelInboundHandlerAdapter {
        private static final String STATIC_FOLDER = "C:\\Users\\Administrator\\Desktop\\milsun-vite-demo-master\\dist";

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) msg;
                String uri = request.uri();
                System.out.println(request.toString());
                System.out.println(request.uri().toString());
                if ("/".equals(uri)) {
                    uri = "/index.html"; // 将根路径的请求重定向到index.html
                }
                System.out.println("uri: " + uri);
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
                } else {
//                    sendError(ctx, HttpResponseStatus.NOT_FOUND);
                }
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


    }
}

