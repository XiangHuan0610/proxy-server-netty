package github.chx.demo.handler;

import github.chx.demo.http.HttpClient;
import github.chx.demo.http.HttpConnectionUtil;
import github.chx.demo.http.HttpParsingPath;
import github.chx.demo.http.HttpResponseHandler;
import github.chx.demo.obj.LocationAddress;
import github.chx.demo.obj.UserAddressFacotry;
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

import java.util.HashMap;


/**
 * @author intel小陈
 * @date 2023年07月21日 19:14
 */
public class HttpGateWayRouteHandler extends SimpleChannelInboundHandler<HttpObject> {

    private UserAddressFacotry facotry;

    public HttpGateWayRouteHandler(UserAddressFacotry facotry){
        this.facotry = facotry;
    }

    private HttpResponseHandler reponseHandler;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof  HttpRequest){
            FullHttpRequest request = (FullHttpRequest) msg;

            // 获取uri
            String uri = request.getUri();

            // 获取服务名称
            String serverName = HttpParsingPath.getServerName(uri);

            // 获取ip和端口
            LocationAddress locationAddress = facotry.getUserAddress(serverName).getLocationAddress();

            // 获取请求路径与携带的参数,并重新设置请求参数
            String url = HttpParsingPath.forwardRequest(uri);
            request.setUri(url);

            // 设置handler
            HashMap<String, ChannelHandler> map = new HashMap<>();
            map.put("httpClient",new HttpClientCodec());
            map.put("dataTransHandler",new DataTransHandler(ctx.channel()));
            ReferenceCountUtil.retain(request);
            HttpConnectionUtil.connectToRemote(ctx,locationAddress.getHost(),locationAddress.getPort(),3000,map).addListener(new ChannelFutureListener() {
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
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }





}
