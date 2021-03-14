package common.java.Coordination.Server;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.Coordination.Common.payPacket;
import common.java.String.StringHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.json.gsc.JSONObject;

import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import static common.java.Coordination.Common.GscCenterEvent.*;

public class TCPServerHandler extends ChannelInboundHandlerAdapter {
    private final GscCenterServer centerServer;
    public static ConcurrentHashMap<String, payPacket> preload = new ConcurrentHashMap<>();    // 通讯线路id, 预存字节集

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
        super.channelInactive(ctx);
        centerServer.removeChannel(ctx);
        preload.remove(ctx.channel().id().asLongText());
    }

    // 异常连接,自动移除对应的全部订阅
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        centerServer.removeChannel(ctx);
        preload.remove(ctx.channel().id().asLongText());
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object source) throws Exception {
        // 拿到传过来的msg数据，开始处理
        GscCenterPacket respMsg = (GscCenterPacket) source;// 转化为GscCenterPacket
        short eventId = respMsg.getEventId();
        System.out.println("Recv:" + eventId);

        // 收到数据修改请求
        switch (eventId) {
            case Subscribe: // 订阅
                centerServer.pushWork(GscChangeMsg.build(respMsg.getKey(), "subscribe", respMsg.getData(), ctx), 0);
                break;
            case UnSubscribe: // 取消订阅
                centerServer.pushWork(GscChangeMsg.build(respMsg.getKey(), "unsubscribe", respMsg.getData(), ctx), 0);
                break;
            case HeartPing:
                ctx.writeAndFlush(GscCenterPacket.build(respMsg.getKey(), JSONObject.build("status", true), HeartPong, true));
                break;
            case Insert:
                centerServer.pushWork(GscChangeMsg.build(respMsg.getKey(), "insert", respMsg.getData(), ctx), 0);
                break;
            case Update:
                centerServer.pushWork(GscChangeMsg.build(respMsg.getKey(), "update", respMsg.getData(), ctx), 0);
                break;
            case Delete:
                centerServer.pushWork(GscChangeMsg.build(respMsg.getKey(), "delete", respMsg.getData(), ctx), 0);
                break;
            case TestDisconnect:    // 客户端发起断开连接
                ctx.disconnect();
                break;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state.equals(IdleState.READER_IDLE)) {
                System.out.println("长期没收到服务器推送数据");
                ctx.disconnect();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}