package common.java.Coordination.Client;

import common.java.Coordination.Common.GscCenterEvent;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.MasterService.MasterActor;
import common.java.nLogger.nLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.json.gsc.JSONObject;

import java.util.concurrent.TimeUnit;


public class TCPClientHandler extends ChannelInboundHandlerAdapter {
    private ChannelHandlerContext ctx;

    public TCPClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        nLogger.errorInfo("掉线了...");
        //使用过程中断线重连
        final EventLoop eventLoop = ctx.channel().eventLoop();
        eventLoop.schedule(() -> TcpClient.build().run(), 1L, TimeUnit.SECONDS);
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object source) throws Exception {
        // 拿到传过来的msg数据，开始处理
        GscCenterPacket respMsg = (GscCenterPacket) source;// 转化为GscCenterPacket
        // 挂载点更新,挂载点内容更新(收到服务端推送过来的数据)
        if (respMsg.getStatus()) {  // 是执行成功的返回就直接略过
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
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                System.out.println("长期没收到服务器推送数据");
                //可以选择重新连接
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                System.out.println("长期未向服务器发送数据");
                //发送心跳包
                ctx.writeAndFlush(GscCenterPacket.build("", JSONObject.build(), GscCenterEvent.HeartPing, true));
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                System.out.println("ALL");
            }
        }
    }

    public TCPClientHandler send(GscCenterPacket packet) {
        this.ctx.writeAndFlush(packet);
        return this;
    }
}
