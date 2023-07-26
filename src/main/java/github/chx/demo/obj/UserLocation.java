package github.chx.demo.obj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author intel小陈
 * @date 2023年07月26日 16:29
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserLocation {
    String host;
    Integer port;
    String url;
}
