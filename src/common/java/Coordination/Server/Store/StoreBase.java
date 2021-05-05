package common.java.Coordination.Server.Store;

import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreBase {
    private final AtomicInteger index;
    private JSONArray<JSONObject> dataArr;
    private JSONObject dataMap;

    public StoreBase(JSONObject block) {
        this.index = new AtomicInteger(block.getInt("idx"));
        this.dataArr = block.getJsonArray("store");
    }

    private void updateIndex() {
        this.dataMap = dataArr.mapsByKey("id");
    }

    public JSONArray<JSONObject> find(String key, Object val) {
        JSONArray<JSONObject> r = JSONArray.build();
        for (JSONObject v : dataArr) {
            if (v.check(key, val)) {
                r.put(v);
            }
        }
        return r;
    }

    // 查找数据
    public JSONObject find(int idx) {
        return dataMap.getJson(String.valueOf(idx));
    }

    // 更新数据
    public void update(JSONObject json) {
        String idx = json.getString("id");
        if (StringHelper.isInvalided(idx)) {
            throw new RuntimeException("非法数据");
        }
        dataMap.getJson(idx).put(json);
    }

    // 获得数据
    public JSONArray<JSONObject> getDataArr() {
        return this.dataArr;
    }

    // 获得数据map
    public JSONObject getDataMap() {
        return this.dataMap;
    }

    // 获得数据map
    public JSONObject getDataMap(String key) {
        return this.dataMap.mapsByKey(key);
    }

    // 删除数据
    public void remove(int idx) {
        dataArr = dataArr.<String>filter("id", v -> Integer.parseInt(v) != idx);
    }

    // 插入数据
    public int insert(JSONObject data) {
        int v = index.incrementAndGet();
        this.dataArr.add(data.put("id", v));
        this.updateIndex();
        return v;
    }

    // 清空数据
    public void clear() {
        this.dataArr.clear();
        this.dataMap.clear();
        this.index.set(0);
    }

    // 返回数据原始结构
    protected JSONArray<JSONObject> pureData() {
        return this.dataArr;
    }

    // 检查配置是否有效
    public boolean checkConfigs(Collection<Object> cfgArr) {
        // 判断APP配置是否有效
        var cfgMap = Store.getInstance().configs.pureData().mapsByKey("name");
        for (Object v : cfgArr) {
            // 数据数据包含不存在的配置名，报错
            if (!cfgMap.has(StringHelper.toString(v))) {
                return true;
            }
        }
        return false;
    }

    // 检查配置模板是否有效
    public boolean checkConfigTemplate(int templateId) {
        // 判断配置的配置模板是否有效
        return Store.getInstance()
                .configTemplate
                .getDataMap()
                .has(String.valueOf(templateId));
    }

    // 检查应用是否有效
    public boolean checkApp(int appId) {
        return Store.getInstance()
                .apps
                .getDataMap()
                .has(String.valueOf(appId));
    }

    // 检查服务是否有效
    public boolean checkService(int serviceId) {
        return Store.getInstance()
                .services
                .getDataMap()
                .has(String.valueOf(serviceId));
    }

    // 输出json
    public JSONObject export() {
        return JSONObject.build("idx", this.index.intValue())
                .put("store", this.dataArr);
    }
}
