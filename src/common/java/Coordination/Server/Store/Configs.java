package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class Configs extends StoreBase {
    private Store $;

    private Configs(JSONObject block) {
        super(block);
    }

    public static Configs build(JSONObject block) {
        return new Configs(block);
    }

    public Configs bind(Store parent) {
        $ = parent;
        return this;
    }
}
