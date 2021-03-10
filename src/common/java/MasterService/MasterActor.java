package common.java.MasterService;

import common.java.Coordination.Client.GscCenterClient;

import java.util.HashMap;

public class MasterActor {
    private static final HashMap<String, GscCenterClient> inst;

    static {
        inst = new HashMap<>();
    }

    public static GscCenterClient getInstance(String actorName) {
        if (!inst.containsKey(actorName)) {
            inst.put(actorName, GscCenterClient.build(actorName));
        }
        return inst.get(actorName);
    }
}
