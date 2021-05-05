package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class ConfigTemplate extends StoreBase {

    private ConfigTemplate(JSONObject block) {
        super(block);
    }

    public static ConfigTemplate build(JSONObject block) {
        return new ConfigTemplate(block);
    }

}
