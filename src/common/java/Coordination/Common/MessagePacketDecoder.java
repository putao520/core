package common.java.Coordination.Common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessagePacketDecoder extends ByteToMessageDecoder {

    public MessagePacketDecoder() throws Exception {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        int bag_length = 0;
        int readable_length;
        do {
            readable_length = buffer.readableBytes();
            if (readable_length <= 0) {
                break;
            }
            byte[] sign_byte = new byte[4];
            buffer.readBytes(sign_byte);
            String sign_str = new String(sign_byte);
            // 当前块起始是包标记
            if (!sign_str.equals("GSC_")) {
                return;
            }
            // 获得包字节长度(获得下一个包入口地址)
            bag_length += buffer.readInt();
            if (readable_length < bag_length) {
                break;
            }
            // 解析数据集合
            out.add(GscCenterPacket.build(buffer));
            // 释放已读包数据,循环利用ringBuffer
            buffer.discardReadBytes();
        } while (true);
    }
}