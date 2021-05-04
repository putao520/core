package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class ConfigTemplate extends StoreBase {
    private Store $;

    private ConfigTemplate(JSONObject block) {
        super(block);
    }

    public static ConfigTemplate build(JSONObject block) {
        return new ConfigTemplate(block);
    }

    public ConfigTemplate bind(Store parent) {
        $ = parent;
        return this;
    }
}
