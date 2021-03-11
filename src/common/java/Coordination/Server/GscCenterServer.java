package common.java.Coordination.Server;

import common.java.Coordination.Common.GscCenterPacket;
import common.java.File.FileText;
import common.java.GscCommon.checkModel;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static common.java.Coordination.Common.GscCenterEvent.DataInit;


public class GscCenterServer {
    private static final HashMap<String, GscCenterServer> handle = new HashMap<>();
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

    /**
     * @param key  分类
     * @param item 数据
     * @apiNote 新增数据
     */
    public GscCenterServer insert(String key, JSONObject item) {
        JSONArray arr = store.getJsonArray(key);
        JSONObject map = arr.mapsByKey("id");
        String uuid;
        do {
            uuid = StringHelper.randomString(64);
        } while (map.containsKey(uuid));
        arr.add(item.puts("id", uuid));
        // 新的部署,需要触发,同时订阅微服务和app的对象
        if (key.equals("servicesDeploy")) {
            // 获得新部署的微服务名
            JSONObject serviceInfo = store.getJsonArray("services").mapsByKey("id").getJson(item.getString("serviceId"));
            if (!JSONObject.isInvalided(serviceInfo)) {
                String serviceName = serviceInfo.getString("name");
                // 触发应用更新
                onChangeApps(serviceName);
                // 触发微服务更新
                onChangeServices(serviceName);
                // 触发配置更新
                onChangeConfigs(serviceName);
            }
        }
        return this;
    }


    private Set<String> findServiceNameByServiceIds(Set<String> serviceIdArr) {
        Set<String> serviceNameArr = new HashSet<>();
        JSONArray<JSONObject> serviceArr = store.getJsonArray("services");
        for (JSONObject serviceInfo : serviceArr) {
            for (String serviceId : serviceIdArr) {
                if (serviceInfo.getString("id").equals(serviceId)) {
                    serviceNameArr.add(serviceInfo.getString("name"));
                }
            }
        }
        return serviceNameArr;
    }

    private Set<String> findServiceNameByDeployKey(Set<String> idArr, String key) {
        JSONArray<JSONObject> deployArr = store.getJsonArray("servicesDeploy");
        Set<String> serviceIdArr = new HashSet<>();
        for (JSONObject deployInfo : deployArr) {
            for (String id : idArr) {
                if (deployInfo.getString(key).equals(id)) {
                    serviceIdArr.add(deployInfo.getString("serviceId"));
                }
            }
        }
        return findServiceNameByServiceIds(serviceIdArr);
    }

    private Set<String> findServiceNameByAppIds(Set<String> appIdArr) {
        return findServiceNameByDeployKey(appIdArr, "appId");
    }

    private Set<String> findServiceNameByDeployIds(Set<String> deployIdArr) {
        return findServiceNameByDeployKey(deployIdArr, "id");
    }

    private HashMap<String, JSONObject> findConfigNameMap(Set<String> configIdArr) {
        HashMap<String, JSONObject> configNameMap = new HashMap<>();
        JSONArray<JSONObject> configArr = store.getJsonArray("configs");
        for (JSONObject configInfo : configArr) {
            for (String configId : configIdArr) {
                if (configInfo.getString("id").equals(configId)) {
                    configNameMap.put(configInfo.getString("name"), configInfo);
                }
            }
        }
        return configNameMap;
    }

    private <T> Set<T> findDeployValueByConfigName(HashMap<String, JSONObject> configNameMap, String key) {
        // 根据微服务部署表,找到受到被更新配置影响的微服务id组
        Set<T> arr = new HashSet<>();
        JSONArray<JSONObject> serviceDeployArr = store.getJsonArray("servicesDeploy");
        for (JSONObject deployInfo : serviceDeployArr) {
            Set<String> _ServiceConfigNameArr = new HashSet<>();
            JSONObject configInfo = deployInfo.getJson("config");
            // 获得单一微服务所有用到的配置名
            if (!JSONObject.isInvalided(configInfo)) {
                pushConfigName(_ServiceConfigNameArr, configInfo, "db");
                pushConfigName(_ServiceConfigNameArr, configInfo, "cache");
                pushConfigName(_ServiceConfigNameArr, configInfo, "store");
                pushConfigName(_ServiceConfigNameArr, configInfo, "mq");
                pushConfigName(_ServiceConfigNameArr, configInfo, "streamComputer");
                pushConfigName(_ServiceConfigNameArr, configInfo, "blockComputer");
                // 如果该服务配置包含在更新内容里
                for (String configName : configNameMap.keySet()) {
                    if (_ServiceConfigNameArr.contains(configName)) {
                        if (StringHelper.isInvalided(key)) {
                            arr.add((T) deployInfo);
                        } else {
                            arr.add((T) deployInfo.get(key));
                        }
                        break;
                    }
                }
            }
        }
        return arr;
    }

