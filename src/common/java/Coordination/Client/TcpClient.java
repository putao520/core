package common.java.Coordination.Client;

import common.java.Config.Config;
import common.java.Coordination.Common.MessagePacketDecoder;
import common.java.Coordination.Common.MessagePacketEncoder;
import common.java.Thread.ThreadEx;
import common.java.nLogger.nLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TcpClient {
    private final Bootstrap bootstrap;
    private final TCPClientHandler clientHandle;
    private final EventLoopGroup group = new NioEventLoopGroup();
    /**
     * 客户端通道
     */
    private Channel clientChannel;

    private TcpClient() {
        clientChannel = null;
        clientHandle = new TCPClientHandler();
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        //自定义解码器
                        pipeline.addLast(new MessagePacketDecoder());
                        //自定义编码器
                        pipeline.addLast(new MessagePacketEncoder());
                        //自定义Handler
                        pipeline.addLast(clientHandle);

                    }
                });
    }


    public static TcpClient build() {
        return new TcpClient();
    }

    public TCPClientHandler run() {
        try {
            ChannelFuture channelFuture = bootstrap.connect(Config.masterHost, Config.masterPort);
            channelFuture.addListener((ChannelFutureListener) future -> {
                //如果连接成功
                if (future.isSuccess()) {
                    nLogger.errorInfo("客户端[" + channelFuture.channel().localAddress().toString() + "]已连接...");
                    clientChannel = channelFuture.channel();
                }
                //如果连接失败，尝试重新连接
                else {
                    nLogger.errorInfo("客户端[" + channelFuture.channel().localAddress().toString() + "]连接失败，重新连接中...");
                    future.channel().close();
                    bootstrap.connect(Config.masterHost, Config.masterPort);
                }
            });
            //注册关闭事件
            channelFuture.channel().closeFuture().addListener(cfl -> {
                close();
                nLogger.errorInfo("客户端[" + channelFuture.channel().localAddress().toString() + "]已断开...");
            });
        } catch (Exception e) {
            nLogger.errorInfo(e, "连接GscCenter服务器失败!");
        }
        long timeOut = 120 * 100;
        long timeCost = 0;
        while (clientChannel == null && timeCost < timeOut) {
            ThreadEx.SleepEx(10);
            timeCost += 10;
        }
        return this.clientHandle;
    }

    private void close() {
        //关闭客户端套接字
        if (clientChannel != null) {
            clientChannel.close();
        }
        //关闭客户端线程组
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
