package common.java.Coordination.Server.Store;

import org.json.gsc.JSONObject;

public class Store {
    public final Apps apps;
    public final Configs configs;
    public final ConfigTemplate configTemplate;
    public final Services services;
    public final ServicesDeploy servicesDeploy;
    public final Nodes nodes;

    private Store(JSONObject data) {
        this.apps = Apps.build(data.getJson("apps"));
        this.configs = Configs.build(data.getJson("configs"));
        this.configTemplate = ConfigTemplate.build(data.getJson("configTemplate"));
        this.services = Services.build(data.getJson("services"));
        this.servicesDeploy = ServicesDeploy.build(data.getJson("servicesDeploy"));
        this.nodes = Nodes.build(data.getJson("nodes"));
    }
}
