package common.java.Apps.MicroService.Model;

import common.java.Check.CheckType;
import org.json.gsc.JSONObject;

/**
 * 模型规则节点
 */
public class MModelRuleNode {
    private final JSONObject nodeInfo;

    public MModelRuleNode(JSONObject node) {
        this.nodeInfo = node;
    }

    //--------------------------------------------------
    public static MModelRuleNode buildRuleNode() {
        return new MModelRuleNode(new JSONObject());
    }

    public String name() {
        return this.nodeInfo.getString("name");
    }

    public int type() {
        return this.nodeInfo.getInt("type");
    }

    public Object init() {
        return this.nodeInfo.get("init");
    }

    public Object failed() {
        return this.nodeInfo.get("failed");
    }

    public CheckType checkId() {
        return new CheckType(this.nodeInfo.getString("checkId"));
    }

    public JSONObject node() {
        return this.nodeInfo;
    }

    public MModelRuleNode name(String fieldName) {
        this.nodeInfo.put("name", fieldName);
        return this;
    }

    public MModelRuleNode type(int fieldType) {
        this.nodeInfo.put("type", fieldType);
        return this;
    }

    public MModelRuleNode init(Object initValue) {
        this.nodeInfo.put("init", initValue);
        return this;
    }

    public MModelRuleNode failed(Object failedValue) {
        this.nodeInfo.put("failed", failedValue);
        return this;
    }

    public MModelRuleNode checkId(int checkType) {
        this.nodeInfo.put("checkId", checkType);
        return this;
    }

    public JSONObject toJson() {
        return this.nodeInfo;
    }

    public static class FieldType {
        public static final int publicField = 0;        //全公开字段
        public static final int maskField = 1;          //隐藏字段，输出时，需要显示声明才包含
        public static final int protectField = 2;       //保护字段，修改时,需要显示声明才包含
        public static final int lockerField = 9;        //锁定字段，不可修改
    }

}
