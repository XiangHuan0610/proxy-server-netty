package github.chx.demo.server;

import github.chx.demo.handler.HttpGateWayRouteHandler;
import github.chx.demo.obj.LocationAddress;
import github.chx.demo.obj.UserAddressFacotry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
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

    private static UserAddressFacotry facotry;

    public static void main(String[] args) {
        loadingConfig();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            System.out.println(facotry.getUserAddress("user").getLocationAddress().toString());
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast("httpCodec",new HttpServerCodec()); // http 解码器
                            pipeline.addLast("aggregator",new HttpObjectAggregator(65536)); //  聚合http消息
                            pipeline.addLast(new ChunkedWriteHandler()); // 支持异步发送大文件
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
            facotry = new UserAddressFacotry();
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                String[] serverName = line.split("-");
                String[] location = serverName[1].split(":");
                LocationAddress locationAddress = new LocationAddress(location[0], Integer.parseInt(location[1]));
//                UserAddressDto userAddressDto = new UserAddressDto(serverName[0],locationAddress);
                facotry.addAddress(serverName[0],locationAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}