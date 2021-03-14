package common.java.Coordination.Client;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterEvent;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.Thread.ThreadEx;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class GscCenterClient {
    private final String rootKey;
    private final HashMap<String, JSONObject> indexMap;
    private final JSONObject store;
    private TcpClient client;
    private boolean isLoaded;

    private GscCenterClient(String rootKey) {
        this.isLoaded = false;
        this.rootKey = rootKey;
        this.store = JSONObject.build();
        this.indexMap = new HashMap<>();
        this.client = TcpClient.build().run();
    }

    public static GscCenterClient build(String rootKey) {
        // 建立连接
        GscCenterClient cls = new GscCenterClient(rootKey);
        return cls;
    }

    // 增加索引字段
    public GscCenterClient addIndex(String indexKey) {
        indexMap.put(indexKey, store.mapsByKey(indexKey));
        return this;
    }

    // 数据变化后重新建立索引
    private void rebuildIndex() {
        for (String indexKey : indexMap.keySet()) {
            indexMap.put(indexKey, store.mapsByKey(indexKey));
        }
    }

    public String getRootKey() {
        return rootKey;
    }

    public JSONArray getData(String className) {
        return store.getJson(className).getJsonArray("store");
    }

    // 根据排序字段获得对应数据(有可能返回null)
    public JSONObject getDataByIndex(String indexString, String indexVal) {
        if (!indexMap.containsKey(indexString)) {   // 隐式新增index
            addIndex(indexString);
        }
        JSONObject val = indexMap.get(indexString);
        return val.getJson(indexVal);
    }

    private void setClient(TcpClient handle) {
        this.client = handle;
    }

    public GscCenterClient waitLoaded() throws TimeoutException {
        int max_length = 100;
        while (!this.isLoaded && max_length > 0) {
            ThreadEx.SleepEx(300);
            max_length--;
        }
        if (!this.isLoaded) {
            throw new TimeoutException("获得订阅初始化数据超时");
        }
        return this;
    }

    public boolean getLoaded() {
        return isLoaded;
    }

    public GscCenterClient setLoaded() {
        isLoaded = true;
        return this;
    }

    private void updateById(JSONArray<JSONObject> local_arr, JSONArray<JSONObject> arr) {
        JSONObject maps = local_arr.mapsByKey("id");
        for (JSONObject v : arr) {
            String nId = v.getString("id");
            // 如果当前存储包含输入项目,更新当前项目
            if (maps.containsKey(nId)) {
                maps.getJson(nId).putAll(v);
            }
            // 如果当前存储不包含输入项目,直接新增数据
            else {
                local_arr.add(v);
            }
        }
    }

    // 初始化数据
    public void onChange(String key, JSONObject data) {
        JSONArray local_arr = store.containsKey(key) ? store.getJsonArray(key) : JSONArray.build();
        updateById(local_arr, data.getJsonArray("data"));
    }

    public void onClear() {
        store.clear();
        indexMap.clear();
    }

    /**
     * @apiNote 订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient subscribe() {
        client.send(GscCenterPacket.build(Config.serviceName, JSONObject.build("node", Config.nodeID), GscCenterEvent.Subscribe, false));
        return this;
    }

    /**
     * @apiNote 取消订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient unSubscribe() {
        client.send(GscCenterPacket.build(Config.serviceName, JSONObject.build("node", Config.nodeID), GscCenterEvent.UnSubscribe, false));
        return this;
    }

    public GscCenterClient disconnect() {
        client.send(GscCenterPacket.build(Config.serviceName, JSONObject.build("node", Config.nodeID), GscCenterEvent.TestDisconnect, false));
        return this;
    }

    public GscCenterClient insert(String className, JSONObject data) {
        client.send(GscCenterPacket.build(Config.serviceName,
                JSONObject.build("data", data).puts("name", className)
                , GscCenterEvent.Insert, false));
        return this;
    }

    public GscCenterClient update(String className, JSONObject data) {
        client.send(GscCenterPacket.build(Config.serviceName,
                JSONObject.build("data", data).puts("name", className)
                , GscCenterEvent.Update, false));
        return this;
    }

    public GscCenterClient delete(String className, JSONObject data) {
        client.send(GscCenterPacket.build(Config.serviceName,
                JSONObject.build("data", data).puts("name", className)
                , GscCenterEvent.Delete, false));
        return this;
    }

    public void close() {
        client.close();
    }
}
