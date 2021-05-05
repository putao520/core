package common.java.Coordination.Server.Store;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class Store {
    public final Apps apps;
    public final Configs configs;
    public final ConfigTemplate configTemplate;
    public final Services services;
    public final ServicesDeploy servicesDeploy;
    public final Nodes nodes;
    private static Store handle = null;

    private Store(JSONObject data) {
        this.apps = Apps.build(data.getJson("apps"));
        this.configs = Configs.build(data.getJson("configs"));
        this.configTemplate = ConfigTemplate.build(data.getJson("configTemplate"));
        this.services = Services.build(data.getJson("services"));
        this.servicesDeploy = ServicesDeploy.build(data.getJson("servicesDeploy"));
        this.nodes = Nodes.build(data.getJson("nodes"));
    }

    public static Store build(JSONObject data) {
        handle = new Store(data);
        return handle;
    }

    public static Store getInstance() {
        if (handle == null) {
            throw new RuntimeException("Store 对象实例为空，需要订阅协调服务信息后使用");
        }
        return handle;
    }

    public void clear(String key) {
        switch (key) {
            case "nodes":
                nodes.clear();
                break;
            case "apps":
                apps.clear();
                break;
            case "configs":
                configs.clear();
                break;
            case "configTemplate":
                configTemplate.clear();
                break;
            case "services":
                services.clear();
                break;
            case "servicesDeploy":
                servicesDeploy.clear();
                break;
        }
    }

    public JSONObject export(String key) {
        switch (key) {
            case "nodes":
                return nodes.export();
            case "apps":
                return apps.export();
            case "configs":
                return configs.export();
            case "configTemplate":
                return configTemplate.export();
            case "services":
                return services.export();
            case "servicesDeploy":
                return servicesDeploy.export();
            default:
                return JSONObject.build("idx", 0).put("store", JSONArray.build());
        }
    }

    public JSONObject export() {
        return JSONObject.build("nodes", nodes.export())
                .put("apps", apps.export())
                .put("configs", configs.export())
                .put("configTemplate", configTemplate.export())
                .put("services", services.export())
                .put("servicesDeploy", servicesDeploy.export());
    }

    public int insert(String key, JSONObject data) {
        switch (key) {
            case "nodes":
                return nodes.insert(data);
            case "apps":
                return apps.insert(data);
            case "configs":
                return configs.insert(data);
            case "configTemplate":
                return configTemplate.insert(data);
            case "services":
                return services.insert(data);
            case "servicesDeploy":
                return servicesDeploy.insert(data);
            default:
                return -1;
        }
    }

    public void update(String key, JSONObject data) {
        switch (key) {
            case "nodes":
                nodes.update(data);
                break;
            case "apps":
                apps.update(data);
                break;
            case "configs":
                configs.update(data);
                break;
            case "configTemplate":
                configTemplate.update(data);
                break;
            case "services":
                services.update(data);
                break;
            case "servicesDeploy":
                servicesDeploy.update(data);
                break;
        }
    }

    public void delete(String key, int idx) {
        switch (key) {
            case "nodes":
                nodes.remove(idx);
                break;
            case "apps":
                apps.remove(idx);
                break;
            case "configs":
                configs.remove(idx);
                break;
            case "configTemplate":
                configTemplate.remove(idx);
                break;
            case "services":
                services.remove(idx);
                break;
            case "servicesDeploy":
                servicesDeploy.remove(idx);
                break;
        }
    }

    public boolean has(String key) {
        return (!key.equals("nodes") &&
                !key.equals("apps") &&
                !key.equals("configs") &&
                !key.equals("configTemplate") &&
                !key.equals("services") &&
                !key.equals("servicesDeploy"));
    }

    @Override
    public String toString() {
        return export().toString();
    }
}
