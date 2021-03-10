package common.java.Apps;

import common.java.Coordination.Client.GscCenterClient;
import common.java.MasterService.MasterActor;
import org.json.gsc.JSONObject;

public class AppsProxy {
    private static final String appPath = "apps";
    private static final GscCenterClient appMapped = MasterActor.getInstance(appPath);
    private static final String servicePath = "services";
    private static final GscCenterClient serviceMapped = MasterActor.getInstance(servicePath);

    // 获得应用信息
    public static JSONObject getAppInfo(int appId) {
        JSONObject info = appMapped.getDataByIndex("id", String.valueOf(appId));
        if (info == null) {
            throw new RuntimeException("当前应用id[" + appId + "]无效!");
        }
        return info;
    }

    public static JSONObject getAppInfo(String domain) {
        JSONObject info = appMapped.getDataByIndex("domain", domain);
        if (info == null) {
            throw new RuntimeException("当前域名[" + domain + "]未绑定!");
        }
        return info;
    }

    // 获得微服务信息
    public static JSONObject getServiceInfo(int appId, String serviceName) {
        JSONObject info = serviceMapped.getDataByIndex("appId", String.valueOf(appId));
        if (info == null) {
            throw new RuntimeException("当前服务[" + serviceName + "]未部署在应用[" + appId + "]!");
        }
        return info;
    }
}