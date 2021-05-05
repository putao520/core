package common.java.Coordination.Client.Store;

import org.json.gsc.JSONObject;

public class App {
    private final JSONObject store;               // 当前应用对应配置信息
    private JSONObject serviceInfo = null;     // 当前应用绑定本微服务的配置信息

    private App(JSONObject data) {
        store = data;
    }

    public static App build(JSONObject data) {
        return new App(data);
    }

    public JSONObject getStore() {
        return store;
    }

    // 设置到微服务信息到当前应用
    public App setService(JSONObject servInfo) {
        serviceInfo = servInfo;
        return this;
    }

    // 获得当前应用对应微服务配置信息
    public JSONObject getServiceInfo() {
        return this.serviceInfo;
    }

    public boolean check(String key, Object val) {
        return store.has(key) && store.get(key).equals(val);
    }

}
