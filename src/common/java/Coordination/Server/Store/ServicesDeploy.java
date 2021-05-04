package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class ServicesDeploy extends StoreBase {
    private Store $;

    private ServicesDeploy(JSONObject block) {
        super(block);
    }

    public static ServicesDeploy build(JSONObject block) {
        return new ServicesDeploy(block);
    }

    public ServicesDeploy bind(Store parent) {
        $ = parent;
        return this;
    }
}
