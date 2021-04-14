package common.java.Coordination.Client;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterEvent;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.MasterProxy.MasterActor;
import common.java.Thread.ThreadHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GscCenterClient {
    private final HashMap<String, JSONObject> indexMap;
    private final JSONObject store;
    private TcpClient client;
    private final AtomicInteger loadCnt;
    private boolean liveStatus;
    private boolean keepLived;

    private GscCenterClient() {
        this.liveStatus = false;
        this.keepLived = false;
        this.loadCnt = new AtomicInteger(0);
        this.store = JSONObject.build();
        this.indexMap = new HashMap<>();
        this.client = TcpClient.build(this).run();
    }

    public static GscCenterClient build() {
        // 建立连接
        GscCenterClient cls = new GscCenterClient();
        return cls;
    }

    public GscCenterClient reConnect() {
        if (this.keepLived) {
            this.client.close().run();
        }
        return this;
    }

    public boolean getLiveStatus() {
        return this.liveStatus;
    }

    public GscCenterClient setLiveStatus(boolean status) {
        this.liveStatus = status;
        return this;
    }

    public boolean getKeepLived() {
        return this.keepLived;
    }

    public GscCenterClient setKeepLived(boolean keepLived) {
        this.keepLived = keepLived;
        return this;
    }

    public void waitLived() {
        int err_no = 100;
        while (!this.liveStatus && err_no > 10) {
            ThreadHelper.sleep(100);
            err_no--;
        }
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

    public JSONArray getData(String className) {
        return store.getJsonArray(className);
    }

    private void setClient(TcpClient handle) {
        this.client = handle;
    }

    // 获得等待
    public void getResponse() {
        if (loadCnt.get() > 0) {
            loadCnt.set(loadCnt.decrementAndGet());
        }
    }

    public void setResponse(int no) {
        loadCnt.addAndGet(no);
    }

    // 需要等待
    public void waitResponse() {
        int max_errno = 100;
        while (loadCnt.get() > 0 && max_errno > 0) {
            ThreadHelper.sleep(100);
            max_errno--;
        }
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
        if (!store.containsKey(key)) {
            store.put(key, JSONArray.build());
        }
        updateById(store.getJsonArray(key), data.getJsonArray("data"));
        // 刷新maps后的对应对象缓存数据
        MasterActor.updateAll(key);
    }

    public void onClear() {
        store.clear();
        indexMap.clear();
    }

    /**
     * @apiNote 订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient subscribe() {
        this.setResponse(3);
        client.send(GscCenterPacket.build(Config.serviceName, JSONObject.build("node", Config.nodeID), GscCenterEvent.Subscribe, false));
        this.waitResponse();
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
        this.setKeepLived(false);
        client.close();
    }
}
