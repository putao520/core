package common.java.Coordination.Server;

import common.java.Coordination.Common.GscCenterPacket;
import common.java.File.FileText;
import common.java.String.StringHelper;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static common.java.Coordination.Common.GscCenterEvent.DataInit;


public class GscCenterServer {
    private static final HashMap<String, GscCenterServer> handle = new HashMap<>();
    private static final HashMap<String, HashMap<String, ChannelHandlerContext>> nodeArr = new HashMap<>();
    private static final ConcurrentHashMap<String, GscChangeMsg> work_queue = new ConcurrentHashMap<>();
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    // 获得该对象时,必须确保并发安全!!!(目前不安全)
    private final JSONObject store;
    /**
     * keyA:[]
     * keyB:[]
     * keyC:[]
     */
    private final HashMap<String, Set<ChannelHandlerContext>> subscribeQueue = new HashMap<>();

    private GscCenterServer() {
        this.store = JSONObject.build();
    }
    private GscCenterServer(JSONObject data) {
        this.store = data;
    }

    private GscCenterServer(String path) {
        this.store = JSONObject.build(FileText.build(path).readString());
    }

    public static GscCenterServer getInstance(Path path) {
        String pathStr = path.toAbsolutePath().toString();
        if (!handle.containsKey(pathStr)) {
            handle.put(pathStr, GscCenterServer.load(pathStr));
        }
        return handle.get(pathStr);
    }

    /**
     * @apiNote 载入初始化数据
     */
    public static GscCenterServer load(JSONObject data) {
        return new GscCenterServer(data);
    }

    public static GscCenterServer load(String path) {
        return new GscCenterServer(path);
    }

    public boolean workerRunner(GscChangeMsg msg) {
        switch (msg.getAction()) {
            case "insert":
                return onInsert(msg.getServiceName(), msg.getData(), msg.getChannel());
            case "update":
                return onUpdate(msg.getServiceName(), msg.getData(), msg.getChannel());
            case "delete":
                return onDelete(msg.getServiceName(), msg.getData(), msg.getChannel());
            case "subscribe":
                subscribe(msg.getServiceName(), msg.getData(), msg.getChannel());
                return true;
            case "unsubscribe":
                unSubscribe(msg.getServiceName(), msg.getData(), msg.getChannel());
                return true;
            default:
                return true;
        }
    }

    public void pushWork(GscChangeMsg msg, int errorNo) {
        if (errorNo < 3) {
            worker.submit(() -> {
                if (!workerRunner(msg)) {
                    pushWork(msg, errorNo + 1);
                }
            });
        }
    }

    /**
     * @apiNote 导出全部内容
     */
    public JSONObject export() {
        return this.store;
    }

    /**
     * @apiNote 按挂载点导出内容
     */
    public JSONObject export(String key) {
        return store.getJson(key);
    }

    public GscCenterServer clear(String key) {
        store.getJsonArray(key).clear();
        return this;
    }

    private void ReturnChannelMsg(String serviceName,           // 微服务名称
                                  String key,                   // 分类名称
                                  JSONArray resultArr,          // 返回结果集合
                                  ChannelHandlerContext ctx) {
        // 没包含特定网络通道
        if (ctx == null) {
            if (subscribeQueue.containsKey(serviceName)) {
                var queue = subscribeQueue.get(serviceName);
                for (ChannelHandlerContext _ctx : queue) {
                    _ctx.writeAndFlush(GscCenterPacket.build(
                            key,
                            JSONObject.build("data", resultArr),
                            DataInit,
                            true
                    ));
                }
            }
        } else {
            ctx.writeAndFlush(GscCenterPacket.build(
                    key,
                    JSONObject.build("data", resultArr),
                    DataInit,
                    true
            ));
        }
    }

    private void registerNode(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        // 注册节点到节点表
        String nodeId = data.getString("node");         // 获得节点Id
        if (StringHelper.isInvalided(nodeId)) {
            return;
        }
        var queue = nodeArr.getOrDefault(serviceName, new HashMap<>());
        queue.put(nodeId, ctx);
    }

