package common.java.Apps;

import common.java.Authority.PermissionsPowerDef;
import common.java.Database.DbFilter;
import common.java.Database.DbLayer;
import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MicroModel {
    private String tableName;
    private MModelRuleArray mmrArrray;
    private MModelPerm mmps;
    private boolean isUserModel;

    public MicroModel(JSONObject modelJson) {
        if (modelJson != null) {
            this.tableName = modelJson.getString("tableName");
            this.isUserModel = modelJson.getBoolean("user");
            this.mmps = new MModelPerm(modelJson.getJson("permissions"));
            this.mmrArrray = new MModelRuleArray(modelJson.getJsonArray("rule"), this.isUserModel);
        }
    }

    /**
     * 获得模型表名称
     */
    public String tableName() {
        return this.tableName;
    }

    /**
     * 获得规则组hashmap
     */
    public HashMap<String, MModelRuleNode> rules() {
        return this.mmrArrray.self();
    }

    /**
     * 获得规则组
     */
    public MModelRuleArray ruleArray() {
        return this.mmrArrray;
    }

    /**
     * 是否是用户服务专属模型
     */
    public boolean isUserModel() {
        return this.isUserModel;
    }

    /**
     * 获得权限组
     */
    public MModelPerm perms() {
        return this.mmps;
    }

    /**
     * 数据JSON结构的微服务模型
     */
    public JSONObject toJson() {
        return JSONObject.putx("tableName", this.tableName)
                .puts("user", this.isUserModel)
                .puts("rule", this.mmrArrray.toJsonArray())
                .puts("permissions", this.mmps.toJson());
    }

    /**
     * 根据当前微模型的权限模型更新模型对应的数据库数据内权限信息
     */
    public boolean updatePermInfo() {
        boolean rb = false;
        MModelPerm oldPerm = new MModelPerm(this.perms().getInitPerm());    // 老的权限模型生成一个perm对象
        MModelPerm nowPerm = this.perms();
        DbFilter tempDbCondition = DbFilter.buildDbFilter().and();
        JSONObject newData = new JSONObject();
        buildUpdateParam(0, oldPerm, nowPerm, tempDbCondition, newData);
        buildUpdateParam(1, oldPerm, nowPerm, tempDbCondition, newData);
        buildUpdateParam(2, oldPerm, nowPerm, tempDbCondition, newData);
        buildUpdateParam(3, oldPerm, nowPerm, tempDbCondition, newData);
        if (tempDbCondition.nullCondition()) {
            DbLayer db = new DbLayer(MicroServiceContext.current().config().db());  // 声明DB类
            db.where(tempDbCondition.build()).data(newData).updateAll();            // 更新数据
            rb = true;
        }
        return rb;
    }

    /**
     * @param op:0:增,1:删,3:查,4:改
     */
    private void buildUpdateParam(int op, MModelPerm oldPerm, MModelPerm newPerm, DbFilter filter, JSONObject data) {
        Object oldValue = null;
        Object newValue = null;
        String modeString = null;
        String valueString = null;
        if (op < 4) {
            switch (op) {
                case 0: {
                    oldValue = oldPerm.createPerm().type();
                    newValue = newPerm.createPerm().type();
                    modeString = PermissionsPowerDef.createMode;
                    valueString = PermissionsPowerDef.createValue;
                    break;
                }
                case 1: {
                    oldValue = oldPerm.deletePerm().type();
                    newValue = newPerm.deletePerm().type();
                    modeString = PermissionsPowerDef.deleteMode;
                    valueString = PermissionsPowerDef.deleteValue;
                    break;
                }
                case 2: {
                    oldValue = oldPerm.readPerm().type();
                    newValue = newPerm.readPerm().type();
                    modeString = PermissionsPowerDef.readMode;
                    valueString = PermissionsPowerDef.readValue;
                    break;
                }
                case 3: {
                    oldValue = oldPerm.updatePerm().type();
                    newValue = newPerm.updatePerm().type();
                    modeString = PermissionsPowerDef.updateMode;
                    valueString = PermissionsPowerDef.updateValue;
                    break;
                }
            }
            if (oldValue != newValue) {   // 比较权限类型有没有改变
                filter.eq(modeString, oldValue);
                data.put(modeString, newValue);
            }
            oldValue = oldPerm.readPerm().value();
            newValue = newPerm.readPerm().value();
            if (oldValue != newValue) {   // 比较权限类型有没有改变
                filter.eq(valueString, oldValue);
                data.put(valueString, newValue);
            }
        }
    }
}
