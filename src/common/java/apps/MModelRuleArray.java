package common.java.apps;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * 规则组
 */
public class MModelRuleArray extends MModelRuleAutoFix {
    private HashMap<String, MModelRuleNode> hashmap;

    public MModelRuleArray(JSONArray rules) {
        init(rules, false);
    }

    public MModelRuleArray(JSONArray rules, boolean user) {
        init(rules, user);
    }

    private void init(JSONArray rules, boolean user) {
        hashmap = new HashMap<>();
        for (Object l : rules) {
            MModelRuleNode mrn = new MModelRuleNode((JSONObject) l);
            hashmap.put(mrn.field(), mrn);
        }
        // 自动补齐模型未定义的gsc-tree模型必须字段
        hashmap = super.ruleNodeFilter(hashmap, user);
    }

    public HashMap<String, MModelRuleNode> self() {
        return hashmap;
    }

    public JSONArray toJsonArray() {
        JSONArray newRuleArray = new JSONArray();
        for (String key : hashmap.keySet()) {
            newRuleArray.adds(hashmap.get(key).toJson());
        }
        return newRuleArray;
    }
}