    private Set<String> findServiceNameByConfigIds(Set<String> configIdArr) {
        return findServiceNameByServiceIds(
                findDeployValueByConfigName(
                        findConfigNameMap(configIdArr),
                        "serviceId"
                )
        );
    }

    private Set<String> findServiceNameByConfigIdsAndPauseDeploy(Set<String> configIdArr) {
        Set<JSONObject> deployInfoArr = findDeployValueByConfigName(findConfigNameMap(configIdArr), null);
        Set<String> serviceIdArr = new HashSet<>();
        JSONArray<JSONObject> deployTable = store.getJsonArray("servicesDeploy");
        for (JSONObject deployInfo : deployInfoArr) {
            // 获得微服务id
            serviceIdArr.add(deployInfo.getString("id"));
            // 设置部署暂停
            for (JSONObject deploy : deployTable) {
                deploy.put("status", checkModel.pending);
            }
        }
        return findServiceNameByServiceIds(serviceIdArr);
    }

    public GscCenterServer update(String key, String indexKey, JSONObject item) {
        JSONArray<JSONObject> arr = store.getJsonArray(key);
        Set<String> idsArr = new HashSet<>();
        for (JSONObject line : arr) {
            if (item.getString(indexKey).equals(line.getString(indexKey))) {
                line.putAll(item);
                idsArr.add(line.getString("id"));
            }
        }
        Set<String> serviceNameArr;
        switch (key) {
            case "apps":
                // 触发应用更新
                serviceNameArr = findServiceNameByAppIds(idsArr);
                for (String serviceName : serviceNameArr) {
                    onChangeApps(serviceName);
                }
                break;
            case "services":
                // 触发微服务更新
                serviceNameArr = findServiceNameByServiceIds(idsArr);
                for (String serviceName : serviceNameArr) {
                    onChangeServices(serviceName);
                }
                break;
            case "configs":
                serviceNameArr = findServiceNameByConfigIds(idsArr);
                for (String serviceName : serviceNameArr) {
                    onChangeConfigs(serviceName);
                }
                break;
        }
        return this;
    }

    private JSONObject Map2JsonQuery(HashMap<String, JSONObject> idsMap, String key) {
        JSONObject d = JSONObject.build();
        for (String id : idsMap.keySet()) {
            d.put(key, id);
        }
        return d;
    }

    private JSONArray<JSONObject> Remove4Map(JSONArray<JSONObject> arr, HashMap<String, JSONObject> idsMap) {
        Iterator<JSONObject> it = arr.iterator();
        while (it.hasNext()) {
            JSONObject line = it.next();
            if (idsMap.containsKey(line.getString("id"))) {
                it.remove();
            }
        }
        return arr;
    }

    public void delete(String key, String indexKey, JSONObject item) {
        delete(key, indexKey, item, true);
    }

