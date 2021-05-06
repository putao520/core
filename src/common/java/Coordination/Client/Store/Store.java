package common.java.Coordination.Client.Store;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;

/**
 * 客户端上下文<视图>
 * <p>
 * 应用上下文组
 * -应用元数据
 * -对应服务上下文
 * -服务元数据
 * 配置上下文组（同时服务微服务和应用）
 * -配置元数据
 */

public class Store {
    public static final HashMap<Integer, App> apps;   // 当前微服务所涉及到的所有应用的上下文
    public static JSONObject configs;           // 当前微服务所涉及到的所有配置的上下文 配置名：配置内容

    static {
        apps = new HashMap<>();
    }

    private Store() {
    }

    public static Store build() {
        return new Store();
    }

    public void clear() {
        configs.clear();
        apps.clear();
    }

    public boolean put(String className, JSONObject info) {
        switch (className) {
            case "apps":
                apps.put(info.getInt("id"), App.build(info));
                return false;
            case "services":
                if (!info.containsKey("appId")) {
                    throw new RuntimeException("错误微服务配置信息 ->应用Id丢失!");
                }
                int appId = info.getInt("appId");
                if (!apps.containsKey(appId)) {
                    throw new RuntimeException("错误微服务配置信息 ->应用Id[" + appId + "]! ->当前上下文不存在");
                }
                apps.get(appId).setService(info);
                return false;
            case "configs":
                if (!info.containsKey("name")) {
                    throw new RuntimeException("错误配置信息 ->配置名称丢失!");
                }
                configs.put(info.getString("name"), info);
                return false;
        }
        return true;
    }

    public boolean put(String className, JSONArray<JSONObject> infoArr) {
        for (JSONObject v : infoArr) {
            if (this.put(className, v)) {
                return false;
            }
        }
        return true;
    }

    public JSONObject find(String className, String key, String val) {
        switch (className) {
            case "apps":
                for (App app : apps.values()) {
                    if (app.check(key, val)) {
                        return app.getStore();
                    }
                }
                break;
            case "services":
                for (App app : apps.values()) {
                    var serviceInfo = app.getServiceInfo();
                    if (serviceInfo.check(key, val)) {
                        return serviceInfo;
                    }
                }
                break;
            case "configs":
                for (String k : configs.keySet()) {
                    var configInfo = configs.getJson(k);
                    if (configInfo.check(key, val)) {
                        return configInfo;
                    }
                }
                break;
        }
        return null;
    }

    public JSONArray<JSONObject> find(String className) {
        var r = JSONArray.<JSONObject>build();
        switch (className) {
            case "apps":
                for (App app : apps.values()) {
                    r.put(app.getStore());
                }
                break;
            case "services":
                for (App app : apps.values()) {
                    var serviceInfo = app.getServiceInfo();
                    r.put(serviceInfo);
                }
                break;
            case "configs":
                for (String k : configs.keySet()) {
                    var configInfo = configs.getJson(k);
                    r.put(configInfo);
                }
                break;
        }
        return r;
    }
}
