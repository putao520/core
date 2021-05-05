package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class Nodes extends StoreBase {

    private Nodes(JSONObject block) {
        super(block);
    }

    public static Nodes build(JSONObject block) {
        return new Nodes(block);
    }

}