    public void delete(String key, String indexKey, JSONObject item, boolean first) {
        JSONArray<JSONObject> arr = store.getJsonArray(key);
        HashMap<String, JSONObject> idsMap = new HashMap<>();   // 被删除的数据id
        for (JSONObject line : arr) {
            if (item.getString(indexKey).equals(line.getString(indexKey))) {
                idsMap.put(line.getString("id"), line);
            }
        }
        // idsMap 记录需要删掉的对象json
        Set<String> serviceNameArr = null;
        switch (key) {
            case "apps":
                serviceNameArr = findServiceNameByAppIds(idsMap.keySet());
                // 触发应用更新(删除app时,一同删除部署表所有涉及到该app的数据)
                delete("servicesDeploy", "appId", Map2JsonQuery(idsMap, "appId"), false);
                // 删除数据
                Remove4Map(arr, idsMap);
                // 同步数据
                for (String serviceName : serviceNameArr) {
                    onChangeApps(serviceName);
                }
                break;
            case "services":
                // 触发微服务更新(删除app时,一同删除部署表所有涉及到该微服务的数据)
                serviceNameArr = findServiceNameByServiceIds(idsMap.keySet());
                delete("servicesDeploy", "serviceId", Map2JsonQuery(idsMap, "serviceId"), false);
                // 删除数据
                Remove4Map(arr, idsMap);
                // 同步数据
                for (String serviceName : serviceNameArr) {
                    onChangeServices(serviceName);
                }
                break;
            // 触发更新
            case "configs":
                // 所有使用被删除config的部署微服务,对应配置项目删除,同时微服务状态设置为暂停
                serviceNameArr = findServiceNameByConfigIdsAndPauseDeploy(idsMap.keySet());
                // 删除数据
                Remove4Map(arr, idsMap);
                // 同步数据
                for (String serviceName : serviceNameArr) {
                    onChangeConfigs(serviceName);
                }
                break;
            case "servicesDeploy":
                // 是网络消息回调
                if (first) {
                    // 获得受影响的serviceName
                    serviceNameArr = findServiceNameByDeployIds(idsMap.keySet());
                }
                // 删除数据
                Remove4Map(arr, idsMap);
                if (first && serviceNameArr != null) {
                    // 同步数据
                    for (String serviceName : serviceNameArr) {
                        onChangeApps(serviceName);
                        onChangeServices(serviceName);
                    }
                }
                break;
        }
    }

    public GscCenterServer clear(String key) {
        store.getJsonArray(key).clear();
        return this;
    }

