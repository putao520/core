package common.java.Coordination.Server;

import common.java.Coordination.Common.GscCenterPacket;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

public class GscBroadCastMsg {
    private final GscCenterPacket content;
    private final ConcurrentHashMap<String, ChannelHandlerContext> queue;

    private GscBroadCastMsg(GscCenterPacket content, ConcurrentHashMap<String, ChannelHandlerContext> queue) {
        this.content = content;
        this.queue = queue;
    }

    public static final GscBroadCastMsg build(GscCenterPacket content, ConcurrentHashMap<String, ChannelHandlerContext> queue) {
        return new GscBroadCastMsg(content, queue);
    }

    public void broadCast() {
        var iter = queue.values().iterator();
        while (iter.hasNext()) {
            var _ctx = iter.next();
            if (!_ctx.isRemoved()) {
                _ctx.writeAndFlush(content);
            }
            iter.remove();
        }
    }
}
