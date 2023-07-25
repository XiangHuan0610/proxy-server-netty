package github.chx.demo.http;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpClient {

    private static DefaultFullHttpResponse defaultFullGetHttpResponse;

    private static DefaultFullHttpResponse defaultFullPostHttpResponse;

    public static DefaultFullHttpResponse sendHttpGetRequest(String host, Integer port, String path){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 生成uri
        String uri = "http://" + host + ":" + port + path;
        HttpGet get = new HttpGet(uri);

        // 设置请求头
        get.setHeader(String.valueOf(HttpHeaderNames.HOST),host);
        get.setHeader(HttpHeaderNames.USER_AGENT.toString(),"Mozilla/5.0" );
        get.setHeader(String.valueOf(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS),"*" );
        try(CloseableHttpResponse response = httpClient.execute(get)){
            // 获取响应状态码
            int responseCode = response.getStatusLine().getStatusCode();
            Header[] headers = response.getHeaders("Content-Type");
            for (Header header : headers) {
                System.out.println(header.toString());
            }

            // 读取响应内容
            String responseContent = EntityUtils.toString(response.getEntity());
            System.out.println(responseContent.toString());
            // 响应数据
            defaultFullGetHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(responseContent, CharsetUtil.UTF_8));

            // 设置响应头
            defaultFullGetHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json");
            defaultFullGetHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,defaultFullGetHttpResponse.content().readableBytes());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return defaultFullGetHttpResponse;
    }
    public static DefaultFullHttpResponse sendHttpPostRequest(String host, Integer port, String path,String json){

       try(CloseableHttpClient httpClient = HttpClients.createDefault()){
           // 生成uri
           String uri = "http://" + host + ":" + port + path;
           HttpPost post = new HttpPost(uri);

           // 将字符串转对象
           StringEntity entity = new StringEntity(json);
           post.setEntity(entity);

           // 设置请求头
           post.setHeader(String.valueOf(HttpHeaderNames.HOST),host);
           post.setHeader(String.valueOf(HttpHeaderNames.CONTENT_TYPE), "application/json");
           post.setHeader(String.valueOf(HttpHeaderNames.USER_AGENT),"Mozilla/5.0" );

           try(CloseableHttpResponse response = httpClient.execute(post)){
               // 获取响应状态码
               int responseCode = response.getStatusLine().getStatusCode();

               // 读取响应内容
               String responseContent = EntityUtils.toString(response.getEntity());

               // 响应数据
               defaultFullPostHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                       Unpooled.copiedBuffer(responseContent, CharsetUtil.UTF_8));

               // 设置响应头
               defaultFullPostHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json");
               defaultFullPostHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,defaultFullPostHttpResponse.content().readableBytes());

           } catch (Exception e) {
               throw new RuntimeException(e);
           }
       }catch (Exception e){
           e.printStackTrace();
       }
        return defaultFullPostHttpResponse;
    }

    public static DefaultFullHttpResponse getResponse(HttpResponseStatus statusCode, String message) {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, statusCode, Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
    }

}

