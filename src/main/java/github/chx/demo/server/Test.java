package github.chx.demo.server;

/**
 * @author intel小陈
 * @date 2023年07月26日 15:17
 */
public class Test {
    public static void main(String[] args) {
        String url = "http://127.0.0.1:9202";
        String s = url.split("http://")[1].split(":")[1];
        System.out.println(s);
        String uri = "/api/product/prompt/list";
        System.out.println(parsimeUrl(parsimeUrl(uri,2),2));
    }

    private static String parsimeUrl(String url,Integer count){
        char[] arry = url.toCharArray();
        int index = 0;
        int i = 0;
        for (i = 0; i < arry.length; i++) {
            if(arry[i] == '/'){
                index++;
            }
            if (index == count) break;
        }
        return url.substring(i,url.length());
    }

}
