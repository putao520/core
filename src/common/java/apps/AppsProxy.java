package common.java.apps;

import common.java.Coordination.PathMapped;
import common.java.master.MasterProxy;
import common.java.rpc.RpcResponse;
import common.java.serviceHelper.MasterServiceName;
import org.json.simple.JSONObject;

public class AppsProxy {
    private static final String appPath = "/apps";
    private static final PathMapped appMapped = PathMapped.build().mapped(appPath);
    private static final String domainPath = "/domian";
    private static final PathMapped domainMapped = PathMapped.build().mapped(domainPath);
    private static final String servicePath = "/services";
    private static final PathMapped serviceMapped = PathMapped.build().mapped(servicePath);

    // 获得应用信息
    public static JSONObject getAppInfo(int appid) {
        JSONObject info = JSONObject.toJSON(appMapped.getData(String.valueOf(appid)));
        if (info == null) {
            info = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.Appliction).find("id", String.valueOf(appid))).asJson();
        }
        return info;
    }

    public static JSONObject getAppInfo(String domain) {
        JSONObject info = JSONObject.toJSON(appMapped.getData(domainMapped.getData(domain)));
        if (info == null) {
            info = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.Appliction).find("domain", domain)).asJson();
        }
        return info;
    }

    // 获得微服务信息
    public static JSONObject getServiceInfo(String serviceName) {
        JSONObject info = JSONObject.toJSON(serviceMapped.getData(serviceName));
        if (info == null) {
            info = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.MicroService).find("serviceName", serviceName)).asJson();
        }
        return info;
    }
}
