package common.java.apps;

import common.java.authority.PermissionsPowerDef;
import common.java.authority.PlvDef;
import common.java.session.Session;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class MModelPerm {
    private static final JSONObject defaultPermInfo;

    static {
        defaultPermInfo = new JSONObject();
        defaultPermInfo.puts(PlvDef.plvType.chkType, 0)
                .puts(PlvDef.plvType.chkVal, 0);
    }

    private final JSONObject initPerm;
    private final HashMap<String, MModelPermInfo> permInfo;

    public MModelPerm(JSONObject pInfo) {
        this.initPerm = pInfo;
        this.permInfo = new HashMap<>();
        this.permInfo.put(PermissionsPowerDef.createMode, getOrCreatePermInfo(pInfo, PermissionsPowerDef.createMode));
        this.permInfo.put(PermissionsPowerDef.updateMode, getOrCreatePermInfo(pInfo, PermissionsPowerDef.updateMode));
        this.permInfo.put(PermissionsPowerDef.readMode, getOrCreatePermInfo(pInfo, PermissionsPowerDef.readMode));
        this.permInfo.put(PermissionsPowerDef.deleteMode, getOrCreatePermInfo(pInfo, PermissionsPowerDef.deleteMode));
        this.permInfo.put(PermissionsPowerDef.statisticsMode, getOrCreatePermInfo(pInfo, PermissionsPowerDef.statisticsMode));
    }

    /**
     * 获得就的权限模型
     */
    public JSONObject getInitPerm() {
        return this.initPerm;
    }

    /**
     * 获得权限定义,如果没有定义创建一个默认权限
     * 默认权限
     * 权限类型:权值
     * 权限值:0
     */
    private MModelPermInfo getOrCreatePermInfo(JSONObject permModel, String key) {
        return new MModelPermInfo((permModel != null && permModel.containsKey(key)) ? permModel.getJson(key) : defaultPermInfo);
    }

    /**
     * 获得创建数据权限信息
     */
    public MModelPermInfo createPerm() {
        return this.permInfo.get(PermissionsPowerDef.createMode);
    }

    /**
     * 获得更新数据权限信息
     */
    public MModelPermInfo updatePerm() {
        return this.permInfo.get(PermissionsPowerDef.updateMode);
    }

    /**
     * 获得读取数据权限信息
     */
    public MModelPermInfo readPerm() {
        return this.permInfo.get(PermissionsPowerDef.readMode);
    }

    /**
     * 获得删除数据权限信息
     */
    public MModelPermInfo deletePerm() {
        return this.permInfo.get(PermissionsPowerDef.deleteMode);
    }

    /**
     * 获得统计数据权限信息
     */
    public MModelPermInfo statisticsPerm() {
        return this.permInfo.get(PermissionsPowerDef.statisticsMode);
    }

    /**
     * 根据权限模型定义,生成检查数据库入库的数据模型
     */
    public HashMap<String, MModelRuleNode> buildPermRuleNode() {
        HashMap<String, MModelRuleNode> rMMRN = new HashMap<>();
        rMMRN.putAll(createPermRuleNode(PermissionsPowerDef.readMode, PermissionsPowerDef.readValue, this.readPerm()));
        rMMRN.putAll(createPermRuleNode(PermissionsPowerDef.updateMode, PermissionsPowerDef.updateValue, this.updatePerm()));
        rMMRN.putAll(createPermRuleNode(PermissionsPowerDef.createMode, PermissionsPowerDef.createValue, this.createPerm()));
        rMMRN.putAll(createPermRuleNode(PermissionsPowerDef.deleteMode, PermissionsPowerDef.deleteValue, this.deletePerm()));
        return rMMRN;
    }

    private MModelRuleNode createModeRuleNode(String fieldName, Object defaultValue, int checkRuleID) {
        return MModelRuleNode.buildRuleNode()
                .field(fieldName)
                .type(2)
                .initValue(defaultValue)
                .failedValue(defaultValue)
                .checkType(checkRuleID);
    }

    private HashMap<String, MModelRuleNode> createPermRuleNode(String modeKey, String valueKey, MModelPermInfo rInfo) {
        HashMap<String, MModelRuleNode> perms = new HashMap<>();
        // 生成字段值效验规则
        int checkValueType = 1;
        Object currentValue = rInfo.value();
        int checkMode = rInfo.type();
        switch (checkMode) {
            case PlvDef.plvType.powerVal:
                checkValueType = 6;  // 权限值类型为6
                break;
            case PlvDef.plvType.userOwn:
                if (currentValue.toString().equals("")) { // 权值是空字符串
                    currentValue = (new Session()).getUID();
                }
                break;
            case PlvDef.plvType.groupOwn:
                if (currentValue.toString().equals("")) { // 权值是空字符串
                    currentValue = (new Session()).getGID();
                }
                break;
            default:
                checkValueType = 1;  // 用户ID类型为1
                break;
        }
        // 生成 xmode ruleNode
        MModelRuleNode rm = createModeRuleNode(modeKey, checkMode, 6);
        perms.put(modeKey, rm);
        MModelRuleNode vm = createModeRuleNode(valueKey, currentValue, checkValueType);
        perms.put(valueKey, vm);
        return perms;
    }

    /**
     * 根据当前权限信息,输出权限结构数据
     */
    public JSONObject toJson() {
        JSONObject newPermJson = new JSONObject();
        for (String key : this.permInfo.keySet()) {
            newPermJson.put(key, this.permInfo.get(key).toJson());
        }
        return newPermJson;
    }
}