    private void pushConfigName(HashMap<String, JSONObject> configNameArr, JSONObject config, String name, JSONObject configStore) {
        if (config.containsKey(name)) {
            configNameArr.put(name, configStore.getJson(name));
        }
    }

    private JSONObject findServiceInfoByName(String serviceName) {
        // 根据服务名获得服务信息
        return (JSONObject) store.getJson("services").getJsonArray("store").mapsByKey("name").get(serviceName, null);
    }

    private JSONArray<JSONObject> findApps4AppIds(Set<String> idArr) {
        JSONArray<JSONObject> result = JSONArray.build();
        JSONObject appsMap = store.getJson("apps").getJsonArray("store").mapsByKey("id");
        for (String id : idArr) {
            result.add(appsMap.getJson(id));
        }
        return result;
    }

    private String getServiceIdByName(String serviceName) {
        JSONObject serviceInfo = findServiceInfoByName(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            return null;
        }
        return serviceInfo.getString("id");
    }

    /**
     * @param serviceName 微服务名称
     * @apiNote 查找与service有关的应用上下文
     */
    private JSONArray<JSONObject> findApps4ServiceName(String serviceName) {
        String serviceId = getServiceIdByName(serviceName);
        if (StringHelper.isInvalided(serviceId)) {
            return null;
        }
        Set<String> appIds = new HashSet<>();
        // 根据部署表获得
        JSONArray<JSONObject> deployArr = store.getJson("servicesDeploy").getJsonArray("store");
        for (JSONObject v : deployArr) {
            if (v.getString("serviceId").equals(serviceId)) {
                appIds.add(v.getString("appId"));
            }
        }
        return findApps4AppIds(appIds);
    }

    private JSONArray<JSONObject> findServices4ServiceName(String serviceName) {
        JSONObject serviceInfo = findServiceInfoByName(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            return null;
        }
        String serviceId = serviceInfo.getString("id");
        // 根据部署表获得聚合微服务信息
        JSONArray<JSONObject> result = JSONArray.build();
        JSONArray<JSONObject> deployArr = store.getJson("servicesDeploy").getJsonArray("store");
        for (JSONObject v : deployArr) {
            if (v.getString("serviceId").equals(serviceId)) {
                result.add(JSONObject.build(v).putAlls(serviceInfo));
            }
        }
        return result;
    }

    private JSONArray<JSONObject> findConfigs4ServiceName(String serviceName) {
        String serviceId = getServiceIdByName(serviceName);
        if (StringHelper.isInvalided(serviceId)) {
            return null;
        }
        // 根据部署表获得配置聚合信息
        JSONObject configMaps = store.getJson("configs").getJsonArray("store").mapsByKey("name");
        HashMap<String, JSONObject> configNameMaps = new HashMap<>();
        // 根据部署表获得
        JSONArray<JSONObject> deployArr = store.getJson("servicesDeploy").getJsonArray("store");
        for (JSONObject v : deployArr) {
            if (v.getString("serviceId").equals(serviceId)) {
                JSONObject config = v.getJson("config");
                if (JSONObject.isInvalided(config)) {
                    continue;
                }
                // 数据库
                pushConfigName(configNameMaps, config, "db", configMaps);
                // 缓存
                pushConfigName(configNameMaps, config, "cache", configMaps);
                // 队列
                pushConfigName(configNameMaps, config, "mq", configMaps);
                // 存储
                pushConfigName(configNameMaps, config, "store", configMaps);
                // 流式计算
                pushConfigName(configNameMaps, config, "streamComputer", configMaps);
                // 批处理计算
                pushConfigName(configNameMaps, config, "blockComputer", configMaps);
            }
        }
        return JSONArray.build(configNameMaps.values());
    }

    private void PushAll(String serviceName, ChannelHandlerContext ctx) {
        ReturnChannelMsg(serviceName, "apps", findApps4ServiceName(serviceName), ctx);
        ReturnChannelMsg(serviceName, "services", findServices4ServiceName(serviceName), ctx);
        ReturnChannelMsg(serviceName, "configs", findConfigs4ServiceName(serviceName), ctx);
    }

