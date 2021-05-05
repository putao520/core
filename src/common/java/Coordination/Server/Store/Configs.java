package common.java.Coordination.Server.Store;

import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

public class Configs extends StoreBase {

    private Configs(JSONObject block) {
        super(block);
    }

    public static Configs build(JSONObject block) {
        return new Configs(block);
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

    // 新增，更新时，检查 配置模板
    private boolean onChangeCheck(JSONObject cfgInfo, boolean update) {
        if (JSONObject.isInvalided(cfgInfo)) {
            nLogger.errorInfo("配置信息无效");
            return true;
        }
        if (!cfgInfo.has("templateId")) {
            return !update;
        }
        // 空字符串模板，表示未启用
        String nullChk = cfgInfo.getString("templateId");
        if (StringHelper.isInvalided(nullChk)) {
            return false;
        }
        return !super.checkConfigTemplate(Integer.parseInt(nullChk));
    }
}
