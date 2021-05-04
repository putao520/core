package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class Services extends StoreBase {
    private Store $;

    private Services(JSONObject block) {
        super(block);
    }

    public static Services build(JSONObject block) {
        return new Services(block);
    }

    public Services bind(Store parent) {
        $ = parent;
        return this;
    }
}