    private void ReturnChannelMsg(String key, JSONArray resultArr, ChannelHandlerContext ctx) {
        // 没包含特定网络通道
        if (ctx == null) {
            var queue = subscribeQueue.get(key);
            for (ChannelHandlerContext _ctx : queue) {
                _ctx.writeAndFlush(GscCenterPacket.build(
                        key,
                        JSONObject.build("data", resultArr),
                        DataInit,
                        true
                ));
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

    private void onChangeServices(String serviceName) {
        onChangeServices(serviceName, null);
    }

    private void onChangeServices(String serviceName, ChannelHandlerContext ctx) {
        // 合并微服务部署表和微服务表
        JSONObject serviceInfo = store.getJsonArray("services").mapsByKey("name").getJson(serviceName);
        String id = serviceInfo.getString("id");
        JSONArray<JSONObject> deployArr = store.getJsonArray("servicesDeploy");
        JSONArray resultArr = JSONArray.build();
        for (JSONObject item : deployArr) {
            if (item.getString("serviceId").equals(id)) {
                JSONObject rData = JSONObject.build();
                rData.putAll(serviceInfo);
                rData.putAll(item);
                resultArr.add(rData);
            }
        }
        ReturnChannelMsg("services", resultArr, ctx);
    }

    private String onNodeIn(JSONObject data) {
        String serviceName = data.getString("name");    // 获得请求来源服务名
        String nodeId = data.getString("node");         // 获得节点Id
        // 注册节点到节点表
        store.getJson("nodes").put(nodeId, TimeHelper.build().nowDatetime());
        return serviceName;
    }

    // 订阅微服务类型数据
    private void subscribeServices(JSONObject data, ChannelHandlerContext ctx) {
        onChangeServices(onNodeIn(data), ctx);
    }

    private void onChangeApps(String serviceName) {
        onChangeApps(serviceName, null);
    }

    private void onChangeApps(String serviceName, ChannelHandlerContext ctx) {
        JSONObject appsMap = store.getJsonArray("apps").mapsByKey("id");
        JSONObject serviceInfo = store.getJsonArray("services").mapsByKey("name").getJson(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            return;
        }
        String id = serviceInfo.getString("id");
        JSONArray<JSONObject> serviceDeployArr = store.getJsonArray("servicesDeploy");
        // 微服务 部署的应用组
        Set<String> appIdArr = new HashSet<>();
        for (JSONObject line : serviceDeployArr) {
            if (line.getString("serviceId").equals(id)) {
                appIdArr.add(line.getString("appId"));
            }
        }
        if (appIdArr.size() == 0) {
            return;
        }
        JSONArray resultArr = JSONArray.build();
        for (String appId : appIdArr) {
            if (appsMap.containsKey(appId)) {
                resultArr.add(appsMap.getJson(appId));
            }
        }
        ReturnChannelMsg("apps", resultArr, ctx);
    }

    // 订阅应用类型数据
    private void subscribeApps(JSONObject data, ChannelHandlerContext ctx) {
        onChangeApps(onNodeIn(data), ctx);
    }

    private void pushConfigName(Set<String> configNameArr, JSONObject config, String name) {
        if (config.containsKey(name)) {
            configNameArr.add(config.getString(name));
        }
    }

    private void onChangeConfigs(String serviceName) {
        onChangeConfigs(serviceName, null);
    }

    private void onChangeConfigs(String serviceName, ChannelHandlerContext ctx) {
        JSONObject configsMap = store.getJsonArray("configs").mapsByKey("name");
        JSONObject serviceInfo = store.getJsonArray("services").mapsByKey("name").getJson(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            return;
        }
        String sId = serviceInfo.getString("id");       // 微服务id
        // 配置集合
        Set<String> configNameArr = new HashSet<>();
        // 从部署表拿到全部配置列表
        JSONArray<JSONObject> deployArr = store.getJsonArray("servicesDeploy");
        for (JSONObject line : deployArr) {
            if (!line.getString("serviceId").equals(sId)) {
                continue;
            }
            JSONObject config = line.getJson("config");
            if (JSONObject.isInvalided(config)) {
                continue;
            }
            // 数据库
            pushConfigName(configNameArr, config, "db");
            // 缓存
            pushConfigName(configNameArr, config, "cache");
            // 队列
            pushConfigName(configNameArr, config, "mq");
            // 存储
            pushConfigName(configNameArr, config, "store");
            // 流式计算
            pushConfigName(configNameArr, config, "streamComputer");
            // 批处理计算
            pushConfigName(configNameArr, config, "blockComputer");
        }
        if (configNameArr.size() == 0) {
            return;
        }
        // 获得全部配置集合
        JSONArray resultArr = JSONArray.build();
        for (String configName : configNameArr) {
            if (configsMap.containsKey(configName)) {
                resultArr.add(configsMap.getJson(configName));
            }
        }
        ReturnChannelMsg("configs", resultArr, ctx);
    }

    // 订阅配置类型数据
    private void subscribeConfigs(JSONObject data, ChannelHandlerContext ctx) {
        onChangeConfigs(onNodeIn(data), ctx);
    }

    /**
     * @param key  订阅分类key
     * @param data 附带数据{ name: 服务名, node:节点id }
     * @apiNote 订阅挂载点数据变更
     */
    public void subscribe(String key, JSONObject data, ChannelHandlerContext ctx) {
        switch (key) {
            case "apps":
                subscribeApps(data, ctx);
                break;
            case "services":
                subscribeServices(data, ctx);
                break;
            case "configs":
                subscribeConfigs(data, ctx);
                break;
            default:
                return;
        }
        // 添加订阅到队列
        getSubscribeChannel(key).add(ctx);
    }

    /**
     * @param key 挂载节点
     * @param ctx 网络通道
     * @apiNote 取消订阅挂载点数据变更
     */
    public GscCenterServer unSubscribe(String key, ChannelHandlerContext ctx) {
        getSubscribeChannel(key).remove(ctx);
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

    private Set<ChannelHandlerContext> getSubscribeChannel(String key) {
        Set<ChannelHandlerContext> queue;
        if (!subscribeQueue.containsKey(key)) {
            queue = new HashSet<>();
            subscribeQueue.put(key, queue);
        } else {
            queue = subscribeQueue.get(key);
        }
        return queue;
    }
}
