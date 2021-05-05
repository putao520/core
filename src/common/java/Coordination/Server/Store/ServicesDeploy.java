package common.java.Coordination.Server.Store;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

public class ServicesDeploy extends StoreBase {
    private ServicesDeploy(JSONObject block) {
        super(block);
    }

    public static ServicesDeploy build(JSONObject block) {
        return new ServicesDeploy(block);
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
            throw new RuntimeException("部署数据不合法");
        }
        super.update(json);
    }

    // 新增，更新时，检查 配置模板
    private boolean onChangeCheck(JSONObject deployInfo, boolean update) {
        if (JSONObject.isInvalided(deployInfo)) {
            nLogger.errorInfo("部署信息无效");
            return true;
        }
        // 检查 appId
        if (!deployInfo.has("appId")) {
            if (!update) {
                return true;
            }
        } else {
            if (!super.checkApp(deployInfo.getInt("appId"))) {
                return true;
            }
        }
        // 检查 serviceId
        if (!deployInfo.has("serviceId")) {
            if (!update) {
                return true;
            }
        } else {
            if (!super.checkService(deployInfo.getInt("serviceId"))) {
                return true;
            }
        }
        // 检查配置
        if (!deployInfo.has("config")) {
            return !update;
        } else {
            return super.checkConfigs(ModelServiceConfig.build(deployInfo.getJson("config")).getSafeConfig().values());
        }
    }
}
