package common.java.JGrapeSystem;

import common.java.Config.nConfig;
import common.java.httpServer.GscServer;
import common.java.httpServer.HttpContext;
import common.java.nlogger.nlogger;
import common.java.node.NodeManage;
import common.java.time.TimeHelper;
import io.netty.channel.ChannelHandlerContext;

public class GscBooter {
    public static void start() {
        startServer(nConfig.netConfig("name"), false);
    }

    public static void start(String serverName) {
        startServer(serverName, false);
    }

    public static void startMaster() {
        startServer("GrapeFW", true);
    }

    private static void _before(String serverName, boolean master) {
        // 设置日志回调
        nlogger.setDebug(nConfig.debug);
        nlogger.clientFunc = (info, type) -> {
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
            if (nConfig.debug) {
                HttpContext.showMessage(ctx, printInfo);
            }
        };
        // 获得端口
        // 注册服务节点(非master节点)
        if (!master) {
            NodeManage.addNode();
            System.out.println("节点号:[" + nConfig.nodeID + "]");
            System.out.println("微服务:[" + serverName + "] ...启动完毕");
        } else {
            System.out.println("GrapeFW主控端 ...启动");
        }
        System.out.println("监听:" + nConfig.bindip + ":" + nConfig.port + " ...成功");
        // 设置本地服务名
        System.setProperty("AppName", serverName);
        if (nConfig.debug) {
            System.out.println("调试模式:开");
        }
    }

    private static void startServer(String serverName, boolean master) {
        try {
            _before(serverName, master);
            // 启动http服务
            GscServer.start(nConfig.bindip, nConfig.port);
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {

        }
    }
}
