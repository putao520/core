package common.java.Apps;

import common.java.HttpServer.HttpContext;

public class AppThreadContext {
    public String MicroServiceName;
    public int AppID;

    private AppThreadContext(int AppID, String MicroServiceName) {
        this.AppID = AppID;
        this.MicroServiceName = MicroServiceName;
    }

    public static AppThreadContext build(HttpContext hCtx) {
        return build(hCtx.appid(), hCtx.serviceName());
    }

    public static AppThreadContext build(int AppID, String MicroServiceName) {
        return new AppThreadContext(AppID, MicroServiceName);
    }
}
