package common.java.rpc;

public class RpcError {
    private final int errorCode;
    private final String msg;

    private RpcError(int errorCode, String msg) {
        this.errorCode = errorCode;
        this.msg = msg;
    }

    public static RpcError Instant(int errorCode, String msg) {
        return new RpcError(errorCode, msg);
    }

    public static RpcError Instant(boolean state, String msg) {
        return new RpcError(state ? 0 : 1, msg);
    }

    public String toString() {
        return rMsg.netMSG(errorCode, msg, "");
    }
}
