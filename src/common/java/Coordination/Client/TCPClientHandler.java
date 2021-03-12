package common.java.Coordination.Client;

import common.java.Coordination.Common.GscCenterEvent;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.Coordination.Common.payPacket;
import common.java.MasterService.MasterActor;
import common.java.nLogger.nLogger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.json.gsc.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class TCPClientHandler extends ChannelInboundHandlerAdapter {
    private ChannelHandlerContext ctx;
    public static ConcurrentHashMap<String, payPacket> preload = new ConcurrentHashMap<>();    // 通讯线路id, 预存字节集
    public TCPClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        nLogger.errorInfo("掉线了...");
        Channel ch = ctx.channel();
        preload.remove(ch.id().asLongText());
        //使用过程中断线重连
        ch.eventLoop().schedule(() -> {
            TcpClient.build().run();
        }, 2, TimeUnit.SECONDS);
        ctx.close();
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        preload.remove(ctx.channel().id().asLongText());
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object source) throws Exception {
        // 拿到传过来的msg数据，开始处理
        GscCenterPacket respMsg = (GscCenterPacket) source;// 转化为GscCenterPacket
        // 挂载点更新,挂载点内容更新(收到服务端推送过来的数据)
        if (!respMsg.getStatus()) {  // 是执行失败的返回就直接略过
            return;
        }
        // 根据订阅key获得实例对象
        GscCenterClient centerClient = MasterActor.getInstance(respMsg.getKey());
        // 处理收到的广播数据
        switch (respMsg.getEventId()) {
            // 订阅后返回初始值(全局初始化)
            case GscCenterEvent.DataInit:
                // 设置初始化数据
                centerClient.onInit(respMsg.getData());
                // 设置已初始化标志
                centerClient.setLoaded();
                break;
            // 新增N行数据
            case GscCenterEvent.Insert:
                centerClient.onInsert(respMsg.getData());
                break;
            // 更新N行数据
            case GscCenterEvent.Update:
                centerClient.onUpdate(respMsg.getData());
                break;
            // 删除N行数据
            case GscCenterEvent.Delete:
                centerClient.onDelete(respMsg.getData());
                break;
            // 清空数据
            case GscCenterEvent.Clear:
                centerClient.onClear();
                break;
            case GscCenterEvent.HeartPong:
                System.out.println("Pong...");
                break;
        }
    }

    private void Ping(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(GscCenterPacket.build("", JSONObject.build(), GscCenterEvent.HeartPing, true));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state.equals(IdleState.WRITER_IDLE)) {
                System.out.println("长期未向服务器发送数据");
                //发送心跳包
                Ping(ctx);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public TCPClientHandler send(GscCenterPacket packet) {
        this.ctx.writeAndFlush(packet);
        return this;
    }
}
