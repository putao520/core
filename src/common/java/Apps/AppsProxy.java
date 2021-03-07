package common.java.Apps;

import common.java.Database.DbFilter;
import common.java.MasterService.MasterProxy;
import common.java.Rpc.RpcResponse;
import common.java.ServiceTemplate.MasterServiceName;
import org.json.gsc.JSONObject;

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
            info = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.Application).find("id", String.valueOf(appid))).asJson();
        }
        return info;
    }

    public static JSONObject getAppInfo(String domain) {
        JSONObject info = JSONObject.toJSON(appMapped.getData(domainMapped.getData(domain)));
        if (info == null) {
            info = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.Application).find("domain", domain)).asJson();
        }
        return info;
    }

    // 获得微服务信息
    public static JSONObject getServiceInfo(int appid, String serviceName) {
        JSONObject info = JSONObject.toJSON(serviceMapped.getData(serviceName));
        if (info == null) {
            info = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.MicroService).findEx(DbFilter.buildDbFilter().eq("serviceName", serviceName).eq("appid", appid).build().toString())).asJson();
        }
        return info;
    }
}
