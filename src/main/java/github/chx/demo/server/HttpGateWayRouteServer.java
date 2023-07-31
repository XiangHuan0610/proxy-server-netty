package github.chx.demo.server;

import github.chx.demo.handler.HttpGateWayRouteHandler;
import github.chx.demo.obj.UserAddressFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author intel小陈
 * @date 2023年07月21日 19:13
 * HTTP网关路由
 */
public class HttpGateWayRouteServer {
    private static final String host = "127.0.0.1";
    private static final Integer prot = 88;



    private static UserAddressFactory facotry;

    public static void main(String[] args) {
        loadingConfig();
        System.out.println(facotry.toString());
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast("httpCodec",new HttpServerCodec()); // http 解码器
                            pipeline.addLast("aggregator",new HttpObjectAggregator(65536)); //  聚合http消息
//                            pipeline.addLast(new ChunkedWriteHandler()); // 支持异步发送大文件
                            pipeline.addLast(new JsonObjectDecoder()); // json数据解码器
                            pipeline.addLast(new HttpGateWayRouteHandler(facotry));
                        }
                    });
            serverBootstrap.bind(prot).sync().channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static void loadingConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/conf/location.txt"))) {
            String line;
            facotry = new UserAddressFactory();
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                facotry.put(split[0],split[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