    /**
     * @param serviceName 订阅微服务名称
     * @param data        附带数据{ name: 服务名, node:节点id }
     * @apiNote 订阅挂载点数据变更
     */
    public void subscribe(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        // 注册计算节点
        registerNode(serviceName, data, ctx);
        // 下发与service有关数据
        PushAll(serviceName, ctx);
        // 添加订阅到队列
        getSubscribeChannel(serviceName).add(ctx);
    }

    /**
     * @param serviceName 微服务名称
     * @param ctx         网络通道
     * @apiNote 取消订阅挂载点数据变更
     */
    public GscCenterServer unSubscribe(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        String nodeId = data.getString("node");
        getSubscribeChannel(serviceName).remove(ctx);
        store.getJson("nodes").remove(nodeId);
        return this;
    }

    /**
     * @apiNote 从所有订阅记录中删除对应通道
     */
    public GscCenterServer removeChannel(ChannelHandlerContext ctx) {
        for (Set<ChannelHandlerContext> ctxArray : subscribeQueue.values()) {
            ctxArray.remove(ctx);
        }
        return this;
    }

    private Set<ChannelHandlerContext> getSubscribeChannel(String serviceName) {
        return subscribeQueue.getOrDefault(serviceName, new HashSet<>());
    }

    private String updateIndex(JSONObject info, JSONObject data) {
        String nIdx = String.valueOf(Integer.valueOf(info.getString("idx")) + 1);
        data.put("id", nIdx);
        info.put("idx", nIdx);
        return nIdx;
    }

    private void onChange(String serviceName, String className, ChannelHandlerContext ctx) {
        // 添加数据, 根据修改后数据触发返回
        switch (className) {
            case "apps":
                ReturnChannelMsg(serviceName, "apps", findApps4ServiceName(serviceName), ctx);
                break;
            case "services":
                ReturnChannelMsg(serviceName, "services", findServices4ServiceName(serviceName), ctx);
                break;
            case "configs":
                ReturnChannelMsg(serviceName, "configs", findConfigs4ServiceName(serviceName), ctx);
                break;
            case "serviceDeploy":
                PushAll(serviceName, ctx);
        }
    }

    /**
     * @param serviceName 微服务名称
     * @param data        添加数据
     * @apiNote 客户端新增数据时
     */
    public boolean onInsert(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        // 获得分类
        String className = data.getString("name");
        if (!store.containsKey(className)) {
            return false;
        }
        JSONObject insertData = data.getJson("data");
        JSONObject info = store.getJson(className);
        // 更新idx更新数据
        updateIndex(info, insertData);
        info.getJsonArray("store").add(insertData);

        onChange(serviceName, className, ctx);
        return true;
    }

    /**
     * @param serviceName 微服务名称
     * @param data        添加数据
     * @apiNote 客户端要求更新数据
     */
    public boolean onUpdate(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        // 获得分类
        String className = data.getString("name");
        if (!store.containsKey(className)) {
            return false;
        }
        JSONObject updateData = data.getJson("data");
        JSONArray<JSONObject> arr = store.getJson(className).getJsonArray("store");
        // 找到对应id的json,覆盖
        for (JSONObject v : arr) {
            if (v.getString("id").equals(updateData.getString("id"))) {
                v.putAll(updateData);
                onChange(serviceName, className, ctx);
                return true;
            }
        }
        return false;
    }

    /**
     * @param serviceName 微服务名称
     * @param data        添加数据
     * @apiNote 客户端要求删除数据
     */
    public boolean onDelete(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        // 获得分类
        String className = data.getString("name");
        if (!store.containsKey(className)) {
            return false;
        }
        JSONObject deleteData = data.getJson("data");
        JSONArray<JSONObject> arr = store.getJson(className).getJsonArray("store");
        // 找到对应id的json,覆盖
        Iterator<JSONObject> it = arr.iterator();
        while (it.hasNext()) {
            JSONObject item = it.next();
            if (item.getString("id").equals(deleteData.getString("id"))) {
                it.remove();
                onChange(serviceName, className, ctx);
                return true;
            }
        }
        return false;
    }
}
