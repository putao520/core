package common.java.Coordination.Server;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.Coordination.Server.Store.Store;
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
                for (String s : broadcast_queue.keySet()) {
                    var arr = broadcast_queue.get(s);
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
    private final Store store;
    /**
     * keyA:[]
     * keyB:[]
     * keyC:[]
     */
    private final HashMap<String, ConcurrentHashMap<String, ChannelHandlerContext>> subscribeQueue = new HashMap<>();

    private GscCenterServer(String path) {
        this.file = FileText.build(path);
        this.store = Store.build(JSONObject.build(file.readString()));
    }

    private void save() {
        file.write(store.toString());
    }

    public GscCenterServer enableSave() {
        // 定时保存数据
        auto_save_worker.scheduleAtFixedRate(this::save, 30, 30, TimeUnit.SECONDS);
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
                return onInsert(msg, null);
            case "update":
                return onUpdate(msg, null);
            case "delete":
                return onDelete(msg, null);
            case "subscribe":
                subscribe(msg, msg.getChannel());
                return true;
            case "unsubscribe":
                unSubscribe(msg, msg.getChannel());
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
        return store.export();
    }

    /**
     * @apiNote 按挂载点导出内容
     */
    public JSONObject export(String key) {
        return store.export(key);
    }

    public JSONArray getClassStore(String key) {
        JSONObject info = export(key);
        if (JSONObject.isInvalided(info)) {
            return null;
        }
        return info.getJsonArray("store");
    }

    /**
     * @apiNote 根据部署ID获得合并后的服务信息
     */
    public JSONObject getDeployServiceInfo(int deployId) {
        return findServices4DeployId(deployId);
    }

    public GscCenterServer clear(String key) {
        store.clear(key);
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
        return store.services.getDataMap("name").getJson(serviceName);
    }

    private JSONObject findServiceInfoById(int id) {
        // 根据服务id获得服务信息
        return store.services.find(id);
    }

    private JSONArray<JSONObject> findApps4AppIds(Set<Integer> idArr) {
        JSONArray<JSONObject> result = JSONArray.build();
        JSONObject appsMap = store.apps.getDataMap();
        for (int id : idArr) {
            result.add(appsMap.getJson(String.valueOf(id)));
        }
        return result;
    }

    private int getServiceIdByName(String serviceName) {
        JSONObject serviceInfo = findServiceInfoByName(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            return -1;
        }
        return serviceInfo.getInt("id");
    }

    /**
     * @param serviceName 微服务名称
     * @apiNote 查找与service有关的应用上下文
     */
    private JSONArray<JSONObject> findApps4ServiceName(String serviceName) {
        int serviceId = getServiceIdByName(serviceName);
        if (serviceId < 0) {
            return null;
        }
        Set<Integer> appIds = new HashSet<>();
        // 根据部署表获得
        JSONArray<JSONObject> deployArr = store.servicesDeploy.getDataArr();
        for (JSONObject v : deployArr) {
            if (v.getInt("serviceId") == serviceId) {
                appIds.add(v.getInt("appId"));
            }
        }
        return findApps4AppIds(appIds);
    }

    private JSONObject findServices4DeployId(int deployId) {
        JSONArray<JSONObject> deployArr = store.servicesDeploy.getDataArr();
        for (JSONObject v : deployArr) {
            // 部署ID获得部署内容
            if (v.getInt("id") == deployId) {
                // 根据服务ID获得服务器内容
                JSONObject serviceInfo = findServiceInfoById(v.getInt("serviceId"));
                if (!JSONObject.isInvalided(serviceInfo)) {
                    return serviceInfo.put(v);
                }
            }
        }
        return null;
    }

    // 下发->微服务信息是 微服务信息+部署信息聚合
    private JSONArray<JSONObject> findServices4ServiceName(String serviceName, int deployId) {
        JSONObject serviceInfo = findServiceInfoByName(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            return null;
        }
        int serviceId = serviceInfo.getInt("id");
        // 根据部署表获得聚合微服务信息
        JSONArray<JSONObject> result = JSONArray.build();
        JSONArray<JSONObject> deployArr = store.servicesDeploy.getDataArr();
        for (JSONObject v : deployArr) {
            // 根据服务ID和部署ID获得聚合内容
            if (v.getInt("serviceId") == serviceId) {
                if (deployId > 0 && v.getInt("id") != deployId) {
                    continue;
                }
                result.add(JSONObject.build(v).put(serviceInfo));
            }
        }
        return result;
    }

    private JSONArray<JSONObject> findConfigs4ServiceName(String serviceName) {
        int serviceId = getServiceIdByName(serviceName);
        if (serviceId < 0) {
            return null;
        }
        // 根据部署表获得配置聚合信息
        JSONObject configMaps = store.configs.getDataMap("name");
        HashMap<String, JSONObject> configNameMaps = new HashMap<>();
        // 根据部署表获得
        JSONArray<JSONObject> deployArr = store.configs.getDataArr();
        for (JSONObject v : deployArr) {
            if (v.getInt("serviceId") == serviceId) {
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

    private void PushAll(GscChangeMsg msg, ChannelHandlerContext ctx) {
        String serviceName = msg.getServiceName();
        ReturnChannelMsg(serviceName, "apps", findApps4ServiceName(serviceName), ctx);
        ReturnChannelMsg(serviceName, "services", findServices4ServiceName(serviceName, msg.getDeployId()), ctx);
        ReturnChannelMsg(serviceName, "configs", findConfigs4ServiceName(serviceName), ctx);
    }

    /**
     * @param msg 订阅微服务名称
     * @apiNote 订阅挂载点数据变更
     */
    public void subscribe(GscChangeMsg msg, ChannelHandlerContext ctx) {
        String serviceName = msg.getServiceName();
        // 注册计算节点
        registerNode(serviceName, msg.getData(), ctx);
        // 下发与service有关数据
        PushAll(msg, ctx);
        // 添加订阅到队列
        getSubscribeChannel(serviceName).put(ctx.name(), ctx);
    }

    /**
     * @param msg 微服务名称
     * @param ctx 网络通道
     * @apiNote 取消订阅挂载点数据变更
     */
    public GscCenterServer unSubscribe(GscChangeMsg msg, ChannelHandlerContext ctx) {
        int nodeId = msg.getData().getInt("node");
        getSubscribeChannel(msg.getServiceName()).remove(ctx.name());
        store.nodes.remove(nodeId);
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

    private void onChange(GscChangeMsg msg, String className, ChannelHandlerContext ctx) {
        String serviceName = msg.getServiceName();
        // 添加数据, 根据修改后数据触发返回
        switch (className) {
            case "apps":
                ReturnChannelMsg(serviceName, "apps", findApps4ServiceName(serviceName), ctx);
                break;
            case "services":
                ReturnChannelMsg(serviceName, "services", findServices4ServiceName(serviceName, msg.getDeployId()), ctx);
                break;
            case "configs":
                ReturnChannelMsg(serviceName, "configs", findConfigs4ServiceName(serviceName), ctx);
                break;
            case "servicesDeploy":
                PushAll(msg, ctx);
        }
    }

    /**
     * @param msg 消息
     * @apiNote 客户端新增数据时
     */
    public boolean onInsert(GscChangeMsg msg) {
        return onInsert(msg, null);
    }

    public boolean onInsert(GscChangeMsg msg, ChannelHandlerContext ctx) {
        JSONObject data = msg.getData();
        // 获得分类
        String className = data.getString("name");
        if (store.has(className)) {
            return false;
        }
        JSONObject insertData = data.getJson("data");
        store.insert(className, insertData);
        onChange(msg, className, ctx);
        return true;
    }

    /**
     * @param msg 消息
     * @apiNote 客户端要求更新数据
     */
    public boolean onUpdate(GscChangeMsg msg) {
        return onUpdate(msg, null);
    }

    /**
     * @apiNote 讲全微服务信息，提取成 部署信息 和 服务信息
     */
    private JSONArray<JSONObject> spiltServiceAndDeploy(JSONObject fullServiceInfo) {
        JSONArray<JSONObject> r = JSONArray.build();
        if (!fullServiceInfo.has("appId")
                || !fullServiceInfo.has("serviceId")
        ) {
            return r;
        }

        JSONObject serviceInfo = JSONObject.build()
                .putIfNotNull("name", fullServiceInfo.getString("name"))
                .putIfNotNull("peerAddr", fullServiceInfo.getString("peerAddr"))
                .putIfNotNull("id", fullServiceInfo.getString("id"))
                .putIfNotNull("desc", fullServiceInfo.getString("desc"));
        JSONObject deployInfo = JSONObject.build()
                .putIfNotNull("debug", fullServiceInfo.getInt("debug"))
                .putIfNotNull("appId", fullServiceInfo.getInt("appId"))
                .putIfNotNull("serviceId", fullServiceInfo.getInt("serviceId"))
                .putIfNotNull("state", fullServiceInfo.getInt("state"))
                .putIfNotNull("updateAt", fullServiceInfo.getString("updateAt"))
                .putIfNotNull("createAt", fullServiceInfo.getString("createAt"))
                .putIfNotNull("dataModel", fullServiceInfo.getJson("dataModel"))
                .putIfNotNull("config", fullServiceInfo.getJson("config"));
        // 根据 fullServiceInfo 查找部署id
        var resultArr = store.servicesDeploy
                .find("serviceId", fullServiceInfo.getInt("serviceId"))
                .<Integer>filter("appId", v -> v == fullServiceInfo.getInt("appId"));
        if (JSONArray.isInvalided(resultArr)) {
            return r;
        }
        deployInfo.put("id", r.get(0).getInt("id"));

        return r.put(serviceInfo).put(deployInfo);
    }

    /**
     * @apiNote 更新对象是微服务信息时，尝试拆分成 部署对象 和 服务对象
     */
    public boolean onUpdate(GscChangeMsg msg, ChannelHandlerContext ctx) {
        JSONObject data = msg.getData();
        // 获得分类
        String className = data.getString("name");
        if (store.has(className)) {
            return false;
        }
        JSONObject updateData = data.getJson("data");
        // 如果是微服务
        if (className.equals("services")) {
            JSONArray<JSONObject> dataArr = spiltServiceAndDeploy(updateData);
            if (dataArr.size() != 2) {
                return false;
            }
            try {
                store.update(className, dataArr.get(0));
                // 切换类型到部署
                className = "servicesDeploy";
                store.update(className, dataArr.get(1));
            } catch (Exception e) {
                return false;
            }
        } else {
            try {
                store.update(className, updateData);
            } catch (Exception e) {
                return false;
            }
        }
        onChange(msg, className, ctx);
        return true;
    }

    /**
     * @param msg        添加数据
     * @apiNote 客户端要求删除数据
     */
    public boolean onDelete(GscChangeMsg msg) {
        return onDelete(msg, null);
    }

    public boolean onDelete(GscChangeMsg msg, ChannelHandlerContext ctx) {
        JSONObject data = msg.getData();
        // 获得分类
        String className = data.getString("name");
        if (store.has(className)) {
            return false;
        }
        JSONObject deleteData = data.getJson("data");
        try {
            store.delete(className, deleteData.getInt("id"));
        } catch (Exception e) {
            return false;
        }
        onChange(msg, className, ctx);
        return true;
    }
}
