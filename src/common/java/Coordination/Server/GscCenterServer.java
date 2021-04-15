package common.java.Coordination.Server;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.File.FileText;
import common.java.String.StringHelper;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static common.java.Coordination.Common.GscCenterEvent.DataInit;


public class GscCenterServer {
    private static final HashMap<String, GscCenterServer> handle = new HashMap<>();
    private static final HashMap<String, ConcurrentHashMap<String, ChannelHandlerContext>> nodeArr = new HashMap<>();
    private static final ConcurrentHashMap<String, List<GscBroadCastMsg>> broadcast_queue = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService broadcast_worker = Executors.newSingleThreadScheduledExecutor();  // 广播数据线程
    private static final ExecutorService cmd_worker = Executors.newSingleThreadExecutor();  // 管理命令线程

    static {
        // 定时广播数据
        broadcast_worker.scheduleAtFixedRate(() -> {
            if (!broadcast_queue.isEmpty()) {
                var iter = broadcast_queue.keySet().iterator();
                while (iter.hasNext()) {
                    var arr = broadcast_queue.get(iter.next());
                    var it = arr.iterator();
                    while (it.hasNext()) {
                        it.next().broadCast();
                        it.remove();
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private final ScheduledExecutorService auto_save_worker = Executors.newSingleThreadScheduledExecutor();  // 管理命令线程
    private final FileText file;
    // 获得该对象时,必须确保并发安全!!!(目前已改成队列执行，确保线程安全)
    private final JSONObject store;
    /**
     * keyA:[]
     * keyB:[]
     * keyC:[]
     */
    private final HashMap<String, ConcurrentHashMap<String, ChannelHandlerContext>> subscribeQueue = new HashMap<>();

    private GscCenterServer(String path) {
        this.file = FileText.build(path);
        this.store = JSONObject.build(file.readString());
    }

    private void save() {
        file.write(store.toJSONString());
    }

    public GscCenterServer enableSave() {
        auto_save_worker.scheduleAtFixedRate(() -> {
            // 定时保存数据
            this.save();
        }, 30, 30, TimeUnit.SECONDS);
        return this;
    }

    public GscCenterServer disableSave() {
        if (auto_save_worker.isTerminated()) {
            try {
                auto_save_worker.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // 无视终止异常，强行终止
            }
        }
        if (auto_save_worker.isShutdown()) {
            auto_save_worker.shutdown();
        }
        return this;
    }

    public static GscCenterServer getInstance(Path path) {
        String pathStr = path.toAbsolutePath().toString();
        if (!handle.containsKey(pathStr)) {
            handle.put(pathStr, GscCenterServer.load(pathStr));
        }
        return handle.get(pathStr);
    }

    public static GscCenterServer getInstance() {
        String path = Config.config("StorePath");
        if (StringHelper.isInvalided(path)) {
            throw new RuntimeException("数据持久化地址未配置");
        }
        return getInstance(Paths.get(path));
    }

    /**
     * @apiNote 载入初始化数据
     */
    public static GscCenterServer load(String path) {
        return new GscCenterServer(path);
    }

    public boolean workerRunner(GscChangeMsg msg) {
        switch (msg.getAction()) {
            case "insert":
                // return onInsert(msg.getServiceName(), msg.getData(), msg.getChannel());
                return onInsert(msg.getServiceName(), msg.getData(), null);
            case "update":
                // return onUpdate(msg.getServiceName(), msg.getData(), msg.getChannel());
                return onUpdate(msg.getServiceName(), msg.getData(), null);
            case "delete":
                // return onDelete(msg.getServiceName(), msg.getData(), msg.getChannel());
                return onDelete(msg.getServiceName(), msg.getData(), null);
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

    /**
     * @apiNote 通过 pushWork 将收到的命令压入命令执行序列,确保多线程下store数据可靠
     */
    public void pushWork(GscChangeMsg msg, int errorNo) {
        if (errorNo < 3) {
            cmd_worker.submit(() -> {
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

    public JSONArray getClassStore(String key) {
        JSONObject info = export(key);
        if (JSONObject.isInvalided(info)) {
            return null;
        }
        return info.getJsonArray("store");
    }

    public GscCenterServer clear(String key) {
        store.getJsonArray(key).clear();
        return this;
    }

    private void ReturnChannelMsg(String serviceName,           // 微服务名称
                                  String key,                   // 分类名称
                                  JSONArray resultArr,          // 返回结果集合
                                  ChannelHandlerContext ctx) {
        // 没包含特定网络通道,需要广播,投递到广播任务队列
        if (ctx == null) {
            if (subscribeQueue.containsKey(serviceName)) {
                var queue = subscribeQueue.get(serviceName);
                if (!queue.isEmpty()) {
                    if (!broadcast_queue.containsKey(serviceName)) {
                        broadcast_queue.put(serviceName, new ArrayList<>());
                    }
                    broadcast_queue.get(serviceName).add(GscBroadCastMsg.build(GscCenterPacket.build(
                            key,
                            JSONObject.build("data", resultArr),
                            DataInit,
                            true
                    ), queue));
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
        if (!nodeArr.containsKey(serviceName)) {
            nodeArr.put(serviceName, new ConcurrentHashMap<>());
        }
        var queue = nodeArr.get(serviceName);
        queue.put(nodeId, ctx);
    }

    private void pushConfigName(HashMap<String, JSONObject> configNameArr, JSONObject config, String name, JSONObject configStore) {
        if (config.containsKey(name)) {
            String val = config.getString(name);
            if (!StringHelper.isInvalided(val) && configStore.containsKey(val)) {
                configNameArr.put(val, configStore.getJson(val));
            }
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
        getSubscribeChannel(serviceName).put(ctx.name(), ctx);
    }

    /**
     * @param serviceName 微服务名称
     * @param ctx         网络通道
     * @apiNote 取消订阅挂载点数据变更
     */
    public GscCenterServer unSubscribe(String serviceName, JSONObject data, ChannelHandlerContext ctx) {
        String nodeId = data.getString("node");
        getSubscribeChannel(serviceName).remove(ctx.name());
        store.getJson("nodes").remove(nodeId);
        return this;
    }

    /**
     * @apiNote 从所有订阅记录中删除对应通道
     */
    public GscCenterServer removeChannel(ChannelHandlerContext ctx) {
        for (ConcurrentHashMap<String, ChannelHandlerContext> ctxArray : subscribeQueue.values()) {
            ctxArray.remove(ctx.name());
        }
        return this;
    }

    private ConcurrentHashMap<String, ChannelHandlerContext> getSubscribeChannel(String serviceName) {
        if (!subscribeQueue.containsKey(serviceName)) {
            subscribeQueue.put(serviceName, new ConcurrentHashMap<>());
        }
        return subscribeQueue.get(serviceName);
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
            case "servicesDeploy":
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
