package common.java.MasterProxy;

import common.java.Coordination.Client.GscCenterClient;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class MasterActor {
    private static final ConcurrentHashMap<String, MasterActor> master_actors = new ConcurrentHashMap<>();
    private static final GscCenterClient client = GscCenterClient.build();
    private final String actionName;

    private MasterActor(String actionName) {
        this.actionName = actionName;
    }

    // 检查客户端网络是否正常,不正常重新连接
    private static GscCenterClient check() {
        if (!client.getLiveStatus()) {
            client.setKeepLived(true).reConnect().waitLived();
        }
        return client;
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

    public JSONArray<JSONObject> getData() {
        return client.getData(actionName);
    }

    public String getActionName() {
        return actionName;
    }

    public JSONArray<JSONObject> getDataByIndex(String key, String value) {
        return client.getData(actionName, key, value);
    }

}
