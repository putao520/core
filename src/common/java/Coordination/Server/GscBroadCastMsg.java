package common.java.Coordination.Server;

import common.java.Coordination.Common.GscCenterPacket;
import io.netty.channel.ChannelHandlerContext;

import java.util.Set;

public class GscBroadCastMsg {
    private final GscCenterPacket content;
    private final Set<ChannelHandlerContext> queue;

    private GscBroadCastMsg(GscCenterPacket content, Set<ChannelHandlerContext> queue) {
        this.content = content;
        this.queue = queue;
    }

    public static final GscBroadCastMsg build(GscCenterPacket content, Set<ChannelHandlerContext> queue) {
        return new GscBroadCastMsg(content, queue);
    }

    public void broadCast() {
        for (ChannelHandlerContext _ctx : queue) {
            if (!_ctx.isRemoved()) {
                _ctx.writeAndFlush(content);
            }
        }
    }
}
