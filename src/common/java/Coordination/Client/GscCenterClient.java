package common.java.Coordination.Client;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterEvent;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.Thread.ThreadEx;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

public class GscCenterClient {
    private final String rootKey;
    private final HashMap<String, JSONObject> indexMap;
    private JSONArray<JSONObject> store;
    private TcpClient client;
    private boolean isLoaded;

    private GscCenterClient(String rootKey) {
        this.isLoaded = false;
        this.rootKey = rootKey;
        this.store = JSONArray.build();
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

    public JSONArray getData() {
        return store;
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

    public GscCenterClient load(JSONArray data) {
        this.store = data;
        return this;
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

    // 初始化数据
    public void onInit(JSONObject data) {
        onClear();
        onInsert(data);
    }

    // 新增数据
    public void onInsert(JSONObject data) {
        store.addAll(data.getJsonArray("data"));
        rebuildIndex();
    }

    // 更新数据(查询条件是2个集合字段值相等)
    public void onUpdate(JSONObject data) {
        String queryKey = data.getString("query");
        JSONArray<JSONObject> content = data.getJsonArray("data");
        for (JSONObject json : content) {
            for (JSONObject item : store) {
                if (item.get(queryKey).equals(json.get(queryKey))) {
                    item.putAll(json);
                }
            }
        }
        rebuildIndex();
    }

    // 删除数据(查询条件是2个集合字段值相等)
    public void onDelete(JSONObject data) {
        String queryKey = data.getString("query");
        JSONArray<JSONObject> content = data.getJsonArray("data");
        for (JSONObject json : content) {
            Iterator<JSONObject> it = store.iterator();
            while (it.hasNext()) {
                JSONObject item = it.next();
                if (item.get(queryKey).equals(json.get(queryKey))) {
                    it.remove();
                }
            }
        }
        rebuildIndex();
    }

    public void onClear() {
        store.clear();
        indexMap.clear();
    }

    /**
     * @apiNote 订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient subscribe() {
        client.getHandle().send(GscCenterPacket.build(rootKey, JSONObject.build("name", Config.serviceName).puts("node", Config.nodeID), GscCenterEvent.Subscribe, false));
        return this;
    }

    /**
     * @apiNote 取消订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient unSubscribe() {
        client.getHandle().send(GscCenterPacket.build(rootKey, JSONObject.build("name", Config.serviceName).puts("node", Config.nodeID), GscCenterEvent.UnSubscribe, false));
        return this;
    }

    public GscCenterClient disconnect() {
        client.getHandle().send(GscCenterPacket.build(rootKey, JSONObject.build("name", Config.serviceName).puts("node", Config.nodeID), GscCenterEvent.TestDisconnect, false));
        return this;
    }

    public void close() {
        client.close();
    }
}
