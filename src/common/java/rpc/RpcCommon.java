package common.java.rpc;

public class RpcCommon {
    /**
     * java参数变gsc-rpc参数
     */
    public static final String paramer2string(Object... args) {
        return ExecRequest.objects2string(args);
    }
}
