package github.chx.demo.obj;

import java.util.HashMap;
import java.util.Map;

/**
 * @author intel小陈
 * @date 2023年07月26日 15:00
 */
public class UserAddressFactory {
    private Map<String,String> map;

    public UserAddressFactory(){
        this.map = new HashMap<String,String>();
    }

    public void put(String key,String value){
        map.put(key,value);
    }

    public String get(String key){
        return map.get(key);
    }
}
