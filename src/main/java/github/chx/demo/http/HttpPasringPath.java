package github.chx.demo.http;

/**
 * @author intel小陈
 * @date 2023年07月26日 15:12
 */
public class HttpPasringPath {



    public static String parsimeUrl(String url){
        return parsimeUrl(parsimeUrl(url,2),2);
    }

    public static String parsimeName(String url){
        return parsimeUrl(url, 2).split("/")[1];
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


    public static String parsimeHost(String location) {
        return location.split("http://")[1].split(":")[0];
    }

    public static Integer parsimePort(String location){
        return Integer.parseInt(location.split("http://")[1].split(":")[1]);
    }
}
