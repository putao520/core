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

import java.util.concurrent.TimeUnit;

public class TcpClient {
    public static TcpClient handle = null;
    private Bootstrap bootstrap;
    private final EventLoopGroup group = new NioEventLoopGroup();
    /**
     * 客户端通道
     */
    private Channel clientChannel;
    private TCPClientHandler clientHandle;

    private TcpClient() {

    }

    /**
     * @apiNote 获得TcpClient实例 ->单例模式
     */
    public static TcpClient build() {
        if (handle == null) {
            handle = new TcpClient();
        }
        return handle;
    }

    private void init() {
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
                        pipeline.addLast(new MessagePacketDecoder(TCPClientHandler.preload));
                        //自定义编码器
                        pipeline.addLast(new MessagePacketEncoder());
                        //自定义Handler
                        pipeline.addLast(clientHandle);
                    }
                });
    }

    public TcpClient run() {
        if (clientChannel != null) {
            clientChannel.close();
            clientChannel = null;
        }
        init();
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
                    future.channel().close().channel().eventLoop().schedule(() -> {
                        run();
                    }, 3, TimeUnit.SECONDS);
                }
            });
            //注册关闭事件
            /*
            channelFuture.channel().closeFuture().addListener(cfl -> {
                nLogger.errorInfo("客户端[" + channelFuture.channel().localAddress().toString() + "]已断开...服务器主动断开");
                // close();
            });
            */
        } catch (Exception e) {
            nLogger.errorInfo(e, "连接GscCenter服务器失败!");
        }
        long timeOut = 120 * 100;
        long timeCost = 0;
        while (clientChannel == null && timeCost < timeOut) {
            ThreadEx.SleepEx(10);
            timeCost += 10;
        }
        return this;
    }

    public TCPClientHandler getHandle() {
        return this.clientHandle;
    }

    public void close() {
        System.out.println("客户端[关闭连接]!!!");
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
