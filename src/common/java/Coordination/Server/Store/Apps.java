package common.java.Coordination.Server.Store;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import org.json.gsc.JSONObject;

public class Apps extends StoreBase {
    private Store $;

    private Apps(JSONObject block) {
        super(block);
    }

    public static Apps build(JSONObject block) {
        return new Apps(block);
    }

    public Apps bind(Store parent) {
        $ = parent;
        return this;
    }

    public int insert(JSONObject data) {
        return super.insert(data);
    }

    // 新增，更新时，检查 配置
    private boolean onChangeCheck(int idx) {
        // 检查配置组成
        JSONObject appInfo = super.find(idx);
        if (JSONObject.isInvalided(appInfo)) {
            return false;
        }
        ModelServiceConfig msc = ModelServiceConfig.build(appInfo.getJson("config"));

    }
}
