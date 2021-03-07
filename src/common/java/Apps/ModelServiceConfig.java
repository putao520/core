package common.java.Apps;

import org.json.gsc.JSONObject;

public class ModelServiceConfig {
    private JSONObject store;

    public ModelServiceConfig(JSONObject store) {
        this.store = store;
    }

    public static ModelServiceConfig buildModelServiceConfig(JSONObject info) {
        return new ModelServiceConfig(info);
    }

    /**
     * 获得数据库配置
     */
    public String db() {
        return store == null ? null : this.store.getString("db");
    }

    /**
     * 获得缓存配置
     */
    public String cache() {
        return store == null ? null : this.store.getString("cache");
    }

    /**
     * 获得队列配置
     */
    public JSONObject mq() {
        return store == null ? null : this.store.getJson("mq");
    }

    /**
     * 获得存储配置
     */
    public String store() {
        return store == null ? null : this.store.getString("store");
    }

    /**
     * 获得其他自定义配置
     */
    public String getOtherConfig(String ConfigKey) {
        JSONObject otherInfo = store == null ? null : this.store.getJson("other");
        return otherInfo == null ? null : otherInfo.getString(ConfigKey);
    }

    /**
     * 填充新的其他配置项
     */
    public ModelServiceConfig otherConfig(JSONObject info) {
        this.store.put("other", info);
        return this;
    }

    /**
     * 获得other全部配置
     */
    public JSONObject otherConfig() {
        return this.store.getJson("other");
    }

    public ModelServiceConfig putOtherConfig(String key, Object value) {
        if (JSONObject.isInvalided(this.store)) {
            this.store = new JSONObject();
            this.store.put("other", new JSONObject());
        }
        this.store.getJson("other").put(key, value);
        return this;
    }

    public ModelServiceConfig removeOtherConfig(String key) {
        if (JSONObject.isInvalided(this.store)) {
            this.store = new JSONObject();
            this.store.put("other", new JSONObject());
        } else {
            this.store.getJson("other").remove(key);
        }
        return this;
    }

    public JSONObject toJson() {
        return this.store;
    }
}
