package common.java.apps;

import common.java.authority.PermissionsPowerDef;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class MModelRuleAutoFix {
    private static final HashMap<String, Object> waitting = new HashMap<>();
    private static final HashMap<String, Object> user_waitting = new HashMap<>();

    static {
        // 这里要补齐的普通DB字段
        waitting.put(PermissionsPowerDef.fatherIDField, "");
        waitting.put(PermissionsPowerDef.deleteField, 0);
        waitting.put(PermissionsPowerDef.visableField, 0);
        waitting.put(PermissionsPowerDef.levelField, 0);
        waitting.put(PermissionsPowerDef.powerValField, 0);
        waitting.put(PermissionsPowerDef.sortField, 0);
        waitting.put(PermissionsPowerDef.saltField, "");

        user_waitting.put(PermissionsPowerDef.adminField, "");
        user_waitting.put(PermissionsPowerDef.userField, "");
        user_waitting.put(PermissionsPowerDef.saltField, "");
    }

    /**
     * 补上没有定义的gsc-tree字段
     */
    protected HashMap<String, MModelRuleNode> ruleNodeFilter(HashMap<String, MModelRuleNode> map) {
        return ruleNodeFilter(map, false);
    }

    protected HashMap<String, MModelRuleNode> ruleNodeFilter(HashMap<String, MModelRuleNode> map, boolean user) {
        HashMap<String, Object> nowWaitting = (HashMap<String, Object>) waitting.clone();
        //合并用户模式需要字段
        if (user) {
            nowWaitting.putAll(user_waitting);
        }
        //生成需要补充的字段
        HashMap<String, Object> newWaitting = (HashMap<String, Object>) nowWaitting.clone();
        for (String fieldName : map.keySet()) {
            if (nowWaitting.containsKey(fieldName)) {
                newWaitting.remove(fieldName);
            }
        }
        // 剩下没有包含的字段
        for (String fieldName : newWaitting.keySet()) {
            map.put(fieldName, this.createEmptyNode(fieldName, newWaitting.get(fieldName)));
        }
        return map;
    }

    private MModelRuleNode createEmptyNode(String fieldName, Object initVal) {
        JSONObject node = new JSONObject();
        node.puts("fieldName", fieldName)
                .puts("field", 0)
                .puts("initValue", initVal)
                .puts("failedValue", initVal)
                .puts("checkType", 1);
        return new MModelRuleNode(node);
    }
}
