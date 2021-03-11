package common.java.Coordination.Server;

import common.java.nLogger.nLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;

public class TcpServer {
    private final int port;
    //处理Accept连接事件的线程，这里线程数设置为1即可，netty处理链接事件默认为单线程，过度设置反而浪费cpu资源
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    //处理handler的工作线程，其实也就是处理IO读写 。线程数据默认为 CPU 核心数乘以2
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    //服务器运行状态
    private volatile boolean isRunning = false;

    private TcpServer(int port) {
        this.port = port;
    }

    public static TcpServer build(int port) {
        return new TcpServer(port);
    }

    private void init() throws Exception {
        //创建ServerBootstrap实例
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //初始化ServerBootstrap的线程组
        serverBootstrap.group(bossGroup, workerGroup);//
        //设置将要被实例化的ServerChannel类
        serverBootstrap.channel(NioServerSocketChannel.class);//
        //在ServerChannelInitializer中初始化ChannelPipeline责任链，并添加到serverBootstrap中
        serverBootstrap.childHandler(new ServerChannelInitializer());
        //标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        // 是否启用心跳保活机机制
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        //绑定端口后，开启监听
        ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
        if (channelFuture.isSuccess()) {
            System.out.println("TCP服务启动 成功---------------");
            isRunning = true;
        }
    }

    /**
     * 服务启动
     */
    public synchronized void run() {
        try {
            this.init();
        } catch (Exception e) {
            nLogger.errorInfo(e, "协调服务启动失败");
        }
    }

    /**
     * 服务关闭
     */
    public synchronized void quit() {
        if (!this.isRunning) {
            throw new IllegalStateException(this.getName() + " 未启动 .");
        }
        this.isRunning = false;
        try {
            Future<?> future = this.workerGroup.shutdownGracefully().await();
            if (!future.isSuccess()) {
                nLogger.errorInfo("workerGroup 无法正常停止");
            }

            future = this.bossGroup.shutdownGracefully().await();
            if (!future.isSuccess()) {
                nLogger.errorInfo("bossGroup 无法正常停止");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(getName() + "服务已经停止...");
    }

    private String getName() {
        return "GscCenter->Server";
    }
}