package common.java.Apps;

import common.java.Check.CheckType;
import org.json.simple.JSONObject;

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

    public String field() {
        return this.nodeInfo.getString("fieldName");
    }

    public int type() {
        return this.nodeInfo.getInt("fieldType");
    }

    public Object initValue() {
        return this.nodeInfo.get("initValue");
    }

    public Object failedValue() {
        return this.nodeInfo.get("failedValue");
    }

    public CheckType checkType() {
        return new CheckType(this.nodeInfo.getString("checkType"));
    }

    public JSONObject node() {
        return this.nodeInfo;
    }

    public MModelRuleNode field(String fieldName) {
        this.nodeInfo.put("fieldName", fieldName);
        return this;
    }

    public MModelRuleNode type(int fieldType) {
        this.nodeInfo.put("fieldType", fieldType);
        return this;
    }

    public MModelRuleNode initValue(Object initValue) {
        this.nodeInfo.put("initValue", initValue);
        return this;
    }

    public MModelRuleNode failedValue(Object failedValue) {
        this.nodeInfo.put("failedValue", failedValue);
        return this;
    }

    public MModelRuleNode checkType(int checkType) {
        this.nodeInfo.put("checkType", checkType);
        return this;
    }

    public JSONObject toJson() {
        return this.nodeInfo;
    }

    public static class FieldType {
        public static final int publicField = 0;        //全公开字段
        public static final int maskField = 1;            //隐藏字段，输出时，需要显示声明才包含
        public static final int protectField = 2;        //保护字段，修改时,需要显示声明才包含
    }

}
