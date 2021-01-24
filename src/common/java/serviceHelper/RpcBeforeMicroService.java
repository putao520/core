package common.java.serviceHelper;

import common.java.encrypt.GscJson;
import common.java.rpc.FilterReturn;
import common.java.rpc.RpcBefore;

public class RpcBeforeMicroService extends RpcBefore {
    static {
        // 默认特定方法解密
        $("insert", (func, param) -> {
            param[0] = GscJson.decode((String) param[0]);
            return FilterReturn.buildTrue();
        });
        $("update", (func, param) -> {
            param[1] = GscJson.decode((String) param[1]);
            return FilterReturn.buildTrue();
        });
    }
}
