package github.chx.demo.server;

import github.chx.demo.handler.NettyProxyHandler;
import github.chx.demo.obj.ProxyPathFacotry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
                        //Http代理服务
                        pipeline.addLast(new NettyProxyHandler(facotry));
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
}

