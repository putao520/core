package common.java.MasterProxy;

import common.java.Coordination.Client.GscCenterClient;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class MasterActor {
    private static final ConcurrentHashMap<String, MasterActor> master_actors = new ConcurrentHashMap<>();
    private static final GscCenterClient client = GscCenterClient.build();
    private final String actionName;
    private final HashMap<String, JSONObject> quickMapKeys;

    private MasterActor(String actionName) {
        this.actionName = actionName;
        this.quickMapKeys = new HashMap<>();
    }

    // 检查客户端网络是否正常,不正常重新连接
    private static GscCenterClient check() {
        if (!client.getLiveStatus()) {
            client.setKeepLived(true).reConnect().waitLived();
        }
        return client;
    }

    public static void updateAll(String actionName) {
        for (MasterActor actor : master_actors.values()) {
            if (actor.getActionName().equals(actionName)) {
                actor.update();
            }
        }
    }

    public static GscCenterClient getClient() {
        return check();
    }

    public static MasterActor getInstance(String actionName) {
        if (!master_actors.containsKey(actionName)) {
            master_actors.put(actionName, new MasterActor(actionName));
        }
        return master_actors.get(actionName);
    }

    public static void updateAll() {
        for (MasterActor actor : master_actors.values()) {
            actor.update();
        }
    }

    public String getActionName() {
        return actionName;
    }

    public void update() {
        for (String key : quickMapKeys.keySet()) {
            quickMapKeys.put(key, client.getData(actionName).mapsByKey(key));
        }
    }

    public JSONObject getDataByIndex(String key, String value) {
        if (!quickMapKeys.containsKey(key)) {
            quickMapKeys.put(key, client.getData(actionName).mapsByKey(key));
        }
        return quickMapKeys.get(key).getJson(value);
    }

    public JSONArray getData() {
        return client.getData(actionName);
    }

    public MasterActor insert(JSONObject data) {
        client.insert(actionName, data);
        return this;
    }

    public MasterActor update(JSONObject data) {
        client.update(actionName, data);
        return this;
    }

    public MasterActor delete(JSONObject data) {
        client.delete(actionName, data);
        return this;
    }

}
