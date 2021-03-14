package common.java.Coordination.Server;

import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;

public class GscChangeMsg {
    private final String serviceName;
    private final JSONObject data;
    private final ChannelHandlerContext ctx;
    private final String action;

    private GscChangeMsg(String serviceName, String action, JSONObject data, ChannelHandlerContext ctx) {
        this.serviceName = serviceName;
        this.action = action;
        this.data = data;
        this.ctx = ctx;
    }

    public static GscChangeMsg build(String serviceName, String action, JSONObject data, ChannelHandlerContext ctx) {
        return new GscChangeMsg(serviceName, action, data, ctx);
    }

    public String getServiceName() {
        return this.serviceName;
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
