package common.java.Coordination.Common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessagePacketEncoder extends MessageToByteEncoder<GscCenterPacket> {
    public MessagePacketEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, GscCenterPacket msg, ByteBuf out) {
        out.writeBytes(msg.toByteArray());
    }
}