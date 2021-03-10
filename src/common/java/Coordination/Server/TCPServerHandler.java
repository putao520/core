package common.java.Coordination.Server;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.String.StringHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.nio.file.Paths;

import static common.java.Coordination.Common.GscCenterEvent.*;

public class TCPServerHandler extends ChannelInboundHandlerAdapter {
    private final GscCenterServer centerServer;

    public TCPServerHandler() {
        String path = Config.config("StorePath");
        if (StringHelper.isInvalided(path)) {
            throw new RuntimeException("数据持久化地址未配置");
        }
        centerServer = GscCenterServer.getInstance(Paths.get(path));
    }

    // 断开连接,自动移除对应的全部订阅
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        centerServer.removeChannel(ctx);
    }

    // 异常连接,自动移除对应的全部订阅
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        centerServer.removeChannel(ctx);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object source) throws Exception {
        // 返回的数据组
        JSONArray resultArr;
        // 拿到传过来的msg数据，开始处理
        GscCenterPacket respMsg = (GscCenterPacket) source;// 转化为GscCenterPacket
        // 收到数据修改请求
        switch (respMsg.getEventId()) {
            case Subscribe: // 订阅
                centerServer.subscribe(respMsg.getKey(), respMsg.getData(), ctx);
                break;
            case UnSubscribe: // 取消订阅
                centerServer.unSubscribe(respMsg.getKey(), ctx);
                break;
            case HeartPing:
                ctx.writeAndFlush(GscCenterPacket.build(respMsg.getKey(), JSONObject.build("status", true), HeartPong, true));
                break;
        }
        // respMsg.getOperatorCode().setStatus(true);
        // ctx.writeAndFlush(respMsg);// 收到及发送，这里如果没有writeAndFlush，上面声明的ByteBuf需要ReferenceCountUtil.release主动释放
    }

}