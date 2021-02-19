package common.java.ServiceTemplate;

import common.java.Encrypt.GscJson;
import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;

public class RpcBeforeMicroService extends RpcBefore {
    public RpcBeforeMicroService() {
        // 默认特定方法解密
        $("insert", (func, param) -> {
            param[0] = GscJson.decode((String) param[0]);
            return FilterReturn.buildTrue();
        }).$("update", (func, param) -> {
            param[1] = GscJson.decode((String) param[1]);
            return FilterReturn.buildTrue();
        });
    }
}
