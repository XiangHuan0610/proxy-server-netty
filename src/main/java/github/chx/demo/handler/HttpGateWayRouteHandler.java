package github.chx.demo.handler;

import com.sun.net.httpserver.HttpsParameters;
import github.chx.demo.http.HttpClient;
import github.chx.demo.http.HttpConnectionUtil;

import github.chx.demo.http.HttpPasringPath;
import github.chx.demo.obj.UserAddressFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;


/**
 * @author intel小陈
 * @date 2023年07月21日 19:14
 */
public class HttpGateWayRouteHandler extends SimpleChannelInboundHandler<HttpObject> {

    private UserAddressFactory facotry;

    private static final String STATIC_FOLDER = "C:\\Users\\Administrator\\Desktop\\milsun-vite-demo-master\\dist";

    public HttpGateWayRouteHandler(UserAddressFactory facotry){
        this.facotry = facotry;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//        ctx.channel().isWritable()
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest){
            FullHttpRequest request = (FullHttpRequest) msg;
            ReferenceCountUtil.retain(request);

            // 获取uri
            String uri = request.getUri();
            System.out.println(uri);
            String location = "";

            // 获取服务名称
            String name = HttpPasringPath.parsimeName(uri);
            location = facotry.get(name);

            // 获取ip和端口
            String host = HttpPasringPath.parsimeHost(location);
            Integer port = HttpPasringPath.parsimePort(location);

            // 设置url
            String url = HttpPasringPath.parsimeUrl(uri);
            request.setUri(url);

            // 添加处理器
            HashMap<String, ChannelHandler> map = new HashMap<>();
            map.put("httpClient",new HttpClientCodec());

            map.put("dataTransHandler",new DataTransHandler(ctx.channel()));

            if (true){
                DefaultFullHttpResponse defaultFullPostHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.copiedBuffer("hello jmeter", CharsetUtil.UTF_8));

                // 设置响应头
                defaultFullPostHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json");
                defaultFullPostHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,defaultFullPostHttpResponse.content().readableBytes());
                ctx.writeAndFlush(defaultFullPostHttpResponse);
                return;
            }

            // 连接
            HttpConnectionUtil.connectToRemote(ctx,host,port,3000,map).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        //代理服务器连接目标服务器成功
                        //发送消息到目标服务器
                        //关闭长连接
//                        request.headers().set(HttpHeaderNames.CONNECTION, "close");

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
//                        ReferenceCountUtil.retain(request);
                        ctx.writeAndFlush(HttpClient.getResponse(HttpResponseStatus.BAD_REQUEST, "代理服务连接远程服务失败"))
                                .addListener(ChannelFutureListener.CLOSE);
                    }
                }
            });

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
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




}
