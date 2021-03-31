package common.java.GscCommon;

import common.java.HttpServer.HttpContext;

public class BrowserHelper {
    public static boolean isMicroMessenger() {
        String agent = HttpContext.current().agent();
        return (agent != null && agent.contains("MicroMessenger"));
    }
}
