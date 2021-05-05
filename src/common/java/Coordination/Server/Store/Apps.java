package common.java.Coordination.Server.Store;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

public class Apps extends StoreBase {

    Apps(JSONObject block) {
        super(block);
    }

    public static Apps build(JSONObject block) {
        return new Apps(block);
    }

    @Override
    public int insert(JSONObject data) {
        if (onChangeCheck(data, false)) {
            return -1;
        }
        return super.insert(data);
    }

    @Override
    public void update(JSONObject json) {
        if (onChangeCheck(json, true)) {
            throw new RuntimeException("更新数据不合法");
        }
        super.update(json);
    }

    // 新增，更新时，检查 配置
    private boolean onChangeCheck(JSONObject appInfo, boolean update) {
        if (JSONObject.isInvalided(appInfo)) {
            nLogger.errorInfo("应用信息无效");
            return true;
        }
        if (!appInfo.has("config")) {
            return !update;
        }
        return super.checkConfigs(ModelServiceConfig.build(appInfo.getJson("config")).getSafeConfig().values());
    }
}
