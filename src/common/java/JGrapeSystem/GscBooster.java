package common.java.JGrapeSystem;

import common.java.Apps.MicroServiceContext;
import common.java.Config.Config;
import common.java.HttpServer.GscServer;
import common.java.HttpServer.HttpContext;
import common.java.MasterProxy.MasterActor;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.ChannelHandlerContext;

public class GscBooster {
    private static void _before(String serverName) {
        boolean debugStatus = MicroServiceContext.current().isDebug();
        // 设置日志回调
        nLogger.setDebug(debugStatus);
        nLogger.clientFunc = (info, type) -> {
            HttpContext context = HttpContext.current();
            if (context == null) {
                return;
            }
            int appid = context.appid();
            ChannelHandlerContext ctx = context.channelContext();
            String printInfo = "时间:[" + TimeHelper.build().nowDatetime() + "]-"
                    + "应用:[" + appid + "]-"
                    + "级别:[" + type.toString() + "]-"
                    + "线程:[" + Thread.currentThread().getId() + "]\n"
                    + "信息:\n" + info + "\n"
                    + "============================";
            System.out.println(printInfo);
            if (MicroServiceContext.current().isDebug()) {
                HttpContext.showMessage(ctx, printInfo);
            }
        };
        // 获得端口
        System.out.println("节点号:[" + Config.nodeID + "]");
        System.out.println("微服务:[" + serverName + "] ...启动完毕");
        System.out.println("监听:" + Config.bindIP + ":" + Config.port + " ...成功");
        // 设置本地服务名
        System.setProperty("AppName", serverName);
        if (debugStatus) {
            System.out.println("调试模式:开");
        }
    }

    public static void start() {
        start(Config.serviceName);
    }

    public static void start(String serverName) {
        try {
            // 此时订阅全部用到的数据
            MasterActor.getClient().subscribe();
            // 设置日志过滤器
            _before(serverName);
            // 启动http服务
            GscServer.start(Config.bindIP, Config.port);
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {

        }
    }
}
