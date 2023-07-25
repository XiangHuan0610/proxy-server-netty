package github.chx.demo.obj;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * @author intel小陈
 * @date 2023年07月23日 19:02
 */
@Data
@ToString
public class ProxyPathFacotry {
    private Map<String,String> map;

    public ProxyPathFacotry(){
        this.map = new HashMap<String,String>();
    }

    public void put(String path,String proxyPath){
        map.put(path,proxyPath);
    }

    public String get(String path){
        return map.get(path);
    }
}

