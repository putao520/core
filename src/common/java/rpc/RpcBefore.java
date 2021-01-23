package common.java.rpc;

import org.json.simple.JSONObject;

public class RpcBefore {
    // 过滤链
    public static final JSONObject filterArray = new JSONObject();
    // 通用过滤链
    public static FilterCallback global_fn = null;

    public static void $(String actionName, FilterCallback fn) {
        if (actionName.equals("*")) {
            global_fn = fn;
        } else {
            filterArray.puts(actionName, fn);
        }

    }

    public static void $(String[] actionNameArray, FilterCallback fn) {
        for (String actionName : actionNameArray) {
            $(actionName, fn);
        }
    }

    public static boolean global_filter(String actionName, Object[] input) {
        if (global_fn != null) {
            return global_fn.run(actionName, input);
        }
        return true;
    }

    public static boolean filter(String actionName, Object[] input) {
        if (!global_filter(actionName, input)) {
            return false;
        }
        FilterCallback fn = (FilterCallback) filterArray.get(actionName);
        if (fn != null) {
            return fn.run(actionName, input);
        }
        return true;
    }
}
