package common.java.common;

import common.java.httpServer.HttpContext;

public class BrowerHelper {
    public static boolean isMicroMessenger() {
        String agent = HttpContext.current().agent();
        return (agent != null && agent.contains("MicroMessenger"));
    }
}
