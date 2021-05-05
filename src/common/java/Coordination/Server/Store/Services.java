package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class Services extends Apps {
    private Services(JSONObject block) {
        super(block);
    }

    public static Services build(JSONObject block) {
        return new Services(block);
    }
}
