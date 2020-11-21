package common.java.rpc;

import common.java.httpServer.HttpContext;
import org.json.simple.JSONObject;

public class RpcTask {
    private String rpcURL;
    private JSONObject header;
    private JSONObject postParameter;

    private RpcTask(JSONObject json) {
        if (!JSONObject.isInvaild(json)) {
            this.rpcURL = json.getString("rpc");
            this.postParameter = json.getJson("parameter");
            this.header = json.getJson("header");
        }
    }

    public static final RpcTask newTask(JSONObject info) {
        return new RpcTask(info);
    }

    public static final RpcTask newTask() {
        return new RpcTask(new JSONObject());
    }

    public boolean hasData() {
        return this.rpcURL != null;
    }

    public RpcTask rpc(String newRpcUrl) {
        this.rpcURL = newRpcUrl;
        return this;
    }

    public RpcTask parameter(JSONObject postParameter) {
        this.postParameter = postParameter;
        return this;
    }

    public RpcTask header(JSONObject header) {
        this.header = header;
        return this;
    }

    public String run() {
        HttpContext ctx = HttpContext.newHttpContext();
        if (this.header != null) {
            ctx.header(this.header);
        }
        Object[] args = ExecRequest.postJson2ObjectArray(this.postParameter);
        String[] rpcs = this.rpcURL.split("/");
        return rpc.service(rpcs[1]).setPath(rpcs[2], rpcs[3]).setContext(ctx).call(args).asString();
    }

    public JSONObject toJson() {
        return JSONObject.putx("rpc", this.rpcURL).puts("header", this.header).puts("parameter", this.postParameter);
    }
}