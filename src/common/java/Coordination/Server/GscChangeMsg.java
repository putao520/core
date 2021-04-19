package common.java.Coordination.Server;

import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;

public class GscChangeMsg {
    private final String serviceName;
    private final JSONObject data;
    private final ChannelHandlerContext ctx;
    private final String action;
    private final int deployId;

    private GscChangeMsg(String serviceInfo, String action, JSONObject data, ChannelHandlerContext ctx) {
        String[] serviceInfoArr = serviceInfo.split("_");
        this.serviceName = serviceInfoArr[0];
        this.deployId = serviceInfoArr.length > 1 ? Integer.valueOf(serviceInfoArr[1]) : 0;
        this.action = action;
        this.data = data;
        this.ctx = ctx;
    }

    public static GscChangeMsg build(String serviceName, String action, JSONObject data, ChannelHandlerContext ctx) {
        return new GscChangeMsg(serviceName, action, data, ctx);
    }

    public static GscChangeMsg build(String serviceName, String action, JSONObject data) {
        return new GscChangeMsg(serviceName, action, data, null);
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public int getDeployId() {
        return this.deployId;
    }

    public JSONObject getData() {
        return data;
    }

    public ChannelHandlerContext getChannel() {
        return this.ctx;
    }

    public String getAction() {
        return this.action;
    }
}
